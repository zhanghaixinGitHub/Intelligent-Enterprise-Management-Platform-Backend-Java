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
 * ??????????
 *
 * <p>????????????????????</p>
 * <ul>
 *     <li>IDE ???????</li>
 *     <li>?? Python ??? Java ????????</li>
 *     <li>????????????????</li>
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
        log.info("HealthController.health   >>>   ????????");
        return ApiResponse.success(MapUtil.<String, Object>builder()
            .put("status", "UP")
            .put("service", "workflow-center")
            .put("timestamp", DateUtil.now())
            .put("internalTokenConfigured", !"change-me-in-prod".equals(workflowCenterProperties.getSecurity().getInternalToken()))
            .build());
    }
}
