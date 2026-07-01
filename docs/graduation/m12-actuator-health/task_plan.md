# M12 Actuator 健康检查任务计划

## M12 目标

为 BitForum 毕业设计补充应用健康检查能力，展示应用、MySQL、Redis、RabbitMQ 的健康状态，同时保持现有 JWT + Interceptor 权限风格。

## 当前接手状态

- 当前分支：`graduation-design`
- 最近提交：`5321630 fix(frontend): align admin table action borders`
- 当前工作区非干净，包含 M1-M11 大量未提交改动，必须保留。
- 当前最新 Flyway 迁移：`V11__add_file_upload_fields.sql`
- 本轮只做 M12，不重做 M1-M11。
- 不自动 commit，不自动 push，不 stash，不 reset，不 checkout，不删除或覆盖既有未提交改动。

## 范围边界

- 可以新增 `spring-boot-starter-actuator`。
- Actuator 原生端点只暴露 `health,info`。
- 匿名 `/actuator/health` 不暴露敏感组件细节。
- 管理员详细健康信息走 `/api/admin/**`，继续由 `AdminInterceptor` 保护。
- 不引入 Spring Security、Spring Cloud、Elasticsearch、Kafka、复杂权限表、Redux、Zustand 或复杂 UI 组件库。
- 不新增数据库表，不新增 Flyway migration，除非发现明确必要原因。
- 健康检查失败必须安全返回 DOWN/UNKNOWN，不能导致接口 500。

## 实现方案

- 在 `pom.xml` 新增 `spring-boot-starter-actuator`。
- 在主配置和测试配置中新增 management 暴露策略。
- 新增健康检查 DTO：
  - `AdminHealthResponse`
  - `HealthComponentStatus`
- 新增 `AdminHealthService`：
  - 应用状态直接返回 `UP`。
  - MySQL 使用 `DataSource` 执行 `SELECT 1`。
  - Redis 使用 `StringRedisTemplate` 连接 ping。
  - RabbitMQ 使用连接工厂创建轻量连接并立即关闭。
  - 每个组件独立捕获异常并返回 `DOWN`。
- 新增 `AdminHealthController`：
  - `GET /api/admin/health`
  - 返回 `Result<AdminHealthResponse>`
  - 添加 OpenAPI 注解。
- 扩展 OpenAPI 测试，确认新接口进入 `/v3/api-docs`。
- 新增控制器和服务层测试，覆盖权限和异常分支。

## 测试清单

- `/actuator/health` 可访问，且 management 配置生效。
- `/api/admin/health` 未登录不能访问。
- 普通用户不能访问 `/api/admin/health`。
- 管理员可以访问 `/api/admin/health`。
- MySQL、Redis、RabbitMQ 某个组件异常时接口仍返回统一成功响应，组件状态为 `DOWN`，不导致 500。
- `/v3/api-docs` 包含 `/api/admin/health`。

## 阶段计划

### Phase 1：接手检查与文档创建

- [x] 执行 git 分支、状态、最近提交、diff 统计和迁移目录检查。
- [x] 阅读毕业设计总览、进度、M11 文档。
- [x] 阅读 `pom.xml`、主配置、测试配置。
- [x] 创建 M12 文档目录和计划文件。

### Phase 2：代码调研

- [x] 阅读 `WebMvcConfig`、`AdminInterceptor`、管理员控制器和 OpenAPI 测试。
- [x] 阅读 M11 相关服务和测试，复用异常安全风格。
- [x] 确认 Actuator 依赖和 `/actuator` 配置当前不存在。

### Phase 3：后端实现

- [x] 新增 actuator dependency 和 management 配置。
- [x] 新增健康检查 DTO、service、controller。
- [x] 保持 `/api/admin/health` 由现有 AdminInterceptor 保护。

### Phase 4：测试

- [x] 新增或扩展 M12 后端测试。
- [x] 运行聚焦测试。

### Phase 5：文档收尾

- [x] 更新毕业设计总览，把 M12 改为真实完成状态。
- [x] 更新毕业设计进度，把 M12 改为真实完成状态。
- [x] 记录没有新增 Flyway migration。

### Phase 6：验证

- [x] 运行完整 `mvn test` 或记录本地服务阻断。
- [x] 运行 `mvn -DskipTests compile`。
- [x] 运行 `frontend` 下 `npm run build`。
- [x] 运行 `git diff --check` 并记录结果。

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 读取 RabbitMQ 配置时误写为 `RabbitConfig.java` | 已改用真实文件 `RabbitMQConfig.java` |
| PowerShell 下把通配路径直接传给 `rg` 导致路径语法错误 | 已改为对目录执行 `rg` |
| 完整测试中 `ArticleServiceTest` 上下文启动报 MySQL `Too many connections` | 已在测试配置中降低 Hikari `maximum-pool-size`，避免多个测试上下文耗尽本地 MySQL 连接数 |
