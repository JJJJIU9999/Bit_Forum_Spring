# BitForum AI Agent 化升级 —— 毕业设计项目计划书

> 本文是 M13-M18 六个 AI 模块的总体计划书，同时用于回应指导老师「项目工作量不够高、是否接入大模型或人工智能技术」的意见。
>
> 项目定位升级为：**基于 Spring Boot 与 React 的多智能体智能社区平台**。

## 一、立项背景与目标

### 1.1 指导老师意见的实质

指导老师的问题不是「你没有用 AI」，而是「这是一个 CRUD 论坛，技术深度不够」。传统论坛系统的能力集中在增删改查、权限校验、缓存优化、异步消息，这些技术本科课程都覆盖过，很难支撑一篇有分量的毕业论文。

### 1.2 本项目的回应方式

不是给论坛加一个聊天框，而是把论坛从「功能型系统」升级为「**多智能体协作的智能社区平台**」：

- 论坛既有的业务能力（文章、审核、举报、通知、看板、关注、收藏）被重新组织成 **Agent 可调用的工具集**；
- AI 不是外挂模块，而是**嵌入业务流程的协作者**，参与内容审核、问答检索、运营分析；
- 引入 **RAG 检索增强生成**、**Tool Calling 工具调用**、**多 Agent 编排**、**LLM 效果评估**四层技术纵深。

### 1.3 工作量量化对比

| 维度 | 升级前（M1-M12） | 升级后（+M13-M18） |
| --- | --- | --- |
| 业务模块 | 12 个 | **18 个** |
| 数据库表 | 12 张 | **19 张**（+7 张 AI 域） |
| Flyway 迁移 | V1-V12 | **V1-V19** |
| REST 接口 | 约 45 个 | **约 65 个**（+20 个 AI 接口） |
| 后端测试 | 176 项 | **目标 300+ 项** |
| 技术纵深 | 缓存 / 消息队列 / 权限 / 内容治理 | **+ LLM 工程 + Agent 编排 + RAG 向量检索 + 内容安全 + AI 效果评估** |
| 论文可写章节 | 系统设计 + CRUD 实现 | **+ AI 系统架构 + Agent 协作机制 + 提示工程 + 效果评估实验** |

### 1.4 核心目标

1. 完成四个协作 Agent 的设计与实现：问答助手、内容审核、运营分析、智能推荐。
2. 建立站内知识库与 RAG 检索链路，回答必须附带站内引用。
3. 让 AI 审核结果与人机协同流程打通，具备可量化的准确率评估。
4. 建立 Agent 执行轨迹可观测性与 Token 成本管控。
5. 产出可复现的 AI 效果评估报告，作为论文实验章节的数据来源。

---

## 二、接手状态（2026-09-18 实测）

| 项目 | 状态 |
| --- | --- |
| 仓库路径 | `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring` |
| 远端 | `https://github.com/JJJJIU9999/Bit_Forum_Spring.git`（公开仓库） |
| 接手时分支 | `main`，与 `origin/main` 完全同步（领先/落后均为 0） |
| 接手时工作区 | 已跟踪文件**零未提交改动**；仅有 30 个未跟踪的图表产物与执行记录 |
| 接手时提交 | `d6dd582 docs: finalize public project documentation` |
| 最新 Flyway 迁移 | `V12__add_notification_source_message.sql` |
| 后端规模 | 98 个 main Java 文件 / 33 个 test Java 文件 / 176 项测试通过 |
| 前端规模 | React 19 + react-router 7 + Vite 7 + vitest 4，纯 CSS |
| 开发分支决策 | 新建 `feat/ai-agent`，不逐模块 push |

### 2.1 既有文档的过期信息（本轮修正）

`docs/graduation/毕业设计文档总览.md` 与 `毕业设计进度.md` 记录的「当前分支 `feat/frontend-refactor`（基于 graduation-design）」「工作区包含 M1-M11 大量未提交改动」等描述**已经过期**：这些分支的成果早已合并进 `main`，当前 `main` 是干净且稳定的封版基线。本轮一并更正。

---

## 三、技术选型与验证结论

所有选型均经过实测验证，证据见 `findings.md`。

| 决策项 | 结论 | 理由 |
| --- | --- | --- |
| Spring AI 版本 | **1.1.8** | 官方文档明确 "Spring AI supports Spring Boot 3.4.x and 3.5.x"，与现有 Spring Boot 3.4.5 直接兼容 |
| 不用 Spring AI 2.0.1 | 排除 | 2.0.1 依赖 `spring-boot-starter-webclient` / `spring-boot-starter-restclient`，需要 Spring Boot 4.x；升级会波及 M1-M12 全部代码 |
| 不升级 Spring Boot | 保持 3.4.5 | 降低风险，把有限时间投在 AI 能力上 |
| 大模型 | DeepSeek（`spring-ai-starter-model-deepseek`） | 已有 API Key，支持 Function Calling，成本低 |
| 向量存储 | Redis Stack（`redis/redis-stack-server:7.4.0-v8`） | 现有 `redis:7-alpine` 缺 RediSearch 模块；换镜像后端口与协议不变，`RedisService` 零改动 |
| 嵌入模型 | 本地 ONNX（`spring-ai-starter-model-transformers`） | DeepSeek 不提供 embedding 接口；本地模型零 API 费用、离线可跑 |
| 编排方式 | 自研轻量 Supervisor | 不引入 LangChain4j / Spring AI Alibaba Graph；依赖少、可解释性强、答辩能讲清每行代码 |
| 会话记忆 | 自定义 `MysqlChatMemoryRepository` | Spring AI 默认内存仓储重启即丢、无法审计；实现 `ChatMemoryRepository` 接口后自动配置自动接管 |
| 降级模型 | 保留 Ollama starter 作为备选依赖 | 断网或额度耗尽时切换到本地模型，保证演示不中断 |

