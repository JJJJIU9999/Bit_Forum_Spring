# BitForum 项目简报（给外部 AI 助手）

> **用途**：把本文件与仓库 `README.md` 一起粘贴给不了解本项目的外部 AI（例如网页版 GPT），
> 让它快速建立正确上下文，避免基于猜测作答。
> **维护**：每个模块结束后同步「二、当前进度」；M13-M18 全部完成时已统一同步 README 与本节。
> **配套**：仓库根目录 `scripts/export-ai-context.sh` 可自动把本文件 + README + 指定代码文件
> 拼成一份可直接粘贴的上下文包。

---

## 一、项目是什么

个人毕业设计项目，把校园技术论坛从传统 Web 应用升级为**带 AI 能力的社区平台**。

- 后端：Java 17、Spring Boot 3.4.5、MyBatis-Plus、MySQL 8、Redis Stack、RabbitMQ
- 前端：React 19、Vite、React Router 7、纯 CSS
- AI：Spring AI 1.1.8 + DeepSeek（对话与工具调用）+ 本地 ONNX 嵌入模型（RAG 检索）
- 现有规模：后端 181 个 Java 源文件（其中 AI 域 75 个）、**416 项后端测试**、**36 项前端测试**

业务主体（M1–M12）已是一个完整论坛：板块、文章生命周期（草稿→审核→发布→下架）、
评论、点赞、收藏、关注、通知、举报治理、后台看板、OpenAPI、文件上传、健康检查。

## 二、当前进度（截至 2026-09-19）

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| M1–M12 | 论坛业务主体 | 已完成 |
| M13 | AI 基础设施与对话骨架（DeepSeek 接入、多轮记忆） | 已完成 |
| M14 | 工具集与 Tool Calling（AI 可查可操作站内数据） | 已完成 |
| M15 | RAG 知识库与向量检索（语义检索 + 回答带引用） | 已完成 |
| M16 | 内容审核 Agent（人机协同 + 评测） | 已完成 |
| M17 | 运营分析 Agent 与智能推荐（含离线评测） | 已完成 |
| M18 | 可观测性、工程化闭环（轨迹 / 用量 / 统一降级 / 预算闸门） | 已完成 |

- 当前分支：`feat/ai-agent`（从 `main` 拉出；相对 main 领先 **41** 个提交，已 push 且 CI success（**限 `514f301` 已提交内容**；本轮收口改动未提交、未过远端 CI））（截至 2026-09-20 快照；实时值以导出页眉或 `git` 命令为准）
- 测试基线：后端 **416 项（406 通过 + 10 条件跳过、0 失败）**、前端 **36 项**
- 数据库迁移：Flyway 已到 **V19**（AI 模块为 V13-V19）

## 三、M13–M18 已实现的 AI 能力（供你理解现状）

1. **多轮对话**：会话与消息持久化在 MySQL（`ai_conversation`、`ai_message`），重启不丢历史。
2. **工具调用**：AI 能真实查询与操作站内数据（搜索文章、看详情、热榜、板块、收藏、点赞、
   关注、建草稿等 12 个工具方法），写操作必须用户明确要求才执行。
3. **RAG 语义检索**：
   - 已发布文章按 350 字切块（相邻重叠 50 字、每块前置标题）→ 本地 ONNX 模型
     `bge-base-zh-v1.5`（768 维）向量化 → 存入 Redis Stack（RediSearch，HNSW + COSINE）
   - 用户提问时检索 topK=5 片段注入上下文，回答中标注来源编号 `[1]`，
     前端在回答下方渲染可点击的原帖链接
   - 文章审核通过 / 下架 / 删除时，经 RabbitMQ 异步更新知识库（事务提交后投递）
4. **内容审核 Agent（M16）**：文章提交审核 / 评论发布后经 RabbitMQ 异步送审，输出五维风险
   （有害 / 广告 / 诈骗 / 灌水 / 敏感，**固定字段**而非变长列表）与置信度；
   **只自动放行、绝不自动驳回**，管理员在 `/admin/moderation` 标记「AI 判断对错」形成反馈闭环。
