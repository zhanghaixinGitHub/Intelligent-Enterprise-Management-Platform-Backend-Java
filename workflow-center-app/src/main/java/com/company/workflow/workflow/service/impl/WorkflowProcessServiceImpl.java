package com.company.workflow.workflow.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.company.workflow.common.exception.BusinessException;
import com.company.workflow.workflow.model.request.CompleteTaskRequest;
import com.company.workflow.workflow.model.request.StartProcessInstanceRequest;
import com.company.workflow.workflow.model.response.ProcessDefinitionSummaryResponse;
import com.company.workflow.workflow.model.response.ProcessStartResponse;
import com.company.workflow.workflow.model.response.TaskOperationResponse;
import com.company.workflow.workflow.model.response.TaskSummaryResponse;
import com.company.workflow.workflow.service.WorkflowProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable 流程服务实现。
 *
 * <p>这里显式做了一层服务封装，而不是让控制器直接操作 Flowable API，主要是为了：</p>
 * <ul>
 *     <li>把流程引擎细节隔离在服务层，避免上层直接依赖第三方对象</li>
 *     <li>集中处理日志、参数归一化、边界校验和统一错误码</li>
 *     <li>为后续接入业务主表、组织架构、审计事件时保留稳定扩展点</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowProcessServiceImpl implements WorkflowProcessService {
    private static final String MANAGER_APPROVE_TASK_DEFINITION_KEY = "managerApproveTask";

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Override
    public List<ProcessDefinitionSummaryResponse> listLatestProcessDefinitions() {
        //repositoryService 是Flowable 引擎的核心服务之一
        //RepositoryService 专门负责管理流程定义（Process Definition），也就是 BPMN 流程文件的元数据
        //它底层会自动操作 Flowable 自己的数据库表（比如 ACT_RE_PROCDEF 等表）
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery() //创建一个流程定义查询对象（类似 MyBatis 的 QueryWrapper）
            .latestVersion() //只查每个流程的最新版本（如果同一个流程部署了多次，只取版本号最大的）
            .active() //只查激活状态的流程（排除已挂起/暂停的流程）
            .orderByProcessDefinitionKey() //按流程定义的 key 排序
            .asc() //按升序排序
            .list();

        log.info("WorkflowProcessServiceImpl.listLatestProcessDefinitions   >>>   查询流程定义完成，count={}", definitions.size());
        return definitions.stream()
            .map(definition -> new ProcessDefinitionSummaryResponse(
                definition.getId(),
                definition.getKey(),
                definition.getName(),
                definition.getVersion(),
                definition.isSuspended(),
                definition.getDeploymentId()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public ProcessStartResponse startProcessInstance(StartProcessInstanceRequest request) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(StrUtil.trim(request.getProcessDefinitionKey()))
            .latestVersion()
            .active()
            .singleResult();
        if (processDefinition == null) {
            throw new BusinessException("PROCESS_DEFINITION_NOT_FOUND", "未找到可用的流程定义，请确认流程已部署");
        }

        Map<String, Object> variables = buildStartVariables(request);
        String businessKey = generateBusinessKey(request);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            processDefinition.getKey(),
            businessKey,
            variables
        );
        List<String> currentTaskNames = queryCurrentTaskNames(processInstance.getProcessInstanceId());

        log.info(
            "WorkflowProcessServiceImpl.startProcessInstance   >>>   发起流程成功，processDefinitionKey={}, processInstanceId={}, businessKey={}, currentTaskCount={}",
            processDefinition.getKey(),
            processInstance.getProcessInstanceId(),
            businessKey,
            currentTaskNames.size()
        );
        return new ProcessStartResponse(
            processDefinition.getKey(),
            processInstance.getProcessInstanceId(),
            businessKey,
            currentTaskNames.isEmpty() ? "FINISHED" : "RUNNING",
            currentTaskNames
        );
    }

    @Override
    public List<TaskSummaryResponse> listUserTasks(String assignee) {
        String normalizedAssignee = StrUtil.trim(assignee);
        List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(normalizedAssignee)
            .active()
            .orderByTaskCreateTime()
            .desc()
            .list();

        log.info("WorkflowProcessServiceImpl.listUserTasks   >>>   查询待办完成，assignee={}, count={}", normalizedAssignee, tasks.size());
        return tasks.stream()
            .map(task -> new TaskSummaryResponse(
                task.getId(),
                task.getName(),
                task.getTaskDefinitionKey(),
                task.getAssignee(),
                task.getProcessInstanceId(),
                task.getProcessDefinitionId(),
                task.getCreateTime() == null ? null : DateUtil.format(task.getCreateTime(), DatePattern.NORM_DATETIME_PATTERN)
            ))
            .collect(Collectors.toList());
    }

    @Override
    public TaskOperationResponse completeTask(String taskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "待办任务不存在或已被处理");
        }
        if (StrUtil.isBlank(task.getAssignee())) {
            throw new BusinessException("TASK_ASSIGNEE_MISSING", "当前任务未配置办理人，暂不允许直接处理");
        }

        String operatorId = StrUtil.trim(request.getOperatorId());
        if (!StrUtil.equals(task.getAssignee(), operatorId)) {
            throw new BusinessException("TASK_OPERATOR_INVALID", "当前任务只能由指定办理人处理");
        }
        if (MANAGER_APPROVE_TASK_DEFINITION_KEY.equals(task.getTaskDefinitionKey()) && request.getApproved() == null) {
            throw new BusinessException("TASK_APPROVAL_RESULT_REQUIRED", "主管审批节点必须明确传入审批结果");
        }

        Map<String, Object> variables = buildCompleteVariables(request);
        taskService.complete(taskId, variables);

        boolean processEnded = runtimeService.createProcessInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .singleResult() == null;
        List<String> currentTaskNames = processEnded ? Collections.emptyList() : queryCurrentTaskNames(task.getProcessInstanceId());

        log.info(
            "WorkflowProcessServiceImpl.completeTask   >>>   完成任务成功，taskId={}, operatorId={}, processInstanceId={}, processEnded={}, nextTaskCount={}",
            taskId,
            operatorId,
            task.getProcessInstanceId(),
            processEnded,
            currentTaskNames.size()
        );
        return new TaskOperationResponse(taskId, task.getProcessInstanceId(), processEnded, currentTaskNames);
    }

    private Map<String, Object> buildStartVariables(StartProcessInstanceRequest request) {
        Map<String, Object> variables = new HashMap<>();
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            variables.putAll(request.getVariables());
        }

        variables.put("initiator", StrUtil.trim(request.getInitiator()));
        
        // 处理直属主管工号：优先使用传入的值，否则使用默认值
        String managerAssignee = StrUtil.isNotBlank(request.getManagerAssignee()) 
            ? StrUtil.trim(request.getManagerAssignee()) 
            : "manager01";
        variables.put("managerAssignee", managerAssignee);

        // 处理 HR 工号：优先使用传入的值，否则使用默认值
        String hrAssignee = StrUtil.isNotBlank(request.getHrAssignee()) 
            ? StrUtil.trim(request.getHrAssignee()) 
            : "hr01";
        variables.put("hrAssignee", hrAssignee);

        // 请假原因（替代原来的 formTitle）
        variables.put("leaveReason", StrUtil.trim(request.getLeaveReason()));

        // 请假时间
        variables.put("leaveTime", StrUtil.trim(request.getLeaveTime()));

        // 表单标题使用请假原因，保持兼容性
        variables.put("formTitle", StrUtil.trim(request.getLeaveReason()));
        return variables;
    }

    private Map<String, Object> buildCompleteVariables(CompleteTaskRequest request) {
        Map<String, Object> variables = new HashMap<>();
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            variables.putAll(request.getVariables());
        }
        variables.put("lastOperatorId", StrUtil.trim(request.getOperatorId()));
        if (request.getApproved() != null) {
            variables.put("approved", request.getApproved());
        }
        if (StrUtil.isNotBlank(request.getComment())) {
            variables.put("approvalComment", StrUtil.trim(request.getComment()));
        }
        return variables;
    }

    private List<String> queryCurrentTaskNames(String processInstanceId) {
        List<Task> currentTasks = taskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .active()
            .orderByTaskCreateTime()
            .asc()
            .list();
        if (currentTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> taskNames = new ArrayList<>();
        for (Task currentTask : currentTasks) {
            taskNames.add(currentTask.getName());
        }
        return taskNames;
    }

    private String normalizeBlank(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 生成业务单号。
     * 规则：LEAVE-日期-序号（例如：LEAVE-20260515-0001）
     * 采用工厂方法设计模式，便于后续扩展其他类型的单号生成规则。
     * 
     * @param request 流程启动请求
     * @return 业务单号
     */
    private String generateBusinessKey(StartProcessInstanceRequest request) {
        // 如果前端已经传入了业务单号，直接使用
        if (StrUtil.isNotBlank(request.getBusinessKey())) {
            return StrUtil.trim(request.getBusinessKey());
        }
        
        // 自动生成：LEAVE-日期-时间戳后4位
        String dateStr = DateUtil.format(DateUtil.date(), "yyyyMMdd");
        String sequence = String.valueOf(System.currentTimeMillis() % 10000);
        // 补齐4位
        sequence = String.format("%04d", Integer.parseInt(sequence));
        
        return String.format("LEAVE-%s-%s", dateStr, sequence);
    }
}

