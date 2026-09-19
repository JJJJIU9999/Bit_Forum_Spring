# BitForum 项目简报（给外部 AI 助手）

> **用途**：把本文件与仓库 `README.md` 一起粘贴给不了解本项目的外部 AI（例如网页版 GPT），
> 让它快速建立正确上下文，避免基于猜测作答。
> **维护**：每个模块（M13-M18）结束后同步更新「二、当前进度」与「五、README 过期信息」两节。
> **配套**：仓库根目录 `scripts/export-ai-context.sh` 可自动把本文件 + README + 指定代码文件
> 拼成一份可直接粘贴的上下文包。

---

## 一、项目是什么

个人毕业设计项目，把校园技术论坛从传统 Web 应用升级为**带 AI 能力的社区平台**。

- 后端：Java 17、Spring Boot 3.4.5、MyBatis-Plus、MySQL 8、Redis Stack、RabbitMQ
- 前端：React 19、Vite、React Router 7、纯 CSS
- AI：Spring AI 1.1.8 + DeepSeek（对话与工具调用）+ 本地 ONNX 嵌入模型（RAG 检索）
- 现有规模：后端约 130 个 Java 源文件，278 项后端测试、23 项前端测试

业务主体（M1–M12）已是一个完整论坛：板块、文章生命周期（草稿→审核→发布→下架）、
评论、点赞、收藏、关注、通知、举报治理、后台看板、OpenAPI、文件上传、健康检查。

## 二、当前进度（截至 2026-09-19）

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| M1–M12 | 论坛业务主体 | 已完成 |
| M13 | AI 基础设施与对话骨架（DeepSeek 接入、多轮记忆） | 已完成 |
| M14 | 工具集与 Tool Calling（AI 可查可操作站内数据） | 已完成 |
| M15 | RAG 知识库与向量检索（语义检索 + 回答带引用） | 已完成 |
| M16 | 内容审核 Agent（人机协同） | **未开始** |
| M17 | 运营分析 Agent 与智能推荐 | 未开始 |
| M18 | 可观测性、评估与工程化闭环 | 未开始 |

- 当前分支：`feat/ai-agent`（从 `main` 拉出，领先约 18 个提交，已推送）
- 测试基线：后端 278 项（0 失败、2 项条件跳过）、前端 23 项；GitHub Actions 通过
- 数据库迁移：Flyway 已到 **V14**（`ai_kb_document`、`ai_kb_chunk`）

## 三、M13–M15 已实现的 AI 能力（供你理解现状）

1. **多轮对话**：会话与消息持久化在 MySQL（`ai_conversation`、`ai_message`），重启不丢历史。
2. **工具调用**：AI 能真实查询与操作站内数据（搜索文章、看详情、热榜、板块、收藏、点赞、
   关注、建草稿等 12 个工具方法），写操作必须用户明确要求才执行。
3. **RAG 语义检索**：
   - 已发布文章按 350 字切块（相邻重叠 50 字、每块前置标题）→ 本地 ONNX 模型
     `bge-base-zh-v1.5`（768 维）向量化 → 存入 Redis Stack（RediSearch，HNSW + COSINE）
   - 用户提问时检索 topK=5 片段注入上下文，回答中标注来源编号 `[1]`，
     前端在回答下方渲染可点击的原帖链接
   - 文章审核通过 / 下架 / 删除时，经 RabbitMQ 异步更新知识库（事务提交后投递）
4. **管理端**：`/admin/kb` 展示知识库统计并可一键全量重建。

## 四、项目硬约定（请不要建议违反这些的做法）

1. **Flyway 只新增、绝不修改历史迁移**（当前最新 V14，新迁移从 V15 开始）。
2. **不引入** Spring Security、Spring Cloud、Elasticsearch、Kafka、Redux、Zustand、UI 组件库。
3. 接口统一返回 `Result<T>`（`code` / `message` / `data`）。
4. 权限用 JWT + `LoginInterceptor` / `AdminInterceptor`，不用 Spring Security。
5. **AI 能力必须有降级路径**：AI 不可用（网络、额度、超时）绝不能影响论坛主流程。
6. 测试栈：JUnit 5 + Spring Boot Test + Mockito + MockMvc；前端 Vitest + Testing Library。
7. 只新增依赖且需说明理由；中间件用 Docker Compose 编排。

## 五、README 中已过期的信息（重要，避免被误导）

仓库 `README.md` 描述的是 M1–M12（AI 升级之前）的状态，以下几处**已经过期**：

| README 中的说法 | 实际情况 |
| --- | --- |
| 「Flyway V1-V12」 | 已到 **V14**（M13 加 AI 会话表，M15 加知识库表） |
| 「Redis 7」 | 已换成 **Redis Stack**（`redis/redis-stack-server:7.4.0-v8`），因为 RAG 需要 RediSearch 向量检索 |
| 技术栈里没有 AI 相关 | 实际已引入 Spring AI 1.1.8（DeepSeek + ONNX 嵌入模型 + Redis 向量库） |
| 架构图只有通知消费者 | 实际还有知识库索引队列 `article.kb.index.queue`（独立 DLQ） |
| 「已实现能力」未提 AI | 见本简报第三节 |

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

## 七、下一步（M16 内容审核 Agent）

计划要点：Flyway V15 建 `ai_moderation_record`；多维度分析（违规/广告/灌水/敏感）+ 结构化输出；
文章与评论审核流联动；管理员「AI 判断对错」人工反馈闭环；100 条样本对照实验
（输出准确率 / 精确率 / 召回率 / 漏判率）。

**已知的现实差异**：
- 文章是「先审后发」（`submit()` → `PENDING` → 人工审核），AI 可在人工审核前给建议；
- 评论是「发布即公开」（`publish()` 直接入库），AI 只能做事后检测，生成疑似违规记录。
- **唯一没实测过的技术点**：DeepSeek 的结构化输出能否稳定映射成 Java 对象。

## 八、怎么配合我使用

- **来源要诚实**：本简报中的测试数字、验证结论都来自真实执行记录；
  如果某个说法没有证据，我会说明"未验证"。请不要把它当作营销材料。
- **需要更多细节时**，按需索取（不要一次全给，太长）：
  - 技术决策与实测证据：`docs/graduation/ai-agent-upgrade/findings.md`（按 6.x 分节）
  - 实施与验证记录：`docs/graduation/ai-agent-upgrade/progress.md`（按 M15-x 分节）
  - 总体计划：`docs/graduation/ai-agent-upgrade/task_plan.md`
  - 交接说明：`docs/graduation/ai-agent-upgrade/m15-handoff.md`
- **不要索取**：`.env`（含真实 API Key 与数据库密码）、任何密钥或生产凭据。

## 九、回答要求（可直接引用）

```text
以上是项目上下文。请遵守：
1. 先复述你对项目的理解，再回答我的问题；理解有偏差时我会纠正。
2. 未提供的文件不要假设其内容；需要更多材料时明确告诉我需要哪个文件。
3. 不要建议引入新框架或中间件（有明确的技术栈约束，见简报第四节）。
4. 不确定的地方请直接说不确定，不要编造。
```
