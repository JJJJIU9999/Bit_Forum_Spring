# Bit Forum 项目完整历史总结

## 2026-08-24 公开仓库整改前基线收口更新

- Docker/Nginx、五服务 Compose、上传卷和 README 已由本地提交 `0c89922 feat(deploy): add Nginx frontend to Docker Compose` 收口。
- 文章详情间距修复已由本地提交 `d9d4f48 fix(frontend): correct article detail spacing` 单独收口。
- 本轮重新验证：Maven 176 个测试全部通过；Vitest 3 个文件、4 个测试全部通过；Vite 生产构建和 `docker compose config --quiet` 通过；可执行 JAR 能启动并在 8080 返回 Actuator 健康响应。
- RabbitMQ 5672 在本轮未启动，因此应用健康聚合状态为 `DOWN` 且测试有 AMQP 重连日志，但测试结果为 0 failure、0 error。
- Docker daemon 本轮未运行，五容器、首页、深层路由和代理的实时 E2E 未重新执行；2026-07-20 的历史记录仍保留在 `docs/graduation/docker-frontend/`。
- 个人学习资料不再作为项目主文档保留在当前公开树；历史内容仍可通过 Git 提交记录恢复。

下文保留 2026-08-02 核查快照，便于追溯当时的代码与工作区状态。其中“Docker 前端化尚未提交”“当前下一步先提交 Docker”等说法已被本节覆盖，不再代表 2026-08-24 的当前状态。

> 核查日期：2026-08-02
> 项目位置：仓库根目录
> 核查依据：当前 Git 工作区、Java/React 源码、Flyway 迁移、自动化测试、Docker 配置和 `docs/graduation` 模块记录。
> 使用说明：请用本文替换旧的 Bit Forum 记忆摘要。本文明确区分“已提交”“当前工作区已实现但未提交”“仍待完成”，不要把待办或风险写成已实现能力。

## 1. 项目定位

Bit Forum 是林坚浩持续维护的个人毕业设计项目，定位为一个后端能力较完整、带 React 管理与用户端界面的社区交流平台。项目最早聚焦注册登录、文章、评论、Redis 热榜和 RabbitMQ 事件，随后按 M1-M12 扩展为包含内容生产、审核发布、互动通知、举报治理、用户关系、运营后台、文件上传、接口文档、指标落库和健康检查的完整论坛系统。

项目覆盖以下 Java 后端工程能力：

- Spring Boot 分层开发、统一响应和全局异常处理。
- MyBatis-Plus 数据访问、分页和事务边界。
- JWT/BCrypt 认证、普通用户与管理员权限隔离。
- Redis String/Set/ZSet 数据结构及最终一致性同步。
- RabbitMQ Confirm/Returns、手动 ACK、DLX/DLQ 和幂等消费。
- Flyway 版本化数据库迁移。
- OpenAPI/Swagger 接口说明与 Actuator 可观测性。
- React 前后端联调、路由、响应式布局和 Docker Compose 部署。

项目不是高并发生产系统，也没有真实线上流量、性能指标或商业运营数据。描述项目时应强调“毕业设计与工程实践”，不要虚构生产规模。

## 2. 当前 Git 与交付状态

### 2.1 当前分支

- 当前分支：`feat/frontend-refactor`。
- 当前 HEAD：`bfbfcce feat(frontend): complete community UI refactor`。
- 当前分支已跟踪 `origin/feat/frontend-refactor`，该提交已经推送到远端。
- `graduation-design` 当前位于 `d4d4503 docs(graduation): summarize M7 through M12 progress`。

### 2.2 已提交的重要阶段

- `871f16f`：完成 M1-M6 与一轮后端审计修复。
- `31c5cfa`：完成用户资料、关注、文件上传和 OpenAPI 等 M7-M10 能力。
- `c6ceee9`：完成 M11 Redis 文章指标同步落库。
- `844ada0`：完成 M12 Actuator 与管理员聚合健康检查。
- `d4d4503`：汇总 M7-M12 毕业设计进度文档。
- `bfbfcce`：完成 React 社区端与管理端整体重构。

