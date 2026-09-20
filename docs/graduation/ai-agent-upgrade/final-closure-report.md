# M18 最终收口报告（DeepSeekHarness + DeepSeek Flash 断点续作版）

> 依据：`BitForum-DeepSeekHarness-Flash-finalization-handoff.md`（收口任务书）。
> 逐阶段的命令、退出码与证据见 `closure-worklog.md`；本报告是**收口结论与交接**，不重复各阶段的全部细节。
> **本报告本身也是尚未提交的工作区改动之一。**

---

## 一、最终核验

| 项 | 实测值 |
| --- | --- |
| 核验时间 | 2026-09-20（阶段 4B） |
| 分支 | `feat/ai-agent` |
| HEAD | `514f301 docs(graduation): refresh status summary with push and CI results (M18)` |
| 相对 `main` | 领先 **41** 个提交 |
| 相对 `origin/feat/ai-agent` | **0** —— 但仅指**截至 `514f301` 的已提交内容**：远端有、CI 有；**本轮收口改动尚未提交，因此尚未经过任何远端 CI** |
| 工作区 | **不干净**：收口改动全部未提交（按用途归并见 §八）。**精确路径数以 `git status --short | wc -l` 为准** —— 该数字会随收口动作变化，故本文不硬编码（写作快照 21 个路径；整改后见 §十一） |
| 向量索引 | `bitforum-kb` = **114** 个片段（4A 全量测试后已重建） |
| 上下文导出 | 页眉与内嵌声明**一致**（41 / 416 项 / 2026-09-20 快照口径），stderr 无易变指标告警 |

---

## 二、收口范围与执行结果

任务书本轮只覆盖三件事，**未新增任何业务功能、未加依赖、未改生产路由**：

| 阶段 | 内容 | 结果 |
| --- | --- | --- |
| 0 | 建立真实基线 + `closure-worklog.md` | 完成（任务书副本入库、Git/文件盘点、发现前序会话未清理的测试数据） |
| 1A | 文档一致性审计（只出清单） | 完成：11 处当前态不一致 + 根因定位（见 §三） |
| 1B | 修正文档与导出方式 | 完成：6 份文档 + 1 个脚本；**不删历史快照**，改用"标注日期 + 页眉声明实时来源" |
| 1C | 验证文档收口 | 完成：导出无冲突、链接存在、`git diff --check` 通过 |
| 2A | 性能实现调查（只出方案） | 完成：四条链路时序 + 事务边界结论（写入均同步、不在长事务） |
| 2B | 确定性本地开销观察 | 完成：6 个指标 × 35 样本，原始样本落盘 |
| 2C | 真实端到端观察（作者批准费用） | 完成：5 场景 × (1 预热 + 5) = 30 次真实调用，70.78 s，约 9.0 万 token |
| 2D | 性能观察报告 | 完成：`ai-performance-observation.md` |
| 3A | 故障注入调查（只出方案） | 完成：评估前序会话已有证据 → 只补两个缺口 |
| 3B | 运行时模型不可用（HTTP 层） | 完成：2 项用例 + 24 项混跑对照 |
| 3C | 预算超限与边界 | 完成：4 项用例（含输出上限硬证据）+ 36 项证据集 |
| 3D | 故障注入报告 | 完成：`ai-fault-injection-report.md` |
| 4A | 最终完整验证 | 完成：**9 项全部通过**（见 §五） |
| 4B | 最终收口报告（本文件） | 完成 |

---

## 三、文档不一致及处理结果

1A 审计共定位 **11 处当前态表述与实际不符**，根因是"**静态文档硬编码了易变指标，而导出页眉是动态生成的**"，
两者会被拼进同一份上下文包，造成"页眉新、内嵌旧"的误读。处理方式（作者确认的口径）：

- **不删除**历史快照（保留数值 + 补"截至 2026-09-20 快照；实时值以导出页眉或 `git` 命令为准"）；
- `scripts/export-ai-context.sh` 页眉新增"本页眉 5 项为实时值，冲突时以页眉为准"，并新增
  `warn_volatile_snapshots()`：对嵌入文档做**只提示不改写**的易变指标检查（写 stderr，不影响退出码）；
- 两处**确定性错误**已修正：`BitForum项目完整历史总结.md` 里技术栈的 `Redis 7` → `Redis Stack`、
  "push 检查点未做" → "push 已完成（`514f301`）/ 合并未做"；
