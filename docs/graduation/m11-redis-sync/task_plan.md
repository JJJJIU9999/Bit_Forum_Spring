# M11 Redis 数据定时落库任务计划

## M11 目标

把 Redis 中的文章浏览量和点赞数定时同步回 MySQL `article.view_count`、`article.like_count` 字段，提升统计数据一致性和毕业设计完整度。

## 当前接手状态

- 当前分支：`graduation-design`
- 最近提交：`5321630 fix(frontend): align admin table action borders`
- 当前工作区非干净，包含 M1-M10 大量未提交改动，必须保留。
- 当前最新 Flyway 迁移：`V11__add_file_upload_fields.sql`
- 本轮只做 M11，不做 M12。
- 不自动 commit，不自动 push，不 stash，不 reset，不 checkout，不删除或覆盖既有未提交改动。

## 范围边界

- 同步范围以数据库文章列表为准，不使用 Redis `KEYS` 全量扫描。
- 复用现有 `article.view_count`、`article.like_count` 字段，不新增表。
- `article:hot` 热榜继续留在 Redis，不新增热榜落库字段。
- Redis 异常不能影响应用启动或定时线程稳定性。
- 后端优先，前端不做功能改动，但仍运行 `npm run build` 验证现有前端未破坏。
- 不引入 Spring Security、Spring Cloud、Elasticsearch、Kafka、复杂权限表、Redux、Zustand 或复杂 UI 组件库。

## 实现方案

- 在主启动类启用 Spring Scheduling。
- 新增 `ArticleMetricSyncService`：
  - 查询数据库中已发布文章 ID 和当前统计值。
  - 调用 `RedisService.getViewsIfPresent(articleId)` 和 `RedisService.getLikeCountIfPresent(articleId)` 获取 Redis 指标。
  - Redis 返回无数据时不把数据库已有统计清零。
  - Redis 指标存在且与数据库不同才更新文章统计字段。
  - 捕获 Redis 或单篇文章同步异常并记录日志，避免任务整体不可控失败。
- 新增定时方法，使用配置化 `fixedDelayString`，默认 5 分钟。
- 不新增管理员手动触发接口，避免扩大接口面和测试面。

## 测试清单

- Redis 有浏览量/点赞数时同步到 `article` 表字段。
- Redis 无数据时不把已有数据库统计错误清零。
- Redis 异常时同步任务不会导致整体失败。
- 定时任务方法会调用同步服务逻辑。

## 阶段计划

### Phase 1：接手检查与文档创建

- [x] 执行 git 分支、状态、最近提交、diff 统计和迁移目录检查。
- [x] 阅读毕业设计总览、进度、M10 文档。
- [x] 阅读 Redis、文章、后台统计相关代码和测试。
- [x] 创建 M11 文档目录和计划文件。

### Phase 2：后端同步能力

- [x] 启用 Spring Scheduling。
- [x] 新增 Redis 文章指标同步服务。
- [x] 新增配置化定时间隔，默认 5 分钟。

### Phase 3：测试

- [x] 新增服务层同步测试。
- [x] 新增定时任务调用测试。
- [x] 运行聚焦测试。

### Phase 4：文档收尾

- [x] 更新毕业设计总览，把 M11 改为真实完成状态。
- [x] 更新毕业设计进度，把 M11 改为真实完成状态。
- [x] 保持 M12 未开始。

### Phase 5：验证

- [x] 运行完整 `mvn test`。
- [x] 运行 `mvn -DskipTests compile`。
- [x] 运行 `frontend` 下 `npm run build`。
- [x] 运行 `git diff --check` 并记录结果。

## 错误记录

| 问题 | 处理 |
| --- | --- |
| `RedisServiceTest` 聚焦运行时连接 `localhost:6379` 失败 | 已确认本机没有 Redis 服务监听；M11 新增 service/job 测试改用 mock Redis 并已通过，完整测试需要先启动 Redis |
| 完整 `mvn test` 失败 | 170 个测试中 4 个错误，均为 `RedisServiceTest` 无法连接 `localhost:6379` |
