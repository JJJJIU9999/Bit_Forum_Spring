# BitForum AI Agent 化升级 —— 进度记录

> 本文记录 M13-M18 六个 AI 模块的执行进度、验证结果与错误处理。
>
> 主计划书见 `task_plan.md`，技术勘察证据见 `findings.md`。

## 当前状态快照

| 项目 | 当前状态 |
| --- | --- |
| 仓库路径 | `/Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring` |
| 开发分支 | `feat/ai-agent`（从干净 `main` 的 `d6dd582` 拉出） |
| 分支基线 | 与 `main` 差异为 0 个提交 |
| 远端同步 | 本地分支未 push（按策略，检查点 1 在 M15 完成后） |
| 当前阶段 | **M14 已完成**（工具集 + Tool Calling + 身份注入修正 + 真实调用验证） |
| 最新 Flyway 迁移 | `V13__add_ai_conversation.sql`（M14 无新增迁移） |
| 模块完成度 | M13、M14 已完成；M15-M18 未开始 |
| 当前主线 | M14 收尾；下一步 M15 RAG 知识库与向量检索 |
| 中间件状态 | MySQL / Redis Stack / RabbitMQ 三容器 `Up (healthy)` |
| 数据库状态 | MySQL 8.0.46，Flyway V1-V13 全部 success |
| 测试状态 | **233 项：232 通过 + 1 项条件跳过** |
| 真实调用验证 | 已通过：AI 能调用搜索工具返回真实站内文章；能执行点赞写操作 |
| 待办 | M15：引入 transformers 与 vector-store-redis、Flyway V14、RAG 检索 |

## 总体进度

| 模块 | 名称 | 状态 | 验证情况 | 文档位置 |
| --- | --- | --- | --- | --- |
| Prep | AI Agent 升级计划书 | 已完成 | 计划书、勘察证据、进度记录已落盘 | `docs/graduation/ai-agent-upgrade/` |
| Prep | 前置环境（Redis Stack 替换 + Key 配置 + 中间件启动） | 已完成 | 三容器 healthy；RediSearch 2.10.20 已加载；向量检索端到端验证通过 | `findings.md` 第五节 |
| M13 | AI 基础设施与对话骨架 | **已完成** | 211 项测试（210 通过 + 1 跳过）；真实 DeepSeek 调用验证通过 | 本文 |
| M14 | 工具集与 Tool Calling | **已完成** | 233 项测试（232 通过 + 1 跳过）；真实调用验证工具可用 | 本文 |
| M15 | RAG 知识库与向量检索 | 未开始 | — | 待创建 |
| M16 | 内容审核 Agent | 未开始 | — | 待创建 |
| M17 | 运营分析 Agent 与智能推荐 | 未开始 | — | 待创建 |
| M18 | 可观测性、评估与工程化闭环 | 未开始 | — | 待创建 |

---

## 2026-09-18

### Phase 0：计划书落盘

- **Status:** complete

#### 立项背景

指导老师反馈「项目工作量不够高，是否接入大模型或人工智能技术」。经勘察确认，现有 M1-M12 已构成完整论坛业务闭环（板块、审核、收藏搜索、通知、举报治理、看板、用户主页、OpenAPI、关注、上传、Redis 落库、健康检查），但技术纵深集中在传统 Web 工程能力。

本计划书把项目定位升级为「基于 Spring Boot 与 React 的多智能体智能社区平台」，新增 M13-M18 六个 AI 模块。

#### 已执行的勘察动作

- 确认仓库分支状态：`git remote -v`、`git branch -a -vv`、`git rev-list --left-right --count main...origin/main`、`git log -8`
- 确认工作区状态：`git status --short`、`git diff --stat`
- 统计项目规模：`find src/main/java -name "*.java" | wc -l`（98）、`find src/test -name "*.java" | wc -l`（33）
- 列出 Flyway 迁移：`ls src/main/resources/db/migration/ | sort`（V1-V12）
- 阅读 `pom.xml`、`src/main/resources/application.yml`、`docker-compose.yml`、`.gitignore`、`.env.example`
- 阅读 `frontend/package.json`、`frontend/vitest.config.js`
- 阅读 `WebMvcConfig`、`LoginInterceptor`、`RedisService` 公开方法签名
- 阅读 `docs/graduation/毕业设计文档总览.md`、`毕业设计进度.md`、`m12-actuator-health/` 三份文档（确认文档风格与过期信息）
- 查询 Spring AI 版本元数据：`spring-ai-bom/maven-metadata.xml`
- 对比 Spring AI 1.1.8 与 2.0.1 的 DeepSeek starter POM 依赖
- 实测五个关键构件 HTTP 状态码（全部 200）
- 反编译 `spring-ai-autoconfigure-model-deepseek-1.1.8.jar` 读取配置元数据
- 抓取 Spring AI 官方文档：getting-started、tools、chat-memory、advisors、vectordbs/redis、embeddings/onnx、upgrade-notes
- 查询 Docker Hub `redis/redis-stack-server` 与 `redis/redis-stack` tags
- 确认 Docker 环境：`docker --version`（29.7.2）、`docker ps`（无运行中容器）

