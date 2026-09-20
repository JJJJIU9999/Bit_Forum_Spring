# Bit Forum 项目完整历史总结

## 2026-08-24 公开仓库整改前基线收口更新

- Docker/Nginx、五服务 Compose、上传卷和 README 已由本地提交 `0c89922 feat(deploy): add Nginx frontend to Docker Compose` 收口。
- 文章详情间距修复已由本地提交 `d9d4f48 fix(frontend): correct article detail spacing` 单独收口。
- 本轮重新验证：Maven 176 个测试全部通过；Vitest 3 个文件、4 个测试全部通过；Vite 生产构建和 `docker compose config --quiet` 通过；可执行 JAR 能启动并在 8080 返回 Actuator 健康响应。
- RabbitMQ 5672 在本轮未启动，因此应用健康聚合状态为 `DOWN` 且测试有 AMQP 重连日志，但测试结果为 0 failure、0 error。
- Docker daemon 本轮未运行，五容器、首页、深层路由和代理的实时 E2E 未重新执行；2026-07-20 的历史记录仍保留在 `docs/graduation/docker-frontend/`。
- 个人学习资料不再作为项目主文档保留在当前公开树；历史内容仍可通过 Git 提交记录恢复。

下文保留 2026-08-02 核查快照，便于追溯当时的代码与工作区状态。其中“Docker 前端化尚未提交”“当前下一步先提交 Docker”等说法已被本节覆盖，不再代表 2026-08-24 的当前状态。

## 2026-09-19 M13-M18 AI Agent 升级完成（最新更新）

- **M13-M18 六个 AI 模块已全部完成**，在 `feat/ai-agent` 分支上按模块提交，**已 push 到 `origin/feat/ai-agent`**（最新 `514f301`，CI success（**限 `514f301` 已提交内容**；本轮收口改动未提交、未过远端 CI），相对 `main` 领先 41 个提交）；是否合并回 `main` 待系统冻结时决定。（截至 2026-09-20 快照；实时值以导出页眉或 `git` 命令为准）
- 当前 HEAD：`514f301 docs(graduation): refresh status summary with push and CI results (M18)`。
- 该次核实时工作区干净（此后收口工作产生了未提交改动，见 `closure-worklog.md`）；Flyway 已到 **V19**；向量索引 `bitforum-kb` 已重建（114 个片段）。
- 最近验证（2026-09-19）：**后端 `mvn test` 416 项（406 通过 + 10 条件跳过），前端 `npm test` 36 项**，`npm run build` 通过；
  真实环境端到端验证：一次提问产生 5 步执行轨迹（含工具调用）、用量 7085 tokens 且成本可估算。
- 技术栈新增：**Spring AI 1.1.8 + DeepSeek + 本地 ONNX 嵌入（bge-base-zh-v1.5）+ Redis Stack（RediSearch 向量检索）**。
- 因此本文后面几处已过期的表述（第 2 节 Git 状态、第 3 节技术栈、第 7 节 Flyway 版本、第 10 节测试规模、
  第 12/13 节风险与下一步、第 14 节“不能说已使用 Spring AI”）**均以本节与第 16 节为准**。
- 完整实施记录、技术证据与决策材料见 `docs/graduation/ai-agent-upgrade/`（`task_plan.md` / `findings.md` / `progress.md` 及各模块决策简报与评测报告）。

> 核查日期：2026-08-02
> 项目位置：仓库根目录
> 核查依据：当前 Git 工作区、Java/React 源码、Flyway 迁移、自动化测试、Docker 配置和 `docs/graduation` 模块记录。
> 使用说明：请用本文替换旧的 Bit Forum 记忆摘要。本文明确区分“已提交”“当前工作区已实现但未提交”“仍待完成”，不要把待办或风险写成已实现能力。

## 1. 项目定位

Bit Forum 是林坚浩持续维护的个人毕业设计项目，定位为一个后端能力较完整、带 React 管理与用户端界面的社区交流平台。项目最早聚焦注册登录、文章、评论、Redis 热榜和 RabbitMQ 事件，随后按 M1-M12 扩展为包含内容生产、审核发布、互动通知、举报治理、用户关系、运营后台、文件上传、接口文档、指标落库和健康检查的完整论坛系统。

项目覆盖以下 Java 后端工程能力：

- Spring Boot 分层开发、统一响应和全局异常处理。
- MyBatis-Plus 数据访问、分页和事务边界。
- JWT/BCrypt 认证、普通用户与管理员权限隔离。
- Redis String/Set/ZSet 数据结构及最终一致性同步。
- RabbitMQ Confirm/Returns、手动 ACK、DLX/DLQ 和幂等消费。
- Flyway 版本化数据库迁移。
- OpenAPI/Swagger 接口说明与 Actuator 可观测性。
- React 前后端联调、路由、响应式布局和 Docker Compose 部署。

项目不是高并发生产系统，也没有真实线上流量、性能指标或商业运营数据。描述项目时应强调“毕业设计与工程实践”，不要虚构生产规模。

## 2. 当前 Git 与交付状态

> ⚠️ 本节为 **2026-08-02 历史快照**，已被文首「2026-09-19 M13-M18 AI Agent 升级完成」块覆盖：
> 当前分支是 `feat/ai-agent`（不是 `feat/frontend-refactor`），HEAD 为 `514f301`，
> 且下面 2.3 列的“尚未提交内容”早已提交。保留本节仅供追溯。

### 2.1 当前分支

- 当前分支：`feat/frontend-refactor`。
- 当前 HEAD：`bfbfcce feat(frontend): complete community UI refactor`。
- 当前分支已跟踪 `origin/feat/frontend-refactor`，该提交已经推送到远端。
- `graduation-design` 当前位于 `d4d4503 docs(graduation): summarize M7 through M12 progress`。

### 2.2 已提交的重要阶段

- `871f16f`：完成 M1-M6 与一轮后端审计修复。
- `31c5cfa`：完成用户资料、关注、文件上传和 OpenAPI 等 M7-M10 能力。
- `c6ceee9`：完成 M11 Redis 文章指标同步落库。
- `844ada0`：完成 M12 Actuator 与管理员聚合健康检查。
- `d4d4503`：汇总 M7-M12 毕业设计进度文档。
- `bfbfcce`：完成 React 社区端与管理端整体重构。