### 3.1 关键版本约束

- Spring AI 1.1.6 起，内置记忆 Advisor 的 `conversationId` 变为**必填**，缺失会直接抛 `IllegalArgumentException`；`ChatMemory.DEFAULT_CONVERSATION_ID` 常量已被移除。实现时必须显式传递会话 ID。
- Redis 向量库需要 `initialize-schema: true` 才会自动建索引；这是破坏性变更，早期版本默认开启。
- 使用元数据过滤时，必须在 `RedisVectorStore` 构造时**显式声明**所有可过滤字段及其类型（TAG / TEXT / NUMERIC）。
- RediSearch 返回的 KNN 距离是**十进制字符串**（如 `0.00362026691437`），不是二进制 float32。

### 3.2 Redis Stack 环境约束（已在 Phase 0.5 落地）

- 镜像：`redis/redis-stack-server:7.4.0-v8`，Redis 7.4.7 + RediSearch 2.10.20。
- 该镜像 `Entrypoint` 为 `null`、`Cmd` 是 `/entrypoint.sh`，由它执行全部 `--loadmodule`。
- **绝不能用 `command` 覆盖 entrypoint**，否则 RediSearch 等模块全部不加载，`FT.CREATE` 会报 unknown command。追加参数请用 `REDIS_ARGS` 环境变量。
- 数据卷使用独立的 `redis-stack-data`，避免与旧 `redis:7-alpine` 数据文件冲突。
- MySQL 宿主机端口已从 3307 改回 **3306**，与 `application.yml` 默认值一致，本机启动后端与测试无需再设 `SPRING_DATASOURCE_URL`。

详细验证记录见 `findings.md` 第五节。

---

## 四、系统架构

### 4.1 运行时架构

```text
                    ┌───────────────────────────────────────┐
   React 19 前端    │  AI 助手浮动面板 / AI 审核台 / AI 运营台   │
                    └──────────────────┬────────────────────┘
                                       │ /api/ai/**
                    ┌──────────────────▼────────────────────┐
                    │        AgentOrchestrator（编排层）       │
                    │  会话管理 · 意图路由 · 步数/超时防护       │
                    └────┬────────┬────────┬────────┬────────┘
                         │        │        │        │
                 ┌───────▼──┐ ┌───▼────┐ ┌─▼─────┐ ┌▼────────┐
                 │ QaAgent  │ │Moderation│ │Analyst│ │Recommend│
                 │ 问答助手  │ │ 审核 Agent│ │ 运营   │ │ 推荐     │
                 └────┬─────┘ └───┬────┘ └───┬───┘ └────┬────┘
                      │           │          │          │
        ┌─────────────▼───────────▼──────────▼──────────▼─────────┐
        │  共享设施层                                                │
        │  ToolRegistry(@Tool 工具集) · RagService(向量检索)          │
        │  MysqlChatMemoryRepository · PromptTemplate · TraceRecorder │
        │  TokenBudget · LlmStructuredOutput · AiDegradeGuard        │
        └───────────┬─────────────────┬────────────────┬───────────┘
                    │                 │                │
        ┌───────────▼──────┐  ┌───────▼──────┐  ┌──────▼─────────┐
        │  DeepSeek API     │  │ Redis Stack  │  │ MySQL 8         │
        │  (chat + tools)   │  │ (向量库+缓存) │  │ (AI 域 7 张表)   │
        └──────────────────┘  └──────────────┘  └────────────────┘
                    │
        ┌───────────▼──────────┐
        │  ONNX 本地嵌入模型     │  ← 离线、零成本
        └──────────────────────┘
```

### 4.2 四个 Agent 的职责划分

| Agent | 输入 | 能力 | 输出 | 论文价值 |
| --- | --- | --- | --- | --- |
| **QaAgent 问答助手** | 用户自然语言提问 | RAG 检索站内帖 + 工具调用（搜索、读详情、收藏、点赞、关注、评论、发帖） | 带站内引用链接的回答 | Agent 工具调用 + RAG |
| **ModerationAgent 审核 Agent** | 新文章 / 新评论 | 多维度风险评估（违规、广告、灌水、敏感）+ 结构化决策 | PASS / REJECT / 转人工 + 置信度 + 理由 | 人机协同审核，可做对照实验 |
| **AnalystAgent 运营分析** | 管理员的问题 + 看板数据 | 把 M6 Dashboard 统计包装为工具供 LLM 读取 | 自然语言运营洞察报告 | 数据 + LLM 结合 |
| **RecommendAgent 推荐** | 用户 ID 或当前问题 | 多路召回（向量相似 + 热度 + 关注关系） | 推荐列表 + 推荐理由 | 推荐算法 + 可解释性 |

### 4.3 包结构规划

```text
com.bitforum.ai
├── agent/          QaAgent / ModerationAgent / AnalystAgent / RecommendAgent / Agent 接口
├── orchestrator/   AgentOrchestrator / AgentRouter / AgentContext
├── tool/           10 个 @Tool 工具类 + ToolRegistry
├── rag/            ArticleChunkingService / KbIndexService / RagService
├── memory/         MysqlChatMemoryRepository
├── prompt/         PromptTemplateService / PromptCatalog
├── observe/        TraceRecorder / TokenBudgetGuard / AiDegradeGuard
├── eval/           AiEvaluationService（LLM-as-Judge）
├── dto/            请求与响应 DTO
├── entity/         AI 域实体
└── mapper/         AI 域 Mapper
```

