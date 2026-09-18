# BitForum AI Agent 化升级 —— 技术勘察与发现记录

> 本文记录 M13-M18 立项前的全部技术勘察证据。所有结论均来自本机实测或官方文档原文，不包含推测。
>
> 勘察时间：2026-09-18
> 勘察环境：macOS（MacBook），`/Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring`

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
| 项目路径 `D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring` | 当前 MacBook 路径为 `/Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring` |

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
| T3 | ONNX 嵌入模型能成功加载并返回向量 | M15 开始前的最小验证 | 待验证 |
| T4 | DeepSeek Tool Calling 实际可用且有稳定的调用成功率 | M14 工具冒烟测试 | 待验证 |
| T5 | `RedisVectorStore` 元数据过滤在实际数据上生效 | M15 检索验证 | **原生命令已验证**（见 5.4），Spring AI 封装层待验证 |
| T6 | 单轮对话的真实 Token 消耗与费用 | M13 结束后首次统计 | 待验证 |
| T7 | 结构化输出在 DeepSeek 上的稳定性 | M16 审核 Agent 验证 | 待验证 |
| T8 | 异步索引队列与既有 `ArticlePublishMessage` 队列不冲突 | M15 接入 RabbitMQ 时验证 | 待验证 |

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

---

## 七、风险记录

| 风险 | 影响面 | 缓解措施 | 状态 |
| --- | --- | --- | --- |
| ONNX 模型首次加载需外网下载 | M15 可能阻塞 | 提前用 `optimum-cli` 导出并用 `modelUri` 指向本地文件 | 待验证 |
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
