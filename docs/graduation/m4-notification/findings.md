# M4 通知中心调查记录

## 接手检查

- 当前分支：`graduation-design`。
- 最近提交：`2061d18 Merge branch 'phase-2-react-frontend'`。
- 工作区状态：存在 M1、M2、M3 未提交改动，包含后端、前端、测试、迁移和 `docs/graduation/` 文档。
- `git diff --stat`：接手时 18 个已跟踪文件变更，约 `1768 insertions(+), 209 deletions(-)`，另有 M1、M2、M3 新增文件未跟踪。
- planning catchup 使用 `.agents` skill 路径执行，未输出额外未同步上下文。

## M1 状态确认

- 已新增 `category` 表。
- 已新增 `article.category_id`。
- 已有文章迁移到默认板块。
- `article.category_id` 最终为 `NOT NULL`。
- 文章发布必须选择启用板块。
- 公共文章分页支持 `categoryId` 筛选。
- 文章响应补充 `categoryName`。
- 管理员可以管理板块。

## M2 状态确认

- `article` 表已新增 `status`。
- 已有文章迁移后默认 `PUBLISHED`。
- 已新增 `article_audit_record` 表。
- 文章状态包括 `DRAFT`、`PENDING`、`PUBLISHED`、`REJECTED`、`OFFLINE`。
- `/api/article/publish` 保留，但语义已调整为提交审核，文章进入 `PENDING`。
- 管理员审核通过后文章变为 `PUBLISHED`，并发送 RabbitMQ 发布消息。
- 管理员驳回必须记录原因。
- 管理员可下架已发布文章，下架文章变为 `OFFLINE`。
- 公开列表、详情、浏览、点赞、热门文章只处理 `PUBLISHED`。
- 前端已有提交审核、保存草稿、我的文章、管理员审核和下架操作。

## M3 状态确认

- 已新增 `article_favorite` 表。
- 已新增 `ArticleFavorite` Entity 和 `ArticleFavoriteMapper`。
- `Article` 响应已补充 `favoriteCount`。
- 用户只能收藏 `PUBLISHED` 文章。
- 同一用户不能重复收藏同一篇文章。
- 用户可以取消自己的收藏。
- 我的收藏只返回当前用户收藏且仍为 `PUBLISHED` 的文章。
- 下架文章不会出现在普通我的收藏列表中。
- 已新增基础搜索接口，按 `title` / `content` 使用 MySQL `LIKE`。
- 搜索只返回 `PUBLISHED` 文章。
- 搜索支持 `categoryId` 筛选。
- 收藏、取消收藏、我的收藏需要登录。
- 普通公开搜索不需要登录。
- 前端已有搜索框、详情页收藏按钮、我的收藏列表。

## 待调查代码

- `src/main/java/com/bitforum/listener/NotificationListener.java`
- `src/main/java/com/bitforum/config/RabbitMQConfig.java`
- `src/main/java/com/bitforum/message/ArticlePublishMessage.java`
- `src/main/java/com/bitforum/service/CommentService.java`
- `src/main/java/com/bitforum/service/RedisService.java`
- `src/main/java/com/bitforum/service/ArticleService.java`
- `src/main/java/com/bitforum/controller/CommentController.java`
- `src/main/java/com/bitforum/controller/ArticleController.java`
- `src/main/java/com/bitforum/controller/AdminArticleController.java`
- `src/test/java/com/bitforum/service/ArticleServiceTest.java`
- `src/test/java/com/bitforum/service/CommentServiceTest.java`
- `src/test/java/com/bitforum/controller/AdminArticleControllerTest.java`
- `src/test/java/com/bitforum/controller/ArticleControllerTest.java`
- `frontend/src/api/articleApi.js`
- `frontend/src/api/commentApi.js`
- `frontend/src/pages/ArticleDetail.jsx`
- `frontend/src/pages/MyArticles.jsx`
- `frontend/src/App.jsx`
- `frontend/src/style.css`

## 初步实现结论

