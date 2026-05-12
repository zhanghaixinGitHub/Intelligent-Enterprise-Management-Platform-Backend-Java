package com.company.workflow.controller;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.company.workflow.common.model.ApiResponse;
import com.company.workflow.config.WorkflowCenterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
/**
 * 系统健康检查控制器。
 *
 * <p>保留这个接口的目的不是为了演示，而是为了给以下场景提供稳定探针：</p>
 * <ul>
 *     <li>IDE 本地启动后的快速自检</li>
 *     <li>后续 Python 平台调用 Java 服务前的可用性探测</li>
 *     <li>未来接入网关、注册中心、容器探针时复用统一契约</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system")
public class HealthController {
    private final WorkflowCenterProperties workflowCenterProperties;
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        log.info("HealthController.health   >>>   收到健康检查请求");
        return ApiResponse.success(MapUtil.<String, Object>builder()
            .put("status", "UP")
            .put("service", "workflow-center")
            .put("timestamp", DateUtil.now())
            .put("internalTokenConfigured", !"change-me-in-prod".equals(workflowCenterProperties.getSecurity().getInternalToken()))
            .build());
    }
}