### 2.3 当前尚未提交的工作区内容

以下能力已经在当前工作区实现并验证，但不属于 `bfbfcce` 提交：

- React 前端 Docker 镜像：Node 22 构建、Nginx 1.27 运行。
- Nginx 对 `/api`、`/uploads` 的同源反向代理和 React Router 回退。
- Compose 增加 `frontend` 服务以及上传文件命名卷。
- MySQL 宿主机映射从 `3306` 调整为 `3307`。
- 文章详情页标题、正文内边距的少量 CSS 修正。
- 本次项目总结文档和 Docker 实施记录。

个人学习文档不属于项目交付范围，当前公开树只保留毕业设计、部署和工程说明。

## 3. 技术栈与版本

### 3.1 后端

| 类别 | 当前技术 |
| --- | --- |
| 语言与运行目标 | Java 17 |
| 核心框架 | Spring Boot 3.4.5 |
| 构建工具 | Maven |
| Web | Spring MVC |
| ORM | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8 |
| 迁移 | Flyway Core + Flyway MySQL |
| 缓存 | Spring Data Redis / **Redis Stack**（`redis/redis-stack-server:7.4.0-v8`，含 RediSearch 向量检索；M15 起） |
| 消息队列 | Spring AMQP / RabbitMQ 3 |
| 认证 | JJWT 0.12.6 + BCrypt |
| 参数校验 | Jakarta Validation |
| 接口文档 | springdoc-openapi 2.8.9 |
| 健康检查 | Spring Boot Actuator |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、Mockito |
| **大模型框架（M13 起）** | **Spring AI 1.1.8**（`spring-ai-starter-model-deepseek`） |
| **大模型服务（M13 起）** | **DeepSeek**（Function Calling；Key 只走环境变量） |
| **嵌入模型（M15 起）** | **本地 ONNX Transformers**（`spring-ai-starter-model-transformers`，bge-base-zh-v1.5 / 768 维，离线可跑、零费用） |
| **向量存储（M15 起）** | **Redis Stack**（`redis/redis-stack-server:7.4.0-v8`，RediSearch 向量索引 + 元数据过滤） |
| **前端 AI 交互（M13 起）** | 全局 AI 助手面板、相关推荐、看板 AI 洞察卡片、管理端知识库/审核/执行轨迹页 |

### 3.2 前端

| 类别 | 当前技术 |
| --- | --- |
| UI | React 19 |
| 构建 | Vite 7 |
| 路由 | React Router 7 |
| HTTP | Axios |
| 图标 | Lucide React |
| 测试 | Vitest、Testing Library、jsdom |
| 样式 | 原生 CSS、CSS Variables、响应式 Media Query |

前端没有使用 Redux/Zustand、TailwindCSS、Ant Design、Element、图表库、SSR 或远程字体。不要把这些技术写进已实现栈。

### 3.3 部署

- Docker、Docker Compose。
- 后端 Dockerfile：Maven 3.9.9 + Java 17 多阶段构建，Java 17 JRE 运行。
- 前端 Dockerfile：Node 22 构建，Nginx 1.27 托管静态资源。
- Compose 编排 MySQL、Redis、RabbitMQ、Spring Boot、React/Nginx 五个服务。

## 4. 后端架构概况

当前主代码共 **181 个 Java 文件**（其中 `com.bitforum.ai` 下 75 个，M13-M18 新增），
测试代码 85 个文件。主要目录如下（下文中“98 个 Java 文件”为 M12 时期的历史数字）：

```text
src/main/java/com/bitforum/
├── ai/            AI 域（M13-M18）：agent/ orchestrator/ tool/ rag/ moderation/ analyst/ recommend/ trace/ usage/ degrade/ config/
├── common/        统一 Result、热门文章返回对象
├── config/        MVC、JWT、MyBatis-Plus、RabbitMQ、OpenAPI 配置
├── controller/    用户端与管理端控制器（含 5 个 AI 管理端控制器）
├── dto/           请求与响应 DTO
├── entity/        数据库实体（M1-M12 的 9 组 + AI 域的 9 张表）
├── exception/     全局异常处理
├── interceptor/   登录与管理员拦截器
├── job/           Redis 指标同步定时任务
├── mapper/        MyBatis-Plus Mapper 与自定义查询
├── message/       RabbitMQ 消息对象
├── service/       业务/基础设施服务
└── util/          JWT 工具
```

应用统一返回 `Result<T>`，Controller 负责 HTTP 参数与响应包装，Service 承担业务校验、状态流转和事务，Mapper 负责数据库访问。系统使用显式 MVC 拦截路径保护写接口和个人接口，管理员路径由独立拦截器统一保护。AI 域同样遵守这套分层，并额外要求“AI 调用必须有降级路径、必须有执行轨迹”（见第 16 节）。

## 5. 已实现功能全景

### 5.1 用户、认证与权限

- 用户注册和登录。
- BCrypt 密码哈希，数据库不保存明文密码。
- 登录成功返回 JWT、用户 ID、用户名和角色。
- JWT 通过 `Authorization: Bearer <token>` 传递。
- 登录拦截器解析 Token 后重新查询数据库，检查用户是否存在、账号是否启用。
- 管理员拦截器对 `/api/admin/**` 再检查 `ADMIN` 角色和账号状态，不只信任 Token 中的旧状态。
- 注册响应使用 `UserResponse`，不直接暴露用户实体或密码哈希。
- 用户禁用后无法继续访问登录保护接口。
- 管理员不能禁用当前登录的自己。

### 5.2 板块分类（M1）

- 公共端查询启用板块。
- 管理员分页查询板块。
- 板块创建、修改、启用、禁用和删除。
- 板块名称唯一。
- 文章发布、草稿保存和修改时校验板块存在且启用。
- 删除仍被文章引用的板块时有业务限制。

### 5.3 文章草稿、审核与生命周期（M2）

文章状态包括：

- `DRAFT`：草稿。
- `PENDING`：待审核。
- `PUBLISHED`：已发布。
- `REJECTED`：审核驳回。
- `OFFLINE`：已下架。

已实现：

