# BitForum AI Agent 化升级 —— 进度记录

> 本文记录 M13-M18 六个 AI 模块的执行进度、验证结果与错误处理。
>
> 主计划书见 `task_plan.md`，技术勘察证据见 `findings.md`。

## 当前状态快照

| 项目 | 当前状态 |
| --- | --- |
| 仓库路径 | `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring` |
| 开发分支 | `feat/ai-agent`（从干净 `main` 的 `d6dd582` 拉出） |
| 分支基线 | 与 `main` 差异为 0 个提交 |
| 远端同步 | 本地分支未 push（按策略，检查点 1 在 M15 完成后） |
| 当前阶段 | **M18 进行中**：T12/T13 前置验证已通过；V18 `ai_execution_trace` + `TraceRecorder` + 四条链路埋点 + 管理端查询接口已落地（381 项测试全绿）；待做 V19 `ai_usage_stat`、`AiDegradeGuard`、前端轨迹页 |
| 最新 Flyway 迁移 | `V18__add_ai_execution_trace.sql`（AI 执行轨迹；下一个是 V19 `ai_usage_stat`） |
| 模块完成度 | M13-M17 已完成；M18 进行中（轨迹模块已完成，用量/降级/前端未做） |
| 当前主线 | **M18（可观测性、工程化闭环，收敛版）**：Trace + Usage + Degrade + 简单可视化；见 `m18-handoff.md` |
| 中间件状态 | MySQL / Redis Stack / RabbitMQ 三容器 `Up (healthy)` |
| 数据库状态 | MySQL 8.0.46，Flyway V1-V18 全部 success |
| 测试状态 | **381 项：372 通过 + 9 项条件跳过**（跳过项均为需要真实 API Key 的调用测试）；前端 33 项 |
| 审核评测 | 开发集 70 条 + 独立测试集 30 条：漏放率 0%、误伤率 0%、安全召回率 100%；严格准确率 96.7%~97.1% |
| **推荐评测** | **合成数据 + 留一法**：融合推荐 HitRate@10 = 0.8095（开发集）/ 0.8889（独立测试集），纯热榜 0.2381 / 0.2222，随机 0.2857 / 0.3333；规范 `m17-eval-protocol.md`，报告 `m17-eval-report.md` |
| 推荐策略 | 已定稿（task_plan.md M17 实施决策）：排序全由 Java 完成、LLM 只写解释；只排除作者本人与已收藏；匿名可见（两路） |
| 自动放行 | **保持默认关闭**：阈值在测试集上覆盖率 0%（< 开发集 13.3%），数据不支持启用 |
| 嵌入模型 | 已定稿 `bge-base-zh-v1.5`（768 维，量化 102MB，缓存于 `~/.cache/bitforum-onnx`） |
| 向量索引 | `bitforum-kb`（HNSW / FLOAT32 / DIM 768 / COSINE），元数据字段已声明 |
| 知识库状态 | 文章审核通过/下架/删除会自动更新；集成测试收尾会清空，需要时用管理页或重建接口恢复 |
| 审核策略 | 已定稿（task_plan.md M16 实施决策）：决策与动作解耦、自动 PASS 但不自动 REJECT、评测集留独立测试集 |
| 真实调用验证 | 已通过：工具调用（M14）；RAG 回答带引用（M15）；审核 Agent 8 条样本 8/8（M16-1） |
| 待办 | M18：V18 轨迹 + `TraceRecorder` + 四条链路埋点 + 管理端查询（已完成）→ V19 `ai_usage_stat` → `AiDegradeGuard` → 前端轨迹页；决策简报 `m18-decision-brief.md`、交接文档 `m18-handoff.md` 未提交 |

## 总体进度

| 模块 | 名称 | 状态 | 验证情况 | 文档位置 |
| --- | --- | --- | --- | --- |
| Prep | AI Agent 升级计划书 | 已完成 | 计划书、勘察证据、进度记录已落盘 | `docs/graduation/ai-agent-upgrade/` |
| Prep | 前置环境（Redis Stack 替换 + Key 配置 + 中间件启动） | 已完成 | 三容器 healthy；RediSearch 2.10.20 已加载；向量检索端到端验证通过 | `findings.md` 第五节 |
| M13 | AI 基础设施与对话骨架 | **已完成** | 211 项测试（210 通过 + 1 跳过）；真实 DeepSeek 调用验证通过 | 本文 |
| M14 | 工具集与 Tool Calling | **已完成** | 233 项测试（232 通过 + 1 跳过）；真实调用验证工具可用 | 本文 |
| M15 | RAG 知识库与向量检索 | **已完成** | 278 项测试（276 通过 + 2 跳过）；真实调用回答带引用；异步索引端到端验证通过 | 本文 |
| M16 | 内容审核 Agent（人机协同） | **接近完成** | 策略已定；V15 + 五维 Agent + 动作规则 + 异步联动 + 管理台完成（307 项测试）；端到端验证通过；仅剩评测实验 | 本文 |
| M17 | 运营分析 Agent 与智能推荐 | **进行中** | T9/T10/T11 验证通过；V16/V17 建表、AnalystAgent、三路召回 + RRF 融合 + 推荐编排、纯热榜基线、洞察存取已落地（343 项测试）；推荐理由/定序方式与评测口径待决策 | `m17-decision-brief.md` |
| M18 | 可观测性、评估与工程化闭环 | **进行中** | T12/T13 前置验证通过；V18 + `TraceRecorder` + 四条链路埋点（对话/审核/洞察/推荐）+ 管理端轨迹接口已落地（381 项测试）；用量、降级、前端待做 | `m18-decision-brief.md`、`m18-handoff.md` |

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
| 技能文档记录的仓库路径 `$HOME/Developer/Projects/Java/BitFrom/bit-forum-spring` 不存在 | 1 | 实测确认真实路径为 `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring`，已按真实路径作业 |
| `web_fetch` 工具对 `spring.io` / `docs.spring.io` / `raw.githubusercontent.com` 报「解析到非公网 IP」 | 1 | 改用 `curl` 抓取官方文档并用 Python 剥离 HTML 标签提取正文，成功获取全部所需原文 |
| `spring-ai-spring-boot-dependencies` 坐标 404 | 1 | 确认该 artifact 名称不存在，改用 `spring-ai-bom` 与各 starter 的 POM 直接验证依赖关系 |
| **redis-stack-server 模块未加载**：`MODULE LIST` 空、`FT._LIST` 报 unknown command | 1 | 根因是 `command: ["redis-server", ...]` 覆盖了镜像的 `/entrypoint.sh`（它负责 `--loadmodule`）。改用 `REDIS_ARGS` 环境变量追加参数后模块全部正常加载 |
| Redis 向量探针脚本解析 score 报 `unsupported format string passed to bytes.__format__` | 1 | 实测发现 RediSearch 返回的 KNN 距离是十进制字符串（如 `b'0.00362026691437'`）而非二进制 float32，改用 `float()` 解析 |
| **push 后 CI backend job 失败**（Errors: 226）：`ERR unknown command 'FT._LIST'` | 1 | 根因是 CI 的 redis service 仍是 `redis:7-alpine`（无 RediSearch），而 M13 只换了本地镜像、未同步 CI。已改用普通 Redis 容器在 6380 端口**本地复现**同一异常链，确认因果；修复为把 CI 镜像换成 `redis/redis-stack-server:7.4.0-v8`，并用全新 Stack 容器（0 索引）跑全量验证 278 项通过。详见 findings.md 6.11 |

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

## 2026-09-18（续）：本机开发脚本

### 背景

每次启动项目需要分别执行中间件、后端、前端三条命令，且在 macOS 上要先手动
`cd` 到项目目录。为此新增两个脚本，简化日常开发。

### 新增脚本

| 脚本 | 作用 |
| --- | --- |
| `dev.sh` | 一键启动：Docker 中间件 → 等待健康 → 后端(8080) → 前端(5173) |
| `stop-local.sh` | 停止后端与前端；`--all` 连中间件一起停；`--status` 只看状态 |

两个脚本都被 `.gitignore` 忽略（含从 `.env` 读取凭据的逻辑，按本机环境各自维护）。

### 实现要点

- 中间件用 `docker compose up -d` 并轮询健康检查，超时给出明确错误
- 后端环境变量从 `.env` 显式导出（Spring Boot 不会自动读 `.env`）
- 后端日志同时写入 `.dev-logs/backend.log`，便于回溯启动失败原因
- 端口占用时**只结束属于本项目的进程**（按工作目录匹配 `bit-forum-spring`），
  避免误杀其他项目的 8080/5173
- 首次运行自动 `npm install`

### 关键发现：Ctrl+C 无法可靠停止后端（macOS）

实测结论，作为已知限制记录下来：

1. **macOS 没有 GNU 的 `setsid`**，不能靠它给子进程建独立进程组。
2. 后端与脚本同属一个进程组时，终端 Ctrl+C 会把 SIGINT 发给整组；
   **Maven 忽略 SIGINT 继续运行**，而 bash 在收到该信号后直接退出，
   来不及执行已注册的 `cleanup` trap。结果是前端被停掉、后端残留并继续占用 8080。
3. 改为轮询（不用 `wait`）后 trap 稳定性有改善，但仍受上述同组信号影响。

**因此采用务实方案**：脚本启动完成后**明确提示用 `./stop-local.sh` 停止**，
该脚本按「端口 + 工作目录匹配」定位进程，实测能可靠释放 8080 与 5173。
`cleanup` trap 仍然保留，作为运气好时的额外清理。

### 验证记录

| 验证项 | 结果 |
| --- | --- |
| `dev.sh` 完整启动 | 通过：中间件就绪 → 后端就绪 → 前端就绪，约 40 秒 |
| 后端接口 | `GET /actuator/health` → `{"status":"UP"}` |
| 前端与代理 | `5173 → /` HTTP 200；`5173 → /api/ai/conversations` HTTP 401（代理正常）|
| `stop-local.sh` | 通过：输出「前端 已停止 / 后端 已停止」，两个端口均释放 |
| 残留进程 | 无 java / vite 残留 |
| 脚本权限 | 755 |
| `bash -n` 语法检查 | 两个脚本均通过 |

### 已知限制

- 直接 Ctrl+C 只停前端，后端需用 `./stop-local.sh`（原因见上）
- 脚本不提交到仓库，Windows 环境需另写 `.cmd` 版本（历史已有 `run-local.cmd`）

---

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

---

## M15 执行记录（2026-09-19）

### M15-1：ONNX 嵌入模型最小验证（findings.md 待验证事项 T3）

- **Status:** complete

#### 为什么先做这一步

handoff 第三节指出：DeepSeek 不提供 embedding 接口，RAG 的 `EmbeddingModel` 必须另找来源；
本地 ONNX 是三条回退路径中的首选，但它是**整个计划里唯一没实测过的技术点**。
按 M13 的教训（`@ConditionalOnBean`、占位 Key 都是真实调用时才暴露问题），
本轮**只加依赖、不写业务代码**，先把"模型能加载 + 能产出向量"跑通，验证通过再进主体。

#### 实测发现（完整证据见 findings.md 6.8）

1. `TransformersEmbeddingModelAutoConfiguration` 只有 `@ConditionalOnClass`、**没有 `@ConditionalOnProperty`**
   —— 只要依赖在类路径上，每个 Spring 上下文启动都会构建 ONNX 会话并加载模型文件，无法用配置开关关闭。
2. 默认模型地址指向 **GitHub 的 main 分支**（不是 HuggingFace），默认缓存目录在**系统临时目录**；
   两者都已在本项目配置中显式固定（写死 URI + `~/.cache/bitforum-onnx`）。
3. `TransformersEmbeddingModel` 内部 pooling **硬编码为 mean、不可配置**，而不同模型的训练约定不同
   （BGE 用 CLS、MiniLM/e5 用 mean）→ **选型不能看模型名气，必须实测**。

#### 三模型检索质量实测

用 `EmbeddingModelComparisonProbe` 在 5 条中文「查询 → 相关文档 + 干扰文档」样本上评测，
指标是"相关文档能否排第一"以及"相关分 − 最高干扰分"的平均差距（差距越大排序越稳）：

| 模型 | 维度 | 体积 | Top-1 命中 | 平均区分度差距 |
| --- | --- | --- | --- | --- |
| all-MiniLM-L6-v2（Spring AI 默认，英文） | 384 | 90MB | 2/5 | −0.0507 |
| **bge-base-zh-v1.5（定稿）** | **768** | 102MB | **5/5** | **+0.1914** |
| multilingual-e5-small | 384 | 118MB | 5/5 | +0.0493 |

**定稿 `Xenova/bge-base-zh-v1.5` 的通用动态量化版（102MB，768 维）**：中文排序质量最优，
平均区分度约为 e5 的 4 倍（e5 虽然 Top-1 也对，但分数全挤在 0.85~0.94，实际检索中容易被噪声反超）；
且通用动态量化不绑定 CPU 架构，双机（Windows / MacBook）开发无需重新导出。

#### 启动耗时实测

| 场景 | 实测 |
| --- | --- |
| 首次（含下载 tokenizer + model.onnx） | Spring 上下文启动 **52.16 秒** |
| 缓存命中后（日志无 `Caching the URL`） | Spring 上下文启动 **2.24 秒** |

#### 验证记录