### 2.3 当前尚未提交的工作区内容

以下能力已经在当前工作区实现并验证，但不属于 `bfbfcce` 提交：

- React 前端 Docker 镜像：Node 22 构建、Nginx 1.27 运行。
- Nginx 对 `/api`、`/uploads` 的同源反向代理和 React Router 回退。
- Compose 增加 `frontend` 服务以及上传文件命名卷。
- MySQL 宿主机映射从 `3306` 调整为 `3307`。
- 文章详情页标题、正文内边距的少量 CSS 修正。
- 本次项目总结文档和 Docker 实施记录。

个人学习文档不属于项目交付范围，当前公开树只保留毕业设计、部署和工程说明。

## 3. 技术栈与版本

### 3.1 后端

| 类别 | 当前技术 |
| --- | --- |
| 语言与运行目标 | Java 17 |
| 核心框架 | Spring Boot 3.4.5 |
| 构建工具 | Maven |
| Web | Spring MVC |
| ORM | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8 |
| 迁移 | Flyway Core + Flyway MySQL |
| 缓存 | Spring Data Redis / Redis 7 |
| 消息队列 | Spring AMQP / RabbitMQ 3 |
| 认证 | JJWT 0.12.6 + BCrypt |
| 参数校验 | Jakarta Validation |
| 接口文档 | springdoc-openapi 2.8.9 |
| 健康检查 | Spring Boot Actuator |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、Mockito |

### 3.2 前端

| 类别 | 当前技术 |
| --- | --- |
| UI | React 19 |
| 构建 | Vite 7 |
| 路由 | React Router 7 |
| HTTP | Axios |
| 图标 | Lucide React |
| 测试 | Vitest、Testing Library、jsdom |
| 样式 | 原生 CSS、CSS Variables、响应式 Media Query |

前端没有使用 Redux/Zustand、TailwindCSS、Ant Design、Element、图表库、SSR 或远程字体。不要把这些技术写进已实现栈。

### 3.3 部署

- Docker、Docker Compose。
- 后端 Dockerfile：Maven 3.9.9 + Java 17 多阶段构建，Java 17 JRE 运行。
- 前端 Dockerfile：Node 22 构建，Nginx 1.27 托管静态资源。
- Compose 编排 MySQL、Redis、RabbitMQ、Spring Boot、React/Nginx 五个服务。

## 4. 后端架构概况

当前主代码共 98 个 Java 文件，主要目录如下：

```text
src/main/java/com/bitforum/
├── common/        统一 Result、热门文章返回对象
├── config/        MVC、JWT、MyBatis-Plus、RabbitMQ、OpenAPI 配置
├── controller/    18 个用户端与管理端控制器
├── dto/           请求与响应 DTO
├── entity/        9 个主要数据库实体
├── exception/     全局异常处理
├── interceptor/   登录与管理员拦截器
├── job/           Redis 指标同步定时任务
├── mapper/        MyBatis-Plus Mapper 与自定义查询
├── message/       RabbitMQ 消息对象
├── service/       14 个业务/基础设施服务
└── util/          JWT 工具
```

应用统一返回 `Result<T>`，Controller 负责 HTTP 参数与响应包装，Service 承担业务校验、状态流转和事务，Mapper 负责数据库访问。系统使用显式 MVC 拦截路径保护写接口和个人接口，管理员路径由独立拦截器统一保护。

## 5. 已实现功能全景

### 5.1 用户、认证与权限

- 用户注册和登录。
- BCrypt 密码哈希，数据库不保存明文密码。
- 登录成功返回 JWT、用户 ID、用户名和角色。
- JWT 通过 `Authorization: Bearer <token>` 传递。
- 登录拦截器解析 Token 后重新查询数据库，检查用户是否存在、账号是否启用。
- 管理员拦截器对 `/api/admin/**` 再检查 `ADMIN` 角色和账号状态，不只信任 Token 中的旧状态。
- 注册响应使用 `UserResponse`，不直接暴露用户实体或密码哈希。
- 用户禁用后无法继续访问登录保护接口。
- 管理员不能禁用当前登录的自己。