#### 已完成产出

- [x] 从干净 `main` 创建 `feat/ai-agent` 分支（与 main 差异 0 提交）
- [x] 创建 `docs/graduation/ai-agent-upgrade/task_plan.md`（完整计划书，含架构、六个模块、数据库变更、测试策略、时间表、风险评估）
- [x] 创建 `docs/graduation/ai-agent-upgrade/findings.md`（全部勘察证据与技术决策依据）
- [x] 创建 `docs/graduation/ai-agent-upgrade/progress.md`（本文）
- [ ] 更新 `docs/graduation/毕业设计文档总览.md`（项目定位 + M13-M18 + 更正过期分支信息）
- [ ] 更新 `docs/graduation/毕业设计进度.md`（同步新模块与分支策略）

#### 关键结论

| 结论 | 依据 |
| --- | --- |
| Spring AI 采用 1.1.8，不动 Spring Boot 3.4.5 | 官方文档明确 "Spring AI supports Spring Boot 3.4.x and 3.5.x" |
| 排除 Spring AI 2.0.1 | 其 POM 依赖 Spring Boot 4.x 专属 starter |
| Redis 必须换 Redis Stack 镜像 | 现有 `redis:7-alpine` 无 RediSearch，向量库无法工作 |
| 嵌入模型必须另找 | DeepSeek starter 只提供 Chat 模型，无 embedding 能力 |
| 新建 `feat/ai-agent` 分支 | 公开仓库 + 求职作品集 + Windows/MacBook 双机开发 |
| 既有文档分支信息已过期 | `feat/frontend-refactor` 等分支成果早已合并进 `main` |

#### 待办（下一轮）

- 更新总览与进度两份顶层文档
- 核对文档一致性
- 进入 Phase 1：M13 AI 基础设施与对话骨架

---

### 前置环境搭建（Phase 0 附加）

- **Status:** complete

#### 背景

用户要求先把三项前置处理好再进入 M13：Redis 镜像替换、DeepSeek Key 配置、中间件拉起。

#### 1. Redis 镜像替换

实测确认：用户此前是通过本项目 `docker-compose.yml` 拉起中间件的（容器名 `bit-forum-spring-redis-1` 可证），容器在 2 周前停止。因此「换镜像」只需修改 compose 文件，无需手工敲 docker 命令。

修改 `docker-compose.yml`：

- `redis:7-alpine` → `redis/redis-stack-server:7.4.0-v8`
- 新增独立数据卷 `redis-stack-data:/data`，避免与旧镜像数据文件冲突
- 新增卷声明 `redis-stack-data`
- 通过 `REDIS_ARGS: "--appendonly yes"` 环境变量开启 AOF 持久化

#### 2. 踩坑与修复：command 覆盖 entrypoint 导致模块未加载

**首次尝试失败**：最初写成 `command: ["redis-server", "--appendonly", "yes"]`，重建容器后 `MODULE LIST` 返回空、`FT._LIST` 报 `ERR unknown command`、启动日志无任何模块加载记录。

**根因定位**：

- `docker inspect` 显示镜像 `Entrypoint: null`、`Cmd: ["/entrypoint.sh"]`
- 读取 `/entrypoint.sh` 确认它负责执行全部 `--loadmodule` 参数（redisearch、rejson、redisbloom、redistimeseries、redisgears）
- 用 `command` 覆盖后，entrypoint 脚本不再执行，模块全部丢失

**修复**：改用镜像支持的 `REDIS_ARGS` 环境变量追加参数，不覆盖 `command`。重建后模块全部正常加载。

#### 3. 模块加载与向量检索验证

修复后验证结果：