5. **运营分析 Agent（M17）**：管理员触发 → 单线程池异步生成洞察报告，正文与数据快照同时落库以便对账。
6. **智能推荐（M17）**：三路召回（向量相似 / 热度 / 关注）+ RRF 融合 + Top-N，**排序完全由 Java 完成、可复现**；
   模型只写推荐理由（不参与选文）。落地在文章详情页「相关推荐」与 AI 助手回答末尾。
7. **可观测性与工程化（M18）**：每次 AI 调用落一条执行轨迹（路由 → 检索 → 工具调用链 → 每步耗时 → token），
   用量按 用户 / Agent / 天 聚合并估算成本，AI 不可用时的降级原因全站统一（`AiDegradeGuard`）；
   管理端 `/admin/ai-traces` 一页看轨迹与用量。另有克制版用量预算闸门（见第四节第 8 条）。
8. **管理端**：`/admin/kb`（知识库统计与全量重建）、`/admin/moderation`（AI 审核）、`/admin/ai-traces`（轨迹与用量）。

## 四、项目硬约定（请不要建议违反这些的做法）

1. **Flyway 只新增、绝不修改历史迁移**（当前最新 **V19**，新迁移从 V20 开始）。
2. **不引入** Spring Security、Spring Cloud、Elasticsearch、Kafka、Redux、Zustand、UI 组件库。
3. 接口统一返回 `Result<T>`（`code` / `message` / `data`）。
4. 权限用 JWT + `LoginInterceptor` / `AdminInterceptor`，不用 Spring Security。
5. **AI 能力必须有降级路径**：AI 不可用（网络、额度、超时）绝不能影响论坛主流程。
6. 测试栈：JUnit 5 + Spring Boot Test + Mockito + MockMvc；前端 Vitest + Testing Library。
7. 只新增依赖且需说明理由；中间件用 Docker Compose 编排。
8. **用量预算刻意保持克制**：只做「单用户每日 token/费用上限 + 超限统一降级 + 单次输入输出保护」，
   **不做**套餐、充值、余额、会员等级、分布式配额中心、复杂配置后台。
9. **不要为优化评测数字而改语料 / 换模型 / 调算法**：推荐指标已完成其验证使命，
   已知局限（向量区分度 0.0147、测试集仅 9 位用户）如实声明即可。

## 五、README 与仓库现状（2026-09-19 已同步）

仓库 `README.md` 已在 M18 收尾时同步到最新状态，可以放心引用：Flyway 版本、Redis Stack、
AI 技术栈、架构图里的知识库索引/审核队列与 DeepSeek 节点、AI 能力与工程边界均已更新。

更细的现状总结（含真实调用数据、评测口径与诚实边界、剩余事项）见：
`docs/graduation/ai-agent-upgrade/m18-status-summary.md`。

## 六、已经踩过的坑（避免你重复建议）

1. **`@ConditionalOnBean` 在用户配置类中不可靠**：Spring AI 的 bean 注册顺序会导致条件
   静默不成立，必须改用 `@ConditionalOnProperty`。
2. **DeepSeek starter 的自动配置没有属性开关**：只要依赖在类路径上就会创建 chat model
   bean 并校验 api-key 非空，因此测试环境必须提供占位 key，否则上下文启动失败。
3. **`spring-ai-starter-vector-store-redis` 的自动配置在本项目不可用**：
   它要求容器里有 `JedisConnectionFactory`（本项目用 Lettuce），且不支持声明元数据字段类型
   （RediSearch 要求过滤字段必须建索引时声明类型）。因此项目自己声明了 `RedisVectorStore` bean。
4. **Redis 必须是 Redis Stack**：普通 Redis 会报 `ERR unknown command 'FT._LIST'`，
   导致整个 Spring 上下文启动失败（CI 曾因此挂过一次）。
5. **异步索引消息必须在事务提交后发送**：否则消费者会读到事务提交前的旧状态，
   把文章误判为"不该在知识库中"而移除，事务提交后又不再触发索引 → 永久漏索引。