- 4A 全量重跑后，**所有当前态文档的测试基线同步为 416 项**（历史小节里的 388/394/407 保留不动）。

---

## 四、两份收口报告与关键结论

### 4.1 性能观察（`ai-performance-observation.md`）

- **本地持久化开销 3.9 ms**（`local_overhead_total`，n=35，P95 4.8 ms），
  占端到端耗时 **0.10%–0.33%** —— 可观测性写入不是瓶颈；
- 端到端（n=5，P95 为小样本描述值）：`qa_plain` 1207 ms、`qa_rag` 2218 ms、`qa_tool` 2063 ms、
  `recommend` 1518 ms、`insight` 4055 ms（异步）；
- `LLM_CALL` 占端到端 **85%–95%**；`RETRIEVE`（本地 ONNX 嵌入 + Redis KNN）仅 **14–81 ms**；
- `qa_tool` 的 token 约为 `qa_plain` 的 2 倍，代价来自 provider 内部多一轮模型调用；
- 明确声明：**不使用"带/不带 Trace 相减"推算开销**；**本地观察不代表生产 QPS、并发容量或 SLA**。

### 4.2 故障注入（`ai-fault-injection-report.md`）

- 运行时模型不可用：**非 AI 主链路照常 200**；AI 请求仍是 HTTP 200 + 统一降级文案；
  响应体与轨迹都不含地址/异常名/堆栈（6 类关键词断言）；轨迹 `DEGRADED + LLM_ERROR`；
  **脱敏修复有直接回归断言**：`TraceRecorderIntegrationTest` 先把
  `provider connection reset: http://internal.example` 作为 detail 传给 `degrade`，
  再断言轨迹里**不出现** `internal.example` —— 即「底层 detail 不再落库」这件事本身被测试锁住；
- 预算超限：**拦截发生在模型调用之前**（`never().call(...)` 硬证据）；
  边界为 `>= `（99 放行 / 100 与 101 拦截）；统一文案 `BUDGET_EXCEEDED`；
  **不新增任何计费行**（仍写一条 0 token 的降级明细用于审计）；
- 单次输入（4000/4001 字符）与输出（`maxTokens=1024` 真的传给模型）双重保护均有断言；
- 两类注入都不需要手工恢复（仅测试上下文属性），测试用户数据零残留。

---

## 五、最终验证结果（阶段 4A，9 项全部通过）

| # | 验证项 | 命令 | 退出码 | 关键统计 |
| --- | --- | --- | --- | --- |
| 1 | 后端全量 | `./mvnw -s maven-settings.xml test` | 0 | **416 项：406 通过 + 10 跳过，0 失败**（72 个测试类） |
| 2 | 前端测试 | `cd frontend && npm test` | 0 | **36 项通过** |
| 3 | 前端构建 | `cd frontend && npm run build` | 0 | `✓ built in 786ms` |
| 4 | Compose 配置 | `docker compose config --quiet` | 0 | 无输出（合法） |
| 5 | 导出脚本 | `./scripts/export-ai-context.sh --out …` | 0 | 502 行，易变指标告警 **0** 条 |
| 6 | 格式检查 | `git diff --check` | 0 | 无输出 |
| 7 | 变更范围 | `git status --short` | — | 20 项（见 §八），无意外改动 |
| 8 | 凭据扫描 | `git diff` + 逐文件 grep | — | **无命中**；`.env` 已被 `.gitignore:47` 忽略 |
| 9 | 向量索引 | `M17_VECTOR_PROBE=true … RecommendVectorRecallProbe` | 0 | 1 项通过；`num_docs = 114` |

证据位置：`.dev-logs/m18-4a-evidence.txt`（逐项时间戳/命令/退出码/统计，该目录被 Git 忽略）。

### 5.1 十个条件跳过项的明细（回应「会不会有本该执行却被意外跳过」的疑问）

`mvn test` 报告的 `Skipped: 10` 全部来自**显式条件注解**，不是笼统的开关：

