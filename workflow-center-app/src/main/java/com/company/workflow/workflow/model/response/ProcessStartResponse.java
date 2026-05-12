package com.company.workflow.workflow.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程发起响应。
 *
 * <p>返回当前活动任务名称集合，而不是直接返回原始执行流对象，
 * 这样前端和 Python 平台可以用统一字段判断“流程刚发起后卡在哪个节点”。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStartResponse {
    private String processDefinitionKey;
    private String processInstanceId;
    private String businessKey;
    private String processStatus;
    private List<String> currentTaskNames;
}

