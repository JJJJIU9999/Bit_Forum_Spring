# M8 Swagger / OpenAPI 进度记录

## 2026-06-30

### Phase 1：接手检查与需求确认

- **Status:** complete
- 已读取用户附加的 M8 续接要求。
- 已按 UTF-8 重新读取附件，修正默认编码读取乱码问题。
- 已执行前置检查：
  - `git branch --show-current`：`graduation-design`
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git status --short`：存在 M7 用户主页与个人资料相关未提交改动，以及根目录规划文件
  - `git diff --stat`：显示 M7 文档、前端、后端服务与测试等未提交修改
- 已阅读毕业设计总览、进度、M1-M7 任务文档和 `audit-fixes/progress.md`。
- 已确认 M8 不应修改数据库迁移链，当前最新迁移仍为 `V9__add_user_profile_fields.sql`。
- 已确认 M8 不能改变 M2 审核/下架、M3 收藏/搜索、M5 举报处理、M6 dashboard、M7 用户主页和个人资料语义。
- 已创建 `docs/graduation/m8-openapi/`。
- 已创建 M8 三份规划文件：
  - `docs/graduation/m8-openapi/task_plan.md`
  - `docs/graduation/m8-openapi/findings.md`
  - `docs/graduation/m8-openapi/progress.md`

### Phase 2：代码现状调研

- **Status:** complete
- 已阅读 `pom.xml`，确认当前 Spring Boot 版本为 `3.4.5`，Java 版本为 `17`，尚未引入 springdoc。
- 已阅读 `WebMvcConfig`，确认登录拦截器使用显式路径，不会默认覆盖 `/swagger-ui/**` 或 `/v3/api-docs`。
- 已阅读 `LoginInterceptor`、`AdminInterceptor` 和 `Result<T>`。
- 已扫描 controller 路径和 DTO 列表，确认 M8 注解可以按现有 controller 增量补充。

### Phase 3：实现 M8

- **Status:** complete
- `pom.xml` 新增 `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9`。
- 新增 `OpenApiConfig`，配置：
  - 标题：`BitForum 社区交流平台接口文档`
  - 版本：`1.0.0`
  - OpenAPI 描述
  - JWT Bearer security scheme：`bearerAuth`
- 给主要 controller 添加 `@Tag` 和关键 `@Operation`。
- 对需要登录或管理员权限的接口添加 Bearer Token 认证说明。
- 给关键请求 DTO 添加少量 `@Schema`。
- 新增 `OpenApiControllerTest`，验证 `/v3/api-docs` 和 `/swagger-ui/index.html`。

### Phase 4：验证

- **Status:** complete
- 聚焦测试 `mvn "-Dtest=OpenApiControllerTest" test` 通过，2 个测试全部通过。
- 后端编译 `mvn -DskipTests compile` 通过。
- 完整后端测试 `mvn test` 通过，132 个测试全部通过。
- 前端构建 `npm run build` 通过，Vite 构建 117 个模块。
- Flyway 校验通过，schema 当前仍为 v9；M8 没有新增数据库迁移。

## 验证记录

| 命令 | 结果 |
|------|------|
| `git branch --show-current` | 通过，当前为 `graduation-design` |
| `git log -1 --oneline` | 通过，最新提交为 `5321630 fix(frontend): align admin table action borders` |
| `git status --short` | 已确认工作区非干净，保留 M7 未提交改动 |
| `git diff --stat` | 已确认 M7 修改范围 |
| `mvn "-Dtest=OpenApiControllerTest" test` | 通过，2 个测试全部通过 |
| `mvn -DskipTests compile` | 通过 |
| `mvn test` | 通过，132 个测试全部通过 |
| `npm run build` | 通过，Vite 构建 117 个模块 |

## 错误记录

| 时间 | 问题 | 处理 |
|------|------|------|
| 2026-06-30 | 附件第一次按默认编码读取出现乱码 | 改用 `Get-Content -Encoding UTF8` 读取 |
| 2026-06-30 | `.codex` 下未找到 planning-with-files catchup 脚本 | 改用本机实际 `.agents` 技能路径 |
| 2026-06-30 | M8 规划文件首次误建到会话根目录 `D:\ClaudeCode\BitFrom\docs` | 已在正确 Maven 项目目录重新创建，并删除误建文件和空目录 |
| 2026-06-30 | `springdoc-openapi-starter-webmvc-ui:2.8.17` 启动 Swagger UI 静态资源映射时报 PathPattern 错误 | 改用官方兼容范围内的 `2.8.9` 重新验证 |

## 下一步

- `git diff --check` 已通过，仅有 Windows LF-to-CRLF 提示。
- 最终状态仍为未提交，等待用户确认是否提交。

## 2026-07-01

### 接手复核与重新验证

- **Status:** complete
- 已重新读取用户 M8 续接要求，并按 UTF-8 处理附件内容。
- 已重新执行接手检查：
  - `git branch --show-current`：`graduation-design`
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git status --short`：工作区仍为非干净状态，包含 M7 未提交改动和 M8 未提交改动
  - `git diff --stat`：确认 M7/M8 相关后端、前端、测试和文档变更仍在工作区
- 已复核 `pom.xml`、`OpenApiConfig`、`WebMvcConfig`、`OpenApiControllerTest`、迁移目录和主要 OpenAPI 注解。
- 已确认当前迁移目录仍为 `V1` 至 `V9__add_user_profile_fields.sql`，M8 没有新增数据库迁移。
- 已重新运行验证命令：
  - 聚焦测试 `mvn "-Dtest=OpenApiControllerTest" test` 通过，2 个测试全部通过。
  - 后端编译 `mvn -DskipTests compile` 通过。
  - 完整后端测试 `mvn test` 通过，132 个测试全部通过。
  - 前端构建 `npm run build` 通过，Vite 构建 117 个模块。
- 完整后端测试期间本机 Redis 6379 初始未启动，先后两次单独启动 Redis 后进程未能跨工具调用稳定保留；最终采用同一 PowerShell 执行块内临时启动 Redis、运行 `mvn test`、再停止 Redis 的方式完成验证。
- RabbitMQ 5672 当前未启动，测试日志有连接拒绝提示，但现有测试未因此失败。

### 2026-07-01 验证记录

| 命令 | 结果 |
|------|------|
| `mvn "-Dtest=OpenApiControllerTest" test` | 通过，2 个测试全部通过 |
| `mvn -DskipTests compile` | 通过 |
| `mvn test` | 通过，132 个测试全部通过；测试期间临时启动 Redis |
| `npm run build` | 通过，Vite 构建 117 个模块 |

### 2026-07-01 错误记录

| 问题 | 处理 |
|------|------|
| 首次聚焦测试在沙箱内无法访问 Maven Central，提示 Spring Boot parent POM 解析失败 | 使用网络权限重跑后通过 |
| 完整 `mvn test` 初次失败，`RedisServiceTest` 无法连接 `localhost:6379` | 确认 Redis 未启动，改为测试命令内临时启动 Redis 后通过 |
| 单独 `Start-Process` 启动 Redis 后进程未能稳定保留到下一次工具调用 | 将 Redis 启动、`mvn test`、Redis 停止放入同一个 PowerShell 执行块 |

### 用户开启 Redis 和 RabbitMQ 后复测

- **Status:** complete
- 用户确认 Redis 和 RabbitMQ 已开启后，重新运行完整后端测试：
  - `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test`
- 测试结果：通过，132 个测试全部通过，0 failures，0 errors，0 skipped。
- 测试日志显示 RabbitMQ 已成功连接到 `127.0.0.1:5672`，Redis 相关测试 `RedisServiceTest` 也已通过。
