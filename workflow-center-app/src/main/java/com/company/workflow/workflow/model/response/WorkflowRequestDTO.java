package com.company.workflow.workflow.model.response;

import lombok.Data;

import java.util.List;

/**
 * 流程请求项 DTO。
 *
 * <p>用于返回用户发起的流程实例列表信息，包含流程状态、当前任务等关键信息。</p>
 */
@Data
public class WorkflowRequestDTO {

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 标题（从流程变量中获取）
     */
    private String title;

    /**
     * 流程状态（RUNNING: 运行中, COMPLETED: 已完成）
     */
    private String processStatus;

    /**
     * 当前任务名称列表
     */
    private List<String> currentTaskNames;

    /**
     * 发起时间
     */
    private String startTime;

    /**
     * 是否可撤回（只有审批中的流程可以撤回）
     */
    private Boolean canRevoke = false;
}