| 命令 | 结果 |
| --- | --- |
| `-Dtest=OnnxEmbeddingSmokeTest` | 2 项通过；维度 768 稳定、元素有限、中文相关 > 不相关 |
| `-Dtest=EmbeddingModelComparisonProbe`（需 `EMBEDDING_PROBE=true`） | 5/5 命中；不带环境变量时正确跳过（Skipped: 1） |
| `mvn test`（全量，清空 surefire-reports 后重跑） | **Tests run: 248, Failures: 0, Errors: 0, Skipped: 1**；耗时 28.7 秒 |
| `npm run build` | 通过，1889 模块（与 M14 基线一致） |

测试数说明：handoff 记录的 233 项是 `6ce4956` 时的数字；其后 `42a6b9c`（作者可打开自己的未发布文章）
新增 `ArticleDetailVisibilityTest`，基线变为 246 项；本轮新增 `OnnxEmbeddingSmokeTest` 2 项 → 248 项。

#### 本模块修改/新增文件

| 文件 | 变更 |
| --- | --- |
| `pom.xml` | 新增 `spring-ai-starter-model-transformers`（`vector-store-redis` 留到检索阶段再引入） |
| `src/main/resources/application.yml` | 新增 `spring.ai.embedding.transformer.*`：显式模型 URI + 持久缓存目录 |
| `src/test/resources/application.yml` | 同上（测试类路径下同名文件整体覆盖主配置，必须重复写） |
| `src/test/java/com/bitforum/ai/embedding/OnnxEmbeddingSmokeTest.java` | 新增，2 项，断言做成模型无关 |
| `src/test/java/com/bitforum/ai/embedding/EmbeddingModelComparisonProbe.java` | 新增，选型评测探针，默认不参与回归 |

#### 遗留优化点（进入主体实现时要处理）

- BGE 官方建议给 **query 一侧**加检索指令前缀「为这个句子生成表示以用于检索相关文章：」
  （passage 一侧不加），应在 `RagService` 中应用，并用真实站内文章复测。
- Redis 向量索引 `DIM` 必须写 **768**，写错会导致 `FT.CREATE` 失败。
- 模型文件缓存在 `~/.cache/bitforum-onnx`，不进入仓库；新机器首次运行需要联网下载 102MB。

### M15-2：Flyway V14 与 Redis 向量库接入

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 迁移 | `src/main/resources/db/migration/V14__add_ai_kb_document.sql` | 新增 `ai_kb_document`、`ai_kb_chunk`（未改 V1-V13） |
| 配置类 | `src/main/java/com/bitforum/ai/config/VectorStoreConfig.java` | 自定义 `JedisPooled` 与 `RedisVectorStore` bean |
| 配置 | `application.yml`（主 + 测试两处） | `spring.ai.vectorstore.redis.*`（索引名、前缀、initialize-schema） |
| 测试 | `src/test/java/com/bitforum/ai/rag/RedisVectorStoreSmokeTest.java` | 2 项：索引维度断言、元数据过滤检索 |

#### 表设计要点

`ai_kb_document` 一篇文章一行，`article_id` 加唯一约束（全量重建时按此 upsert，防重复索引）。
其中三个字段是计划书没写、实施中补上的，理由如下：

| 字段 | 为什么加 |
| --- | --- |
| `content_hash` | 文章每次保存都重新算向量很浪费（一次嵌入要几百毫秒）；指纹未变就直接跳过 |
| `index_status` | 管理端要能回答"还有多少篇没进知识库 / 失败"；也是失败重试的基础 |
| `embedding_model` | 本轮就换了 3 个模型；记下来才能在换模型时自动识别"这些向量是旧模型算的、需要重建" |

`ai_kb_chunk` 一段一行，`(document_id, chunk_index)` 唯一；`vector_id` 保存该分块在 Redis 中的向量 id，
删除与重建时用于精确定位。与既有表一致，只加索引不建物理外键。

#### 关键发现：starter 的自动配置在本项目里用不了

详细证据见 findings.md 6.9.1，两点原因：

1. 自动配置的 `vectorStore` 方法要求容器里有 `JedisConnectionFactory`，
   而本项目用的是 `spring-boot-starter-data-redis` 默认的 **Lettuce**，Spring Boot 不会再建 Jedis 连接工厂；
2. 自动配置**不支持声明元数据字段类型**，而 RediSearch 要求过滤用到的字段必须建索引时声明。

因此改为自己声明 bean（连接参数仍复用 `spring.data.redis.*`，不引入第二套配置）；
自动配置的 bean 带 `@ConditionalOnMissingBean`，会自觉让路。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| Flyway V14 | `Successfully applied 1 migration ... now at version v14`；`ai_kb_document` / `ai_kb_chunk` 已在 information_schema 中确认存在 |
| 索引维度 | `FT.INFO bitforum-kb` → `dim, 768`、`algorithm, HNSW`、`data_type, FLOAT32`、`distance_metric, COSINE` |
| 元数据字段 | `articleId` / `categoryId` / `status` = TAG；`publishTime` / `chunkIndex` = NUMERIC |
| **T5 过滤检索** | 写入 3 条（2 条 PUBLISHED + 1 条 OFFLINE），过滤检索只返回 2 条 PUBLISHED，**下架内容未被召回** |
| `mvn test`（全量，清空报告后重跑） | **Tests run: 250, Failures: 0, Errors: 0, Skipped: 1**；31.3 秒 |
| `npm run build` | 通过，1889 模块 |

#### 踩坑

jedis 的 `ftInfo` 返回的 `attributes` 是**扁平的键值列表**而不是 `Map`，
首版按 Map 解析导致一处断言失败，已改为按相邻两元素配对解析。

### M15-3：文章分块与索引服务（手动全量重建）

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 实体 | `ai/entity/AiKbDocument.java`、`ai/entity/AiKbChunk.java` | 对应 V14 两张表 |
| Mapper | `ai/mapper/AiKbDocumentMapper.java`、`ai/mapper/AiKbChunkMapper.java` | 所在包已在 `@MapperScan` 列表中，无需改扫描配置 |
| 服务 | `ai/rag/ArticleChunkingService.java` | 按段落/句子切块，带标题上下文与相邻重叠 |
| 服务 | `ai/rag/KbIndexService.java` | 单篇索引、全量重建、移除、统计 |
| 接口 | `controller/AdminAiKbController.java` | `POST /api/admin/ai/kb/rebuild`、`GET /api/admin/ai/kb/stats` |
| 配置 | `application.yml` | `bitforum.ai.rag.*`：模型标识、分块长度、重叠长度 |
| 测试 | `ArticleChunkingServiceTest`（6 项）、`KbIndexServiceTest`（5 项）、`AdminAiKbControllerTest`（4 项） | |

#### 分块参数与依据

| 参数 | 取值 | 依据 |
| --- | --- | --- |
| 分块长度 | 350 字 | bge-base-zh-v1.5 有效输入约 512 token，中文约 1 字 1 token，留余量避免模型侧静默截断 |
| 重叠长度 | 50 字 | 防止一个完整结论正好落在切分边界被切断 |
| 标题前缀 | 每块前置 `《标题》` | 单块被召回时模型看不到其它块，标题提供自解释的上下文 |
| 超长无标点段落 | 按目标长度硬切 | 否则单块会超出模型输入上限 |

#### 一致性策略

向量写入（Redis）与元数据写入（MySQL）无法放进同一个事务，因此顺序固定为
「先删旧向量与旧分块 → 落 document 行 → 写向量 → 写分块记录 → 标记 INDEXED」；
任一步失败就把文档标记为 `FAILED` 并记录 `last_error`，由下次重建修复。
检索侧只信 MySQL 中的 `INDEXED` 记录，半成品不会被当作可用知识。

增量更新用内容指纹判定：指纹与嵌入模型都未变化时直接跳过（`SKIPPED`），
避免文章每次保存都重新计算向量；指纹变化时按分块表保存的 `vector_id` 精确删除旧向量再重建。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| **验收：统计数一致** | 全量重建后 `indexedDocuments=6` == `publishedArticles=6`（已发布 6 篇 → 索引 6、跳过 0、失败 0，231 ms） |
| 检索召回 | 新索引的文章可被语义查询命中（`filterExpression("status == 'PUBLISHED'")`） |
| 指纹跳过 | 内容与模型未变化 → 第二次索引返回 `SKIPPED` |
| 内容变化 | 内容修改后重新索引，且不残留旧内容的分块记录 |
| 下架移除 | 状态改为 `OFFLINE` 后再次索引 → `REMOVED`，文档与分块记录被删除，向量删除后不再被召回 |
| 分块边界 | 单块长度不超过「目标长度 + 重叠 + 标题前缀」上限 |
| 接口权限 | 匿名 401、普通用户 403、管理员 200 且返回 `Result` 结构 |
| `mvn test`（全量，清空报告后重跑） | **Tests run: 265, Failures: 0, Errors: 0, Skipped: 1** |
| `npm run build` | 通过，1889 模块 |

#### 修复的缺陷

分块合并时未把换行分隔符计入长度，导致单块比目标长度多 1 个字符（由边界测试暴露），已修正。

#### 覆盖说明

`AdminAiKbControllerTest` 只验证未授权路径与 `stats` 接口，**不触发真实重建** ——
重建会写入 Redis 向量，而 `@Transactional` 只能回滚 MySQL，会留下孤儿向量；
重建的业务正确性由 `KbIndexServiceTest` 覆盖。

### M15-4：检索服务与回答引用注入

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 服务 | `ai/rag/RagService.java` | 语义检索：状态过滤 + 二次校验 + topK/长度限制 |
| 模型 | `ai/rag/Citation.java` | 引用来源 record（articleId + title） |
| 修改 | `ai/agent/QaAgent.java` | 每轮先检索、注入片段、提示词增加引用规则 |
| 修改 | `ai/agent/AgentResponse.java` | 增加 `citations` |
| 修改 | `ai/orchestrator/AgentOrchestrator.java` | 引用落库 + 直接返回前端 |
| 修改 | `ai/service/AiConversationService.java` | 保存与还原引用（批量查标题，避免 N+1） |
| 修改 | `ai/dto/AiMessageResponse.java` | 增加 `citations` |
| 修改 | `application.yml` | `top-k` / `similarity-threshold` / `max-chunk-chars` / `query-prefix` |
| 前端 | `AiAssistantPanel.jsx`、`styles/ai-panel.css` | 助手消息下方渲染「参考来源」可点击链接 |
| 测试 | `RagServiceTest`（3 项）、`RagQaSmokeTest`（1 项，默认跳过） | |

#### 关键设计决策

1. **不用 `QuestionAnswerAdvisor`，改为自己拼检索上下文**。
   计划书写的是接入 `QuestionAnswerAdvisor`，实施时改为主张：
   M14 的工具调用已经占用了 prompt 组装，框架 advisor 会再改写一遍 prompt，
   两者叠加后 token 不可控、引用来源也难以精确记录；
   自己拼可以精确控制片段数量与长度，并把**引用独立于模型输出**——
   即使模型忘记标注编号，前端仍能显示可点击链接。
2. **三重召回约束**：检索请求带 `status == 'PUBLISHED'` 过滤 → 召回后二次校验 →
   `topK=5`、每段截断 300 字（M14 已观察到工具调用把单轮 token 推到 8000+）。
3. **二次校验查 `article` 表的当前状态，而不是索引快照**：
   文章下架后若索引消息尚未消费，Redis 中仍有旧向量，只查 `ai_kb_document`
   （索引时快照）会漏判。`RagServiceTest` 专门覆盖了这条兜底。
4. **BGE 查询前缀**：query 侧加「为这个句子生成表示以用于检索相关文章：」（passage 侧不加），
   落实 findings.md 6.8.4 记录的遗留优化点。
5. **检索片段不写入会话记忆**：只作为本轮上下文注入，不随轮次累积、不污染后续对话。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 检索召回 | 相关文章被召回（1 段，29 ms），引用自带标题 |
| **下架兜底** | 只改 `article.status`、不重建索引时，检索不再召回该文章 |
| 空查询 | 不触发向量检索 |
| **真实链路（DeepSeek）** | 回答「根据站内《玄武岩缓存策略说明》[1]，它的失效处理方式是这样的：…」；引用来源 = `[Citation[articleId=1469, title=玄武岩缓存策略说明]]`；合计 3014 token |
| 引用持久化 | 引用 id 写入 `ai_message.retrieved_doc_ids`，刷新页面重新加载历史时链接仍在 |
| 前端 | 助手消息下方渲染「参考来源」列表，点击跳转 `/articles/:id` |
| `mvn test`（全量） | **Tests run: 268, Failures: 0, Errors: 0, Skipped: 1**；39.2 秒 |
| `npm test` / `npm run build` | 23 项通过 / 构建通过 |

#### 遗留

- 相似度阈值当前为 0.5，文章库变大后需要用真实数据复测（避免过严导致漏召回）。
- 异步索引（文章发布/更新/下架自动重建）与管理端统计页仍属于 M15 后续步骤。

