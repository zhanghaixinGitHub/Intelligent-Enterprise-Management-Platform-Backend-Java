package com.company.workflow.workflow.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程定义摘要响应。
 *
 * <p>前端或 Python 聚合层通常只需要最关键的元数据，不应直接暴露 Flowable 原生对象，
 * 这样可以形成稳定的防腐层，避免后续引擎升级时上层接口大面积震荡。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionSummaryResponse {
    private String id;
    private String key;
    private String name;
    private Integer version;
    private boolean suspended;
    private String deploymentId;
}