| 测试类 | 跳过数 | 跳过条件 | 类别 |
| --- | --- | --- | --- |
| `ai.agent.DeepSeekSmokeTest` | 1 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 |
| `ai.rag.RagQaSmokeTest` | 1 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 |
| `ai.recommend.RecommendReasonSmokeTest` | 2 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 |
| `ai.moderation.ModerationAgentSmokeTest` | 1 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 |
| `ai.moderation.ModerationEvaluationTest` | 1 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用（评测集） |
| `ai.moderation.ModerationAutoReviewIntegrationTest` | 2 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 + 自动放行开关 |
| `ai.analyst.AiInsightSmokeTest` | 1 | `DEEPSEEK_CHAT_ENABLED=true` | 真实 DeepSeek 调用 |
| `ai.observation.M18EndToEndObservationTest` | 1 | `M18_E2E_OBSERVATION=true` | **默认关闭的端到端观察**（避免 CI 产生费用） |

即 9 项属「需要真实 API Key 的调用测试」、1 项是本次新增的引导式观察测试；
**没有配置错误导致的意外跳过** —— 打开对应环境变量即可执行（真实调用类会消耗少量额度）。
另：预算维度（token / 费用）与输入输出保护的边界测试见 §四 与 §五 对应小节。

---

## 六、未执行项与原因

| 未执行 | 原因 |
| --- | --- |
| 对"运行中的本地服务"做故障注入演示 | 任务书要求优先补自动化集成测试；未编写会改动运行中服务配置的脚本（避免触碰 `.env` 与生产配置） |
| 真实供应商中断、限流、长时间故障演练 | 注入的是"端点不可达"，非真实 5xx/限流；也不属于本轮范围 |
| 前端浏览器层面的降级验证 | 脱敏在后端响应体层面断言，无浏览器截图/抓包证据 |
| 审核（MQ）与洞察（线程池）的链路级故障注入 | 这两条链路的降级由各自 Agent 内的 `AiDegradeGuard` 覆盖，但未做链路级注入 |
| ~~主流程隔离观察~~ | **已补**（阶段 4C 后追加）：`M18MainFlowIsolationTest` —— 8 并发慢模型（替身延迟 300 ms）下，主流程 P50 不变、P95 +3 ms、0 错误；见 `ai-performance-observation.md` §七.5 |
| 更高并发/吞吐压测、Chaos 式持续注入 | 任务书明确不做；JMeter/K6/Prometheus 等均在禁项内 |
| 更大数据量下的预算查询与轨迹表增长 | 2B 只对照"空表 vs 当日 50 行" |
| `docs/technical-debt.md` 内容级过期审查 | 仅确认文件存在（已记入工作日志） |
| `npm ci` | 依赖已就绪；`npm ci` 会重装 `node_modules`，不影响验证结论 |

---

## 七、是否具备合并 `main` 的条件

对照任务书 §六 的 14 条检查清单：

| 检查项 | 结果 |
| --- | --- |
| README / status summary / handoff / progress / task plan 当前状态一致 | ✅（1B 修正 + 4B 同步 416 项后复核） |
| `TokenBudgetGuard` 已完成且边界表述一致 | ✅（克制版：三件事做了、四件事明确不做） |
| 新导出的上下文包没有新旧 Git 快照冲突 | ✅（页眉与内嵌同为 41 / 416 / 2026-09-20，0 告警） |
| 性能报告只引用真实执行数据 | ✅（全部可回溯到 `target/m18-observation/` 原始样本） |
| 故障注入报告有可复现证据 | ✅（提供了不需要真实 Key 的复现命令） |
| 模型不可用时论坛主流程正常 | ✅ **就已验证范围而言**：3B 断言公开文章接口 `GET /api/article/listAll` 仍 200。未覆盖的链路见 §六（审核 MQ / 洞察线程池 / 前端浏览器层）—— 引用时不要扩大成「全站全链路已验证」 |
| 预算超限在模型调用前拦截 | ✅（3C 的 `never().call(...)` 硬证据） |
| 后端测试结果已记录 | ✅ 416 项 |
| 前端测试与构建结果已记录 | ✅ 36 项 + 构建通过 |
| Docker Compose 配置检查已记录 | ✅ 退出码 0 |
| `git diff --check` 通过 | ✅ |
| Git 变更中没有 `.env`、密钥、JWT、密码或敏感日志 | ✅（扫描无命中，`.env` 被忽略） |
| 所有未执行项均明确写出、未冒充通过 | ✅（本报告 §六 + 两份报告的局限节） |
| 没有擅自新增业务功能或重构 | ✅（唯一生产代码改动是前序会话的脱敏修复，作者已确认接受） |