- 直接投稿进入待审核。
- 保存草稿、修改草稿或驳回文章、再次提交审核。
- 作者查看自己的全状态文章并按状态筛选。
- 管理员查看审核列表、通过、驳回、下架和删除文章。
- 审核记录保存审核人、结果、原因和时间。
- 公共列表、详情、用户公开文章只展示 `PUBLISHED` 内容。
- 作者只能修改自己的草稿或驳回文章，不能直接修改待审核、已发布或下架内容。
- 作者删除与管理员删除会清理关联评论、收藏和 Redis 文章数据。

### 5.4 文章发现、浏览、点赞、热榜、收藏与搜索（M3 及基础能力）

- 最新文章列表和分页。
- 按板块筛选。
- 使用 MySQL `LIKE` 搜索标题和正文。
- 文章详情与浏览量累加。
- Redis Set 实现用户点赞去重，支持取消点赞。
- 禁止用户给自己的文章点赞。
- Redis ZSet 维护热度排行；浏览加 1 分、点赞加 3 分。
- 收藏与取消收藏。
- 数据库唯一键防止同一用户重复收藏同一文章。
- 我的收藏分页只返回仍处于公开状态的文章。

### 5.5 评论与通知中心（M4）

- 登录用户发表评论，评论前校验目标文章存在且已发布。
- 按文章查询评论。
- 管理员分页查询和删除评论。
- 通知中心支持分页、未读数、单条已读和全部已读。
- 已实现通知类型：评论、点赞、收藏、审核通过、审核驳回、文章下架。
- 对自己文章的自发行为会避免生成无意义的自通知。
- 前端导航展示未读通知徽标。

通知中心的业务通知主要通过 `NotificationService` 写入 MySQL；它与 RabbitMQ 的文章发布事件是两条不同链路，不能混为一谈。

### 5.6 举报与内容治理（M5）

- 用户举报文章和评论。
- 用户查看自己的举报及处理状态。
- 管理员按状态分页查看举报。
- 管理员处理或驳回举报并记录处理人、处理结果和时间。
- 举报状态包括 `PENDING`、`RESOLVED`、`REJECTED`。
- 数据库使用生成列和唯一键，阻止同一用户对同一目标重复提交未处理举报，避免只靠先查后插造成并发重复。
- 举报处理结果暂不额外生成通知，用户通过“我的举报”查看状态。

### 5.7 管理员数据看板（M6）

- 用户总数、正常/禁用用户等用户统计。
- 文章总量、各审核状态文章统计。
- 评论、收藏、通知和举报统计。
- 板块统计。
- 热门文章摘要。
- 前端管理看板集中展示运营数据。

### 5.8 用户资料与公开主页（M7）

- 当前用户读取和更新头像、昵称、个人简介。
- 用户公开主页。
- 公开主页展示已发布文章、文章数、收藏数和关注数据。
- 私有资料与公共资料使用不同响应 DTO，避免无边界暴露用户实体。

### 5.9 OpenAPI（M8）

- 配置 Swagger UI 和 OpenAPI JSON。
- 18 个控制器均有 `@Tag`。
- 当前识别到 63 个 `@Operation` 和 27 个 `@SecurityRequirement`。
- OpenAPI 配置声明 Bearer JWT 认证方式。
- 存在 Swagger UI/OpenAPI JSON 可访问性自动化测试。

### 5.10 关注关系（M9）

- 关注和取消关注。
- 禁止关注自己。
- 数据库唯一键防止重复关注。
- 粉丝列表、关注列表及分页。
- 关注数、粉丝数以及当前用户是否已关注。
- 公开用户主页集成关注操作和关系列表。

### 5.11 文件上传（M10）

- 头像上传：最大 2MB。
- 文章封面上传：最大 5MB。
- 允许 JPEG、PNG、WebP。
- UUID 生成服务端文件名，原始文件名仅清理后返回。
- 保存路径规范化并检查目标必须位于上传根目录内，降低路径穿越风险。
- `/uploads/**` 映射为静态资源。
- 文章实体和数据库支持 `cover_url`。
- 前端支持头像选择上传、投稿封面上传以及草稿/驳回文章封面修改。

### 5.12 Redis 指标落库（M11）

- Redis String 保存文章浏览量。
- Redis Set 保存点赞用户并计算点赞数。
- Redis ZSet 保存文章热度。
- 定时任务默认每 5 分钟扫描已发布文章。
- Redis 中存在浏览/点赞指标时，将其同步到 MySQL `article.view_count` 和 `like_count`。
- 单篇同步失败会记录日志并继续处理其他文章。
- Long 转 int 时处理负数和溢出边界。

该机制是周期性最终一致，不是强一致计数，也不应描述成绝对不丢数据。

### 5.13 Actuator 与管理员健康检查（M12）

- 引入 Spring Boot Actuator。
- 公开仅暴露 `health` 和 `info`。
- `/actuator/health` 不展示内部组件详细信息。
- 管理员专用 `/api/admin/health` 聚合检查应用、MySQL、Redis 和 RabbitMQ。
- 健康响应包含各组件状态、整体状态和检查时间。
- 管理员健康接口由后端管理员拦截器保护。

### 5.14 AI Agent 能力（M13-M18，2026-09-19 完成）

论坛在 M13-M18 增加了四个协作 Agent 与配套能力：问答助手（RAG + 12 个工具调用 + 引用回答）、
内容审核（五维风险评估 + 人机协同反馈）、运营分析（异步洞察报告）、推荐理由（为 Java 定序的 Top-N 写解释）；
并补齐了可观测性（执行轨迹、用量与成本、统一降级）与前端入口（全局 AI 助手面板、相关推荐、看板洞察卡片、
管理端知识库/AI 审核/执行轨迹三页）。

**完整内容、验证数据与诚实边界见本文第 16 节**；实施细节见 `docs/graduation/ai-agent-upgrade/`。

## 6. RabbitMQ 真实实现与边界

文章审核通过后，`ArticleService` 发送结构化 `ArticlePublishMessage`，消息包含 `messageId`、文章 ID、用户 ID、标题和发布时间。

当前可靠性机制包括：

- Direct Exchange 与明确 routing key。
- 持久化普通队列。
- Producer Confirm 记录是否到达 Broker。
- Returns + mandatory 记录是否成功路由。
- JSON 消息转换。
- 消费者手动 ACK。
- 失败时 `basicNack(requeue=false)`。
- DLX/DLQ 接收失败消息。
- Redis Set 根据 `messageId` 做重复消费判断。

必须保留的边界：

