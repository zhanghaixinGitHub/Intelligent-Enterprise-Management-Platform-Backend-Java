package com.company.workflow;
import com.company.workflow.config.WorkflowCenterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
/**
 * Java 审批与企业级后台服务启动入口。
 *
 * <p>第一阶段先落基础工程骨架，后续在此基础上接入 Flowable、任务中心与企业集成能力。</p>
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(WorkflowCenterProperties.class)
public class WorkflowCenterApplication {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
            log.error("WorkflowCenterApplication.main   >>>   捕获到未处理异常，threadName={}", thread.getName(), throwable)
        );
        log.info("WorkflowCenterApplication.main   >>>   workflow-center 服务开始启动");
        SpringApplication.run(WorkflowCenterApplication.class, args);
    }
}