### M15-5：异步索引与管理端知识库页（M15 收尾）

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 消息 | `message/KbIndexMessage.java` | 只携带 `articleId` + `messageId` |
| 队列 | `config/RabbitMQConfig.java` | 新增 `article.kb.index.queue` 与独立 DLQ |
| 触发 | `service/ArticleService.java` | 审核通过 / 下架 / 删除后投递索引消息（事务提交后） |
| 消费 | `ai/rag/KbIndexMessageListener.java` | 手动 ACK + Redis 幂等 + 死信队列 |
| 前端 | `pages/admin/KnowledgeBase.jsx`、`AdminLayout.jsx`、`App.jsx`、`api/adminApi.js` | 管理端知识库统计页 + 全量重建按钮 |
| 样式 | `styles/layout.css` | 统计卡片样式 |
| 测试 | `KbIndexMessageListenerTest`（5）、`KbIndexTriggerTest`（2）、`KbAutoIndexIntegrationTest`（2） | |

#### 触发点

只挂三个真实改变可见性的点（**已发布文章不可编辑**，`ArticleService.update` 仅允许草稿与被驳回文章，
因此不存在"内容修改"触发点）：

| 事件 | 消息投递 | 消费者动作 |
| --- | --- | --- |
| 审核通过 | ✅ | 文章为 PUBLISHED → 写入索引 |
| 下架 | ✅ | 非 PUBLISHED → 移除索引 |
| 删除（作者 / 管理员） | ✅ | 查不到 → 移除索引 |

#### 关键实现要点（详见 findings.md 6.10）

1. **消息在事务提交后发送**：`approve()` 等方法是 `@Transactional`，若事务内直接投递，
   消费者会读到旧状态（PENDING）而把文章移出知识库，事务提交后又不再触发索引 —— 造成永久漏索引。
   实现上用 `TransactionSynchronizationManager` 注册 `afterCommit` 回调。
   相应地，`KbIndexTriggerTest` **刻意不加 `@Transactional`**，否则测的是"不发送"这个假象。
2. **消费者按文章当前状态决定写入还是移除**：三个触发点共用一种消息，消息堆积后也能收敛到正确状态。
3. **队列隔离（T8 验证通过）**：独立队列 + 独立死信队列，不影响既有文章发布通知链路。
4. **MQ 不可用不影响主流程**：发送失败只记日志；漏掉的消息可用管理员全量重建接口补齐。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 监听器分支 | 5 项：正常索引 / 下架移除 / 删除移除 / 重复消息只 ACK / 异常进死信 |
| 触发点 | 2 项：审核通过与管理员删除均发出索引消息（提交后投递） |
| **端到端（真实 RabbitMQ）** | 审核通过 → 异步入库 → INDEXED 且分块数一致 → 可被语义检索召回；下架 → 自动移除且检索不到 |
| 管理页 | 新增 `/admin/kb`，展示已发布 / 已入库 / 待处理 / 失败 / 片段数与嵌入模型，含「全量重建」 |
| `mvn test`（全量） | **Tests run: 278, Failures: 0, Errors: 0, Skipped: 2**；35.2 秒 |
| `npm test` / `npm run build` | 23 项通过 / 构建通过 |

#### M15 完成情况对照计划书验收标准

| 计划书验收标准 | 结果 |
| --- | --- |
| 知识库统计数与已发布文章数一致 | ✅ 全量重建后 `indexedDocuments == publishedArticles` |
| 提问能召回正确帖子且回答带引用链接 | ✅ 真实调用实测：回答带 `[1]` 标注，引用 articleId=1469，前端渲染可点击原帖链接 |

### M15-6：CI 环境修复（push 后发现）

- **Status:** complete

首次 push 后 GitHub Actions 的 backend job 失败（`Errors: 226`）。完整定位过程与证据见 findings.md 6.11。

| 项 | 内容 |
| --- | --- |
| 失败步骤 | `Run backend tests`（frontend job 正常通过） |
| 根因 | CI 的 redis service 是 `redis:7-alpine`，**不含 RediSearch 模块**；向量库初始化执行 `FT._LIST` 报 unknown command → Spring 上下文启动失败 |
| 连锁现象 | 226 个 errors 全部来自 Spring 的 "context failure threshold exceeded"，**不是 226 个独立缺陷** |
| 本地复现 | 用 `redis:7-alpine` 容器（6380 端口）跑单测，得到完全相同的异常链，确认因果 |
| 修复 | `.github/workflows/ci.yml` 的 redis 镜像改为 `redis/redis-stack-server:7.4.0-v8`（不加 `command` 覆盖，否则模块不加载） |
| 本地验证 | 用**全新** Redis Stack 容器（0 索引，等同 CI 干净环境）跑全量 → **278 项通过**；索引自动创建且 `dim = 768` |
| **CI 实测** | 推送后 run `35422730508`：**backend 与 frontend 两个 job 均通过**（backend 2m18s，日志为 `Tests run: 278, Failures: 0, Errors: 0, Skipped: 2` + `BUILD SUCCESS`）；runner 上模型成功下载、索引自动创建 |

**有意不采用的做法**（保留 CI 的有效覆盖）：删除或跳过向量相关测试、在 CI 关闭 `initialize-schema`、跳过 backend job。

**后续提醒**：CI 每次运行都会重新下载 ONNX 模型（102MB，实测可成功）。
若后续发现 backend job 耗时过长，可加 `actions/cache` 缓存 `~/.cache/bitforum-onnx`——本次按"最小改动"原则未加。

### M15 已完成（小结）

五个提交覆盖：模型验证（`96fcae9`）→ 建表与向量库（`29f96d8`）→ 分块与索引（`0c81b27`）
→ 检索与引用（`e963bf3`）→ 异步索引与管理页（`930a081`），外加 CI 环境修复（本次）。

遗留（不阻塞交付）：

- 相似度阈值 0.5 待文章库变大后复测。
- 知识库集成测试收尾会清空索引，开发/演示前需调用一次全量重建。

---

## M16 执行记录（2026-09-19）

### M16-0：审核策略决策（与外部 AI 讨论后确定）

- **Status:** complete

M16 的唯一技术未知点是「结构化输出能否稳定映射成 Java 对象」（findings.md 的 T7）。
按 M15 的经验先做前置验证，结论是**技术可行**（解析 16/16、字段完整 16/16），
但实验暴露出真正的问题不在技术而在**策略**：

| 提示词 | 决策与预期相符 |
| --- | --- |
| A 保守版（只说"拿不准就转人工"，不给判据） | 5/8 |
| B 明确判据版（逐条写出 REJECT 的适用情形） | 8/8 |

A 版把「明显广告 / 人身攻击 / 疑似诈骗」全判成 REVIEW —— 不是模型不会拒绝，而是提示词没给判据；
B 版全部正确但变得机械，连"方法放在我主页了"这种模糊表述也直接拒绝。

因此由项目作者与外部 AI 讨论确定了审核策略（讨论材料见 `m16-decision-brief.md`，
结论已固化到 `task_plan.md` 的 M16 实施决策）。**三条最关键的约定**：

1. **Decision 与 Action 解耦** —— 三档判断只是 AI 判断，不等于数据库动作；
2. **自动 PASS 但不自动 REJECT** —— 唯一允许的自动动作是「高置信 PASS 自动放行」，绝不自动驳回或删除；
3. **评测集留独立测试集** —— 阈值只在开发集上调，最终指标只在未参与调参的测试集上产出。

### M16-1：V15 建表 + 五维审核 Agent + 动作规则

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 迁移 | `V15__add_ai_moderation_record.sql` | 审核记录表，AI 判断与系统动作**分列保存** |
| 枚举 | `ModerationDecision` / `ModerationAction` / `ModerationTargetType` | 三档判断 / 系统动作 / 对象类型 |
| 模型 | `ModerationAssessment` | 五维固定字段的结构化结论（刻意不含综合分） |
| Agent | `ModerationAgent` | 五维分析，不抛异常（AI 不可用时降级） |
| 服务 | `ModerationService` | Decision → Action 的确定性规则 + 落库 + 自动放行 |
| 实体 | `AiModerationRecord` + `AiModerationRecordMapper` | |
| 配置 | `application.yml` | `bitforum.ai.moderation.*`，阈值刻意用「永不满足」的安全占位 |
| 测试 | `ModerationServiceTest`（11 项）、`ModerationAgentSmokeTest`（1 项，默认跳过） | |

#### 关键实现说明

1. **五维用固定字段而不是列表**：T7 实测发现让模型返回「维度列表」时数量不稳定（4/5/6 个都出现过），
   固定字段才能在数据库与校验层面强制五维齐全。
2. **综合分由 Java 计算**（取五维最大值），不用模型自评分数 —— 阈值判定必须可复现、可解释。
3. **降级路径**：AI 不可用时返回 `ANALYSIS_FAILED`，判断兜底为 REVIEW、五维记 0 分，
   语义由 action 与 errorMessage 表达，不会被误当成「低风险可放行」。
4. **自动放行复用 `ArticleService.approve`**，因此审核记录、作者通知、知识库索引等既有流程一个不少；
   放行失败（如文章状态已被人工改变）会降级为待人工，而不是吞掉异常。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| V15 迁移 | 已应用（`flyway_schema_history` version=15），`ai_moderation_record` 表已在 information_schema 中确认 |
| 动作规则单测 | 11 项通过：REJECT 绝不触发自动动作、开关关闭时不放行、置信度/风险分不足不放行、默认占位阈值永不自动放行、放行失败降级、评论 PASS 无动作 |
| **真实调用冒烟** | 8 条样本 8/8 成功；决策分布 `[PASS, REJECT, REJECT, REJECT, PASS, REJECT, REJECT, REVIEW]`；广告/攻击/诈骗均正确判 REJECT，正常内容未被误伤 |
| `mvn test`（全量） | **Tests run: 290, Failures: 0, Errors: 0, Skipped: 3**；34.5 秒 |
| `npm run build` | 通过 |

#### 观察到的待调点（交由评测集阶段用数据决定）

- 「灌水」被判 REJECT（spam=1.00）：是否违规取决于社区规则，属产品决策；
- 「软性引流」被判 REJECT（promotion=0.90）：决策要求「模糊内容判 REVIEW」，此处可能偏严。

两者都需要在带人工标注的评测集上量化后，再决定是否调整提示词或阈值 —— 符合"阈值由实验确定"的约定。

### M16-2：审核流联动（RabbitMQ 异步触发）

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 消息 | `message/ModerationMessage.java` | 只携带对象类型 + id，消费者回查内容 |
| 工具 | `message/AfterCommitExecutor.java` | 抽取「事务提交后执行」公共组件 |
| 队列 | `config/RabbitMQConfig.java` | 新增 `article.moderation.queue` 与独立 DLQ |
| 触发 | `service/ArticleService.java` | `submit()` 后投递；同时重构 `sendKbIndexMessage` 复用公共组件 |
| 触发 | `service/CommentService.java` | `publish()` 后投递 |
| 消费 | `ai/moderation/ModerationMessageListener.java` | 手动 ACK + Redis 幂等 + 死信队列 |
| 测试 | `ModerationMessageListenerTest`（6 项）、`ModerationTriggerTest`（2 项） | |

#### 关键设计

1. **消费者回查内容，且只审核仍处于 `PENDING` 的文章**：管理员已手动处理过的文章不再重复分析 ——
   既省掉无谓的模型调用，也避免自动放行去改一个状态已经变化的文章。
2. **消息只带对象 id、不带正文**：避免消息体过大（文章可能上千字），也避免消费到已被修改的旧内容。
3. **抽取 `AfterCommitExecutor`**：M15 把「事务提交后发送」内联写在 `ArticleService`，
   现在评论发布也要发消息，抽成公共组件，避免两处各写一份、写漏一处就是难查的时序 bug。
4. **MQ 故障不影响主流程**：漏掉的审核消息只会让内容回到"纯人工审核"的原始流程，
   不会导致内容丢失或误放行。
5. **审核标题而不只是正文**：文章送审时用「标题 + 正文」，标题党的风险往往比正文更明显。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 监听器分支 | 6 项：待审文章送审（标题+正文）、非待审跳过、对象不存在跳过、评论送审、重复消息只 ACK、异常进死信 |
| 触发点 | 2 项：提交审核 / 评论发布后确实投递了审核消息（事务提交后） |
| `mvn test`（全量） | **Tests run: 298, Failures: 0, Errors: 0, Skipped: 3**；30.2 秒 |
| `npm run build` | 通过 |

#### 踩坑

`notification` 表的接收人列名是 `receiver_id` 而不是 `user_id`，测试的清理语句因此报 `BadSqlGrammar`（已修正）。

### M16-3：管理台与端到端验证

- **Status:** complete

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 接口 | `controller/AdminAiModerationController.java` | 记录查询、人工反馈、标记已处理 |
| DTO | `ai/dto/ModerationFeedbackRequest.java` | 反馈请求（CORRECT / WRONG） |
| 服务 | `ModerationService` 新增 | `pageRecords` / `submitFeedback` / `markHandled` |
| 前端 | `pages/admin/ModerationRecords.jsx`、`AdminLayout.jsx`、`App.jsx`、`api/adminApi.js` | 管理页 `/admin/moderation` |
| 样式 | `styles/layout.css` | 决策标签、筛选栏、展开详情 |
| 测试 | `AdminAiModerationControllerTest`（7 项）、`ModerationAutoReviewIntegrationTest`（2 项，默认跳过） | |

#### 管理台设计要点

1. **默认视图是「只看待处理」**，排序为「高优先级在前 + 时间倒序」——管理员打开页面第一眼
   看到的就是最该处理的 REJECT 记录。
2. **没有「按 AI 判断直接驳回」的按钮**：驳回/删除始终走原有文章审核流程由管理员决定，
   与实施决策「绝不自动驳回」保持一致。