6. **涉及工具/提示词的改动必须用干净会话验证**：旧会话历史会让模型延续旧结论。
7. **嵌入模型选型必须实测**：`TransformersEmbeddingModel` 的 pooling 硬编码为 mean、
   不可配置，而不同模型训练约定不同（BGE 用 CLS）。实测 `bge-base-zh-v1.5` 中文检索最优。
8. **让模型返回「变长列表」时元素数量不稳定**（实测出现过 4/5/6 个）→ 结构化输出一律用固定字段（M16）。
9. **工具执行循环在 provider 内部完成**：业务拿到的最终响应里 `hasToolCalls()` 为 false，
   想采集工具链必须包装 `ToolCallback`（M18 实测）。
10. **ThreadLocal 不包装就传不进线程池**（实测子线程读到 null）：异步链路要显式传 traceId
    （MQ 消息头 / 任务闭包），并在目标线程重新挂载（M18 实测）。
11. **`@MapperScan` 不扫子包**：新增 Mapper 包必须在启动类里同步声明；
    新增需要用户身份的接口要同步加进 `WebMvcConfig` 的登录拦截列表（漏配直接 500）。

## 七、下一步（M13-M18 已完成；收口三项任务亦已完成）

功能开发已结束。**收口任务（文档一致性 / 性能观察 / 故障注入）已于 2026-09-20 完成**：
结果见 `final-closure-report.md`，专题见 `ai-performance-observation.md` 与 `ai-fault-injection-report.md`，
原始证据归档在同目录 `evidence/`。因此"压测 / 性能观察"不再是待办。

当前真正的下一步：

1. **提交并推送本轮收口改动，等新 HEAD 的远端 CI 结果**（旧 HEAD `514f301` 的 CI 成功**不能**覆盖未提交改动）；
2. 合并回 `main`（建议 `merge --no-ff`、不 squash，保留 M13-M18 的演进历史）+ 合并后 CI + 打版本 tag；
3. 论文 / 答辩材料撰写（三份报告的定位见最终收口报告）；
4. 建议的后续补强（**非合并阻塞**）：主流程隔离观察（并发/慢模型下非 AI 接口的 P95）、
   审核（MQ）与洞察（线程池）的链路级故障注入。

**已知的模型行为差异（写论文/答辩时可直接引用）**：
- 让模型输出变长列表时数量不稳定 → 固定字段；
- 结构化输出在 DeepSeek 上稳定可解析（M16 实测 8/8）；
- 模型会"不看数据就下结论" → M17 的 AnalystAgent 用「工具调用次数=0」留痕。

## 八、怎么配合我使用

- **来源要诚实**：本简报中的测试数字、验证结论都来自真实执行记录；
  如果某个说法没有证据，我会说明"未验证"。请不要把它当作营销材料。
- **需要更多细节时**，按需索取（不要一次全给，太长）：
  - 技术决策与实测证据：`docs/graduation/ai-agent-upgrade/findings.md`（按 6.x 分节）
  - 实施与验证记录：`docs/graduation/ai-agent-upgrade/progress.md`（按 M13-x ~ M18-x 分节）
  - 总体计划：`docs/graduation/ai-agent-upgrade/task_plan.md`
  - 现状总结：`docs/graduation/ai-agent-upgrade/m18-status-summary.md`
  - 环境与约束交接：`docs/graduation/ai-agent-upgrade/m18-handoff.md`
- **不要索取**：`.env`（含真实 API Key 与数据库密码）、任何密钥或生产凭据。

## 九、回答要求（可直接引用）

```text
以上是项目上下文。请遵守：
1. 先复述你对项目的理解，再回答我的问题；理解有偏差时我会纠正。
2. 未提供的文件不要假设其内容；需要更多材料时明确告诉我需要哪个文件。
3. 不要建议引入新框架或中间件（有明确的技术栈约束，见简报第四节）。
4. 不确定的地方请直接说不确定，不要编造。
```
