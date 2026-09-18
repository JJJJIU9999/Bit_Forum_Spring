# M15 交接文档：RAG 知识库与向量检索

> 本文供**新会话（干净上下文）**接手 M15 时使用。
> 写于 2026-09-18，此时 M13、M14 已完成并提交，工作区干净。
>
> 阅读顺序建议：本文 → `task_plan.md`（总体计划）→ `findings.md`（技术证据与原委）→ `progress.md`（执行记录）。
> 本文只写"新会话必须知道、但上面三份文档里没有或分散"的内容，不重复既有结论。

## 一、接手时的状态（已实测确认）

| 项目 | 状态 |
| --- | --- |
| 仓库 | `/Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring` |
| 分支 | `feat/ai-agent`，**8 个提交**，工作区已跟踪文件干净 |
| 最新提交 | `7a07b58`（本机开发脚本） |
| 基线 | 基于 `main` 的 `d6dd582`，未 push（按策略只在 M15 完成、系统冻结两个检查点 push） |
| 后端测试 | **233 项：232 通过 + 1 条件跳过** |
| 前端测试 | 23 项通过；构建 1889 模块 |
| Flyway | 已到 **V13**（`ai_conversation`、`ai_message`） |
| 中间件 | 三容器当前**已停止**；用 `./dev.sh` 启动 |
| 真实调用 | 已验证 DeepSeek 可用、工具调用可用（含写操作） |

## 二、M15 范围（来自 task_plan.md Phase 3）

让搜索从**关键词匹配**升级为**语义检索**，并让回答带可点击的引用来源。

计划内的 4 项：

1. 引入 `spring-ai-starter-model-transformers` 与 `spring-ai-starter-vector-store-redis`
2. 新增 Flyway V14：`ai_kb_document`、`ai_kb_chunk`
3. 实现 `ArticleChunkingService` / `KbIndexService` / `RagService`
4. 异步索引走 RabbitMQ（复用既有 DLQ + 手动 ACK + Redis 幂等套路）
5. 管理员知识库统计接口与管理页

## 三、最大风险：ONNX 嵌入模型必须先行验证

**这是整个计划里唯一还没实测过的技术点（findings.md 待验证事项 T3）。**

### 为什么它是风险

- DeepSeek **不提供 embedding 接口**，所以嵌入模型必须另找来源
- 选型是本地 ONNX（`TransformersEmbeddingModel`），依赖 `spring-ai-starter-model-transformers`
- 它需要 ONNX 格式的模型文件与 tokenizer，**官方默认行为可能是启动时联网下载模型**
- 若加载失败，M15 整体不成立

### 建议的验证顺序（先最小验证，再写业务代码）

> 教训来自 M13：当时我一开始写成 `@ConditionalOnBean` 与硬编码占位 Key，都是在真实调用时才暴露问题。
> M15 请**先把"模型能加载 + 能产出向量"跑通，再动手建表写代码**。

1. **只加依赖，不加业务代码**：在 `pom.xml` 加入 `spring-ai-starter-model-transformers`，确认能编译、能启动
2. **观察启动行为**：若启动时联网下载模型，记录下载地址、体积与耗时；确认是否可缓存到本地
3. **写一个最小测试**：注入 `EmbeddingModel`，调用 `embed("测试文本")`，断言返回向量长度 > 0 且维度稳定
4. **确认维度**：得到实际维度（Redis 向量索引必须显式声明 `DIM`，写错会导致建索引失败）
5. **确认离线可行性**：若首次需联网，评估用 `optimum-cli` 预导出 + `modelUri` 指向本地文件

### 三条回退路径（按优先级）

| 方案 | 说明 |
| --- | --- |
| 1. 本地 ONNX | 首选。零 API 费用、离线可跑 |
| 2. Ollama 本地嵌入 | 已列入计划作为备选依赖；需要本机跑 Ollama |
| 3. MySQL 关键词检索兜底 | 完全放弃向量，RAG 退化为关键词召回；技术含量下降但仍可交付 |

**结论要回写到 `findings.md` 的待验证事项 T3**，无论成功还是失败。

## 四、M13/M14 留下的、M15 必须知道的既有设计

### 可直接复用