**结论：技术维度已具备合并 `main` 的条件。** 但合并前有两个动作需要作者决定：

1. **先提交这批收口改动**（当前 20 项全部未提交；建议拆分见 §九）；
2. **选择合并时机**：原计划为"系统冻结后一次性 `merge --no-ff`"；若希望公开仓库首页（`main`）
   立即展示完整的 M13-M18 能力，也可以现在合并——合并后仍可继续在 `feat/ai-agent` 上小改并再次合并。

---

## 八、改动文件清单（按用途归并；精确路径数以 `git status --short` 为准）

**修改（13）**

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/bitforum/ai/trace/TraceRecorder.java` | **唯一的生产代码改动**：轨迹只持久化统一降级文案，底层 detail 改 debug 日志（前序会话，作者已接受） |
| `src/test/java/com/bitforum/ai/trace/TraceRecorderIntegrationTest.java` | 测试上下文隔离属性 |
| `scripts/export-ai-context.sh` | 页眉实时值声明 + 易变指标**只提示**检查；README 章节标题纠正 |
| `docs/technical-debt.md` | 更新「无生产负载与端到端基线」条目（补 M18 已做部分）；新增「测试与开发共用同一个 Redis 向量索引」 |
| `docs/graduation/ai-agent-upgrade/progress.md` | 执行记录（本轮各阶段）+ 当前态快照同步 |
| `docs/graduation/ai-agent-upgrade/findings.md` | 收口基线证据 |
| `docs/graduation/ai-agent-upgrade/task_plan.md` | 收口计划与状态 |
| `docs/graduation/ai-agent-upgrade/m18-status-summary.md` | 当前态口径（含快照日期标注） |
| `docs/graduation/ai-agent-upgrade/external-ai-briefing.md` | 进度/规模/测试基线同步 |
| `docs/graduation/毕业设计文档总览.md` | 顶层总览同步 |
| `docs/graduation/毕业设计进度.md` | 顶层进度同步 |
| `docs/graduation/BitForum项目完整历史总结.md` | 新增第 16 节（M13-M18）+ 两处确定性错误修正 |

**新增（10 组 / 11 个文件）**

| 文件 | 说明 |
| --- | --- |
| `BitForum-DeepSeekHarness-Flash-finalization-handoff.md` | 任务书副本（入库以便断点续作；不含任何凭据） |
| `docs/graduation/ai-agent-upgrade/closure-worklog.md` | 断点续作工作日志（阶段 0 → 4B） |
| `docs/graduation/ai-agent-upgrade/ai-performance-observation.md` | 性能观察报告 |
| `docs/graduation/ai-agent-upgrade/ai-fault-injection-report.md` | 故障注入报告 |
| `docs/graduation/ai-agent-upgrade/final-closure-report.md` | 本报告 |
| `docs/graduation/ai-agent-upgrade/evidence/` | **脱敏证据归档**（本地开销原始样本、端到端原始样本、4A 验证流水 + README） |
| `src/test/java/com/bitforum/ai/agent/QaAgentFaultInjectionIntegrationTest.java` | 组件级故障注入（前序会话） |
| `src/test/java/com/bitforum/ai/controller/AiFaultInjectionHttpTest.java` | HTTP 层故障注入（3B） |
| `src/test/java/com/bitforum/ai/usage/AiBudgetEnforcementIntegrationTest.java` | 预算边界与拦截（3C） |
| `src/test/java/com/bitforum/ai/observation/` | 本地开销观察 + 端到端观察（2B/2C，E2E 默认关闭） |

---

## 九、风险与建议的提交拆分

**风险**

1. **提交体积大**：20 项改动混在同一工作区，包含"生产代码修复 + 测试 + 文档"三类；
   建议按下面的拆分提交，避免一个巨型 commit 难以回溯。
2. **性能与故障注入的结论边界**：两份报告都已写明局限（单机串行、n=5 的 P95 为小样本描述值、
   不代表生产 QPS/SLA），**引用时不要摘掉这些限定**。
3. **`M18EndToEndObservationTest` 默认关闭**（`M18_E2E_OBSERVATION=true` 才跑），
   全量回归里它计为跳过项 —— 这是刻意的，避免 CI 产生真实费用。
4. **全量测试会清空向量索引**：跑完必须重建（4A 已重建为 114 片段）。

**建议的提交拆分（4 个，按依赖顺序）**

```text
1) fix(ai): stop persisting provider detail in AI execution trace
   └─ src/main/java/com/bitforum/ai/trace/TraceRecorder.java
      src/test/java/com/bitforum/ai/trace/TraceRecorderIntegrationTest.java

