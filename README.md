# workflow-center
## 项目说明
`workflow-center` 是智能企业管理平台的 Java 后端基础工程，定位为后续承接：
- Flowable 审批流中心
- 企业级任务中心
- 复杂事务与长生命周期流程管理
- 与 Python 平台的内部服务集成
当前阶段先完成企业级多模块骨架建设，确保后续接入 Flowable、MySQL、Redis、消息队列与统一鉴权时具备稳定基础。
## 模块说明
- `workflow-center-common`：公共响应模型、异常定义、跨模块通用能力
- `workflow-center-app`：Spring Boot 启动模块，承载控制层、配置层、全局异常处理等
## 运行要求
- Maven 3.6+
- JDK 8 语义兼容编译（当前构建配置为 `source/target=1.8`）
## 本地启动
```powershell
cd D:\ideaProjects\workSpace03
mvn -q clean test
mvn -pl workflow-center-app -am spring-boot:run
```
默认启动端口：`8081`
健康检查接口：
- `GET http://localhost:8081/api/v1/system/health`
## 配置说明
当前默认使用本地 H2 文件数据库，目的是让工程初始化即可启动，便于第一阶段联调与骨架验证。
企业正式环境必须切换到 MySQL，并通过环境变量或配置中心覆盖以下配置：
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `WORKFLOW_CENTER_SECURITY_INTERNAL_TOKEN`
## 后续演进建议
1. 引入 Flowable 并建立流程定义、流程实例、任务中心模块
2. 增加 Python 平台内部调用鉴权与统一 TraceId 透传
3. 切换 MySQL / Redis / MQ 等基础设施
4. 增加审批业务 API、流程监听器、历史轨迹与审计能力
