# M18 交接文档：可观测性、评估与工程化闭环（收敛版）

> **2026-09-19 收尾更新（本文其余部分为 M18 开工时的记录）**：
> M18 已全部完成 —— V18 执行轨迹、V19 用量统计、`TraceRecorder`（四条链路埋点）、
> `AiDegradeGuard` 统一降级、管理端轨迹页与用量概览，**外加克制版 `TokenBudgetGuard`**。
> 最终状态：后端 **416 项测试**（406 通过 + 10 条件跳过）/ 前端 36 项全绿，Flyway 到 V19，
> 向量索引 114 片段；真实环境端到端验证通过（一次提问 5 步轨迹含工具调用）。
> 执行记录见 `progress.md` 的 M18-0 ~ M18-5；当前状况与讨论材料见 `m18-status-summary.md`。
> 下文第一节的测试数（367）等开工时数据仅作历史留存。

> 本文供**新会话（干净上下文）**接手 M18 时使用。
> 写于 2026-09-19，此时 M13-M17 已完成并**本地提交**（未推送）。
>
> 阅读顺序建议：本文 → `task_plan.md`（总体计划 + M17 实施决策 + M18 收敛范围）
> → `findings.md`（技术证据，按 6.x）→ `progress.md`（执行记录，M17-0 ~ M17-12）。

---

## 一、接手时的状态（已实测确认）

| 项目 | 状态 |
| --- | --- |
| 仓库 | `/Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring` |
| 分支 | `feat/ai-agent`，最新提交 `862f5b0`，**领先 `origin/feat/ai-agent` 7 个提交（未推送）** |
| 工作区 | 干净（`git status --short` 无输出） |
| 后端测试 | **367 项：358 通过 + 9 条件跳过**（跳过项均需真实 API Key） |
| 前端测试 | 33 项通过；构建成功 |
| Flyway | 已到 **V17**（下一个新迁移是 **V18**） |
| 环境 | `./dev.sh` 已启动：后端 8080、前端 5173 均在运行；三容器 healthy |
| 向量索引 | `bitforum-kb`：**114 个片段**（55 篇已发布文章） |
| 评测数据 | 50 篇合成评测文章 + 30 位评测用户 + 173 条收藏，**保留在库中**（答辩演示用） |

**M17 的六个提交**（本地）：

```
862f5b0 docs(graduation): record M17 decisions, technical evidence and progress (M17)
3ec6c8e test(ai): add M17 evaluation corpus, frozen protocol and report (M17)
29bf6c5 feat: expose recommendation APIs and render them on three surfaces (M17)
2d46086 feat(ai): add analyst agent with async insight generation and admin API (M17)
355afc5 feat(ai): implement three-channel recall, RRF fusion and recommendation reasons (M17)
6a53993 feat(ai): add M17 insight and recommendation tables (M17)
```

---

## 二、M18 的范围（2026-09-19 与外部 AI 讨论后收敛）

**目标变了**：不再追求"功能更多"，而是把 AI 系统从"功能很多"收束成
**"全过程可解释、可追踪、可降级"**。

### 要做（按优先级）

| 优先级 | 内容 | 说明 |
| --- | --- | --- |
| 1 | **Flyway V19 `ai_execution_trace`** + `TraceRecorder` | 记录：路由 → 工具调用链 → 每步耗时 → token。这是 M18 的核心 |
| 2 | **Flyway V18 `ai_usage_stat`** | 按 用户 / Agent / 天 统计 token 与费用。只需 `ai_usage_stat`，**不含** `ai_prompt_template` |
| 3 | **`AiDegradeGuard`** | LLM 超时 / 异常 / 额度的统一降级处理（把散在各 Agent 里的降级收敛成一处） |
| 4 | **简单可视化** | 前端「AI 执行轨迹」页 + 用量概览。**简单优先**，不追求大而全的报表 |

### 砍掉（本轮明确不做，不要实现）

- ❌ `ai_prompt_template`（Prompt 动态管理 / 版本表）
- ❌ LLM-as-Judge 评估脚本
- ❌ 管理员「AI 设置」页（模型参数、功能开关的完整配置台）

### 时间充足才做

- ✅ `TokenBudgetGuard`（**克制版，已在 M18 收尾实现**；原计划为「时间充足才做」）：
  单轮输入上限、单用户日配额、超限友好提示（不做套餐/充值/会员等级/分布式配额）

