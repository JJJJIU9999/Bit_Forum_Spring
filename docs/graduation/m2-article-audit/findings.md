# M2 文章审核流调查记录

## 接手检查
- 当前分支：`graduation-design`。
- 最近提交：`2061d18 Merge branch 'phase-2-react-frontend'`。
- 工作区状态：存在 M1 板块分类未提交改动，包含后端、测试、前端和 `docs/graduation/` 文档。
- `git diff --stat`：12 个已跟踪文件变更，约 528 行新增、137 行删除；另有 M1 新增文件未跟踪。
- planning catchup 默认 `.codex` 路径不存在；已使用 `.agents` skill 路径执行，未输出未同步上下文。

## M1 状态确认
- 已新增 `category` 表。
- 已新增 `article.category_id`。
- 已有文章迁移到默认板块。
- `article.category_id` 最终 `NOT NULL`。
- 文章发布必须选择启用板块。
- 公共文章分页支持 `categoryId` 筛选。
- 文章响应补充 `categoryName`。
- 管理员可以管理板块。
- `npm run build` 已通过。
- `mvn test` 已通过，46 个测试全部通过。

## 已知边界
- 不修改 `V1__init_schema.sql`、`V2__add_user_role_status.sql`、`V3__add_category.sql`。
- M2 数据库变更使用新迁移 `V4`。
- 旧文档中的“历史 M2 数据一致性”不是本轮 M2。

## 待调查代码
- `src/main/java/com/bitforum/entity/Article.java`
- `src/main/java/com/bitforum/service/ArticleService.java`
- `src/main/java/com/bitforum/controller/ArticleController.java`
- `src/main/java/com/bitforum/controller/AdminArticleController.java`
- `src/main/java/com/bitforum/dto/ArticlePublishRequest.java`
- `src/test/java/com/bitforum/service/ArticleServiceTest.java`
- `src/test/java/com/bitforum/controller/AdminArticleControllerTest.java`

## 代码调查发现
- `Article` 当前包含 M1 的 `categoryId` 和非表字段 `categoryName`，尚无 `status`。
- `ArticleService.publish(...)` 当前会校验启用板块、插入文章，并立即发送 RabbitMQ 文章发布消息。
- `ArticleService.pageArticles(...)` 当前只支持可选 `categoryId` 筛选，未过滤公开状态。
- `ArticleController` 的 `/api/article/listAll`、`/api/article/page`、`/api/article/detail`、`/api/article/view`、`/api/article/like`、`/api/article/hot` 都会按文章存在与否处理，尚未限制 `PUBLISHED`。
- `AdminArticleController` 当前只有管理员分页和删除任意文章接口。
- `ArticlePublishRequest` 已包含 M1 的 `categoryId` 必填校验，可继续复用到提交审核。
- `ArticleServiceTest` 使用真实数据库、mock `RabbitTemplate` 和 `RedisService`，适合覆盖状态流转。
- `AdminArticleControllerTest` 通过 mock `JwtUtil` 和 `UserService` 验证 `AdminInterceptor` 权限，适合覆盖“普通用户不能审核”。

## 初步实现结论
- `/api/article/publish` 保留路径，但语义改为“提交审核”，插入文章时设置 `PENDING`，不立即发送 RabbitMQ。
- 新增 `saveDraft` 保存 `DRAFT`，仍校验启用板块，避免后续提交时出现无效板块。
- 新增 `submit` 只允许作者将 `DRAFT` 或 `REJECTED` 提交为 `PENDING`。
- 审核通过时将 `PENDING` 改为 `PUBLISHED`，并在此时发送原文章发布 MQ 消息。
- 驳回必须要求原因，记录 `article_audit_record`，状态改为 `REJECTED`。
- 下架只允许管理员将 `PUBLISHED` 改为 `OFFLINE`，并清理该文章 Redis 热度、浏览和点赞数据，避免下架文章继续出现在热门列表。

## 最终实现记录
- `V4__add_article_audit_status.sql` 新增 `article.status` 和 `article_audit_record`。
- `ArticleService` 使用字符串常量维护五种状态，避免额外引入复杂状态机依赖。
- `/api/article/publish` 保留路径，但插入文章状态为 `PENDING`，不再立即发送 RabbitMQ。
- `ArticleService.approve` 将 `PENDING` 改为 `PUBLISHED`，记录审核记录，并发送原有 `ArticlePublishMessage`。
- `ArticleService.reject` 要求驳回原因，状态改为 `REJECTED`，写入 `article_audit_record`。
- `ArticleService.offline` 只允许下架 `PUBLISHED`，状态改为 `OFFLINE`，写入审核记录并清理 Redis 文章数据。
- 公开列表、分页、详情、浏览、点赞、热门均通过 `PUBLISHED` 限制公开可见内容。
- 管理端分页保留全状态查看，审核分页默认查询 `PENDING`。
- 前端新增“我的文章”页，支持按状态查看作者自己的文章，并支持草稿/驳回文章重新提交审核。
- 管理员面板新增“文章审核”页签，并在文章管理中支持下架已发布文章。

## 验证发现
- 首次 `mvn -DskipTests compile` 在沙箱内因 Maven Central 网络访问被拦截失败，提权重跑后通过。
- 首次 `mvn test` 因测试代码中 `RabbitTemplate.convertAndSend` matcher 重载歧义失败，限定 matcher 类型后通过。
- 完整后端测试最终通过：58 个测试全部通过。
- 前端 `npm run build` 通过：Vite 成功构建 94 个模块。
