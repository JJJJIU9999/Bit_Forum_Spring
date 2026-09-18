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
| 当前阶段 | Phase 0 已完成；前置环境已就绪 |
| 最新 Flyway 迁移 | `V12__add_notification_source_message.sql`（AI 模块将新增 V13-V19） |
| 模块完成度 | M13-M18 全部未开始 |
| 当前主线 | **前置环境已就绪，可进入 M13** |
| 中间件状态 | MySQL / Redis Stack / RabbitMQ 三容器 `Up (healthy)` |
| 数据库状态 | MySQL 8.0.46，`bit_forum` 库存在，Flyway V1-V12 全部 success |
| 待办 | 无（Key 已填写、端口已改回 3306、中间件已就绪） |

## 总体进度

| 模块 | 名称 | 状态 | 验证情况 | 文档位置 |
| --- | --- | --- | --- | --- |
| Prep | AI Agent 升级计划书 | 已完成 | 计划书、勘察证据、进度记录已落盘 | `docs/graduation/ai-agent-upgrade/` |
| Prep | 前置环境（Redis Stack 替换 + Key 配置 + 中间件启动） | 已完成 | 三容器 healthy；RediSearch 2.10.20 已加载；向量检索端到端验证通过 | `findings.md` 第五节 |
| M13 | AI 基础设施与对话骨架 | 未开始 | — | 待创建 |
| M14 | 工具集与 Tool Calling | 未开始 | — | 待创建 |
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
- ONNX 嵌入模型与 Redis Stack 替换是本计划两个尚未实测的技术点，M13/M15 开始前必须先做最小验证。
