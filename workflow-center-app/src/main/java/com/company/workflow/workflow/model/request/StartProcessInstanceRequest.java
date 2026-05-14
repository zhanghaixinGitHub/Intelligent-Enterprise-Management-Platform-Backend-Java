package com.company.workflow.workflow.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 发起流程实例请求。
 *
 * <p>当前最小审批流骨架显式要求传入发起人、主管审批人、HR 办理人，原因是：</p>
 * <ul>
 *     <li>先确保跨服务联调时流程一定能够流转，不会因为组织架构尚未接入而出现孤儿任务</li>
 *     <li>后续接入 Python 用户中心或组织中心时，可以逐步把这些字段改造成后端自动推导</li>
 *     <li>保留 variables 扩展字段，避免后面每新增一个表单字段都修改基础接口契约</li>
 * </ul>
 */
@Data
public class StartProcessInstanceRequest {
    @NotBlank(message = "流程定义 Key 不能为空")
    private String processDefinitionKey;

    @NotBlank(message = "发起人不能为空")
    private String initiator;

    @NotBlank(message = "主管审批人不能为空")
    private String managerAssignee;

    @NotBlank(message = "HR 办理人不能为空")
    private String hrAssignee;

    /**
     * 业务单号可选。
     * 企业项目里建议传业务单号，便于把流程实例和业务主表做稳定关联。
     */
    private String businessKey;

    /**
     * 表单标题可选。
     * 当前作为流程变量保存，方便后续待办列表直接展示摘要信息。
     */
    private String title;

    /**
     * 扩展流程变量。
     * 这里保留 Map 是为了支持后续请假天数、金额、部门、原因等表单字段渐进扩展。
     */
    private Map<String, Object> variables;
}

