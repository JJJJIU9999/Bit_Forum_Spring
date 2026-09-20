# BitForum AI Agent 化升级 —— 技术勘察与发现记录

## 九、2026-09-20 最终收口只读基线

- 分支为 `feat/ai-agent`，HEAD 为 `514f301`，相对本地 `main` 领先 41 个提交；`origin/HEAD...HEAD` 为 `0 41`。本轮开始时工作区干净。
- README 已表述 M13–M18 与克制版 `TokenBudgetGuard` 已完成；`m18-status-summary.md`、`external-ai-briefing.md`、`progress.md` 顶部和 `毕业设计文档总览.md` 仍把不同日期的 Git/推送状态写成当前事实，存在同一导出包内新旧快照混排风险。
- `scripts/export-ai-context.sh` 已动态生成页眉，但会原样拼入项目简报和 README；因此根因是被嵌入文档含未标注的旧动态快照，不是页眉的 Git 查询错误。
- 已有 `AiTokenBudgetGuardTest`、`AiUsageRecorderTest`、`AiDegradeGuardTest`、`AgentOrchestratorTest` 与控制器测试。后续先评估其断言能否直接形成故障注入和预算边界证据，避免为展示目的改变生产路由。
- 首次运行新增隔离测试时，测试配置的 RabbitMQ 默认凭据与本机容器不一致，监听器启动被拒绝；MySQL 连接和 Flyway V19 校验成功。性能观察与故障注入均不使用消息消费，因此测试类通过 `spring.rabbitmq.listener.simple.auto-startup=false` 关闭测试上下文的 listener 自动启动，既不读取 `.env` 也不改变生产配置。
- 故障注入第一次真正请求不可达回环端口后命中 Spring AI 默认的 10 次重试与指数退避；这是正确的 provider 行为，但不适合可重复的单测。测试专用属性固定 `spring.ai.retry.max-attempts=1`，仅让故障注入在一次实际连接失败后断言降级；生产重试策略未改变。
- 故障注入发现真实缺陷：`TraceRecorder.degrade(reason, detail)` 将 provider 的异常详情作为轨迹 `message` 保存，虽不影响 QA 返回的统一文案，但管理端可见 Trace 会暴露连接 URL 等底层信息。最小修复是让 Trace 始终按原因码保存统一文案，原始 detail 仅写 debug 日志；集成测试锁定“Trace 不含连接详情”。
- 运行既有 `TraceRecorderIntegrationTest` 时遇到同一 RabbitMQ listener 启动前置条件；其 6 个用例只验证 MySQL Trace/Usage 与线程传播，不消费 MQ。测试类同样限定 listener 自动启动为 false，保持生产与消息消费测试配置不变。

> 本文记录 M13-M18 立项前的全部技术勘察证据。所有结论均来自本机实测或官方文档原文，不包含推测。
>
> 勘察时间：2026-09-18
> 勘察环境：macOS（MacBook），`/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring`

## 一、现有项目基线实测

### 1.1 仓库与分支状态

```bash
git remote -v
# origin  https://github.com/JJJJIU9999/Bit_Forum_Spring.git (fetch)
# origin  https://github.com/JJJJIU9999/Bit_Forum_Spring.git (push)

git branch -a -vv
# * main                                     d6dd582 [origin/main] docs: finalize public project documentation
#   remotes/origin/HEAD                      -> origin/main
#   remotes/origin/feat/frontend-refactor    e81302a docs: improve GitHub recruitment presentation
#   remotes/origin/feat/frontend-ui-redesign e740adb feat(frontend): 重构前端 UI 布局与视觉样式
#   remotes/origin/graduation-design         d4d4503 docs(graduation): summarize M7 through M12 progress
#   remotes/origin/main                      d6dd582 docs: finalize public project documentation
#   remotes/origin/phase-2-frontend-admin    447d663 feat: add phase 2 admin backend
#   remotes/origin/phase-2-react-frontend    68bae14 fix: prevent duplicate likes from increasing hot score

git rev-list --left-right --count main...origin/main
# 0	0     ← main 与 origin/main 完全同步

git status --short
# 仅有 30 个未跟踪条目（docs 下的架构图/时序图产物与顶层执行记录），
# 已跟踪文件零未提交改动（git diff --stat 输出为空）

git log -8 --format='%h %ci %s'
# d6dd582 2026-08-24 22:31:14 docs: finalize public project documentation
# 9dd7d8c 2026-08-24 22:30:39 ci: add project verification workflow
# a1a9d78 2026-08-24 22:30:19 chore: improve service health orchestration
# a92dbcc 2026-08-24 22:30:00 fix: harden backend correctness
# e81302a 2026-08-24 21:06:58 docs: improve GitHub recruitment presentation
# 2af893c 2026-08-24 01:30:41 docs(learning): add BitForum project study notes
# c927713 2026-08-24 01:21:03 docs(graduation): record BitForum project baseline summary
# d9d4f48 2026-08-24 01:19:47 fix(frontend): correct article detail spacing
```

**结论**：

- `main` 是干净、稳定、与远端同步的封版基线 —— 拉分支的最佳时机。
- 最近 4 个提交均为封版性收尾（correctness 修复、健康编排、CI、文档定稿），说明 `main` 被刻意维护为「可公开、可运行、可测试」的冻结基线。
- 这是一个**公开仓库**，同时是用户的求职作品集。

### 1.2 项目规模

```bash
find src/main/java -name "*.java" | wc -l    # 98
find src/test -name "*.java" | wc -l         # 33
ls src/main/resources/db/migration/ | sort
# V1__init_schema.sql
# V2__add_user_role_status.sql
# V3__add_category.sql
# V4__add_article_audit_status.sql
# V5__add_article_favorite.sql
# V6__add_notification.sql
# V7__add_content_report.sql
# V8__add_pending_report_unique_key.sql
# V9__add_user_profile_fields.sql
# V10__add_user_follow.sql
# V11__add_file_upload_fields.sql
# V12__add_notification_source_message.sql
```

后端包结构：

```text
com.bitforum
├── common/        统一 Result<T> 等
├── config/        JwtProperties, MybatisPlusConfig, OpenApiConfig, RabbitMQConfig, WebMvcConfig
├── controller/    18 个 Controller
├── dto/           33 个 DTO
├── entity/        9 个实体
├── exception/
├── interceptor/   LoginInterceptor, AdminInterceptor
├── job/           ArticleMetricSyncJob
├── mapper/
├── message/       ArticlePublishMessage
├── service/       14 个 Service
└── util/
```

前端结构：

```text
frontend/src
├── api/           adminApi, articleApi, categoryApi, commentApi, notificationApi,
│                  reportApi, request, uploadApi, userApi
├── components/    ArticleCard, ConfirmDialog, EmptyState, LoadingSpinner,
│                  MessageBanner, PageHeader, Pagination, StatusBadge
├── layouts/       AdminLayout, MainLayout
├── pages/         ArticleDetail, ArticleList, Login, MyArticles, NotificationCenter,
│                  PublicUserProfile, PublishArticle, Register
├── pages/admin/   AuditArticles, Dashboard, ManageArticles, ManageCategories,
│                  ManageComments, ManageReports, ManageUsers
└── styles/
```

### 1.3 技术栈实测

`pom.xml`（关键部分）：

- parent：`spring-boot-starter-parent:3.4.5`
- `java.version`：17
- 已有依赖：web、actuator、mysql-connector-j、flyway-core、flyway-mysql、validation、springdoc-openapi-starter-webmvc-ui:2.8.9、lombok、mybatis-plus-spring-boot3-starter:3.5.9、mybatis-plus-jsqlparser:3.5.9、data-redis、amqp、jjwt 0.12.6、spring-security-crypto
- **当前没有任何 Spring AI 依赖**

`frontend/package.json`：

- dependencies：react ^19.0.0、react-dom ^19.0.0、react-router ^7.18.1、axios ^1.17.0、lucide-react ^1.24.0、vite ^7.0.0、@vitejs/plugin-react ^5.0.0
- devDependencies：vitest ^4.1.10、jsdom ^29.1.1、@testing-library/react ^16.3.2、@testing-library/user-event ^14.6.1
- scripts：`dev` / `build` / `preview` / `test`（`vitest run`）

**注意**：前端用的是 **React 19 + react-router 7**（不是文档里可能提到的旧版本），AI 面板要按这个版本写。

### 1.4 关键基础设施现状

`docker-compose.yml` 服务清单：

| 服务 | 镜像 | 端口 | 健康检查 |
| --- | --- | --- | --- |
| mysql | `mysql:8.0` | 3306:3306（接手时为 3307:3306，本轮已改回） | mysqladmin ping |
| **redis** | **`redis:7-alpine`** | 6379:6379 | redis-cli ping |
| rabbitmq | `rabbitmq:3-management` | 5672、15672 | rabbitmq-diagnostics |
| app | 本地 Dockerfile | 8080:8080 | 依赖前三个 healthy |
| frontend | `./frontend` | 80:80 | — |

**关键发现**：Redis 用的是官方 `redis:7-alpine`，**不包含 RediSearch 模块**，因此 Spring AI 的 `RedisVectorStore` 无法工作。必须换成 Redis Stack 镜像。

`application.yml` 现状：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
  flyway:
    baseline-on-migrate: true
jwt:
  secret: ${JWT_SECRET}
bitforum:
  article-metric-sync:
    fixed-delay: ${ARTICLE_METRIC_SYNC_FIXED_DELAY:300000}
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**结论**：配置里**没有** `spring.ai.*`，AI 配置需要全新增加。

现有 `RedisService` 公开能力（可被 AI 层复用的基础设施）：

```text
increaseViews / getViews / getViewsIfPresent
like / unlike / hasLiked / getLikeCount / getLikeCountIfPresent
increaseHot / deleteArticleData
isMessageProcessed / markMessageProcessed     ← 幂等能力，可复用于 AI 异步任务
```

**结论**：`isMessageProcessed` / `markMessageProcessed` 是现成的幂等基础设施，AI 审核与索引任务可直接复用。`increaseHot` 用的 ZSet 可以直接作为推荐 Agent 的热度召回源。

`WebMvcConfig` 拦截器现状：

- `AdminInterceptor` → `/api/admin/**`
- `LoginInterceptor` → 18 个具体写接口路径（`/api/article/publish`、`/api/comment/publish`、`/api/users/*/follow` 等）

**结论**：新增 `/api/ai/**` 与 `/api/admin/ai/**` 时，`/api/admin/ai/**` 自动被 `AdminInterceptor` 保护；`/api/ai/**` 需要在 `WebMvcConfig` 中显式登记需要登录的路径。

### 1.5 环境变量与敏感信息处理

`.gitignore` 已包含：

```text
### 敏感文件 ###
*.log
.env
.env.*
!.env.example
run-local.cmd
```

`.env.example` 现有字段：

```text
MYSQL_ROOT_PASSWORD / MYSQL_DATABASE
SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
SPRING_RABBITMQ_USERNAME / SPRING_RABBITMQ_PASSWORD
JWT_SECRET / JWT_EXPIRATION
```

**结论**：`.env` 已被忽略、`.env.example` 已被显式保留，新增 `DEEPSEEK_API_KEY` 只需加入 `.env.example` 模板与本地 `.env`，不会误提交。

### 1.6 既有文档的过期信息

`docs/graduation/毕业设计文档总览.md` 与 `毕业设计进度.md` 中以下描述**已过期**：