- 当前消费者业务主体仍是日志和模拟耗时处理，没有真正实现积分、站外推送或自动补偿。
- Confirm/Returns 失败当前主要记日志，没有 Outbox、发送失败表或自动重试调度。
- DLQ 监听器当前主要记录错误日志，没有管理页面或一键重放。
- 幂等标记在实际业务处理之前写入 Redis；如果后续处理失败，死信重放可能被判为重复并跳过。
- 因此只能描述为“实现 RabbitMQ 可靠性基础机制”，不能宣称 exactly-once、零丢失或完整生产级消息闭环。

## 7. 数据库与 Flyway

> ⚠️ 本节版本信息为历史快照。**当前实际为 V1-V19**：M1-M12 到 V12，M13-M18 新增 V13-V19（见下表与第 16 节）。

当前 `src/main/resources/db/migration` 有 V1-V11 共 11 个 SQL 文件，数据库 schema 当前版本为 11。

| 版本 | 内容 |
| --- | --- |
| V1 | 用户、文章、评论基础表 |
| V2 | 用户角色和账号状态 |
| V3 | 板块表及文章板块字段 |
| V4 | 文章状态、审核记录 |
| V5 | 文章收藏表 |
| V6 | 通知表 |
| V7 | 内容举报表 |
| V8 | 待处理举报并发唯一约束 |
| V9 | 用户昵称和个人简介 |
| V10 | 用户关注表 |
| V11 | 文章封面字段 |
| V12 | 通知来源消息字段（M1-M12 阶段最后一条） |
| **V13** | **`ai_conversation` / `ai_message`（M13：会话与消息，含 tool_calls、token、latency）** |
| **V14** | **`ai_kb_document` / `ai_kb_chunk`（M15：知识库文档与分块）** |
| **V15** | **`ai_moderation_record`（M16：AI 审核记录，decision 与 action 解耦 + 人工反馈）** |
| **V16** | **`ai_insight_report`（M17：运营洞察，正文与数据快照分开保存）** |
| **V17** | **`ai_recommend_log`（M17：推荐记录，含实验批次供基线对比）** |
| **V18** | **`ai_execution_trace`（M18：执行轨迹，单表 + steps JSON）** |
| **V19** | **`ai_usage_stat`（M18：用量明细，按 用户 / Agent / 天 聚合与成本估算）** |

主要实体/表共 9 组（M1-M12）：

- `user_info`
- `article`
- `comment`
- `category`
- `article_audit_record`
- `article_favorite`
- `notification`
- `content_report`
- `user_follow`

M13-M18 新增 9 张 AI 表（同样**只加索引、不建物理外键**，与既有约定一致）：
`ai_conversation`、`ai_message`、`ai_kb_document`、`ai_kb_chunk`、`ai_moderation_record`、
`ai_insight_report`、`ai_recommend_log`、`ai_execution_trace`、`ai_usage_stat`。
AI 模块严格遵守“只新增迁移、绝不修改历史迁移”的约束。

迁移脚本主要使用唯一键和查询索引，没有定义数据库外键。关系完整性主要由 Service 业务校验和级联清理维护，这是当前架构取舍，同时也是后续审计应持续关注的风险。

## 8. React 前端现状

### 8.1 路由

公共路由：

- `/`：社区首页。
- `/articles/:articleId`：文章详情。
- `/users/:userId`：公开用户主页。
- `/login`、`/register`。
- 未知地址显示前端 404。

登录保护路由：

- `/publish`：投稿/草稿。
- `/notifications`：通知中心。
- `/me/profile`：个人资料。
- `/me/articles`：我的文章。
- `/me/favorites`：我的收藏。
- `/me/reports`：我的举报。

管理员路由：

- `/admin`：数据看板和健康状态。
- `/admin/audit`：文章审核。
- `/admin/articles`：文章管理。
- `/admin/comments`：评论管理。
- `/admin/reports`：举报处理。
- `/admin/users`：用户管理。
- `/admin/categories`：板块管理。
- `/admin/kb`：**AI 知识库（M15：索引统计与全量重建）**。
- `/admin/moderation`：**AI 审核记录与人工反馈（M16）**。
- `/admin/ai-traces`：**AI 执行轨迹与用量概览（M18）**。

M13-M18 另外把 AI 助手做成了**全局浮动面板**（登录后任意页面可用），并在文章详情页加了「相关推荐」、
在管理看板加了「AI 运营洞察」卡片（见第 16 节）。

游客访问保护页面时会携带 `redirectTo` 跳转登录，登录成功后返回原页面。URL 能表达搜索、分页和标签状态，支持刷新、深链、前进与后退。

### 8.2 页面功能

- 首页：文章分页、板块筛选、标题/正文搜索、热榜、热门板块。
- 文章详情：封面、正文、浏览/点赞/收藏数据、评论、举报操作。
- 投稿页：板块选择、封面上传、保存草稿、提交审核。
- 个人中心：头像、昵称、简介、文章状态、收藏和举报记录。
- 用户主页：公开资料、已发布文章、关注/粉丝列表。
- 通知中心：未读徽标、分页、单条已读、全部已读。
- 管理后台：独立布局、分组导航、表格管理、确认弹窗和移动端抽屉。

### 8.3 UI 重构结果

- 视觉方向为暖色编辑社区。
- 使用纸张米白背景、暖白内容表面、深墨文字、森林绿主色和陶土色强调。
- 公共端桌面采用文章主栏加侧栏，移动端使用底部主导航。
- 管理端桌面使用深色固定侧栏，窄屏使用抽屉导航。
- 已增加跳过导航、可见焦点、分页语义、对话框语义、Escape 关闭、焦点圈定和焦点恢复。
- 响应式断点覆盖约 1000、760、390px；管理表格在自身容器中横向滚动，避免整页溢出。
- 没有引入 UI 组件库或全局状态库，保持当前项目规模下的最小复杂度。

### 8.4 前端边界

- JWT 和用户摘要存放在 `localStorage`，适合毕业设计演示，但存在 XSS 窃取风险。
- 前端根据本地角色控制管理入口只是体验层保护，真实权限始终由后端管理员拦截器校验。
- 评论目前显示“用户 #ID”，尚未补充作者昵称与头像。
- 当前没有富文本编辑器、暗色模式、国际化、实时 WebSocket 通知或 SSR。
- 前端自动化测试覆盖较少，完整页面流程仍需要人工回归或 E2E 测试。