### 验收标准（相应调整）

原计划要求"评估报告含准确率、召回率、幻觉率、平均延迟、单次成本"——
**由于 LLM-as-Judge 被砍掉，验收改为**：

1. 任意一次 AI 调用都能查到**完整执行轨迹**（路由、工具调用链、每步耗时、token）；
2. 用量可按 用户 / Agent / 天 聚合，**能算出单次成本**（延迟与 token 数据现成，见下）；
3. AI 不可用时，**全站 AI 功能有统一的降级表现**，且用户能看懂"为什么降级"；
4. 轨迹页可演示（答辩时的关键画面）。

---

## 三、可以直接复用的既有能力（M13-M17 留下）

| 能力 | 位置 | M18 怎么用 |
| --- | --- | --- |
| Agent 抽象与路由 | `ai/agent/Agent`、`ai/orchestrator/AgentOrchestrator` | 轨迹的"路由"环节直接从这里埋点 |
| 工具注册表 | `ai/tool/ToolRegistry` | 工具调用链的起点 |
| 结构化输出 | `ModerationAgent` / `RecommendReasonAgent` 的做法 | 轨迹条目的落库形态参考 |
| **token 与耗时已在采集** | `ai_message`（prompt/completion/total tokens、latency_ms）、`ai_moderation_record`、`ai_insight_report`、`ai_recommend_log` | `ai_usage_stat` 可从这些表聚合，**不必重新埋点** |
| 异步执行器 | `ai/config/AiInsightConfig`（单线程池）、`RabbitMQConfig` | 轨迹要能覆盖异步链路 |
| 降级路径 | 每个 Agent 都实现了降级，但**各写各的** | `AiDegradeGuard` 就是把这些收敛成一处 |
| 评测方法论 | `m17-eval-protocol.md`、`m16-eval-protocol.md` | 若将来要做评估，沿用"先冻结规范再跑数" |

---

## 四、必须遵守的约束（都踩过坑）

1. **`@ConditionalOnBean` 在用户配置类中不可靠** → 用 `@ConditionalOnProperty`
2. **`@MapperScan` 不扫子包** → 新包名要同步更新 `BitForumSpringApplication`
3. **测试配置值写成占位符形式** → 硬编码会覆盖环境变量
4. **工具/提示词改动必须用干净会话验证** → 旧会话历史会延续旧结论
5. **Redis 必须是 Redis Stack** → 普通 Redis 报 `FT._LIST` unknown command
6. **异步消息必须在事务提交后发送** → 用现成的 `AfterCommitExecutor`
7. **结构化输出用固定字段** → 让模型返回"变长列表"时数量不稳（T7/T9 实测）
8. **只新增 Flyway 迁移，绝不修改历史迁移**
9. **AI 能力必须有降级路径** → AI 不可用不能影响论坛主流程
10. **新增接口若用 `@RequestAttribute("userId")`，必须同时加进 `WebMvcConfig` 的登录拦截列表** ——
    漏配会让接口直接 500（M17 实测踩到）

---

## 五、M17 已定稿的决定（新会话不要重新讨论）

| 主题 | 决定 |
| --- | --- |
| 推荐排序 | **完全由 Java 完成**（三路召回 → RRF → Top-N）；LLM 只写解释，不参与选文 |
| 推荐评测 | 合成数据 + 留一法，`HitRate@10` / `MRR@10`；结果：融合 **0.8889**（测试集）vs 纯热榜 0.2222 |
| 向量区分度 | **接受 0.0147 作为已知局限**结束 M17。**不为了达标重新改语料、换模型或改算法** |
| 候选过滤 | 只排除作者本人与已收藏；浏览不排除；**「已点赞」本阶段明确不支持** |
| 运营洞察 | 管理员主动触发 + 异步生成 + 结果落库；不做定时生成 |
| 匿名访客 | 可以看到相关推荐（走"内容相似 + 热度"两路） |

> **⚠️ 最重要的纪律**：M17 的推荐数字已经完成它的实验使命。
> **下一步最不该做的就是继续优化 0.8889** —— 把时间投到 M18 的
> 可解释、可追踪、可降级上，价值高得多。

---

## 六、答辩时必须主动强调的三件事（不要等评委问）