---

## 五、关键设计决策

1. **编排方式**：自研轻量 Supervisor，用 Spring AI 原生 `ChatClient` + `@Tool` + `Advisor` 组合。不引入重型 Agent 框架。
2. **会话记忆**：实现 `MysqlChatMemoryRepository`，会话与消息落库，支持重启不丢与审计。必须显式传 `conversationId`。
3. **写操作安全**：Agent 的「读工具」（搜索、查详情、查统计）可直接执行；「写工具」（收藏、点赞、关注、评论、发帖）必须经**用户身份透传 + 二次确认**。绝不豁免 `LoginInterceptor` / `AdminInterceptor`。
4. **降级链路**：LLM 调用失败或超时 → 降级为规则版审核 / 关键词检索问答，响应标记 `degraded=true`。**论坛主流程永不因 AI 故障而阻塞**。
5. **成本控制**：单轮输入 Token 上限 + 单用户日配额（复用现有 Redis 计数能力）+ 会话窗口裁剪 + 长内容截断。
6. **异步化**：内容审核与知识库索引全部异步执行，不阻塞用户发帖与评论响应。
7. **工具粒度**：每个工具复用现有 Service，不重复编写 SQL，保证业务逻辑单一来源。

---

## 六、模块划分（M13-M18）

### M13 AI 基础设施与对话骨架

**目标**：打通 Spring AI 与 DeepSeek，建立可持久化的多轮对话能力。

- `pom.xml` 引入 `spring-ai-bom:1.1.8` 与四个 starter：deepseek、transformers、vector-store-redis、ollama。
- `docker-compose.yml` 把 `redis:7-alpine` 换成 `redis/redis-stack-server:7.4.0-v8`，保留 6379 端口。
- **Flyway V13**：`ai_conversation`、`ai_message`。
- `application.yml` 新增 `spring.ai.deepseek.*`，Key 走环境变量 `DEEPSEEK_API_KEY`；同步更新 `.env.example`。
- 实现 `MysqlChatMemoryRepository`、`AgentOrchestrator` 骨架、`Agent` 接口、`AiConversationService`。
- 接口：`POST /api/ai/conversations`、`GET /api/ai/conversations`、`POST /api/ai/conversations/{id}/messages`、`GET /api/ai/conversations/{id}/messages`。
- 前端：`AiAssistantPanel.jsx` 浮动面板 + `aiApi.js`，第一版同步响应 + 加载态。
- **验收**：能多轮对话、重启服务后历史不丢、176 项既有测试 + 新增测试全部通过。

### M14 工具集与 Tool Calling

**目标**：让 Agent 能真实操作论坛，而不只是聊天。

- `com.bitforum.ai.tool` 下 10 个工具类，全部使用 `@Tool(description = ...)`：
  `ArticleSearchTool`、`ArticleDetailTool`、`CategoryTool`、`HotArticleTool`、`FavoriteTool`、`LikeTool`、`FollowTool`、`CommentTool`、`ArticlePublishTool`、`RecommendTool`。
- `ToolRegistry`：按 Agent 装配不同工具子集（审核 Agent 绝不装配发帖工具）。
- `AgentRouter`：按用户消息与会话上下文选择 Agent。
- **验收**：`scripts/agent-tool-smoke.md` 记录 10 个工具的真实调用会话（提问 → 工具调用 → 结果）。

### M15 RAG 知识库与向量检索

**目标**：回答基于站内真实内容，并给出可点击引用。

- **Flyway V14**：`ai_kb_document`、`ai_kb_chunk`（向量本体存 Redis，MySQL 只存映射与状态）。
- `ArticleChunkingService`：按标题与段落切块，附 metadata（articleId、categoryId、作者、发布时间、状态）。
- `KbIndexService`：支持全量重建与增量更新（文章发布 / 更新 / 下架时触发）。
- **异步索引走 RabbitMQ**：复用现有 `RabbitMQConfig` 模式新增索引队列，复用现有 DLQ + 手动 ACK + Redis 幂等套路。
- `RagService`：`VectorStore.similaritySearch` + metadata filter，只检索 `PUBLISHED` 文章。
- `QuestionAnswerAdvisor` 接入 QaAgent。
- 接口：`POST /api/admin/ai/kb/rebuild`、`GET /api/admin/ai/kb/stats`。
- **验收**：知识库统计数与已发布文章数一致；提问能召回正确帖子且回答带引用链接。

### M16 内容审核 Agent（人机协同）

**目标**：AI 参与真实审核流程，并有可量化的人工反馈闭环。

- **Flyway V15**：`ai_moderation_record`（AI 判断 + 系统动作 + 五维分 + 耗时 + 模型版本 + 人工反馈）。
- `ModerationAgent`：五维 Prompt + 结构化输出映射到 Java record。
- 决策联动 M2 审核流（规则见下方「实施决策」）。
- 接入点：`ArticleService.submit()` 后、`CommentService.publish()` 后，均异步执行。
- 人工反馈闭环：审核台标记「AI 判断正确 / 错误」写入 `feedback` 字段。
- 接口：`GET /api/admin/ai/moderation/records`、`POST /api/admin/ai/moderation/records/{id}/feedback`、`POST /api/ai/moderation/precheck`。
- **验收**：100 条**合成**评测样本对照实验，在**独立测试集**上输出准确率 / 精确率 / 召回率 / 漏判率。

