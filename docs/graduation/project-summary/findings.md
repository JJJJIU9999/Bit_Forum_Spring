# Bit Forum 项目总结核查发现

## 当前状态

- 核查日期：2026-08-02。
- 当前分支：`feat/frontend-refactor`。
- 当前提交：`bfbfcce feat(frontend): complete community UI refactor`。
- 工作区已有 Docker、前端样式与学习文档改动，均视为用户现有工作并保持不动。
- 根 README 的基础能力描述仍偏早期，且 Docker 端口、前端容器化等部分已落后于当前工作区，不能作为唯一总结来源。

## 已确认技术基线

- Java 17、Spring Boot 3.4.5、Maven。
- MyBatis-Plus 3.5.9、MySQL、Flyway MySQL。
- Spring Data Redis、Spring AMQP/RabbitMQ。
- JJWT 0.12.6、BCrypt、Jakarta Validation。
- springdoc-openapi 2.8.9、Spring Boot Actuator。
- 后端测试基于 Spring Boot Test、JUnit 5、Mockito。
- 前端为 React + Vite；Docker 工作区改动已增加独立 Nginx 前端镜像。

## 后端模块与数据模型

- 当前共有 18 个控制器，覆盖普通用户、文章、评论、板块、收藏、通知、举报、用户资料、关注、上传以及管理员文章/板块/评论/用户/举报/仪表盘/健康检查。
- 文章工作流状态包括 `DRAFT`、`PENDING`、`PUBLISHED`、`REJECTED`、`OFFLINE`，支持草稿保存与更新、提交审核、审核通过/驳回、下架、作者修改/删除及管理员删除。
- 内容发现支持公开文章分页、板块筛选、标题/正文关键字搜索、文章详情、浏览、点赞/取消点赞、Redis 热榜和收藏列表。
- 通知类型明确包含评论、点赞、收藏、审核通过、审核驳回、文章下架；支持分页、未读数、单条已读和全部已读。
- 举报治理覆盖文章和评论，状态为 `PENDING`、`RESOLVED`、`REJECTED`；数据库通过生成列和唯一键阻止同一用户对同一目标重复提交未处理举报。
- 用户能力覆盖注册登录、当前资料读写、公开主页、公开文章、关注/取消关注、粉丝/关注列表和统计。
- 文件上传分头像（2MB）和文章封面（5MB），仅允许服务端定义的图片类型，文章表已增加 `cover_url`。
- 管理端包含用户启停、板块增删改启停、内容审核与下架、评论删除、举报处理、统计仪表盘和聚合健康检查。
- 数据库迁移目前为 V1-V11，共 9 个主要实体表：`user_info`、`article`、`comment`、`category`、`article_audit_record`、`article_favorite`、`notification`、`content_report`、`user_follow`。

## 基础设施与安全确认

- JWT 密钥和数据库/RabbitMQ 密码通过环境变量注入，登录密码使用 BCrypt；登录拦截器同时检查 Token、用户存在性与账号启用状态。
- `/api/admin/**` 由独立管理员拦截器检查登录、账号状态和 `ADMIN` 角色。
- 接口统一使用 `Result<T>`，存在参数校验、上传大小异常、未知路由和兜底异常处理。
- Actuator 仅暴露 `health`、`info`，并关闭公开详细组件信息；另有管理员专用聚合健康接口。
- Redis 用于浏览量、点赞用户集合、热度 ZSet 和 RabbitMQ 消费幂等；定时任务负责把文章浏览/点赞指标同步回 MySQL。
- RabbitMQ 已配置 Confirm、Returns、mandatory、手动 ACK、死信交换机/队列和消费幂等，但不能据此宣称 exactly-once 或绝对不丢消息。

## 需要如实保留的实现边界

- RabbitMQ 当前承载“文章审核通过后的发布事件”；消费者主体目前是日志与模拟耗时处理，并没有完整的积分、外部推送或自动补偿业务，不能描述成成熟的异步通知平台。
- 消息幂等标记在实际消费处理之前写入 Redis；若标记成功后业务失败，死信重放可能被当作重复消息跳过，这是后续全面检查应关注的一致性风险。
- `GlobalExceptionHandler` 返回业务失败包装，但多数异常响应没有同步设置对应 HTTP 4xx/5xx 状态；例如不存在的上传资源可能以 HTTP 200 携带 `code: 400`，接口语义仍可改进。
- 上传校验依据 MIME 类型、大小和路径规范化，尚未验证文件真实魔数，也没有云存储、病毒扫描、图片压缩和旧文件回收。
- 数据表主要依靠索引和业务代码维护关系，迁移中没有定义数据库外键；完整性更多依赖服务层。
- Redis 指标同步只扫描已发布文章，并以 Redis 当前值覆盖 MySQL；属于周期性最终一致，不是强一致或零丢失计数方案。
- RabbitMQ 发布确认当前主要记录日志，没有实现发送失败持久化、自动重试或 Outbox，因此不能宣称消息绝对可靠。

## 历史验证证据

- M1-M12 的模块文档均标为完成；M12 阶段曾验证完整 Maven 测试 176 个全部通过，前端生产构建通过。
- 前端整体重构文档记录：3 个 Vitest 文件、4 个测试通过；完整 Maven 测试 176 个通过。
- 审计修复阶段记录的后端全量验证曾达到 120 个测试通过；不同文档测试数量差异来自阶段与测试集合变化，不能简单相加。
- Docker 前端化记录显示 Node 22 + Nginx 镜像、React Router 回退、`/api` 与 `/uploads` 同源代理均实际启动验证通过。
- 上述为历史记录；本轮是否仍通过需单独运行当前工作区测试后再确认。