## 9. Docker 与运行方式

### 9.1 当前 Compose 服务

| 服务 | 宿主机端口 | 持久化 |
| --- | --- | --- |
| React/Nginx | 80 | 镜像内静态文件 |
| Spring Boot | 8080 | `uploads-data` 上传卷 |
| MySQL | 3307 -> 容器 3306 | `mysql-data` |
| Redis | 6379 | 当前未配置独立持久化卷 |
| RabbitMQ | 5672、15672 | 当前未配置独立持久化卷 |

前端 Docker 访问地址是 `http://localhost`，不是 Vite 开发端口 `5173`。开发模式才使用 `npm run dev` 和 `http://localhost:5173`。

重建整个项目：

```powershell
docker compose up --build -d
```

只重建前端：

```powershell
docker compose up --build -d frontend
```

只重建后端：

```powershell
docker compose up --build -d app
```

普通停止使用 `docker compose down`。不要把 `docker compose down -v` 当作常规停止命令，因为 `-v` 会删除 MySQL 与上传数据卷。

### 9.2 Docker 当前边界

- `depends_on` 只保证启动顺序，当前没有为数据库、Redis、RabbitMQ 和应用配置 Compose healthcheck/健康依赖。
- Redis 和 RabbitMQ 没有单独的持久化卷。
- Docker 镜像构建阶段跳过 Maven 测试，正确流程应在构建/发布前单独运行测试。
- 根 README 仍有旧端口和旧前端启动说明，需要后续同步更新。

## 10. 自动化测试与实时验证

### 10.0 最新结果（2026-09-19，M13-M18 完成后）

| 验证 | 结果 |
| --- | --- |
| 后端 `mvn test` | **416 项：406 通过 + 10 条件跳过，0 failure / 0 error**（跳过项均为需要真实 API Key 的调用测试） |
| 前端 `npm test` | **36 项全部通过**（7 个测试文件） |
| 前端 `npm run build` | 通过（Vite 7，产物 css/js 均正常生成） |
| Flyway | schema 当前版本 **19**，无需迁移 |
| 真实调用（按需执行，默认跳过） | 工具调用（M14）、带引用回答（M15）、审核 8/8（M16）、洞察 5.0s/2695 tokens（M17）、推荐理由 5/5（M17）、执行轨迹含工具链（M18） |
| 真实环境端到端 | 一次 AI 助手提问 → 轨迹 5 步（含 `TOOL_CALL`）+ 用量 7085 tokens / 估算 0.0178 元 |

> ⚠️ 每次跑**全量** `mvn test` 会清空 Redis 向量索引 `bitforum-kb`（M15 起的既有行为），
> 之后需用 `M17_VECTOR_PROBE=true -Dtest=RecommendVectorRecallProbe` 重建。

### 10.1 测试规模（2026-08-02 历史快照）

- 后端：33 个 Java 测试文件，176 个测试方法。
- 前端：3 个 Vitest 文件、4 个用例。
- 后端测试覆盖 Controller、Service、Redis、RabbitMQ 消费、上传、关注集成、OpenAPI、Actuator、定时任务和权限规则。
- 前端测试覆盖认证角色存储、损坏本地数据清理、文章卡片插槽不重复、确认弹窗语义/Escape。

### 10.2 本轮实时结果（2026-08-02 历史快照）

在 2026-08-02 当前工作区实际运行：

| 验证 | 结果 |
| --- | --- |
| `npm test` | 3 个测试文件、4 个测试全部通过 |
| `npm run build` | 通过，Vite 7.3.5 构建 1884 个模块 |
| `docker compose config --quiet` | 通过 |
| `mvn test` | 176 个测试全部通过，0 failure、0 error、0 skipped |
| Flyway | schema 当前版本 11，无需迁移 |

环境说明：

- 本轮测试时本机 MySQL 3306、Redis 6379可连接。
- RabbitMQ 5672 未启动，因此测试日志出现 AMQP 重连失败，但没有导致测试失败。
- Maven 本轮实际使用本机 Java 21 运行测试；项目编译目标与 Docker 运行环境仍为 Java 17。
- 本轮 `docker compose ps` 没有运行中的容器；只代表检查时容器未启动，不代表 Compose 配置失败。

## 11. 已完成的审计与修复

旧计划中“下一步先做全面检查，再重构前端”已经落后。仓库已经完成过一轮后端 P0/P1 审计修复和前端布局审查，随后完成了前端整体重构。

已落实的审计修复包括：

- 测试专用 JWT 配置，不给生产 JWT 密钥设置危险默认值。
- 登录保护接口拒绝禁用用户。
- 禁止直接修改待审核、已发布或下架文章。
- 数据库级阻止重复待处理举报。
- 禁止用户给自己的文章点赞。
- Controller 返回更新/删除业务错误信息。
- JWT 解析异常与账号状态异常分开处理。
- 修复管理表格操作列边框布局。
- 修复前端无 URL 路由、移动端布局不足、管理员入口角色识别、弹窗可访问性和文章卡片重复内容等问题。

因此，后续维护不应再把“全面检查”和“前端整体重构”当作尚未开始的首要任务。

## 12. 当前已知风险与未实现项

以下内容应被记为待办或架构边界，不能描述成已完成：

### 12.1 优先级较高

1. RabbitMQ 幂等标记写入时机可能导致失败消息重放被跳过，需要重新设计“处理成功后标记”或可恢复状态机。
2. RabbitMQ 发布失败目前主要日志记录，缺少 Outbox、发送失败持久化和自动补偿。
3. 统一错误包装与 HTTP 状态不完全一致，部分业务错误仍以 HTTP 200 返回。
4. 上传仅校验声明的 MIME 和大小，缺少文件魔数检测、图片解码验证、旧文件清理和安全扫描。
5. JWT 存储于 `localStorage`，生产级安全仍需评估 HttpOnly/Secure/SameSite Cookie 和 CSRF 方案。

### 12.2 工程与测试

6. 前端只有 4 个自动化测试，缺少主要页面集成测试和端到端测试。
7. 尚缺接口限流、登录防暴力破解、投稿/评论/举报频率限制。
8. 测试仍依赖本机 MySQL/Redis，隔离性不如 Testcontainers 或专用测试容器。
9. Compose 缺少 healthcheck 和基于健康状态的启动依赖。
10. Redis、RabbitMQ 在 Compose 中未配置持久化卷。
11. 根 README 的功能、端口和 Docker 前端说明已落后。

