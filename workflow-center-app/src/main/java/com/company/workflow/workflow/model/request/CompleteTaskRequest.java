package com.company.workflow.workflow.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 完成待办任务请求。
 *
 * <p>企业级项目里，任务完成不仅仅是“点一下通过”，还需要把操作者、审批结果、审批意见和扩展变量一起沉淀，
 * 这样后续才能继续接审计、历史轨迹、催办、通知和 BI 分析。</p>
 */
@Data
public class CompleteTaskRequest {
    @NotBlank(message = "操作者不能为空")
    private String operatorId;

    /**
     * 审批结果。
     * 仅在需要条件网关判断的任务上使用；当前主管审批节点会强制要求该字段不能为空。
     */
    private Boolean approved;

    /**
     * 审批意见可选。
     * 当前先作为流程变量保留，后续可以迁移到独立审批意见表或 Flowable comment/history 体系。
     */
    private String comment;

    /**
     * 扩展变量可选。
     * 用于后续传递加签人、抄送人、审批金额等动态参数。
     */
    private Map<String, Object> variables;
}

