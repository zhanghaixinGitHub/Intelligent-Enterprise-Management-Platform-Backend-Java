package com.company.workflow.workflow.controller;

import com.company.workflow.common.model.ApiResponse;
import com.company.workflow.workflow.model.request.CompleteTaskRequest;
import com.company.workflow.workflow.model.request.StartProcessInstanceRequest;
import com.company.workflow.workflow.model.response.ProcessDefinitionSummaryResponse;
import com.company.workflow.workflow.model.response.ProcessStartResponse;
import com.company.workflow.workflow.model.response.TaskOperationResponse;
import com.company.workflow.workflow.model.response.TaskSummaryResponse;
import com.company.workflow.workflow.service.WorkflowProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Flowable 流程控制器。
 *
 * <p>这里先提供最小可联调接口集，确保：
 * Python 平台、前端页面、后续自动化测试都能基于稳定 REST 契约推进。
 * 等流程域稳定后，再继续扩展认领、退回、转办、加签、抄送等企业级能力。</p>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workflows")
public class WorkflowProcessController {
    private final WorkflowProcessService workflowProcessService;

    @GetMapping("/process-definitions")
    public ApiResponse<List<ProcessDefinitionSummaryResponse>> listLatestProcessDefinitions() {
        log.info("WorkflowProcessController.listLatestProcessDefinitions   >>>   收到查询流程定义请求");
        return ApiResponse.success(workflowProcessService.listLatestProcessDefinitions());
    }

    @PostMapping("/process-instances/start")
    public ApiResponse<ProcessStartResponse> startProcessInstance(@Valid @RequestBody StartProcessInstanceRequest request) {
        log.info(
            "WorkflowProcessController.startProcessInstance   >>>   收到发起流程请求，processDefinitionKey={}, initiator={}",
            request.getProcessDefinitionKey(),
            request.getInitiator()
        );
        return ApiResponse.success(workflowProcessService.startProcessInstance(request));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TaskSummaryResponse>> listUserTasks(@RequestParam @NotBlank(message = "办理人不能为空") String assignee) {
        log.info("WorkflowProcessController.listUserTasks   >>>   收到查询待办请求，assignee={}", assignee);
        return ApiResponse.success(workflowProcessService.listUserTasks(assignee));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<TaskOperationResponse> completeTask(
        @PathVariable("taskId") String taskId,
        @Valid @RequestBody CompleteTaskRequest request
    ) {
        log.info("WorkflowProcessController.completeTask   >>>   收到完成任务请求，taskId={}, operatorId={}", taskId, request.getOperatorId());
        return ApiResponse.success(workflowProcessService.completeTask(taskId, request));
    }
}