3. **可展开查看五维理由与动作依据**：把"AI 为什么这么判"与"系统为什么这么动作"都摊开，
   便于管理员判断该不该采信；这也是人工反馈闭环的前提。
4. **人工反馈与处理标记分离**：反馈记录的是"AI 判断对不对"，处理标记记录的是"这单办完了没"，
   两个维度独立，避免互相覆盖。

#### 端到端验证（真实 MQ + 真实 AI 调用）

```text
文章：提交审核 → 异步入库 → AI 判 PASS（置信 1.0）→ action = PENDING_REVIEW
      ⇒ 文章仍是 PENDING（默认开关关闭，不自动放行）—— 直接印证「自动 PASS 需显式开启」
评论：发布 → 异步审核 → AI 判 PASS → action = NO_ACTION
      ⇒ 评论已公开，无需动作，也不产生任何删改 —— 印证「评论只做事后检测」
```

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 管理台接口 | 7 项：匿名 401、普通用户 403、分页查询、待处理筛选（高优先在前）、反馈落库、非法反馈值 400、标记已处理 |
| **端到端联动** | 2 项通过：提交文章 / 发布评论后均自动产生 AI 审核记录；默认不自动放行；评论动作不含任何删改 |
| `mvn test`（全量） | **Tests run: 307, Failures: 0, Errors: 0, Skipped: 5**（5 项均为需要真实 API Key 的调用测试） |
| `npm test` / `npm run build` | 23 项通过 / 构建通过 |

### M16-4：评测规范与评测实验（M16 收尾）

- **Status:** complete

#### 先写规范，再跑数据（方法论要求）

用户指出：如果不先把"什么算对"钉死，每改一次提示词就可能顺手改一次标注，
最终 100 条数据会失去说服力。因此先产出并冻结 **`m16-eval-protocol.md`（评测规范 v1）**：
社区内容规则（可判定的条款编号）、Gold Label 标注规则、指标公式、数据集边界、阈值流程，
以及**「规则=球门、提示词=实现」**的原则（为对齐规则改提示词允许，为迁就模型改规则不允许）。

#### 产出

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 规范 | `m16-eval-protocol.md` | v1 冻结，含变更记录与阈值迭代记录 |
| 样本 | `scripts/ai-eval/moderation-dev-samples.json`（20）+ `moderation-dev-samples-extra.json`（50） | 开发集 70 条，每条标注引用规范条款 |
| 样本 | `scripts/ai-eval/moderation-test-samples.json` | **独立测试集 30 条**，冻结阈值后才运行且只运行一次 |
| 评测器 | `ModerationEvaluationTest` | 一级/二级指标 + REVIEW 落点分布 + 分类混淆矩阵 + 阈值扫描 |

#### 评测结果

| 指标 | 开发集（70 条） | 测试集（30 条） |
| --- | --- | --- |
| **漏放率**（硬约束 = 0） | **0.0%**（0/32） | **0.0%**（0/13） |
| **误伤率** | **0.0%**（0/30） | **0.0%**（0/13） |
| **安全召回率** | **100%**（32/32） | **100%**（13/13） |
| 严格三分类准确率 | 97.1%（68/70） | 96.7%（29/30） |
| REJECT 精确率 | 97.0%（32/33） | 92.9%（13/14） |
| REJECT 召回率 | 100%（32/32） | 100%（13/13） |

两个集合结论一致（安全指标全满分、严格准确率约 97%），说明**没有出现过拟合**。

#### 过程中的三个关键发现

1. **硬约束真的抓到了漏放**：开发集首轮跑出 `dev-070`（学历歧视"大专生就别碰分布式了"）
   被判 PASS —— 违规内容被放过。只看总准确率时它会被淹没在 69 条正确里。
2. **提示词按规范条款对齐后修复**：把"学历/职业/性别/地域等身份贬低属于歧视"、
   "模糊引流与含外链内容应判 REVIEW"写进提示词（属规范允许的实现调整），
   漏放消除，promotion 类 3 条偏宽松也一并修正。
3. **阈值过拟合开发集 → 自动放行不启用**：开发集扫描出的最优阈值
   （`confidence ≥ 0.99` 且 `risk < 0.01`，放行 13.3%）在测试集上**放行 0 条（0.0%）**，
   覆盖率条件未通过。按规范禁止用测试集回改阈值，且开发集数据未变、重扫无意义，
   因此结论是**样本量不足以稳定估计阈值**，`auto-approve-enabled` **保持默认关闭**。

> 第 3 点尤其重要：它把"自动放行默认关闭"从一个保守的选择，变成了**有数据支撑的结论**。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 开发集评测 | 漏放 0%、误伤 0%、严格准确率 97.1%（68/70） |
| 测试集验证（只运行一次） | 漏放 0%、误伤 0%、严格准确率 96.7%（29/30） |
| 阈值验证 | **未通过**（覆盖率 0% < 13.3%），如实记录并保持开关关闭 |

### 下一步：M16 收尾

- 全量回归与文档同步
- M16 已实现的功能：五维审核 Agent、Decision/Action 解耦、异步联动、管理台、人工反馈、评测闭环

---

## M17 执行记录（2026-09-19）

### M17-0：最大风险点确认与最小验证（T9）

#### 为什么先做这一步

handoff 要求"先确认 M17 的最大技术风险并做最小验证，再写业务代码"（沿用 M15/M16 惯例）。
对 M17 的分析结论是：**风险不在"三路召回融合"**（那是纯 Java 确定性代码），
而在**输出形态** —— M17 必须让模型输出一个**不定长列表**（Top-N 文章 + 每篇理由），
而 M13-M16 只验证过自由文本与**固定字段**结构；T7 实测恰好发现"让模型返回列表时数量不稳定
（4/5/6 个都出现过）"，M16 正是因此改用固定字段。这是 M17 唯一没实测过的技术点。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/test/java/com/bitforum/ai/recommend/RecommendReasonProbe.java` | T9 探针：A/B 两方案的对照实验（默认跳过，消耗真实额度） |
| `src/test/java/com/bitforum/ai/analyst/AnalystInsightProbe.java` | T10 探针：看板统计 → 模型洞察的「数字保真」（默认跳过） |
| `src/test/java/com/bitforum/ai/recommend/RecommendVectorRecallProbe.java` | T11 探针：向量召回的阈值与区分度实测（`M17_VECTOR_PROBE=true` 启用） |
| `docs/.../findings.md` 6.13 / 6.14 / 6.15 | 三轮验证证据 + 推荐数据现状实测 |
| `docs/.../m17-decision-brief.md` | 待用户确认的五个决策问题（交给外部 AI 讨论） |

#### 验证结果（6 轮真实调用，deepseek-flash）

**技术路径成立**：A（模型自选 id + 理由）与 B（Java 定序、模型只填序号理由）各 3 轮，
**解析 6/6 成功、条数 8/8 精确、id/序号越界 0、重复 0、空理由 0**，单轮 1.6~2.0 秒。

关键推论：T7 的"数量不稳"并非模型能力上限，而是提示词没约束条数；
把「必须恰好 N 篇」写进提示词后数量即稳定 → **M17 提示词必须写死条数**。

两方案的取舍（待决策，见决策简报 Q1）：
A 需把整个候选池喂给模型（成本随候选池线性增长、结果不完全可复现、评测无法归因）；
B 的模型输入恒为 N 篇（成本与候选池解耦、结构上不可能出现 id 幻觉）；
代价是 B 不能补充 Java 召回之外的文章。

#### T10 / T11（同日续做，沿用同一惯例）

**T10 · 运营洞察的数字保真**（2 轮真实调用）：模型调用工具 1 次/轮，
输出数字 10/10、10/12 命中真实统计；未命中的 2 个经核对是百分比换算（2/10、41/44），非幻觉；
模型还主动声明"工具未提供的数据暂无"。看板统计 JSON 仅 699 字符（约 300 token），
**"工具返回值过大"的担心不成立**。→ AnalystAgent 技术风险低。

**T11 · 向量召回的阈值与区分度**：重建索引（5 篇 → 6 段，400 ms）后实测：

- 阈值 0.5 **不是**问题（跨文章相似度 0.66~0.78，均高于门槛）；
- 但**区分度是问题**：以《Redis 热点数据同步方案》为查询，
  《React 与 Vite 前端开发笔记》（主题无关）0.7179 **高于**《Spring Boot 论坛项目实践》0.7131，
  最相关与最不相关只差 0.0048 —— 向量排序接近随机；
- 原因已定位：**已发布文章正文只有 44/46/53/67 字**（测试占位句），仅 1 篇为 621 字，
  可嵌入的信息只剩标题。

→ 对 M17 的硬约束：数据准备必须**同时扩正文长度**；三路召回**不能等权**；
召回需排除查询文章自身；小候选池下"召回率"指标无意义。

#### 数据现状核查（同轮实测，M17 的真正障碍）

| 对象 | 实测 | 问题 |
| --- | --- | --- |
| 已发布文章 | **5 篇** | Top-10 凑不出 10 篇 |
| 已发布文章正文 | **44~67 字**（4/5 篇为占位文本） | 向量召回无区分度（T11 实测） |
| 热榜 ZSet 成员 | **2 篇** | 第二路召回几乎无数据 |
| 向量索引 | 重建前 `num_docs=0`，重建后 6 段 | 第一路召回原先为空，现已重建 |
| 收藏 / 关注 / 用户 | 7 / 4 / 5 | 真值样本不足以支撑比例型指标 |

→ 在 5 篇候选上，纯热榜基线与推荐结果极可能重合，**"命中率对比"无法产出有意义的数字**。
因此 M17 的实现顺序被调整为：**先定评估口径与数据准备方案，再写业务代码**。

#### 待办

- 等用户确认 `m17-decision-brief.md` 的 Q1-Q5（推荐定序方式、验收口径、是否排除已读、洞察生成时机、未登录可见性）
- 确认后：Flyway V16/V17 → AnalystAgent → RecommendAgent → 前端 → 评测实验

---

### M17-1：Flyway V16/V17 与运营分析 Agent（生成侧）

> **范围说明**：用户对 5 个待决问题选择了"先与外部 AI 讨论再定"，
> 因此本轮只推进**与这些决策无关**的部分 —— 计划书已明确要求的建表，
> 以及不依赖"何时生成"的 Agent 生成逻辑。
> 触发方式（Q4）与推荐链路（Q1/Q3/Q5）**未动**。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/main/resources/db/migration/V16__add_ai_insight_report.sql` | 运营洞察报告表 |
| `src/main/resources/db/migration/V17__add_ai_recommend_log.sql` | 推荐记录表（M17 评测数据来源） |
| `src/main/java/com/bitforum/ai/entity/AiInsightReport.java` | 实体 |
| `src/main/java/com/bitforum/ai/entity/AiRecommendLog.java` | 实体 |
| `src/main/java/com/bitforum/ai/mapper/AiInsightReportMapper.java` | Mapper |
| `src/main/java/com/bitforum/ai/mapper/AiRecommendLogMapper.java` | Mapper |
| `src/main/java/com/bitforum/ai/analyst/AnalystTools.java` | 看板统计工具（一个工具返回完整统计） |
| `src/main/java/com/bitforum/ai/analyst/AnalystAgent.java` | 运营洞察生成（含降级链路） |
| `src/test/java/com/bitforum/ai/repository/M17PersistenceIntegrationTest.java` | 建表与列映射验证 |
| `src/test/java/com/bitforum/ai/analyst/AnalystAgentTest.java` | 降级链路与快照留存验证 |

#### 关键设计

1. **报告正文与统计快照同存**（V16）：`content` 是模型写的结论，`data_snapshot` 是当时喂给模型的原始统计。
   只存正文，事后无法回答"报告里那个数字当时对不对"。
   AnalystAgent 因此**只取一次统计**，同一份数据既交给工具给模型读、又作为快照落库 ——
   取两次会拿到两份不同的实时聚合结果，报告与依据就对不上了。
2. **与触发方式解耦**：V16 的 `trigger_type` 预留 MANUAL / SCHEDULED；
   AnalystAgent 只负责"生成"，不负责"何时生成"。Q4 定了之后只需补控制器或监听器，
   表结构与 Agent 都不用改。
3. **`rank_no` 而非 `rank`**：`rank` 是 MySQL 8 保留字；也刻意不用 `is_hit` 作列名
   （MyBatis-Plus 对 `isXxx` 布尔字段按 getter 反推属性名，容易与列名对不上）。
4. **基线对比靠 `experiment_tag` 分组**（V17）：不额外加"基线专用字段"，
   同一批候选分别写入 RECOMMEND 与 BASELINE_HOT 两组记录，评估脚本按批次分组统计命中率；
   该字段为空表示线上真实请求，不参与实验统计。
5. **推荐记录与排序方式解耦**（V17）：无论最终选"模型自选文章"还是"应用定序、模型只写理由"，
   记录字段完全相同（推荐文章、排名、分数、召回来源、理由），因此 Q1 的结论不需要改表。
6. **降级时仍保留快照**：统计是本地聚合的，与 AI 是否可用无关。
   降级记录 `status=FAILED` 但快照可用，便于排查与重试。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| Flyway 迁移 | V16、V17 成功执行，本地库升至 **v17** |
| 持久化集成测试 | 3 项通过（`rank_no` / `hit` / `degraded` 列映射、DECIMAL(12,6) 精度、按 experiment_tag 查询） |
| AnalystAgent 单元测试 | 4 项通过（模型不可用、统计失败均降级且不抛异常；快照留存且为可解析 JSON） |
| 后端全量回归 | **315 项：309 通过 + 6 条件跳过，0 失败**（基线 308 + 本次新增 7） |
| 前端构建 | `npm run build` 成功（1891 模块，962 ms） |

#### 未做（等用户确认）

- 运营洞察的**触发与落库接入**（Q4：同步 / 异步 / 定时）
- RecommendAgent 的三路召回与融合排序（Q1 定序方式、Q3 是否排除已读）
- 推荐接口与前端展示（Q5 未登录可见性）
- 评测规范与命中率对比实验（Q2 验收口径）

---

### M17-2：三路召回与 RRF 融合（推荐链路地基）

> **范围说明**：这部分是"模型自选"与"应用定序"两种方案**共用**的地基 ——
> 无论最终谁来决定推荐列表，都得先把三路候选捞出来并排出顺序。
> 因此可以在 Q1 未定时先做；"谁决定最终列表"与"理由怎么生成"仍未实现。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/bitforum/ai/recommend/RecommendRecallService.java` | 三路召回（vector / hot / follow） |
| `src/main/java/com/bitforum/ai/recommend/RecommendFusionService.java` | RRF 融合排序 |
| `src/test/java/com/bitforum/ai/recommend/RecommendRecallServiceTest.java` | 6 项：去重、排除自身、下架过滤、未登录、单路失败降级 |
| `src/test/java/com/bitforum/ai/recommend/RecommendFusionServiceTest.java` | 8 项：计分、多路命中、排除、截断、确定性 |

