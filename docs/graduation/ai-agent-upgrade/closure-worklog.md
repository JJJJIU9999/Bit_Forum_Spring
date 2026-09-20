# BitForum 最终收口工作日志

> 依据：`BitForum-DeepSeekHarness-Flash-finalization-handoff.md`（收口任务书，断点续作协议）。
> 规则：每完成一个小步骤立即更新本文件；不依赖聊天记忆；本轮范围只有文档一致性、性能观察、故障注入。

## 当前状态

- 当前阶段：**阶段 4B（最终收口报告与交接）已完成 —— 任务书全部 14 个阶段收束**
- 阶段状态：阶段 0 / 1A / 1B / 1C / 2A / 2B / 2C / 2D / 3A / 3B / 3C / 3D / 4A / 4B **全部完成**
- 技术结论：**已具备合并 `main` 的条件**（对照任务书 §六 的 14 条清单，全部满足）；剩余动作是作者的提交与合并决策
- 最后更新时间：2026-09-20（本轮会话）
- 当前分支：`feat/ai-agent`
- 当前 HEAD：`514f301 docs(graduation): refresh status summary with push and CI results (M18)`
- 工作区摘要：**不干净** —— 5 项已跟踪文件被修改 + 2 项新增未跟踪（详见「阶段 0」第三节），均属**前一执行会话**的收口工作，本轮未改动它们
- 下一步：**等作者决策**（均需明确授权）：
  ① 合并回 `main`（建议 `merge --no-ff` + 合并后 CI + 打版本 tag）——**当前尚未授权**；
  ② 是否补第二项建议补强（审核 MQ 与洞察线程池的链路级故障注入）——**不补则必须保持"未验证"口径**

## 已完成步骤

### 阶段 0：建立真实基线和工作日志

- 实际操作：
  1. 读取任务书全文（722 行）、AI 升级目录文档清单、`scripts/export-ai-context.sh` 的页眉生成逻辑；
  2. 把任务书复制进仓库根目录（保留原文件名）；
  3. 只读核实 Git 状态、文件盘点、导出脚本机制；未读取 `.env`，未发起真实模型调用，未修改业务代码。
- 修改文件：
  - 新增 `BitForum-DeepSeekHarness-Flash-finalization-handoff.md`（任务书副本，22806 字节，sha256 前缀 `96df8a3d` 与附件一致，权限已改为 644 以便后续会话读取）
  - 新增 `docs/graduation/ai-agent-upgrade/closure-worklog.md`（本文件）
- 执行命令与真实结果：

| 命令 | 实际结果 |
| --- | --- |
| `git status --short` | 5 改 + 2 新（见下节），非空 |
| `git branch --show-current` | `feat/ai-agent` |
| `git rev-parse --short HEAD` | `514f301` |
| `git log -1 --oneline` | `514f301 docs(graduation): refresh status summary with push and CI results (M18)` |
| `git rev-list --count main..HEAD` | **41** |
| `git rev-list --count origin/main..HEAD` | **41** |
| `git rev-list --count origin/feat/ai-agent..HEAD` | **0**（本地与远端一致，已 push） |
| `git remote -v` | `origin https://github.com/JJJJIU9999/Bit_Forum_Spring.git`（fetch/push 相同） |

- 文件盘点：

| 任务书要求盘点 | 实际 |
| --- | --- |
| `AGENTS.md` | **不存在**（任务书为"如存在"，不阻塞） |
| `README.md` | 存在 |
| 项目简报 / 上下文导出源 | `docs/graduation/ai-agent-upgrade/external-ai-briefing.md` + `scripts/export-ai-context.sh` |
| `task_plan.md` / `progress.md` / `findings.md` | 存在 |
| `m18-status-summary.md` / `m18-handoff.md` | 存在 |
| `docs/technical-debt.md` | 存在 |
| `closure-worklog.md` | **本轮创建**（此前不存在，尽管已有会话在按任务书作业） |
| 性能/故障注入报告 | **不存在**（阶段 2D/3D 未产出） |

### 前一执行会话的已完成工作（本轮登记，未改动）

任务书前一次执行已推进到「故障注入」并留下未提交改动。工作区 7 项：

| 文件 | 性质 | 内容 |
| --- | --- | --- |
| `src/main/java/com/bitforum/ai/trace/TraceRecorder.java` | **生产代码（修改）** | `degrade(reason, message)` → `degrade(reason, detail)`：轨迹只持久化 `TraceDegradeReason` 的统一文案，底层 detail 改写入 debug 日志 |
| `src/test/java/com/bitforum/ai/agent/QaAgentFaultInjectionIntegrationTest.java` | 新增（未跟踪） | 阶段 3B 故障注入：`spring.ai.deepseek.base-url=http://127.0.0.1:9` + 占位 key + `spring.ai.retry.max-attempts=1` + `rabbitmq.listener.simple.auto-startup=false` |
| `src/test/java/com/bitforum/ai/observation/M18PersistenceObservationTest.java` | 新增（未跟踪） | 阶段 2B 的确定性本地开销观察（`rabbitmq.listener.simple.auto-startup=false`） |
| `src/test/java/com/bitforum/ai/trace/TraceRecorderIntegrationTest.java` | 修改 | 增加测试上下文隔离属性 |
| `docs/graduation/ai-agent-upgrade/progress.md` | 修改 | 顶部新增「2026-09-20 最终收口执行记录：Final-0 / Final-1 / Final-2」 |
| `docs/graduation/ai-agent-upgrade/findings.md` | 修改 | 顶部新增「九、2026-09-20 最终收口只读基线」 |
| `docs/graduation/ai-agent-upgrade/task_plan.md` | 修改 | 顶部新增「2026-09-20 最终收口计划」 |

- 本轮对上述改动的**只读核对**结论：
  - `traceRecorder.degrade(...)` 的调用方为 `AiDegradeGuard`、`AgentOrchestrator`（预算分支）、`RecommendService`（预算分支），三者传入的本就是内部细节（`e.getMessage()`、预算 detail），**新语义与调用方意图一致**，属收紧而非破坏；且满足任务书阶段 3B 的"Trace 不得暴露连接详情"验收项。
  - 但该改动**改变了公共方法的参数含义**（第二参数从"可覆盖文案"变为"仅内部 detail"），需作者确认是否接受，并在阶段 1B/3D 文档中如实说明。
  - 上述会话未按任务书建立 `closure-worklog.md`，而是把记录写进了 `progress.md` / `findings.md` / `task_plan.md` 顶部 → 存在两套日志的风险（见"发现但不在本轮处理的问题"）。

### 阶段 1A：文档一致性审计（只读，输出冲突清单）

- 实际操作：按任务书搜索词逐项检索 `README.md` 与 `docs/graduation/**/*.md`，区分"真正错误 / 历史记录 / 易变 Git 数据"；**未修改任何既有文档**。
- 搜索词：未开始、时间充足、TokenBudgetGuard、M16、M17、M18、5a385db、514f301、领先 38、领先 41、394、407、Redis 7、Redis Stack、V18、V19、未 push、工作区干净。

#### 归类一：真正错误（当前态声明与实时仓库不符，建议修正）

| # | 文件:行 | 当前表述 | 实时证据 | 建议处理 |
| --- | --- | --- | --- | --- |
| 1 | `ai-agent-upgrade/m18-status-summary.md:40` | 相对 `main` 领先 **38** 个提交 | `git rev-list --count main..HEAD` = **41** | 不硬编码；改为"以导出页眉实时值为准"，或标注"截至 2026-09-19 快照" |
| 2 | `ai-agent-upgrade/m18-status-summary.md:41,42,100` | 已 push（`b82537d..5a385db`）、HEAD `5a385db` | 实际已 push 至 `514f301`（ahead of upstream = 0） | 同上（易变数据） |
| 3 | `ai-agent-upgrade/m18-status-summary.md:43` | 工作区干净 | 现有 7 项未提交改动 | 同上（易变数据） |
| 4 | `ai-agent-upgrade/external-ai-briefing.md:35` | 相对 main 领先 38，其中 **16 个尚未 push** | 41 / 0（已全部 push） | 同上 |
| 5 | `毕业设计文档总览.md:62,392,658` | 领先 origin **14** 个提交、尚未 push | 41（vs main）、已 push | 同上 |
| 6 | `毕业设计进度.md:13,14,27,802` | 最近提交 `61c2c26`、领先 14、未 push | HEAD `514f301`、41、已 push | 同上 |
| 7 | `BitForum项目完整历史总结.md:16,17,18,51` | 领先 origin **15**、尚未 push、HEAD `d8bc308`、工作区干净 | HEAD `514f301`、已 push、工作区有改动 | 同上 |
| 8 | `BitForum项目完整历史总结.md:96` | 技术栈表「缓存 \| Spring Data Redis / **Redis 7**」 | 已换 `redis/redis-stack-server:7.4.0-v8`（同文件 3.1 节新增行可证） | **改为 Redis Stack**（确定性错误，与易变数据无关） |
| 9 | `BitForum项目完整历史总结.md:747` | 「push 检查点 / 合并回 `main` \| **未做**」 | push 已完成（`514f301`，CI success）；合并确实未做 | 拆成两行：push 已完成；合并待定 |
| 10 | `ai-agent-upgrade/progress.md:36` | 快照表「远端同步 \| 本地分支**未 push**」 | 已 push（ahead 0） | 更新并标注日期 |
| 11 | `BitForum项目完整历史总结.md:745` | 「压测 / 性能观察 \| **未做**」 | 仍属实，但正由本轮任务书阶段 2 处理 | 保留，补一句「本轮任务书阶段 2 进行中」 |

