# M12 Actuator 健康检查进度记录

## 2026-07-01

### Phase 1：接手检查与文档创建

- **Status:** complete
- 已执行前置检查：
  - `git branch --show-current`：`graduation-design`
  - `git status --short`：工作区非干净，包含 M1-M11 未提交改动
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git diff --stat`：确认已有大量未提交改动，未做回滚或清理
  - `Get-ChildItem -Path src\main\resources\db\migration | Sort-Object Name`：当前迁移包含 `V1` 至 `V11__add_file_upload_fields.sql`
- 已阅读：
  - `docs/graduation/毕业设计文档总览.md`
  - `docs/graduation/毕业设计进度.md`
  - `docs/graduation/m11-redis-sync/task_plan.md`
  - `docs/graduation/m11-redis-sync/findings.md`
  - `docs/graduation/m11-redis-sync/progress.md`
  - `pom.xml`
  - `src/main/resources/application.yml`
  - `src/test/resources/application.yml`
- 已创建：
  - `docs/graduation/m12-actuator-health/task_plan.md`
  - `docs/graduation/m12-actuator-health/findings.md`
  - `docs/graduation/m12-actuator-health/progress.md`

### Phase 2：代码调研

- **Status:** complete
- 已阅读：
  - `WebMvcConfig`
  - `AdminInterceptor`
  - `LoginInterceptor`
  - `AdminController`
  - `AdminDashboardController`
  - `AdminDashboardService`
  - `OpenApiConfig`
  - `OpenApiControllerTest`
  - `AdminControllerTest`
  - M11 `ArticleMetricSyncService` / `ArticleMetricSyncJob` / `RedisService`
- 已确认：
  - `/api/admin/**` 已由 `AdminInterceptor` 保护。
  - `/api/admin/health` 当前只是权限探活桩接口，需要升级为 M12 聚合健康检查接口。
  - 当前没有 actuator 依赖和 `management.*` 配置。

### Phase 3：后端实现

- **Status:** complete
- 已新增 `spring-boot-starter-actuator`。
- 已新增 management 配置：
  - 仅暴露 `health,info`。
  - `/actuator/health` 不显示组件明细。
  - `DOWN`、`OUT_OF_SERVICE`、`UNKNOWN` 的 HTTP 状态映射为 200。
- 已新增：
  - `AdminHealthResponse`
  - `HealthComponentStatus`
  - `AdminHealthService`
  - `AdminHealthController`
- 已将原 `/api/admin/health` 权限探活桩接口升级为管理员聚合健康检查接口。
- 本轮未新增 Flyway migration。

### Phase 4：测试

- **Status:** complete
- 已新增 `AdminHealthServiceTest`，覆盖：
  - 全部组件正常时整体 `UP`。
  - MySQL 异常时整体 `DOWN`，且 Redis/RabbitMQ 仍可正常返回。
  - Redis 异常时整体 `DOWN`。
  - RabbitMQ 异常时整体 `DOWN`。
- 已更新并移动为 `AdminHealthControllerTest`，覆盖：
  - 未登录不能访问 `/api/admin/health`。
  - 普通用户不能访问 `/api/admin/health`。
  - 禁用管理员不能访问 `/api/admin/health`。
  - 管理员可访问 `/api/admin/health`。
  - 组件 `DOWN` 时接口仍返回 `Result.ok`。
- 已新增 `ActuatorHealthControllerTest`，覆盖 `/actuator/health` 可访问且不暴露 `components` 明细。
- 已更新 `OpenApiControllerTest`，确认 `/api/admin/health` 进入 `/v3/api-docs`。

### Phase 5：文档收尾

- **Status:** complete
- 已更新 `docs/graduation/毕业设计文档总览.md`，将 M12 改为已完成、未提交。
- 已更新 `docs/graduation/毕业设计进度.md`，补充 M12 完成记录、接口、验证结果和文档入口。
- 已明确记录：M12 没有新增 Flyway migration。

### Phase 6：验证

- **Status:** complete
- M12 聚焦测试通过。
- 完整后端测试通过。
- 后端编译通过。
- 前端构建通过。
- `git diff --check` 通过，只有 LF/CRLF warning，没有实际 whitespace error。

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `Test-NetConnection -ComputerName localhost -Port 3306` | `TcpTestSucceeded: True` |
| `Test-NetConnection -ComputerName localhost -Port 6379` | 首次检查为 `False`，后续完整测试运行时 Redis 相关测试通过 |
| `Test-NetConnection -ComputerName localhost -Port 5672` | 首次检查为 `False`，后续测试运行时 RabbitMQ 可连接 |
| `mvn "-Dtest=AdminHealthServiceTest,AdminHealthControllerTest,ActuatorHealthControllerTest,OpenApiControllerTest" test` | 通过，12 个测试全部通过 |
| `mvn "-Dtest=AdminHealthServiceTest,AdminHealthControllerTest,ActuatorHealthControllerTest,OpenApiControllerTest,ArticleServiceTest" test` | 通过，40 个测试全部通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，176 个测试全部通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` | 通过 |
| `npm run build` | 通过，Vite 构建 118 个模块 |
| `git diff --check` | 通过，仅 LF/CRLF warning |

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 读取 RabbitMQ 配置时误写为 `RabbitConfig.java` | 已改用真实文件 `RabbitMQConfig.java` |
| PowerShell 下把通配路径直接传给 `rg` 导致路径语法错误 | 已改为对目录执行 `rg` |
| 完整测试中 `ArticleServiceTest` 上下文启动报 MySQL `Too many connections` | 已在测试配置中降低 Hikari `maximum-pool-size`，避免多个测试上下文耗尽本地 MySQL 连接数 |
