package com.company.workflow.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 平台业务配置。
 *
 * <p>当前先把跨服务内部鉴权等关键配置收敛到配置类，避免后续散落硬编码。</p>
 */
@Data
@ConfigurationProperties(prefix = "workflow-center")
public class WorkflowCenterProperties {
    private Security security = new Security();
    @Data
    public static class Security {
        /**
         * Python 平台与 Java 审批中心内部调用令牌。
         * 正式环境必须通过环境变量或配置中心覆盖，禁止使用默认值长期运行。
         */
        private String internalToken = "change-me-in-prod";
    }
}