#### 归类二：历史记录（可保留，建议补日期或"历史"标注，不要求修改内容）

| 文件:行 | 表述 | 判断依据 |
| --- | --- | --- |
| `ai-agent-upgrade/m15-handoff.md:4,14,16` | 写于 09-18、8 个提交、未 push | 文件本身即当时交接记录 |
| `ai-agent-upgrade/m17-handoff.md:4,14,15` | 写于 09-19、`b82537d`、与远端同步 | 同上 |
| `ai-agent-upgrade/m18-handoff.md:25` | 工作区干净（"接手时的状态"表） | 已由顶部 09-19 收尾块覆盖，属开工时记录 |
| `BitForum项目完整历史总结.md:637` | 08-02 摘要（含"不得宣称 Spring AI"） | 已在其后标注"该摘要为历史状态"；第 14 节已校正 |
| `ai-agent-upgrade/findings.md:5`、`progress.md:8` | HEAD `514f301`、领先 41、工作区干净 | **前一会话 09-20 的实测记录，准确**（"工作区干净"指该会话开始时） |
| `progress.md` 的 M17-13 / M18-x 各节 | 各阶段测试数与提交 hash | 带日期的执行记录 |

#### 归类三：易变 Git 数据的根因（本次审计的核心结论）

- **现象**：同一份导出的上下文包里，页眉写 `514f301 / 领先 41`（新），内嵌的 `m18-status-summary.md` 写 `5a385db / 领先 38`（旧）。
- **根因（已核实到代码行）**：`scripts/export-ai-context.sh:130-151` 的页眉是**运行时动态生成**的
  （`生成时间` / `分支（相对 main 领先 N）` / `最新提交` / `工作区` / `测试基线`），
  而**被拼接进来的静态文档**（`m18-status-summary.md`、`external-ai-briefing.md`、
  `毕业设计文档总览.md`、`毕业设计进度.md`、`BitForum项目完整历史总结.md`）把同一批指标**硬编码**了。
  → 因此**根因不在脚本的 Git 查询**，而在于"静态文档承载了易变指标且未标注日期"。
- **建议（交阶段 1B 决策，本阶段不改）**：
  1. 页眉增加 `（实时）` 标识，并声明"页眉为唯一实时来源"；
  2. 内嵌文档里的 Git 指标二选一：**删除**（指向页眉）或**改写为"截至 YYYY-MM-DD 的快照"**；
  3. 测试基线（407 / 36）保留但统一标注为"最近一次已验证基线 + 日期"，避免被读成本轮新结果；
  4. 若希望更强约束，可让导出脚本在拼接前对易变关键词做一次"未标注日期"的提示性检查（**只提示、不改写**，避免误伤历史记录）。

- 阶段 1A 未修改任何既有文档；本清单即为 1B 的输入。

### 阶段 1B：修正文档与导出方式

- 作者已确认两个前置决策：① 接受 `TraceRecorder.degrade(reason, detail)` 的语义变更（轨迹只存统一文案）；
  ② 易变 Git 数据采用「**保留数值 + 标注"截至 2026-09-20 快照" + 指向实时来源**」的口径（不删除）。
- 本轮修改文件（先列后改，全部为文档/轻量脚本，**未改业务代码**）：

| 文件 | 改动 |
| --- | --- |
| `scripts/export-ai-context.sh` | ① 页眉新增"本页眉 5 项是实时值，与内嵌文档快照冲突时以页眉为准"；② 新增 `warn_volatile_snapshots()`，对简报/README/相关文件做**只提示不改写**的易变指标检查（stderr 输出、不影响退出码）；③ README 章节标题由"注意其中有已过期信息"改为"已与 M13-M18 收口状态同步" |
| `ai-agent-upgrade/m18-status-summary.md` | 章节三加"Git 字段为时间点快照"说明；领先 38→**41**、push 区间 `b82537d..5a385db`→`b82537d..514f301`、HEAD→`514f301`、push 行→`514f301`，均加"截至 2026-09-20 快照"；工作区行改为"该次核实为干净，此后收口工作产生未提交改动（见本日志）" |
| `ai-agent-upgrade/external-ai-briefing.md` | 分支行：领先 38 / 16 未 push → 领先 **41** / **已全部 push** + 快照标注 |
| `毕业设计文档总览.md` | 3 处：分支说明、当前状态行、结尾代码块 → 已 push（`514f301`）+ 相对 main 41 |
| `毕业设计进度.md` | 4 处：最近提交行、工作区行、过期更正表、"下一步"代码块与 M13-M18 小节标题 → 已 push |
| `BitForum项目完整历史总结.md` | 文首块（HEAD/推送/工作区）、第 2 节指引、**技术栈"Redis 7"→"Redis Stack"**、剩余事项表（拆出"push 已完成 / 合并未做"、性能观察标注阶段 2 进行中） |
| `ai-agent-upgrade/progress.md` | 状态快照表"远端同步"行 → 已 push + 快照标注 |

- 未采用的做法（刻意）：**没有**批量删除内嵌文档里的 Git 指标，也**没有**让脚本改写内嵌内容 ——
  历史记录与"当时快照"本身有价值，一刀切删除会丢掉上下文；改用"标注 + 页眉声明"消歧。
- 针对性验证（本轮实测）：

| 检查 | 结果 |
| --- | --- |
| `bash -n scripts/export-ai-context.sh` | 通过（语法无误） |
| `git diff --check` | 通过，无空白/格式问题 |
| 重新导出 `.dev-logs/m18-status-context.md` | 页眉 `领先 41 / 514f301`，内嵌 status summary 同为 `41 / 514f301` + 快照标注 → **不再自相矛盾** |
| 导出脚本 stderr | 无易变指标告警（说明内嵌文档已带日期标注） |
| `TokenBudgetGuard` 未完成表述 | 仅剩 4 处**已澄清的历史记录**（`~~未实现~~ → 已实现`、"原本标为时间充足才做，收尾时按克制版补上"），无当前态误述 |
| Markdown 结构 | 表格、代码块、链接未破坏（改动均为行内文本替换） |

- 阶段 1B 结束时**未 commit / push / merge / tag**（遵守任务书第 10 条）。

### 阶段 1C：验证文档收口

- 实际执行：重新运行上下文导出、逐份核对五份当前态文档、链接路径检查、`git diff --check`、变更范围检查。
- 本轮**未修改功能代码**；仅补了一处文档口径（见下表最后一行）。

| 检查项 | 命令 / 方法 | 实际结果 |
| --- | --- | --- |
| 导出命令退出码 | `./scripts/export-ai-context.sh --out .dev-logs/m18-status-context.md <status-summary>` | **退出码 0**；输出 502 行；stderr 无易变指标告警 |
| 页眉 vs 内嵌一致性 | 比对页眉实时值与内嵌文档声明 | 页眉 `领先 41 / 514f301`；内嵌 `briefing:41 已全部 push`、`status-summary:41 514f301` → **一致**（1A 的冲突已消除） |
| README | 检查技术栈与迁移版本 | `Flyway V1-V19`、`Redis Stack`、`Spring AI 1.1.8` 均在：与现状一致；未含 Git 指标 |
| `m18-status-summary.md` | 检查状态表 | 全部 Git 字段带"截至 2026-09-20 快照；实时值以导出页眉或 git 为准" |
| `m18-handoff.md` | 检查当前态与历史区分 | 顶部有 09-19 收尾块（407 项 / V19）；正文第一节 367 项所在表属于"接手时的状态"，且顶部明确声明"本文其余部分为 M18 开工时的记录" → **可区分** |
| `progress.md` | 检查快照表 | "远端同步"行已改为"已 push（最新 514f301，领先 0）+ 截至 2026-09-20 快照" |
| `task_plan.md` | 检查顶部收口计划 | 顶部有"2026-09-20 最终收口计划（进行中）"，仅覆盖三项收口任务，无过期 Git 指标 |
| 链接路径 | 提取包内所有相对 Markdown 链接并判断存在性 | 2 个（`docs/graduation/ai-agent-upgrade/m17-eval-report.md`、`docs/technical-debt.md`）**全部存在** |
| `git diff --check` | 直接运行 | **通过**（无空白/格式问题） |
| 变更范围 | `git status --short` | 文档/脚本 9 项 + 新增 4 项（任务书副本、本日志、2 个测试文件）；**非文档改动 4 项全部来自前一执行会话且已获作者接受**（`TraceRecorder` 脱敏 + 3 个测试文件），本轮未新增业务代码改动 |