- RediSearch 模块已加载，**version 21020**，Redis 服务版本 7.4.7
- 其余模块齐全：ReJSON 20809、timeseries 11206、bf 20816、redisgears_2 20020、RedisCompat 1

向量检索端到端验证（Python 原始 RESP 协议脚本 `/tmp/redis_vector_probe.py`）：

- 4 维验证：索引创建、二进制向量写入、KNN 距离排序、`status` 过滤、`categoryId` 过滤、混合过滤、索引统计、DD 清理 —— 全部符合预期
- 768 维验证（ONNX 典型维度）：距离排序 0.000000 / 0.127120 / 0.983072，完全正确
- 顺带发现：RediSearch 返回的 KNN 距离是**十进制字符串**而非二进制 float32，Java 侧解析需注意

兼容性验证（现有 `RedisService` 依赖的数据结构）：String `SET/GET`、ZSet `ZADD/ZREVRANGE`、Set `SADD/SMEMBERS` 全部正常，**M11 指标同步能力不受影响**。

#### 4. DeepSeek API Key 配置

三处配置全部就位：

| 文件 | 内容 | 是否入库 |
| --- | --- | --- |
| `.env.example` | `DEEPSEEK_API_KEY=sk-replace-with-your-deepseek-api-key` 模板 + 中文说明 | 会提交 |
| `.env` | `DEEPSEEK_API_KEY` 已由用户填入真实 Key（**不记录实际值**） | 已被 gitignore |
| `docker-compose.yml` | `app` 服务新增 `DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY:-}` | 会提交 |

安全性验证：`git check-ignore -v .env` 返回 `.gitignore:47:.env`，确认 Key 不会进入 Git 历史。

**注意**：真实 Key 需由用户本人填入，助手不代填，避免凭据经手。

#### 5. 中间件拉起

```bash
docker compose up -d mysql redis rabbitmq
```

结果：三容器全部 `Up (healthy)`。

| 服务 | 镜像 | 宿主机端口 | 状态 |
| --- | --- | --- | --- |
| mysql | `mysql:8.0` | 3306（初为 3307，已改回） | healthy |
| redis | `redis/redis-stack-server:7.4.0-v8` | 6379 | healthy |
| rabbitmq | `rabbitmq:3-management` | 5672 / 15672 | healthy |

数据库验证：MySQL 8.0.46，`bit_forum` 库存在，`flyway_schema_history` 12 条记录且 V1-V12 全部 `success=1`。

RabbitMQ 验证：`rabbitmq-diagnostics -q ping` → `Ping succeeded`；队列为空（应用未启动）。

**端口调整**：初次拉起时 MySQL 沿用历史的 `3307:3306` 映射。经用户说明，`3307` 是为了避开 Windows 主机上已装的本机 MySQL 而做的折中方案；当前 macOS 开发机 `3306` 空闲，因此已改回 `3306:3306`，与 `application.yml` 及测试配置的默认值一致。改后复核数据完整性：5 个用户、9 篇文章、12 条迁移记录，数据卷未受影响。

#### 6. 本轮新增/修改文件

| 文件 | 变更 |
| --- | --- |
| `docker-compose.yml` | Redis 镜像替换、`REDIS_ARGS`、独立数据卷、app 服务新增 `DEEPSEEK_API_KEY`、MySQL 端口改回 `3306:3306` |
| `.env.example` | 新增 `DEEPSEEK_API_KEY` 模板与说明 |
| `.env` | 新增 `DEEPSEEK_API_KEY`（已被 gitignore，用户已填入真实 Key） |
| `README.md` | MySQL 端口 3307 → 3306，本地测试命令移除已不需要的 `SPRING_DATASOURCE_URL` |
| `docs/graduation/ai-agent-upgrade/findings.md` | 新增第五节「Redis Stack 替换实战验证」，六/七/八节顺延，风险表更新 |
| `docs/graduation/ai-agent-upgrade/task_plan.md` | 新增 3.2 Redis Stack 环境约束，Phase 0.5 清单 |
| `docs/graduation/ai-agent-upgrade/progress.md` | 本文，新增本小节 |
| `docs/graduation/毕业设计文档总览.md` | 项目定位升级、M13-M18 模块表、更正过期分支与路径 |
| `docs/graduation/毕业设计进度.md` | 同步 M13-M18 进度、更正过期状态、命令改 macOS 版、补充 AI 风险约束 |

#### 待办（进入 M13 前）

