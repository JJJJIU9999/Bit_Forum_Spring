# M11 Redis 数据定时落库进度记录

## 2026-07-01

### Phase 1：接手检查与文档创建

- **Status:** complete
- 已执行前置检查：
  - `git branch --show-current`：`graduation-design`
  - `git status --short`：工作区非干净，包含 M1-M10 未提交改动和文档、前端、后端、测试变更
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git diff --stat`：确认已有大量未提交改动，未做回滚或清理
  - `Get-ChildItem -Path src\main\resources\db\migration | Sort-Object Name`：当前迁移包含 `V1` 至 `V11__add_file_upload_fields.sql`
- 已阅读：
  - `docs/graduation/毕业设计文档总览.md`
  - `docs/graduation/毕业设计进度.md`
  - `docs/graduation/m10-file-upload/task_plan.md`
  - `docs/graduation/m10-file-upload/findings.md`
  - `docs/graduation/m10-file-upload/progress.md`
  - Redis、文章、后台统计相关服务和测试
- 已创建：
  - `docs/graduation/m11-redis-sync/task_plan.md`
  - `docs/graduation/m11-redis-sync/findings.md`
  - `docs/graduation/m11-redis-sync/progress.md`

### Phase 2：后端同步能力

- **Status:** complete
- 已在 `BitForumSpringApplication` 启用 Spring Scheduling。
- 已新增 `ArticleMetricSyncService`：
  - 查询数据库中 `PUBLISHED` 文章作为同步范围。
  - 使用 Redis 可空读取方法获取浏览量和点赞数。
  - Redis 无数据时跳过更新，保留数据库既有统计。
  - 单篇文章同步异常会记录日志并继续处理后续文章。
- 已新增 `ArticleMetricSyncJob`：
  - 通过 `@Scheduled` 定时调用同步服务。
  - `fixed-delay` 默认 300000 毫秒。
  - `initial-delay` 默认 300000 毫秒，避免应用启动后立即触发后台同步。
- 已在 `application.yml` 增加 `bitforum.article-metric-sync` 配置项。
- 本轮未新增 Flyway migration。

### Phase 3：测试

- **Status:** complete
- 已新增 `ArticleMetricSyncServiceTest`，覆盖：
  - Redis 有浏览量和点赞数时同步到 `article` 表字段。
  - Redis 无数据时不清零数据库已有统计。
  - Redis 异常时同步方法不向外抛出异常。
- 已新增 `ArticleMetricSyncJobTest`，覆盖定时任务方法委托调用同步服务。
- 已扩展 `RedisServiceTest`，覆盖 `getViewsIfPresent` 和 `getLikeCountIfPresent` 缺失 key 返回 null。

### Phase 4：文档收尾

- **Status:** complete
- 已更新 `docs/graduation/毕业设计文档总览.md`，将 M11 改为已完成、未提交。
- 已更新 `docs/graduation/毕业设计进度.md`，补充 M11 完成记录、文档入口和验证结果。
- M12 仍保持可选、未开始。

### Phase 5：验证

- **Status:** complete with environment blocker
- 后端编译通过。
- M11 新增 service/job 聚焦测试通过。
- 完整后端测试已运行，但当前本机 Redis 未监听导致已有 `RedisServiceTest` 失败。
- 前端构建通过。
- `git diff --check` 通过，只有 LF/CRLF warning，没有实际 whitespace error。

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `mvn "-Dtest=ArticleMetricSyncServiceTest,ArticleMetricSyncJobTest,RedisServiceTest" test` | 失败；`RedisServiceTest` 连接 `localhost:6379` 被拒绝，当前本机 Redis 未监听 |
| `mvn "-Dtest=ArticleMetricSyncServiceTest,ArticleMetricSyncJobTest" test` | 通过；4 个测试全部通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` | 通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 失败；Tests run: 170, Failures: 0, Errors: 4，均为 `RedisServiceTest` 无法连接 Redis |
| `npm run build` | 通过；Vite 构建 118 个模块 |
| `git diff --check` | 通过；仅 LF/CRLF warning |
| `Test-NetConnection -ComputerName localhost -Port 6379` | `TcpTestSucceeded: False` |
| `Test-NetConnection -ComputerName localhost -Port 5672` | `TcpTestSucceeded: False` |

## 错误记录

| 问题 | 处理 |
| --- | --- |
| `RedisServiceTest` 失败：`Unable to connect to Redis` | 已确认 `localhost:6379` 未监听；记录为环境阻断，完整 `mvn test` 需先启动 Redis |
| Spring AMQP 日志提示 RabbitMQ `localhost:5672` 连接拒绝 | 聚焦 M11 新增测试未因此失败；完整运行前仍建议启动 RabbitMQ |
