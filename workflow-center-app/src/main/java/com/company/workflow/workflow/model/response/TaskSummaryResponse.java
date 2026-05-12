package com.company.workflow.workflow.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待办任务摘要响应。
 *
 * <p>这里保留流程实例和定义标识，是为了让后续 Python 聚合层既能展示待办列表，
 * 也能按业务场景继续查询流程详情、表单详情和历史轨迹。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryResponse {
    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String assignee;
    private String processInstanceId;
    private String processDefinitionId;
    private String createTime;
}