#### 关键设计

1. **三路各自复用什么**：vector 复用 M15 的向量库与 `bitforum-kb` 索引；
   hot 复用 M6 的热榜 ZSet；follow 复用 M9 的 `user_follow`。
2. **不直接调用 `RagService`**：它的 `topK`（5）与相似度下限（0.5）是为问答检索定的，
   问答只要几段最相关的片段，而推荐需要更大的候选池。因此复用**同一套向量库与嵌入模型**，
   但用推荐自己的参数，避免改动 M15 已定型的问答链路。
3. **用 RRF 而不是加权求和**，两条理由：
   三路分数根本不可比（向量 0.66~0.78 挤在一起、热度是 ZSet 累积分、关注路是时间序号）；
   而权重需要数据才能定 —— M16 的教训正是数据不足时拍出来的阈值过拟合。
   RRF 只用**排名**计分（`score = Σ 1/(k+rank)`，k=60），不需要调参，
   且**同样输入必然得到同样排序**，这是"与纯热榜基线对比"能够归因的前提。
4. **召回层不做产品过滤决策**：排除集由调用方通过 `RecallRequest.excludedArticleIds` 传入
   （Q3 定了"是否排除收藏/点赞"之后由上层决定）。
   唯一强制排除的是**来源文章自身** —— T11 实测"自己找自己"相似度恒为最高，
   不排除就会出现"相关推荐推的是当前这篇"。
5. **三路统一做"当前必须是 PUBLISHED"的二次校验**：热榜 ZSet 与向量索引都不是文章状态的
   权威来源。向量那一路最初漏了这道校验，写测试时发现并补上（与 `RagService` 的三重约束一致）。
   校验后**保留原始排名不压缩**，因为 RRF 按原始排名计分才符合其定义。
6. **一路失败不影响其它两路**：向量库挂掉时热度路仍正常返回（已有单测覆盖）。
7. **未登录不查关注表**：`userId` 为 null 时 follow 路直接返回空，
   未登录推荐自然退化为"内容相似 + 热度"两路。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 融合单测 | 8 项通过（含多路命中累加、并列分数按 id 稳定排序、同样输入同样输出） |
| 召回单测 | 6 项通过（含同文章多片段取最高分、来源文章排除、下架过滤、单路失败降级、未登录不查关注） |
| 后端全量回归 | **329 项：323 通过 + 6 条件跳过，0 失败**（上轮 315 + 本次新增 14） |
| 前端构建 | `npm run build` 成功（1891 模块，1.11 s） |

#### 未做（等用户确认）

- `RecommendAgent`：**谁决定最终列表**（Q1）与**理由由谁生成**
- 推荐接口（含未登录可见性与权限路径，Q5）与前端「相关推荐」
- 评测规范与命中率 / 基线对比实验（Q2、Q3 影响真值口径）

---

### M17-3：推荐编排与降级路径

> **范围说明**：只做不依赖 Q1 的部分 —— 召回 → 融合 → 补全展示信息 → 落库。
> **"推荐理由"仍未实现**（由 Q1 决定由谁写）。当前是**确定性推荐 + 无理由**的形态。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/bitforum/ai/recommend/RecommendService.java` | 推荐编排：个人排除集、召回、融合、展示信息补全、记录落库 |
| `src/test/java/com/bitforum/ai/recommend/RecommendServiceTest.java` | 5 项：空候选、正常路径与落库、文章失效重排、未登录不查个人数据、落库失败不影响返回 |

#### 关键设计

1. **"没有理由的推荐列表"是一条必须独立可用的路径，不是临时凑合**：
   handoff 硬约束第 9 条要求"AI 不可用不能影响论坛主流程"。
   因此 `RecommendService` 先实现这条确定性链路，理由生成接入后它仍然是无模型的降级路径。
   当前 `degraded` 恒为 true 并如实落库，评测时能区分"带理由"与"不带理由"的结果。
2. **排除集范围**：自己写的文章（`article.user_id`）+ 已收藏的文章（`article_favorite`）。
   **已点赞的排除不掉** —— 详见 `findings.md` 6.16：点赞只有"文章 → 用户集合"的 Redis Set，
   没有反向索引，反查需要 SCAN 全库。两个排除项都做成了配置开关
   （`exclude-own` / `exclude-favorited`，默认 true），产品策略若不同改配置即可。
3. **每次推荐都写 `ai_recommend_log`**：这是 M17 验收（命中率与纯热榜基线对比）的数据来源。
   落库失败**只记日志**，绝不影响推荐结果返回 —— 记录是评测的副产品，不是主流程。
4. **补全阶段做防御**：若候选文章在召回与补全之间被下架，跳过该条并**重新连续编号**，
   避免前端看到 1、3、4 这样的排名空档。
5. **未登录访客不查任何个人数据**：没有个人排除集，也不召回关注路。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 编排单测 | 5 项通过（含落库内容断言：rank 连续、degraded 如实记录、实验批次为 null） |
| 后端全量回归 | **334 项：328 通过 + 6 条件跳过，0 失败**（上轮 329 + 本次新增 5） |
| 前端构建 | `npm run build` 成功（1891 模块） |

#### 未做（等用户确认）

- 推荐理由生成与最终列表定序方式（Q1）
- 推荐接口与前端「相关推荐」卡片（Q5：未登录能否查看）
- 运营洞察的触发与落库接入（Q4）
- 评测规范与命中率 / 基线对比实验（Q2）

---

### M17-4：纯热榜基线与运营洞察存取

> **范围说明**：两块都是"评测与展示"必需的地基，且不依赖任何待决项 ——
> 基线是计划书明写的对比对象；报告的存取与"何时生成"无关。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `RecommendService.hotBaseline(...)` | 纯热榜基线：只走热榜一路，按热度分值排序，带实验批次落库 |
| `RecommendService.assemble(...)` | 推荐与基线**共用**的后半段（补全展示信息 → 组装 → 落库） |
| `RecommendRecallService.RecallRequest.sources` | 召回支持"只要指定通道"，基线借此复用同一套状态校验 |
| `src/main/java/com/bitforum/ai/analyst/AiInsightService.java` | 洞察报告存取：保存 / 最新成功 / 历史 / 是否有生成中 |
| `src/test/java/com/bitforum/ai/analyst/AiInsightServiceTest.java` | 7 项测试 |

#### 关键设计

1. **基线有三点刻意与推荐系统对齐**，否则对比会失真：
   ① 共用同一份排除集（否则基线能推荐用户已收藏的文章、凭空多出命中）；
   ② 共用同一套"文章当前必须是 PUBLISHED"的状态校验（通过复用召回层的 HOT 通道，
   而不是再写一遍查询）；③ 共用同一套落库逻辑，只有 `experiment_tag` 不同。
   抽出 `assemble()` 就是为了让两条路径**只差排序方式**。
2. **基线只走热榜一路**：新增 `RecallRequest.sources` 指定通道，
   让"纯热榜"这个语义在代码里显式成立（并被单测钉住：不得混入 vector / follow）。
3. **洞察报告的存取与"何时生成"解耦**（Q4）：
   同步、异步、定时三种方式只是调用方不同，`AiInsightService` 不用改。
4. **生成失败也落库，且保留统计快照**：统计是本地聚合的，与 AI 是否可用无关。
   管理员能看到"数据取到了、是模型这一步失败"，重试也有依据。
5. **看板只展示最新一份成功报告**：失败的记录留痕供排查，但不能被当成"当前洞察"显示出来
   （否则管理员会看到一份没有正文的记录）。`latestSuccess()` 显式加 `LIMIT 1`，
   避免同一秒内多条记录让 `selectOne` 抛异常。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 推荐链路单测 | 21 项通过（融合 8 + 召回 7 + 编排 6，含新增的基线与通道选择用例） |
| 洞察存取单测 | 7 项通过（成功/失败落库、快照留存、只取成功、条数上限、生成中状态） |
| 后端全量回归 | **343 项：337 通过 + 6 条件跳过，0 失败**（上轮 334 + 本次新增 9） |
| 前端构建 | `npm run build` 成功（1891 模块） |

#### 未做（等用户确认）

- 推荐理由生成与最终列表定序方式（Q1）
- 推荐接口与前端「相关推荐」卡片（Q5）
- 运营洞察的**触发**（Controller / 异步消息 / 定时任务）与前端卡片（Q4、Q5）
- 评测规范与命中率 / 基线对比实验（Q2、Q3）

---

### M17-5：产品策略定稿、评测规范冻结与评测工具链（2026-09-19）

> 从本轮起 M17 进入"按定稿策略实现"阶段 —— 五个待决问题已全部确定，
> 不再有"等确认"的阻塞项。定稿内容见 `task_plan.md` 的「M17 实施决策」。

#### 一、策略定稿（五条）

| 主题 | 结论 |
| --- | --- |
| 排序 | **完全由 Java 完成**（三路召回 → RRF 融合 → Top-N）；LLM 不参与选文，只写解释 |
| 评测 | **合成离线数据 + 留一法**；Ground Truth = 隐藏的收藏行为；指标 `HitRate@10` / `MRR@10` |
| 候选过滤 | 只排除**作者本人 + 已收藏**；浏览不排除；**已点赞本阶段明确不支持** |
| 运营洞察 | **管理员主动触发 + 异步生成 + 结果落库**；不让 HTTP 同步等待；暂不定时生成 |
| 匿名用户 | **可以看到相关推荐**（走"内容相似 + 热度"两路）；AI 理由失败时列表仍可用 |

**投入优先级**（作者判断）：T11 已证明短板是**数据**而不是模型，
因此时间优先投入「评测数据与 Ground Truth 设计」，而不是继续给推荐 Agent 增加"智能"。

#### 二、评测规范冻结：`m17-eval-protocol.md` v1

冻结时间 2026-09-19，与 M16 同一套方法论（先定球门再射门）。核心内容：

1. **留一法，以及它必须解决的一个自相矛盾**：
   产品策略是"排除已收藏"，而评测要拿"用户收藏过的文章"当命中目标 ——
   直接这么做，命中率**恒为 0**。解法：把目标那一篇**留在候选池里**
   （等价于"用户还没收藏它"），其余收藏进排除集。这条是规范里最容易被忽略、
   一旦写错就得出"系统完全无效"荒谬结论的地方。
2. **指标**：`HitRate@10`（能不能找到）+ `MRR@10`（排得够不够前），
   并要求附带**命中排名分布**与**召回通道覆盖**，避免只报总数掩盖结构。
3. **三条对比线**：融合推荐 / 纯热榜（主基线）/ 随机（sanity check）。
   基线通过共用排除集、共用状态校验、共用落库逻辑来保证"只差排序方式"。
4. **数据质量的硬条件 C1**：`区分度 = 同主题平均相似度 − 跨主题平均相似度` 必须 > 0。
   这是 T11 那个失败指标（0.0048）的正面验收 —— **不达标就重做数据，而不是调算法**。
5. **四条硬约束**：C1 区分度、C2 融合必须严格高于热榜基线、
   C3 测试集只跑一次、C4 报告必须带合成数据声明。

#### 三、评测工具链

| 文件 | 作用 |
| --- | --- |
| `M17EvalCorpus`（测试资源） | 50 篇**有实质正文**的合成技术文章，分 8 个主题 |
| `M17EvalDataSeeder` | 入库：板块 / 作者 / 评测用户 / 文章 / 收藏 / 关注 / 热度 + 重建向量索引；可重复执行 |
| `M17CorpusDiscriminationCheck` | 硬条件 C1：同主题 vs 跨主题的相似度区分度检验 |
| `M17RecommendEvaluation` | 留一法评测：三条线对比 + 排名分布 + 通道覆盖 + C2 判定 |
| `RecommendService.recommend(request, exclusionOverride)` | 让评测能覆盖默认排除集（否则目标文章会被排除、命中率恒 0） |
| `application.yml` 的 `bitforum.ai.recommend` | 把 `recall-top-k` / `top-n` / `rrf-k` / 排除开关集中成可配置项 |

#### 关键实现说明

1. **数据必须真的入库**：评测要在真实链路上跑 —— 向量通道走 Redis 向量索引、
   热度通道走热榜 ZSet、关注通道走 `user_follow` 表。用内存假数据评测，测的就不是这套系统。
2. **用户兴趣必须有结构**：留一法测的是"能不能推出用户会感兴趣的同主题文章"，
   如果用户兴趣在 8 个主题上均匀随机分布，任何算法都不可能命中，评测就没有意义。
   因此每个评测用户有 1~2 个明确的兴趣主题，收藏集中在该主题内。
3. **热度与主题无关，且如实声明**：热度用随机值模拟"浏览行为不针对主题"。
   这意味着纯热榜基线在"主题兴趣"上没有信息量 —— 报告里必须写明这一点，
   不能包装成"推荐算法大幅超越热榜"。
4. **评测不对效果做断言**：命中率高低是**实验结果**，不是代码正确性。
   若融合没赢过热榜，正确反应是按 C2 如实记录，而不是把构建改红或换口径。
   评测里唯一的断言是**实现正确性**：推荐列表不得出现被排除的文章。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 主代码编译 | 通过（含 `RecommendService` 的排除集覆盖改造） |
| 语料生成 | 见下节「M17-6」 |
| 区分度检验（C1） | 见下节 |
| 留一法评测（C2） | 见下节 |

---

### M17-6：M17 评测执行（合成数据 + 留一法，2026-09-19）

> 完整报告见 `m17-eval-report.md`。本节只记执行过程与结论摘要。

#### 一、数据生成（`M17EvalDataSeeder`，可重复执行）

| 项 | 实测 |
| --- | --- |
| 语料 | 50 篇，8 个主题（spring 7 / persistence 7 / redis 6 / mq 6 / frontend 6 / deploy 6 / concurrency 6 / api 6） |
| 评测用户 | 30 位（+ 8 位主题作者） |
| 收藏 / 关注 | 173 条 / 48 条 |
| 向量索引 | 已发布 55 篇 → 索引 50、跳过 5（既有测试文章指纹未变），耗时 6.8 s |

#### 二、数据质量检验（硬条件 C1）

| 指标 | 实测 | 判定 |
| --- | --- | --- |
| 同主题平均相似度 | 0.8850（576 对） | |
| 跨主题平均相似度 | 0.8703（4716 对） | |
| 区分度 | **0.0147** | > 0 ✅；目标 0.05 ❌（如实记录） |
| Top-5 同主题占比 | **45.6%** | 随机 12.5% ✅ |

结论：平均相似度差是弱信号（bge 对中文技术段落整体偏高），但**排序区分度强** ——
这反过来印证了融合选 RRF（按排名计分）是对的。

#### 三、评测结果

| 数据集 | 对比线 | HitRate@10 | MRR@10 |
| --- | --- | --- | --- |
| 开发集（21 位用户） | 融合推荐 | **0.8095** | 0.5330 |
| | 纯热榜基线 | 0.2381 | 0.1302 |
| | 随机（sanity check） | 0.2857 | 0.0938 |
| **独立测试集（9 位，只跑一次）** | 融合推荐 | **0.8889** | 0.3745 |
| | 纯热榜基线 | 0.2222 | 0.0926 |
| | 随机（sanity check） | 0.3333 | 0.2444 |

通道贡献（Top-10 累计篇次）：开发集 `vector=193 / hot=169 / follow=82`；测试集 `vector=81 / hot=70 / follow=42`。

#### 四、硬条件核对

| 编号 | 约束 | 结果 |
| --- | --- | --- |
| C1 | 区分度 > 0 | ✅ 0.0147 |
| C2 | 融合严格高于纯热榜 | ✅ 两集均达标 |
| C3 | 测试集只跑一次 | ✅ 运行前记录实现 sha256（`.dev-logs/m17-eval-freeze.txt`） |
| C4 | 报告含合成数据声明 | ✅ |

#### 五、过程中发现并修正的三个问题

1. **向量通道零贡献（已修正，改变了结果）**：第一次开发集评测的通道贡献是
   `{hot=160, follow=96}` —— vector 一篇都没有，而 HitRate 0.7619 看起来很正常。
   原因是用户维度的推荐没有"来源文章"，向量通道直接返回空。
   修正为多查询（来源文章 + 用户最近收藏的若干篇，即内容画像）后，
   开发集 HitRate **0.7619 → 0.8095**，vector 成为贡献最大的通道。
   → 教训：永远返回空的通道不报错、也不让指标变难看，它只是静默地不参与；
   评测报告必须打印通道贡献。
2. **留一法的泄漏风险（已修正）**：目标文章绝不能被当作"已知兴趣"参与向量查询，
   否则等于提前告诉算法答案。评测传入的兴趣画像 = 除目标之外的收藏。
3. **语料文件版本漂移（已对齐）**：并行的子任务在测试集运行后又改写了一次语料
   （512~1200 字/篇），与已评测数据（329~763 字/篇）不一致，会导致仓库语料无法复现数据。
   评测完成后从数据库反向重建语料并逐字校验（50/50 一致）；
   **实现代码 sha256 未变**，不影响结果有效性。
   → 教训：数据与实现一样需要冻结。

#### 六、验证记录

| 验证项 | 结果 |
| --- | --- |
| 后端全量回归 | **343 项：337 通过 + 6 条件跳过，0 失败**（评测类默认禁用，不计入） |
| 前端构建 | `npm run build` 成功 |
| 评测数据留档 | 写入 `ai_recommend_log`（`experiment_tag` = `RECOMMEND` / `BASELINE_HOT`），可按批次复核 |

#### 未做

- 推荐理由生成（LLM 只写解释）与推荐接口 / 前端「相关推荐」
- 运营洞察的触发（异步）与前端卡片

---

### M17-7：推荐理由生成（LLM 只写解释，2026-09-19）

> 落地 M17 实施决策的第一条：**排序完全由 Java 完成，LLM 只负责生成基于真实信号的解释**。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/bitforum/ai/recommend/RecommendReasonAgent.java` | 理由生成：按序号填理由、越界丢弃、失败降级 |
| `src/test/java/com/bitforum/ai/recommend/RecommendReasonSmokeTest.java` | 真实调用复验（默认跳过，消耗额度） |
| `RecommendService` 集成 | 理由回填进推荐列表；`reason-enabled` 开关 |
| `application.yml` | `reason-enabled` / `reason-max-articles` |