- 1C 中发现并处理的一处口径遗漏：`progress.md` 的"测试状态"行原本只写 407/36，未标注"最近一次已验证基线"（违反 1B 原则 4）。
  已补为"**最近一次已验证基线：2026-09-19；本轮收口尚未重跑全量**"，并**重新导出确认页眉继承了该日期口径**。
- 未验证项（如实记录，不冒充通过）：
  1. **全量 `mvn test` / `npm test` / `npm run build` 本轮未重跑**（属阶段 4A；且全量测试会清空 Redis 向量索引，需重建）；
  2. 真实 DeepSeek 端到端调用未执行（属阶段 2C，需作者批准费用）；
  3. `docs/technical-debt.md` 的内容级过期审查未做（仅确认文件存在）；
  4. 阶段 2/3 的性能与故障注入证据尚未产出。

### 阶段 2A：性能实现调查（只出方案）

> 本阶段只读代码，未写 benchmark、未调用真实模型、未修改任何生产代码。

#### 2A-1 真实调用链与时序（含类名与写入点）

| 链路 | 入口（真实类.方法） | 调用线程内的持久化与外部调用（按发生顺序） | 是否异步 |
| --- | --- | --- | --- |
| 对话 QA | `AgentOrchestrator.chat`（类上**无** `@Transactional`） | ① `AiConversationService.getOwnedConversation`（MySQL 读）；② `saveMessage(user)`（MySQL 写）；③ `MysqlChatMemoryRepository.findByConversationId`（MySQL 读）；④ **`TraceRecorder.start` → `traceMapper.insert`（`TraceRecorder.java:112`）**；⑤ **`AiTokenBudgetGuard.checkInputLength` + `check` → `AiUsageStatMapper.selectUserDaily`（`AiTokenBudgetGuard.java:100`，走 `idx_aius_user_date`）**；⑥ `RagService.retrieve` → 本地 ONNX 嵌入 + Redis `similaritySearch`（**同步、CPU 密集**，`RagService.java:86,92`）；⑦ `QaAgent` → `ChatClient.call()` DeepSeek HTTP（工具循环在 provider 内部）；⑧ `saveMessage(assistant)` + `updateConversationStats`（MySQL 写）；⑨ **`TraceRecorder.finish` → `traceMapper.update`（`TraceRecorder.java:297`）+ `AiUsageRecorder.record` → `usageMapper.insert`（`AiUsageRecorder.java:74`）** | **无**（全在主请求线程） |
| 内容审核 | `ModerationMessageListener.handleModeration` | `TraceRecorder.attachOrStart`（SELECT/INSERT）→ `ModerationService.moderate` → `ModerationAgent.analyze`（DeepSeek）→ `TraceRecorder.finish`（UPDATE + usage INSERT） | 相对发布请求是 **MQ 异步**；自身写入在消费线程内同步 |
| 运营洞察 | `AiInsightGenerationService.trigger` → `insightExecutor`（`AiInsightConfig` 单线程池） | 触发线程：`AiInsightService.createPending`（INSERT report）+ `TraceRecorder.start`（INSERT trace）+ 提交任务；任务线程：`attach`（SELECT）→ `AnalystAgent.analyze`（工具 + DeepSeek）→ `finish`（UPDATE + usage INSERT） | **线程池异步**（端到端需轮询报告状态） |
| 智能推荐 | `RecommendService.recommend`（类上无 `@Transactional`） | `RecommendRecallService.recall`（Redis 读）；`RecommendFusionService.fuse`（纯 Java）；`reasonEnabled=true` 时 `TraceRecorder.start`（`RecommendService.java:219`）→ `RecommendReasonAgent.generate`（DeepSeek）→ `saveRecommendLog`（MySQL INSERT ×N）→ `finish`（UPDATE + usage INSERT） | **无**（HTTP 线程内同步） |

**关键结论（决定 2B 怎么测）**：

1. `@Transactional` 在 AI 域**只出现在 `AiConversationService`**（5 处），
   Trace/Usage/Budget 的 Mapper 调用**不在任何长事务内** → 写入即提交，
   因此"主线程等待时间"就是真实开销，**不需要**额外区分"异步完成时间"。
2. 所有观测点的写入都发生在各自链路的调用线程内，**没有 `@Async`**（AI 域的 `@Async` 数为 0）。
3. 表结构与 INSERT 成本相关：
   `ai_execution_trace` 有 1 个唯一键 + 4 个二级索引（`V18` 中 5 条 KEY），
   `ai_usage_stat` 有 6 条 KEY → **usage 的 INSERT 比 trace 更贵**，这与观察值应当一致。

#### 2A-2 现有脚手架与它的三个问题

已有 `src/test/java/com/bitforum/ai/observation/M18PersistenceObservationTest.java`
（前一执行会话新增，未提交）：预热 5 次 + 35 个样本，测 `budget_query` / `trace_insert` / `usage_insert`，
输出形如 `M18_OBSERVATION operation=... n=35 average_ms=... p50_ms=... p95_ms=... samples_ms=[...]`，
并按 traceId 精确清理数据。

问题（2B 需要修）：

| # | 问题 | 影响 |
| --- | --- | --- |
| a | `trace_insert` 实际度量的是 `start`(INSERT) **+** `finish`(UPDATE **+ usage INSERT**) | 指标名与内容不符，且与 `usage_insert` 数字互相包含 |
| b | 预算查询只测"该用户当日无数据"一种情形 | 真实用户当日已有 N 行，`SUM` 的行数会影响耗时，缺上界参考 |
| c | 原始样本只打到 stdout（Surefire 报告） | 可追溯性弱；环境（JVM/MySQL 版本、基线行数、预热次数）未随样本一起记录 |

#### 2A-3 最小方案（交阶段 2B 实施）

**A 类：确定性本地开销（不调用模型、不新增生产代码）** —— 扩展现有观察测试：

| 指标名 | 度量内容（真实调用） | 为什么需要 |
| --- | --- | --- |
| `budget_query_empty` | `AiTokenBudgetGuard.check(无当日数据的用户)` | 空聚合基线 |
| `budget_query_with_rows` | 预置 50 行当日 usage 后再 `check` | 真实情形：`SUM` 随行数增长 |
| `trace_start` | 仅 `TraceRecorder.start`（`TraceRecorder.java:112`） | 冷 INSERT 成本（含 5 条 KEY 维护） |
| `trace_finish` | 仅 `TraceRecorder.finish`（`TraceRecorder.java:297` + `AiUsageRecorder.java:74`） | 收尾的真实代价（UPDATE + usage INSERT，**含 usage 是实现的真实行为**） |
| `usage_insert` | 直接 `AiUsageRecorder.record`（`AiUsageRecorder.java:74`） | 与 `trace_finish` 相减可得 UPDATE 的净代价 —— **这是允许的相减**（同类型、受控、无 LLM 噪声） |
| `local_overhead_total` | `checkInputLength + check + start + 3×step + finish` | 一次对话"纯本地"部分的合计，报告里最有用的单个数 |

执行要求：
- 预热 5 次 + **≥30 个有效样本**（沿用 35）；
- 输出 avg / P50 / P95 / min / max **以及原始样本数组**，并**落盘**到
  `target/m18-observation/local-persistence-<yyyyMMdd-HHmmss>.txt`（`target/` 不入 Git），文件头记录：
  JVM 版本、MySQL 版本、`ai_execution_trace` / `ai_usage_stat` 当前行数、预热次数、样本量、脚本名；
- 清理沿用"按 traceId + 测试用户 id 精确删除"，不触碰既有数据（复用 `cleanUp()`）；
- 报告中显式声明局限："本机单实例串行观察，不代表并发能力或生产 QPS"。

**B 类：真实端到端（阶段 2C，执行前必须由作者批准费用）**

- 固定输入、每类预热 1 次 + 记录 **5** 次（不得擅自加次数）；样本量 5 时 P95 必须标注"**小样本描述值**"。
- 覆盖四类：① QA（纯回答）；② QA + RAG；③ QA + Tool Calling；④ 推荐（`reason-enabled=true`）；
  运营洞察单列（异步链路，端到端 = 触发 → 报告 `SUCCESS`，需轮询）。
- **分组用数据驱动**：按轨迹 `steps` 中是否出现 `RETRIEVE` 命中片段 / `TOOL_CALL` 自动归类，
  而不是靠"我挑了一道没有 RAG 的题"——后者无法保证，会让分类失真。
- 每条记录：端到端耗时 + 轨迹各步 `latencyMs`（`RETRIEVE` / `LLM_CALL` / `TOOL_CALL` / `PERSIST`）
  + token + 成功/降级。
- **明令禁止的做法**：用"带 Trace 的总耗时 − 不带 Trace 的总耗时"来推算 Trace 开销。
  理由：LLM 调用方差远大于本地持久化开销；且生产路径不存在"不带 Trace"的合法形态，
  为对照而改代码违反任务书第 7 条。两类数据必须**各自独立呈现**（A 类给本地开销，B 类给端到端与步耗时）。

#### 2A-4 是否需要生产代码改动

**不需要**，因此本阶段不触发"停止并等作者确认"。

