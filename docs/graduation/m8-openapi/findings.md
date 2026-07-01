# M8 Swagger / OpenAPI 调研记录

## 需求摘录

- 为比特论坛补充 Swagger / OpenAPI 接口文档。
- 默认访问地址：
  - Swagger UI：`/swagger-ui/index.html`
  - OpenAPI JSON：`/v3/api-docs`
- 文档标题：`BitForum 社区交流平台接口文档`。
- 文档版本：`1.0.0`。
- 使用 Spring Boot 3 兼容的 springdoc-openapi。
- 不引入 Springfox，不引入 Spring Security。
- 不修改 JWT + Interceptor 权限方案。
- 不修改数据库，不新增 Flyway 迁移，不修改 V1-V9。
- 不改变业务返回结构，继续使用 `Result<T>`。
- 优先覆盖主要 controller，DTO 注解保持克制。

## 已核对的当前状态

- 当前分支：`graduation-design`。
- 最新提交：`5321630 fix(frontend): align admin table action borders`。
- 工作区存在 M7 用户主页与个人资料相关未提交改动，必须保留。
- 根目录已有未提交的 `task_plan.md`、`findings.md`、`progress.md`，本轮不覆盖。
- 当前最新已知 Flyway 迁移为 `V9__add_user_profile_fields.sql`。

## 毕业设计文档阅读发现

- 总览和进度文档确认：M1-M7 已完成并验证，当前进入 M8；M8-M12 不能写成已完成。
- 当前迁移链为 `V1` 至 `V9__add_user_profile_fields.sql`，M8 不需要新增迁移。
- M2 语义：`/api/article/publish` 保留旧路径但语义为提交审核，公开文章查询只处理 `PUBLISHED`。
- M3 语义：收藏、我的收藏、搜索都只面向 `PUBLISHED` 文章，搜索使用 MySQL `LIKE`。
- M4 语义：通知中心同步写 `notification` 表，不新增通知队列。
- M5 语义：举报处理只记录处理结果，不自动下架文章或删除评论。
- M6 语义：管理员 dashboard 实时聚合现有表和 Redis 热门文章，Redis 异常不影响整体返回。
- M7 语义：用户资料仅允许更新 `avatar`、`nickname`、`bio`；公开用户文章只返回 `PUBLISHED`。
- audit-fixes 说明 `V8__add_pending_report_unique_key.sql` 来源于举报待处理唯一约束修复，M8 不能重做 audit fixes。

## 代码调研发现

- `pom.xml` 当前使用 Spring Boot `3.4.5`、Java `17`，尚未引入 springdoc。
- `WebMvcConfig` 对登录接口使用显式 `addPathPatterns`，未全局拦截，Swagger 默认路径理论上不需要排除。
- 管理员接口统一由 `/api/admin/**` 的 `AdminInterceptor` 保护。
- `Result<T>` 保持简单 `{code,message,data}` 结构，M8 不需要调整。
- 当前 controller 路径已覆盖认证、文章、评论、板块、我的内容、管理员、举报、dashboard、用户主页。
- 已新增 `springdoc-openapi-starter-webmvc-ui:2.8.9`。
- 已新增 `OpenApiConfig`，配置标题、版本、描述和 `bearerAuth` JWT security scheme。
- 已给主要 controller 补充 `@Tag`、关键 `@Operation`，并对登录/管理员接口标注 Bearer Token。
- 已给关键请求 DTO 补充少量 `@Schema`，不改变校验规则。

## 技术判断

| 判断 | 依据 |
|------|------|
| M8 应为后端优先 | Swagger/OpenAPI 主要由后端暴露 |
| 不应新增数据库迁移 | 文档能力不需要持久化结构 |
| 不应修改 LoginInterceptor | 当前拦截器按明确路径拦截，Swagger 默认路径理论上不受影响 |
| 前端默认不改 | M8 目标是 API 可见性，用户明确要求不重写前端布局 |

## 待确认风险

- 官方兼容矩阵显示 Spring Boot 3.4.x 可使用 springdoc 2.7.x - 2.8.x；`2.8.17` 在本项目测试中触发 Swagger UI 静态资源 PathPattern 初始化错误，改用同属 2.8.x 的 `2.8.9` 验证。
- `OpenApiControllerTest` 已验证 `/v3/api-docs` 返回 200，`/swagger-ui/index.html` 返回 200。
- Maven 首次拉取依赖已完成，后续本机应可复用本地仓库缓存。
- 2026-07-01 重新验证时，完整 `mvn test` 依赖本机 Redis 6379；Redis 未启动会导致 `RedisServiceTest` 失败。使用临时 Redis 进程后完整测试通过。
- RabbitMQ 5672 未启动时会在 Spring Boot 测试日志中出现连接拒绝提示，但当前测试套件不因此失败。