| 文档中的描述 | 实际情况 |
| --- | --- |
| 「当前分支 `feat/frontend-refactor`（基于 graduation-design）」 | 当前是 `main`，且 `feat/frontend-refactor` 成果早已合并 |
| 「工作区包含 M1-M11 大量未提交改动，必须保留」（M12 文档） | 已跟踪文件零未提交改动 |
| 「M1-M12 已提交；前端整体重构实现已完成且未提交」 | 前端重构已提交，`main` 已封版 |
| 项目路径 `D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring` | 当前 MacBook 路径为 `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring` |

**处理**：本轮随计划书一并更正，不再让过期状态误导后续开发。

### 1.7 当前未运行的服务

```bash
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
# NAMES     IMAGE     STATUS     PORTS
# （空）
```

**结论**：Docker 已安装（version 29.7.2），但当前没有容器在运行。M13 开始前需要 `docker compose up -d` 拉起 MySQL / Redis Stack / RabbitMQ。

---

## 二、Spring AI 生态勘察

### 2.1 版本可用性（Maven Central 实测）

```bash
curl -s https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-bom/maven-metadata.xml
```

已发布版本（尾部节选）：

```text
1.0.0 ... 1.0.9
1.1.0-M1 ... 1.1.0-RC1, 1.1.0 ... 1.1.8
2.0.0-M1 ... 2.0.0-M8, 2.0.0-RC1, 2.0.0-RC2, 2.0.0, 2.0.1
```

`<release>` 为 **2.0.1**（当前最新稳定版）。

各 starter 的 `<release>`：

```text
spring-ai-starter-model-deepseek        → 2.0.1
spring-ai-starter-vector-store-redis    → 2.0.1
spring-ai-starter-model-openai          → 2.0.1
spring-ai-starter-model-ollama          → 2.0.1
spring-ai-starter-mcp-client            → 2.0.1
spring-ai-starter-vector-store-mysql    → （无独立 metadata，未采用）
```

### 2.2 版本兼容性判定（决定性证据）

**官方文档原文**（Spring AI Reference / Getting Started）：

> Spring AI supports Spring Boot **3.4.x** and **3.5.x**.

来源：`https://docs.spring.io/spring-ai/reference/1.1/getting-started.html`（页面标注版本 1.1.8）

**Spring AI 2.0.1 的 DeepSeek starter POM 依赖实测**：

```bash
curl -s https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-starter-model-deepseek/2.0.1/spring-ai-starter-model-deepseek-2.0.1.pom
```

关键依赖：

```xml
<artifactId>spring-ai-autoconfigure-model-deepseek</artifactId>
<artifactId>spring-ai-deepseek</artifactId>
<artifactId>spring-ai-client-chat</artifactId>
<artifactId>spring-boot-starter-webclient</artifactId>    ← Spring Boot 4.x 才有的 starter
<artifactId>spring-boot-starter-restclient</artifactId>   ← Spring Boot 4.x 才有的 starter
```

**Spring AI 1.1.8 的 DeepSeek starter POM 依赖实测**：

```xml
<artifactId>spring-ai-autoconfigure-model-deepseek</artifactId>
<artifactId>spring-ai-deepseek</artifactId>
<artifactId>spring-ai-autoconfigure-model-chat-client</artifactId>
<artifactId>spring-boot-starter</artifactId>              ← 通用，无 Boot 4 专属依赖
```

Spring Boot 版本可用性实测：

```bash
curl -s https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml
# 3.5.8 ... 3.5.16, 4.0.0 ... 4.0.8, 4.1.0, 4.1.1
# <release>4.2.0-M1</release>
```

**决策结论**：

| 方案 | 结论 |
| --- | --- |
| Spring AI **1.1.8** + 现有 Spring Boot **3.4.5** | **采用**。官方明确支持 3.4.x，零升级风险 |
| Spring AI 2.0.1 + Spring Boot 4.x | 排除。需要把 Boot 从 3.4.5 升到 4.x，会波及 M1-M12 全部 98 个 Java 文件、33 个测试文件与全部配置 |

### 2.3 构件可下载性实测

```bash
# 逐个 HEAD 请求，返回 HTTP 状态码
200  spring-ai-bom-1.1.8.pom
200  spring-ai-starter-model-deepseek-1.1.8.jar
200  spring-ai-starter-vector-store-redis-1.1.8.jar
200  spring-ai-starter-model-transformers-1.1.8.jar
200  spring-ai-starter-model-ollama-1.1.8.jar
```

**结论**：四个 starter 与 BOM 全部可从 Maven Central 正常下载，无需额外仓库配置。

### 2.4 DeepSeek starter 配置项实测

反编译 `spring-ai-autoconfigure-model-deepseek-1.1.8.jar` 中的 `spring-configuration-metadata.json`：

```text
配置前缀组：
  spring.ai.deepseek                        （连接配置）
  spring.ai.deepseek.chat                   （对话配置）
  spring.ai.deepseek.chat.options           （对话参数）
  spring.ai.deepseek.chat.options.tool-choice（工具选择策略）

关键属性：
  spring.ai.deepseek.api-key
  spring.ai.deepseek.base-url
  spring.ai.deepseek.chat.api-key
  spring.ai.deepseek.chat.base-url
  spring.ai.deepseek.chat.completions-path       默认 /chat/completions
  spring.ai.deepseek.chat.beta-prefix-path       默认 /beta
  spring.ai.deepseek.chat.enabled
```

自动配置类：`DeepSeekChatAutoConfiguration`、`DeepSeekChatProperties`、`DeepSeekConnectionProperties`、`DeepSeekParentProperties`。

**结论**：配置前缀确定为 `spring.ai.deepseek.*`，支持 `tool-choice` 参数（Tool Calling 必需），也支持自定义 `base-url`（可指向代理或自建网关）。

### 2.5 Tool Calling 能力实测（官方文档）

来源：`https://docs.spring.io/spring-ai/reference/1.1/api/tools.html`

核心机制原文：

> The model can only request a tool call and provide the input arguments, whereas the application is responsible for executing the tool call from the input arguments and returning the result. **The model never gets access to any of the APIs provided as tools**, which is a critical security consideration.

声明式用法实测示例：

```java
class DateTimeTools {
    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}

ChatModel chatModel = ...
String response = ChatClient.create(chatModel)
        .prompt("What day is tomorrow?")
        .tools(new DateTimeTools())
        .call()
        .content();
```

`@Tool` 注解支持：`name`（默认取方法名，同一次请求内必须唯一）、`description`（强烈建议填写）、`returnDirect`。

**额外发现（对答辩演示极有价值）**：Spring AI 提供 `AugmentedToolCallbackProvider`，可以向工具 schema 注入额外的推理字段并在回调中接收：

```java
AugmentedToolCallbackProvider<AgentThinking> provider = AugmentedToolCallbackProvider
        .<AgentThinking>builder()
        .toolObject(new MyTools())
        .argumentType(AgentThinking.class)
        .argumentConsumer(event -> {
            AgentThinking thinking = event.arguments();
            log.info("Tool: {} | Reasoning: {}", event.toolDefinition().name(), thinking.innerThought());
        })
        .removeExtraArgumentsAfterProcessing(true)
        .build();
```

**用途**：可在执行轨迹页展示每次工具调用时 LLM 的推理依据，作为 M18 可观测性的可选增强。

### 2.6 会话记忆能力实测（含破坏性变更）

来源：`https://docs.spring.io/spring-ai/reference/1.1/api/chat-memory.html` 与 `upgrade-notes.html`

默认行为：

> Spring AI auto-configures a `ChatMemory` bean... By default, it uses an in-memory repository (`InMemoryChatMemoryRepository`) and a `MessageWindowChatMemory` implementation... If a different repository is already configured (e.g., Cassandra, **JDBC**, or Neo4j), Spring AI will use that instead.

`MessageWindowChatMemory` 默认窗口 **20 条消息**，超出后移除较旧消息但保留 system message。

**1.1.6 起的破坏性变更（必须遵守）**：

> The conversation ID is no longer optional for the built-in memory advisors (`MessageChatMemoryAdvisor`, `PromptChatMemoryAdvisor`, and `VectorStoreChatMemoryAdvisor`). Every call through these advisors must supply `ChatMemory.CONVERSATION_ID` via the advisor context. If the value is absent or `null`, the advisor throws an `IllegalArgumentException` immediately.

> The constant `ChatMemory.DEFAULT_CONVERSATION_ID` (value `"default"`) has been removed from the `ChatMemory` interface.

**决策依据**：

- 默认内存仓储**重启即丢**，无法支撑毕业设计的审计需求 → 实现自定义 `MysqlChatMemoryRepository`。
- 官方明确「若已配置其他仓储实现，Spring AI 会使用它」→ 自定义实现会被自动接管，无需手动装配 Advisor。
- 代码中必须显式传 `conversationId`，绝不能引用已删除的 `DEFAULT_CONVERSATION_ID` 常量。

### 2.7 Advisor 机制

来源：`https://docs.spring.io/spring-ai/reference/1.1/api/advisors.html`

> The Spring AI Advisors API provides a flexible and powerful way to intercept, modify, and enhance AI-driven interactions in your Spring applications.

**用途**：`QuestionAnswerAdvisor`（RAG 检索注入）与 `MessageChatMemoryAdvisor`（会话记忆）都通过 Advisor 链路挂载到 `ChatClient`。M13 装配记忆 Advisor，M15 叠加问答 Advisor。

---

## 三、向量存储与嵌入模型勘察

### 3.1 Redis Stack 镜像可用性实测

```bash
curl -s "https://hub.docker.com/v2/repositories/redis/redis-stack-server/tags?page_size=8"
# latest        2025-11-03
# 7.4.0-v8      2025-11-03
# 7.4.0-v8-arm64 2025-11-03
# 7.4.0-v8-x86_64 2025-11-03
# 7.2.0-v20     2025-11-03
# 7.2.0-v20-arm64 / 7.2.0-v20-x86_64
```

**结论**：`redis/redis-stack-server:7.4.0-v8` 存在且有 arm64 变体（MacBook 必需），可直接替换现有 `redis:7-alpine`。

**兼容性评估**：端口仍是 6379，基础 Redis 命令协议不变，现有 `RedisService` 使用的 String / Set / ZSet 操作零影响。唯一变化是镜像体积变大（含 RediSearch、RedisJSON 等模块）。

### 3.2 Redis 向量库配置实测

来源：`https://docs.spring.io/spring-ai/reference/1.1/api/vectordbs/redis.html`

**依赖**：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
```

**配置**：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    vectorstore:
      redis:
        initialize-schema: true      # 必须显式开启
        index-name: bitforum-kb
        prefix: "bitforum:kb:"
```

**破坏性变更原文**：

> The vector store implementation can initialize the requisite schema for you, but you must opt-in by specifying the `initializeSchema` boolean... **this is a breaking change!** In earlier versions of Spring AI, this schema initialization happened by default.

**元数据过滤约束原文**：

> You must list explicitly all metadata field names and types (`TAG`, `TEXT`, or `NUMERIC`) for any metadata field used in filter expressions.

编程式构造示例（含元数据字段声明）：

```java
RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("bitforum-kb")
        .prefix("bitforum:kb:")
        .metadataFields(
                MetadataField.tag("articleId"),
                MetadataField.tag("categoryId"),
                MetadataField.tag("status"),
                MetadataField.numeric("publishTime"))
        .initializeSchema(true)
        .batchingStrategy(new TokenCountBatchingStrategy())
        .build();
```

过滤表达式会自动转换为 Redis 原生查询语法：