依据：
- A 类直接调用现有 bean（`TraceRecorder` / `AiUsageRecorder` / `AiTokenBudgetGuard`），用 `System.nanoTime()` 外部计时；
- B 类的分步耗时 M18 已随轨迹落库（`ai_execution_trace.steps`），无需为观测新增埋点；
- 唯一牵涉的既有生产改动是前一执行会话的 `TraceRecorder` 脱敏修复（与性能无关，作者已接受）。

若 2B 实测发现某一步无法从外部区分（例如 provider 内部重试耗时），
**将停下单独向作者申请**，不擅自修改生产代码。

#### 2A-5 2B/2C/2D 的产物与精确路径

| 阶段 | 产物 |
| --- | --- |
| 2B | 扩展 `src/test/java/com/bitforum/ai/observation/M18PersistenceObservationTest.java`；原始样本落盘 `target/m18-observation/local-persistence-<时间戳>.txt` |
| 2C | 新增 `src/test/java/com/bitforum/ai/observation/M18EndToEndObservationTest.java`，用 `@EnabledIfEnvironmentVariable(named = "M18_E2E_OBSERVATION", matches = "true")` 默认关闭（避免 CI 误跑真实调用），配置手法复用 `src/test/java/com/bitforum/ai/agent/QaAgentFaultInjectionIntegrationTest.java`（`rabbitmq.listener.simple.auto-startup=false`、`spring.ai.retry.max-attempts=1`） |
| 2D | 报告 `docs/graduation/ai-agent-upgrade/ai-performance-observation.md` |

### 阶段 2B：实现并运行确定性本地开销观察

- 修改文件：`src/test/java/com/bitforum/ai/observation/M18PersistenceObservationTest.java`（扩展，非新增文件）
- **未改生产代码**；未调用任何模型；未引入性能平台（JMeter/K6/Prometheus 均在任务书禁项内）。
- 相对 2A 列出的三个问题，本轮的处理：
  1. 拆开了原先把 `start`+`finish` 混在一起的 `trace_insert`，改为 `trace_start`（仅 INSERT）与
     `trace_finish`（UPDATE + 用量 INSERT），并补 `usage_insert` 与 `local_overhead_total`；
  2. 预算查询补了"当日已有 50 行"的场景（用户 99092），与"空聚合"（用户 99091）对照；
  3. 原始样本与统计**落盘**：`target/m18-observation/local-persistence-20260920-165242.txt`
     （文件头含 Java/OS/MySQL 版本、两张表当前行数、写入线程、预热次数、样本量、场景说明；文件尾含每个场景的纳秒原始数组）。

#### 运行命令与结果（n=35，预热 5 次，同一 JVM / 同一 MySQL 容器）

```
./mvnw -s maven-settings.xml -Dtest=M18PersistenceObservationTest test
```

| 指标 | average | p50 | p95 | min | max |
| --- | --- | --- | --- | --- | --- |
| `budget_query_empty`（预算 SELECT，当日无用量） | **0.397 ms** | 0.376 | 0.462 | 0.333 | 0.746 |
| `budget_query_with_rows`（预算 SELECT，当日 50 行） | **0.400 ms** | 0.396 | 0.460 | 0.368 | 0.471 |
| `trace_start`（轨迹 INSERT） | **1.273 ms** | 1.244 | 1.603 | 1.020 | 1.811 |
| `trace_finish`（轨迹 UPDATE + 用量 INSERT） | **2.648 ms** | 2.513 | 3.633 | 2.144 | 4.380 |
| `usage_insert`（用量 INSERT） | **1.200 ms** | 1.168 | 1.533 | 0.979 | 1.608 |
| **`local_overhead_total`**（输入保护+预算+轨迹创建+3 步+收尾） | **3.942 ms** | 3.790 | 4.815 | 3.554 | 6.393 |

#### 观察结论（可由原始样本复算）

1. **一次对话的纯本地开销合计约 3.9 ms（P95 4.8 ms）**，其中轨迹创建 ~1.3 ms、收尾 ~2.6 ms、预算查询 ~0.4 ms。
   与真实 LLM 调用（既有实测为**秒级**，如推荐理由/洞察 1.6-5.0 s）相比，本地持久化占比 **< 1%**。
2. `trace_finish − usage_insert ≈ 1.45 ms` 可看作轨迹 UPDATE 的净代价
   （**这是允许的相减**：两者同类型、受控、无 LLM 噪声；而"用带/不带 Trace 的对话总耗时相减"被任务书与 2A 明确禁止）。
3. 预算查询在"空表"与"当日 50 行"之间**几乎无差**（0.397 vs 0.400 ms），
   说明 `idx_aius_user_date` 生效、行数增长对单次查询影响可忽略（本机数据量下）。

#### 本轮发现并处理的问题（重要）

- **发现前一执行会话遗留的 3 行未清理测试数据**（`user_id=99092`）：2 条 `result=DEGRADED` 的用量明细
  （07:06、07:08）与 1 条 `status=RUNNING`、`step_count=0` 的轨迹（07:00）——
  显然来自其故障注入测试，说明**该测试的清理不完整**（轨迹停在 RUNNING 也意味着收尾未执行）。
  本轮已按 traceId 精确删除，清理后 `user_id IN (99091,99092)` 的行数为 0（已复验）。
  → 建议在阶段 3C 一并核对故障注入测试的数据清理与测试用户隔离（见"发现但不在本轮处理的问题"）。
- 观察测试自身的数据清理已复验干净（按 traceId 删除轨迹与用量，并在 `@AfterEach` 清理线程上下文）。

#### 局限（报告引用时必须一并给出）

- 单机、单实例、串行、无并发压力；样本 35 个、预热 5 次；同一 JVM 与同一 MySQL 容器；
  数值受本机磁盘与 Docker 卷状态影响 —— **不代表并发能力或生产 QPS**。
- 写入均为**同步**（2A 已核实 AI 域无 `@Async`、且这些 Mapper 调用不在长事务内），
  因此"主线程等待时间"即完整耗时，不需要额外记录"异步完成时间"。
- 本阶段**未包含**真实模型耗时（属 2C）、未包含 RAG 的本地嵌入与向量检索耗时（属真实链路，2C 观测）。

### 阶段 2C：真实端到端观察（真实 DeepSeek 调用）

- 作者已明确批准费用；新增 `src/test/java/com/bitforum/ai/observation/M18EndToEndObservationTest.java`，
  由 `@EnabledIfEnvironmentVariable(named = "M18_E2E_OBSERVATION", matches = "true")` **默认关闭**（CI/全量回归不会误跑真实调用）。
- **未改生产代码**；只读 `.env` 取值到环境变量，未打印 Key / Authorization / JWT。
- 方法：每场景**预热 1 次 + 记录 5 次**（共 30 次真实调用，不额外加次数）；
  隔离测试用户（`m18_obs_*`，本次 id=1451）+ 每个样本新建干净会话；结束时按 id 精确清理。
- 排除干扰：仅在本测试上下文把 `bitforum.ai.budget.daily-token-limit` 调高到 1,000,000
  （18 次对话累计可能接近生产默认 200k，否则后半样本会被预算闸门截断成降级）；生产默认值未改。
- 固定输入：普通 QA「用一句话解释 Java 里的 happens-before 原则。」/ RAG「站内关于 Spring 循环依赖的讨论都说了什么？」/
  工具「站内现在最热门的 3 篇文章是哪些？给我标题和文章 id。」；推荐 `forArticleDetail(topN=5)`；洞察=管理员触发+轮询报告终态。
- 证据：`target/m18-observation/end-to-end-20260920-174724.txt`（样本明细 + 按场景汇总 + 固定输入与方法说明）；
  运行命令：`M18_E2E_OBSERVATION=true` + `DEEPSEEK_CHAT_ENABLED=true` + 真实 key 后
  `./mvnw -s maven-settings.xml -Dtest=M18EndToEndObservationTest test`（实测 70.78 s，BUILD SUCCESS）。

#### 端到端结果（ms；n=5，**P95 为小样本描述值**）

| 场景 | average | p50 | p95 | min | max | 平均 token | 轨迹状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `qa_plain`（通用提问） | **1207** | 1264 | 1380 | 984 | 1380 | 3,687 | 5/5 SUCCESS |
| `qa_rag`（站内检索问答） | **2218** | 2176 | 2706 | 1841 | 2706 | 3,941 | 5/5 SUCCESS |
| `qa_tool`（需工具查询） | **2063** | 2004 | 2357 | 1765 | 2357 | 7,382 | 5/5 SUCCESS |
| `recommend`（推荐理由） | **1518** | 1566 | 1685 | 1366 | 1685 | 837 | 5/5 SUCCESS |
| `insight`（运营洞察，异步） | **4055** | 3996 | 4597 | 3689 | 4597 | ~2,613 | 4/5 SUCCESS，1/5 轨迹仍 RUNNING（见下） |

各步耗时（轨迹 `steps` 汇总，同场景 5 次范围）：

