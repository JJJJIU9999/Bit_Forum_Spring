# AI 故障注入验证报告（M18 收口）

> **性质**：本报告只记录**实际执行过**的故障注入验证。所有命令、通过数与日志摘录均来自本机运行输出；
> **没有补造截图、日志或测试结果**。未执行的场景在 §五 明确列出。
>
> **证据来源**（均可复现）：
> - 组件级：`src/test/java/com/bitforum/ai/agent/QaAgentFaultInjectionIntegrationTest.java`（前序执行会话新增）
> - HTTP 级：`src/test/java/com/bitforum/ai/controller/AiFaultInjectionHttpTest.java`（阶段 3B 新增）
> - 预算边界：`src/test/java/com/bitforum/ai/usage/AiBudgetEnforcementIntegrationTest.java`（阶段 3C 新增）
> - 执行记录：`closure-worklog.md` 的「阶段 3B」「阶段 3C」两节
>
> 性能侧的观察另见 `ai-performance-observation.md`。

---

## 一、目的与非目标

**目的**：证明"AI 不可用"与"用户超额"两种故障下，系统仍然满足三条硬约束：

1. **故障只影响 AI 能力**：论坛主链路（文章列表/详情等）照常工作；
2. **用户拿到的是可理解的统一降级**，而不是异常、堆栈或底层连接细节；
3. **可观测性如实记录**：执行轨迹标记降级并给出可枚举原因；用量口径正确（不产生未发生的费用）。

**非目标（本轮不做）**

- 不测真实供应商中断（不拔网线、不改供应商配置）；
- 不做混沌工程式的持续注入、不做长时间故障演练；
- 不引入故障注入框架或新的依赖；
- 不为演示而修改生产路由或默认配置。

---

## 二、故障 A：运行时模型不可用

### 2.1 注入方法

应用**照常启动**，只把模型端点的运行期属性指向不可达的回环端口：

```java
@SpringBootTest(properties = {
        "spring.ai.deepseek.chat.enabled=true",
        "spring.ai.deepseek.api-key=fault-injection-placeholder",   // 占位，非真实凭据
        "spring.ai.deepseek.base-url=http://127.0.0.1:9",           // 不可达端口
        "spring.ai.retry.max-attempts=1",                           // 让失败快速返回，便于断言
        "spring.rabbitmq.listener.simple.auto-startup=false"        // 不消费测试消息
})
```

- **不读取、不修改 `.env`**；不使用真实 API Key；不访问外网。
- 这满足任务书的约束："不能通过把 API Key 设为空让应用启动失败来完成" —— 本方案里 bean 装配完全正常，
  故障只发生在真正发起模型请求的那一刻。
- 检索侧使用 `@MockitoBean RagService` 替身，确保失败**只**来自模型端点，避免"检索失败降级"混入断言。

### 2.2 运行命令与真实结果

```bash
./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest test
# Tests run: 2, Failures: 0, Errors: 0, Skipped: 0   (8.183 s, BUILD SUCCESS)
# 日志：M18_FAULT_INJECTION 降级响应已脱敏：status=DEGRADED，reason=LLM_ERROR，usageTokens=0
```

```bash
./mvnw -s maven-settings.xml \
  -Dtest=AiFaultInjectionHttpTest,QaAgentFaultInjectionIntegrationTest,AiControllerTest,AgentOrchestratorTest test
# Tests run: 24, Failures: 0, Errors: 0, Skipped: 0   (BUILD SUCCESS)
```

第二条命令是关键对照：**故障注入上下文与正常上下文（`AiControllerTest` / `AgentOrchestratorTest`）
在同一次运行中并存，两者互不影响** —— 这是"注入配置不会继续影响正常环境"的可复现证据。

### 2.3 断言清单与证据

| 任务书 3B 验收项 | 断言/证据 |
| --- | --- |
| 应用可以启动 | 上下文正常装配（注入只影响请求期属性） |
| **非 AI 主链路仍正常** | `GET /api/article/listAll` → HTTP 200、`code=200`、`data` 为数组 |
| 统一降级结构与文案 | `$.code=200`、`$.data.role=assistant`、`$.data.content` 恰等于 `TraceDegradeReason.userMessage(LLM_ERROR)` |
| HTTP 与 `Result<T>` 约定 | 降级仍是 **HTTP 200**（`ResultHttpStatusAdvice` 按 `Result.code` 映射；降级不是错误） |
| Trace 标记 degraded + 安全 reason | `status=DEGRADED`、`degradeReason=LLM_ERROR`、`message` 为统一文案；**`steps` 也做了脱敏断言** |
| 响应体不含堆栈/密钥/连接详情 | 对响应体与轨迹 `steps` 检查 6 类关键词：`127.0.0.1`、`connection refused`、`Exception`、`at com.bitforum`、api-key 占位值、`bearer ` 回显 |
| 配置恢复后无副作用 | 属性仅作用于该测试上下文；并由上面的 24 项混跑结果佐证 |

