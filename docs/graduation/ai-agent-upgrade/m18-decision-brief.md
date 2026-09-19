# M18 决策简报：可观测性、工程化闭环（待确认）

> **状态：待确认（2026-09-19）。** 本文是**开工前**的材料，M18 尚未写任何业务代码。
> 技术可行性已用两轮前置验证确认（见第二节），**不需要再讨论"能不能做"**；
> 需要你拍板的是第三节的五个**口径与范围**选择。
>
> **用法**：先粘贴 `scripts/project-overview.sh` 生成的项目全景，再粘贴本文件，
> 然后让外部 AI 回答第三节的五个问题。
>
> 背景与范围收敛的原文见 `m18-handoff.md`；M17 的产品决定已定稿，本文不回退、不重议。

---

## 一、M18 要做什么（收敛版，已由 M17 收尾会议确定）

一句话：**不再加功能，把已有的四个 AI 能力收束成"全过程可解释、可追踪、可降级"。**

| 优先级 | 交付物 | 验收里对应的条款 |
| --- | --- | --- |
| 1 | `ai_execution_trace` + `TraceRecorder`：路由 → 工具调用链 → 每步耗时 → token | 验收 1、4 |
| 2 | `ai_usage_stat`：按 用户 / Agent / 天 聚合 token 与费用 | 验收 2 |
| 3 | `AiDegradeGuard`：LLM 超时/异常/额度 的统一降级 | 验收 3 |
| 4 | 前端「AI 执行轨迹」页 + 用量概览（**简单优先**） | 验收 4 |

**明确不做**（M17 收尾会议已砍）：`ai_prompt_template`、LLM-as-Judge 评估脚本、管理员「AI 设置」页。
**时间充足才做**：`TokenBudgetGuard`。

**与 M17 决定的关系**：推荐排序仍然完全由 Java 完成、模型只写理由；
M17 的 `0.8889` 不再优化（它的实验使命已结束）——
M18 对推荐链路做的是**把"为什么是这 10 篇"记下来并展示出来**，不碰排序算法。

---

## 二、开工前的前置验证结论（本次新增，两轮实测）

沿用 M13-M17 惯例：**先把没实测过的技术点验掉，再写业务代码**。
M18 的最大风险点是"轨迹要覆盖工具调用链与异步链路"，已用两个探针实测通过。

### 2.1 T12：工具调用链能不能埋点？（真实 DeepSeek 复验通过）

**为什么这是最大风险**：M18 要求轨迹里有「工具调用链 + **每步耗时**」，
而 M13-M17 从未采集过。若埋点位置选错（比如在 Agent 里解析最终响应），
会得到一个"看起来做了、实际永远是空"的功能，且不会有任何报错。

探针 `ToolCallTraceProbe`（stub 模型）+ `ToolCallTraceSmokeProbe`（真实调用）的实测结论：

| 问题 | 实测结论 |
| --- | --- |
| 工具循环发生在哪一层？ | **不在 `ChatClient`**，在 provider（`DeepSeekChatModel.call`）内部 |
| 最终 `ChatResponse` 里还有工具链吗？ | **没有**（`hasToolCalls()=false`），链只存在于中间轮次 |
| 传入的 `ToolCallback` 实例会被原样使用吗？ | **是**（options 中同一引用）→ 可以自己包一层装饰器 |
| 装饰器能拿到什么？ | 工具名、模型给的参数 JSON、真实返回值、**单步耗时** |

真实调用实测（一次提问触发两个工具调用）：

```text
工具=currentServerTime 入参={}                 结果="2026-09-19T21:30:00+08:00"                     单步耗时=3ms
工具=searchArticles     入参={"keyword": "Redis"} 结果=["Redis 缓存穿透与布隆过滤器", "Redis 分布式锁…"] 单步耗时=1ms
最终回答 = 服务器当前时间是 …，搜索关键词 Redis 返回了两篇文章，…
token: prompt=946 completion=110 total=1056   总耗时 1166 ms   最终响应 hasToolCalls()=false
```

**结论**：轨迹的工具链**必须靠包装 `ToolCallback` 采集**（不能解析最终响应）。
这意味着 `QaAgent` 的装配方式要从 `.tools(对象数组)` 改为
`ToolCallbacks.from(对象数组)` + 逐层包装 + `.toolCallbacks(包装后的数组)` ——
`ToolRegistry.toolsFor()` 的返回值形态可以保留，改动集中在装配处。