| 步骤 | 实测区间 | 说明 |
| --- | --- | --- |
| `LLM_CALL` | 905–4,424 ms | **绝对主导项**，占端到端 85%–95% |
| `RETRIEVE`（本地 ONNX 嵌入 + Redis KNN） | **14–81 ms** | 本地嵌入零费用且很快，远小于模型调用 |
| `TOOL_CALL` | 17–26 ms（工具本身）+ 因多一轮模型调用使 `LLM_CALL` 翻倍 | `qa_tool` 的 token（7.4k）≈ `qa_plain`（3.7k）的 2 倍，就是这一轮循环的代价 |
| `PERSIST` | 未单列 | 本次输出只汇总了 LLM/RETRIEVE/TOOL 三步（改进项，见下） |

#### 结论

1. **本地持久化开销可忽略**：2B 测得的 3.9 ms 对应端到端占比 —— 最快场景 `qa_plain` 约 **0.32%**，
   洞察场景约 **0.10%**；端到端耗时由真实模型调用决定。
2. **RAG 检索本身很便宜**（14–81 ms，含本地嵌入与向量检索），说明"本地嵌入 + Redis Stack"这条路线在本机没有成为瓶颈。
3. **工具调用是成本与延迟的主要放大项**：token 翻倍、端到端 +~0.9 s（相对 `qa_plain`）。
4. **推荐链路比对话便宜**（~1.5 s / 837 tokens）：因为排序由 Java 完成，模型只写理由。
5. **洞察最慢（~4.1 s）但异步**：管理员触发后不阻塞任何用户请求，这与其"线程池 + 落库"的设计一致。

#### 本轮发现的问题（如实记录）

1. **报告状态与轨迹收尾之间存在毫秒级窗口**：`AiInsightGenerationService.generate` 先写报告
   （`insightService.complete`）再收尾轨迹（`traceRecorder.finish`），因此以"报告终态"为端到端边界时，
   可能读到仍为 `RUNNING` 的轨迹（本次 5 个洞察样本中出现了 1 次）。
   该样本的端到端耗时（3689 ms）依然有效，只是轨迹快照早于收尾 —— 报告引用时需说明这一点。
2. **"retrieve=true" 的语义比预期弱**：`QaAgent` **每轮都会先检索**，所以该标记只表示"执行过检索步骤"，
   不能表示"命中片段数 > 0"；本次也未把每步 `detail`（命中数）落盘，因此无法在报告中给出"命中片段数"分布。
   → 若后续需要，可扩展输出（会再产生真实调用费用，故本轮不做）。
3. **`PERSIST` 步耗时未单列**：输出只含 `LLM/RETRIEVE/TOOL` 三步汇总；如需补全，同样属于"扩展输出"而非必须。
4. **成本量级**：本次 30 次真实调用合计约 **9.0 万 token**（按输入 ¥2 / 输出 ¥8 每百万估算 ≈ **0.2 元**），
   另有 6 次预热调用未计入样本。

#### 局限（报告引用时必须一并给出）

- 每场景 **n=5**，P95 仅为小样本描述值，不能当作稳定分位数结论；
- 单机、单实例、串行、无并发压力；不含网络抖动与供应商侧排队；
- 数值受本机与 DeepSeek 服务端当时状态影响，**不代表生产 QPS、并发容量或 SLA**。

### 阶段 2D：生成性能观察报告

- 产物：`docs/graduation/ai-agent-upgrade/ai-performance-observation.md`（十节：目的与非目标 / 环境与方法 /
  场景与固定输入 / 结果 A 确定性本地开销 / 结果 B 真实端到端 / 交叉结论（本地占比）/ 未执行项 /
  局限与诚实声明 / 原始数据与复算 / 成本记录）。
- **只引用 2B、2C 的实际证据，未补造任何数字**；两组数据（本地持久化 vs 真实端到端）**分开呈现**，
  并在 §六 明确写出"不使用差值法"的两个理由。
- 完成后的三项检查（本轮实测）：

| 检查 | 方法 | 结果 |
| --- | --- | --- |
| 敏感信息 | 在报告中检索 `sk-` / JWT 前缀 `eyJ` / `Bearer <token>` / `password=` / `DEEPSEEK_API_KEY=<值>` | **无命中**（仅出现"从 `.env` 读入环境变量"的变量名示例，不含真实值） |
| 表格结构 | 逐行校验每张 Markdown 表格的 `|` 数量一致 | **无不一致行** |
| 数据来源一致性 | 把报告中的 10 个指标数字与两个证据文件逐项比对 | **全部一致**（2B 的 5 个指标 + 2C 的 5 个场景汇总） |
| 交叉引用 | 检查报告内 `见 §x` 指向 | 修正 1 处（洞察的 RUNNING 说明指向 §八.5，原写 §七） |

- 报告中的关键结论（供答辩/论文直接引用）：
  1. M18 新增的可观测性写入合计约 **3.9 ms**，占端到端 **0.10%–0.33%**；
  2. `LLM_CALL` 占端到端 **85%–95%**（905–4,424 ms）；
  3. `RETRIEVE`（本地 ONNX 嵌入 + Redis KNN）仅 **14–81 ms**；
  4. `qa_tool` 的 token 约为 `qa_plain` 的 **2 倍**（provider 内部多一轮模型调用）；
  5. `insight` 最慢（~4.1 s）但**异步**，不阻塞用户请求。
- 2D 结束时**未 commit / push / merge / tag**。

### 阶段 3A：故障注入实现调查（只出方案）

> 本阶段只读代码与现有测试；未改代码、未改 `.env`、未触发任何外部调用。

#### 3A-1 现状评估：前一执行会话已完成的 3B 工作（**结论：保留，不重写**）

已有 `src/test/java/com/bitforum/ai/agent/QaAgentFaultInjectionIntegrationTest.java`（组件级，77 行）：
通过 `@SpringBootTest(properties = …)` 把 `spring.ai.deepseek.base-url` 指向不可达回环端口 `http://127.0.0.1:9`、
占位 api-key、`spring.ai.retry.max-attempts=1`、关闭 Rabbit 监听，然后直接调 `QaAgent.execute`。

它已覆盖 3B 验收中的：

| 3B 验收项 | 现状 |
| --- | --- |
| 应用可以启动 | ✅（属性覆盖只在请求时生效，上下文正常启动） |
| AI 请求返回统一降级结构与文案 | ✅（`response.degraded()` + 等于 `TraceDegradeReason.userMessage(LLM_ERROR)`） |
| Trace 标记 degraded，并有安全、可理解的 reason | ✅（`status=DEGRADED`、`degradeReason=LLM_ERROR`、`message` 为统一文案） |
| 不把底层连接详情写进轨迹 | ✅（`TraceRecorder.degrade` 已改为只持久化统一文案，detail 仅 debug 日志） |
| 注入配置恢复后不影响正常环境 | ✅（属性只作用于该测试上下文） |

**缺口（本轮 3A 需要补的三项）**：

| 缺口 | 为什么重要 |
| --- | --- |
| ① 没有走 HTTP 层 | 无法证明"用户实际拿到的响应"符合约定：`Result<T>` 结构、HTTP 状态、响应体不含堆栈/连接细节 |
| ② 没有验证**非 AI 主链路仍正常** | 任务书 3B 明确要求"登录、文章列表或详情至少一条非 AI 主链路仍正常" |
| ③ 降级只覆盖 QA 组件，未覆盖"预算拦截"路径 | 属 3C 范围，见方案 B |

#### 3A-2 相关配置与真实类（方案依据）

| 关注点 | 真实位置 |
| --- | --- |
| 模型配置开关 | `spring.ai.deepseek.chat.enabled` / `api-key` / `base-url`（`src/test/resources/application.yml` 用 `${…:占位}` 回退；`AiConfig.chatClient` 由 `@ConditionalOnProperty` 控制） |
| 统一降级 | `AiDegradeGuard.classify/degrade`（超时→`LLM_TIMEOUT`，其余→`LLM_ERROR`）、`TraceDegradeReason`（原因码 + 统一文案） |
| 轨迹契约 | `TraceRecorder.start/step/degrade/finish`；`AiExecutionTrace.STATUS_DEGRADED`；`degrade(reason, detail)` 只把**统一文案**落库 |
| 预算 | `AiTokenBudgetGuard.check` / `checkInputLength`；阈值 `bitforum.ai.budget.*`（`enabled` / `daily-token-limit` / `daily-cost-limit` / `max-input-chars` / `max-output-tokens`） |
| 用量 | `AiUsageRecorder.record`（`result` 取 `SUCCESS/DEGRADED/FAILED`） |
| 主业务接口 | 文章：`GET /api/article/listAll`、`GET /api/article/page`、`GET /api/article/detail`；AI：`POST /api/ai/conversations/{conversationId}/messages` |
| HTTP 约定 | `ResultHttpStatusAdvice` 按 `Result.code` 设置 HTTP 状态 → **降级应仍是 HTTP 200 + `code=200`**（降级不是错误） |
| 鉴权 | `JwtUtil` + `LoginInterceptor`（拦截 `/api/ai/conversations/**`）；现有测试用 `@AutoConfigureMockMvc` + `@MockitoBean JwtUtil` + `when(jwtUtil.getUserId(token))` |

#### 3A-3 方案 A：运行时模型不可用（补 HTTP 层证据）

