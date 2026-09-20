# BitForum 工程技术债

> 更新日期：2026-08-24
>
> 范围：当前 `main` 工作树的代码、配置、测试与容器编排。优先级表示后续处理顺序，不代表已发生生产事故。

## 当前开放项

### High Priority：MQ 发布与本地事务之间没有持久化补偿

- `ArticleService.approve` 在 MySQL 本地事务中更新文章/审核记录，并通过 `RabbitTemplate` 发布消息。
- Confirm/Returns 回调能记录 Broker 确认和路由失败，但尚无 Outbox、投递状态表或自动重试任务。
- 后续如要增强，应优先评估本地 Outbox + 定时投递，而不是引入更大的分布式架构。

### High Priority：DLQ 只保留失败消息，没有自动重放工具

- 消费失败会 `basicNack(requeue=false)` 并进入死信队列。
- 当前没有自动重试、指数退避、可视化重放或人工补偿状态。
- 在有明确故障频率和运维需求前，保持死信队列与可重放幂等语义，不提前搭建复杂平台。

### Medium Priority：Redis 指标同步仍是全量扫描

- `ArticleMetricSyncService` 每轮查询全部已发布文章，再读取 Redis 指标并更新变化项。
- 当前数据量下可运行，但扫描成本随已发布文章数增长。
- dirty set 需要同时改造浏览/点赞写入、同步失败恢复和删除链路；本轮不为尚未出现的规模问题扩大改动。

### Medium Priority：后端测试本地依赖基础服务

- 多数 Spring 集成测试会连接 MySQL；Redis 测试和容器启动还需要 Redis/RabbitMQ。
- GitHub Actions 使用 MySQL、Redis、RabbitMQ service containers 提供可复现环境，本地开发则通过 Compose 启动三个依赖。
- 尚未将测试系统拆成完全无依赖的单元测试和独立的集成测试 profile。

### Medium Priority：CI 仍依赖外网下载 ONNX 嵌入模型

- M15 起测试上下文会加载本地 ONNX 模型与 tokenizer（`TransformersEmbeddingModel` 在类路径存在时
  无条件构建，无法用开关关闭）。CI 原先没有任何缓存，**每次都要从 HuggingFace 下载上百 MB**。
- 实测后果：一次 `push` 的 backend job 因
  `Failed to cache the resource … SocketException: Connection reset` 直接失败——
  失败原因与当次代码无关，属环境/网络抖动。
- **已缓解**（2026-09-20，提交 `ci: cache the ONNX embedding model and retry its download`）：
  给 backend job 加 `actions/cache` 缓存 `~/.cache/bitforum-onnx`，并在缓存未命中时用
  `curl --retry 5 --retry-all-errors` **先带重试地预下载**，把网络依赖收敛到一个可重试的步骤。
- **仍未根治**：缓存首次填充仍需外网；彻底消除需要把模型文件托管到项目可用的位置
  （或让 CI 用替身 embedding 模型，但那会削弱 RAG 链路的测试覆盖）。

### Medium Priority：自动化测试与本地开发共用同一个 Redis 向量索引

- 全量 `mvn test` 会清空 Redis 向量索引 `bitforum-kb`（M15 起的既有行为），
  跑完需要用 `M17_VECTOR_PROBE=true ./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test` 重建，
  否则依赖向量召回的路径会**静默退化**为两路召回（不报错，只是少一路信号）。
- 影响：在本地开发/演示环境跑一次全量回归会破坏演示数据；换机器或他人复现时也容易踩到。
- 建议（未实施）：测试使用**独立索引名**（`BITFORUM_KB_INDEX` 已是环境变量）或独立 Redis database/容器，
  与开发环境的索引彻底隔开。

### Medium Priority：没有生产负载与端到端基线

- 当前验证聚焦于功能、业务规则、事务回滚、HTTP 响应和前端组件。
- **M18 已补**（2026-09-20）：AI 链路的本地性能观察（确定性持久化开销 n=35、真实端到端 n=5）与
  运行时故障注入（QA 对话链路 + 预算闸门），见 `docs/graduation/ai-agent-upgrade/` 的
  `ai-performance-observation.md` 与 `ai-fault-injection-report.md`，原始证据归档在同目录 `evidence/`。
- **M18 又补**：主流程隔离观察（`M18MainFlowIsolationTest`）—— 8 并发慢模型下非 AI 接口的 P50 不变、P95 +3 ms、0 错误。
- **仍未建立**：生产流量、更高并发的吞吐压测、浏览器端到端基线、审核（MQ）与洞察（线程池）的链路级故障注入。
  因此依旧**不对吞吐量或可用性做定量声明**；性能结论只代表"本地串行固定场景"。

## 已处理项

### MQ 消费幂等与失败恢复

- 删除模拟 `sleep` 和未接入业务的线程池。
- 消费者现在执行真实的“创建审核通过通知”业务，仅在成功后写入 Redis 标记并 ACK。
- `notification.source_message_id` 唯一约束作为持久幂等保证，Redis 每消息独立 key 保留 7 天。
- 测试覆盖正常消费、重复消息、处理失败和失败后重放。

### HTTP 错误语义

- 保留统一 `Result` 响应体，并将其错误码同步映射为实际 HTTP 状态。
- 校验、认证、授权、资源不存在、冲突与未知异常分别有 400/401/403/404/409/500 语义。
- 未知异常对外返回安全的通用消息，不暴露内部异常文本。

### 上传、列表查询与收藏冲突

- 图片上传不再只信任客户端 MIME，已校验扩展名、声明类型、文件头和 JPEG/PNG 可解码性。
- 文章列表分类名与收藏数由逐文章查询改为按页批量查询。
- 收藏保留“预检查 + 数据库唯一约束”，并将竞态中的 `DuplicateKeyException` 转为稳定的 HTTP 409。

### 健康检查、Compose 与 CI

- Actuator 健康状态语义已修正：`DOWN`、`OUT_OF_SERVICE` 和 `UNKNOWN` 对应 HTTP 503，不再强制返回 200。
- Compose 为 MySQL、Redis 和 RabbitMQ 配置健康检查，应用等待三个依赖健康后启动。
- GitHub Actions 分别运行后端测试与前端的 `npm ci`、`npm test`、`npm run build`；不包含部署步骤。

## 验证入口

```powershell
mvn test
cd frontend
npm ci
npm test
npm run build
cd ..
docker compose config --quiet
```

记录结果时应同时保留执行日期、分支/SHA、依赖服务状态与实际通过/失败数；历史结果不替代当前验证。