### 12.3 产品体验

12. 评论作者信息仍主要显示用户 ID，缺少昵称、头像和作者主页联动字段。
13. 没有富文本、图片正文、站内私信、实时通知、全文搜索引擎、对象存储、邮件/短信推送。
14. 没有生产流量、压测、监控告警、CI/CD、云部署或 SLA 证据。

### 12.4 M13-M18（AI 模块）新增的边界与遗留（2026-09-19）

以下都是**如实声明**，答辩时不应回避：

1. **推荐指标来自合成数据**：`HitRate@10` = 0.8889（测试集 9 位用户）/ 0.8095（开发集 21 位用户），
   对比纯热榜基线 0.2222；语料、用户与行为均由作者构造，热榜被刻意设为与主题无关、
   “关注=同兴趣”是直接设定的 —— 因此“融合大幅超越热榜”里有一部分是数据构造的功劳。
   局限与诚实声明见 `ai-agent-upgrade/m17-eval-report.md`。
2. **向量召回的区分度偏弱**：同主题与跨主题文章的平均相似度只差 0.0147（目标 0.05），
   已在 M17 决策为“接受为已知局限”，不通过改语料/换模型/调算法去凑指标。
3. ~~`TokenBudgetGuard` 未实现~~ → **已实现（克制版，2026-09-19）**：单用户每日 token/费用上限、
   超限统一降级、单次输入输出保护；套餐/充值/余额、会员等级、分布式配额中心、动态配置后台**明确不做**。
4. **执行轨迹与用量明细没有归档/清理策略**：数据随 AI 调用量增长，长期需要保留策略。
5. **没有做 AI 相关压测**：轨迹与用量是新增的写库路径，尚未评估其对延迟的影响。
6. **知识库与审核链路依赖 Redis Stack 与 RabbitMQ**：中间件异常时 AI 能力降级（主流程不受影响），
   但降级期间不会有 AI 审核结果与向量召回。
7. **未做「已点赞」反向索引**：M17 只能排除“作者本人 + 已收藏”，点赞数据无法反查（要改 M6 数据模型）。
8. **评测数据仍留在库中并带 `[M17Eval]` 前缀**：答辩演示用，展示时必须明确标为**演示数据**；
   一键清理命令见 `ai-agent-upgrade/m17-eval-report.md` 与 `m18-handoff.md`。
9. **AI 审核的自动放行阈值保持关闭**：该阈值在独立测试集上覆盖率为 0%（过拟合开发集），
   数据不支持启用。

## 13. 当前真正的下一步

> ⚠️ 下面的 1-6 条是 **2026-08-02 的历史建议**（Docker/README/回归/RabbitMQ 幂等等），
> 其中多数已处理或不再是首要任务。**M13-M18 完成后（2026-09-19）的剩余事项见第 16.8 节。**

建议按以下顺序继续：

1. 确认当前未提交的 Docker 前端化、文章详情 CSS 修正和文档是否作为同一批提交；提交时按路径暂存，保护两份学习文档。
2. 更新 README：补充 `http://localhost`、MySQL 3307、五服务 Compose、上传卷和禁止常规使用 `down -v`。
3. 对 360/375/768/1024/1440px 进行浏览器人工回归，覆盖游客、普通用户、管理员三种身份和深层路由刷新。
4. 优先修复 RabbitMQ 幂等/补偿与 HTTP 状态语义，再处理上传魔数校验和限流。
5. 增加少量高价值前端集成/E2E 测试，而不是机械堆叠组件单测。
6. 收口毕业设计答辩材料、架构图、数据库关系图和部署说明。

## 14. 项目描述时的真实性规则

> ⚠️ 本节已按 2026-09-19 的实际情况校正（原第 4 条“不能说已使用 Spring AI”已失效）。

可以明确说：

- 已实现完整文章审核状态机、通知中心、举报治理、关注关系、文件上传、OpenAPI、Redis 指标同步、Actuator 和 React 管理/用户端。
- 已实现 RabbitMQ Confirm/Returns、手动 ACK、DLQ 和 Redis 幂等基础机制。
- 已有 **416 个后端测试方法（406 通过 + 10 条件跳过）与 36 个前端测试**，最近一次完整运行全部通过。
- React 已完成整体路由与响应式视觉重构，并能通过 Docker/Nginx 部署。
- **已接入 Spring AI 1.1.8 + DeepSeek，落地四个协作 Agent（问答 / 审核 / 运营分析 / 推荐），
  具备 12 个 `@Tool` 工具调用、RAG 向量知识库与引用回答、人机协同审核、可解释推荐，
  以及执行轨迹、用量成本与统一降级（M13-M18，已本地提交）。**
- 推荐与审核的效果数据有明确口径与局限说明，可引用但必须带上“合成数据/样本量小”的前提。

不能说：

- RabbitMQ exactly-once、消息绝对不丢或完整自动补偿。
- 系统已上线、有真实并发、真实用户量或生产性能数据。
- 已使用 Elasticsearch、WebSocket、微服务、Kubernetes、对象存储或完整 CI/CD。
- 前端已经有完整 E2E 测试或生产级认证安全。
- AI 能力已具备生产级的多租户配额体系（当前只有单用户每日上限的克制版预算）、或推荐指标代表真实社区流量效果。
- 已使用 TailwindCSS、Redux 等前端库（前端仍是原生 CSS + React Context，无状态管理库、无图表库）。

## 15. 项目真实性摘要

下面保留一段当时的历史状态摘要：

