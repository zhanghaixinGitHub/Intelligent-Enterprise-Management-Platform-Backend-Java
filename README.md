# workflow-center
## 项目说明
`workflow-center` 是智能企业管理平台的 Java 后端基础工程，定位为后续承接：
- Flowable 审批流中心
- 企业级任务中心
- 复杂事务与长生命周期流程管理
- 与 Python 平台的内部服务集成
当前阶段已经完成企业级多模块骨架与 Flowable 最小审批流接入，确保后续扩展 MySQL、Redis、消息队列、统一鉴权与 Python 平台集成时具备稳定基础。
## 模块说明
- `workflow-center-common`：公共响应模型、异常定义、跨模块通用能力
- `workflow-center-app`：Spring Boot 启动模块，承载控制层、配置层、全局异常处理、Flowable 流程接口等
## 运行要求
- Maven 3.6+
- JDK 8 语义兼容编译（当前构建配置为 `source/target=1.8`）
## 当前已实现能力
- Flowable 流程引擎接入
- 本地 H2 自动建表与流程定义自动部署
- 最小审批流定义：`leaveApproval`
- 流程定义查询、流程发起、待办查询、任务完成接口
- 健康检查、统一异常处理、统一响应体

## 本地启动
```powershell
cd D:\ideaProjects\workSpace03
mvn -q clean test
mvn -q -DskipTests package
& 'D:\DevEnv\Java\jdk-21\bin\java.exe' -jar 'D:\ideaProjects\workSpace03\workflow-center-app\target\workflow-center-app-1.0.0-SNAPSHOT.jar'
```
默认启动端口：`8081`
健康检查接口：
- `GET http://localhost:8081/api/v1/system/health`

## 最小审批流接口清单
- `GET /api/v1/workflows/process-definitions`：查询已部署的最新流程定义
- `POST /api/v1/workflows/process-instances/start`：发起流程实例
- `GET /api/v1/workflows/tasks?assignee=manager01`：按办理人查询待办
- `POST /api/v1/workflows/tasks/{taskId}/complete`：完成待办任务

### 发起流程示例
```json
{
  "processDefinitionKey": "leaveApproval",
  "initiator": "zhangsan",
  "managerAssignee": "manager01",
  "hrAssignee": "hr01",
  "businessKey": "LEAVE-20260512-0001",
  "title": "张三请假审批",
  "variables": {
	"leaveDays": 2,
	"reason": "病假"
  }
}
```

### 当前流程说明
- 第一步：直属主管审批
- 第二步：如果主管同意，则流转到 `HR备案`
- 第三步：HR 处理完成后流程结束
- 如果主管驳回，则流程直接结束
## 配置说明
当前默认使用本地 H2 文件数据库，目的是让工程初始化即可启动，便于第一阶段联调与骨架验证。
企业正式环境必须切换到 MySQL，并通过环境变量或配置中心覆盖以下配置：
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `WORKFLOW_CENTER_SECURITY_INTERNAL_TOKEN`
## 后续演进建议
1. 补充认领、退回、转办、加签、抄送、历史轨迹等企业级审批能力
2. 增加 Python 平台内部调用鉴权、统一 TraceId 透传与适配层
3. 切换 MySQL / Redis / MQ 等基础设施
4. 增加审批业务主表、流程监听器、审计日志与组织架构集成