### 2.2 T13：轨迹能不能覆盖异步链路？（真实 RabbitMQ 验证通过）

AI 调用并不都发生在 HTTP 请求线程：**运营洞察**走单线程池，**知识库索引 / 内容审核**走 RabbitMQ。

| 验证项 | 实测结论 |
| --- | --- |
| MQ 用**消息头**透传 traceId | **可行**：发送端 `MessagePostProcessor` 写 header，消费者从 `Message` 读回 |
| 加 header 是否影响消息体反序列化 | **不影响**（`KbIndexMessage` 正常还原，`articleId` 一致） |
| 两个既有消费者是否需要改签名 | **不需要**：`KbIndexMessageListener` / `ModerationMessageListener` 已有 `Message rawMessage` 参数 |
| 线程池里 ThreadLocal 不包装会怎样 | **必然丢失**（实测 `null`）—— 所以必须显式传播 |
| 包装 Runnable 后 | 子线程可见，且任务结束后清理（避免线程复用串味） |

**结论**：traceId 的载体定为 "**ThreadLocal（请求线程）+ MQ header（跨进程）**"，
不修改任何既有**消息体**类，也不修改历史迁移。

### 2.3 数据取证：`ai_usage_stat` 的聚合口径缺口（查现库得到）

现有四张表里能拿到的"用量"信息并不齐：

| 来源表 | token | 耗时 | 归属用户 | 备注 |
| --- | --- | --- | --- | --- |
| `ai_message`（QA 对话） | ✅ | ✅ | ✅（经 conversation） | 现库 8 条助手消息，合计 41076 tokens |
| `ai_insight_report`（运营洞察） | ✅ | ✅ | ✅（requested_by） | 现库 1 条成功，2695 tokens / 5004 ms |
| `ai_recommend_log`（推荐） | ❌ | ✅ | ✅ | 现库 1074 条，其中 54 条生成了理由；**理由的 token 从未落库** |
| `ai_moderation_record`（审核） | ❌ | ✅ | ✅ | 表里根本没有 token 字段 |

**于是"按 用户 / Agent / 天 聚合 token 与费用"这件事，现有数据只能覆盖 2 个 Agent**
（QA、ANALYST）；MODERATION 与 RECOMMEND 的 token 缺口必须补采集 —— 这是 Q1 的由来。

---

## 三、需要你确认的五个问题

### Q1：`ai_usage_stat` 的数据来源怎么定？

- **方案 A（推荐）：新建统一埋点，`ai_usage_stat` 存"每次 LLM 调用的用量明细"**
  （一行 = 一次调用：时间/用户/Agent/模型/prompt+completion tokens/耗时/是否降级）。
  四个 Agent 都走同一处埋点，**顺带把审核与推荐的 token 补采上来**。
  优点：口径统一、能算单次成本、按 Agent 聚合名副其实；审核与推荐终于有 token 数据。
  代价：要动 4 个 Agent 的调用处（其中 `ModerationAgent` 用的是 `.call().entity(...)`，
  需改成能拿到 `ChatResponse` 的形态），回归面较大；写库失败必须只记日志、绝不影响主流程。
- **方案 B：不新增写路径，只从既有表聚合**（`ai_message` + `ai_insight_report`）。
  优点：零侵入。代价：审核与推荐的 token 永远缺失，"按 Agent 统计"是空的；
  而且将来每加一个 Agent 都要再改一次聚合 SQL。
- **方案 C：`ai_usage_stat` 只存"聚合结果"**，由定时任务或按需 SQL 刷新。
  代价：实时性差、要引入定时任务（与 M17"不做定时生成"的取向不一致），
  且仍需解决审核/推荐的 token 缺口。

### Q2：轨迹表用"单表 + 步骤 JSON"还是"主子两表"？

- **方案 A（推荐）：单表 `ai_execution_trace`，步骤明细存 JSON 字段**（`steps`）。
  一次调用写一行、异步链路回来时 `UPDATE` 同一行；前端取一行即可渲染整条时间线。
  优点：实现最简、演示效果一致、写入次数最少（异步链路尤其省事）。
  代价：不能按"某个工具"直接做 SQL 统计（需要 JSON 解析）。
- **方案 B：`ai_execution_trace` + `ai_execution_trace_step` 两表**。
  优点：可按步骤类型/工具名做统计与索引。代价：写入次数翻倍、异步链路要跨线程补写多行、
  前端要两次查询；对"轨迹页可演示"这个验收目标属于过度设计。