#### 实施决策（2026-09-19 与外部 AI 讨论后确定）

> 来源：T7 结构化输出验证（`findings.md` 6.12）暴露出「判据决定质量」的问题后，
> 由项目作者与外部 AI 讨论确定；完整讨论材料见 `m16-decision-brief.md`。

**三条最关键的约定**：

1. **Decision 与 Action 解耦**：`PASS / REVIEW / REJECT` 只是 **AI 的判断**，不等于系统动作。
   记录中分别保存 `decision`（AI 判断）与 `action`（系统实际执行的动作），两者独立演化。
2. **自动 PASS 但不自动 REJECT**：唯一允许的自动动作是「高置信 PASS 自动放行」（可配置开关，默认关闭）；
   **绝不自动驳回或删除任何内容** —— 误判代价不对称，错删用户内容远比漏放一条难挽回。
3. **评测集必须留独立测试集**：100 条合成样本划分开发集与测试集；
   阈值与提示词只在**开发集**上调整，最终指标只在**从未参与调参的测试集**上产出。

**具体规则**：

| 项 | 规则 |
| --- | --- |
| 三档判断 | `PASS` / `REVIEW` / `REJECT`，仅代表 AI 判断，不等于数据库动作 |
| 文章 | AI 只提供建议；默认人工确认；可配置「高置信 PASS 自动放行」；**绝不自动驳回** |
| 评论 | 保持发布即公开；AI 事后检测；`REVIEW` → 普通待处理记录，`REJECT` → 高优先级记录；不自动删除 |
| 边界内容 | **检测高召回、执行高精度**：明确违规判 `REJECT`，模糊内容判 `REVIEW` |
| 风险维度 | 五维：`harmful` / `promotion` / `fraud` / `spam` / `sensitive` |
| 综合风险分 | **不由模型输出**，由 Java 按确定性规则计算（保证可复现、可解释） |
| 评测样本 | 100 条**合成**数据并如实声明；自动放行阈值由实验结果确定，**不预设** 0.9 / 0.3 |

### M17 运营分析 Agent 与智能推荐

**目标**：让 AI 读懂运营数据，并为用户生成可解释推荐。

- **Flyway V16**：`ai_insight_report`。
- `AnalystAgent`：把 M6 `AdminDashboardService` 的全部统计包装为 `@Tool`，LLM 生成自然语言洞察与建议。
- **Flyway V17**：`ai_recommend_log`（用于评估与基线对比）。
- `RecommendAgent`：三路召回融合 —— 向量相似（同主题）+ 热度（M6 热榜 ZSet）+ 关注关系（M9），LLM 生成推荐理由。
- 前端：文章详情页「相关推荐」、AI 助手回答末尾推荐相关帖、管理员看板「AI 运营洞察」卡片。
- **验收**：给出 Top-10 推荐及理由；命中率与纯热榜基线对比。

#### 实施决策（2026-09-19 与外部 AI 讨论后确定）

> 来源：T9/T10/T11 三轮前置验证（`findings.md` 6.13-6.15）暴露出
> 「最大短板是数据而不是模型」之后，由项目作者与外部 AI 讨论确定；
> 完整讨论材料见 `m17-decision-brief.md`。

**三条最关键的约定**：

1. **推荐排序完全由 Java 完成**：三路召回 → 融合分数 → Top-N，**LLM 不参与选文**。
   模型只负责基于真实信号生成解释 —— 排序必须可复现，理由只是排序之上的"说明"。
2. **评测用合成离线数据 + 留一法（leave-one-out）**：以**隐藏的**收藏行为作为 Ground Truth。
   有个必须写清的细节：被隐藏的那一篇**不进入排除集**（等价于"用户还没收藏它"），
   否则「排除已收藏」的策略会让命中率恒为 0 —— 详见 `m17-eval-protocol.md` 第三节。
3. **本阶段明确不做「排除已点赞」**：点赞只有 `article:{id}:likes`（文章 → 用户集合）这一方向，
   没有反向索引，反查需要 SCAN 全库（证据见 `findings.md` 6.16）。
   补反向索引属于改动 M6 的数据模型，单独立项另议。

**具体规则**：

| 项 | 规则 |
| --- | --- |
| 排序 | 完全由 Java 决定：三路召回 → RRF 融合 → Top-N；**LLM 不参与选文** |
| 推荐理由 | 模型只生成"为什么推荐"的解释，且必须引用真实信号（召回来源、热点、关注关系） |
| 召回通道 | `vector`（M15 向量库找同主题）+ `hot`（M6 热榜 ZSet）+ `follow`（M9 关注关系） |
| 匿名访客 | **可以看到相关推荐**：走"内容相似 + 热度"两路，理由不含个人信息 |
| 登录用户 | 在匿名两路之上增加 `follow` 与个人偏好信号（收藏/作者去重） |
| 候选过滤 | 只排除**作者本人**与**已收藏**；浏览过的不排除；**已点赞本阶段不支持** |
| 降级路径 | AI 不可用时推荐列表**仍能正常工作**（只是没有理由，`degraded` 如实落库） |
| 评测指标 | `HitRate@10` 与 `MRR@10`；融合推荐 vs 纯热榜基线 |
| 评测数据 | 40-60 篇**有真实正文**的合成技术文章 + 合成用户行为，如实声明为合成数据 |
| 数据集划分 | 沿用 M16：开发集与独立测试集分离；测试集在口径冻结前不得查看、只跑一次 |
| 运营洞察 | **管理员主动触发 + 异步生成 + 结果落库**；不让 HTTP 请求同步等待；暂不做定时生成 |
| 洞察失败 | 失败也落库并保留统计快照；看板只展示最新一份**成功**报告 |

