# M17 交接文档：运营分析 Agent 与智能推荐

> 本文供**新会话（干净上下文）**接手 M17 时使用。
> 写于 2026-09-19，此时 M13-M16 已完成并推送，工作区干净、CI 通过。
>
> 阅读顺序建议：本文 → `task_plan.md`（总体计划）→ `findings.md`（技术证据）→ `progress.md`（执行记录）。
> 本文只写"新会话必须知道、但上面三份文档里没有或分散"的内容。

## 一、接手时的状态（已实测确认）

| 项目 | 状态 |
| --- | --- |
| 仓库 | `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring` |
| 分支 | `feat/ai-agent`，最新提交 `b82537d`，**与远端同步** |
| 基线 | 基于 `main` 的 `d6dd582`，已 push（M15 完成与 M16 完成各推一次） |
| 后端测试 | **308 项：302 通过 + 6 项条件跳过**（跳过项均为需要真实 API Key 的调用测试） |
| 前端测试 | 23 项通过；构建 1889 模块 |
| Flyway | 已到 **V15**（下一个新迁移是 **V16**） |
| CI | GitHub Actions backend + frontend 均通过（最近 run `35427320716`） |
| 环境启动 | `./dev.sh`（一键起中间件 + 后端 + 前端）；停止用 `./stop-local.sh` |
| 中间件 | MySQL / Redis Stack / RabbitMQ 三容器 `Up (healthy)`；8080 / 5173 端口空闲无残留 |

## 二、M17 范围（来自 task_plan.md Phase 5）

**目标**：让 AI 读懂运营数据，并为用户生成可解释的推荐。

- **Flyway V16**：`ai_insight_report`（运营洞察报告）
- `AnalystAgent`：把 M6 `AdminDashboardService` 的全部统计包装为 `@Tool`，
  LLM 生成自然语言洞察与建议
- **Flyway V17**：`ai_recommend_log`（推荐记录，用于评估与基线对比）
- `RecommendAgent`：三路召回融合 —— 向量相似（同主题，复用 M15 的 `RagService`）+
  热度（M6 热榜 ZSet）+ 关注关系（M9），LLM 生成推荐理由
- 前端：文章详情页「相关推荐」、AI 助手回答末尾推荐相关帖、管理员看板「AI 运营洞察」卡片
- **验收**：给出 Top-10 推荐及理由；命中率与纯热榜基线对比

## 三、可直接复用的既有能力（M13-M16 留下）

| 能力 | 位置 | M17 怎么用 |
| --- | --- | --- |
| Agent 抽象与路由 | `ai/agent/Agent`、`ai/orchestrator/AgentOrchestrator` | `AgentType` 已有 `ANALYST` / `RECOMMEND` 常量 |
| 工具注册表 | `ai/tool/ToolRegistry` | 按 Agent 类型装配工具（最小权限） |
| RAG 检索 | `ai/rag/RagService` | 推荐的第一路召回（向量相似） |
| 热榜数据 | `RedisService`（ZSet） | 第二路召回（热度） |
| 关注关系 | `UserFollowService`、`user_follow` 表 | 第三路召回（关注） |
| 看板统计 | `AdminDashboardService` | AnalystAgent 的工具来源 |
| 异步消息模式 | `RabbitMQConfig` + `AfterCommitExecutor` | 若洞察生成要异步，照抄这套 |
| 结构化输出 | `ModerationAgent` 的做法 | 若洞察要结构化，用**固定字段**而非列表 |
| 人工反馈闭环 | `ai_moderation_record.feedback` | 推荐效果可复用同样的反馈思路 |
| 幂等 | `RedisService.isMessageProcessed/markMessageProcessed` | 消息去重 |

## 四、必须遵守的约束（都踩过坑）

1. **`@ConditionalOnBean` 在用户配置类中不可靠** → 用 `@ConditionalOnProperty`
2. **`@MapperScan` 不扫子包** → 新起包名要同步更新 `BitForumSpringApplication`
3. **测试配置值写成占位符形式** → 硬编码会覆盖环境变量（M13 因此 401）
4. **工具/提示词改动必须用干净会话验证** → 旧会话历史会延续旧结论
5. **Redis 必须是 Redis Stack** → 普通 Redis 报 `FT._LIST` unknown command，整个上下文起不来
6. **异步消息必须在事务提交后发送** → 用现成的 `AfterCommitExecutor`，否则消费者读到旧状态
7. **结构化输出用固定字段** → 让模型返回"维度列表"时数量不稳定（T7 实测）
8. **只新增 Flyway 迁移，绝不修改历史迁移**
9. **AI 能力必须有降级路径** → AI 不可用不能影响论坛主流程