```text
country in ['UK', 'NL'] && year >= 2020   →   @country:{UK | NL} @year:[2020 inf]
```

**对 M15 的直接约束**：

1. 必须显式设置 `initialize-schema: true`，否则索引不会创建。
2. 计划用于过滤的 `articleId`、`categoryId`、`status`、`publishTime` 字段必须在构造时声明类型。
3. 只有 `PUBLISHED` 状态的文章进入知识库的过滤条件，可写成 `status == 'PUBLISHED'`。

### 3.3 嵌入模型方案实测

**问题**：DeepSeek 的 starter 只提供 Chat 模型，**不提供 embedding 接口**（`spring-ai-starter-model-deepseek` 只含 `DeepSeekChatModel`）。RAG 必须有 `EmbeddingModel`，因此必须另找来源。

候选方案对比：

| 方案 | 实测状态 | 评估 |
| --- | --- | --- |
| 本地 ONNX（`spring-ai-starter-model-transformers`） | jar 可下载（200） | **采用**。零 API 费用、离线可跑、演示不依赖外网 |
| DashScope / 智谱 embedding | 需额外 API Key | 用户未持有，排除 |
| OpenAI embedding | 需额外 API Key 且需外网 | 排除 |
| Ollama 本地模型 | jar 可下载（200） | 作为降级备选依赖保留 |

**本地 ONNX 方案实测说明**（来源：`https://docs.spring.io/spring-ai/reference/1.1/api/embeddings/onnx.html`）：

> The `TransformersEmbeddingModel` is an `EmbeddingModel` implementation that locally computes sentence embeddings using a selected sentence transformer. You can use any HuggingFace Embedding model. It uses pre-trained transformer models, serialized into the Open Neural Network Exchange (ONNX) format. The Deep Java Library and the Microsoft ONNX Java Runtime libraries are applied to run the ONNX models and compute the embeddings in Java.

**注意（待 M15 实测确认）**：该方案需要 ONNX 格式的模型文件与 tokenizer。Spring AI 默认会从网络下载模型（首次启动需要外网），也可通过 `optimum-cli` 预先导出并配合 `spring.ai.embedding.transformer.onnx.modelUri` 指向本地文件实现完全离线。M15 开始前必须先做一次最小验证，确认模型加载成功且 `embed()` 能返回向量。

**风险与对策**：若 ONNX 模型加载失败或效果不可接受，回退路径为 Ollama + 本地嵌入模型（依赖已引入，可随时切换），或改用 MySQL 关键词检索兜底 —— 三条路径都已具备可行性。

---

## 四、技术决策汇总

| 编号 | 决策 | 证据来源 |
| --- | --- | --- |
| D1 | 采用 Spring AI **1.1.8**，Spring Boot 保持 **3.4.5** | 官方 getting-started 原文 + 两个版本 starter POM 依赖对比 |
| D2 | 排除 Spring AI 2.0.1 | 其 POM 依赖 `spring-boot-starter-webclient` / `restclient`（Boot 4.x 专属） |
| D3 | Redis 换用 `redis/redis-stack-server:7.4.0-v8` | Docker Hub tags 实测 + 现有 `redis:7-alpine` 无 RediSearch |
| D4 | 嵌入模型用本地 ONNX | DeepSeek starter 无 embedding；jar 可下载；零成本离线 |
| D5 | 自定义 `MysqlChatMemoryRepository` | 默认内存仓储重启即丢；官方支持替换仓储并自动接管 |
| D6 | 显式传 `conversationId` | 1.1.6 破坏性变更原文 |
| D7 | 只新增 Flyway V13-V19，不改 V1-V12 | 项目硬约束 + 现有迁移历史 |
| D8 | 新建 `feat/ai-agent` 分支，不逐模块 push | 公开仓库 + 求职作品集 + 双机开发 |
| D9 | DeepSeek Key 走环境变量，加入 `.env.example` 模板 | `.gitignore` 已忽略 `.env` 与 `.env.*`，保留 `.env.example` |
| D10 | 保留 Ollama starter 作为降级依赖 | 断网或额度耗尽时保证演示不中断 |

---

## 五、Redis Stack 替换实战验证（2026-09-18 已完成）

本节记录 `redis:7-alpine` → `redis/redis-stack-server:7.4.0-v8` 的实际替换过程与验证结果。

### 5.1 镜像启动方式的坑（重要，必须遵守）

**redis-stack-server 镜像没有 Entrypoint，`Cmd` 是 `/entrypoint.sh`**：

```bash
docker inspect redis/redis-stack-server:7.4.0-v8 --format 'Entrypoint: {{json .Config.Entrypoint}} Cmd: {{json .Config.Cmd}}'
# Entrypoint: null
# Cmd: ["/entrypoint.sh"]
```

`/entrypoint.sh` 的职责是加载全部模块：

```sh
${CMD} \
${CONFFILE} \
--dir ${REDIS_DATA_DIR} \
--protected-mode no \
--daemonize no \
--loadmodule /opt/redis-stack/lib/rediscompat.so \
--loadmodule /opt/redis-stack/lib/redisearch.so ${REDISEARCH_ARGS} \
--loadmodule /opt/redis-stack/lib/redistimeseries.so ${REDISTIMESERIES_ARGS} \
--loadmodule /opt/redis-stack/lib/rejson.so ${REDISJSON_ARGS} \
--loadmodule /opt/redis-stack/lib/redisbloom.so ${REDISBLOOM_ARGS} \
--loadmodule /opt/redis-stack/lib/redisgears.so v8-plugin-path /opt/redis-stack/lib/libredisgears_v8_plugin.so ${REDISGEARS_ARGS} \
${REDIS_ARGS}
```

**踩坑记录**：最初在 `docker-compose.yml` 中写 `command: ["redis-server", "--appendonly", "yes"]`，这会**覆盖** `/entrypoint.sh`，导致所有模块不加载。表现为：

- `MODULE LIST` 返回空
- `FT._LIST` 报 `ERR unknown command 'FT._LIST'`
- 启动日志只有普通 Redis 启动信息，没有任何模块加载记录

**正确写法**：用 `REDIS_ARGS` 环境变量追加参数，绝不覆盖 `command`。

### 5.2 最终生效的 Compose 配置

```yaml
  # M13 起使用 Redis Stack：在 Redis 7.4 基础上内置 RediSearch 模块，
  # 这是 Spring AI 的 RedisVectorStore 做向量检索的前置条件。
  # 原来的 redis:7-alpine 没有该模块，向量索引无法创建。
  # 注意：镜像变了，6379 端口和协议不变，现有 RedisService 的 String/Set/ZSet 用法零改动。
  redis:
    image: redis/redis-stack-server:7.4.0-v8
    restart: unless-stopped
    environment:
      # 镜像自带的 /entrypoint.sh 负责 --loadmodule 加载 RediSearch 等模块，
      # 这里只通过 REDIS_ARGS 追加参数，千万不要用 command 覆盖 entrypoint，
      # 否则模块不会加载，FT.CREATE 等向量检索命令会报 unknown command。
      REDIS_ARGS: "--appendonly yes"
    ports:
      - "6379:6379"
    volumes:
      # 用独立的卷名，避免与旧的 redis:7-alpine 数据文件冲突
      - redis-stack-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 5s
```

新增卷声明：

```yaml
volumes:
  mysql-data:
  redis-stack-data:
  uploads-data:
```

### 5.3 模块加载验证结果

```bash
docker compose exec -T redis redis-cli MODULE LIST
```

| 模块名 | 版本 | 库文件 |
| --- | --- | --- |
| **search**（RediSearch） | **21020** | `/opt/redis-stack/lib/redisearch.so` |
| ReJSON | 20809 | rejson.so |
| timeseries | 11206 | redistimeseries.so |
| bf（Bloom） | 20816 | redisbloom.so |
| redisgears_2 | 20020 | redisgears.so |
| RedisCompat | 1 | rediscompat.so |

Redis 服务版本：**7.4.7**。

**结论**：RediSearch 2.10.20 可用，满足 Spring AI `RedisVectorStore` 的前置条件。

### 5.4 向量检索端到端验证

用原始 RESP 协议（Python 标准库 socket + struct，不依赖 redis-py）做完整验证，探针脚本位于 `/tmp/redis_vector_probe.py`。

**验证 1：小维度精确性验证（4 维，可人工核对）**

| 步骤 | 结果 |
| --- | --- |
| `FT.CREATE` HNSW / FLOAT32 / DIM 4 / COSINE | 成功 |
| 写入 4 条文档（含 16 字节 float32 二进制向量） | 成功 |
| 纯向量 KNN 检索 | 命中 3 条，距离排序正确：articleId=1(0.003620) → articleId=4(0.003620) → articleId=2(0.842935) |
| `@status:{PUBLISHED}` 过滤 | 命中 3 条，DRAFT 草稿被正确过滤 |
| `@categoryId:{20}` 过滤 | 命中 2 条，符合预期 |
| 混合过滤 `@status:{PUBLISHED} @categoryId:{10}` | 命中 2 条，符合预期 |
| `FT.INFO num_docs` | 4，与写入数一致 |
| `FT.DROPINDEX ... DD` 清理 | 残留 key 数 0 |

**验证 2：真实维度验证（768 维，ONNX 模型典型维度）**

| 步骤 | 结果 |
| --- | --- |
| `FT.CREATE` DIM 768 | 成功 |
| 写入基准 / 近似 / 远离三条归一化向量 | 成功 |
| KNN 检索排序 | articleId=1(距离 0.000000) → articleId=2(0.127120) → articleId=3(0.983072)，完全符合预期 |
| 清理 | 残留 key 数 0 |

**结论**：Redis Stack 的向量检索链路（索引创建、二进制向量写入、KNN 检索、metadata 过滤、混合过滤、索引清理）在真实维度下**完全可用**。

**顺带发现的细节**：RediSearch 把 KNN 距离以**十进制字符串**返回（如 `b'0.00362026691437'`），不是二进制 float32。Java 侧解析时需注意。

### 5.5 现有 Redis 能力兼容性验证

替换镜像后立即验证 `RedisService` 依赖的三类数据结构：

| 能力 | 命令 | 结果 |
| --- | --- | --- |
| String（浏览量） | `SET` / `GET` | `OK` / `ok` |
| ZSet（热榜） | `ZADD` / `ZREVRANGE ... WITHSCORES` | `1` / `1  1.5` |
| Set（点赞去重） | `SADD` / `SMEMBERS` | `1` / `1` |

**结论**：端口与协议未变，现有 `RedisService` 的 String / Set / ZSet 用法**零改动**，M11 指标同步能力不受影响。

### 5.6 三个中间件的最终状态

```bash
docker compose ps
# SERVICE    IMAGE                               STATUS
# mysql      mysql:8.0                           Up (healthy)
# rabbitmq   rabbitmq:3-management               Up (healthy)
# redis      redis/redis-stack-server:7.4.0-v8   Up (healthy)
```

宿主机端口监听：

| 端口 | 服务 | 状态 |
| --- | --- | --- |
| 3306 | MySQL（容器 3306 映射） | 监听中 |
| 6379 | Redis Stack | 监听中 |
| 5672 | RabbitMQ AMQP | 监听中 |
| 15672 | RabbitMQ 管理台 | 监听中 |

MySQL 验证：

- 版本：`8.0.46`
- 数据库 `bit_forum` 存在
- `flyway_schema_history` 有 **12 条**记录，V1 至 V12 全部 `success=1`，与代码库迁移文件一一对应
- 端口改回 3306 后数据完整性复核：5 个用户、9 篇文章，数据卷未受影响