**投入优先级（作者判断）**：T11 已证明 M17 的短板是**数据**而不是模型，
因此时间优先投入「评测数据与 Ground Truth 设计」，而不是继续给推荐 Agent 增加"智能"。

#### 收尾决定（2026-09-19 与外部 AI 讨论后确定）

> 来源：M17 完成后的评审。完整过程见 `m17-eval-report.md`、`progress.md` 的 M17-12。

1. **M17 按冻结规范已达标，但只证明合成数据上的相对效果** ——
   答辩必须**主动强调**：测试集仅 9 位用户、数据是合成的、热榜基线偏弱、关注信号过强。
2. **向量区分度选"接受现状"**：实测 0.0147（目标 0.05）作为**已知局限**结束 M17。
   **不为了达到目标值重新改语料、换模型或改算法**。
3. **点赞反向索引不补**：写入后续优化，**不在 M18 之前动 M6 的数据模型**。
4. **评测数据答辩时保留**，但展示成明确的**"演示数据"**而不是伪装成正常帖子；
   保留 `M17EvalDataSeeder` 与一键清理能力。
5. **M18 范围收敛**：聚焦 **Trace + Usage + Degrade + 简单可视化**；
   Prompt 动态管理、LLM-as-Judge、完整 AI Settings **直接砍掉**；`TokenBudgetGuard` 时间充足再做。

> **执行纪律**：M17 的推荐数字已完成它的实验使命。
> **下一步最不该做的就是继续优化 0.8889** —— 价值最高的是把 AI 系统
> 从"功能很多"收束成"全过程可解释、可追踪、可降级"。

### M18 可观测性、评估与工程化闭环（收敛版）

**目标**：把 AI 系统从"功能很多"收束成**"全过程可解释、可追踪、可降级"**。

**要做（按优先级）**：

- **Flyway V19**：`ai_execution_trace`（路由 → 工具调用链 → 每步耗时 → Token）+ `TraceRecorder`
  —— 这是 M18 的核心，也是答辩演示的关键画面。
- **Flyway V18**：`ai_usage_stat`（按用户 / Agent / 天的 Token 与费用统计）。
  **只建这一张表，不含 `ai_prompt_template`。**
- `AiDegradeGuard`：LLM 超时 / 异常 / 额度的**统一**降级处理
  （把目前散在各 Agent 里的降级收敛到一处）。
- 前端「AI 执行轨迹」页 + 用量概览：**简单优先**，不做大而全的报表。

**砍掉（本轮明确不做）**：

- ❌ `ai_prompt_template`（Prompt 动态管理 / 版本表）
- ❌ LLM-as-Judge 评估脚本
- ❌ 管理员「AI 设置」页

**时间充足才做**：

- ⏳ `TokenBudgetGuard`（单轮输入上限、单用户日配额、超限友好提示）

**验收（相应调整）**：

1. 任意一次 AI 调用都能查到**完整执行轨迹**（路由、工具调用链、每步耗时、token）；
2. 用量可按 用户 / Agent / 天 聚合，且**能算出单次成本**；
3. AI 不可用时全站 AI 功能有**统一的降级表现**，且用户能看懂"为什么降级"；
4. 轨迹页可演示。

#### 实施决策（2026-09-19 与外部 AI 讨论后确定）

前置验证：T12（工具调用链可埋点）与 T13（轨迹覆盖异步链路）均已实测通过，见 `findings.md` 6.18 / 6.19；
材料与取舍见 `m18-decision-brief.md`。五个口径问题确定如下：

| # | 问题 | 决定 |
| --- | --- | --- |
| 1 | `ai_usage_stat` 的数据来源 | **新建统一埋点明细**（一行 = 一次 LLM 调用）：四个 Agent 走同一处埋点，并顺带补采审核与推荐理由的 token —— 现有表里这两个 Agent 根本没有 token 字段，只聚合既有表会让"按 Agent 统计"名不副实 |
| 2 | 轨迹表结构 | **单表 `ai_execution_trace` + `steps` JSON**：一次调用写一行、异步段回来 UPDATE 同一行；代价是不能按工具名直接做 SQL 统计（可接受） |
| 3 | `AiDegradeGuard` 范围 | **回头改造四个既有 Agent**，把各自的 try/catch 与降级文案收敛到 Guard，并用既有 381 项测试做回归 —— 否则验收第 3 条只能算部分达成 |
| 4 | 迁移编号 | **V18 = `ai_execution_trace`、V19 = `ai_usage_stat`**（轨迹是核心先占 V18，避免留空号；原表编号作废） |
| 5 | 轨迹与用量的可见性 | **只在管理端**（`/api/admin/ai/**`）：轨迹含用户 id 与提问原文，用量与费用属于运营信息 |
| 6 | 工具链采集方式 | 包装 `ToolCallback`（`TraceRecorder.wrapTools`）—— T12 证明最终响应里没有工具链，这是唯一能拿到单步耗时的位置 |
| 7 | 异步链路接法 | traceId 走 ThreadLocal（同线程）+ MQ 消息头（跨进程），异步段 `attach` 后 **UPDATE 同一行**；不修改任何消息体类 |
| 8 | 推荐链路是否记轨迹 | **记，但只在 `reason-enabled=true` 时**：离线评测会产生成千上万次推荐，那些记录没有解释价值 |