> Bit Forum 是林坚浩持续维护的个人毕业设计项目。截至 2026-08-02，项目使用 Java 17、Spring Boot 3.4.5、MyBatis-Plus 3.5.9、MySQL 8、Flyway、Redis 7、RabbitMQ 3、JWT/BCrypt、OpenAPI、Actuator，以及 React 19 + Vite 7 + React Router 7 + Axios 前端。后端已完成注册登录、普通/管理员权限、板块分类、文章草稿-待审-发布-驳回-下架状态机、评论、点赞与热榜、收藏与搜索、通知中心、举报治理、管理员运营看板、用户资料、公开主页、关注关系、头像/封面上传、Redis 指标定时落库、Swagger/OpenAPI 和管理员聚合健康检查。数据库由 Flyway V1-V11 管理，共 9 个主要实体表。前端已从演示页重构为有正式 URL 路由、公共站点布局、个人中心、通知中心和独立管理后台的暖色响应式社区界面，并补充基础可访问性。当前分支为 `feat/frontend-refactor`，HEAD `bfbfcce` 已推送远端；M1-M12 和前端整体重构已提交。React 的 Node 22 + Nginx Docker 镜像、Compose 前端服务、上传卷和一处文章详情 CSS 修正已在工作区实现并验证，但截至该日期尚未提交。2026-08-02 实时验证结果为 Maven 176 个测试全部通过、Vitest 4 个测试通过、Vite 生产构建通过、Compose 配置校验通过。当前高优先级不是再次从头做前端重构，而是收口 Docker/README/人工回归，并处理 RabbitMQ 幂等与补偿、HTTP 错误状态、上传真实内容校验、限流、测试隔离和前端测试覆盖。描述项目时必须区分已实现与待办；不得宣称 exactly-once、绝对不丢消息、生产流量、Elasticsearch、WebSocket、Spring AI、TailwindCSS、微服务、Kubernetes、对象存储或完整 CI/CD。

> 以上摘要为 **2026-08-02 的历史状态**（其中“不得宣称 Spring AI”已被 M13-M18 推翻）。
> 2026-09-19 的现状见文首更新块与下面的第 16 节。

---

## 16. M13-M18：AI Agent 升级（2026-09-19 完成）

### 16.1 定位升级

项目定位从「后端能力较完整的论坛」升级为「**基于 Spring Boot + React 的多智能体智能社区平台**」：
论坛既有的查询与写操作被封装成工具交给大模型调用，形成四个协作 Agent，
并在其上补了 RAG 知识库、人机协同审核、可解释推荐与可观测性。

四个 Agent 的分工（都在 `com.bitforum.ai` 下）：

| Agent | 触发方式 | 职责 | 关键约束 |
| --- | --- | --- | --- |
| `QaAgent`（问答助手） | 用户在全局 AI 助手面板提问 | RAG 检索 + 12 个工具调用 + 带引用回答 | 写操作必须先与用户确认；身份由 `ToolContext` 注入，不由模型传 |
| `ModerationAgent`（内容审核） | 文章提交审核 / 评论发布 → RabbitMQ | 五维风险评估（有害/广告/诈骗/灌水/敏感）+ 置信度 | **只判断不动作**：自动放行由 Java 按确定性规则决定，永不自动驳回 |
| `AnalystAgent`（运营分析） | 管理员主动触发 → 单线程池异步 | 读看板统计工具，生成运营洞察报告 | 数字必须来自工具；快照与正文同时落库以便对账 |
| `RecommendReasonAgent`（推荐理由） | 文章详情页 / AI 助手回答末尾 | 为**已由 Java 定序**的 Top-N 写推荐理由 | **不参与选文**；列表在无理由时同样可用（降级路径） |

### 16.2 六个模块的交付

| 模块 | 交付内容 | 验证 |
| --- | --- | --- |
| M13 | Spring AI + DeepSeek 接入、V13 会话/消息表、`AgentOrchestrator` 路由、自定义会话记忆、AI 助手前端面板、全链路降级 | 真实多轮对话；211 项测试 |
| M14 | 12 个 `@Tool` 工具（`ArticleTools` 5 + `UserInteractionTools` 7）、`ToolRegistry` 按 Agent 装配、工具身份注入 | 真实调用完成搜索与点赞写操作；findings 6.6 / 6.7 |
| M15 | V14 知识库表、ONNX 本地嵌入（bge-base-zh-v1.5 / 768 维）、Redis Stack 向量检索、引用回答、MQ 异步索引与全量重建 | 真实回答带引用；端到端索引链路；findings 6.8-6.10 |
| M16 | V15 审核记录表、五维固定字段结构化输出、决策/动作解耦、人工反馈闭环、管理台审核台 | 评测 70 + 30 条：漏放率 0%、误伤率 0%、安全召回率 100% |
| M17 | V16/V17、AnalystAgent 异步洞察、三路召回（向量/热度/关注）+ RRF 融合 + 推荐理由、三处前端落地、离线评测 | 洞察真实调用 5.0s / 2695 tokens；推荐 `HitRate@10` 0.8889 vs 热榜 0.2222 |
| M18 | V18 执行轨迹 + V19 用量统计、四条链路埋点、`AiDegradeGuard` 统一降级、管理端轨迹页与用量概览、克制版用量预算闸门 | 416 / 36 项测试；真实环境 5 步轨迹含工具调用 |

### 16.3 新增接口与前端入口

管理端（`/api/admin/**`，`AdminInterceptor` 鉴权）：

- `GET /api/admin/ai/kb/stats`、`POST /api/admin/ai/kb/rebuild`（知识库统计与全量重建）
- `GET /api/admin/ai/moderation/records`、`POST .../records/{id}/feedback`、`POST .../records/{id}/handle`
- `POST /api/admin/ai/insight/generate`、`GET /api/admin/ai/insight/latest|status|history`
- `GET /api/admin/ai/traces`、`GET /api/admin/ai/traces/{traceId}`（**M18 执行轨迹**）
- `GET /api/admin/ai/usage/overview?days=7`（**M18 用量与成本**）

用户端（`/api/ai/**`，`LoginInterceptor` 鉴权）：会话 CRUD、发消息、`/api/ai/recommendations`。

前端入口：全局 AI 助手面板、文章详情页「相关推荐」、看板「AI 运营洞察」卡片、
管理端「知识库 / AI 审核 / **执行轨迹**」三页。

### 16.4 关键实测结论（全部来自本机实测，非推测）