### 5.2 板块分类（M1）

- 公共端查询启用板块。
- 管理员分页查询板块。
- 板块创建、修改、启用、禁用和删除。
- 板块名称唯一。
- 文章发布、草稿保存和修改时校验板块存在且启用。
- 删除仍被文章引用的板块时有业务限制。

### 5.3 文章草稿、审核与生命周期（M2）

文章状态包括：

- `DRAFT`：草稿。
- `PENDING`：待审核。
- `PUBLISHED`：已发布。
- `REJECTED`：审核驳回。
- `OFFLINE`：已下架。

已实现：

- 直接投稿进入待审核。
- 保存草稿、修改草稿或驳回文章、再次提交审核。
- 作者查看自己的全状态文章并按状态筛选。
- 管理员查看审核列表、通过、驳回、下架和删除文章。
- 审核记录保存审核人、结果、原因和时间。
- 公共列表、详情、用户公开文章只展示 `PUBLISHED` 内容。
- 作者只能修改自己的草稿或驳回文章，不能直接修改待审核、已发布或下架内容。
- 作者删除与管理员删除会清理关联评论、收藏和 Redis 文章数据。

### 5.4 文章发现、浏览、点赞、热榜、收藏与搜索（M3 及基础能力）

- 最新文章列表和分页。
- 按板块筛选。
- 使用 MySQL `LIKE` 搜索标题和正文。
- 文章详情与浏览量累加。
- Redis Set 实现用户点赞去重，支持取消点赞。
- 禁止用户给自己的文章点赞。
- Redis ZSet 维护热度排行；浏览加 1 分、点赞加 3 分。
- 收藏与取消收藏。
- 数据库唯一键防止同一用户重复收藏同一文章。
- 我的收藏分页只返回仍处于公开状态的文章。

### 5.5 评论与通知中心（M4）

- 登录用户发表评论，评论前校验目标文章存在且已发布。
- 按文章查询评论。
- 管理员分页查询和删除评论。
- 通知中心支持分页、未读数、单条已读和全部已读。
- 已实现通知类型：评论、点赞、收藏、审核通过、审核驳回、文章下架。
- 对自己文章的自发行为会避免生成无意义的自通知。
- 前端导航展示未读通知徽标。

通知中心的业务通知主要通过 `NotificationService` 写入 MySQL；它与 RabbitMQ 的文章发布事件是两条不同链路，不能混为一谈。

### 5.6 举报与内容治理（M5）

- 用户举报文章和评论。
- 用户查看自己的举报及处理状态。
- 管理员按状态分页查看举报。
- 管理员处理或驳回举报并记录处理人、处理结果和时间。
- 举报状态包括 `PENDING`、`RESOLVED`、`REJECTED`。
- 数据库使用生成列和唯一键，阻止同一用户对同一目标重复提交未处理举报，避免只靠先查后插造成并发重复。
- 举报处理结果暂不额外生成通知，用户通过“我的举报”查看状态。

### 5.7 管理员数据看板（M6）

- 用户总数、正常/禁用用户等用户统计。
- 文章总量、各审核状态文章统计。
- 评论、收藏、通知和举报统计。
- 板块统计。
- 热门文章摘要。
- 前端管理看板集中展示运营数据。

### 5.8 用户资料与公开主页（M7）

- 当前用户读取和更新头像、昵称、个人简介。
- 用户公开主页。
- 公开主页展示已发布文章、文章数、收藏数和关注数据。
- 私有资料与公共资料使用不同响应 DTO，避免无边界暴露用户实体。

### 5.9 OpenAPI（M8）

- 配置 Swagger UI 和 OpenAPI JSON。
- 18 个控制器均有 `@Tag`。
- 当前识别到 63 个 `@Operation` 和 27 个 `@SecurityRequirement`。
- OpenAPI 配置声明 Bearer JWT 认证方式。
- 存在 Swagger UI/OpenAPI JSON 可访问性自动化测试。

### 5.10 关注关系（M9）

