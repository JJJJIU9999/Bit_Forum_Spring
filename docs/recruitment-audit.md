# BitForum 求职展示代码审计

> 审计日期：2026-08-24
>
> 范围：`feat/frontend-refactor` 当前代码、配置、测试与仓库内容
>
> 原则：本文件记录问题，不在本轮扩大为业务重构；优先级表示后续处理顺序，不代表生产事故等级。

## 审计结论

- 未发现需要立即停止 README 整改或阻止 `main` 快进合并的确定性 P0 代码问题。
- `main` 是 `feat/frontend-refactor` 的祖先，分支没有双向分叉，具备 `--ff-only` 合并条件。
- 前端测试、前端构建和 Compose 配置解析已通过。
- 后端本轮执行 176 个测试，0 failure、4 errors；错误均因本机 Redis 未启动。RabbitMQ 也未启动并产生连接警告，因此当前不能写成“后端完整测试已通过”。
- 当前最值得后续处理的是 MQ 幂等/补偿、HTTP 状态语义、上传真实内容校验、N+1 查询和测试隔离。

## P0：影响正确性

当前静态审计未确认 P0。

说明：RabbitMQ 与 Redis 未启动造成的验证阻塞属于环境问题，但在依赖服务启动并重新跑绿完整测试前，不应把当前提交标记为“完整验证通过”。

## P1：面试容易被追问

### P1-1 MQ 幂等标记早于业务处理

- 证据：`NotificationListener.handlePublish` 先调用 `RedisService.markMessageProcessed`，随后才执行实际处理并 ACK。
- 风险：处理阶段异常后消息进入 DLQ，但幂等 Set 已有该 `messageId`；后续人工重放可能被当成重复消息直接 ACK。
- 建议：把幂等状态改为“处理中/成功”两阶段，或在业务结果与幂等记录之间建立可恢复的一致性方案；补一条失败后重放测试。

### P1-2 MQ 发布确认只有日志，没有完整补偿

- 证据：`RabbitMQConfig` 的 Confirm/Returns 回调只记录成功或失败日志；`ArticleService.approve` 在本地事务中更新数据库后直接发送消息。
- 风险：MySQL 本地事务、RabbitMQ 发布与 Redis 不属于同一事务，异步确认失败时没有 Outbox、重试表或人工补偿状态。
- 建议：面试中只描述为“可靠性基础链路”；后续如要增强，优先评估 Outbox + 定时投递/确认状态，而不是宣称 Exactly Once。

### P1-3 消费者保留模拟耗时与误导日志

- 证据：`NotificationListener` 使用 `Thread.sleep(2000)`，并打印“异步任务还在后台跑”；`ThreadPoolService` 没有被其他生产代码引用。
- 风险：实际阻塞的是 RabbitMQ 消费线程，和日志描述不一致；面试时容易被追问线程池是否真的接入。
- 建议：下一轮删除模拟代码，接入真实、可测试的通知处理；若无真实异步子任务，直接删除未使用线程池。

### P1-4 全局异常未映射正确 HTTP 状态

- 证据：`GlobalExceptionHandler` 返回 `Result.fail(code=400)`，但未使用 `ResponseEntity` 或 `@ResponseStatus` 设置 HTTP 状态。
- 风险：校验失败、404 与服务器异常可能都以 HTTP 200 返回，只在 JSON 中区分业务码，影响客户端、监控与 REST 语义。
- 建议：建立少量明确异常类型，并为 400/401/403/404/409/500 设置对应 HTTP 状态；避免一次性重写全部异常体系。

### P1-5 上传只信任声明的 MIME 类型

- 证据：`FileUploadService` 根据 `MultipartFile.getContentType()` 决定扩展名，只校验大小、声明类型和路径。
- 风险：客户端可伪造 Content-Type，把非图片内容保存为图片扩展名。
- 建议：增加文件签名或图片解码校验，并补充伪造 MIME 的测试；继续保留当前路径规范化和随机文件名。

### P1-6 列表元数据存在 N+1 查询

