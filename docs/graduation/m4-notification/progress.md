# M4 通知中心进度

## 当前状态

M4 已完成通知中心后端、前端、测试补充和文档更新，尚未提交。

## 已完成

- 执行 `git branch --show-current`，确认当前分支为 `graduation-design`。
- 执行 `git status --short`，确认工作区存在 M1、M2、M3 未提交改动，需要保留。
- 执行 `git log -1 --oneline`，最近提交为 `2061d18 Merge branch 'phase-2-react-frontend'`。
- 执行 `git diff --stat`，记录当前 M1、M2、M3 已跟踪文件变更概况。
- 执行 planning session catchup，未发现额外待同步上下文。
- 阅读毕业设计总览、毕业设计进度、M1 文档、M2 文档、M3 文档。
- 创建 `docs/graduation/m4-notification/`。
- 创建 M4 `task_plan.md`、`findings.md`、`progress.md`。
- 阅读 `NotificationListener`、`RabbitMQConfig`、`ArticlePublishMessage`，确认 M4 不复用 RabbitMQ 写通知。
- 阅读 `CommentService`、`RedisService`、`ArticleService`，确认评论、点赞、收藏、审核、下架的通知接入点。
- 阅读 `CommentController`、`ArticleController`、`AdminArticleController`、`WebMvcConfig`，确认接口和登录拦截路径。
- 阅读 `ArticleServiceTest`、`CommentServiceTest`、`ArticleControllerTest`、`AdminArticleControllerTest`，确认测试风格。
- 阅读 `frontend/src/api/articleApi.js`、`commentApi.js`、`request.js`、`ArticleDetail.jsx`、`MyArticles.jsx`、`App.jsx`、`style.css`，确认前端沿用无路由页面切换结构。
- 新增 `V6__add_notification.sql`，创建 `notification` 表和查询索引。
- 新增 `Notification`、`NotificationMapper`、`NotificationService`、`UserNotificationController`。
- `WebMvcConfig` 增加 `/api/user/notifications` 和 `/api/user/notifications/**` 登录拦截。
- `CommentService.publish` 限制只评论已发布文章，并在评论他人文章成功后创建评论通知。
- `ArticleController.like` 只在首次点赞成功时创建点赞通知。
- `ArticleService.favoriteArticle` 在收藏他人文章成功后创建收藏通知。
- `ArticleService.approve/reject/offline` 分别创建审核通过、审核驳回、文章下架通知，驳回和下架通知内容包含原因。
- 新增 `NotificationServiceTest`。
- 补充 `ArticleServiceTest`、`CommentServiceTest`、`ArticleControllerTest`。
- 新增前端 `notificationApi.js` 和 `NotificationCenter.jsx`。
- `App.jsx` 增加“通知”入口、未读数展示和通知中心页面切换。
- `style.css` 增加通知列表、未读状态和响应式布局样式。

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `git branch --show-current` | `graduation-design` |
| `git status --short` | 存在 M1、M2、M3 未提交改动和新增文件 |
| `git log -1 --oneline` | `2061d18 Merge branch 'phase-2-react-frontend'` |
| `git diff --stat` | 18 个已跟踪文件变更，约 `1768 insertions(+), 209 deletions(-)` |
| `python "$env:USERPROFILE\.agents\skills\planning-with-files\scripts\session-catchup.py" (Get-Location)` | 无额外输出 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 首次失败：Spring 上下文启动时 Flyway 无法连接 MySQL，报 `Communications link failure` |
| `docker compose ps` | 失败：Docker Desktop Linux Engine 未运行，无法连接 `npipe:////./pipe/dockerDesktopLinuxEngine` |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` | 通过，`BUILD SUCCESS` |
| `npm run build` | 通过，Vite 构建 96 个模块 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，85 个测试全部通过；Flyway 成功应用 `V6__add_notification.sql` |

## 错误处理记录

| 问题 | 处理 |
| --- | --- |
| 完整 `mvn test` 无法连接 MySQL | 用户手动启动 Windows `MySQL` 服务后重跑完整测试，通过 |

## 下一步

- 等待用户确认是否提交。