- 当前存在 `NotificationListener`，但它只是 `article.publish.queue` 的 RabbitMQ 消费者，用于文章发布异步消息和幂等消费日志，不是站内通知中心。
- `RabbitMQConfig` 只有文章发布队列、交换机、死信队列和 JSON 消息转换配置；M4 不新增队列，采用同步写 `notification` 表更稳。
- `RedisService.like(articleId, userId)` 返回本次是否首次点赞，适合只在 `true` 时创建点赞通知，避免重复点赞通知。
- `CommentService.publish(userId, articleId, content)` 当前只校验文章是否存在，没有限制 `PUBLISHED`；M4 需要补齐只允许评论已发布文章，并在插入评论成功后给非本人作者发通知。
- `ArticleController.like` 当前先用 `articleService.findPublishedById` 限制已发布文章，再调用 `redisService.like`；M4 可以在首次点赞成功后创建通知。
- `ArticleService.favoriteArticle` 当前已限制只能收藏 `PUBLISHED` 文章，并先查重再插入；M4 可以在插入收藏成功后给非本人作者发通知。
- `ArticleService.approve/reject/offline` 是审核通过、驳回、下架的集中入口；M4 可以在状态更新和审核记录写入成功后创建作者通知。
- `WebMvcConfig` 当前显式列出登录拦截路径；M4 需要把 `/api/user/notifications/**` 加入登录拦截。
- 控制器测试使用 `@MockitoBean JwtUtil` 和 `UserService` 配合 MockMvc 验证 Interceptor；通知控制器测试应沿用这个模式。
- 服务测试使用真实数据库事务回滚，`RabbitTemplate` 和 `RedisService` 用 mock；通知业务测试可在 `ArticleServiceTest`、`CommentServiceTest` 中直接查询 `NotificationMapper`。
- 前端没有 React Router，`App.jsx` 通过 `activePage` 字符串控制当前页面；M4 应新增 `notifications` 页面状态，不改变整体结构。
- `frontend/src/api/request.js` 已统一处理 token 和 `Result<T>`；通知 API 只需要新增普通 axios 封装。
- `style.css` 已有 `content-view`、`section-heading`、`article-list`、`status-badge`、`pager` 等通用样式，通知中心可以复用并少量补充通知状态样式。

## 最终实现记录

- 使用 `V6__add_notification.sql` 新增 `notification` 表。
- 通知类型使用字符串常量：`COMMENT`、`LIKE`、`FAVORITE`、`AUDIT_APPROVED`、`AUDIT_REJECTED`、`ARTICLE_OFFLINE`。
- 通知读状态使用 `0` 未读、`1` 已读。
- `NotificationService` 负责通知分页、未读计数、单条已读、全部已读和各类业务通知创建。
- 通知创建统一跳过“自己给自己发通知”的场景。
- 评论通知在评论插入成功后创建，且评论只允许作用于 `PUBLISHED` 文章。
- 点赞通知只在 `RedisService.like` 返回 `true` 时创建，重复点赞不创建通知。
- 收藏通知只在 `favoriteArticle` 插入收藏成功后创建，重复收藏仍由 M3 业务校验阻止。
- 审核通过、审核驳回、文章下架通知在 `ArticleService` 对应状态流转成功后创建；驳回和下架内容包含原因。
- 通知接口挂在 `/api/user/notifications`，并接入现有 JWT + `LoginInterceptor`。
- 前端新增通知中心页面、通知 API、顶部导航入口和未读数量展示。

## 验证发现

- `mvn test` 首次执行时测试环境无法连接 MySQL，Spring Boot 在 Flyway 初始化阶段报 `Communications link failure`，未进入业务断言阶段。
- `docker compose ps` 显示 Docker Desktop Linux Engine 未运行，无法检查或启动 Compose 服务；本轮不使用 Docker。
- 用户手动启动 Windows `MySQL` 服务后，`mvn test` 重跑通过，85 个测试全部通过。
- Flyway 成功应用 `V6__add_notification.sql`，schema 从 v5 升到 v6。
- `mvn -DskipTests compile` 通过，新增后端代码编译正常。
- `npm run build` 通过，前端通知中心构建正常。
