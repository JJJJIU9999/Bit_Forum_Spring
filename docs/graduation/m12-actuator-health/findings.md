# M12 Actuator 健康检查调研记录

## 接手结论

- 当前分支为 `graduation-design`，最近提交为 `5321630 fix(frontend): align admin table action borders`。
- 工作区包含 M1-M11 未提交改动，属于预期状态，本轮必须增量实现 M12。
- 当前迁移目录最高到 `V11__add_file_upload_fields.sql`。
- `pom.xml` 当前已有 springdoc、Redis、RabbitMQ、MyBatis-Plus 等依赖，尚未发现 actuator 依赖。
- 主配置和测试配置当前尚未发现 `management.*` 配置。
- M11 文档明确当前本机 Redis `localhost:6379` 和 RabbitMQ `localhost:5672` 可能未监听，完整测试可能受本地服务状态影响。

## 设计决策

| 决策 | 理由 |
| --- | --- |
| `/actuator/health` 匿名可访问但不显示细节 | 当前项目没有 Spring Security，避免公开敏感组件细节 |
| 详细健康信息放到 `/api/admin/health` | 复用现有 `/api/admin/**` + `AdminInterceptor` 鉴权边界 |
| 组件失败返回 `DOWN` | 健康检查不应导致接口 500 或影响应用启动 |
| 不新增 Flyway migration | M12 不需要新增数据库结构 |

## 待确认点

- `AdminInterceptor` 已统一拦截 `/api/admin/**`，新增或升级 `/api/admin/health` 不需要额外配置路径。
- 当前已有 `AdminController` 提供 `/api/admin/health` 权限探活桩接口，M12 需要把它升级为真实组件健康检查接口。
- `OpenApiControllerTest` 当前通过 jsonPath 断言关键路径存在，可直接增加 `/api/admin/health` 断言。
- RabbitMQ 当前通过 Spring AMQP 自动配置连接工厂，健康检查可注入 `org.springframework.amqp.rabbit.connection.ConnectionFactory` 并在测试中 mock。

## 代码调研结论

- `Result<T>` 成功响应使用 `Result.ok(message, data)`，业务失败使用 `Result.fail(message)`。
- 管理员控制器已有 `@Tag`、`@SecurityRequirement(name = "bearerAuth")`、`@Operation` 注解风格。
- M11 的异常安全风格是组件异常只影响当前分支，并返回可控结果。
- 当前没有发现 actuator 依赖，也没有发现 `management.*` 配置。
- 完整测试中新增 actuator 相关上下文后，MySQL 出现 `Too many connections`，原因是多个 Spring 测试上下文各自持有 Hikari 连接池；测试配置需要降低 Hikari 池大小，避免环境连接数耗尽。

## 实现结果

- `pom.xml` 已新增 `spring-boot-starter-actuator`。
- `application.yml` 和测试配置已新增 management 配置，仅暴露 `health,info`。
- `/actuator/health` 设置为 `show-details: never`，匿名访问不显示组件明细。
- `management.endpoint.health.status.http-mapping` 将 `DOWN`、`OUT_OF_SERVICE`、`UNKNOWN` 映射为 HTTP 200，避免本地 Redis/RabbitMQ 波动导致匿名健康端点不可访问。
- 原 `AdminController` 已升级并移动为 `AdminHealthController`。
- 新增 `AdminHealthService`，独立检查应用、MySQL、Redis、RabbitMQ。
- 新增 `AdminHealthResponse` 和 `HealthComponentStatus`。
- M12 没有新增 Flyway migration。

## 验证发现

- 聚焦测试通过，覆盖 actuator health、管理员权限、管理员健康响应、组件异常分支和 OpenAPI 路径。
- 完整 `mvn test` 通过，176 个测试全部通过。
- 后端编译通过。
- 前端构建通过。
- `git diff --check` 通过，仅有 LF/CRLF warning。