| 已有能力 | 位置 | M15 怎么用 |
| --- | --- | --- |
| 会话记忆仓储 | `ai/memory/MysqlChatMemoryRepository` | 不改动；M15 加检索是旁路增强 |
| Agent 抽象与工具 | `ai/agent/Agent`、`ai/tool/*` | M15 给 QaAgent 增加"检索结果注入"能力 |
| 工具注册表 | `ai/tool/ToolRegistry` | 按 Agent 类型装配，M15 无需改动 |
| 幂等能力 | `RedisService.isMessageProcessed/markMessageProcessed` | **直接复用**于索引任务的幂等 |
| 消息队列模式 | `RabbitMQConfig`、`NotificationListener` | 索引队列照抄这套（手动 ACK + DLQ + 幂等） |
| BM25 式关键词检索 | `ArticleService.searchPublishedArticles` | 保留作为混合检索的一半或兜底 |

### 必须遵守的既有约束（踩过坑的）

1. **`@MapperScan` 不扫子包** —— 若 M15 在 `com.bitforum.ai.mapper` 下加 Mapper 是安全的（M13 已把该包加入扫描列表），但**新起包名要同步更新** `BitForumSpringApplication` 的 `@MapperScan`
2. **`@ConditionalOnBean` 在用户配置类中不可靠** —— 需要条件 bean 时用 `@ConditionalOnProperty`
3. **测试配置里的值要写成占位符形式** —— 硬编码会覆盖环境变量（M13 因此 401）
4. **工具/提示词改动必须用干净会话验证** —— 旧会话历史会延续旧结论
5. **Redis 向量库需要 `initialize-schema: true`** 才会建索引；元数据过滤字段必须在构造时显式声明类型
6. **RediSearch 返回的 KNN 距离是十进制字符串**，不是二进制 float32

## 五、环境与工具约束（macOS）

| 事项 | 说明 |
| --- | --- |
| 启动项目 | `./dev.sh`（一键起中间件 + 后端 + 前端）；停止用 `./stop-local.sh` |
| Ctrl+C | **只停前端**，后端需 `./stop-local.sh`（Maven 忽略 SIGINT，原因见 progress.md） |
| JDK | 必须先 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`；本机默认是 25 |
| Maven | 无 `mvn` 命令，用 `./mvnw -s maven-settings.xml` |
| 测试命令 | 见下 |
| 后端日志 | `.dev-logs/backend.log`（dev.sh 写入） |
| 沙箱 | 当前为 danger-full-access，无需提权 |

### 跑测试的完整环境变量（必须显式注入，Spring Boot 不读 .env）

```bash
cd /Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_DATASOURCE_USERNAME="$(grep '^SPRING_DATASOURCE_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_DATASOURCE_PASSWORD="$(grep '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_USERNAME="$(grep '^SPRING_RABBITMQ_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_PASSWORD="$(grep '^SPRING_RABBITMQ_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'
./mvnw -s maven-settings.xml test
```

真实调用 DeepSeek（默认跳过，需显式开启）：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2- | tr -d ' \r\n\t')"
./mvnw -s maven-settings.xml -Dtest=DeepSeekSmokeTest test
```

## 六、成本参考（M18 做预算时要用）

工具调用会显著提高 token 消耗，M15 引入检索后会进一步上升：

| 场景 | token |
| --- | --- |
| M13 纯对话 | 1,842 |
| M14 一次搜索工具调用 | 7,823 |
| M14 写操作（读详情 + 点赞） | 8,893 |
| M14 格式优化后 | 5,880 |

M15 需要关注：**检索到的文档片段会作为上下文注入**，若不限制 topK 与片段长度，token 会失控。

## 七、开工前的准备动作

1. 启动环境：`./dev.sh`（或用 `docker compose up -d mysql redis rabbitmq` 只起中间件）
2. 读 `task_plan.md` 的 Phase 3 与 `findings.md` 的第三节（向量存储与嵌入模型勘察）
3. **先做第三节的 ONNX 最小验证**，把结论回写 `findings.md`
4. 验证通过再建 Flyway V14 与业务代码

## 八、纪律（沿用既有约定）

- 只新增 Flyway 迁移，**不修改 V1-V13**
- 不回滚 M1-M14 的任何功能与文档
- 不提交 `.env`；API Key 只走环境变量
- 不 `git add .`；只暂存计划内文件
- 每个模块完成即跑 `mvn test` + `npm run build`，红了不开下一个
- 描述能力必须区分「已实现」与「规划中」，不虚构评估数据
- 提交前检查暂存区是否含真实 Key 片段（M14 曾误把 Key 前 6 位写进文档）