> **注意区分**：M17 决定的是"不优化 0.8889"；
> M18 对推荐链路做的是**把"为什么是这 10 篇"记下来并展示**（排序算法一行不改）。

---

## 七、数据库变更（Flyway V13-V19）

严格只新增迁移，**绝不修改 V1-V12**。表名延续现有小写下划线风格，主键 `BIGINT AUTO_INCREMENT`，字段带中文注释。

| 迁移 | 表 | 说明 |
| --- | --- | --- |
| V13 | `ai_conversation`、`ai_message` | 会话与消息，含 `tool_calls`、`prompt_tokens`、`completion_tokens` |
| V14 | `ai_kb_document`、`ai_kb_chunk` | 知识库文档与分块，含唯一键防重复索引 |
| V15 | `ai_moderation_record` | AI 审核记录，`feedback` 承载人工反馈 |
| V16 | `ai_insight_report` | 运营洞察报告 |
| V17 | `ai_recommend_log` | 推荐记录（评估用） |
| V18 | `ai_execution_trace` | Agent 执行轨迹（M18 核心；**编号修正**：轨迹先占 V18） |
| V19 | `ai_usage_stat` | 用量统计（**不含** `ai_prompt_template` —— M18 收敛时砍掉） |

---

## 八、前端变更

沿用现有 React 19 + react-router 7 + 纯 CSS + lucide-react 图标风格，**不引入 UI 组件库、不引入状态管理库**。

新增：

- `pages/AiAssistant.jsx` + 右侧浮动面板（全局挂载在 `MainLayout`）
- `components/AiMessageBubble.jsx`（含引用来源、工具调用折叠展示）
- `pages/admin/AiModeration.jsx`（AI 审核记录 + 人工反馈）
- `pages/admin/AiKnowledge.jsx`（知识库重建与统计）
- `pages/admin/AiSettings.jsx`（AI 开关与预算）
- `pages/admin/AiTraces.jsx`（执行轨迹可视化）
- `api/aiApi.js`、`api/adminAiApi.js`

修改：

- `App.jsx`：新增 `/ai`、`/admin/ai/*` 路由
- `AdminLayout.jsx`：新增 AI 相关页签
- `ArticleDetail.jsx`：新增「相关推荐」区
- `PublishArticle.jsx`：提交前调用 `precheck` 展示 AI 预审提示
- `Dashboard.jsx`：新增「AI 运营洞察」卡片

---

## 九、分支与交付策略

**决策：新建 `feat/ai-agent` 分支开发，不逐模块 push。**

理由：

1. `main` 与 `origin/main` 完全同步、已跟踪文件零未提交改动，是干净基线，正是拉分支的最佳时机。
2. 这是**公开仓库**，也是求职作品集；逐模块 push 会让公开仓库长期展示半成品。
3. 同一仓库在 Windows 机器与 MacBook 上都在使用，分支隔离能防止两台机器工作区互相污染。
4. 最后一次性合并回 `main`，Git 历史干净；`main` 在整个开发期保持「可运行、可演示、可回退」。

流程：

```bash
git checkout -b feat/ai-agent          # 从干净的 main 拉出（已完成）
# M13 → M18 期间：本地按模块提交，不 push
git push origin feat/ai-agent          # 检查点 1：中期检查前（M15 完成）
git push origin feat/ai-agent          # 检查点 2：2027 年 3 月系统冻结前
git checkout main && git merge --no-ff feat/ai-agent
```

分支内约束（沿用现有开发规范）：

- 每个模块独立提交，提交信息体现模块范围。
- 不 `git add .`，只暂存计划内文件。
- 不 reset / stash / revert / 覆盖已有工作。
- 每个模块完成即跑 `mvn test` + `npm run build`，红着不开下一个模块。
- 提交与 push 前分别向用户确认，不擅自发布。

---

## 十、范围边界

### 10.1 明确要做

- Spring AI + DeepSeek 接入，Key 走环境变量。
- 四个 Agent 与 10 个工具。
- RAG 知识库、向量检索、异步索引。
- AI 审核与人机协同闭环。
- 执行轨迹、Token 预算、降级链路。
- LLM-as-Judge 效果评估与成本评估。

### 10.2 明确不做

- 不升级 Spring Boot 到 4.x（保持 3.4.5 稳定）。
- 不引入 LangChain4j / Spring Cloud / 微服务 / Kafka。
- 不做模型微调（成本与收益不匹配，用提示工程 + RAG 代替）。
- 不做 MCP Server 对外暴露（列为未来工作）。
- 不引入 Redux / Zustand / UI 组件库。
- 不修改 V1-V12 历史迁移，不回滚 M1-M12。
- 不逐模块 push 到公开仓库。
- 不虚构性能、流量、生产效果或评估结果。

### 10.3 统一实现约束

必须保持：MyBatis-Plus、Flyway、Redis、RabbitMQ、JWT + Interceptor 权限方案、统一 `Result<T>` 返回结构、Jakarta Validation、简单 React / Vite 前端结构。

每个模块完成标准沿用现有规范：新增库表必须有新 Flyway 迁移、分层清晰、统一 `Result<T>`、参数校验、权限正确、不返回敏感字段、关键异常有明确提示、补后端测试、跑 `mvn test`、涉及前端跑 `npm run build`、更新 `docs/graduation/` 对应文档、输出修改文件清单与验证结果。

---

## 十一、测试与验证策略