- ~~用户把 `.env` 中 `DEEPSEEK_API_KEY=sk-replace-me` 替换为真实 Key~~ —— **已完成**，Key 已填写（`sk-` 前缀，35 字符），`.env` 确认被 gitignore
- ~~MySQL 端口改回 3306~~ —— **已完成**，容器重建后数据完整（5 用户 / 9 文章 / 12 迁移）
- 进入 Phase 1：M13 AI 基础设施与对话骨架（引入 Spring AI 依赖 → 跑 T1/T2 验证 → 建 Flyway V13）

---

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `git checkout -b feat/ai-agent` | 成功，`Switched to a new branch 'feat/ai-agent'` |
| `git branch --show-current` | `feat/ai-agent` |
| `git rev-list --count main..feat/ai-agent` | `0`（分支与 main 完全一致） |
| `git rev-list --left-right --count main...origin/main` | `0	0`（main 与远端同步） |
| `git diff --stat` | 空（已跟踪文件零未提交改动） |
| `find src/main/java -name "*.java" \| wc -l` | `98` |
| `find src/test -name "*.java" \| wc -l` | `33` |
| `ls src/main/resources/db/migration/ \| sort` | V1 至 V12，共 12 个迁移 |
| Spring AI 构件可下载性检查 | 5/5 返回 HTTP 200 |
| `docker --version` | `Docker version 29.7.2, build a7dcaa6` |
| `docker ps` | 接手时无运行中容器；本轮已拉起三个中间件 |
| `docker compose config --quiet` | 通过（配置有效） |
| `docker compose up -d mysql redis rabbitmq` | redis-stack-server 镜像拉取成功，三容器启动 |
| `docker compose ps` | mysql / redis / rabbitmq 全部 `Up (healthy)` |
| `redis-cli MODULE LIST`（修复前） | 空 —— 定位为 command 覆盖 entrypoint |
| `redis-cli MODULE LIST`（修复后） | search(21020)、ReJSON(20809)、timeseries(11206)、bf(20816)、redisgears_2(20020)、RedisCompat(1) |
| `redis-cli FT._LIST`（修复后） | 不再报 unknown command |
| `python3 /tmp/redis_vector_probe.py` | 4 维向量检索全流程通过，含 KNN 排序、status 过滤、categoryId 过滤、混合过滤、DD 清理 |
| 768 维向量检索验证 | 距离排序 0.000000 / 0.127120 / 0.983072，正确 |
| Redis 兼容性：`SET/GET`、`ZADD/ZREVRANGE`、`SADD/SMEMBERS` | 全部正常 |
| `mysql SELECT VERSION()` | `8.0.46` |
| `mysql SHOW DATABASES LIKE 'bit_forum'` | `bit_forum` 存在 |
| `mysql SELECT * FROM flyway_schema_history` | 12 条记录，V1-V12 全部 `success=1` |
| `rabbitmq-diagnostics -q ping` | `Ping succeeded` |
| `git check-ignore -v .env` | `.gitignore:47:.env`（确认 Key 不会入库） |
| 端口监听检查（3306/6379/5672/15672） | 四个端口全部监听中 |

**注意**：本轮为文档、配置与基础设施阶段，**未改动任何 Java 或前端代码**，因此未运行 `mvn test` 与 `npm run build`。M13 引入 Spring AI 依赖后将执行完整回归验证（含待验证事项 T1、T2）。

---

## 错误记录

| 问题 | 尝试 | 处理 |
| --- | --- | --- |
| 技能文档记录的仓库路径 `$HOME/Developer/BitFrom/spring_code/bit-forum-spring` 不存在 | 1 | 实测确认真实路径为 `/Users/jiu/Developer/Projects/Java/BitFrom/spring_code/bit-forum-spring`，已按真实路径作业 |
| `web_fetch` 工具对 `spring.io` / `docs.spring.io` / `raw.githubusercontent.com` 报「解析到非公网 IP」 | 1 | 改用 `curl` 抓取官方文档并用 Python 剥离 HTML 标签提取正文，成功获取全部所需原文 |
| `spring-ai-spring-boot-dependencies` 坐标 404 | 1 | 确认该 artifact 名称不存在，改用 `spring-ai-bom` 与各 starter 的 POM 直接验证依赖关系 |
| **redis-stack-server 模块未加载**：`MODULE LIST` 空、`FT._LIST` 报 unknown command | 1 | 根因是 `command: ["redis-server", ...]` 覆盖了镜像的 `/entrypoint.sh`（它负责 `--loadmodule`）。改用 `REDIS_ARGS` 环境变量追加参数后模块全部正常加载 |
| Redis 向量探针脚本解析 score 报 `unsupported format string passed to bytes.__format__` | 1 | 实测发现 RediSearch 返回的 KNN 距离是十进制字符串（如 `b'0.00362026691437'`）而非二进制 float32，改用 `float()` 解析 |