RabbitMQ 验证：

- `rabbitmq-diagnostics -q ping` → `Ping succeeded`
- 当前队列列表为空（应用未启动，队列尚未声明）

**端口调整记录（2026-09-18）**：MySQL 宿主机端口已从 `3307` 改回 `3306`。

历史背景：`3307` 是为了避开 Windows 主机上已安装的本机 MySQL 而做的折中映射。当前 macOS 开发机 `3306` 空闲，改回后与 `application.yml` 和 `src/test/resources/application.yml` 的默认值一致，**本机启动后端与运行测试都无需再显式设置 `SPRING_DATASOURCE_URL`**。

```bash
# 现在无需设置 SPRING_DATASOURCE_URL，默认值即可生效
SPRING_DATASOURCE_PASSWORD=... SPRING_RABBITMQ_PASSWORD=... JWT_SECRET=... mvn test
```

### 5.7 DeepSeek API Key 配置方式

配置位置共三处，均已就位：

| 文件 | 状态 | 说明 |
| --- | --- | --- |
| `.env.example` | 已添加 | 模板含 `DEEPSEEK_API_KEY=sk-replace-with-your-deepseek-api-key`，会被提交 |
| `.env` | 已添加并填写完成 | 实际使用位置，**用户已填入真实 Key**（`sk-` 前缀，35 字符）；已被 `.gitignore` 忽略，实测确认不会入库 |
| `docker-compose.yml` 的 `app` 服务 | 已添加 | `DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY:-}`，Docker 部署时透传 |

本机开发（不走 Docker）时，后端通过 Spring Boot relaxed binding 自动读取 `.env` 中的 `DEEPSEEK_API_KEY`。

**安全约束**：`.env` 已被 `.gitignore` 第 47 行忽略（`git check-ignore -v .env` 验证通过），Key 不会进入 Git 历史。

---

## 六、待验证事项（M13-M18 期间必须实测）

| 编号 | 待验证内容 | 计划验证时机 | 当前状态 |
| --- | --- | --- | --- |
| T1 | Spring AI 1.1.8 在 Spring Boot 3.4.5 下真实启动无冲突 | M13 引入依赖后立即验证 | **已验证通过**（编译成功 + 189 项测试全绿） |
| T2 | Redis Stack 替换后现有 `RedisService` 全部功能正常 | M13 换镜像后跑 M11 相关测试回归 | **已验证通过**（`mvn test` 189 项全绿，含 M11） |
| T3 | ONNX 嵌入模型能成功加载并返回向量 | M15 开始前的最小验证 | **已验证通过**（见 6.8）：bge-base-zh-v1.5 量化版，维度 768，缓存后离线可跑 |
| T4 | DeepSeek Tool Calling 实际可用且有稳定的调用成功率 | M14 工具冒烟测试 | 待验证 |
| T5 | `RedisVectorStore` 元数据过滤在实际数据上生效 | M15 检索验证 | **已验证通过**（见 6.9）：封装层过滤生效，OFFLINE 内容未被召回 |
| T6 | 单轮对话的真实 Token 消耗与费用 | M13 结束后首次统计 | 待验证 |
| T7 | 结构化输出在 DeepSeek 上的稳定性 | M16 审核 Agent 验证 | 待验证 |
| T8 | 异步索引队列与既有 `ArticlePublishMessage` 队列不冲突 | M15 接入 RabbitMQ 时验证 | **已验证通过**（见 6.10）：独立队列 + 独立死信队列，端到端测试两条链路互不影响 |
| T12 | 工具调用链与单步耗时能否被采集 | M18 开工前的最小验证 | **已验证通过**（见 6.18）：循环在 provider 内部，最终响应无工具链；包装 `ToolCallback` 可采集（真实调用复验） |
| T13 | 执行轨迹能否覆盖异步链路（MQ + 线程池） | M18 开工前的最小验证 | **已验证通过**（见 6.19）：MQ 消息头透传、线程池显式包装，两条异步链路都能续写同一 trace |

### 6.1 T1/T2 验证执行记录（2026-09-18）

```bash
# 编译（验证依赖解析与 Spring Boot 3.4.5 兼容性）
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -s maven-settings.xml -DskipTests compile
# 退出码 0

# 完整测试回归
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=... \
SPRING_RABBITMQ_USERNAME=... SPRING_RABBITMQ_PASSWORD=... \
JWT_SECRET=... ./mvnw -s maven-settings.xml test
# Tests run: 189, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

依赖树确认（Spring AI 1.1.8 构件全部解析成功）：

```text
spring-ai-starter-model-deepseek:1.1.8
├── spring-ai-autoconfigure-model-deepseek:1.1.8
│   ├── spring-ai-autoconfigure-model-tool:1.1.8
│   ├── spring-ai-autoconfigure-retry:1.1.8
│   └── spring-ai-autoconfigure-model-chat-observation:1.1.8
├── spring-ai-deepseek:1.1.8
│   ├── spring-ai-model:1.1.8
│   │   ├── spring-ai-commons:1.1.8
│   │   └── spring-ai-template-st:1.1.8
│   └── spring-ai-retry:1.1.8
└── spring-ai-autoconfigure-model-chat-client:1.1.8
    └── spring-ai-client-chat:1.1.8
```

**结论**：T1、T2 均通过。不存在 Spring AI 与 Spring Boot 3.4.5 的冲突；Redis Stack 替换未破坏任何既有功能。

### 6.2 DeepSeek 自动配置的重要发现（M14+ 必须知道）

**`spring.ai.deepseek.chat.enabled` 不是控制 bean 创建的总开关。**

实测证据：

```bash
# 自动配置条件元数据：DeepSeekChatAutoConfiguration 只有 @ConditionalOnClass
cat META-INF/spring-autoconfigure-metadata.properties
# ...DeepSeekChatAutoConfiguration.ConditionalOnClass=org.springframework.ai.deepseek.api.DeepSeekApi

# 反编译确认：deepSeekChatModel() 方法上没有 @ConditionalOnProperty
javap -c -p DeepSeekChatAutoConfiguration
#   public DeepSeekChatModel deepSeekChatModel(...)   ← 无条件创建
#   private DeepSeekApi deepSeekApi(...)              ← 内部 Assert.hasText 校验 api-key
```

**表现**：只要 `spring-ai-starter-model-deepseek` 在类路径上，自动配置就会无条件创建
`deepSeekChatModel` bean；api-key 为空时抛
`IllegalArgumentException: DeepSeek API key must be set`，应用启动失败。
设置 `spring.ai.deepseek.chat.enabled: false` **无法阻止**该 bean 创建。

**应对策略**：

1. 测试环境在 `src/test/resources/application.yml` 提供占位 api-key，使 Spring 上下文能启动。
   测试不进行真实网络调用；需要验证 AI 逻辑时应使用 Spring AI 的 Mock 聊天模型。
2. 生产/开发环境 api-key 缺失会启动失败，与 datasource / rabbitmq / jwt 的既有约定一致
   （必需凭据缺失即快速失败），避免"看似启动成功、调用时才报错"。
3. 真正的"AI 不可用降级"应在调用层实现（M18 的 `AiDegradeGuard`），而不是依赖 starter 开关。

### 6.3 M11 既有测试脆弱性修复（Redis 换镜像后暴露）

替换 Redis 镜像并引入 Spring AI 后，`ArticleMetricSyncServiceTest` 有 2 个测试失败。
**深入排查确认这不是 Redis Stack 或 Spring AI 造成的回归，而是既有测试本身的脆弱假设**：

| 脆弱假设 | 真实现象 |
| --- | --- |
| 认为"数据库中只有本测试创建的已发布文章" | 实际库中有 4 篇历史 PUBLISHED 文章，`syncArticleMetrics()` 遍历全库 |
| 认为"Mockito 对未打桩方法返回 null" | **Mockito 对 `Long` 返回类型默认返回 `0L` 而非 `null`** |

诊断证据：

```text
>>> DIAG 已发布文章数 = 4
>>> DIAG 对 articleId=85 打桩返回 999
>>> DIAG articleId=85 (viewCount=130) -> mock views=999, mock likes=0     ← 打桩生效
>>> DIAG articleId=86 (viewCount=87)  -> mock views=0,   mock likes=0     ← 未打桩返回 0L！
>>> DIAG 未打桩的 articleId=999999999 -> 0                                ← 不是 null
```

由于 `syncSingleArticle` 用 `null` 表示"Redis 无该指标"，返回 `0L` 会被判定为
"指标值为 0"，导致 4 篇无关文章被更新，`updatedCount` 从期望的 1 变成 5。

**修复方式**：先用 `lenient()` 显式声明"库中已有已发布文章在 Redis 中无数据"，
再用 `eq()` 把打桩精确限定到测试创建的文章，使断言不再依赖数据库初始状态。
修复后 `mvn test` 189 项全绿。

### 6.4 M13 实施中发现的四个坑（M14+ 必须知道）

#### 坑 1：`@MapperScan` 不扫描子包

`BitForumSpringApplication` 上原本是 `@MapperScan("com.bitforum.mapper")`。
AI 域的 Mapper 放在 `com.bitforum.ai.mapper` 子包下，**不会被扫描到**，表现为
`No qualifying bean of type 'AiMessageMapper' available`，Spring 上下文启动失败。

**修复**：显式列出两个包

```java
@MapperScan({"com.bitforum.mapper", "com.bitforum.ai.mapper"})
```

#### 坑 2：`@ConditionalOnBean` 在用户配置类中不可靠

最初 `AiConfig` 用 `@ConditionalOnBean(DeepSeekChatModel.class)` 决定是否创建 ChatClient。
实测出现"`DeepSeekChatModel` bean 存在、但 `ChatClient` 未被创建"：
用户配置类处理时，自动配置的 bean 定义可能尚未注册，条件评估失败且**无任何警告**。

**修复**：改用属性条件，不依赖 bean 注册顺序

```java
@Bean
@ConditionalOnProperty(name = "spring.ai.deepseek.chat.enabled", havingValue = "true")
public ChatClient chatClient(DeepSeekChatModel deepSeekChatModel) { ... }
```

#### 坑 3：测试配置硬编码 api-key 会覆盖环境变量的真实 Key

这是最隐蔽的一个，只有真实调用才会暴露。现象：真实 Key 经 curl 验证有效（HTTP 200），
但应用调用返回 **HTTP 401 Authentication Fails, Your api key: ****alls is invalid**。

诊断输出（对比 Key 的形状而非内容，输出中已对真实 Key 脱敏）：

```text
>>> 环境变量 DEEPSEEK_API_KEY   = 长度 35, 首6=sk-***, 尾4=****   ← 真实 Key（形状正确）
>>> spring.ai.deepseek.api-key  = 长度 44, 首6=test-p, 尾4=alls    ← 被占位符覆盖
>>> DeepSeekConnectionProperties.apiKey = 长度 44, 首6=test-p, 尾4=alls
```

`test-placeholder-key-not-used-for-real-calls` 恰好 44 字符、尾 4 位 `alls`，
与 DeepSeek 报错中的 `****alls` 完全吻合，据此定位。

真实 Key 的任何片段都不应写入仓库文档；上述诊断只需长度与占位符特征即可定位问题。

**根因**：`src/test/resources/application.yml` 中把 `api-key` 写成了硬编码字符串，
其优先级高于环境变量，把真实 Key 覆盖掉了。

**修复**：改成带默认值的占位符，让环境变量优先

```yaml
api-key: ${DEEPSEEK_API_KEY:test-placeholder-key-not-used-for-real-calls}
```

**通用教训**：测试配置里凡是要被环境变量覆盖的值，都必须写成占位符形式，
不能写死字面量，否则会静默屏蔽外部传入的真实配置。

#### 坑 4：同一个测试类的多个方法共用 Spring 上下文与 bean 实例

`AgentOrchestratorTest` 中的测试 Agent 是有状态的（记录调用次数与最近一次回答）。
初始未重置状态，导致：调用计数跨方法累加到 3、上一轮设置的降级回答残留到下一个测试。

**修复**：显式重置

```java
@BeforeEach
void resetTestAgent() {
    testRoutingAgent.reset();
}
```

#### 附带发现：测试用 Agent 不要用 Mockito mock

`AgentOrchestrator` 在**构造阶段**就读取每个 Agent 的 `type()` 建立路由表，
而 `@MockitoBean` 的打桩在该时点尚未生效，`type()` 返回 `null`，
导致 Agent 被跳过、路由表为空（表现为运行时报"没有任何可用的 Agent 实现"）。
改用真实实现类（`type()` 硬编码）即可，与打桩时序无关。

**顺手加固**：`AgentOrchestrator` 现在会显式跳过 `type()` 为 null 的实现并打警告，
避免这类实现错误变成静默失效。

### 6.5 M13 真实链路验证（mock 测试覆盖不到的部分）

新增 `DeepSeekSmokeTest`，默认跳过，仅在 `DEEPSEEK_CHAT_ENABLED=true` 时执行：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2-)"
./mvnw -Dtest=DeepSeekSmokeTest test
```