- **注入方式**：`@SpringBootTest(properties = { "spring.ai.deepseek.chat.enabled=true",
  "spring.ai.deepseek.api-key=fault-injection-placeholder",
  "spring.ai.deepseek.base-url=http://127.0.0.1:9",
  "spring.ai.retry.max-attempts=1",
  "spring.rabbitmq.listener.simple.auto-startup=false" })`
  —— **不读取也不修改 `.env`**；不使用真实凭据；不访问外网（回环不可达端口）。
- **目标文件（新建）**：`src/test/java/com/bitforum/ai/controller/AiFaultInjectionHttpTest.java`
  （与组件级的 `QaAgentFaultInjectionIntegrationTest` 分工：一个测 Agent，一个测 HTTP 面向用户的契约）
- **断言清单**：
  1. 上下文正常启动（类能跑起来即证明）；
  2. **非 AI 主链路仍正常**：`GET /api/article/listAll`（或 `/page`）返回 HTTP 200 且 `data` 非空；
  3. AI 请求：`POST /api/ai/conversations/{id}/messages` 返回 **HTTP 200**、`Result.code=200`；
  4. 响应 `data.content` 等于 `TraceDegradeReason.userMessage(LLM_ERROR)`（统一友好文案）；
  5. 响应体**不含**底层细节：断言原始响应字符串不出现 `127.0.0.1`、`Connection refused`、`Exception`、`at com.bitforum`；
  6. 轨迹：该会话最新一条为 `STATUS_DEGRADED` 且 `degradeReason=LLM_ERROR`；
  7. 用量：产生一条 `result=DEGRADED`、`total_tokens=0` 的明细（**注意**：拦截/降级仍会记一条 0-token 行，用于审计，不是"不记录"）；
  8. 清理：按 traceId / conversationId 精确删除测试数据。
- **恢复方式**：属性仅作用于该测试上下文，运行结束即失效，**无需手工恢复**；
  Spring 会因 properties 不同而缓存出独立上下文，不影响其它测试。

#### 3A-4 方案 B：用户每日预算超限（补"可计数替身"证据）

- **注入方式**：`@SpringBootTest(properties = { "spring.ai.deepseek.chat.enabled=true",
  "spring.ai.deepseek.api-key=fault-injection-placeholder",
  "bitforum.ai.budget.daily-token-limit=100",
  "spring.rabbitmq.listener.simple.auto-startup=false" })`
  + **`@MockitoBean DeepSeekChatModel`**（可计数的模型替身；`DeepSeekChatModel` 是普通类，可 mock）。
- **目标文件（新建）**：`src/test/java/com/bitforum/ai/usage/AiBudgetEnforcementIntegrationTest.java`
- **断言清单**（用户专属 id，如 `99093`；当日用量直接插 `ai_usage_stat` 构造）：
  1. **边界-放行**：当日用量 = 阈值 − 1 → 请求进入模型链路（`verify(chatModel).call(any())` 至少一次）；
  2. **边界-拦截**：当日用量 = 阈值 → 拦截，**`verify(chatModel, never()).call(any(Prompt.class))`**（"调用次数不增加"的硬证据）；
  3. **越界-拦截**：当日用量 = 阈值 + 1 → 同样拦截；
  4. 返回统一降级文案（`TraceDegradeReason.userMessage(BUDGET_EXCEEDED)`）；
  5. 轨迹 `DEGRADED` + `degradeReason=BUDGET_EXCEEDED`（**没有被记成模型成功**）；
  6. **计费口径**：被拦截请求新增的用量行为 `total_tokens=0`、`result=DEGRADED`，
     且**不产生任何 `total_tokens>0` 的新行**（"没有重复计费"）；
  7. 单次输入/输出保护：输入超 `max-input-chars` 时同样在模型调用前拦截，文案为 `INPUT_TOO_LONG`；
  8. 隔离与清理：断言结束后该测试用户在 `ai_execution_trace` / `ai_usage_stat` 无残留（避免重演 2B 发现的遗留数据问题）。
- **恢复方式**：属性仅测试上下文；`@MockitoBean` 只替换该上下文的 bean；**生产默认阈值未改动**。

#### 3A-5 是否需要生产代码改动

**不需要**。两套方案都只使用"测试属性覆盖 + 测试替身 + 现有公开 API"，
不触碰生产路由、不改 `.env`、不新增依赖。若实施中发现必须改生产代码（例如某个降级路径无法从外部观测），
**将停下单独向作者申请**（任务书 3A 验收要求）。

#### 3A-6 复用清单（避免重复实现）

| 已有资产 | 处理 |
| --- | --- |
| `QaAgentFaultInjectionIntegrationTest`（组件级降级 + 轨迹契约） | **保留**，不重写 |
| `AiTokenBudgetGuardTest`（10 项：阈值/边界/输入保护/fail-open） | **保留**，作为 3C 的单测证据 |
| `AgentOrchestratorTest`（超预算与超长输入不调用 Agent、轨迹 `DEGRADED`） | **保留**，作为集成级证据 |
| `RecommendServiceTest`（超预算跳过理由生成，列表照常返回） | **保留** |
| 新增内容 | 仅两个缺口：**HTTP 层契约（方案 A）** 与 **可计数模型替身（方案 B）** |

### 阶段 3B：模型运行时不可用（HTTP 层故障注入）

- 新增文件：`src/test/java/com/bitforum/ai/controller/AiFaultInjectionHttpTest.java`（2 个用例）。
- **未改生产代码、未改 `.env`、未使用真实凭据、未访问外网**：全部注入靠本测试上下文的属性覆盖
  （`base-url=http://127.0.0.1:9` + 占位 api-key + `spring.ai.retry.max-attempts=1` + 关闭 Rabbit 监听）。
  应用**照常启动**，故障只发生在真正发起模型请求的那一刻。
- 组件级测试 `QaAgentFaultInjectionIntegrationTest` **保留未改动**（分工：它测 Agent 与轨迹，本类测 HTTP 响应契约）。

#### 对应任务书 3B 的七项验收

| 3B 验收项 | 证据 |
| --- | --- |
| 应用可以启动 | ✅ 上下文正常启动；注入只影响请求期属性，不影响 bean 装配 |
| 至少一条非 AI 主链路仍正常 | ✅ `GET /api/article/listAll` → HTTP 200 + `code=200` + `data` 为数组（公开文章接口） |
| 返回现有统一降级结构与文案 | ✅ `$.data.role=assistant`、`$.data.content` 恰等于 `TraceDegradeReason.userMessage(LLM_ERROR)` |
| HTTP 状态与 `Result<T>` 符合约定 | ✅ HTTP 200 + `$.code=200`（`ResultHttpStatusAdvice` 按 `Result.code` 映射状态；降级不是错误） |
| Trace 标记 degraded 且有安全 reason | ✅ `status=DEGRADED`、`degradeReason=LLM_ERROR`、`message` 为统一文案；**`steps` 也一并做了脱敏断言** |
| 响应体不含堆栈 / 密钥 / 底层连接详情 | ✅ `assertNoLeakedDetail` 检查 6 类关键词：`127.0.0.1`、`connection refused`、`Exception`、`at com.bitforum`、api-key 占位值、`bearer ` 回显 |
| 注入配置恢复后不影响正常环境 | ✅ 属性只作用于该测试上下文；**混跑证据**：`AiFaultInjectionHttpTest` + `QaAgentFaultInjectionIntegrationTest` + `AiControllerTest` + `AgentOrchestratorTest` 共 **24 项全部通过**（故障上下文与正常上下文并存互不影响） |

#### 运行结果

```
./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest test
  → Tests run: 2, Failures: 0, Errors: 0  （8.183 s，BUILD SUCCESS）
  → 日志：M18_FAULT_INJECTION 降级响应已脱敏：status=DEGRADED，reason=LLM_ERROR，usageTokens=0

./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest,QaAgentFaultInjectionIntegrationTest,AiControllerTest,AgentOrchestratorTest test
  → Tests run: 24, Failures: 0, Errors: 0  （BUILD SUCCESS）
```

#### 顺带确认的实现口径（为 3C 打样）

**降级请求仍会写一条 `total_tokens=0`、`result=DEGRADED` 的用量明细**（用于事后审计"今天降级了多少次"），
但**不产生任何费用**。本测试已就此断言（`usage.totalTokens=0` + `usage.result=DEGRADED`）——
所以 3C 的正确断言是"**不新增 `total_tokens>0` 的行**"，而不是"用量行数不变"。

#### 清理

测试用专属用户（id `99094`）+ 专属会话；`@AfterEach` 按 id 删除消息、会话、轨迹、用量与用户。
运行后复验：`user_info` / `ai_conversation` / `ai_execution_trace` / `ai_usage_stat` 四表对 `99094` **均为 0 行**。

#### 产物与凭据安全

- 新增 `src/test/java/com/bitforum/ai/controller/AiFaultInjectionHttpTest.java`；
- 未产生任何临时 Token / 凭据文件；响应体脱敏断言本身也在防回归（防止未来改动把连接详情带回用户可见响应）。