---

## 常用验证命令

```bash
# 后端全量测试（需要 MySQL / Redis / RabbitMQ 已启动）
SPRING_DATASOURCE_PASSWORD=... SPRING_RABBITMQ_PASSWORD=... \
JWT_SECRET=bit-forum-local-test-secret-minimum-32-bytes mvn test

# 后端编译
mvn -DskipTests compile

# 前端构建与测试
cd frontend && npm run build && npm test

# 格式检查
git diff --check

# 拉起本地中间件（M13 起 Redis 换为 Redis Stack）
docker compose up -d mysql redis rabbitmq
```

---

## 风险与注意事项

- **不要修改历史 Flyway 脚本 V1 至 V12**，AI 模块的数据库变更一律新增 V13 及以后。
- **不要回滚 M1-M12** 的任何功能与文档。
- **不要提交 `.env`**；DeepSeek Key 只通过环境变量 `DEEPSEEK_API_KEY` 读取。
- **不要逐模块 push**；按策略只在 M15 完成与系统冻结两个检查点 push。
- **不要 `git add .`**；只暂存计划内文件。
- 描述项目能力时必须区分「已实现」与「规划中」，不得虚构评估数据、性能或效果。
- ONNX 嵌入模型是本计划尚未实测的技术点，M15 开始前必须先做最小验证。

---

## M13 执行记录（2026-09-18）

### M13-1：引入 Spring AI 依赖并验证 T1

- **Status:** complete

#### 范围收窄决策

计划书原本要求 M13 引入 4 个 starter。实施时收窄为**只引入 DeepSeek**，理由：

| 依赖 | M13 是否引入 | 原因 |
| --- | --- | --- |
| `spring-ai-starter-model-deepseek` | ✅ 引入 | M13 对话骨架的核心依赖 |
| `spring-ai-starter-model-transformers` | ❌ 延后至 M15 | M13 没有 EmbeddingModel 用途；提前引入会让 ONNX 模型在启动阶段就被要求加载，平白增加 M13 失败面 |
| `spring-ai-starter-vector-store-redis` | ❌ 延后至 M15 | 向量库 M15 才使用 |
| `spring-ai-starter-model-ollama` | ❌ 延后至 M18 | 降级能力 M18 才实现 |

**pom.xml 变更**：

- `properties` 新增 `<spring-ai.version>1.1.8</spring-ai.version>`
- 新增 `dependencyManagement` 导入 `spring-ai-bom:1.1.8`
- `dependencies` 新增 `spring-ai-starter-model-deepseek`（版本由 BOM 管理）
- 用注释标明 M15 需补充的两个 starter

#### 配置变更

`src/main/resources/application.yml` 新增 `spring.ai.deepseek.*`：

- `api-key: ${DEEPSEEK_API_KEY:}` — 只从环境变量读取，不写死
- `chat.enabled: ${DEEPSEEK_CHAT_ENABLED:true}` — 与 datasource / rabbitmq / jwt 约定一致：必需凭据缺失即启动失败
- `chat.options.model: deepseek-chat`、`temperature: 0.7`

`src/test/resources/application.yml` 新增占位 api-key（原因见 findings.md 6.2）。

#### 验证结果

| 命令 | 结果 |
| --- | --- |
| `./mvnw -s maven-settings.xml -DskipTests compile`（JDK 17） | 退出码 0 |
| `./mvnw dependency:tree \| grep spring-ai` | 8 个 Spring AI 构件全部解析为 1.1.8 |
| `./mvnw -s maven-settings.xml test` | **Tests run: 189, Failures: 0, Errors: 0** |

**T1 通过**：Spring AI 1.1.8 与 Spring Boot 3.4.5 无冲突。**T2 通过**：Redis Stack 替换未破坏既有功能。

#### 工具链环境说明（MacBook）