**验证结果**：通过。确认了 ChatClient 装配、API Key 生效、网络可达、模型能返回内容。

**辅助排查手段**：用 curl 直连官方端点可快速区分"Key 问题"与"应用配置问题"

```bash
curl -s https://api.deepseek.com/models -H "Authorization: Bearer $DEEPSEEK_API_KEY"
# 200 + 模型列表 => Key 有效，问题在应用侧
```

**顺带发现**：当前 Key 可访问的模型为 `deepseek-flash` 与 `deepseek-v4-pro`。
默认 `deepseek-chat` 调用能成功（服务端实际返回 `deepseek-flash`），
M14-M16 如需指定模型，应以 `/models` 返回的 id 为准。

### 6.6 M14 实施中发现的关键设计问题：工具的用户身份绝不能由模型传

**现象**：M14 首版把 `userId` 设计成工具参数（`@ToolParam(description = "当前登录用户的 id")`）。
真实调用后 AI 的回复是：

> 点赞是写操作……**请提供你的用户 id**（当前登录用户的 id），我才能执行这次点赞。
> 工具要求必须传入当前登录用户 id，我这边拿不到你的账号信息，也没法猜。

**问题本质**：当前登录用户是**应用侧已知信息**（JWT 里就有，`LoginInterceptor` 已解析为
`request attribute`），却被推给模型去索要。后果有三：
1. 用户体验荒谬 —— 系统自己知道用户是谁，却问用户要 id；
2. 可靠性风险 —— 模型可能传错 id，或从对话里"推断"出一个错误身份；
3. 安全边界模糊 —— 身份本应不可协商，不应出现在模型的参数空间里。

**正确做法**：Spring AI 提供 `ToolContext`，作为**隐式参数**注入，不进入暴露给模型的 schema。
官方文档示例：

```java
@Tool(description = "Retrieve customer information")
Customer getCustomerInfo(Long id, ToolContext toolContext) {
    return customerRepository.findById(id, toolContext.getContext().get("tenantId"));
}
```

**落地方式**：

```java
// 工具侧：接收 ToolContext，从中取身份
public ActionResult likeArticle(
        @ToolParam(description = "要点赞的文章 id，必须来自搜索结果") Long articleId,
        ToolContext toolContext) {
    Long userId = currentUserId(toolContext);
    ...
}

// Agent 侧：调用模型时注入
Map<String, Object> toolContext = new HashMap<>();
toolContext.put(AgentContextKeys.USER_ID, context.userId());
chatClient.prompt().messages(messages).tools(tools).toolContext(toolContext).call();
```

**验证方式与一个容易误判的坑**：修复后首次在**旧会话**复测，AI 仍在索要 userId。
原因不是修复失效，而是该会话的历史里已经留下了"工具要求传入 userId"的旧结论，
模型在延续上下文行为。**改用全新会话复测，AI 直接执行了点赞并返回真实结果**。
教训：涉及提示词/工具 schema 的改动，必须用干净会话验证，否则会被历史上下文误导。

### 6.7 M14 工具调用的成本观察

工具调用会显著提高单轮 token 消耗，原因有二：工具的 JSON Schema 会随每次请求发送，
以及工具返回结果会进入上下文。

| 场景 | token 消耗 |
| --- | --- |
| M13 纯对话（无工具） | 1,842 |
| M14 一次工具调用（搜索文章） | 7,823 |
| M14 一次写操作（点赞，先读详情再执行） | 8,893 |

**结论**：M18 的 Token 预算与成本统计必须考虑工具 schema 的固定开销。
若工具数量继续增长（M16 审核、M17 运营各自会增加工具），需要评估
"按 Agent 装配工具子集"的实际收益 —— 当前 `ToolRegistry` 已按类型隔离，
正是为控制这一开销所做的设计。

### 6.8 T3 验证执行记录：ONNX 嵌入模型实测（2026-09-19，M15 开工前）

**结论：T3 通过。** 本地 ONNX 嵌入模型能在 arm64 + JDK 17 下加载、产出稳定向量，缓存后可完全离线复跑。
M15 的最大技术风险解除，两条回退路径（Ollama 本地嵌入 / MySQL 关键词兜底）无需启用。

#### 6.8.1 引入依赖后的实测发现

加入 `spring-ai-starter-model-transformers` 后，反编译 `spring-ai-autoconfigure-model-transformers:1.1.8` 确认：

- `TransformersEmbeddingModelAutoConfiguration` 的自动配置元数据**只有 `ConditionalOnClass`、没有 `ConditionalOnProperty`**
  （与 6.2 记录的 DeepSeek 是同一个模式）。因此只要依赖在类路径上，**每个 Spring 上下文启动都会构建 ONNX 会话并加载模型文件**，无法用配置开关关闭。
- 默认模型地址指向 **GitHub 而不是 HuggingFace**，两个地址实测均可直连（HTTP 206，约 1s 响应，无需代理）：
  - tokenizer：`raw.githubusercontent.com/spring-projects/spring-ai/main/models/spring-ai-transformers/src/main/resources/onnx/all-MiniLM-L6-v2/tokenizer.json`
  - model：`media.githubusercontent.com/media/spring-projects/spring-ai/refs/heads/main/models/spring-ai-transformers/src/main/resources/onnx/all-MiniLM-L6-v2/model.onnx`
  - 但两者都指向上游 **main 分支**，会随上游提交漂移 → 因此本项目在配置里显式写死 URI，不用默认值。
- 默认缓存目录是 `${java.io.tmpdir}/spring-ai-model-cache`（系统临时目录，macOS 会定期清理），
  已改为持久的 `${user.home}/.cache/bitforum-onnx`。

启动日志实证（首次运行）：

```text
o.s.a.transformers.ResourceCacheService : Create cache root directory: /Users/jiu/.cache/bitforum-onnx
o.s.a.transformers.ResourceCacheService : Caching the URL [...tokenizer.json] resource to: ...
o.s.a.transformers.ResourceCacheService : Caching the URL [...model.onnx] resource to: ...
o.s.a.t.TransformersEmbeddingModel       : Model input names: input_ids, attention_mask, token_type_ids
o.s.a.t.TransformersEmbeddingModel       : Model output names: last_hidden_state
```

#### 6.8.2 启动耗时：首次 vs 缓存命中

| 场景 | 实测结果 |
| --- | --- |
| 首次（含下载 tokenizer 712KB + model 90.4MB） | Spring 上下文启动 **52.16 秒** |
| 缓存命中后（日志中不再出现 `Caching the URL`） | Spring 上下文启动 **2.24 秒** |

**结论**：模型加载只在首次需要联网，之后每次上下文启动仅约 2 秒，属于可接受成本。

#### 6.8.3 嵌入模型的 pooling 约束（决定选型必须实测）

`TransformersEmbeddingModel` 内部只有私有方法 `meanPooling(NDArray, NDArray)`，
**pooling 方式硬编码为 mean、不可配置**。而不同模型的训练约定不同
（BGE 系列用 CLS pooling，多语言 MiniLM / e5 用 mean pooling），
所以"模型名气"不能作为选型依据，必须实测。

#### 6.8.4 三模型检索质量对比（同一批中文样本）

评测工具：`EmbeddingModelComparisonProbe`。5 条样本，每条为「中文查询 + 1 段相关文档 + 3 段干扰文档」，
判断相关文档能否被排到第一位（Top-1），并记录「相关分 − 最高干扰分」的平均差距（差距越大，排序越稳、越抗噪）。

| 模型 | 维度 | 体积 | Top-1 命中 | 平均区分度差距 |
| --- | --- | --- | --- | --- |
| all-MiniLM-L6-v2（英文，Spring AI 默认） | 384 | 90MB（fp32） | **2/5** | **−0.0507** |
| **bge-base-zh-v1.5（Xenova，动态量化）** | **768** | **102MB** | **5/5** | **+0.1914** |
| multilingual-e5-small（Xenova，动态量化） | 384 | 118MB | 5/5 | +0.0493 |

英文模型的典型失败例子：查询「网站打开很慢怎么办」，相关文档得分 0.3963，
而无意义干扰项「如何更换主题颜色」得分 0.5900 —— **相关文档反而排在后面**，说明英文模型不能用于中文检索。

**选型结论：采用 `Xenova/bge-base-zh-v1.5` 的 `onnx/model_quantized.onnx`。** 理由：

1. 中文检索质量明显最优，平均区分度约为 e5 的 4 倍（e5 虽然 Top-1 也对，但所有分数挤在 0.85~0.94，排序脆弱）；
2. 量化版 102MB 且为**通用动态量化**，不绑定 CPU 架构，双机（Windows / MacBook）开发无需重新导出；
3. 即便 pooling 方式与 BGE 官方约定不一致（mean vs CLS），实测仍大幅领先，因此不引入自研实现。

**遗留优化点**：BGE 官方建议给查询加检索指令前缀「为这个句子生成表示以用于检索相关文章：」。
该处理应在 `RagService` 实现时应用到 **query 一侧**（passage 一侧不加），届时用真实站内文章复测。

#### 6.8.5 对 M15 后续实现的硬约束

1. **Redis 向量索引 `DIM` 必须写 768**，与选定模型一致；写错会导致 `FT.CREATE` 失败（呼应 3.2 节约束）。
2. 每个 Spring 上下文启动都会加载 ONNX 会话（缓存命中时约 2 秒），全量测试的启动开销以此为基线；
   模型文件已缓存时**不需要网络**。
3. 更换模型只改 `src/main/resources/application.yml` 与 `src/test/resources/application.yml` 两处的
   `model-uri` / `tokenizer.uri`，但**必须同步修改 Redis 索引 DIM**，
   并重跑 `OnnxEmbeddingSmokeTest`（维度与区分度下限）与 `EmbeddingModelComparisonProbe`（排序质量）。

