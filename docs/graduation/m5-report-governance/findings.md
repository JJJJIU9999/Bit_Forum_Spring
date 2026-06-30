# M5 举报与内容治理调研记录

## 接手检查

- 当前分支：`graduation-design`。
- 最近提交：`2061d18 Merge branch 'phase-2-react-frontend'`。
- 当前工作区已有 M1-M4 未提交改动，必须保留。
- 已存在迁移：`V1__init_schema.sql` 至 `V6__add_notification.sql`，M5 数据库变更使用 `V7`。

## 已确认约束

- 本轮只做 M5，不做 M6 或后续任务。
- 不修改历史 Flyway 脚本。
- 不引入 WebSocket、Spring Security、Spring Cloud、Kafka、复杂 RBAC、Redux、zustand 或复杂 UI 组件库。
- 保持 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation 和现有 React/Vite 风格。

## 待补充发现

- M1 文档确认：板块分类已完成，`article.category_id` 为必填，公开文章分页支持 `categoryId`，后续不能破坏禁用板块历史文章仍可浏览的语义。
- M2 文档确认：文章状态流已完成，公开列表、详情、浏览、点赞、热门均只处理 `PUBLISHED`；M5 举报文章必须复用这一公开可见性判断。
- M3 文档确认：收藏与搜索已完成，搜索和收藏只处理 `PUBLISHED`，相关登录拦截路径在 `WebMvcConfig` 中显式配置。
- M4 文档确认：通知中心已完成，通知接口在 `/api/user/notifications`；M5 本轮不新增举报通知类型，处理结果通过举报列表体现。
- 后端现有模式：Controller 捕获 `RuntimeException` 并返回 `Result.fail`；Service 使用 MyBatis-Plus `QueryWrapper` / `Page`；Mapper 多数直接继承 `BaseMapper`。
- `WebMvcConfig` 通过显式路径配置登录拦截，M5 需要加入 `/api/user/reports` 相关路径；管理员路径已有 `/api/admin/**` 统一拦截。
- `ArticleService.findPublishedById` 是举报文章的公开可见性判断入口；举报非 `PUBLISHED` 文章应失败。
- `CommentService` 通过 `CommentMapper` 和 `ArticleMapper` 判断评论所属文章是否存在且 `PUBLISHED`；M5 举报评论可沿用同样判断。
- 管理端测试使用 `@MockitoBean JwtUtil`、`UserService` 和业务 service 模拟权限；用户端接口测试可沿用 `UserNotificationControllerTest` 模式。
- 前端现有模式：`App.jsx` 用 `activePage` 字符串切换页面；文章详情、我的页面、管理员面板均为局部状态管理，M5 不引入路由或状态管理库。

## 最终实现记录

- 使用 `V7__add_content_report.sql` 新增 `content_report` 表。
- 举报对象类型使用字符串：`ARTICLE`、`COMMENT`。
- 举报状态使用字符串：`PENDING`、`RESOLVED`、`REJECTED`。
- `ContentReportService` 负责举报提交、用户举报分页、管理员分页、处理成立、处理驳回。
- 文章举报只允许举报他人的 `PUBLISHED` 文章。
- 评论举报要求评论存在，且评论所属文章存在并处于 `PUBLISHED`。
- 用户不能举报自己的文章或评论。
- 同一用户不能对同一对象重复提交 `PENDING` 举报；举报处理后允许再次提交。
- 管理员处理举报只更新举报记录，不自动下架文章或删除评论。
- 本轮没有新增举报通知类型，处理结果通过“我的举报”列表查看。
- 前端文章详情页增加举报文章和举报评论入口。
- 前端“我的”页增加“我的举报”列表。
- 前端管理员面板增加“举报处理”页签，支持按状态查看和处理举报。

## 验证发现

- `mvn -DskipTests compile` 通过，新增后端源码编译正常。
- 聚焦测试 `mvn "-Dtest=ContentReportServiceTest,UserReportControllerTest,AdminReportControllerTest" test` 通过，21 个测试全部通过。
- 完整后端测试 `mvn test` 通过，106 个测试全部通过；Flyway schema 已到 v7。
- 前端 `npm run build` 通过，Vite 构建 97 个模块。