## 前端现状

- React 19 + React Router 7 + Axios + Vite 7，使用 Lucide 图标；没有引入 Redux、Tailwind、Ant Design 等额外状态或 UI 框架。
- 公共路由包括社区首页、文章详情、公开用户主页、登录、注册和 404；登录保护路由包括投稿、通知中心、个人中心。
- 管理端通过嵌套路由提供数据看板、文章审核、文章管理、评论管理、举报处理、用户管理和板块管理。
- 登录状态保存在 `localStorage`，Axios 请求拦截器统一附加 Bearer Token，响应拦截器统一拆解后端 `Result` 并转成页面错误。
- 前端路由会依据本地保存的 `role` 隐藏或阻止普通用户进入管理界面，后端管理员拦截器仍会再次做真实授权校验。
- 社区首页支持板块筛选、标题/正文搜索、分页、热门讨论和热门板块；文章详情支持浏览、点赞/取消、收藏/取消、评论以及文章/评论举报。
- 个人中心包含资料、我的文章、我的收藏、我的举报等标签；支持头像上传、昵称/简介维护、草稿/驳回文章的封面编辑和再次提交审核。
- 用户主页包含公开资料、已发布文章、关注/取消关注、粉丝与关注列表及分页。
- 通知中心包含未读徽标、分页、单条已读、全部已读和文章跳转。
- UI 已从单页演示重构为公共站点布局与独立管理后台布局；公共端含桌面导航和移动底栏，管理端含分组侧栏、移动抽屉、表格横向滚动和 1000/760/390px 响应式断点。
- 当前视觉系统以暖白画布、深绿色管理侧栏、陶土色强调和衬线标题为主，样式拆分为全局页面、布局和变量文件。

## 前端真实边界

- 当前 Vitest 只有 3 个测试文件、4 个用例，覆盖认证存储、文章卡片子内容和确认弹窗可访问性；主要页面与完整用户流程仍主要依赖人工验收和后端测试。
- JWT 放在 `localStorage`，适合毕业设计演示，但存在 XSS 窃取风险；生产环境更适合评估 HttpOnly/Secure/SameSite Cookie 与 CSRF 策略。
- 前端角色判断只用于导航和体验，不能作为安全边界；真实权限由后端拦截器提供。
- 评论列表当前直接显示“用户 #ID”，没有补充评论作者昵称/头像；这是体验层仍可完善的点。
- 当前前端没有复杂全局状态管理、SSR、国际化、富文本编辑器或实时 WebSocket 通知，不能把这些写成已实现能力。

## 工程规模与接口文档

- 当前主代码有 98 个 Java 文件，其中 18 个控制器、14 个服务、9 个实体；测试目录有 33 个 Java 测试文件和 176 个 `@Test`/`@ParameterizedTest` 方法。
- 前端有 29 个 JSX 文件、3 个 Vitest 文件；后端与前端测试规模明显不对称。
- 18 个控制器均有 OpenAPI `@Tag`，共识别 63 个 `@Operation` 和 27 个 `@SecurityRequirement` 标记；Swagger UI/OpenAPI JSON 已有自动化可访问性测试。
- 应用启用了 Spring Scheduling，文章指标同步任务默认初始延迟和固定间隔均为 5 分钟，可通过环境变量调整。

## Docker 与当前交付状态

- 已提交的后端 Dockerfile 使用 Maven 3.9.9 + Java 17 多阶段构建，运行阶段为 Java 17 JRE；镜像打包时跳过测试，测试需在构建前单独执行。
- 当前工作区已实现但尚未提交的 Docker 前端化：Node 22 构建 React，Nginx 1.27 托管静态文件，`/api` 和 `/uploads` 反向代理到 `app:8080`，React Router 深层路由回退到 `index.html`。
- Compose 当前工作区包含 MySQL 8、Redis 7、RabbitMQ 3 Management、Spring Boot、React/Nginx 五个服务；前端暴露 80，后端 8080，MySQL 宿主机端口为 3307，RabbitMQ 管理端为 15672。
- MySQL 使用 `mysql-data`，上传使用 `uploads-data` 命名卷；普通重建保留数据，`docker compose down -v` 会删除卷，不能用于常规停止。
- 当前分支 `feat/frontend-refactor` 已跟踪远端同名分支，HEAD 为 `bfbfcce`；M1-M12 与前端整体重构已提交，Docker 前端化和文章详情样式微调仍在工作区未提交。
- 当前还存在两份未跟踪学习文档，本次总结不修改它们。

## 2026-08-02 实时验证

- `npm test`：通过，3 个测试文件、4 个测试，0 失败。
- `npm run build`：通过，Vite 7.3.5 构建 1884 个模块；产物约为 CSS 30.91kB、JS 351.90kB（gzip 111.28kB）。
- `docker compose config --quiet`：通过，说明当前 Compose 语法和变量解析有效。
- `docker compose ps`：当前没有运行中的 Compose 容器；这是运行状态，不是配置失败。
- 本机端口检查：MySQL 3306、Redis 6379 可连接，RabbitMQ 5672 未监听。
- `mvn test`：通过，176 个测试，0 failure、0 error、0 skipped，`BUILD SUCCESS`；测试运行时使用本机 Java 21，但项目编译目标和 Docker 运行时仍为 Java 17。
- Maven 测试期间 Flyway 确认数据库 schema 当前版本为 11；RabbitMQ 未运行导致监听器重连日志，但不影响本轮测试通过。

## 记录原则

- 代码与迁移优先于旧记忆摘要。
- 测试存在不等于本轮已重新跑通；二者必须分开表述。
- 未提交改动单列，不混入当前提交已交付状态。
