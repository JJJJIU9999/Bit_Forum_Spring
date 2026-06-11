# 面试通关调查发现

## 岗位要求
- 岗位核心不是大厂级 Java，而是实习生能在指导下完成需求、写代码、联调、定位问题。
- 硬要求集中在 Java、Spring Boot、MySQL/SQL。
- 标签里的 Redis、Docker、MyBatis 与当前项目高度匹配。
- SpringCloud 和 Python 当前不是项目主线，面试时应诚实处理：SpringCloud 了解基础概念，Python 可用于脚本/接口调用，主栈是 Java。
- 岗位明确提到 AI 工具，这对用户是优势：可以结合 Claude Code、Cursor、Codex 的使用经历讲“快速学习和定位问题”。

## 项目匹配点
- `bit-forum-spring` 已覆盖 Spring Boot、MyBatis-Plus、MySQL、Redis、RabbitMQ、JWT、Docker Compose。
- README 中已有较完整的项目结构、API 列表和 curl 示例，适合面试前打磨成演示材料。
- 项目从线程池异步升级到 RabbitMQ，可以作为“质量、可靠性、解耦”的回答素材。
- Redis 用 String 做浏览量、Set 做点赞去重、热门排行计算，是最适合被追问的项目亮点。
- JWT + BCrypt 是安全相关的核心素材，但要讲准确：BCrypt 保护密码存储，JWT 做登录态，传输安全依赖 HTTPS。

## 当前短板
- 注册接口可能返回密码哈希，安全面试容易被追问。
- JWT 密钥和部分配置硬编码，`application.yml` 的 JWT 配置未被真正读取。
- 缺少数据库建表/迁移脚本，全新环境“一键启动”不够扎实。
- 业务测试很弱，目前只有 `contextLoads()`。
- Dockerfile 依赖已有 target JAR，不是真正全新环境一条命令构建。
- 缺少 SpringCloud 和 Python 项目证据，不能强装熟练。

## 面试策略
- 主线不要讲“我用了很多技术”，而要讲“我完成了一个可部署论坛，并围绕安全、缓存、异步、联调做过取舍”。
- 面试官问项目时，优先把话题引到登录认证、点赞/热榜、RabbitMQ、Docker 部署、问题排查。
- 通过这个岗位的关键不是继续堆 AI 或复杂新功能，而是把现有项目修到能演示、能解释、能经得住追问。

## 代码证据
- 注册接口当前返回 `Result<User>` 并把 `user` 放入响应：`UserController.java:26`、`UserController.java:31`。这是安全补强的第一优先级。
- 登录成功通过 `JwtUtil.generateToken` 生成 Token：`UserController.java:42`，可作为认证链路讲解入口。
- BCrypt 在注册和登录中直接使用：`UserService.java:36`、`UserService.java:47`，可以讲密码哈希和随机盐。
- JWT 密钥硬编码在 `JwtUtil.java:21`，这是配置外置的补强点。
- 浏览量和点赞接口分别在 `ArticleController.java:99`、`ArticleController.java:106`，但当前未先校验文章存在。
- Redis 浏览量实现入口在 `RedisService.java:17`，点赞用 Set，热榜 ZSet 方法存在但主流程没有真正使用。
- RabbitMQ 队列声明在 `RabbitMQConfig.java:14`，文章发布后发送消息在 `ArticleService.java:34`，消费者监听在 `NotificationListener.java:13`。
- Dockerfile 当前 `COPY target/...jar`：`Dockerfile:8`，不能证明全新环境一条命令构建。
- 全局异常处理返回 `e.getMessage()`：`GlobalExceptionHandler.java:21`，可被问到安全和质量问题。

## 计划结论
- 14 天内优先补：安全泄露、文章存在校验、数据库初始化脚本、Docker 构建、核心测试、项目演示。
- 不建议 14 天内主攻 SpringCloud；只准备注册中心、配置中心、服务调用、网关的基础概念即可。
- Python 只准备脚本能力：requests、json、文件读写，并用“主栈 Java，能快速补工具语言”作答。

## 2026-06-06 上下文维护
- `spring_code/bit-forum-spring/项目改善与提升计划.md` 已重写为分模块执行清单，模块为 M1 安全基础、M2 数据一致性、M3 部署可信度、M4 自动化测试、M5 接口规范化、M6 性能与架构亮点。
- 文档风格已按用户偏好调整：提供改法提示、涉及文件、自己敲代码步骤、验收标准和面试话术；不贴完整代码答案。
- 后续协作边界：用户希望自己敲项目代码，Codex 不应自作主张修改 Java、Docker、YAML 或测试文件；真正修改前需要用户明确授权。
- 下一步最适合处理 M1 安全基础中的“注册接口不返回密码哈希”，当前证据仍是 `UserController.register` 返回 `Result<User>`。