| 编号 | 结论 | 位置 |
| --- | --- | --- |
| T1/T2 | Spring AI 1.1.8 与 Boot 3.4.5 无冲突；Redis Stack 替换未破坏既有功能 | findings 6.1 |
| — | `spring.ai.deepseek.chat.enabled` **不是** bean 创建总开关，缺 Key 会启动失败 → 降级必须写在调用层 | findings 6.2 |
| T3 | ONNX 嵌入模型 768 维、缓存后可离线跑；启动期即加载，无法用开关关闭 | findings 6.8 |
| T5 | RediSearch 元数据过滤在封装层生效（OFFLINE 内容不被召回） | findings 6.9 |
| T8 | 知识库索引队列与文章发布队列互不干扰（独立队列 + 独立死信） | findings 6.10 |
| T7 | 让模型返回**变长列表**时元素数量不稳定（4/5/6 个都出现过）→ 结构化输出改用固定字段 | findings 6.12 |
| T12 | 工具执行循环在 provider 内部，**最终响应里没有工具链**；只能包装 `ToolCallback` 采集"工具名/入参/返回值/单步耗时" | findings 6.18 |
| T13 | 轨迹跨异步链路：MQ 用消息头透传 traceId（不影响消息体），线程池必须显式包装（不包装必然丢失） | findings 6.19 |
| T11 | 文章之间的向量相似度分布很窄（同主题 vs 跨主题仅差 0.0147），因此 RRF 按**排名**而不是分数融合 | findings 6.15 |

### 16.5 真实调用验证数据（真实 DeepSeek，非 mock）

| 场景 | 数据 |
| --- | --- |
| 工具调用（M14/M18 复验） | 一次提问触发 2 个工具：`currentServerTime` 3ms、`searchArticles` 1ms；token 946 / 110 / 1056 |
| RAG 引用回答（M15） | 回答带可点击文章引用，引用来自向量检索命中片段 |
| 内容审核（M16） | 8 条样本 8/8 成功、五维分与档位合法 |
| 运营洞察（M17） | 5.0 秒 / 2695 tokens，且报告主动指出了数据异常 |
| 推荐理由（M17） | 5/5 条理由生成成功 |
| 执行轨迹（M18） | 一次 AI 助手提问：轨迹 5 步（`ROUTE`→`RETRIEVE`→`TOOL_CALL`→`LLM_CALL`→`PERSIST`）、7085 tokens、估算 0.0178 元 |

### 16.6 评测结果与诚实边界

**内容审核**（开发集 70 条 + 独立测试集 30 条，人工标注）：漏放率 0%、误伤率 0%、安全召回率 100%；
严格准确率 96.7%~97.1%；自动放行阈值在测试集上覆盖率 0%（过拟合开发集）→ **保持关闭**。

**智能推荐**（合成数据 + 留一法）：融合 `HitRate@10` = 0.8889（测试集）/ 0.8095（开发集），
纯热榜基线 0.2222 / 0.2381，随机 0.3333 / 0.2857。

必须在任何场合同时说明的三条局限：

1. 测试集只有 **9 位用户**（开发集 21 位），指标粒度粗，单个用户排名变化就会明显影响结果；
2. 文章、用户、收藏行为**全部由作者合成**，不是真实社区流量；
3. 热榜基线被刻意设成与主题无关、「关注=同兴趣」是直接设定的，
   所以“融合大幅超越热榜”里有**一部分是数据构造的功劳**。

### 16.7 工程约束（M13-M18 踩过的坑，后续开发必须遵守）

1. `@ConditionalOnBean` 在用户配置类中不可靠 → 用 `@ConditionalOnProperty`；
2. `@MapperScan` 不扫子包 → 新包要同步注册（本项目把 AI 的 Mapper 统一放在 `com.bitforum.ai.mapper`）；
3. 测试配置值写成占位符形式，硬编码会覆盖环境变量；
4. 工具/提示词改动必须用干净会话验证，旧会话历史会延续旧结论；
5. Redis 必须是 Redis Stack，普通 Redis 报 `FT._LIST` unknown command；
6. 异步消息必须在事务提交后发送（`AfterCommitExecutor`）；
7. 结构化输出用固定字段，让模型返回变长列表时数量不稳；
8. 只新增 Flyway 迁移，绝不修改历史迁移；
9. AI 能力必须有降级路径，AI 不可用不能影响论坛主流程；
10. 新增接口若用 `@RequestAttribute("userId")`，必须同时加进 `WebMvcConfig` 的登录拦截列表（漏配直接 500）。

### 16.8 剩余事项（**不含新功能开发**）

| 项 | 状态 |
| --- | --- |
| 全量回归 | 已完成（416 / 36 项）；注意全量测试会清空向量索引，需重建 |
| 压测 / 性能观察 | 功能侧的确定性开销观察正由收口任务书阶段 2 进行（见 `closure-worklog.md`）；真实端到端性能仍未测 |
| 演示脚本与答辩材料 | 骨架已有（`ai-agent-upgrade/task_plan.md` 第十三节，第四幕即「执行轨迹页」逐帧演示） |
| ~~push 检查点~~ | **已完成**（`feat/ai-agent` 已 push，最新 `514f301`，CI success（**限 `514f301` 已提交内容**；本轮收口改动未提交、未过远端 CI）） |
| 合并回 `main` | **未做**，需项目作者单独确认（原计划为系统冻结后一次性合并） |
| ~~`TokenBudgetGuard`~~ | **已完成（克制版）**，见 `ai-agent-upgrade/progress.md` M18-5 |
| 轨迹与用量表的归档清理 | 未做（长期需要） |
| `scripts/agent-tool-smoke.md`（M14 工具调用会话整理） | 未做（记录类小事） |

### 16.9 代码与文档入口

```text
src/main/java/com/bitforum/ai/           四个 Agent、工具、RAG、推荐、轨迹(trace)、用量(usage)、降级(degrade)
src/main/resources/db/migration/V13~V19  AI 模块的全部迁移
frontend/src/components/AiAssistantPanel.jsx / RelatedRecommendations.jsx / AiInsightCard.jsx
frontend/src/pages/admin/KnowledgeBase.jsx / ModerationRecords.jsx / AiTraces.jsx
docs/graduation/ai-agent-upgrade/task_plan.md   总体计划 + M16/M17/M18 决策
docs/graduation/ai-agent-upgrade/findings.md    技术证据（6.1~6.19）
docs/graduation/ai-agent-upgrade/progress.md    执行记录（M13-x ~ M18-x）
docs/graduation/ai-agent-upgrade/m17-eval-report.md     推荐评测报告（含局限与诚实声明）
docs/graduation/ai-agent-upgrade/m18-decision-brief.md  M18 五个口径问题的决策材料
docs/graduation/ai-agent-upgrade/m18-handoff.md         环境、命令、约束与演示账号
```
