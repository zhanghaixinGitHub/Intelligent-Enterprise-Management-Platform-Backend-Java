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
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
            .latestVersion()
            .active()
            .orderByProcessDefinitionKey()
            .asc()
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
        String businessKey = normalizeBlank(request.getBusinessKey());
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
        variables.put("managerAssignee", StrUtil.trim(request.getManagerAssignee()));
        variables.put("hrAssignee", StrUtil.trim(request.getHrAssignee()));
        variables.put("formTitle", StrUtil.blankToDefault(StrUtil.trim(request.getTitle()), "通用审批单"));
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
}