### Q3：`AiDegradeGuard` 要不要回头改造 4 个既有 Agent？

- **方案 A（推荐）：改**。把四个 Agent 各自的 `try/catch + 降级文案` 收敛到 Guard，
  由 Guard 统一产出"降级原因码 + 用户可读文案"，并顺带记录轨迹。
  理由：验收第 3 条要的是"**全站 AI 功能有统一的降级表现，且用户能看懂为什么降级**"；
  只在新增代码里用一个没人调用的 Guard，等于没做这件事。
  代价：改动 M13-M17 已定稿的 4 个 Agent，需要用既有 367 项测试做回归；
  四个 Agent 的返回值形态不同（`AgentResponse` / `ModerationOutcome` / `InsightOutcome` / `ReasonOutcome`），
  Guard 需要泛型设计。
- **方案 B：不改既有 Agent**，Guard 只作为新代码的入口 + 提供统一的原因码定义。
  优点：零回归风险。代价：验收第 3 条只能算"部分达成"，答辩时会被追问。

### Q4：Flyway 编号怎么排？

handoff 与 `task_plan.md` 的表格写的是 `V18 = ai_usage_stat`、`V19 = ai_execution_trace`，
但 M18 的**第一优先级是轨迹**（核心交付物），
先做 V19 会留下一个 V18 空号（Flyway 允许，但仓库里看起来像漏了一次迁移）。

- **方案 A（推荐）：`V18 = ai_execution_trace`、`V19 = ai_usage_stat`**，
  同时在 `task_plan.md` / handoff 里注明"编号修正：轨迹是核心，先占 V18"。
- **方案 B：严格按原表 `V18 = ai_usage_stat`、`V19 = ai_execution_trace`**，
  实现顺序仍是轨迹优先，只是仓库里先出现 V19。

两个方案都只新增迁移、不改历史迁移；差别只在编号与文档一致性。

### Q5：轨迹的"用户可见性"边界？

- **方案 A（推荐）：轨迹与用量都只在管理端**（`/admin/ai/traces`、`/admin/ai/usage`），
  普通用户看不到任何 token/费用信息，只保留现有的"AI 助手"体验。
  理由：用量与费用属于运营信息；轨迹里含用户 id 与提问原文。
- **方案 B：普通用户也能看到"自己这次提问的执行轨迹"**（脱敏后）。
  优点：可解释性对用户更直观。代价：要额外做脱敏与权限分支，演示价值有限。

---

## 四、不需要决策、按默认做法执行的几点（若有异议请一并说明）

1. **成本单价配置化**：`application.yml` 里配置每百万 token 的输入/输出单价（按模型区分），
   默认填 DeepSeek 的公开价格并注明"价格可能变动，以配置为准"；算出来的成本**标注为估算**。
2. **轨迹写入失败不影响主流程**：与 M16「`action_reason` 只是记录」、M17「推荐记录是副产品」同一原则，
   失败只记日志。
3. **推荐链路的确定性排序也记一条轨迹**（无 LLM 调用）：把三路召回 → RRF 融合 → Top-N 的选择过程
   记成步骤，专门展示"为什么是这 10 篇"。这是 M17 已定稿决定（Java 定序）的直接延伸，
   也是答辩时最有说服力的一屏。
4. **异步链路用同一 traceId 续写**：HTTP 请求产生 traceId → 消息头/线程池传递 →
   子链路 `UPDATE` 同一行（补上异步段的步骤与 token），而不是各写各的记录。
5. **`@MapperScan` 新包要同步注册**（`BitForumSpringApplication`）、
   新增需登录的接口要加进 `WebMvcConfig` 拦截列表 —— 两条 M17 踩过的坑，实现时按约束执行。

---

## 五、确认后的实施顺序（每个模块跑完 `mvn test` + `npm run build` 再开下一个）

1. **V18 `ai_execution_trace` + 轨迹埋点与展示（核心）**
   —— 含 `ToolCallback` 装饰器、异步链路续写、管理端轨迹 API；
2. **V19 `ai_usage_stat` + 用量聚合与成本估算**；
3. **`AiDegradeGuard` 统一降级**（改造 4 个 Agent，全量回归）；
4. **前端轨迹页 + 用量概览**（简单优先）；
5. 时间充足再做 `TokenBudgetGuard`。

每步本地提交（不 push），提交前做安全自检（有无真实 Key 片段）。