### 6.9 M15 向量库接入实测：自动配置在本项目不可用，必须自己声明 bean（2026-09-19）

**结论**：Redis Stack 向量库已接通，索引维度 768、HNSW + COSINE、元数据字段按 TAG/NUMERIC 正确声明；
**T5（元数据过滤在 Spring AI 封装层生效）验证通过**。

#### 6.9.1 关键发现：starter 的自动配置有两个硬限制

反编译 `spring-ai-starter-vector-store-redis:1.1.8` 的 `RedisVectorStoreAutoConfiguration`：

**限制一：它要求容器里存在 `JedisConnectionFactory`。**

```text
public RedisVectorStore vectorStore(EmbeddingModel, RedisVectorStoreProperties,
        org.springframework.data.redis.connection.jedis.JedisConnectionFactory,
        ObjectProvider<ObservationRegistry>, ObjectProvider<VectorStoreObservationConvention>,
        BatchingStrategy)
```

本项目用的是 `spring-boot-starter-data-redis` 默认的 **Lettuce**，
Spring Boot 不会同时创建 Jedis 连接工厂（`RedisConnectionFactory` 已存在），因此自动配置拿不到依赖。

**限制二：它不支持声明元数据字段类型。**

builder 调用链里只有 `initializeSchema` / `observationRegistry` / `batchingStrategy` / `indexName` / `prefix`，
**没有 `metadataFields`**；配置元数据里也只有 3 个属性：

```text
spring.ai.vectorstore.redis.index-name          | default = 'default-index'
spring.ai.vectorstore.redis.initialize-schema   | default = None
spring.ai.vectorstore.redis.prefix              | default = 'default:'
```

而 RediSearch 要求所有出现在过滤表达式里的元数据字段必须在建索引时显式声明类型（见 3.2）。

**应对**：在 `com.bitforum.ai.config.VectorStoreConfig` 中自己声明 `JedisPooled` 与 `RedisVectorStore` bean，
元数据字段显式声明；连接参数复用 Spring Boot 的 `spring.data.redis.*`，不引入第二套连接配置。
自动配置的 bean 带 `@ConditionalOnMissingBean`，会自觉让路，不会产生重复 bean。

#### 6.9.2 实测索引定义（`FT.INFO bitforum-kb`）

```text
[identifier, $.content,     attribute, content,     type, TEXT,  WEIGHT, 1]
[identifier, $.embedding,   attribute, embedding,   type, VECTOR, algorithm, HNSW,
 data_type, FLOAT32, dim, 768, distance_metric, COSINE, M, 16, ef_construction, 200]
[identifier, $.articleId,   attribute, articleId,   type, TAG, SEPARATOR, ]
[identifier, $.categoryId,  attribute, categoryId,  type, TAG, SEPARATOR, ]
[identifier, $.status,      attribute, status,      type, TAG, SEPARATOR, ]
[identifier, $.publishTime, attribute, publishTime, type, NUMERIC]
[identifier, $.chunkIndex,  attribute, chunkIndex,  type, NUMERIC]
```

维度 768 与嵌入模型一致；`status` 声明为 TAG 后可直接用于 `status == 'PUBLISHED'` 过滤。

#### 6.9.3 T5 验证：元数据过滤在封装层生效

`RedisVectorStoreSmokeTest` 写入 3 条向量（2 条 PUBLISHED、1 条 OFFLINE），
用 `SearchRequest.builder().filterExpression("status == 'PUBLISHED'")` 检索：

```text
>>> 过滤检索返回 2 条，命中文章 = [9001, 9002]     ← OFFLINE 的 9003 未被召回
```

#### 6.9.4 小坑记录

jedis 的 `ftInfo` 返回的 `attributes` 是**扁平的键值列表**
（`[identifier, $.embedding, attribute, embedding, ..., dim, 768, ...]`）而**不是 `Map`**，
解析时必须按相邻两元素配对；首版按 Map 解析导致断言失败。

#### 6.9.5 新增数据库对象

V14 已应用（`flyway_schema_history` version=14、success=1）：`ai_kb_document`、`ai_kb_chunk`。

### 6.10 M15 异步索引的两个实现要点（T8 验证）

**T8 结论：通过。** 知识库索引使用独立队列 `article.kb.index.queue` 与独立死信队列
`article.kb.index.dlq`（复用同一个 `article.exchange`，靠 routingKey 区分），
与既有的 `article.publish.queue` 完全隔离；端到端测试实测两条链路各自独立工作。

#### 6.10.1 消息必须在事务提交后发送（否则会永久漏索引）

`ArticleService.approve()` / `offline()` / `delete()` 都是 `@Transactional`。
若在事务内直接 `convertAndSend`，消费者可能在事务提交前就处理消息：

```text
approve() 事务内：article.status 仍是 PENDING
   → 消费者 selectById 读到 PENDING
   → 判定「不该在知识库中」→ removeArticle
   → 事务随后提交，文章变成 PUBLISHED
   → 但不会再触发索引 ⇒ 这篇文章永远不会被索引
```

**应对**：`sendKbIndexMessage` 通过 `TransactionSynchronizationManager` 注册 `afterCommit` 回调，
事务提交后才投递。测试类 `KbIndexTriggerTest` 因此**刻意不加 `@Transactional`** ——
测试事务一旦回滚，afterCommit 根本不会执行，就验证不到真实行为。

#### 6.10.2 消费者按文章当前状态决定写入还是移除

消息只携带 `articleId`，消费者重新查库后再决定：

| 消费时的文章状态 | 动作 |
| --- | --- |
| `PUBLISHED` | `indexArticle` 写入/更新索引 |
| 其它状态 | `removeArticle` 移出知识库 |
| 查不到（已删除） | `removeArticle` 移出知识库 |

这样「审核通过 / 下架 / 删除」三个触发点共用一种消息类型，
消息堆积后也能按最新状态收敛，不会出现"先入队再回滚"造成的不一致。

#### 6.10.3 端到端实测

`KbAutoIndexIntegrationTest` 通过真实 RabbitMQ 验证：

```text
审核通过 → 异步入库（轮询等待，实测秒级完成）→ INDEXED + 分块记录一致 → 可被语义检索召回
下架     → 异步移除 → 文档记录消失 → 检索结果中不再出现
```

### 6.11 CI 环境配置漂移：push 后 backend job 失败（2026-09-19）

**现象**：M15 完成后首次 push `feat/ai-agent`，GitHub Actions 的 backend job 失败，
汇总行为 `Tests run: 278, Failures: 0, Errors: 226, Skipped: 2`。

**根因**（CI 日志原文）：

```text
Error creating bean with name 'vectorStore' defined in class path resource
[com/bitforum/ai/config/VectorStoreConfig.class]:
ERR unknown command 'FT._LIST', with args beginning with:
    at org.springframework.ai.vectorstore.redis.RedisVectorStore.afterPropertiesSet(RedisVectorStore.java:406)
```

CI 的 redis service 用的是 **`redis:7-alpine`（不含 RediSearch 模块）**，
而 M15 引入的 Redis 向量库需要 `FT.*` 命令；`initialize-schema: true` 使 bean 初始化时
就执行 `FT._LIST` 检查索引 → 命令不存在 → `vectorStore` 创建失败 →
依赖它的 `kbIndexService` / `ragService` / `qaAgent` 依次失败 → **Spring 上下文根本起不来**。

**226 个 errors 不是 226 个缺陷**：Spring Test 的 "context failure threshold (1) exceeded"
机制在第一次加载失败后跳过后续重试并直接计为错误，日志里的绝大多数条目都只是同一根因的连锁。

**本地复现**（确认因果链，排除模型下载/数据库等其他可能）：

```bash
docker run -d --name bitforum-redis-plain -p 6380:6379 redis:7-alpine
SPRING_DATA_REDIS_PORT=6380 ./mvnw -Dtest=RedisVectorStoreSmokeTest test
# 相同的异常链：qaAgent → ragService → vectorStore → ERR unknown command 'FT._LIST'
```

**为什么会漏**：M13 把本地 Redis 换成 `redis/redis-stack-server:7.4.0-v8` 时**没有同步更新 CI 配置**；
而 CI 只在 push 时触发，M13-M15 期间的 16 个提交一直没有推送，配置漂移长期不可见。
本地始终是 Stack 版本，因此本地全量测试一直是绿的。

**修复**：`.github/workflows/ci.yml` 的 redis service 改为 `redis/redis-stack-server:7.4.0-v8`。
不写 `command` 覆盖——该镜像 Cmd 是 `/entrypoint.sh`，覆盖会导致模块不加载（见 5.1）。

**修复验证**：用一个**全新的** Redis Stack 容器（0 索引，等同 CI 的干净环境）跑全量测试 →
**278 项通过**；索引 `bitforum-kb` 被自动创建且 `dim = 768`。

**CI 实测**：推送后 run `35422730508` 的 backend 与 frontend 两个 job **均通过**；
backend 日志为 `Tests run: 278, Failures: 0, Errors: 0, Skipped: 2` 与 `BUILD SUCCESS`，
且可见 ONNX 模型在 runner 上成功下载（`/home/runner/.cache/bitforum-onnx`）、索引自动创建。

**教训**：引入新的中间件能力（如 RediSearch）时，必须同步检查 CI 与部署环境是否具备该能力。
**本地测试全绿不能证明 CI 可用** —— 两者的中间件镜像可能不同；环境配置属于代码的一部分，
应与依赖变更一起提交。

### 6.12 T7 验证执行记录：DeepSeek 结构化输出（M16 前置验证，2026-09-19）

**技术结论：通过。** Spring AI 的 `ChatClient...entity(Class)` 能把模型返回结构化结果稳定映射为 Java record：
两次独立实验各 8 条样本，**解析成功 8/8、字段完整 8/8**（四维度齐全、decision 取值合法、confidence 在 0~1）。
因此 M16 审核 Agent 的技术路径成立，**不需要**回退到"手工解析 JSON"。

**但判断质量完全取决于提示词**，两套提示词的对照实验差异显著：

| 提示词 | 解析成功 | 字段完整 | 决策与预期相符 |
| --- | --- | --- | --- |
| A 保守版（只说"拿不准就转人工"，不给 REJECT 判据） | 8/8 | 8/8 | **5/8** |
| B 明确判据版（逐条写出 REJECT 的适用情形） | 8/8 | 8/8 | **8/8** |

A 版的具体偏差（模型倾向把明确违规也判成 REVIEW）：

| 样本 | A 保守版 | B 明确判据版 | 说明 |
| --- | --- | --- | --- |
| 明显广告（加微信领课程） | REVIEW | REJECT | A 版只提示"建议转人工" |
| 人身攻击 | REVIEW | REJECT | 同上 |
| 疑似诈骗（索取身份证/银行卡） | REVIEW | REJECT | 同上 |
| 灌水（连续重复字） | REVIEW | PASS | 是否违规取决于社区规则 |
| 软性引流（"方法在我主页"） | REVIEW | REJECT | 同上 |

**三个关键发现**：

1. **"总是转人工"不是模型能力不足**：B 版证明模型完全有能力按判据给出 REJECT，
   A 版的偏差是提示词没给判据导致的（模型把"保守"理解成"一律交给人"）。
2. **判据写成清单后会变得机械**：B 版把"软性引流"这种模糊表述直接判 REJECT，
   存在误伤正常内容分享的风险。判据的松紧需要在更多真实样本上继续调。
3. **灌水的处理属于产品决策**：A 版判 REVIEW、B 版判 PASS，本项目尚未定义灌水规则，
   因此这不构成模型错误，需要先定规则。