- 关注和取消关注。
- 禁止关注自己。
- 数据库唯一键防止重复关注。
- 粉丝列表、关注列表及分页。
- 关注数、粉丝数以及当前用户是否已关注。
- 公开用户主页集成关注操作和关系列表。

### 5.11 文件上传（M10）

- 头像上传：最大 2MB。
- 文章封面上传：最大 5MB。
- 允许 JPEG、PNG、WebP。
- UUID 生成服务端文件名，原始文件名仅清理后返回。
- 保存路径规范化并检查目标必须位于上传根目录内，降低路径穿越风险。
- `/uploads/**` 映射为静态资源。
- 文章实体和数据库支持 `cover_url`。
- 前端支持头像选择上传、投稿封面上传以及草稿/驳回文章封面修改。

### 5.12 Redis 指标落库（M11）

- Redis String 保存文章浏览量。
- Redis Set 保存点赞用户并计算点赞数。
- Redis ZSet 保存文章热度。
- 定时任务默认每 5 分钟扫描已发布文章。
- Redis 中存在浏览/点赞指标时，将其同步到 MySQL `article.view_count` 和 `like_count`。
- 单篇同步失败会记录日志并继续处理其他文章。
- Long 转 int 时处理负数和溢出边界。

该机制是周期性最终一致，不是强一致计数，也不应描述成绝对不丢数据。

### 5.13 Actuator 与管理员健康检查（M12）

- 引入 Spring Boot Actuator。
- 公开仅暴露 `health` 和 `info`。
- `/actuator/health` 不展示内部组件详细信息。
- 管理员专用 `/api/admin/health` 聚合检查应用、MySQL、Redis 和 RabbitMQ。
- 健康响应包含各组件状态、整体状态和检查时间。
- 管理员健康接口由后端管理员拦截器保护。

## 6. RabbitMQ 真实实现与边界

文章审核通过后，`ArticleService` 发送结构化 `ArticlePublishMessage`，消息包含 `messageId`、文章 ID、用户 ID、标题和发布时间。

当前可靠性机制包括：

- Direct Exchange 与明确 routing key。
- 持久化普通队列。
- Producer Confirm 记录是否到达 Broker。
- Returns + mandatory 记录是否成功路由。
- JSON 消息转换。
- 消费者手动 ACK。
- 失败时 `basicNack(requeue=false)`。
- DLX/DLQ 接收失败消息。
- Redis Set 根据 `messageId` 做重复消费判断。

必须保留的边界：

- 当前消费者业务主体仍是日志和模拟耗时处理，没有真正实现积分、站外推送或自动补偿。
- Confirm/Returns 失败当前主要记日志，没有 Outbox、发送失败表或自动重试调度。
- DLQ 监听器当前主要记录错误日志，没有管理页面或一键重放。
- 幂等标记在实际业务处理之前写入 Redis；如果后续处理失败，死信重放可能被判为重复并跳过。
- 因此只能描述为“实现 RabbitMQ 可靠性基础机制”，不能宣称 exactly-once、零丢失或完整生产级消息闭环。

## 7. 数据库与 Flyway

当前 `src/main/resources/db/migration` 有 V1-V11 共 11 个 SQL 文件，数据库 schema 当前版本为 11。

| 版本 | 内容 |
| --- | --- |
| V1 | 用户、文章、评论基础表 |
| V2 | 用户角色和账号状态 |
| V3 | 板块表及文章板块字段 |
| V4 | 文章状态、审核记录 |
| V5 | 文章收藏表 |
| V6 | 通知表 |
| V7 | 内容举报表 |
| V8 | 待处理举报并发唯一约束 |
| V9 | 用户昵称和个人简介 |
| V10 | 用户关注表 |
| V11 | 文章封面字段 |

主要实体/表共 9 组：

- `user_info`
- `article`
- `comment`
- `category`
- `article_audit_record`
- `article_favorite`
- `notification`
- `content_report`
- `user_follow`

迁移脚本主要使用唯一键和查询索引，没有定义数据库外键。关系完整性主要由 Service 业务校验和级联清理维护，这是当前架构取舍，同时也是后续审计应持续关注的风险。