1. **测试集只有 9 位用户**（开发集 21 位）——指标粒度粗，单用户排名变化就会明显影响 MRR；
2. **数据是合成的**：文章、用户、行为均由作者构造，不是真实社区流量；
3. **热榜基线偏弱、关注信号过强**：热度被刻意设成与主题无关，而"关注=同兴趣"是直接设定的，
   所以"融合大幅超越热榜"里**有一部分是数据构造的功劳**。

这三条已写在 `m17-eval-report.md` 的「局限与诚实声明」一节，答辩材料直接引用即可。

---

## 七、环境与工具约束（macOS）

| 事项 | 说明 |
| --- | --- |
| 启动 | `./dev.sh`（中间件 + 后端 + 前端）；停止用 `./stop-local.sh` |
| Ctrl+C | **只停前端**；后端需 `./stop-local.sh` |
| JDK | 必须先 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`（本机默认 25） |
| Maven | 无 `mvn` 命令，用 `./mvnw -s maven-settings.xml` |
| 后端日志 | `.dev-logs/backend.log` |
| **跑完全量测试必须重建向量索引** | 全量 `mvn test` 会清空 `bitforum-kb`（M15 起的既有行为），
此后所有依赖向量召回的路径**静默退化为两路**。重建命令见第八节 |

### 跑测试的完整环境变量（Spring Boot 不读 .env）

```bash
cd /Users/jiu/Developer/Projects/Java/BitFrom/bit-forum-spring
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_DATASOURCE_USERNAME="$(grep '^SPRING_DATASOURCE_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_DATASOURCE_PASSWORD="$(grep '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_USERNAME="$(grep '^SPRING_RABBITMQ_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_PASSWORD="$(grep '^SPRING_RABBITMQ_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'
./mvnw -s maven-settings.xml test
cd frontend && npm test && npm run build
```

真实调用 DeepSeek（默认跳过）：

```bash
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2- | tr -d ' \r\n\t')"
./mvnw -s maven-settings.xml -Dtest=AiInsightSmokeTest test
```

---

## 八、已知遗留与常用命令

| 遗留 | 处理方式 |
| --- | --- |
| 全量测试会清空向量索引 | 重建：`export M17_VECTOR_PROBE=true && ./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test` |
| 向量区分度弱（0.0147） | **已知局限，本轮不动**（决定 2） |
| 「已点赞」无法反查 | 写入后续优化，**不在 M18 前动 M6**（决定 3） |
| 评测数据带 `[M17Eval]` 前缀 | 答辩保留，**展示成明确的"演示数据"**（决定 4）；一键清理：`M17_EVAL_CLEANUP_ONLY=true ./mvnw -s maven-settings.xml -Dtest=M17EvalDataSeeder test` |
| 洞察报告无前端历史列表 | 接口 `/api/admin/ai/insight/history` 已有，前端只展示最新一份（非验收项） |
| 本地 7 个提交未推送 | 按 handoff 策略：检查点才 push；**推送需要用户单独确认** |

### 演示账号（合成评测数据）

- 普通用户：`m17eval_user_01` ~ `m17eval_user_30`，密码 `M17Eval@2026`
- 主题作者：`m17eval_expert_<theme>`（theme ∈ spring / persistence / redis / mq / frontend / deploy / concurrency / api）
- 管理员账号：请向项目作者确认（M17 期间未使用）

---

## 九、开工前的准备动作

1. 确认环境：`./stop-local.sh --status`（应看到 8080/5173 运行中、三容器 healthy）；
   没有就 `./dev.sh`。
2. 读 `task_plan.md` 的 M18 章节与 Phase 6、本文第二节。
3. **先确认 M18 的最大技术风险并做最小验证**（沿用 M13-M17 的惯例：
   先把唯一没实测过的技术点验掉，再写业务代码）。
   M18 最可能的风险点是：**轨迹要覆盖异步链路**（洞察是线程池、索引是 RabbitMQ），
   以及**在既有表已有 token/耗时的前提下如何设计 `ai_usage_stat` 的聚合口径**。
4. 遇到需要产品/策略取舍的选择，先整理材料交用户确认（用
   `scripts/export-ai-context.sh --clip <文件...>` 生成可粘贴材料）。
5. **提交规范**：每个模块完成即本地提交（不 push）；提交前跑 `mvn test` + `npm run build`，
   红了不开下一个；提交前做一次安全自检（有无真实 Key 片段）。