**实现定型后的复验**：上面的临时探针验证的是「结构化输出」这一**通用能力**。
当 M16 实现定型为五维固定字段（`ModerationAssessment`）后，改由 `ModerationAgentSmokeTest`
对**真实实现**复验：8 条样本 8/8 成功，决策分布
`[PASS, REJECT, REJECT, REJECT, PASS, REJECT, REJECT, REVIEW]`，
广告 / 人身攻击 / 疑似诈骗均正确判 REJECT，正常内容未被误伤 —— A 版的「一律转人工」问题已解决。
临时探针已完成使命并被移除，其对照数据保留在本节。

**复现方式**（消耗真实 API 额度，默认跳过）：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2-)"
./mvnw -Dtest=ModerationStructuredOutputProbe test
```

**待决策的产品策略**（不属于技术验证范畴，见 `m16-decision-brief.md`）：
审核三档决策的判据与松紧、灌水处理规则、自动放行开关与阈值、评论事后检测的语义。

---

### 6.13 T9 验证执行记录：不定长推荐列表的结构化理由（M17 前置验证，2026-09-19）

**为什么验这一件事**：M13-M16 里模型只输出过两种形态 —— 自由文本（QA）与**固定字段**结构
（`ModerationAssessment`，五维写死）。T7（6.12）已实测得出"让模型返回**列表**时元素数量不稳定
（4/5/6 个都出现过）"，M16 因此改用固定字段。而 M17 的 RecommendAgent 本质上**必须**输出一个
不定长列表（Top-N 文章 + 每篇一句理由），这是本项目唯一没验证过的输出形态，
所以在写任何 M17 业务代码之前先把它测掉。

**技术结论：通过。** 6 轮真实调用（`deepseek-flash`，temperature 0.7）全部无缺陷：

| 方案 | 条数 | 解析 | id/序号越界（幻觉） | 重复 | 缺失/空理由 | 单轮耗时 |
| --- | --- | --- | --- | --- | --- | --- |
| A：模型自选文章 + 写理由（`{articleId, reason}`） | 8/8 × 3 轮 | 3/3 | 0 | 0 | 0 | 1708 / 1801 / 1737 ms |
| B：Java 定序，模型只按序号写理由（`{index, reason}`） | 8/8 × 3 轮 | 3/3 | 0 | 0 | 0 | 1645 / 2038 / 2045 ms |

探针输入：模拟三路召回融合后的候选池 15 篇（含本地库真实已发布文章标题 5 篇
+ 合成技术文章标题 10 篇，后者如实声明为构造数据），要求输出 Top-8；
另给一份用户画像（收藏过 2 篇、关注 2 位后端/中间件作者、近期浏览集中在中间件/后端/数据库）。

**三个关键发现**：

1. **与 T7 的"数量不稳"不矛盾**：T7 没有约束条数，本次把「必须恰好 N 篇」写进系统提示词
   并给出明确候选清单后，两种方案都稳定给出 8 条。
   → **对 M17 的硬约束：推荐提示词必须写死条数，并用固定结构承载结果**（记录型/固定字段，
   循环次数由调用方给定，而不是让模型自己决定要写几条）。
2. **A 方案也没有出现 id 幻觉**：3 轮选出的文章高度重合且全都来自候选池，
   说明"编造文章 id"在候选池 ≤15 篇且带标题时并不容易发生。
   但 A 方案有三点结构性代价：模型要读完整个候选池（**token 与费用随候选池线性增长**）、
   两次运行结果不完全一致（**评测波动里混入模型随机性，无法归因**）、
   以及候选池变大后幻觉风险无法保证（本次只测了 15 篇）。
3. **B 方案在结构上消灭了幻觉**：模型完全接触不到文章 id，输入恒定为 N 篇
   （**成本与候选池规模解耦**），召回与排序由 Java 决定因而完全可复现。
   代价是模型不能补充 Java 召回之外的文章。

**推荐数据现状实测（同一轮，本地库/Redis 实测）**：

| 对象 | 实测数量 | 对 M17 的意义 |
| --- | --- | --- |
| 已发布文章 PUBLISHED | **5** | 推荐候选池的全部来源；**Top-10 凑不出 10 篇** |
| 全部文章 | 10 | 其余为 DRAFT/PENDING/REJECTED/OFFLINE，不应进推荐 |
| 用户 | 5 | |
| 收藏记录 | 7 | 唯一能表达"兴趣"的显式行为 |
| 关注关系 | 4 | 第三路召回规模 |
| 热榜 ZSet `article:hot` 成员 | **2**（分值 3、1） | 第二路召回实际只有 2 篇可用 |
| 向量索引 `bitforum-kb` | `num_docs=0`，`bitforum:kb:*` 键数 0 | 第一路召回当前为空，需先全量重建 |
| 评论 | 4 | |

**结论：M17 当前最大的障碍不是技术，而是数据量与评估口径** ——
候选池不足 Top-10、真值样本（7 条收藏）不足以支撑比例型指标、三路召回实际只有一路有数据，
且 5 篇候选上纯热榜基线与推荐结果极可能重合、对比无区分度。
因此 M17 的验收口径与数据准备必须先决策，见 `m17-decision-brief.md`（Q1-Q5）。

**复现方式**（消耗真实 API 额度，默认跳过）：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2-)"
./mvnw -s maven-settings.xml -Dtest=RecommendReasonProbe test
```

探针文件 `src/test/java/com/bitforum/ai/recommend/RecommendReasonProbe.java` 暂时保留：
M17 实现定型后，按 T7 的先例**改造成对真实 `RecommendAgent` 复验的冒烟测试**
（`RecommendAgentSmokeTest`）再移除本探针。

---

### 6.14 T10 验证执行记录：AnalystAgent 的「数字保真」（M17 前置验证，2026-09-19）

**为什么验这一件事**：AnalystAgent 的思路是把 `AdminDashboardService` 的统计包装成工具交给模型写洞察。
真正会翻车的地方不是"模型会不会写文章"，而是**它会不会把数字说错** ——
运营报告里的每个数字都来自工具返回的 JSON，模型一旦记错、算错或编造，管理员很难发现。
M14 只验证过"工具能被正确调用"，没验证过"工具返回一大段统计后，模型引用数字是否可靠"。

**技术结论：通过。** 2 轮真实调用（`deepseek-flash`）：

| 轮次 | 调用工具 | 输出中的数字 | 命中统计 | 未命中 | 耗时 | prompt / completion tokens |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 是（1 次） | 10 | **10** | 无 | 4780 ms | 1553 / 715 |
| 2 | 是（1 次） | 12 | **10** | `20`、`93` | 4505 ms | 1553 / 713 |

- 工具返回值实测只有 **699 字符**（完整看板统计的 JSON），约 300 token ——
  "工具返回对象过大压垮上下文"的担心**不成立**（promptTokens 1553 里大头是工具定义本身）。
- 第 2 轮未命中的两个数字 `20`、`93` 经人工核对是**合理派生值**（待审核占比 2/10 = 20%、
  未读通知占比 41/44 ≈ 93%），**不是幻觉**。
- 模型主动声明了数据缺口（"用户增长趋势、访问量等工具未提供，暂无该项数据"），
  没有编造工具没给的数据 —— 这正是提示词里"数字纪律"那一条要的效果。

**结论**：AnalystAgent 的技术路径成立，提示词必须保留"数字只能来自工具返回、缺数据就说暂无"这条硬约束。