| 层级 | 方案 | 覆盖内容 |
| --- | --- | --- |
| 单元测试 | Spring AI Mock 聊天模型 / Mockito 打桩 | 工具方法、Prompt 装配、Token 预算、降级逻辑、结构化输出解析 |
| 集成测试 | Mock 模型 + 真实 Redis Stack + MySQL | 会话持久化、向量写入与检索、异步索引队列 |
| 控制器测试 | MockMvc（沿用现有 17 个 Controller 测试写法） | 20 个新接口的权限、参数校验、错误码 |
| 前端测试 | vitest（沿用现有配置） | AI 面板渲染、消息流、错误态 |
| 效果评估 | LLM-as-Judge + 人工标注对照 | 问答准确率 / 引用正确率 / 幻觉率；审核精确率 / 召回率；推荐命中率 |
| 成本评估 | 从 `ai_usage_stat` 聚合 | 单轮 Token、单轮费用、月度预估 |

统一验证命令：

```bash
# 后端全量测试
SPRING_DATASOURCE_PASSWORD=... SPRING_RABBITMQ_PASSWORD=... JWT_SECRET=... mvn test

# 后端编译
mvn -DskipTests compile

# 前端构建与测试
cd frontend && npm run build && npm test

# 格式检查
git diff --check
```

---

## 十二、时间表（2026.10 → 2027.06）

| 阶段 | 时间 | 交付物 | 里程碑 |
| --- | --- | --- | --- |
| 准备期 | 10 月第 1-2 周 | 计划书定稿 + 开题材料修订 + DeepSeek Key 与 Redis Stack 环境跑通 + Spring AI 最小 Demo + 建 `feat/ai-agent` 分支 | 开题通过 |
| M13 | 10 月第 3-4 周 + 11 月第 1 周 | AI 基础设施 + 对话骨架 + 前端面板 | 能对话、能落库 |
| M14 | 11 月第 2-3 周 | 工具集 + Tool Calling + 意图路由 | 中期检查材料：Agent 能真实操作论坛 |
| M15 | 11 月第 4 周 + 12 月第 1-2 周 | RAG 知识库 + 向量检索 + 异步索引 | 带引用的站内问答 + push 检查点 1 |
| M16 | 12 月第 3-4 周 | 审核 Agent + 人机协同 + 反馈闭环 | AI 参与真实审核流程 |
| M17 | 2027 年 1 月 | 运营分析 Agent + 推荐 | 四 Agent 齐备 |
| M18 | 2 月 | 轨迹可视化 + 评估体系 + 预算管控 | 评估报告成文 |
| 集成期 | 3 月 | 全量回归 + 性能优化 + 演示脚本 | 系统冻结 + push 检查点 2 |
| 论文与答辩 | 4-6 月 | 论文 + PPT + 演示视频 + 合并回 main | 答辩 |

---

## 十三、答辩演示脚本

1. **第一幕 · 会查**：提问「上周有哪些关于 Redis 缓存穿透的讨论？」→ AI 调搜索工具 → 带引用链接回答。
2. **第二幕 · 会做**：说「把第一篇收藏了，顺便关注作者」→ AI 调收藏 + 关注工具 → 二次确认 → 数据库真的变了（当场刷新页面验证）。
3. **第三幕 · 会审**：发一条带广告的文章 → AI 自动判定并给出理由 → 管理员审核台看到 AI 建议与置信度 → 标记反馈。
4. **第四幕 · 会分析 + 透明**：问「最近一周社区运营有什么问题？」→ 运营 Agent 读看板数据出报告 → 打开执行轨迹页，逐帧展示 Agent 的路由、工具调用链、每步耗时与 Token。

---

## 十四、风险与对策

| 风险 | 对策 |
| --- | --- |
| LLM 调用失败 / 超时导致论坛卡死 | 全部 AI 调用异步化 + 超时熔断 + 降级为规则版，论坛主流程永不依赖 AI |
| API 费用超预算 | Token 预算 + 日配额 + 内容截断 + ONNX 本地嵌入零成本；单轮成本写入 `ai_usage_stat` 可审计 |
| Redis 升级破坏 M11 指标同步 | 只换镜像不换端口与协议；升级后立即跑 M11 相关测试回归 |
| Spring AI 与 Spring Boot 3.4.5 冲突 | 已实测确认 1.1.8 官方支持 3.4.x；回退路径是改用 `spring-ai-starter-model-openai` 手动指向 DeepSeek 的 OpenAI 兼容端点 |
| 向量检索效果差 | 保留 MySQL 关键词检索兜底 + 做混合检索对比实验（本身即论文素材） |
| Prompt 注入 / 越权操作 | 工具白名单 + 写操作二次确认 + 身份透传现有拦截器 + 系统提示隔离用户输入 |
| 六个月中途失控 | 每个模块独立可交付、可回退；M13-M15 是必做核心，M17 可裁剪 |
| Windows / MacBook 双机冲突 | 分支隔离 + 每模块完成即提交，不跨机留未提交改动 |

---

## 十五、阶段计划

### Phase 0：计划书落盘（本轮）

- [x] 完成既有项目与 Spring AI 生态勘察
- [x] 确认分支策略与时间预算
- [x] 从干净 main 创建 `feat/ai-agent` 分支
- [x] 创建 `docs/graduation/ai-agent-upgrade/` 三份文档
- [x] 更新毕业设计总览与进度文档

### Phase 0.5：前置环境搭建（已完成）