### 阶段 3C：预算超限与边界

- 新增文件：`src/test/java/com/bitforum/ai/usage/AiBudgetEnforcementIntegrationTest.java`（4 个用例）。
- **不调用真实 DeepSeek**：模型 bean 由 `@MockitoBean DeepSeekChatModel` 替换为**可计数替身**
  （`base-url` 同时指向不可达回环端口作双保险）；阈值通过属性覆盖为 **100 token**（便于构造 `=` 与 `>` 边界），
  输入上限覆盖为 **4000 字符**；生产默认值（20 万 token / 2 元 / 4000 字符 / 1024 输出）**未改动**。
- 未读未改 `.env`；未改生产代码；测试使用专属用户 id `99095` 与专属会话，`@AfterEach` 精确清理。

#### 对应任务书 3C 的七项断言

| 3C 要求 | 证据（均可复现） |
| --- | --- |
| 达到/超过阈值的边界行为与实现一致 | ✅ 用量 `99`（阈值−1）→ 放行；`100`（=阈值）→ 拦截；`101` → 拦截（实现用的是 `>=`） |
| **超限时 ChatModel 调用次数不增加** | ✅ `verify(deepSeekChatModel, never()).call(any(Prompt.class))` —— 超限请求**没有进入模型**，因此不产生任何模型费用 |
| 返回统一友好降级 | ✅ 返回内容恰等于 `TraceDegradeReason.userMessage(BUDGET_EXCEEDED)` |
| 被拦截请求没有被记录成模型成功 | ✅ 轨迹 `status=DEGRADED`、`degradeReason=BUDGET_EXCEEDED`（而非 SUCCESS） |
| 没有重复计费 | ✅ 断言"除预置行外，**不新增任何 `total_tokens > 0` 的用量行**" |
| 未超限用户仍会进入模型链路 | ✅ 阈值−1 用例 `verify(atLeastOnce()).call(...)`，轨迹 `SUCCESS` |
| 单次**输入**保护有边界测试 | ✅ 输入 = 4000 字符 → 放行；4001 字符 → 拦截（`INPUT_TOO_LONG`），且模型调用次数仍为 1 |
| 单次**输出**保护有边界测试 | ✅ **本轮补齐**：捕获传给模型的 `Prompt`，断言 `options.maxTokens = 1024`（即 `bitforum.ai.budget.max-output-tokens` 真的生效，而不只是"配置存在"） |
| 测试数据不污染普通用户 | ✅ 运行后复验：`user_info` / `ai_usage_stat` / `ai_execution_trace` / `ai_conversation` 对 `99095` **均为 0 行** |

#### 运行结果

```
-Dtest=AiBudgetEnforcementIntegrationTest
  → Tests run: 4, Failures: 0, Errors: 0（3.5 s，BUILD SUCCESS）
  → 日志：M18_BUDGET 边界通过：用量=99 阈值=100 结果=SUCCESS 输出上限=1024
          M18_BUDGET 边界通过：用量=100 阈值=100 拦截=BUDGET_EXCEEDED

-Dtest=AiBudgetEnforcementIntegrationTest,AiTokenBudgetGuardTest,AgentOrchestratorTest,RecommendServiceTest
  → Tests run: 36, Failures: 0, Errors: 0（BUILD SUCCESS）—— 3C 的完整证据集
```

#### 记录一条容易写错的口径（已在测试中固定）

被预算拦截的请求**仍会写一条 `total_tokens=0`、`result=DEGRADED` 的用量明细**（供审计"今天被拦了多少次"），
但**不产生任何计费行**。因此本测试断言的是"没有新增 `total_tokens > 0` 的行"，
预置的"今日已用"行用 `traceId` 前缀 `budget-seed-` 与本次新增区分开。

### 阶段 3D：生成故障注入报告

- 产物：`docs/graduation/ai-agent-upgrade/ai-fault-injection-report.md`（七节：目的与非目标 /
  故障 A 运行时模型不可用 / 故障 B 预算超限 / 恢复步骤 / 未执行项与局限 / 复现清单 / 结论）。
- **只引用 3B、3C 的实际运行输出**（通过数、耗时、日志行、用户 id、阈值），未补造截图或日志。
- 报告明确写出"脱敏响应的实际形态"，并**标注哪些字段是被测试断言锁定的、哪些未断言**（未断言的用 `…` 占位），
  避免把没验证过的字段当成已验证。
- 完成后的三项检查（本轮实测）：

| 检查 | 结果 |
| --- | --- |
| 敏感信息（`sk-` / JWT / `Bearer <token>` / `password=` / api-key 实值） | **无命中**（只有变量名与占位值示例） |
| Markdown 表格列数一致性 | **无不一致行** |
| 报告数字 vs 工作日志 | **一致**（2 / 24 / 4 / 36 项、8.183 s 与 3.531 s、两条 `M18_BUDGET` 日志、`M18_FAULT_INJECTION` 日志） |

- 报告如实列出的未执行项：**没有对运行中的本地服务做故障注入演示**（本轮按任务书优先补自动化测试）、
  未做真实供应商中断演练、未做前端浏览器层面的验证（脱敏在后端响应体断言）、
  未覆盖审核（MQ）与洞察（线程池）的链路级故障注入、不覆盖并发下的预算竞争、不是混沌工程结论。
- 3D 结束时**未 commit / push / merge / tag**。

### 阶段 4A：最终完整验证

> 命令映射（任务书给的是**参考命令**，本轮按仓库实际执行并在下表说明差异）：
> `mvn test` → `./mvnw -s maven-settings.xml test`（本机无全局 `mvn`，且需要一组环境变量）；
> `npm test -- --run` → `npm test`（`package.json` 的 `scripts.test` 本身已是 `vitest run`，`--run` 是多余参数）；
> `npm ci` → **未执行**（依赖已就绪，`npm ci` 会重装 `node_modules`，不影响本次验证结论）。

| # | 验证项 | 命令 | 退出码 | 结果 | 关键统计 | 证据位置 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 后端全量测试 | `./mvnw -s maven-settings.xml test` | **0** | 通过 | **Tests run: 416, Failures: 0, Errors: 0, Skipped: 10**（72 个测试类；跳过项为需真实 API Key 的调用测试） | `/tmp/m18-4a-backend.log` |
| 2 | 前端测试 | `cd frontend && npm test` | **0** | 通过 | **Tests 36 passed (36)** | `/tmp/m18-4a-fe-test.log` |
| 3 | 前端构建 | `cd frontend && npm run build` | **0** | 通过 | `✓ built in 786ms` | `/tmp/m18-4a-fe-build.log` |
| 4 | Compose 配置 | `docker compose config --quiet` | **0** | 通过 | 无输出（配置合法） | `/tmp/m18-4a-compose.log` |
| 5 | 导出脚本 | `./scripts/export-ai-context.sh --out /tmp/m18-4a-export.md <m18-status-summary>` | **0** | 通过 | 502 行；**易变指标告警 0 条**（说明内嵌文档已带日期口径） | `/tmp/m18-4a-export.log` |
| 6 | 格式检查 | `git diff --check` | **0** | 通过 | 无输出 | `/tmp/m18-4a-diffcheck.log` |
| 7 | 变更范围 | `git status --short` | — | 已核对 | 18 项：10 个修改（含 `TraceRecorder` 脱敏修复、10 份文档、1 个脚本）+ 8 个新增（2 份收口报告、worklog、任务书副本、4 个测试文件/目录） | 本文件 |
| 8 | 凭据扫描 | `git diff` + 逐文件 grep（`sk-` / `eyJ` / api-key 实值 / 数据库口令） | — | **无命中** | `.env` 已被 `.gitignore:47` 忽略（`git check-ignore` 验证） | 本文件 |
| 9 | 向量索引重建 | `M17_VECTOR_PROBE=true ./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test` | **0** | 通过 | `Tests run: 1, Failures: 0`；`bitforum-kb` `num_docs = 114`（全量测试会清空索引，故重建） | `/tmp/m18-4a-reindex.log` |

- 逐项证据（时间戳、命令、退出码、关键统计、证据路径）已同步落盘到 `.dev-logs/m18-4a-evidence.txt`
  （该目录已被 Git 忽略，不会进入提交）。
- 4A 结束时**未 push、未 merge、未打 tag**；本轮也未 commit（等作者确认提交拆分方式）。

### 阶段 4B：最终收口报告与交接

- 产物：`docs/graduation/ai-agent-upgrade/final-closure-report.md`（十节：最终核验 / 收口范围与执行结果 /
  文档不一致及处理 / 两份专题报告的关键结论 / 最终验证结果 / 未执行项 / **是否具备合并 `main` 的条件** /
  改动文件清单 / 风险与建议提交拆分 / 交接说明）。
- **最后一次文档同步**（本轮实测后完成）：
  - 4A 全量重跑得到 **416 项（406 通过 + 10 跳过）**，此前当前态文档写的是 407 项 →
    已把 **7 份文档的当前态基线同步为 416**（`progress.md` 只改"当前状态快照"与"总体进度 M18 行"两处，
    **M18-2/3/4 历史小节里的 388/394/407 保留不动**）；
  - 历史小节保留原值并在快照行标注"最近一次已验证基线：2026-09-20，收口阶段 4A 全量重跑"。