复现（消耗真实 API 额度，默认跳过）：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2-)"
./mvnw -s maven-settings.xml -Dtest=AnalystInsightProbe test
```

探针文件 `src/test/java/com/bitforum/ai/analyst/AnalystInsightProbe.java` 暂时保留，
M17 实现定型后按 T7 先例改造成正式冒烟测试或移除。

---

### 6.15 T11 验证执行记录：向量召回的区分度（M17 前置验证，2026-09-19）

**为什么验这一件事**：M17 推荐的第一路召回计划复用 M15 的 `RagService`。
但 RagService 是为**问答检索**调优的 —— 它的相似度下限 0.5 是在"用户提问 → 文章片段"
这种语义落差较大的场景下定的；而推荐要做的是"**文章 → 文章**"，
两边表达同一类内容，阈值是否会把召回全部滤掉，只能实测。

**结论：阈值不是问题，但区分度是问题。**

**（1）索引重建实测（也是演示前必须做的动作）**：

当前 `bitforum:kb:*` 为空（`num_docs=0`）。执行全量重建：
已发布 5 篇 → 索引 5、跳过 0、失败 0、耗时 **400 ms**；
知识库统计 `documents=5 chunks=6 embeddingModel=bge-base-zh-v1.5`。
**索引重建后 M17 的第一路召回才有数据可用。**

**（2）阈值 0.5 不会滤掉跨文章召回**：所有文章间的相似度都在 **0.66~0.78**，
远高于 0.5，`RagService` 默认配置下每篇都能召回 4~5 篇。

**（3）真正的问题：区分度极低**。以《Redis 热点数据同步方案》(id=87) 为查询的实测分数：

| 候选 | 相似度 | 说明 |
| --- | --- | --- |
| 87 自身 | 0.8942 | |
| 86《React 与 Vite 前端开发笔记》 | **0.7179** | 前端，主题无关 |
| 85《Spring Boot 论坛项目实践》 | 0.7131 | 后端，语义最近 |
| 88《社区使用指南》 | 0.6897 | |
| 749《Spring 和 Spring Boot 到底是什么关系？》 | 0.6845 / 0.6770 | |

**语义上最相关的（85）与最不相关的（86）只差 0.0048** ——
若按相似度排序，Redis 文章会把《React 与 Vite 前端开发笔记》排在《Spring Boot 论坛项目实践》前面。
也就是说：**在当前数据下，"向量相似"这一路做主题召回几乎等于随机排序**。

**（4）原因已定位：正文长度不足**。实测已发布文章的正文长度：

| 文章 id | 正文长度 | 内容性质 |
| --- | --- | --- |
| 85 | 67 字 | 测试占位（"……适合测试搜索、详情……"） |
| 86 | 46 字 | 测试占位 |
| 87 | 44 字 | 测试占位 |
| 88 | 53 字 | 测试占位 |
| 749 | **621 字** | 唯一内容充实的文章 |

其余 4 篇的正文本身就是"用于测试某某功能"的一句话，可嵌入的信息几乎只剩标题。
**这解释了相似度全部挤在 0.67~0.75 的现象**，也说明：即使把文章数量扩到 40 篇，
如果正文仍是几十字的占位文本，向量召回的区分度依然不足以支撑推荐排序。

**对 M17 的硬约束**：

1. 数据准备不能只扩**数量**，必须扩**内容长度** —— 否则第一路召回形同虚设；
2. 推荐不能只靠向量相似度排序，热度与关注两路（以及受欢迎程度等确定性信号）必须参与融合，
   这与 task_plan 的"三路召回融合"设计一致，只是**权重不能平均**；
3. 召回后必须**排除查询文章自身**（实测自身相似度 0.85~0.89 恒为最高，不排除会把当前文章推荐给自己）；
4. 候选池只有 5 篇时 `topK=5` 等于全量返回，"召回率"这类指标在此规模下没有意义。

复现（会重建 Redis 向量索引，默认跳过）：

```bash
export M17_VECTOR_PROBE=true
./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test
```

探针文件 `src/test/java/com/bitforum/ai/recommend/RecommendVectorRecallProbe.java` 暂时保留。

---

### 6.16 M17 实施中发现的既有数据结构约束：点赞无法反查（2026-09-19）

**结论：M6 的点赞数据只支持"某篇文章被谁点赞"，不支持"某个用户点赞过哪些文章"。**

证据：

- `RedisService.like(articleId, userId)` 写入的是 Set 键 **`article:{articleId}:likes`**，成员是 userId；
  `hasLiked` / `unlike` / `getLikeCount` 也都围绕这个方向的键。
- 全项目 Redis 键模式只有四种：`article:{id}:views`、`article:{id}:likes`、`article:hot`、
  `mq:processed:article_publish:{id}`（遍历 `RedisService` 全文确认）。
- **不存在** `user:{id}:likes` 这类"用户 → 文章"的反向索引。

**对 M17 的直接影响**：推荐要"排除用户已点赞的文章"就必须反查，
而按现有结构只能 `SCAN` 整个 keyspace 再逐个 `SISMEMBER`，成本随文章数线性增长，
不能放在每次推荐请求里。因此：

| 排除项 | 可行性 | 依据 |
| --- | --- | --- |
| 自己写的文章 | **可行** | `article.user_id`，普通索引查询 |
| 已收藏的文章 | **可行** | `article_favorite(user_id, article_id)`，唯一键 + `idx_article_favorite_user_create_time` |
| 已点赞的文章 | **不可行** | Redis 只有"文章 → 用户集合"，无反查索引 |

**处理方式**：M17 的排除集只包含"自己写的 + 已收藏的"（见 `RecommendService` 的类注释）。
若产品上必须排除已点赞，需要先给点赞补一份反向索引（Redis Set `user:{id}:liked`
或落一张 `article_like` 表）——这属于改动 M6 的数据模型，会牵动点赞/取消点赞/热度计算三处，
建议单独立项，不要塞进 M17。

**附带影响（评测）**：这也意味着"点赞"当前**无法作为推荐评测的真值来源**，
可用真值只有收藏（`article_favorite`）与将来的行为日志 —— 这一点已并入决策简报的 Q2/Q3。

---

### 6.17 M17 评测实施中发现的两个问题（2026-09-19）

#### 问题 1：用户维度的推荐里，向量通道从未被触发（已修正）

开发集评测第一次跑出来的 Top-10 通道贡献是 **`{hot=160, follow=96}`** —— 一个 `vector` 都没有。

原因：向量召回当时只接受"来源文章"（文章详情页的"因为你在看这篇"），
而**用户维度的推荐**（"给你推荐"）没有来源文章，`sourceArticleId` 为 null，这一路直接返回空。
也就是说：那次 76% 的命中率跟"内容相似"毫无关系，实际只测了关注与热度两路。

修正：向量通道改为**多查询** —— 来源文章 + 用户最近收藏的若干篇（内容画像），
同一篇文章取最高相似度，再按相似度排名作为一个通道。
这同时是产品策略「登录用户增加收藏等个性化信号」的落地。

> 教训：一个"永远返回空"的通道不会报错、也不会让指标变难看 —— 它只是**静默地不参与**。
> 评测报告必须打印通道贡献，否则这种缺陷可以一直藏着。

#### 问题 2：留一法的泄漏风险 —— 目标文章不能被当成已知兴趣

留一法把用户的某条收藏 `t` 当作预测目标，并把它留在候选池里。
但"兴趣画像"若仍把 `t` 算进去，等于**把答案提前告诉算法**，命中率会虚高。
因此评测传入兴趣文章时必须**剔除目标**（排除集本来就不含 `t`，两者要一致）。

修正后 `RecommendService.recommend(request, exclusionOverride, interestOverride)`
的两个参数都可被外部指定，评测同时覆盖二者。

#### 附带发现：区分度检验的两个指标给出的信号强度不同

C1 的判据是"同主题与跨主题的平均相似度差"，实测（50 篇合成语料）：

| 指标 | 实测值 | 参考 |
| --- | --- | --- |
| 同主题文章对平均相似度 | 0.8850（576 对） | |
| 跨主题文章对平均相似度 | 0.8703（4716 对） | |
| **区分度（差值）** | **0.0147** | 硬条件 > 0 ✅；目标 > 0.05 ❌ |
| **Top-5 中同主题文章占比** | **45.6%** | 随机期望 12.5% ✅ |

解读：bge 对中文技术段落的相似度整体偏高（都挤在 0.87~0.89 的窄区间），
所以**平均相似度差是个弱信号**；而**排序上的区分度很强**（45.6% vs 12.5%）。

这反过来印证了融合方式的选择是对的：RRF 按**排名**而非分数计分 ——
在本项目的数据上，分数绝对值几乎没有意义，排名才有意义。

---

### 6.18 T12 验证执行记录：工具调用链能不能被埋点（M18 前置验证，2026-09-19）

**为什么验**：M18 验收第 1 条要求轨迹里有「路由 → **工具调用链** → **每步耗时** → token」。
token 与整体耗时 M13-M17 已在采集，但工具链与单步耗时**从未采集过**。
埋点位置猜错的代价是：功能照常工作、轨迹里却永远没有工具链，而且不会报错。

**探针**：`ToolCallTraceProbe`（stub 模型，纯单测）+ `ToolCallTraceSmokeProbe`（真实 DeepSeek）。

| 问题 | 实测结论 | 证据 |
| --- | --- | --- |
| 工具执行循环在哪一层？ | **不在 `ChatClient`**，在 provider（`DeepSeekChatModel.call`）内部 | stub 模型只被调用 1 次；ChatClient 不自己跑循环 |
| 传入的 `ToolCallback` 实例会被原样使用吗？ | **会**（`ToolCallingChatOptions.getToolCallbacks()` 里是同一引用） | stub 断言 `assertSame` 通过 |
| 最终 `ChatResponse` 里还有工具链吗？ | **没有**（`hasToolCalls() == false`） | 真实调用实测 |
| 装饰器能拿到什么？ | 工具名 / 模型给的参数 JSON / 真实返回值 / **单步耗时** | 见下表 |

真实调用实测（一次提问触发两个工具调用，`ToolCallTraceSmokeProbe`）：

```text
总耗时 1166 ms；工具调用 2 次
工具=currentServerTime 入参={}                  结果="2026-09-19T21:30:00+08:00"                      单步耗时=3ms
工具=searchArticles     入参={"keyword": "Redis"}  结果=["Redis 缓存穿透与布隆过滤器", "Redis 分布式锁…"]  单步耗时=1ms
最终回答 = 服务器当前时间是 2026-09-19T21:30:00+08:00，搜索关键词 Redis 返回了两篇文章，…
token: prompt=946 completion=110 total=1056
最终响应 hasToolCalls() = false
```

**对实现的硬约束（已落地）**：
`QaAgent` / `AnalystAgent` 不能再把工具对象直接交给 `.tools(...)`，
必须走 `TraceRecorder.wrapTools(...)` → `ToolCallbacks.from(...)` → 逐个包一层
`TracingToolCallback` → `.toolCallbacks(...)`。装饰器只记录、不改变行为
（入参/返回值/异常原样透传，记录只是内存追加，落库在轨迹收尾时统一发生）。

---

### 6.19 T13 验证执行记录：执行轨迹能不能覆盖异步链路（M18 前置验证，2026-09-19）

**为什么验**：AI 调用并不都发生在 HTTP 请求线程 —— 运营洞察走单线程池，
知识库索引与内容审核走 RabbitMQ。traceId 若只活在请求线程的 ThreadLocal 里，
异步段就会变成"看不出前因后果的孤立记录"。

**探针**：`M18AsyncTraceProbe`（真实 RabbitMQ + 真实 exchange，探针专用队列）。

| 验证项 | 实测结论 |
| --- | --- |
| MQ 用**消息头**透传 traceId | **可行**：发送端 `MessagePostProcessor` 写 header，消费者从 `Message` 读回，值一致 |
| 加 header 是否影响消息体反序列化 | **不影响**：`KbIndexMessage` 正常还原（`articleId=999999`） |
| 两个既有消费者是否需要改签名 | **不需要**：`KbIndexMessageListener` / `ModerationMessageListener` 本来就有 `Message rawMessage` 参数 |
| 线程池里 ThreadLocal 不包装 | **必然丢失**（实测 `null`）—— 这就是必须显式传播的原因 |
| 包装 Runnable 后 | 子线程可见；任务结束后清理，线程复用时不会串味 |

**对实现的硬约束（已落地）**：
1. 新增消息头常量 `x-trace-id`（`TraceHeaders`），业务发送处无条件挂
   `TraceHeaders.propagate()` —— 当前线程没有轨迹时它是空操作，因此不需要判空分支；
2. 跨进程/跨线程一律走 `TraceRecorder.attach(...)` / `attachOrStart(...)`，
   在目标线程重新挂载后**UPDATE 同一行**，而不是另写一条记录；
3. 消费者读不到 traceId（旧消息、非 AI 触发的请求）时新建一条轨迹，
   保证"审核仍然有轨迹"，不会因为上游没有 traceId 就什么都不记。

---

## 七、风险记录

| 风险 | 影响面 | 缓解措施 | 状态 |
| --- | --- | --- | --- |
| ~~ONNX 模型首次加载需外网下载~~ | ~~M15 可能阻塞~~ | 实测用 `cache.directory` 固定到 `~/.cache/bitforum-onnx`，首次下载后离线可用（见 6.8.2） | **已解决** |
| ~~Redis 镜像更换影响 M11 指标同步~~ | ~~M11 功能回归~~ | 已验证端口协议不变、三类数据结构正常 | **已消除**（见 5.5） |
| ~~Redis Stack 模块未加载~~ | ~~向量检索不可用~~ | 已定位为 `command` 覆盖 entrypoint，改用 `REDIS_ARGS` 后模块正常加载 | **已解决**（见 5.1） |
| Spring AI 1.1.8 与 Boot 3.4.5 潜在冲突 | M13 起步 | 已确认官方支持；若冲突回退 `spring-ai-starter-model-openai` + 自定义 base-url | 待验证 |
| DeepSeek API 额度或网络不稳定 | 全部 AI 功能 | 全链路降级 + Ollama 备选依赖 | 已设计 |
| 公开仓库误提交 API Key | 安全事故 | `.env` 已被 gitignore 并实测验证；Key 只读环境变量；提交前检查 | 已设计 |
| 双机（Windows / MacBook）开发冲突 | 开发效率 | 分支隔离 + 每模块完成即提交 | 已设计 |
| ~~MySQL 端口 3307 与默认 3306 不一致~~ | ~~本机启动后端失败~~ | 已把宿主机端口改回 3306，与 `application.yml` 默认值对齐，无需再设 `SPRING_DATASOURCE_URL` | **已消除** |

---

## 八、参考资料

- [Spring AI Getting Started（版本要求）](https://docs.spring.io/spring-ai/reference/1.1/getting-started.html)
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/1.1/api/tools.html)
- [Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/1.1/api/chat-memory.html)
- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/1.1/api/advisors.html)
- [Spring AI Redis Vector Store](https://docs.spring.io/spring-ai/reference/1.1/api/vectordbs/redis.html)
- [Spring AI ONNX Transformers Embeddings](https://docs.spring.io/spring-ai/reference/1.1/api/embeddings/onnx.html)
- [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/1.1/upgrade-notes.html)
- [Spring AI 2.0.0 GA Available Now](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)
- [Tool Calling in Spring AI 2.0: A Composable, Agentic Architecture](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling)