## 8. React 前端现状

### 8.1 路由

公共路由：

- `/`：社区首页。
- `/articles/:articleId`：文章详情。
- `/users/:userId`：公开用户主页。
- `/login`、`/register`。
- 未知地址显示前端 404。

登录保护路由：

- `/publish`：投稿/草稿。
- `/notifications`：通知中心。
- `/me/profile`：个人资料。
- `/me/articles`：我的文章。
- `/me/favorites`：我的收藏。
- `/me/reports`：我的举报。

管理员路由：

- `/admin`：数据看板和健康状态。
- `/admin/audit`：文章审核。
- `/admin/articles`：文章管理。
- `/admin/comments`：评论管理。
- `/admin/reports`：举报处理。
- `/admin/users`：用户管理。
- `/admin/categories`：板块管理。

游客访问保护页面时会携带 `redirectTo` 跳转登录，登录成功后返回原页面。URL 能表达搜索、分页和标签状态，支持刷新、深链、前进与后退。

### 8.2 页面功能

- 首页：文章分页、板块筛选、标题/正文搜索、热榜、热门板块。
- 文章详情：封面、正文、浏览/点赞/收藏数据、评论、举报操作。
- 投稿页：板块选择、封面上传、保存草稿、提交审核。
- 个人中心：头像、昵称、简介、文章状态、收藏和举报记录。
- 用户主页：公开资料、已发布文章、关注/粉丝列表。
- 通知中心：未读徽标、分页、单条已读、全部已读。
- 管理后台：独立布局、分组导航、表格管理、确认弹窗和移动端抽屉。

### 8.3 UI 重构结果

- 视觉方向为暖色编辑社区。
- 使用纸张米白背景、暖白内容表面、深墨文字、森林绿主色和陶土色强调。
- 公共端桌面采用文章主栏加侧栏，移动端使用底部主导航。
- 管理端桌面使用深色固定侧栏，窄屏使用抽屉导航。
- 已增加跳过导航、可见焦点、分页语义、对话框语义、Escape 关闭、焦点圈定和焦点恢复。
- 响应式断点覆盖约 1000、760、390px；管理表格在自身容器中横向滚动，避免整页溢出。
- 没有引入 UI 组件库或全局状态库，保持当前项目规模下的最小复杂度。

### 8.4 前端边界

- JWT 和用户摘要存放在 `localStorage`，适合毕业设计演示，但存在 XSS 窃取风险。
- 前端根据本地角色控制管理入口只是体验层保护，真实权限始终由后端管理员拦截器校验。
- 评论目前显示“用户 #ID”，尚未补充作者昵称与头像。
- 当前没有富文本编辑器、暗色模式、国际化、实时 WebSocket 通知或 SSR。
- 前端自动化测试覆盖较少，完整页面流程仍需要人工回归或 E2E 测试。

## 9. Docker 与运行方式

### 9.1 当前 Compose 服务

| 服务 | 宿主机端口 | 持久化 |
| --- | --- | --- |
| React/Nginx | 80 | 镜像内静态文件 |
| Spring Boot | 8080 | `uploads-data` 上传卷 |
| MySQL | 3307 -> 容器 3306 | `mysql-data` |
| Redis | 6379 | 当前未配置独立持久化卷 |
| RabbitMQ | 5672、15672 | 当前未配置独立持久化卷 |

前端 Docker 访问地址是 `http://localhost`，不是 Vite 开发端口 `5173`。开发模式才使用 `npm run dev` 和 `http://localhost:5173`。

重建整个项目：

```powershell
docker compose up --build -d
```

只重建前端：

```powershell
docker compose up --build -d frontend
```

只重建后端：

```powershell
docker compose up --build -d app
```

普通停止使用 `docker compose down`。不要把 `docker compose down -v` 当作常规停止命令，因为 `-v` 会删除 MySQL 与上传数据卷。

### 9.2 Docker 当前边界

