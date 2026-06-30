# M1 板块分类调查记录

## 已知要求
- 普通用户可以查看板块、按板块筛选文章、发布文章时选择板块。
- 管理员可以创建、修改、启用、禁用、删除无文章板块。
- 已有文章迁移到默认板块。
- `article.category_id` 最终不应为空。
- 禁用板块后，已有文章仍可浏览，但用户不能向该板块发布新文章。
- 有文章的板块不能物理删除。

## 待调查
- 当前 Article DTO、Controller、Service 的参数结构。
- 当前管理员测试如何模拟管理员权限。
- 当前前端 API 封装和管理员页面结构。

## 发现记录
- 现有文章发布接口是 `POST /api/article/publish`，请求体只有 `title`、`content`，作者 ID 来自 `@RequestAttribute("userId")`。
- 现有文章分页接口是 `GET /api/article/page?pageNum=&pageSize=`，返回 `Page<Article>`。
- 管理员文章接口复用 `ArticleService.pageArticles(pageNum, pageSize)`。
- 文章详情、热门文章直接返回 `Article` 或包含 `Article` 的 `HotArticle`，M1 最小兼容方案是在 `Article` 实体新增 `categoryId` 和非表字段 `categoryName`。
- 管理员接口权限由 `AdminInterceptor` 统一保护，测试中通过 mock `JwtUtil` 和 `UserService` 模拟管理员。
- 现有前端没有路由，文章列表、发布页和管理员面板都是单文件状态控制，M1 应保持这种结构。
- 现有测试中 `ArticleServiceTest` 使用真实 MySQL 和 mock `RabbitTemplate`、`RedisService`；新增分类测试可以复用这个模式。

## 最终实现结论
- 数据库新增 `category` 表，`article.category_id` 先允许为空用于迁移，填充默认板块后改为 `NOT NULL`。
- 公共板块接口只返回启用板块，保证普通用户发布文章时只能选择可用板块。
- 文章分页支持可选 `categoryId`，禁用板块下的历史文章仍可通过文章列表或详情浏览。
- 发布文章前通过 `CategoryService.findEnabledById` 校验板块，禁用或不存在的板块会拒绝发布。
- 删除板块前检查文章数量，有文章的板块不做物理删除。
- 管理员板块接口沿用现有 `/api/admin/**` 和 `AdminInterceptor` 权限模型。
- 前端保持原有 React/Vite 单页结构，没有新增路由或状态管理库。
