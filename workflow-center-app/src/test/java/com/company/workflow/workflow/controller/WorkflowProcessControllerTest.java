package com.company.workflow.workflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flowable 流程控制器集成测试。
 *
 * <p>这里走真实 HTTP 接口而不是直接调 Service，目的是同时验证：</p>
 * <ul>
 *     <li>流程定义是否自动部署成功</li>
 *     <li>接口契约是否稳定</li>
 *     <li>流程从发起、主管审批到 HR 备案的最小闭环是否跑通</li>
 * </ul>
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:workflow-center-test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@AutoConfigureMockMvc
class WorkflowProcessControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDeployDefinitionAndRunMinimalApprovalProcess() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/process-definitions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].key").value("leaveApproval"));

        String startPayload = "{"
            + "\"processDefinitionKey\":\"leaveApproval\","
            + "\"initiator\":\"zhangsan\","
            + "\"managerAssignee\":\"manager01\","
            + "\"hrAssignee\":\"hr01\","
            + "\"businessKey\":\"LEAVE-20260512-0001\","
            + "\"title\":\"张三请假审批\","
            + "\"variables\":{\"leaveDays\":2,\"reason\":\"病假\"}"
            + "}";

        MvcResult startResult = mockMvc.perform(post("/api/v1/workflows/process-instances/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(startPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.processDefinitionKey").value("leaveApproval"))
            .andExpect(jsonPath("$.data.processStatus").value("RUNNING"))
            .andReturn();

        JsonNode startJson = objectMapper.readTree(startResult.getResponse().getContentAsString());
        String processInstanceId = startJson.path("data").path("processInstanceId").asText();

        MvcResult managerTaskResult = mockMvc.perform(get("/api/v1/workflows/tasks").param("assignee", "manager01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].taskName").value("直属主管审批"))
            .andExpect(jsonPath("$.data[0].processInstanceId").value(processInstanceId))
            .andReturn();

        JsonNode managerTaskJson = objectMapper.readTree(managerTaskResult.getResponse().getContentAsString());
        String managerTaskId = managerTaskJson.path("data").path(0).path("taskId").asText();

        String approvePayload = "{"
            + "\"operatorId\":\"manager01\","
            + "\"approved\":true,"
            + "\"comment\":\"同意请假\""
            + "}";

        mockMvc.perform(post("/api/v1/workflows/tasks/{taskId}/complete", managerTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.processEnded").value(false))
            .andExpect(jsonPath("$.data.currentTaskNames[0]").value("HR备案"));

        mockMvc.perform(get("/api/v1/workflows/tasks").param("assignee", "hr01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].taskName").value("HR备案"))
            .andExpect(jsonPath("$.data[0].processInstanceId").value(processInstanceId));
    }
}

