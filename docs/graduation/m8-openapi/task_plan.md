# M8 Swagger / OpenAPI 接口文档任务计划

## M8 目标

为当前比特论坛社区交流平台补充 Swagger / OpenAPI 接口文档，使后端接口在毕业设计答辩、前后端联调和验收时能够被清晰查看、按模块检索并支持 Bearer Token 调试。

## 当前阶段

Phase 5：文档更新与交付。

## 范围边界

- 仅实现 M8：Swagger / OpenAPI 接口文档。
- 不新增业务模块，不做 M9-M12。
- 不修改数据库结构，不新增 Flyway 迁移，不修改历史迁移 V1-V9。
- 不引入 Spring Security，不改变现有 JWT + Interceptor 权限方案。
- 不改变现有 controller 路径、业务返回结构或 `Result<T>` 格式。
- 不重构 M1-M7 的业务逻辑和前端布局。
- 默认不修改 React 前端页面；如需记录入口，优先写入毕业设计文档。
- 不自动 commit，不 stash、不回滚、不覆盖当前 M7 未提交改动。

## 依赖方案

- 使用 Spring Boot 3 兼容的 springdoc-openapi 方案。
- Maven 依赖优先选择 `org.springdoc:springdoc-openapi-starter-webmvc-ui`。
- 当前使用 `2.8.9`，兼容 Spring Boot 3.4.5 与 Java 17。
- 不使用 Springfox，不引入 Spring Security、Spring Cloud、Kafka 或复杂 RBAC。

## Swagger / OpenAPI 配置方案

- 新增 `src/main/java/com/bitforum/config/OpenApiConfig.java`。
- 配置文档标题：`BitForum 社区交流平台接口文档`。
- 配置版本：`1.0.0`。
- 描述中说明项目基于 Spring Boot 与 React，并覆盖认证、文章、评论、板块、审核、收藏、通知、举报、管理员看板、用户主页等接口。
- 暴露 springdoc 默认路径：
  - Swagger UI：`/swagger-ui/index.html`
  - OpenAPI JSON：`/v3/api-docs`
- 仅在确认 Swagger 路径被拦截时调整 `WebMvcConfig`，不主动改拦截器业务规则。

## 接口分组方案

- 用户认证：`UserController`
- 用户资料 / 用户主页：`UserProfileController`
- 文章：`ArticleController`、`UserArticleController`
- 评论：`CommentController`
- 板块：`CategoryController`、`AdminCategoryController`
- 我的内容：用户文章、收藏、通知、举报相关接口
- 管理员：`AdminArticleController`、`AdminUserController`、`AdminReportController`
- 管理员数据看板：`AdminDashboardController`

## 安全认证说明方案

- 在 OpenAPI 中配置 Bearer Token 安全方案：
  - type：HTTP
  - scheme：bearer
  - bearerFormat：JWT
- 在文档描述和必要接口注解中说明需要登录的接口使用 `Authorization: Bearer <token>`。
- 不要求 Swagger UI 自动登录。
- 不把所有接口统一强制标注为需要认证；公开接口与需登录接口保持区分。

## 测试清单

- 新增 MockMvc 测试验证 `/v3/api-docs` 返回 200。
- 视 springdoc 静态资源行为验证 `/swagger-ui/index.html` 返回 2xx 或 3xx。
- 运行后端聚焦测试。
- 运行 `mvn -DskipTests compile`。
- 运行完整 `mvn test`。
- 未修改前端时仍建议运行 `cd frontend && npm run build`，确认 M7 前端未受影响。

## 验证命令

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile
```

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

```powershell
cd frontend
npm run build
```

如需本地手工验证：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn spring-boot:run
```

访问：

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

## 风险和待确认事项

- springdoc 版本需与 Spring Boot 3.4.5 兼容，若版本不匹配可能出现启动失败。
- Swagger UI 静态资源在 MockMvc 测试中可能返回重定向或转发，需要按实际行为记录。
- 当前工作区存在 M7 未提交改动，M8 修改时必须避免覆盖这些文件中的 M7 语义。
- 如果网络受限导致 Maven 首次下载 springdoc 失败，需要用户确认网络或本地仓库状态。

## 阶段计划

### Phase 1：接手检查与文档阅读

- [x] 读取用户附加的 M8 续接要求。
- [x] 执行 git 分支、状态、最新提交和 diff 统计检查。
- [x] 阅读毕业设计总览、进度、M1-M7 文档和 audit-fixes 进度。
- **Status:** complete

### Phase 2：代码现状调研

- [x] 阅读 `pom.xml`、WebMvcConfig、拦截器、主要 controller、DTO 列表。
- [x] 判断依赖版本和注解覆盖范围。
- **Status:** complete

### Phase 3：实现 M8

- [x] 添加 springdoc 依赖。
- [x] 新增 OpenAPI 配置。
- [x] 给主要 controller 和关键 DTO 添加克制的 OpenAPI 注解。
- [x] 新增 OpenAPI 访问测试。
- **Status:** complete

### Phase 4：验证与修正

- [x] 运行聚焦测试。
- [x] 运行后端编译。
- [x] 运行完整后端测试。
- [x] 运行前端构建。
- **Status:** complete

### Phase 5：文档更新与交付

- [x] 更新毕业设计总览和进度。
- [x] 更新 M8 findings/progress。
- [x] 汇总修改、验证结果、风险和未提交状态。
- **Status:** complete

## 决策记录

| 决策 | 理由 |
|------|------|
| M8 不新增 Flyway 迁移 | 接口文档不需要数据库结构变化，且用户明确要求不修改 V1-V9 |
| 使用 springdoc-openapi-starter-webmvc-ui | 兼容 Spring Boot 3，符合用户要求 |
| springdoc 版本采用 2.8.9 | 2.8.17 在本项目 Spring Boot 3.4.5 下触发 Swagger UI PathPattern 初始化错误；2.8.9 聚焦测试、编译和完整测试均通过 |
| 保持 JWT + Interceptor | 避免引入 Spring Security 或改变现有鉴权语义 |
| 控制注解粒度 | 满足答辩和联调可见性，避免大规模重构业务代码 |
| 完整测试需要 Redis 可用 | `RedisServiceTest` 依赖 `localhost:6379`，2026-07-01 复核时通过临时 Redis 进程完成 `mvn test` |

## 错误记录

| 错误 | 尝试 | 处理 |
|------|------|------|
| 附件第一次按默认编码读取出现乱码 | 1 | 改用 UTF-8 重新读取 |
| planning-with-files catchup 脚本按 `.codex` 路径不存在 | 1 | 改用本机实际 `.agents` 技能路径 |
| M8 规划文件首次误建到会话根目录 `D:\ClaudeCode\BitFrom\docs` | 1 | 重新创建到 Maven 项目目录 `spring_code/bit-forum-spring/docs/graduation/m8-openapi`，并删除误建文件 |
| 2026-07-01 首次聚焦测试无法解析 Maven parent POM | 1 | 以网络权限重跑后通过 |
| 2026-07-01 完整 `mvn test` 因 Redis 6379 未启动失败 | 2 | 在同一 PowerShell 执行块内临时启动 Redis、运行测试并停止 Redis，完整测试通过 |
