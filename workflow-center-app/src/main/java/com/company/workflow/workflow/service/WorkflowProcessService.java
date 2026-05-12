package com.company.workflow.workflow.service;

import com.company.workflow.workflow.model.request.CompleteTaskRequest;
import com.company.workflow.workflow.model.request.StartProcessInstanceRequest;
import com.company.workflow.workflow.model.response.ProcessDefinitionSummaryResponse;
import com.company.workflow.workflow.model.response.ProcessStartResponse;
import com.company.workflow.workflow.model.response.TaskOperationResponse;
import com.company.workflow.workflow.model.response.TaskSummaryResponse;

import java.util.List;

/**
 * Flowable 流程服务。
 *
 * <p>当前阶段先暴露最小流程能力：查询流程定义、发起流程、查询待办、完成任务。
 * 后续可以在不破坏控制层契约的前提下，继续扩展认领、退回、转办、加签、抄送等企业级能力。</p>
 */
public interface WorkflowProcessService {
    List<ProcessDefinitionSummaryResponse> listLatestProcessDefinitions();

    ProcessStartResponse startProcessInstance(StartProcessInstanceRequest request);

    List<TaskSummaryResponse> listUserTasks(String assignee);

    TaskOperationResponse completeTask(String taskId, CompleteTaskRequest request);
}