- `depends_on` 只保证启动顺序，当前没有为数据库、Redis、RabbitMQ 和应用配置 Compose healthcheck/健康依赖。
- Redis 和 RabbitMQ 没有单独的持久化卷。
- Docker 镜像构建阶段跳过 Maven 测试，正确流程应在构建/发布前单独运行测试。
- 根 README 仍有旧端口和旧前端启动说明，需要后续同步更新。

## 10. 自动化测试与 2026-08-02 实时验证

### 10.1 测试规模

- 后端：33 个 Java 测试文件，176 个测试方法。
- 前端：3 个 Vitest 文件、4 个用例。
- 后端测试覆盖 Controller、Service、Redis、RabbitMQ 消费、上传、关注集成、OpenAPI、Actuator、定时任务和权限规则。
- 前端测试覆盖认证角色存储、损坏本地数据清理、文章卡片插槽不重复、确认弹窗语义/Escape。

### 10.2 本轮实时结果

在 2026-08-02 当前工作区实际运行：

| 验证 | 结果 |
| --- | --- |
| `npm test` | 3 个测试文件、4 个测试全部通过 |
| `npm run build` | 通过，Vite 7.3.5 构建 1884 个模块 |
| `docker compose config --quiet` | 通过 |
| `mvn test` | 176 个测试全部通过，0 failure、0 error、0 skipped |
| Flyway | schema 当前版本 11，无需迁移 |

环境说明：

- 本轮测试时本机 MySQL 3306、Redis 6379可连接。
- RabbitMQ 5672 未启动，因此测试日志出现 AMQP 重连失败，但没有导致测试失败。
- Maven 本轮实际使用本机 Java 21 运行测试；项目编译目标与 Docker 运行环境仍为 Java 17。
- 本轮 `docker compose ps` 没有运行中的容器；只代表检查时容器未启动，不代表 Compose 配置失败。

## 11. 已完成的审计与修复

旧计划中“下一步先做全面检查，再重构前端”已经落后。仓库已经完成过一轮后端 P0/P1 审计修复和前端布局审查，随后完成了前端整体重构。

已落实的审计修复包括：

- 测试专用 JWT 配置，不给生产 JWT 密钥设置危险默认值。
- 登录保护接口拒绝禁用用户。
- 禁止直接修改待审核、已发布或下架文章。
- 数据库级阻止重复待处理举报。
- 禁止用户给自己的文章点赞。
- Controller 返回更新/删除业务错误信息。
- JWT 解析异常与账号状态异常分开处理。
- 修复管理表格操作列边框布局。
- 修复前端无 URL 路由、移动端布局不足、管理员入口角色识别、弹窗可访问性和文章卡片重复内容等问题。

因此，后续维护不应再把“全面检查”和“前端整体重构”当作尚未开始的首要任务。

## 12. 当前已知风险与未实现项

以下内容应被记为待办或架构边界，不能描述成已完成：

### 12.1 优先级较高

1. RabbitMQ 幂等标记写入时机可能导致失败消息重放被跳过，需要重新设计“处理成功后标记”或可恢复状态机。
2. RabbitMQ 发布失败目前主要日志记录，缺少 Outbox、发送失败持久化和自动补偿。
3. 统一错误包装与 HTTP 状态不完全一致，部分业务错误仍以 HTTP 200 返回。
4. 上传仅校验声明的 MIME 和大小，缺少文件魔数检测、图片解码验证、旧文件清理和安全扫描。
5. JWT 存储于 `localStorage`，生产级安全仍需评估 HttpOnly/Secure/SameSite Cookie 和 CSRF 方案。

### 12.2 工程与测试

6. 前端只有 4 个自动化测试，缺少主要页面集成测试和端到端测试。
7. 尚缺接口限流、登录防暴力破解、投稿/评论/举报频率限制。
8. 测试仍依赖本机 MySQL/Redis，隔离性不如 Testcontainers 或专用测试容器。
9. Compose 缺少 healthcheck 和基于健康状态的启动依赖。
10. Redis、RabbitMQ 在 Compose 中未配置持久化卷。
11. 根 README 的功能、端口和 Docker 前端说明已落后。

### 12.3 产品体验