2) test(ai): add fault-injection and budget-boundary coverage
   └─ src/test/java/com/bitforum/ai/{agent/QaAgentFaultInjectionIntegrationTest,
                                      controller/AiFaultInjectionHttpTest,
                                      usage/AiBudgetEnforcementIntegrationTest}.java

3) test(ai): add deterministic and end-to-end observation harnesses
   └─ src/test/java/com/bitforum/ai/observation/

4) docs(graduation): close out M18 with performance and fault-injection reports
   └─ BitForum-DeepSeekHarness-Flash-finalization-handoff.md
      scripts/export-ai-context.sh
      docs/graduation/**（含 worklog 与三份报告）
```

提交后即可按作者的时机决定是否 `git checkout main && git merge --no-ff feat/ai-agent`。

---

## 十、交接说明（新会话如何继续）

1. 先读 `closure-worklog.md` 的「当前状态」：它给出分支、HEAD、工作区摘要与**下一步**；
2. 再按需读本报告（结论）与两份专题报告（性能 / 故障注入）；
3. **不要重跑阶段 2C**（会产生真实 API 费用）、不要重复执行 4A 的九项验证（结论已固化）；
4. 未完成的事项只剩"作者的提交与合并决策"，以及任务书 §六 之外的常规收尾
   （论文/答辩材料、`scripts/agent-tool-smoke.md`、`docs/technical-debt.md` 复审）。

---

## 十一、外部复查后的整改（2026-09-20）

收到一份外部复查意见（针对本报告的自洽性与证据口径），已逐条处理：

| 复查指出的问题 | 处理 |
| --- | --- |
| 工作区数量 20/21 自相矛盾 | **不再硬编码**：§一 与 §八 改为「以 `git status --short \| wc -l` 为准」并说明按用途归并；写作快照与整改后快照分别标注 |
| 「已全部 push、CI success」易误导 | §一 明确限定为「**截至 `514f301` 的已提交内容**」；本轮收口改动**未提交、未过远端 CI**（§九 已把它列为合并前动作） |
| 原始证据在被忽略目录，不可长期回溯 | **新增 `evidence/` 目录**并归档三份脱敏原始证据 + `README.md`（含来源、脱敏声明、复现命令、边界说明） |
| 416 项里 10 个跳过项缺明细 | 新增 §5.1 明细表：按类列出跳过数、条件与类别（9 项真实调用 + 1 项默认关闭的观察） |
| Trace 脱敏修复的直接回归证据不清 | §四 点名 `TraceRecorderIntegrationTest` 的**直接断言**（传入 `http://internal.example` 后断言轨迹不含该串） |
| 费用阈值缺独立证据 | **新增链路级用例** `AiBudgetEnforcementIntegrationTest.shouldBlockWhenDailyCostLimitReached`（token 未超、仅费用达上限 → 同样在模型调用前拦截）；单测 `AiTokenBudgetGuardTest.shouldDenyWhenDailyCostLimitReached` 覆盖阈值判定 |
| 测试会清空共享的向量索引 | 记入 `docs/technical-debt.md`（含影响与建议：测试用独立索引名 / 独立 database），本轮不修 |
| 「AI 会不会拖慢论坛」证据不足 | **如实收窄**：现有结论只支持「M18 持久化不是 AI 请求的瓶颈」，**不支持**「AI 运行不会拖慢论坛」。并发隔离观察（用可控延迟的测试模型测非 AI 接口 P50/P95）列为**建议的后续补强**，见 §九 风险项 |
| 「主流程正常」表述过宽 | §七 该条已限定为「公开文章接口」，并指向 §六 的未覆盖链路 |

**仍未处理（属于建议的后续补强，非合并阻塞）**：
① ~~主流程隔离观察~~ → **已于同日补做**（`M18MainFlowIsolationTest`，结果见 §六 与性能报告 §七.5）；
② 审核（MQ）与洞察（线程池）的链路级故障注入 —— 若不补，引用时必须保持 §六 的口径。