#### 关键设计

1. **模型接触不到文章 id**（T9 的方案 B）：模型只输出 `{index, reason}`，
   序号 → 文章 id 的映射由 Java 完成。**"推荐错文章"在结构上不可能发生** ——
   即使模型返回越界或重复序号，也只是丢几条理由，不会推错内容（越界会被丢弃并记警告）。
2. **降级路径是生产必需能力**：模型不可用/超时/解析失败时返回"无理由"的推荐列表，
   而不是让推荐失败。这与 handoff 的硬约束"AI 不可用不能影响论坛主流程"一致，
   也是"确定性排序"这一设计的直接收益 —— 排序不依赖模型，理由才依赖。
3. **只给模型用户情况摘要**（"已登录/未登录 + 有无个性化信号"），不给具体收藏清单：
   既不泄漏用户行为，也不把上下文浪费在罗列标题上。
4. **离线评测必须关闭理由生成**（`BITFORUM_RECOMMEND_REASON_ENABLED=false`）：
   评测关心排序结果，理由会白烧额度并拖慢流程。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 真实调用（2 项） | 5/5 条理由生成成功，序号与召回来源严格对应，单次约 1.0 s |
| 单测 | `RecommendServiceTest` 8 项通过（新增：理由回填与落库、开关关闭时不调用模型） |
| 后端全量回归 | **347 项：339 通过 + 8 条件跳过，0 失败**（上轮 343 + 新增 4） |
| 前端构建 | `npm run build` 成功 |

真实调用样例（模型输出的理由与真实召回来源一致）：

| 序号 | 文章 | 召回来源 | 模型给出的理由 |
| --- | --- | --- | --- |
| 1 | 《Redis 热点数据同步方案》 | 内容相似 + 热度 | 与你正在看的文章主题相近，且近期社区热度较高 |
| 2 | 《Spring Boot 论坛项目实践》 | 内容相似 | 内容与你正在看的文章相近，可延续当前阅读方向 |
| 3 | 《Spring 和 Spring Boot…》 | 热度 | 近期社区热度较高，适合补充框架基础知识 |
| 4 | 《React 与 Vite 前端开发笔记》 | 关注 | 来自你关注的作者，前端方向更新值得留意 |

#### 对既有评测的影响

理由生成是在 M17-6 评测**之后**接入的，且**不参与召回与排序**（只影响展示文案），
因此 M17-6 的 `HitRate@10` / `MRR@10` 结论仍然成立。
冻结凭证中实现代码的 sha256 变化仅来自这次接入（`RecommendService.java`），
排序逻辑本身未改动 —— 已在 `m17-eval-report.md` 中说明。

#### 未做

- 推荐接口与前端「相关推荐」卡片（匿名可见）
- 运营洞察的异步触发与看板卡片

---

### M17-8：推荐接口与前端「相关推荐」（2026-09-19）

> 落地产品决策第 5 条：**匿名用户也能看到相关推荐**（走"内容相似 + 热度"两路），
> 登录用户在此基础上增加关注与个人偏好信号。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `ArticleController#recommendations` | `GET /api/article/recommendations?articleId=&limit=`，**公开接口**（不在登录拦截列表内） |
| `src/test/java/com/bitforum/controller/ArticleRecommendationTest.java` | 5 项：匿名可访问、登录态识别、无效 Token 降级、limit 上限、失败返回空列表 |
| `frontend/src/api/articleApi.js` | `getRecommendations(articleId, limit)` |
| `frontend/src/components/RelatedRecommendations.jsx` | 相关推荐区块（含理由展示） |
| `frontend/src/components/RelatedRecommendations.test.jsx` | 5 项 |
| `frontend/src/pages/ArticleDetail.jsx` | 在评论之前插入「相关推荐」区块 |
| `frontend/src/style.css` | 相关推荐样式（沿用既有配色变量） |

#### 关键设计

1. **匿名可访问是产品决策的落地**：接口刻意**不加入** `LoginInterceptor` 的路径列表；
   未登录时 `userId` 为 null，召回层据此自动跳过"关注"这一路 ——
   不需要额外的分支判断，产品规则与实现是同一条。
2. **无效 Token 按匿名处理**：复用详情接口既有的 `parseOptionalUserId`（解析失败返回 null），
   不会因为一个过期 Token 就让推荐接口报 401。
3. **失败返回空列表而不是 5xx**：推荐是增强能力，底层异常时前端显示"暂无推荐"即可，
   绝不能把文章页拖下水。
4. **前端"没有理由"也要能看**：AI 降级时后端只返回列表，
   前端如实显示"暂无推荐理由"而不是把区块藏掉 —— 推荐文章本身仍有价值。
5. **加载失败/无候选时整块隐藏**：不给阅读带来干扰，与后端的降级约定对应。

#### 真实链路端到端验证（`./dev.sh` 启动后实测）

| 场景 | 结果 |
| --- | --- |
| 匿名（无 Token） | `code=200`，返回 3 条；召回来源为 `hot`，理由如"并发锁题近期社区讨论热度高" |
| 登录（评测用户 `m17eval_user_01`） | 返回 3 条；召回来源为 **`hot,follow`**，理由如"来自你关注的作者，近期社区热议的锁与死锁排查实战" |

匿名与登录的差异（`hot` → `hot,follow`）正是产品决策第 5 条要的效果。
**环境已保持运行**，可直接打开 http://localhost:5173 查看；不用时执行 `./stop-local.sh`。

#### 评测数据的展示说明（需要留意）

评测语料的标题带 `[M17Eval]` 前缀、板块名为 `M17Eval-<主题>`，
因此它们出现在页面（文章列表、相关推荐）时会带有这个前缀。
这是**刻意的**：既是"合成数据"的显式标记，也便于精确清理。两种选择：

- **保留**（推荐用于答辩）：候选池有 55 篇，"相关推荐"效果与评测报告一致；
- **清理**：`M17_EVAL_CLEANUP_ONLY=true ./mvnw -s maven-settings.xml -Dtest=M17EvalDataSeeder test`
  —— 移除全部评测数据、回到只有原有文章的干净状态（候选池变小，推荐效果会明显变弱）。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 接口测试 | 5 项通过 |
| 前端测试 | **28 项通过**（原 23 + 新增 5） |
| 后端全量回归 | **352 项：344 通过 + 8 条件跳过，0 失败**（上轮 347 + 新增 5） |
| 前端构建 | `npm run build` 成功 |
| 端到端 | 匿名与登录两种场景均通过（见上表） |

#### 未做

- 运营洞察的异步触发与看板 「AI 运营洞察」卡片
- AI 助手回答末尾推荐相关帖

---

### M17-9：运营洞察的异步触发与接口（2026-09-19）

