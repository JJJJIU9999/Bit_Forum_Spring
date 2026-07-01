# M11 Redis 数据定时落库调研记录

## 接手结论

- 当前分支为 `graduation-design`，最近提交为 `5321630 fix(frontend): align admin table action borders`。
- 工作区包含 M1-M10 未提交改动，属于预期状态，本轮必须增量实现 M11。
- 当前迁移目录最高到 `V11__add_file_upload_fields.sql`。
- `article` 表和 `Article` 实体已经有 `view_count` / `like_count` 字段，本轮不需要新增 Flyway migration。
- M10 文档明确 Redis 定时落库和 Actuator 仍未开始，本轮只推进 Redis 定时落库。

## 现有 Redis 用法

- `article:{articleId}:views`：String，文章浏览量。
- `article:{articleId}:likes`：Set，文章点赞用户集合。
- `article:hot`：ZSet，热门文章排行。
- `mq:processed:article_publish`：Set，RabbitMQ 消费幂等。

## 代码调研结论

- `RedisService.getViews(articleId)` 当前在 Redis key 不存在时返回 `0L`。
- `RedisService.getLikeCount(articleId)` 当前在 Redis set 不存在时返回 `0L`。
- `ArticleController.detail` 和 `ArticleController.hotList` 当前会直接读取 Redis 统计值并覆盖响应里的 `viewCount` / `likeCount`。
- `ArticleService.delete`、`deleteByAdmin` 和 `offline` 已经通过 `redisService.deleteArticleData(articleId)` 清理文章 Redis 指标和热榜成员，本轮不能破坏该行为。
- `AdminDashboardService` 已有 Redis 异常安全返回空热榜的模式，可作为 M11 异常处理风格参考。
- `ArticleMapper` 继承 MyBatis-Plus `BaseMapper<Article>`，可用 `selectList` 和 `updateById` 完成小范围同步。

## 设计决策

| 决策 | 理由 |
| --- | --- |
| 以数据库已发布文章列表作为同步范围 | 避免 Redis `KEYS` 全量扫描，且不会同步已删除文章 |
| Redis 无数据时跳过字段更新 | 避免应用重启、Redis 清理或单个 key 缺失时把 MySQL 既有统计清零 |
| 同步服务捕获运行时异常 | Redis 异常不能影响应用启动或定时线程稳定性 |
| 不新增管理员手动触发接口 | 当前需求后台定时和可直接调用 service 测试即可满足，避免扩大接口面 |
| 不新增数据库迁移 | 现有 `article.view_count`、`article.like_count` 已满足需求 |

## 待验证点

- MyBatis-Plus 同步服务基于数据库查询出来的 `Article` 实体更新，避免构造半实体时误写默认字段。
- 定时任务测试直接调用 `ArticleMetricSyncJob.syncArticleMetrics()`，不等待真实 fixed delay。

## 实现结果

- 主启动类已启用 `@EnableScheduling`。
- 新增 `ArticleMetricSyncService`，按数据库中 `PUBLISHED` 文章列表同步 Redis 浏览量和点赞数。
- 新增 `ArticleMetricSyncJob`，使用 `bitforum.article-metric-sync.fixed-delay` 和 `bitforum.article-metric-sync.initial-delay` 配置，默认均为 300000 毫秒。
- `RedisService` 新增 `getViewsIfPresent` 和 `getLikeCountIfPresent`，用于区分 Redis key 缺失和已有计数。
- 本轮没有新增 Flyway migration。
- 本轮没有新增管理员手动触发接口。

## 验证发现

- `mvn "-Dtest=ArticleMetricSyncServiceTest,ArticleMetricSyncJobTest" test` 通过，4 个测试全部通过。
- 把 `RedisServiceTest` 一起纳入聚焦测试时失败，原因是当前本机 `localhost:6379` 没有 Redis 监听；这是环境问题，不是 M11 新增 service/job 测试失败。
- 当前本机 `localhost:5672` 也没有 RabbitMQ 监听；聚焦测试中表现为 Spring AMQP 连接警告，但未导致 M11 新增测试失败。
