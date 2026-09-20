# 收口证据归档（M18）

> 本目录保存 **脱敏后的原始观察数据与验证记录**，用于让收口结论**在仓库层面可回溯** ——
> 而不是只存在于某台机器的 `target/`（该目录被 Git 忽略，`mvn clean` 即消失）。
>
> 已扫描确认：本目录文件**不含** API Key、JWT、数据库口令、真实用户隐私数据。

## 文件清单

| 文件 | 来源 | 生成方式（可复现） |
| --- | --- | --- |
| `local-persistence-20260920-165242.txt` | 阶段 2B 确定性本地开销观察 | `./mvnw -s maven-settings.xml -Dtest=M18PersistenceObservationTest test` |
| `end-to-end-20260920-174724.txt` | 阶段 2C 真实端到端观察（30 次真实调用） | 见下"真实调用"命令（默认关闭，需显式开启并产生少量费用） |
| `final-verification-20260920.txt` | 阶段 4A 九项最终验证（命令 / 退出码 / 关键统计） | 见 `closure-worklog.md` 阶段 4A 与 `final-closure-report.md` §五 |

## 文件内容说明

两份观察证据的文件头都记录了：**Java / OS / MySQL 版本、相关表当前行数、预热次数、样本量、场景说明**；
正文是**每个场景的纳秒原始样本**与统计值，因此报告中的 average / P50 / P95 / min / max
都可以从这些原始样本复算（P95 采用 ceil 取整的百分位，与测试实现一致）。

`final-verification-20260920.txt` 是逐项验证的流水：时间戳、命令、退出码、关键统计与证据位置。

## 复现命令（不需要真实 API Key）

```bash
# 2B：确定性本地开销（不调用模型）
./mvnw -s maven-settings.xml -Dtest=M18PersistenceObservationTest test

# 3B / 3C：故障注入与预算边界（使用测试替身与不可达端点，不产生费用）
./mvnw -s maven-settings.xml -Dtest=AiFaultInjectionHttpTest test
./mvnw -s maven-settings.xml -Dtest=AiBudgetEnforcementIntegrationTest test

# 4A：全量回归（注意：会清空 Redis 向量索引，跑完需重建）
./mvnw -s maven-settings.xml test
export M17_VECTOR_PROBE=true && ./mvnw -s maven-settings.xml -Dtest=RecommendVectorRecallProbe test
```

**真实调用（会产生少量 API 费用，默认关闭）**：

```bash
export M18_E2E_OBSERVATION=true
export DEEPSEEK_CHAT_ENABLED=true
export DEEPSEEK_API_KEY="$(grep '^DEEPSEEK_API_KEY=' .env | cut -d= -f2-)"
./mvnw -s maven-settings.xml -Dtest=M18EndToEndObservationTest test
```

## 边界说明

- 这些数据是**本机、单实例、串行**观察的结果，**不代表并发能力、生产 QPS 或 SLA**；
- 端到端场景样本量为 **n=5**，其 P95 **只是小样本描述值**；
- 复现时数值会随机器、磁盘状态、网络与供应商侧状态变化，**结论应看量级与相对关系，而不是具体毫秒数**。