> 落地产品决策第 4 条：**管理员主动触发 + 异步生成 + 结果落库**，不让 HTTP 请求同步等待模型。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/bitforum/ai/config/AiInsightConfig.java` | 单线程执行器 `insightExecutor`（daemon + shutdown 语义） |
| `src/main/java/com/bitforum/ai/analyst/AiInsightGenerationService.java` | 触发受理 + 异步编排 + 异常兜底 |
| `AiInsightService`（扩展） | `createPending` / `complete` / `markFailed` / `pending` |
| `src/main/java/com/bitforum/controller/AdminAiInsightController.java` | `POST /generate`、`GET /latest`、`GET /status`、`GET /history` |
| `AiInsightGenerationServiceTest` | 3 项（先落 PENDING、连点保护、异常兜底） |
| `AdminAiInsightControllerTest` | 6 项（受理、拒绝重复、只展示成功、无报告时空 data、状态查询、历史） |
| `AiInsightSmokeTest` | 真实调用端到端（默认跳过） |

#### 关键设计

1. **先落 PENDING 再提交任务，顺序不能反**：否则从管理员点下按钮到任务真正开始之间，
   前端既查不到"正在生成"也看不到失败 —— 只能干等或反复点击。
   这条记录同时是**防重复触发**的依据，也是重启后判断"哪次生成没跑完"的线索。
2. **异常绝不让记录停在 PENDING**：`AnalystAgent` 本身不抛异常（失败返回降级结果），
   `generate()` 仍加了兜底 —— 记录卡在 PENDING 会让前端永远转圈、且再也无法触发新生成。
3. **为什么用单线程池而不是消息队列**：MQ 适合"事件驱动、需要持久化与重试"的异步
   （M15 的知识库索引就是这类）。洞察生成是"人点一下、等几秒"的操作，
   用队列还要额外定义消息体、消费者与幂等；而 PENDING 记录本身就是任务状态的持久化载体。
   单线程天然保证同一时刻只有一次生成，避免连点把额度并发烧掉。
4. **执行器是注入的**（`Executor`）：生产用单线程池，测试注入同步执行器（`Runnable::run`），
   于是单元测试无需等待、也没有不确定性，却仍覆盖完整时序。
5. **看板只取最新一份成功报告**：失败记录留痕供排查，但不会被当成"当前洞察"展示。

#### 真实调用端到端验证（`AiInsightSmokeTest`）

| 项 | 实测 |
| --- | --- |
| 时序 | 触发 → 立刻返回 PENDING（id=9）→ 异步执行 → 写回 SUCCESS |
| 真实模型 | deepseek-flash，**5004 ms**，**2695 tokens** |
| 落库内容 | 正文非空 + 统计快照非空（含 `userStats`）+ token 与耗时齐全 |
| 报告质量 | 正文准确引用了真实统计（43 用户 / 60 文章 / 55 已发布 / 180 收藏），并给出了分点建议 |

**一个有意思的旁证**：模型在报告里主动指出
「排行前 10 的文章热度分在 25~30，但浏览量（viewCount）与点赞数（likeCount）均为 0，
热度分与真实互动脱节，建议核实」—— 这确实是我们生成评测数据时只写热榜分值、
没写浏览/点赞造成的。**说明它读的是真实数据，而不是在编。**

#### 接口权限验证

| 请求 | 结果 |
| --- | --- |
| `GET /api/admin/ai/insight/latest`（无 Token） | **HTTP 401**（`/api/admin/**` 的 AdminInterceptor 生效） |
| `GET /api/article/recommendations`（匿名） | 200 + 推荐与理由（公开接口仍正常） |

环境已按新代码重启（`./stop-local.sh` → `./dev.sh`），运行中。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 洞察相关单测 | 9 项通过（生成编排 3 + 接口 6） |
| 后端全量回归 | **362 项：353 通过 + 9 条件跳过，0 失败**（上轮 352 + 新增 10） |
| 前端 | 本轮未改动前端 |

#### 未做

- 管理员看板「AI 运营洞察」卡片（前端）
- AI 助手回答末尾推荐相关帖

---

### M17-10：管理员看板「AI 运营洞察」卡片（2026-09-19）

#### 产出

| 文件 | 说明 |
| --- | --- |
| `frontend/src/components/AiInsightCard.jsx` | 洞察卡片：加载 / 触发 / 轮询 / 渲染 Markdown / 元信息 |
| `frontend/src/components/AiInsightCard.test.jsx` | 5 项 |
| `frontend/src/api/adminApi.js` | 4 个洞察接口函数 |
| `frontend/src/pages/admin/Dashboard.jsx` | 在热门文章之前插入洞察卡片 |
| `frontend/src/style.css` | 卡片样式 |

#### 关键设计

1. **点完立刻进"生成中"**：后端 `generate` 只受理不等模型，若前端点完不切状态，
   管理员会以为没反应而反复点击（后端虽有连点保护，但体验上仍然像坏了）。
2. **刷新页面能接上未完成的任务**：挂载时先查一次 `status`，若仍在 PENDING 就直接进入轮询，
   而不是显示"暂无报告"误导管理员。
3. **只展示成功报告**：`latest` 接口本身就只返回成功的记录，
   因此失败不会被显示成"当前洞察"，历史里仍可追溯。
4. **渲染 Markdown**：模型输出的是带标题与列表的 Markdown，
   复用 M15 的 `MarkdownText`（内部已做 HTML 转义与链接协议限制）。
5. **展示生成元信息**（时间、模型、耗时）：让管理员知道这份结论是什么时候、由什么模型产生的。

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 前端测试 | **33 项通过**（原 28 + 新增 5） |
| 前端构建 | `npm run build` 成功 |
| 接口权限 | `GET /api/admin/ai/insight/latest` 未登录返回 401（实测） |

> 前端改动由 Vite dev 热更新，**不需要重启环境**：刷新 http://localhost:5173 即可看到新卡片。

#### 未做

- AI 助手回答末尾推荐相关帖（M17 最后一块）

---

### M17-11：AI 助手回答末尾的「相关帖子」（2026-09-19）

> M17 的最后一块。与 M15 的「参考来源」互补：
> 参考来源是**回答用到了**哪些文章，相关帖子是"你可能还想看"的。

#### 产出

| 文件 | 说明 |
| --- | --- |
| `RecommendRecallService` | `RecallRequest.queryText`：向量通道支持用**一段自由文本**检索（AI 助手没有来源文章） |
| `RecommendService.forQuery(...)` | 按提问文本驱动推荐；**候选只保留向量命中**（见下） |
| `AiController#recommendations` | `GET /api/ai/recommendations?query=&limit=`（要求登录，与 AI 助手一致） |
| `WebMvcConfig` | 把该路径加入登录拦截（**漏配会让 `@RequestAttribute` 直接 500**） |
| `AiRecommendationTest` | 4 项（必须登录、提问传入召回层、limit 上限、失败返回空列表） |
| `frontend/src/components/AiAssistantPanel.jsx` | 回答下方展示「相关帖子」（含理由） |
| `frontend/src/api/aiApi.js` | `recommendForQuery(query, limit)` |

#### 端到端验证中发现并修掉的三个问题

1. **`/api/ai/recommendations` 没被登录拦截** → `@RequestAttribute("userId")` 取不到值，
   接口直接 500。补进 `WebMvcConfig` 的拦截列表后正常（未登录 → 401）。
   → 教训：新端点要么显式加入拦截列表，要么别用 `@RequestAttribute`。
2. **收藏画像淹没了提问**：向量通道当时把"提问 + 用户最近收藏"混在一起做多查询，
   结果问「Redis 缓存穿透」却推出一屏 MySQL —— 用户收藏的主题压过了他刚说出的意图。
   修正：**有明确提问时不再混入收藏画像**。
3. **理由措辞错配**：AI 助手场景没有"正在看的文章"，模型却写"与你正在看的文章相近"。
   修正：把场景传给理由生成，提问场景改为"内容与你的问题相关"。
   另外把候选限制为**只保留向量命中**的热度/关注加成 —— 否则"多路命中优先"会把
   与提问无关的热门文章顶到前面（对"根据提问推荐"就是答非所问）。

#### 已知局限（如实记录，未通过调参掩盖）

问「Redis 缓存穿透怎么处理」时，结果里仍有 MySQL 主题排在 Redis 之前。
**这不是代码缺陷，而是向量检索本身区分度不足** —— T11 与评测报告已实测：
同主题与跨主题的平均相似度只差 **0.0147**（Top-5 同主题占比 45.6%）。
按 M17 评测规范的纪律，**不允许对着结果拍权重或调参来掩盖**；
要真正改善需要更高质量的文章语料（正文更长、主题更聚焦），属于数据侧工作。

#### 端到端验证

| 场景 | 结果 |
| --- | --- |
| 未登录 | HTTP 401 |
| 登录 + 提问 | 200，返回相关帖子与理由；候选全部来自向量命中（相关性前提生效） |
| 措辞 | "与你的问题相关 / 来自你关注的作者 / 近期热度较高"，不再出现"正在看的文章" |

#### 验证记录

| 验证项 | 结果 |
| --- | --- |
| 后端测试 | `RecommendServiceTest` 9 项、`AiRecommendationTest` 4 项通过 |
| 后端全量回归 | 见下方汇总 |
| 前端 | 33 项通过；`npm run build` 成功 |

#### ⚠️ 运维提醒：跑完全量测试必须重建向量索引

全量 `mvn test` 会清空 Redis 向量索引（M15 起的既有行为）。此时**所有依赖向量召回的路径
都会静默退化为两路**——本次实测就出现过"AI 推荐结果与提问主题完全无关"，
排查后才发现是索引为空（`num_docs=0`）。演示前或跑完测试后请执行一次重建：

```bash
export M17_VECTOR_PROBE=true
./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test
```

---

### M17-12：M17 收尾核对（对照 handoff 与 task_plan，2026-09-19）

#### 一、交付物核对（handoff 第二节「M17 范围」）

| handoff 要求 | 状态 | 证据 |
| --- | --- | --- |
| **Flyway V16** `ai_insight_report` | ✅ | `V16__add_ai_insight_report.sql` + 实体/Mapper + 持久化测试 |
| **Flyway V17** `ai_recommend_log` | ✅ | `V17__add_ai_recommend_log.sql` + 实体/Mapper + 持久化测试 |
| `AnalystAgent`：看板统计包装为 `@Tool`，LLM 生成洞察 | ✅ | `AnalystTools` + `AnalystAgent`；真实端到端 5.0 s / 2695 tokens |
| `RecommendAgent`：三路召回融合 + LLM 生成理由 | ✅ | `RecommendRecallService` + `RecommendFusionService` + `RecommendService` + `RecommendReasonAgent` |
| 前端：文章详情页「相关推荐」 | ✅ | `RelatedRecommendations.jsx` + `GET /api/article/recommendations`（匿名可见） |
| 前端：AI 助手回答末尾推荐相关帖 | ✅ | `AiAssistantPanel` + `GET /api/ai/recommendations`（提问驱动） |
| 前端：管理员看板「AI 运营洞察」卡片 | ✅ | `AiInsightCard.jsx` + 4 个管理员接口 |
| **验收**：给出 Top-10 推荐及理由 | ✅ | 端到端实测返回 10 条且 10/10 带理由 |
| **验收**：命中率与纯热榜基线对比 | ✅ | `m17-eval-report.md`：融合 0.8095 / 0.8889 vs 热榜 0.2381 / 0.2222 |

#### 二、handoff 第四节「必须遵守的约束」核对（9 条）

| # | 约束 | 核对 |
| --- | --- | --- |
| 1 | 用 `@ConditionalOnProperty` 而非 `@ConditionalOnBean` | ✅ 本轮未新增条件 Bean；既有约定未破坏 |
| 2 | `@MapperScan` 不扫子包 | ✅ 新 Mapper 放在已在扫描列表的 `com.bitforum.ai.mapper` |
| 3 | 测试配置用占位符形式 | ✅ 未改动测试配置 |
| 4 | 工具/提示词改动需干净会话验证 | ✅ 用 `RecommendReasonSmokeTest` / `AiInsightSmokeTest` 对真实实现复验 |
| 5 | Redis 必须是 Redis Stack | ✅ 沿用既有环境 |
| 6 | 异步消息在事务提交后发送 | ➖ 本轮未使用 MQ（洞察用单线程池），约束不适用 |
| 7 | 结构化输出用固定字段 | ✅ 推荐理由用 `{index, reason}` 固定结构，模型接触不到文章 id |
| 8 | 只新增 Flyway 迁移 | ✅ 只加 V16/V17，未改 V1-V15 |
| 9 | AI 能力必须有降级路径 | ✅ 推荐：无理由仍返回列表；洞察：失败落库为 FAILED 且快照保留 |

#### 三、handoff 第七节「协作方式」核对

| 要求 | 核对 |
| --- | --- |
| 重大选择先说明方案与取舍、等确认 | ✅ 五个问题经确认后才实现；过程材料在 `m17-decision-brief.md` |
| 每个模块跑 `mvn test` + `npm run build`，红了不开下一个 | ✅ 每轮均执行，全程未带着失败继续 |
| 提交前检查是否含真实 Key 片段 | ✅ 见下节安全自检 |
| 只新增迁移、不回滚历史功能 | ✅ |
| 不提交 `.env`、Key 只走环境变量 | ✅ `.env` 被 gitignore（实测确认） |
| 区分「已实现」与「规划中」、不虚构评估数据 | ✅ 评测报告含合成数据声明与局限；未达标项（区分度 0.0147 < 目标 0.05）如实记录 |

#### 四、安全自检（提交前）

| 检查项 | 结果 |
| --- | --- |
| 真实 API Key 片段（前 10 位）出现在文档/源码 | **0 处** |
| `sk-` 形式的密钥串 | **0 处** |
| 数据库口令出现在 M17 相关文档 | 0 处（全局 32 处命中经核实为常见词误报，集中在未改动的 M1/M6/M12 旧文档） |
| `.env` 被 gitignore | ✅ `.gitignore:47` |

#### 五、本轮（M17）新增/修改清单

改动 **37 项**：已跟踪文件修改 14 个、新增文件 23 个。

**后端主代码**
- 迁移：`V16__add_ai_insight_report.sql`、`V17__add_ai_recommend_log.sql`
- 实体/Mapper：`AiInsightReport`、`AiRecommendLog`、对应两个 Mapper
- 运营分析：`AnalystTools`、`AnalystAgent`、`AiInsightService`、`AiInsightGenerationService`、`AiInsightConfig`
- 推荐：`RecommendRecallService`、`RecommendFusionService`、`RecommendService`、`RecommendReasonAgent`
- 接口：`AdminAiInsightController`；`ArticleController`、`AiController` 各加一个推荐端点；`WebMvcConfig` 补登录拦截
- 配置：`application.yml` 的 `bitforum.ai.recommend` / `bitforum.ai.analyst`

**后端测试**（新增 55 项）
- 单元/契约：推荐链路 24、洞察 16、接口 15
- 真实调用（默认跳过）：`RecommendReasonSmokeTest`、`AiInsightSmokeTest`
- 评测工具链：`M17EvalDataSeeder`、`M17CorpusDiscriminationCheck`、`M17RecommendEvaluation`

**前端**
- 新增：`RelatedRecommendations.jsx(+test)`、`AiInsightCard.jsx(+test)`
- 修改：`articleApi.js`、`aiApi.js`、`adminApi.js`、`ArticleDetail.jsx`、`Dashboard.jsx`、`AiAssistantPanel.jsx`、`style.css`

**文档**
- 新增：`m17-decision-brief.md`、`m17-eval-protocol.md`、`m17-eval-report.md`
- 修改：`task_plan.md`（M17 实施决策 + Phase 5）、`findings.md`（6.13-6.17）、`progress.md`（M17-0 ~ M17-12）

#### 六、遗留与建议

| 项 | 说明 |
| --- | --- |
| 向量区分度偏弱 | 实测 0.0147 < 目标 0.05（`findings.md` 6.15）。要改善需提高语料质量（正文更长、主题更聚焦），属数据侧工作 |
| 「已点赞」无法排除 | Redis 只有"文章 → 用户集合"（`findings.md` 6.16）；补反向索引需改 M6 数据模型，建议单独立项 |
| 评测数据带 `[M17Eval]` 前缀 | 刻意保留（合成数据标记 + 便于清理）；可用 `M17_EVAL_CLEANUP_ONLY=true` 一键移除 |
| 跑完全量测试需重建向量索引 | M15 起的既有行为；`progress.md` M17-11 已给出命令 |
| 洞察报告未做前端历史列表 | 接口已提供 `/history`，看板目前只展示最新一份（够用，非验收项） |

---

### M17-13：M17 正式完结（外部 AI 评审决定，2026-09-19）

评审材料：`m17-decision-brief.md` + `m17-eval-protocol.md` + `m17-eval-report.md`
（用 `scripts/export-ai-context.sh` 打包，见 `.dev-logs/m17-review-dialog.md`）。

#### 五条最终决定

| # | 决定 | 对本项目的影响 |
| --- | --- | --- |
| 1 | M17 按冻结规范**已达标**，但只证明**合成数据上的相对效果** | 答辩必须主动强调：测试集仅 9 人、合成数据、弱热榜、强关注信号 |
| 2 | 向量区分度**选"乙"：接受 0.0147**，作为已知局限结束 M17 | **不改语料、不换模型、不改算法** |
| 3 | 点赞反向索引**不补** | 写入后续优化，**不在 M18 前动 M6** |
| 4 | 评测数据**答辩保留**，但展示成明确的**"演示数据"** | 保留 Seeder + 一键清理；不伪装成正常帖子 |
| 5 | **M18 范围收敛**：Trace + Usage + Degrade + 简单可视化 | Prompt 动态管理 / LLM-as-Judge / AI 设置页**砍掉**；TokenBudgetGuard 时间足再做 |

> **执行纪律（写入 task_plan）**：M17 的推荐数字已完成实验使命 ——
> **下一步最不该做的就是继续优化 0.8889**。

#### M17 提交记录（6 个 commit，本地未推送）

```
862f5b0 docs(graduation): record M17 decisions, technical evidence and progress (M17)
3ec6c8e test(ai): add M17 evaluation corpus, frozen protocol and report (M17)
29bf6c5 feat: expose recommendation APIs and render them on three surfaces (M17)
2d46086 feat(ai): add analyst agent with async insight generation and admin API (M17)
355afc5 feat(ai): implement three-channel recall, RRF fusion and recommendation reasons (M17)
6a53993 feat(ai): add M17 insight and recommendation tables (M17)
```

共 56 个文件。提交后工作区干净，分支 `feat/ai-agent` 领先 `origin` **7 个提交**。

#### M17 最终结算

| 项 | 结果 |
| --- | --- |
| 交付物 | Flyway V16/V17、AnalystAgent、推荐链路（三路召回 + RRF + 编排 + 理由）、前端三处 —— **全部达成** |
| 测试 | 后端 **367 项：358 通过 + 9 条件跳过，0 失败**；前端 **33 项通过** |
| 评测 | 融合 `HitRate@10` = 0.8095（开发集）/ **0.8889**（独立测试集）；纯热榜 0.2381 / 0.2222 |
| 真实调用 | 推荐理由 5/5 条；运营洞察 5.0 s / 2695 tokens（且报告主动指出了数据异常） |
| 文档 | `m17-decision-brief.md`、`m17-eval-protocol.md`、`m17-eval-report.md` + `findings.md` 6.13-6.17 + 本文 M17-0 ~ M17-13 |

#### 交接

`m18-handoff.md` 已写好（新会话入口）：含实测状态、M18 收敛范围、复用清单、
10 条硬约束、M17 已定稿决定、答辩要强调的三件事、环境与常用命令、演示账号。

---

## 2026-09-19（第二轮：M18）

### M18-0：开工准备、风险验证与决策（2026-09-19）

- **Status:** complete

#### 一、开工前的状态核对

| 项 | 实测 |
| --- | --- |
| 环境 | `./stop-local.sh --status`：8080/5173 运行中，三容器 healthy |
| 分支 | `feat/ai-agent`，领先 origin 7 个提交（未 push） |
| Flyway | 已到 V17，下一个新迁移是 V18 |
| 起始测试 | 后端 367 项（358 通过 + 9 跳过）、前端 33 项 |

#### 二、最大风险点的最小验证（沿用 M13-M17 惯例）

按 handoff 的判断，M18 有两个没实测过的技术点，先各自验掉再写业务代码：

| 探针 | 问题 | 结论 |
| --- | --- | --- |
| `ToolCallTraceProbe` + `ToolCallTraceSmokeProbe`（T12） | 工具调用链与单步耗时能否采集 | 工具循环在 provider 内部，**最终响应里没有工具链**；包装 `ToolCallback` 可完整采集（真实 DeepSeek 复验：两次工具调用，耗时 3ms/1ms，token 946+110） |
| `M18AsyncTraceProbe`（T13） | 轨迹能否覆盖异步链路 | MQ **消息头**透传 traceId 可用且不影响消息体；线程池不包装必然丢失、包装后可见并能清理 |

证据与实现约束记入 `findings.md` 6.18 / 6.19，待验证事项表新增 T12 / T13。

#### 三、需要用户拍板的五个问题（已确认，全部取推荐项）

材料见 `m18-decision-brief.md`（`scripts/export-ai-context.sh` 打包为 `.dev-logs/m18-review-context.md`）：

| # | 问题 | 决定 |
| --- | --- | --- |
| Q1 | `ai_usage_stat` 的数据来源 | **新建统一埋点明细**：四个 Agent 走同一处，顺带补采审核/推荐理由的 token（现有表里这两个 Agent 根本没有 token 字段） |
| Q2 | 轨迹表结构 | **单表 + `steps` JSON**：一次调用一行、异步回来 UPDATE 同一行，简单优先 |
| Q3 | `AiDegradeGuard` 范围 | **回头改造四个既有 Agent**：验收第 3 条要的是全站统一降级表现 |
| Q4 | 迁移编号 | **V18 = 轨迹、V19 = 用量**（轨迹是核心，先占 V18；修正 handoff/task_plan 原表的编号） |
| Q5 | 轨迹可见性 | **只在管理端**（`/api/admin/ai/traces`），普通用户看不到 token/费用 |

---

### M18-1：V18 `ai_execution_trace` + `TraceRecorder` + 四条链路埋点（2026-09-19）

- **Status:** complete

#### 一、新增文件

| 文件 | 作用 |
| --- | --- |
| `V18__add_ai_execution_trace.sql` | 轨迹表：一行 = 一次 AI 调用；`steps` 存步骤 JSON；`degrade_reason`/`message` 承载降级原因 |
| `ai/entity/AiExecutionTrace.java`、`ai/mapper/AiExecutionTraceMapper.java` | 实体与 Mapper（Mapper 落在既有的 `com.bitforum.ai.mapper`，无需改 `@MapperScan`） |
| `ai/trace/TraceRecorder.java` | 记录器：start / attach / step / degrade / finish，**写库失败只记日志** |
| `ai/trace/TraceContext.java` | ThreadLocal 上下文（T13 结论：只负责同线程，跨线程必须显式交接） |
| `ai/trace/TraceSession.java`、`TraceStep.java`、`TraceStepType.java`、`TraceDegradeReason.java` | 会话、步骤、步骤类型、降级原因码 |
| `ai/trace/TracingToolCallback.java` | 工具装饰器（T12 结论：唯一能拿到工具名/入参/返回值/单步耗时的位置） |
| `ai/trace/TraceHeaders.java` | MQ 消息头约定 `x-trace-id` + 无条件可挂的 `propagate()` |
| `ai/trace/AiTraceQueryService.java`、`ai/dto/AiTraceResponse.java`、`controller/AdminAiTraceController.java` | 管理端查询（列表不解析 steps、详情解析成数组） |

#### 二、埋点接入的四个链路

| 链路 | 接入点 | 记录的步骤 |
| --- | --- | --- |
| 对话（CHAT） | `AgentOrchestrator.chat` 起点 + `QaAgent` 内部 | ROUTE → RETRIEVE → LLM_CALL → TOOL_CALL（每次工具一步）→ PERSIST |
| 审核（MODERATION） | `ModerationMessageListener`（MQ 消费者） | ASYNC（消费消息）→ PERSIST（审核结果）；降级时记 DEGRADE |
| 洞察（INSIGHT） | `AiInsightGenerationService.trigger` 开头 + 线程池任务 `attach` 续写 | ASYNC（提交任务）→ LLM_CALL → 收尾 UPDATE 同一行 |
| 推荐（RECOMMEND） | `RecommendService.recommend`（仅 `reason-enabled=true` 时） | RECALL → FUSION → LLM_CALL（写理由）→ PERSIST |

设计要点：

1. **状态由显式标记驱动**：只有链路里调用过 `degrade(...)` 才记为 `DEGRADED`，
   不靠"返回值长什么样"反推 —— 否则"部分降级"（检索失败但回答正常）会被判错；
2. **推荐链路只在会调用模型时才开轨迹**：离线评测（`reason-enabled=false`）会产生
   成千上万次推荐，那些记录没有解释价值，只会把轨迹表灌满；
3. **`route` 字段由第一条 ROUTE 步骤派生**，列表页不必解析 JSON 就能看到"分派给谁、有没有回退"；
4. 审核消息发送处（`ArticleService` / `CommentService`）无条件挂 `TraceHeaders.propagate()`，
   没有轨迹上下文时是空操作。

#### 三、验证

| 项 | 结果 |
| --- | --- |
| 新增测试 | `TraceRecorderIntegrationTest`（6：步骤落库/route 派生/降级原因/跨线程续写/未知 trace 安全/工具包装）、`AdminAiTraceControllerTest`（6：鉴权/过滤/步骤解析/404）、`AgentOrchestratorTest` 新增 2 条轨迹断言 |
| 后端全量 | **381 项：372 通过 + 9 条件跳过，0 失败**（M17 收尾时 367 项） |
| 前端 | 33 项通过；`npm run build` 成功（本轮未改前端） |
| 真实调用 | `ToolCallTraceSmokeProbe` 通过（DeepSeek 两次工具调用全部捕获，token 946/110/1056） |

#### 四、踩到并修掉的问题

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| `ModerationTriggerTest` 2 条失败 | 发送处多了一个 `MessagePostProcessor` 参数，`verify` 的签名对不上 | 断言补上 `any(MessagePostProcessor.class)` |
| `ModerationMessageListenerTest` 6 条 NPE | 监听器新增 `@Autowired TraceRecorder`，`@InjectMocks` 没有对应 mock | 补 `@Mock TraceRecorder` |
| 全量测试会清空向量索引 | M15 起的既有行为 | 跑完后用 `M17_VECTOR_PROBE=true` 重建 `bitforum-kb` |