## 五、M16 的方法论遗产：评测规范（M17 应沿用）

M16 的评测没有直接跑数据，而是**先冻结规范再评测**，产出 `m16-eval-protocol.md` v1：

- **规则=球门，提示词=实现**：为对齐规则改提示词允许；为迁就模型放宽规则不允许
- 一级指标（漏放率/误伤率/安全召回率）与二级指标（严格准确率等）分离
- 数据集划分开发集与独立测试集，**测试集在阈值冻结前不得查看输出、只运行一次**
- 阈值必须扫描确定，且**禁止用测试集结果回改阈值**

**M17 的"推荐命中率与纯热榜基线对比"应当沿用同一套方法论**：先定义命中率的口径、
数据集划分与评估流程，再跑数据。M16 的教训很直接 —— 硬约束抓到了一条被总准确率掩盖的漏放，
独立测试集又暴露了阈值过拟合；如果 M17 直接跑数字，很可能重演同样的问题。

## 六、环境与工具约束（macOS）

| 事项 | 说明 |
| --- | --- |
| 启动项目 | `./dev.sh`（一键起中间件 + 后端 + 前端）；停止用 `./stop-local.sh` |
| Ctrl+C | **只停前端**，后端需 `./stop-local.sh`（Maven 忽略 SIGINT） |
| JDK | 必须先 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`；本机默认是 25 |
| Maven | 无 `mvn` 命令，用 `./mvnw -s maven-settings.xml` |
| 后端日志 | `.dev-logs/backend.log` |

### 跑测试的完整环境变量（Spring Boot 不读 .env，必须显式注入）

```bash
cd /Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring
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
./mvnw -s maven-settings.xml -Dtest=ModerationAgentSmokeTest test
```

## 七、协作方式（重要，请遵守）

1. **重大选择必须先说明方案与取舍，等用户确认后再动手** ——
   用户会把这些材料拿给外部 AI 讨论后再定；纯技术验证与实现细节可自行推进。
2. **用户对技术术语的耐受度有限**：解释时用平实语言并说明"这对你意味着什么"，
   避免术语堆砌；如果用户说"看不懂"，改用类比与后果描述，而不是换更专业的词。
3. 每个模块完成跑 `mvn test` + `npm run build`，**红了不开下一个**。
4. 提交前检查暂存区是否含真实 Key 片段（M14 曾误把 Key 前 6 位写进文档）。
5. 只新增 Flyway 迁移；不回滚 M1-M16 的任何功能与文档。
6. 不提交 `.env`；API Key 只走环境变量。
7. 描述能力必须区分「已实现」与「规划中」，**不虚构评估数据**。

## 八、交接时可用的工具（M16 期间新增）

| 工具 | 用途 |
| --- | --- |
| `scripts/project-overview.sh` | 生成**项目全景**（代码地图 + 数据库结构 + 接口清单 + AI 模块要点），供外部 AI 通读；`--with-code` 附核心类全文 |
| `scripts/export-ai-context.sh` | **按需**上下文导出（简报 + README + 指定文件 + git 状态），支持 `--clip` 复制到剪贴板 |
| `docs/.../external-ai-briefing.md` | 给外部 AI 的项目简报（含 README 中已过期的信息与已踩过的坑） |

需要外部 AI 参与决策时，用前两个工具生成可粘贴的材料。

## 九、已知遗留（不阻塞交付）

- 知识库集成测试收尾会清空向量索引，演示前需在管理页点一次「全量重建」。
- **自动放行保持默认关闭**：M16 评测显示阈值在独立测试集上覆盖率 0%，
  数据不足以支撑启用一个会自动改变线上状态的动作。
- 知识库相似度阈值 0.5 待文章库变大后复测。
- M16 评测的偏差集中在"模糊内容"的档位选择（严格准确率约 97%，漏放与误伤均为 0）。

## 十、开工前的准备动作

1. 启动环境：`./dev.sh`（或 `docker compose up -d mysql redis rabbitmq` 只起中间件）。
2. 读 `task_plan.md` 的 Phase 5 与 `findings.md` 的相关小节。
3. **先确认 M17 的最大技术风险并做最小验证**（沿用 M15/M16 的惯例：
   先把唯一没实测过的技术点验掉，再写业务代码）。
4. 遇到需要产品/策略取舍的选择，先整理材料交用户确认。