- 本机无 `mvn` 命令，必须使用项目自带的 `./mvnw`（Maven 3.9.16）
- 本机有两个 JDK：默认 25、另有 17。项目要求 Java 17，需显式指定：
  `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- `mvnw` 原本没有执行权限，已 `chmod +x`
- Maven 仓库在 `~/.m2`（会话工作区之外）；沙箱为 workspace-write 时会被拒绝写入，
  需要 danger-full-access 策略才能编译/测试

### M13-2：修复引入依赖后暴露的既有测试脆弱性

- **Status:** complete

`ArticleMetricSyncServiceTest` 有 2 项失败。经诊断确认**不是 Redis Stack 或 Spring AI 造成的回归**，
而是既有测试本身的脆弱假设（详见 findings.md 6.3）：

1. 假设"数据库中只有本测试创建的已发布文章"，实际库中有 4 篇历史 PUBLISHED 文章
2. 假设"Mockito 对未打桩方法返回 null"，实际 **Mockito 对 `Long` 返回类型默认返回 `0L`**

修复方式：`lenient()` 显式声明库中已有文章在 Redis 中无数据 + `eq()` 精确限定打桩范围，
使断言不再依赖数据库初始状态。修复后 189 项全绿。

**说明**：这 2 项失败此前被"测试库为空"掩盖。本次修复让测试在真实有数据的库上也能稳定通过，
属于顺带修好的既有缺陷，不是 M13 引入的问题。

### M13-3 ~ M13-7：对话骨架实现与验证

- **Status:** complete

#### 数据库

新增 Flyway `V13__add_ai_conversation.sql`（未修改历史迁移 V1-V12）：

| 表 | 用途 |
| --- | --- |
| `ai_conversation` | AI 会话：user_id、title、agent_type、message_count、total_tokens |
| `ai_message` | AI 消息：role、content、tool_calls、retrieved_doc_ids、Token 统计、latency_ms |

实测迁移记录：`Migrating schema bit_forum to version "13 - add ai conversation"` → `Successfully applied 1 migration`。

#### 后端新增类

| 层 | 类 | 说明 |
| --- | --- | --- |
| entity | `AiConversation`、`AiMessage` | MyBatis-Plus 实体 |
| mapper | `AiConversationMapper`、`AiMessageMapper` | 基础 Mapper |
| memory | `MysqlChatMemoryRepository` | 实现 Spring AI `ChatMemoryRepository`，会话记忆落库 |
| service | `AiConversationService` | 会话/消息读写、归属校验、标题生成 |
| agent | `Agent`、`AgentContext`、`AgentResponse`、`AgentType`、`QaAgent` | Agent 抽象与问答助手实现 |
| orchestrator | `AgentOrchestrator` | 历史加载 → 路由 → 持久化 → 统计更新 |
| config | `AiConfig` | `ChatClient` 装配 |
| dto | `AiConversationResponse`、`AiMessageResponse`、`AiConversationCreateRequest`、`AiChatRequest` | 接口出入参 |
| controller | `AiController` | 4 个对话接口 |

同时修改 `BitForumSpringApplication`（`@MapperScan` 增加 AI 域包）与 `WebMvcConfig`（`/api/ai/**` 接入 `LoginInterceptor`）。

新增接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/ai/conversations` | 新建会话 |
| `GET` | `/api/ai/conversations` | 我的会话列表 |
| `POST` | `/api/ai/conversations/{id}/messages` | 发送消息并获取回答 |
| `GET` | `/api/ai/conversations/{id}/messages` | 会话消息历史 |

#### 前端

| 文件 | 变更 |
| --- | --- |
| `src/api/aiApi.js` | 新增，4 个接口封装；AI 请求单独放宽超时到 60s |
| `src/components/AiAssistantPanel.jsx` | 新增，右下角浮动面板（会话列表、消息流、发送、错误与加载态） |
| `src/styles/ai-panel.css` | 新增，使用项目既有 CSS 变量；移动端避开底部导航 |
| `src/layouts/MainLayout.jsx` | 引入面板，仅登录用户渲染 |
| `src/main.jsx` | 引入 ai-panel.css |

#### 测试

| 测试类 | 项数 | 覆盖内容 |
| --- | --- | --- |
| `MysqlChatMemoryRepositoryTest` | 4 | 消息持久化与顺序还原、toolCalls 还原、未知会话容错、按会话删除隔离 |
| `AiControllerTest` | 9 | 三个接口的登录拦截、创建/列表/发消息/历史、空消息与超长消息校验 |
| `AgentOrchestratorTest` | 8 | 消息落库、历史传递、按类型路由、统计更新、标题生成、403/404、降级回答落库 |
| `DeepSeekSmokeTest` | 1 | 真实 DeepSeek 调用（默认跳过，条件执行） |

#### 验证结果

| 命令 | 结果 |
| --- | --- |
| `mvn test` | **Tests run: 211, Failures: 0, Errors: 0, Skipped: 1**，BUILD SUCCESS |
| `mvn -Dtest=DeepSeekSmokeTest test`（`DEEPSEEK_CHAT_ENABLED=true`） | 通过，真实调用 DeepSeek 成功 |
| `npm run build` | 通过，Vite 构建 1887 个模块 |
| `npm test` | 通过，4 项 |
| `git diff --check` | 通过 |

#### 未完成的手工验证

前端面板的**浏览器交互**尚未人工验证（需启动前后端后在页面点击确认）。
已完成的验证是构建通过、逻辑经后端接口测试覆盖。

### M13 修改/新增文件汇总

| 文件 | 变更 |
| --- | --- |
| `pom.xml` | 新增 spring-ai BOM 与 DeepSeek starter |
| `mvnw` | 补执行权限（chmod +x） |
| `src/main/resources/application.yml` | 新增 `spring.ai.deepseek.*` 配置块 |
| `src/test/resources/application.yml` | api-key 改为占位符形式（避免覆盖环境变量）+ enabled 开关 |
| `src/main/resources/db/migration/V13__add_ai_conversation.sql` | 新增 AI 会话与消息表 |
| `src/main/java/com/bitforum/BitForumSpringApplication.java` | `@MapperScan` 增加 `com.bitforum.ai.mapper` |
| `src/main/java/com/bitforum/config/WebMvcConfig.java` | `/api/ai/**` 接入登录拦截器 |
| `src/main/java/com/bitforum/ai/**`（18 个文件） | entity / mapper / memory / service / agent / orchestrator / config / dto / controller |
| `src/test/java/com/bitforum/service/ArticleMetricSyncServiceTest.java` | 修复脆弱假设（M11 顺带修复） |
| `src/test/java/com/bitforum/ai/**`（4 个测试类） | 21 项 AI 测试 |
| `frontend/src/api/aiApi.js` | 新增 |
| `frontend/src/components/AiAssistantPanel.jsx` | 新增 |
| `frontend/src/styles/ai-panel.css` | 新增 |
| `frontend/src/layouts/MainLayout.jsx` | 引入面板 |
| `frontend/src/main.jsx` | 引入样式 |

## M14 执行记录（2026-09-18）

### 目标与产出

让 AI 能真正查询和操作站内数据，而不是只能依赖提示词里的静态说明。

| 类型 | 新增文件 | 说明 |
| --- | --- | --- |
| 工具 | `ArticleTools` | searchArticles、getArticleDetail、getHotArticles、listCategories、createDraftArticle |
| 工具 | `UserInteractionTools` | 收藏/取消收藏、点赞/取消点赞、关注/取消关注、getFollowStats |
| 视图 | `ToolDtos` | 返回给模型的精简视图（ArticleBrief / ArticleDetail / CategoryBrief / HotArticleBrief / PagedResult / ActionResult）|
| 注册 | `ToolRegistry` | 按 Agent 类型装配工具，实现最小权限 |
| 上下文 | `AgentContextKeys` | ToolContext 键名常量 |
| 修改 | `QaAgent` | 接入工具；通过 ToolContext 注入当前用户身份 |

工具共 12 个方法（原计划 10 个工具类，实际按能力聚合为 2 个工具类 + 12 个方法）：
- 查询类 5 个：搜索、详情、热榜、板块列表、关注统计
- 写操作类 7 个：收藏/取消、点赞/取消、关注/取消、建草稿

### 关键设计决策

1. **不重复写 SQL**：所有工具复用既有 Service（`ArticleService`、`CategoryService`、
   `RedisService`、`UserFollowService`），只做参数适配与结果裁剪。
2. **控制上下文**：搜索每页上限 20 条，正文截断到 800 字并标注，避免模型一次拉取过多内容。
3. **点赞行为对齐站内接口**：`RedisService.like()` + `increaseHot(3)` 组合，
   与 `ArticleController` 的点赞逻辑一致；重复点赞不重复加热度。
4. **最小权限**：`ToolRegistry` 按 Agent 类型装配，审核/运营/推荐 Agent 当前不装配任何工具，
   为 M16 的审核 Agent 预留安全边界。

### 关键修正：工具的用户身份改为应用注入

**首版缺陷**：`userId` 被设计成 `@ToolParam` 工具参数，导致真实调用时 AI 回复
「**请提供你的用户 id**，我才能执行这次点赞」。

**问题**：当前登录用户是应用侧已知信息（JWT 已解析），推给模型索要既荒谬又不可靠。

**修正**：改用 Spring AI 的 `ToolContext` 隐式参数（不进入模型可见的 schema）：

```java
// 工具侧
public ActionResult likeArticle(@ToolParam(...) Long articleId, ToolContext toolContext) {
    Long userId = currentUserId(toolContext);
    ...
}
// Agent 侧
chatClient.prompt().messages(messages).tools(tools).toolContext(toolContext).call();
```

**验证时的坑**：修复后在**旧会话**复测仍在索要 userId —— 因为该会话历史里已留下旧结论，
模型在延续上下文。**新建干净会话复测，AI 直接执行点赞并返回真实结果**。
涉及工具 schema 或提示词的改动，必须用干净会话验证。

### 测试

| 测试类 | 项数 | 覆盖内容 |
| --- | --- | --- |
| `ArticleToolsTest` | 18 | 只检索已发布文章、空结果容错、分页上限、超长正文截断、草稿不可见、收藏落库、
草稿不可收藏、点赞成功加热度、重复点赞不加热度、关注与粉丝统计、建草稿为 DRAFT 状态、缺参拒绝 |
| `ToolRegistryTest` | 4 | QA 拿到工具、其他类型不拿到、null 类型安全返回空、枚举常量稳定 |

| 命令 | 结果 |
| --- | --- |
| `mvn -Dtest=ArticleToolsTest,ToolRegistryTest test` | 22 项通过 |
| `mvn test`（全量） | **Tests run: 233, Failures: 0, Errors: 0, Skipped: 1** |

### 真实链路验证（干净会话）

| 验证项 | 结果 |
| --- | --- |
| 「站内有没有关于 Redis 的文章？」 | AI 调用 searchArticles，返回真实文章（id=87《Redis 热点数据同步方案》，作者 Alice 后端笔记，板块 Java 后端，浏览 206，点赞 3）|
| 「帮我点赞文章87」 | AI 通过 getArticleDetail 确认文章存在后执行点赞，返回成功，**不再索要 userId** |
| 顺带观察 | AI 主动指出「点赞接口返回 1，而详情页显示 3，口径可能不同」—— 说明它在交叉比对真实数据 |

### 成本观察

工具调用会显著提高 token 消耗（工具 schema 随每次请求发送 + 返回结果进入上下文）：

| 场景 | token |
| --- | --- |
| M13 纯对话 | 1,842 |
| M14 搜索工具调用 | 7,823 |
| M14 写操作（先读详情再点赞） | 8,893 |

已记入 `findings.md` 6.7，M18 的成本统计必须计入工具 schema 的固定开销。

### M14 修改/新增文件

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/bitforum/ai/tool/ArticleTools.java` | 新增 |
| `src/main/java/com/bitforum/ai/tool/UserInteractionTools.java` | 新增 |
| `src/main/java/com/bitforum/ai/tool/ToolDtos.java` | 新增 |
| `src/main/java/com/bitforum/ai/tool/ToolRegistry.java` | 新增 |
| `src/main/java/com/bitforum/ai/tool/AgentContextKeys.java` | 新增 |
| `src/main/java/com/bitforum/ai/agent/QaAgent.java` | 接入工具与 ToolContext，提示词改为"用工具查而非依赖静态说明" |
| `src/test/java/com/bitforum/ai/tool/ArticleToolsTest.java` | 新增，18 项 |
| `src/test/java/com/bitforum/ai/tool/ToolRegistryTest.java` | 新增，4 项 |

### 下一步：M15 RAG 知识库与向量检索

- 引入 `spring-ai-starter-model-transformers` 与 `spring-ai-starter-vector-store-redis`
- 新增 Flyway V14：`ai_kb_document`、`ai_kb_chunk`
- 实现 `ArticleChunkingService` / `KbIndexService` / `RagService`
- 异步索引走 RabbitMQ，复用既有 DLQ + 手动 ACK + Redis 幂等套路
- 扩展名搜索能力：当前 searchArticles 是关键词匹配，M15 补语义检索