12. 评论作者信息仍主要显示用户 ID，缺少昵称、头像和作者主页联动字段。
13. 没有富文本、图片正文、站内私信、实时通知、全文搜索引擎、对象存储、邮件/短信推送。
14. 没有生产流量、压测、监控告警、CI/CD、云部署或 SLA 证据。

## 13. 当前真正的下一步

建议按以下顺序继续：

1. 确认当前未提交的 Docker 前端化、文章详情 CSS 修正和文档是否作为同一批提交；提交时按路径暂存，保护两份学习文档。
2. 更新 README：补充 `http://localhost`、MySQL 3307、五服务 Compose、上传卷和禁止常规使用 `down -v`。
3. 对 360/375/768/1024/1440px 进行浏览器人工回归，覆盖游客、普通用户、管理员三种身份和深层路由刷新。
4. 优先修复 RabbitMQ 幂等/补偿与 HTTP 状态语义，再处理上传魔数校验和限流。
5. 增加少量高价值前端集成/E2E 测试，而不是机械堆叠组件单测。
6. 收口毕业设计答辩材料、架构图、数据库关系图和部署说明。

## 14. 项目描述时的真实性规则

可以明确说：

- 已实现完整文章审核状态机、通知中心、举报治理、关注关系、文件上传、OpenAPI、Redis 指标同步、Actuator 和 React 管理/用户端。
- 已实现 RabbitMQ Confirm/Returns、手动 ACK、DLQ 和 Redis 幂等基础机制。
- 已有 176 个后端测试方法，本轮完整测试通过。
- React 已完成整体路由与响应式视觉重构，并能通过 Docker/Nginx 部署；Docker 前端化当前仍未提交。

不能说：

- RabbitMQ exactly-once、消息绝对不丢或完整自动补偿。
- 系统已上线、有真实并发、真实用户量或生产性能数据。
- 已使用 Elasticsearch、WebSocket、Spring AI、TailwindCSS、Redux、微服务、Kubernetes、对象存储或 CI/CD。
- 前端已经有完整 E2E 测试或生产级认证安全。

## 15. 项目真实性摘要

下面保留一段当时的历史状态摘要：

> Bit Forum 是林坚浩持续维护的个人毕业设计项目。截至 2026-08-02，项目使用 Java 17、Spring Boot 3.4.5、MyBatis-Plus 3.5.9、MySQL 8、Flyway、Redis 7、RabbitMQ 3、JWT/BCrypt、OpenAPI、Actuator，以及 React 19 + Vite 7 + React Router 7 + Axios 前端。后端已完成注册登录、普通/管理员权限、板块分类、文章草稿-待审-发布-驳回-下架状态机、评论、点赞与热榜、收藏与搜索、通知中心、举报治理、管理员运营看板、用户资料、公开主页、关注关系、头像/封面上传、Redis 指标定时落库、Swagger/OpenAPI 和管理员聚合健康检查。数据库由 Flyway V1-V11 管理，共 9 个主要实体表。前端已从演示页重构为有正式 URL 路由、公共站点布局、个人中心、通知中心和独立管理后台的暖色响应式社区界面，并补充基础可访问性。当前分支为 `feat/frontend-refactor`，HEAD `bfbfcce` 已推送远端；M1-M12 和前端整体重构已提交。React 的 Node 22 + Nginx Docker 镜像、Compose 前端服务、上传卷和一处文章详情 CSS 修正已在工作区实现并验证，但截至该日期尚未提交。2026-08-02 实时验证结果为 Maven 176 个测试全部通过、Vitest 4 个测试通过、Vite 生产构建通过、Compose 配置校验通过。当前高优先级不是再次从头做前端重构，而是收口 Docker/README/人工回归，并处理 RabbitMQ 幂等与补偿、HTTP 错误状态、上传真实内容校验、限流、测试隔离和前端测试覆盖。描述项目时必须区分已实现与待办；不得宣称 exactly-once、绝对不丢消息、生产流量、Elasticsearch、WebSocket、Spring AI、TailwindCSS、微服务、Kubernetes、对象存储或完整 CI/CD。