### 2.4 脱敏响应的实际形态

用户实际收到的响应形状（取值均为**测试断言锁定**的内容；未断言的字段以 `…` 表示）：

```json
{
  "code": 200,
  "message": "AI 回答生成成功",
  "data": {
    "role": "assistant",
    "content": "AI 服务暂时不可用，已使用降级结果，请稍后重试。",
    "citations": [],
    "…": "其余字段（id / totalTokens / latencyMs / createTime）由现有实现填充"
  }
}
```

说明：

- `content` 是 `TraceDegradeReason.LLM_ERROR` 对应的**全站统一文案**（与问答、审核、洞察、推荐共用同一套）；
- 响应中**没有**出现模型地址、连接错误文本、Java 异常名或堆栈帧；
- 轨迹侧同样只持久化统一文案，底层 `detail` 仅写 debug 日志（该行为是前序执行会话的最小修复，作者已确认接受）。

---

## 三、故障 B：用户每日预算超限

### 3.1 注入方法

```java
@SpringBootTest(properties = {
        "spring.ai.deepseek.chat.enabled=true",
        "spring.ai.deepseek.api-key=budget-enforcement-placeholder",
        "spring.ai.deepseek.base-url=http://127.0.0.1:9",       // 双保险：即使替身失效也不会访问外网
        "spring.ai.retry.max-attempts=1",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "bitforum.ai.budget.daily-token-limit=100",             // 小阈值，便于构造边界
        "bitforum.ai.budget.max-input-chars=4000"
})
```

- 模型 bean 由 **`@MockitoBean DeepSeekChatModel`（可计数替身）**替换 —— 这是"超限请求没有进入模型"的关键手段；
- 生产默认阈值（20 万 token / 2 元 / 4000 字符输入 / 1024 输出）**未改动**；
- "今日已用"通过直接插入 `ai_usage_stat` 行构造，`traceId` 以 `budget-seed-` 前缀与本次新增行区分。

### 3.2 运行命令与真实结果

```bash
./mvnw -s maven-settings.xml -Dtest=AiBudgetEnforcementIntegrationTest test
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0   (3.531 s, BUILD SUCCESS)
# 日志：M18_BUDGET 边界通过：用量=99 阈值=100 结果=SUCCESS 输出上限=1024
#       M18_BUDGET 边界通过：用量=100 阈值=100 拦截=BUDGET_EXCEEDED
```

```bash
./mvnw -s maven-settings.xml \
  -Dtest=AiBudgetEnforcementIntegrationTest,AiTokenBudgetGuardTest,AgentOrchestratorTest,RecommendServiceTest test
# Tests run: 36, Failures: 0, Errors: 0, Skipped: 0   (BUILD SUCCESS)
```

### 3.3 边界与拦截证据

| 场景 | 构造 | 结果 |
| --- | --- | --- |
| 低于阈值 | 当日已用 99 | **放行**：`verify(atLeastOnce()).call(...)`，轨迹 `SUCCESS` |
| 恰好等于阈值 | 当日已用 100 | **拦截**：`verify(never()).call(...)`，轨迹 `DEGRADED + BUDGET_EXCEEDED` |
| 超过阈值 | 当日已用 101 | **拦截**：同上 |
| 输入等于上限 | 4000 字符 | 放行（进入模型链路） |
| 输入超过上限 | 4001 字符 | **拦截**：`DEGRADED + INPUT_TOO_LONG`，模型调用次数不增加 |
| 输出保护 | 对话请求 | 捕获传给模型的 `Prompt`：`options.maxTokens = 1024`（配置真的生效） |

### 3.4 计费口径（一处容易被写错的地方）

