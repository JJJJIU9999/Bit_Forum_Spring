# Java 实习面试通关计划

## 目标
结合岗位要求与 `spring_code/bit-forum-spring` 项目，制定一份能用于真实面试准备的通过计划：明确项目卖点、短板补强、八股范围、实战演示、面试话术和每日执行节奏。

## 岗位画像
- 标题：Java 开发实习生。
- 标签：Java、Docker、SpringCloud、MySQL、MyBatis、Redis、Spring、Python。
- 职责：需求分析、代码编写；解决安全和质量问题；参与联调、定位测试与生产问题。
- 要求：熟悉 Java、Spring Boot、MySQL/SQL；会用 AI 工具快速学习；沟通、责任感、抗压；27 届秋招提前批。

## 项目画像
- 主项目：`spring_code/bit-forum-spring`。
- 核心能力：Spring Boot 3、MyBatis-Plus、MySQL、JWT、BCrypt、Redis 浏览量/点赞/热榜、RabbitMQ 异步通知、Docker Compose、REST API。
- 学习叙事：从 JDBC 版到 Spring Boot 版，能讲清楚技术升级路径。

## 阶段
- [x] 读取岗位文本和项目既有调查结论
- [x] 核对 Spring Boot 项目 README、改进计划和依赖
- [x] 阅读关键业务代码，形成岗位-项目映射
- [x] 制定 14 天面试通过计划
- [x] 输出简历项目话术、Top 面试问题和每日安排
- [x] 将 `spring_code/bit-forum-spring/项目改善与提升计划.md` 改造成分模块执行手册
- [x] M1 安全基础：注册不返回密码、JWT 配置外置、异常信息不泄露
- [x] M2 数据一致性：文章存在校验、评论校验、删除文章清理评论和 Redis
- [x] M3 部署可信度：数据库初始化、Docker 多阶段构建、Docker Compose 启动验证
- [x] M4 自动化测试：补核心业务测试
- [x] M5 接口规范化：DTO、参数校验、HTTP 状态码
- [x] M6 性能与架构亮点：Redis ZSet 热榜、分页、RabbitMQ 可靠性
- [x] M7 项目收口与面试表达：README、演示流程、简历描述、面试讲稿、最终验收

## 当前执行上下文
- 用户希望自己敲代码，Codex 负责解释、拆步骤、review、验证；除非用户明确授权，不直接修改业务代码。
- 每个小任务完成后，需要给本次更改的关键代码补理解型注释，方便用户回看和复习。
- 当前项目改善文档已从“体检报告”改为“分模块执行手册”，M1 到 M6 已基本完成，当前进入 M7 项目收口与面试表达。
- M7 第一优先级是让项目“能被别人跑起来、能被自己讲清楚”：README、演示流程、简历描述、面试讲稿、最终验收。
- 已知工作区状态：`spring_code/bit-forum-spring/.gitignore` 存在既有改动；`项目改善与提升计划.md` 在子项目 Git 中显示为未跟踪文件。不要误改或清理无关状态。

## 成功标准
- 计划直接围绕这个岗位，而不是泛泛 Java 学习路线。
- 每个准备任务都能对应到面试中的证明材料。
- 明确哪些必须做、哪些可以不做。
- 最终答案包含可执行的日程和项目讲法。

## Errors Encountered

| 错误                                             | 尝试 | 处理                                               |
| ------------------------------------------------ | ---: | -------------------------------------------------- |
| `session-catchup.py` 默认 `.codex` 路径不存在    |    1 | 记录后继续；本轮直接读取已有规划文件和项目文件     |
| PowerShell 中复杂 `rg` 正则被拆成多个参数        |    1 | 改用单引号包裹模式并拆成两个简单查询               |

## 2026-06-09 状态
- M5 接口规范化主线已完成：用户、文章发布、文章更新、评论发布均已完成 Request/Response DTO 或参数校验改造，并通过 `mvn test`。
- M6 性能与架构亮点已完成：Redis ZSet 热榜、分页查询、RabbitMQ Confirm/Returns、结构化消息、Redis 幂等、手动 ACK、DLX/DLQ 和 DLQ 监听入口均已完成，并通过 `mvn test`。

## 2026-06-10 状态
- 进入 M7 项目收口与面试表达。
- M7-1 已重写 `spring_code/bit-forum-spring/README.md`：补充项目简介、技术栈、核心功能、Docker Compose 启动方式、本地 Maven 启动方式、接口验证流程、API 总览、RabbitMQ 链路、面试演示流程、常用排查命令和项目亮点总结。
- M7-2 已新增 `spring_code/bit-forum-spring/5分钟项目演示稿.md`：按开场、启动、注册登录、发布文章、Redis 热榜、评论一致性、RabbitMQ 可靠性、自动化测试和收尾总结组织，可用于面试现场演示。
- M7-3 已重写 `spring_code/bit-forum-spring/简历描述.md`：更新为当前项目真实能力，突出 DTO/参数校验、JWT/BCrypt、Redis String/Set/ZSet、RabbitMQ Confirm/Returns/手动 ACK/幂等/DLQ、Flyway、Docker Compose 和核心测试。
- M7-4 已更新 `spring_code/bit-forum-spring/面试八股问答清单.md`：新增“必背 12 问速记版”，覆盖项目介绍、登录认证、JWT、BCrypt、DTO 安全、数据一致性、Redis、RabbitMQ、Docker 和测试；同时修正旧的“继续优化”回答，避免把已完成事项说成待办。
- M7-5 已新增 `spring_code/bit-forum-spring/面试前最终验收清单.md`：整理环境检查、`mvn test`、Docker Compose 启动、核心接口演示、RabbitMQ/Redis 展示、常见失败排查和最终通过标准，并在 README 中增加入口链接。
- M7-6 已完成最终真实验收：运行 `mvn test` 通过，结果为 `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；运行 `docker compose config` 通过，Compose 能正确解析 app、mysql、redis、rabbitmq 服务、端口映射、环境变量和数据卷。
