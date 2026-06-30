# M6 管理员数据统计看板调研记录

## 接手检查

- 当前分支：`graduation-design`。
- 最近提交：`2061d18 Merge branch 'phase-2-react-frontend'`。
- 当前工作区已有 M1-M5 未提交改动，必须保留。
- 当前已存在迁移：`V1__init_schema.sql` 至 `V7__add_content_report.sql`。
- 本轮优先不新增数据库迁移，使用现有表和 Redis 聚合统计。

## 已确认约束

- 本轮只做 M6，不提前做 M7、M8 或后续任务。
- 不修改历史 Flyway 脚本。
- 不引入 Spring Security、Spring Cloud、Kafka、复杂 RBAC、Redux、zustand、React Router、ECharts 或复杂 UI 组件库。
- 保持 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation 和现有 React/Vite 风格。
- 不改变 M2 文章审核/下架语义。
- 不改变 M5 举报处理只记录结果的语义。

## M1-M5 状态确认

- M1 已完成板块分类：`category` 表、`article.category_id`、公共板块、管理员板块管理、文章按板块筛选。
- M2 已完成文章状态流：`DRAFT`、`PENDING`、`PUBLISHED`、`REJECTED`、`OFFLINE`，公开列表、详情、浏览、点赞、热门均只处理 `PUBLISHED`。
- M3 已完成收藏与基础搜索：`article_favorite` 表、收藏/取消收藏/我的收藏、MySQL `LIKE` 搜索，收藏和搜索只处理 `PUBLISHED`。
- M4 已完成通知中心：`notification` 表、通知列表、未读数、单条已读、全部已读。
- M5 已完成举报与内容治理：`content_report` 表、用户举报、管理员处理，处理结果只记录，不自动下架或删除。

## 初步实现结论

- M6 不需要新增数据库表或 Flyway 迁移。
- 后端可新增独立 `AdminDashboardService`，通过现有 Mapper 实时 `selectCount` 聚合。
- Dashboard 返回结构使用 DTO，避免直接返回杂乱 `Map`。
- 管理员接口放在 `/api/admin/dashboard/summary`，复用现有 `AdminInterceptor`。
- 热门文章复用现有 `ArticleService.getHotArticles` 或同等逻辑；异常时返回空列表，避免 Redis 状态影响统计总览。
- 前端在现有 `AdminPanel.jsx` 增加页签和局部组件，不重写布局。

## 待调查代码

- `src/main/java/com/bitforum/controller/AdminArticleController.java`
- `src/main/java/com/bitforum/controller/AdminCommentController.java`
- `src/main/java/com/bitforum/controller/AdminReportController.java`
- `src/main/java/com/bitforum/controller/AdminCategoryController.java`
- `src/main/java/com/bitforum/controller/AdminUserController.java`
- `src/main/java/com/bitforum/service/ArticleService.java`
- `src/main/java/com/bitforum/service/CommentService.java`
- `src/main/java/com/bitforum/service/ContentReportService.java`
- `src/main/java/com/bitforum/service/NotificationService.java`
- `src/main/java/com/bitforum/service/RedisService.java`
- `src/main/java/com/bitforum/config/WebMvcConfig.java`
- `src/main/java/com/bitforum/interceptor/AdminInterceptor.java`
- `frontend/src/api/adminApi.js`
- `frontend/src/pages/AdminPanel.jsx`
- `frontend/src/style.css`
- `frontend/src/App.jsx`

## 最终实现记录

- 本轮没有新增数据库迁移，当前最新 Flyway 仍为 `V7__add_content_report.sql`。
- 新增 `AdminDashboardController`，接口为 `GET /api/admin/dashboard/summary`。
- 新增 `AdminDashboardService`，通过现有 Mapper 对 `user_info`、`article`、`comment`、`category`、`article_favorite`、`notification`、`content_report` 实时 `selectCount` 聚合。
- 新增 dashboard DTO：`AdminDashboardSummaryResponse`、`DashboardUserStats`、`DashboardArticleStats`、`DashboardCommentStats`、`DashboardCategoryStats`、`DashboardFavoriteStats`、`DashboardNotificationStats`、`DashboardReportStats`、`DashboardHotArticle`。
- 文章、评论、举报的 `todayCreated` 按当天零点起算。
- 文章、评论、举报的 `last7DaysCreated` 按当天和前 6 天组成的 7 个自然日窗口起算。
- 热门文章读取 Redis ZSet Top 10，并通过 `ArticleService.findPublishedById` 过滤为 `PUBLISHED` 文章。
- Redis 热门数据为空、Redis 异常、热榜成员格式异常或文章已下架时，不影响整体 dashboard summary 返回。
- 前端 `adminApi.js` 新增 `getAdminDashboardSummary`。
- 前端 `AdminPanel.jsx` 新增“数据看板”页签，默认进入看板，展示核心统计卡、状态分组和热门文章 Top 10。
- 前端 `style.css` 新增 dashboard 统计卡片、分组指标和热门文章列表样式，保持现有后台工作台风格。
- 本轮没有引入 React Router、Redux、zustand、ECharts 或 UI 组件库。

## 验证发现

- 聚焦测试 `mvn "-Dtest=AdminDashboardControllerTest,AdminDashboardServiceTest" test` 最终通过，6 个测试全部通过。
- 完整后端测试 `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` 通过，112 个测试全部通过。
- 后端编译 `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` 通过。
- 前端 `npm run build` 通过，Vite 构建 97 个模块。
- 聚焦测试中使用 `@MockitoBean RedisService` 覆盖 Redis 热门为空和热榜文章过滤场景，避免依赖真实 Redis 热榜数据。