- 证据：`ArticleService.fillArticleMetadata(List<Article>)` 对每篇文章分别查询板块，再分别统计收藏数。
- 风险：分页每多一篇文章会增加两次数据库访问，面试中很容易被追问 SQL 次数与分页性能。
- 建议：按当前页 ID 批量查询板块和收藏数，或通过联表/聚合查询一次返回；先用 SQL 日志或测试证明查询次数再改。

### P1-7 完整测试依赖本地基础设施

- 证据：`RedisServiceTest` 直接连接 `localhost:6379`；Spring 上下文启动时尝试连接本地 RabbitMQ。
- 本轮结果：176 tests、0 failures、4 Redis connection errors。
- 风险：新环境或 CI 未启动依赖服务时无法稳定得到完整绿灯，历史测试数字也容易被误用为当前证据。
- 建议：区分纯单元测试与基础设施集成测试；集成测试使用可复现的容器环境或明确的测试启动脚本。

### P1-8 收藏并发重复时缺少业务异常转换

- 证据：`article_favorite` 有 `(user_id, article_id)` 唯一索引；`favoriteArticle` 先查询再插入，但没有像关注/举报服务那样捕获 `DuplicateKeyException`。
- 风险：并发重复收藏不会产生脏数据，但竞态下可能落入全局未知异常，返回通用“服务器内部错误”。
- 建议：保留唯一索引，并把重复键转换为稳定的“不能重复收藏”业务响应；补一条并发或重复键测试。

## P2：可以继续优化

### P2-1 Redis key 没有生命周期策略

- 浏览量、点赞集合、热榜和 `mq:processed:article_publish` 均未设置 TTL。
- 对长期保留的业务指标可明确“不设 TTL”的理由；对 MQ 幂等记录应设计保留时间或归档策略，避免集合无限增长。

### P2-2 指标同步会扫描全部已发布文章

- `ArticleMetricSyncService` 每轮查询全部 `PUBLISHED` 文章，再逐篇访问 Redis，并对变化项逐条更新 MySQL。
- 当前毕业设计数据量下可运行，但不应包装为高并发方案；后续可考虑 dirty set、分批扫描或增量队列。

### P2-3 Actuator DOWN 被映射为 HTTP 200

- `application.yml` 把 `down`、`out-of-service`、`unknown` 都映射为 200。
- 这会让只看 HTTP 状态的探针误判；如用于容器健康检查，应恢复可区分的非 2xx 状态或明确使用响应体判断。

### P2-4 Compose 只有启动顺序，没有就绪检查

- `depends_on` 使用 `service_started`，没有 MySQL、Redis、RabbitMQ 的 `healthcheck` 和 `service_healthy` 条件。
- 冷启动时应用可能早于依赖就绪；后续可在确有启动失败证据时补最小健康检查。

### P2-5 历史过程文档较多

- `docs/graduation`、`docs/phase1-records` 与 `docs/phase2-*` 保存了完整开发过程、历史测试快照和少量本机路径。
- 本轮保留毕业资料，不批量删除；根 README 不再把招聘者引向过程记录，并把两个直接含“GPT”的文件名改为中性历史名称。
- 后续如要进一步公开整理，可把纯协作记录集中到 `docs/dev-notes/`，但需先逐个确认毕业资料引用。

### P2-6 历史差异存在 EOF 多余空行

- `git diff --check main..feat/frontend-refactor` 报告多个历史文件 `new blank line at EOF`。
- 不影响运行；本轮避免批量格式化造成噪声，后续可在独立清理提交中处理。

## 本轮明确不做

- 不引入 Spring Cloud、Nacos、Kafka、Elasticsearch、Kubernetes 或新的中间件。
- 不为展示效果虚构 QPS、用户量、生产运行时间或性能提升比例。
- 不把 MySQL 本地事务描述为覆盖 RabbitMQ/Redis 的分布式事务。
- 不因历史文档出现 AI 协作痕迹而删除有价值的毕业设计资料。

## 后续验证入口

在 MySQL、Redis、RabbitMQ 均可用后执行：

```powershell
mvn test
cd frontend
npm test
npm run build
cd ..
docker compose config --quiet
```

测试结果必须记录执行日期、分支/SHA、依赖服务状态和实际通过/失败数；历史数字不能替代当前验证。