- 导出无冲突复验：页眉 `41 / 416 项 / 2026-09-20`，内嵌声明同值且带"截至 2026-09-20 快照"，
  stderr **0 条**易变指标告警。
- 报告质量检查（三项）：敏感信息**无命中**、Markdown 表格列数**无不一致行**、
  关键数字与实测一致（领先 41、索引 114、测试 416）；改动数 20 项与写作后 21 项的差异已在报告中注明。
- 合并条件结论：**技术维度已具备**（14 条检查清单逐条通过）；合并前需作者决定"先提交（建议 4 个提交拆分）
  与合并时机"。
- 4B 结束时**未 commit / push / merge / tag**。

### 阶段 4C：外部复查后的整改（2026-09-20）

任务书 14 个阶段收束后，作者把一份外部复查意见（针对报告自洽性与证据口径）带回，逐条整改：

| 复查意见 | 处理 | 证据 |
| --- | --- | --- |
| 工作区数量 20/21 自相矛盾 | `final-closure-report.md` §一/§八 改为「**以 `git status --short \| wc -l` 为准**」并说明按用途归并，不再硬编码易变数字 | 报告 §一、§八、§十一 |
| 「已全部 push、CI success」易误导 | 6 份文档 + 简报 + status summary 统一加限定：**仅指截至 `514f301` 的已提交内容**；本轮收口改动未提交、未过远端 CI | `grep "限 \`514f301\` 已提交内容"` |
| 简报第七节「下一步」把已完成的性能观察列为待办 | 改写为「收口三项已完成」，下一步改为：提交→推送→等 CI→合并→论文；并列出建议的后续补强 | `external-ai-briefing.md` §七 |
| 原始证据在被忽略目录、不可长期回溯 | **新增 `docs/graduation/ai-agent-upgrade/evidence/`**：2B/2C 脱敏原始样本 + 4A 验证流水 + `README.md`（来源、脱敏声明、复现命令、边界） | `evidence/`（4 个文件） |
| 10 个跳过项缺明细 | `final-closure-report.md` 新增 §5.1：按类列出跳过数与条件（9 项真实调用 + 1 项默认关闭的 E2E 观察） | 报告 §5.1 |
| Trace 脱敏修复的回归证据不清 | 报告 §四 点名**直接断言**：`TraceRecorderIntegrationTest` 传入 `http://internal.example` 后断言轨迹不含该串 | 报告 §四 |
| 费用阈值缺独立证据 | **新增链路级用例** `shouldBlockWhenDailyCostLimitReached`（token 未超、仅费用达上限 → 仍在模型调用前拦截）；连同单测共 5 项全绿 | `AiBudgetEnforcementIntegrationTest`（5 项通过） |
| 测试会清空共享向量索引 | 记入 `docs/technical-debt.md`（含影响与建议：独立索引名 / 独立 database），本轮不修 | `technical-debt.md` |
| 「AI 会不会拖慢论坛」证据不足 | **如实收窄**：性能报告新增 §八.5「本报告**不支持**的结论」，明确只支持"持久化不是瓶颈"，不支持"AI 不影响主流程"；并把并发隔离观察列为建议补强 | `ai-performance-observation.md` §八.5 |
| 「主流程正常」表述过宽 | `final-closure-report.md` §七 限定为"公开文章接口"，并指向 §六 的未覆盖链路 | 报告 §七 |

**整改后仍未处理（建议的后续补强，不阻塞合并）**：
① 主流程隔离观察（并发/慢模型下非 AI 接口 P95 与错误率）；
② 审核（MQ）与洞察（线程池）链路级故障注入。
→ 二者都已记入 `final-closure-report.md` §六/§九 与 `technical-debt.md`，引用时须保持"未验证"口径。

### 阶段 4D：主流程隔离观察（外部复查建议的补强，2026-09-20）

外部复查指出："性能观察回答了'可观测性自身开销多大'，但没有回答'AI 并发运行时会不会拖慢论坛主流程'"。
本轮补上这项观察（**不烧真实 token**：模型用固定延迟替身）。

- 新增文件：`src/test/java/com/bitforum/ai/observation/M18MainFlowIsolationTest.java`
  （`webEnvironment = RANDOM_PORT`，走**真实 Tomcat**；模型 bean 由 `@MockitoBean DeepSeekChatModel` 替换，
  每次调用固定延迟 **300 ms**；`base-url` 指向不可达回环端口作双保险）。
- 关键设置：Hikari 连接池 **10**（与生产默认一致，避免用测试专用的 2 连接得出失真结论）；
  预算上限调高以免被闸门截断；Rabbit 监听关闭。
- 观察方式：先测空载基线下 `GET /api/article/listAll` 20 次，再在 **8 个并发 AI 对话请求**
  （各 2 轮）持续运行的同时测同一接口 20 次。

| 指标 | 空载基线 | AI 负载中 | 变化 |
| --- | --- | --- | --- |
| P50 | 6 ms | 6 ms | 不变 |
| P95 | 9 ms | 12 ms | **+3 ms（比值 1.33）** |
| max | 71 ms | 23 ms | 更低（基线 max 属偶发抖动） |
| 非 2xx / 超时 | 0 | **0** | 可用性未受影响 |
| AI 侧失败 | — | **0**（16 次调用全部成功） | — |

- 断言口径：**只硬断言"主流程保持 2xx、无超时"**；延迟劣化倍数**只记录不断言**
  （并发观察在 CI/不同机器上抖动较大，强断言会变成 flaky 测试）。
- 证据落盘：`evidence/main-flow-isolation-20260920-220227.txt`（含基线与负载的原始毫秒样本）。
- 同步更新的文档：`ai-performance-observation.md` 新增 §七.5（并把 §八.5 中"不支持 AI 不拖慢主流程"
  改为"已由 §七.5 部分回答"）、`final-closure-report.md`（未执行项与整改表）、`docs/technical-debt.md`。
- 仍不覆盖：更高并发、真实供应商长尾延迟、后台审核/洞察同时运行时的资源占用 —— 已在三处文档中如实保留。

## 发现但不在本轮处理的问题

1. **两套日志并存**：前一会话把 09-20 的记录写进 `progress.md` / `findings.md` / `task_plan.md` 顶部，
   而任务书协议要求写入本文件。建议后续统一口径（保留本文件为唯一"当前阶段/下一步"来源，
   三份文档的顶部记录缩为一行指路），但**需作者确认**。
2. **生产代码未提交**：`TraceRecorder` 的脱敏修复是任务书认可的最小修复，但改动公共方法语义，
   其提交时机与拆分方式需作者决定（任务书禁止未经授权 commit）。
3. **任务书参考命令与本仓库实际不一致**：本机无全局 `mvn`（须用 `./mvnw -s maven-settings.xml` 且需一组环境变量）；
   `npm ci` 会重装依赖；**全量 `mvn test` 会清空 Redis 向量索引 `bitforum-kb`，跑完必须重建**。
   阶段 4A 必须按仓库实际命令执行（任务书亦要求"不得盲目照抄"）。
4. ~~上次导出的上下文包已过期~~ → **1B 已重新导出**（`.dev-logs/m18-status-context.md`，502 行）；
   阶段 1C 需再做一次完整核对（含 README / handoff / task plan）。
5. **故障注入测试的数据清理与用户隔离待核对**：前一执行会话的测试留下了 3 行未清理数据
   （含 1 条停在 `RUNNING` 的轨迹），本轮已清除；阶段 3C 需确认其清理逻辑与测试用户是否与其它观察测试隔离。
6. **`docs/technical-debt.md` 未审计**：任务书阶段 0 要求盘点，本轮已确认存在但未逐项比对其内容是否过期，建议 1B 一并扫一次。

## 中断恢复说明

- 上一步停在哪里：**阶段 4B / 4C 完成，阶段 4D（主流程隔离观察补强）完成**；
  5 个收口提交已 push（`514f301..79a2c7d`），4D 的改动待提交。**仍未 merge / tag。**
- 恢复后先检查：
  1. `git status --short`：应包含本日志、任务书副本、6 份文档修改、`scripts/export-ai-context.sh`，
     以及前一执行会话的 7 项改动（若清单变化，说明有其它会话在工作，先别动手）；
  2. `git rev-parse --short HEAD` 是否为 `514f301` 或更新；
  3. 本文件「当前状态」的"下一步"字段（当前为"等作者决策提交与合并"）。
- 不要重复执行：任务书 14 个阶段（0 / 1A / 1B / 1C / 2A-2D / 3A-3D / 4A / 4B）的全部工作 —— 结论已固化在本文件与三份报告（性能观察 / 故障注入 / 最终收口）中。特别注意：**2C 会再次产生真实 API 费用**、**全量测试会清空向量索引需重建**、**未经作者授权不要 commit / push / merge / tag**。
- 特别注意：1B 改动**尚未提交**，与前一执行会话的改动混在同一工作区；
  提交拆分方式需作者确认（任务书禁止未经授权 commit）。