- [x] 确认中间件由本项目 `docker-compose.yml` 管理
- [x] Redis 镜像替换为 `redis/redis-stack-server:7.4.0-v8`，使用独立数据卷
- [x] 修复 `command` 覆盖 entrypoint 导致模块未加载的问题（改用 `REDIS_ARGS`）
- [x] 验证 RediSearch 2.10.20 已加载
- [x] 向量检索端到端验证（4 维精确性 + 768 维真实维度 + metadata 过滤）
- [x] 验证现有 `RedisService` 的 String / Set / ZSet 用法零影响
- [x] 配置 `DEEPSEEK_API_KEY`（`.env.example` 模板 + `.env` 实际值 + compose 透传）
- [x] 拉起 MySQL / Redis Stack / RabbitMQ 三容器并确认 healthy
- [x] 验证 MySQL 库与 Flyway V1-V12 状态
- [x] 用户已填入真实 Key
- [x] MySQL 宿主机端口改回 3306

### Phase 1：M13 AI 基础设施与对话骨架

- [x] 引入 Spring AI 依赖（仅 DeepSeek starter；transformers 与 vector-store-redis 延后至 M15）
- [x] 跑待验证事项 T1（Spring AI 1.1.8 + Boot 3.4.5 编译与启动）—— 通过
- [x] 跑待验证事项 T2（M11 回归）—— 211 项测试通过
- [x] 修复引入依赖后暴露的 M11 既有测试脆弱性
- [x] 新增 Flyway V13 与 AI 实体 / Mapper / Service
- [x] 实现 `MysqlChatMemoryRepository` 与编排骨架
- [x] 新增 4 个对话接口与前端面板
- [x] 补测试并跑通全量回归（211 项：210 通过 + 1 条件跳过）
- [x] 真实 DeepSeek 调用验证（`DeepSeekSmokeTest`，条件执行）

**M13 已完成**。实施中发现并记录的四个坑见 `findings.md` 6.4：
`@MapperScan` 不扫子包、`@ConditionalOnBean` 在用户配置类不可靠、
测试配置硬编码 api-key 覆盖环境变量、测试类内 bean 状态跨方法污染。

### Phase 2：M14 工具集与 Tool Calling

- [x] 实现工具类（实际按能力聚合为 `ArticleTools` 5 个方法 + `UserInteractionTools` 7 个方法，共 12 个工具）
- [x] 实现 `ToolRegistry`（按 Agent 装配工具子集，审核/运营类型当前不装配）
- [x] 修正设计缺陷：工具的用户身份改由 `ToolContext` 注入，不作为模型参数
- [x] 真实链路验证：AI 能调用搜索工具返回真实文章、能执行点赞写操作
- [ ] 记录工具调用冒烟会话到 `scripts/agent-tool-smoke.md`（真实会话已验证，待整理成文档）

**M14 已完成**。关键设计问题与验证方法见 `findings.md` 6.6（身份注入）与 6.7（工具成本）。

### Phase 3：M15 RAG 知识库与向量检索

- [ ] 新增 Flyway V14 与分块 / 索引服务
- [ ] 接入 RabbitMQ 异步索引队列
- [ ] 实现 `RagService` 与引用回答
- [ ] 知识库统计接口与管理页

### Phase 4：M16 内容审核 Agent

- [ ] 新增 Flyway V15 与审核 Agent
- [ ] 联动 M2 审核流与人工反馈闭环
- [ ] 100 条样本对照实验

### Phase 5：M17 运营分析与推荐

- [x] 新增 Flyway V16 / V17
- [x] 实现 AnalystAgent（看板统计工具 + 洞察生成 + 降级链路）
- [x] 推荐链路：三路召回 + RRF 融合 + 编排 + 纯热榜基线（排序全部由 Java 完成）
- [x] 推荐命中率对比实验（合成数据 + 留一法；规范 `m17-eval-protocol.md`，报告 `m17-eval-report.md`）
- [x] 运营洞察的触发（管理员主动触发 + 异步生成）与落库接入
- [x] 推荐接口与前端（文章详情页相关推荐 / 看板 AI 洞察卡片 / AI 助手相关帖）
- [x] 推荐理由生成（LLM 只写解释，不参与选文）

### Phase 6：M18 可观测性与工程化闭环（收敛版）

- [ ] 新增 Flyway V19 `ai_execution_trace` + `TraceRecorder`（核心）
- [ ] 新增 Flyway V18 `ai_usage_stat`（不含 `ai_prompt_template`）
- [ ] `AiDegradeGuard`：统一降级
- [ ] 前端「AI 执行轨迹」页 + 用量概览（简单优先）
- [ ] （时间充足再做）`TokenBudgetGuard`
- [x] ~~LLM-as-Judge 评估脚本~~ / ~~Prompt 动态管理~~ / ~~AI 设置页~~（**本轮砍掉**）

### Phase 7：集成与答辩

- [ ] 全量回归与性能优化
- [ ] 演示脚本与答辩材料
- [ ] 合并回 main

---

## 十六、错误记录

| 问题 | 处理 |
| --- | --- |
| 暂无 | 待记录 |

---

## 十七、需要用户确认的假设

1. 计划书主文件落在 `bit-forum-spring/docs/graduation/ai-agent-upgrade/`（与现有模块文档结构一致）。
2. DeepSeek Key 放入项目 `.env`（已在 `.gitignore` 中），只通过环境变量 `DEEPSEEK_API_KEY` 读取，绝不提交。
3. 不新建仓库、不修改 remote 地址。
4. 第一版 AI 对话用同步响应（非流式），流式 SSE 作为 M18 可选增强 —— 现有 axios 响应拦截器不支持流式，改造面较大。