被拦截的请求**仍会写一条 `total_tokens = 0`、`result = DEGRADED` 的用量明细** ——
这是 `TraceRecorder.finish` 的真实行为，用于事后审计"今天被拦了多少次"。
因此正确断言是"**不新增任何 `total_tokens > 0` 的行**"（无重复计费），而不是"没有新增记录"。
该口径已在测试中固定，并在 `closure-worklog.md` 中记录。

### 3.5 数据隔离

两类注入测试都使用**专属用户 id**（HTTP 级 `99094`、预算级 `99095`）与专属会话，
`@AfterEach` 按 id 精确清理。运行后复验：

| 表 | `99094` | `99095` |
| --- | --- | --- |
| `user_info` / `ai_conversation` / `ai_execution_trace` / `ai_usage_stat` | 全部 **0 行** | 全部 **0 行** |

---

## 四、恢复步骤

两套注入**都不需要手工恢复**，因为：

1. 所有覆盖都写在测试类的 `@SpringBootTest(properties = …)` 上，只作用于该测试的 Spring 上下文，
   进程结束即失效；Spring 会因属性不同缓存出独立上下文，不会污染其它测试；
2. 没有修改 `application.yml`、`.env` 或任何运行中的服务配置；
3. 测试数据由 `@AfterEach` 按 id 删除，并已通过 SQL 复验为 0 行。

若要手工复现或排查，只需按 §六 的命令分别运行对应测试类；
**不需要**重启 `./dev.sh` 起的服务，也**不会**影响正在运行的本地环境。

---

## 五、未执行项与局限（如实声明）

| 未执行 / 局限 | 说明 |
| --- | --- |
| **没有对"运行中的本地服务"做故障注入演示** | 本轮按任务书"优先补自动化集成测试"执行；未编写会改动运行中服务配置的演示脚本（避免触碰 `.env` 与生产配置） |
| 未做真实供应商中断/网络故障演练 | 注入的是"端点不可达"，不是真实供应商 5xx 或限流 |
| 未验证前端页面渲染 | 脱敏是在**后端响应体**层面断言的，没有浏览器截图或网络抓包证据 |
| 未覆盖审核（MQ）与洞察（线程池）链路的端到端降级 | 本轮覆盖的是 QA 对话链路（HTTP + 组件）与预算闸门；审核/洞察的降级由各自 Agent 内部的 `AiDegradeGuard` 覆盖，但未做链路级故障注入 |
| 不覆盖并发下的预算竞争 | 预算是 soft limit（判定基于已落库用量），并发场景下可能略微超限；这是 M18 决策时的已知边界 |
| 不是混沌工程结论 | 单机、单实例、按用例注入，**不代表生产故障恢复能力或 SLA** |

---

## 六、复现清单

```bash
# 前置：中间件已启动（./dev.sh 或 docker compose up -d mysql redis rabbitmq）
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_DATASOURCE_USERNAME="$(grep '^SPRING_DATASOURCE_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_DATASOURCE_PASSWORD="$(grep '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_USERNAME="$(grep '^SPRING_RABBITMQ_USERNAME=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export SPRING_RABBITMQ_PASSWORD="$(grep '^SPRING_RABBITMQ_PASSWORD=' .env | cut -d= -f2 | tr -d ' \r\n\t')"
export JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'

# 故障 A（组件级 + HTTP 级 + 正常上下文对照）
./mvnw -s maven-settings.xml -Dtest=QaAgentFaultInjectionIntegrationTest test
./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest test
./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest,AiControllerTest,AgentOrchestratorTest test

# 故障 B（预算边界与拦截）
./mvnw -s maven-settings.xml -Dtest=AiBudgetEnforcementIntegrationTest test
```

以上命令**都不需要真实 API Key**（占位 key + 不可达端点 + 模型替身），也不会产生任何模型费用。

---

## 七、结论

1. **故障确实只影响 AI 能力**：注入模型不可用时，公开文章接口照常返回数据；
2. **用户拿到的是统一、可理解的降级文案**，响应体与轨迹均不含地址、异常名或堆栈；
3. **降级与拦截都被如实记录**：轨迹 `DEGRADED` + 可枚举原因（`LLM_ERROR` / `BUDGET_EXCEEDED` / `INPUT_TOO_LONG`），
   用量按"是否真的花了 token"记 0 或实际值，**没有把被拦截的请求记成模型成功**；
4. **预算拦截发生在模型调用之前**（`never().call(...)` 为硬证据），且落地了单次输入与输出双重保护；
5. 上述结论都有可在无凭据环境下重复执行的测试命令支撑。
