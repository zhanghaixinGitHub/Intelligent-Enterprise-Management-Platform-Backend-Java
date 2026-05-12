package com.company.workflow.workflow.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务办理结果响应。
 *
 * <p>企业级审批场景里，调用方在完成任务后通常马上要判断：</p>
 * <ul>
 *     <li>流程是否已经结束</li>
 *     <li>如果未结束，当前流转到哪些节点</li>
 * </ul>
 * 因此这里直接返回流程结束态和下一步任务摘要，减少上层二次查询次数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskOperationResponse {
    private String taskId;
    private String processInstanceId;
    private boolean processEnded;
    private List<String> currentTaskNames;
}

