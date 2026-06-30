# M3 收藏与基础搜索执行计划

## M3 目标

实现用户收藏已发布文章，以及按关键词搜索已发布文章。

本轮只做 M3，不提前实现通知中心、举报、统计、用户主页等后续模块。

## 范围边界

- 保留 M1、M2 未提交改动，不删除、不回滚、不覆盖、不 stash。
- 不在 `main` 分支开发，当前分支保持为 `graduation-design`。
- 不修改历史 Flyway 脚本 `V1`、`V2`、`V3`、`V4`。
- 不引入 Elasticsearch，搜索使用 MySQL `LIKE`。
- 不引入 Spring Security、Spring Cloud、Kafka、复杂 RBAC、Redux、Zustand 或 UI 组件库。
- 继续使用现有 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation 和 React/Vite 简单结构。
- 收藏只针对 `PUBLISHED` 文章；搜索只返回 `PUBLISHED` 文章。
- 下架文章不能出现在公开搜索和普通“我的收藏”列表中。

## 数据库迁移方案

已新增 Flyway 脚本：

```text
src/main/resources/db/migration/V5__add_article_favorite.sql
```

新增表：

```text
article_favorite
```

字段：

- `id BIGINT PRIMARY KEY AUTO_INCREMENT`
- `article_id BIGINT NOT NULL`
- `user_id BIGINT NOT NULL`
- `create_time DATETIME DEFAULT CURRENT_TIMESTAMP`

索引和约束：

- `UNIQUE KEY uk_article_favorite_user_article (user_id, article_id)`，防止重复收藏。
- `KEY idx_article_favorite_article_id (article_id)`，支持统计文章收藏数。
- `KEY idx_article_favorite_user_create_time (user_id, create_time)`，支持我的收藏分页。

本轮没有在 `article` 表新增 `favorite_count` 字段，使用实时 count 查询，降低状态同步风险。

## 后端接口方案

新增实体和 Mapper：

- `ArticleFavorite`
- `ArticleFavoriteMapper`

调整 `Article` 响应字段：

- 新增非表字段 `favoriteCount`。

新增服务方法：

- `favoriteArticle(userId, articleId)`：只能收藏 `PUBLISHED` 文章；重复收藏返回明确错误。
- `unfavoriteArticle(userId, articleId)`：只能取消自己的收藏。
- `pageFavoriteArticles(userId, pageNum, pageSize)`：只返回当前用户收藏且仍为 `PUBLISHED` 的文章。
- `searchPublishedArticles(keyword, categoryId, pageNum, pageSize)`：按标题和正文 `LIKE` 搜索 `PUBLISHED` 文章，支持板块筛选，按 `create_time DESC` 排序。
- 公共列表、详情、搜索和我的收藏返回文章时补充收藏数。

新增接口：

- `POST /api/article/favorite?articleId=1`
- `DELETE /api/article/favorite?articleId=1`
- `GET /api/user/favorites?pageNum=1&pageSize=10`
- `GET /api/article/search?keyword=xxx&pageNum=1&pageSize=10`
- `GET /api/article/search?keyword=xxx&categoryId=1&pageNum=1&pageSize=10`

兼容要求：

- `GET /api/article/page` 保持可用。
- 公开文章列表仍只展示 `PUBLISHED`。
- 搜索不需要登录。
- 收藏、取消收藏、我的收藏需要登录。

## 前端兼容方案

- `articleApi.js` 增加收藏、取消收藏、我的收藏和搜索 API 封装。
- `ArticleList.jsx` 增加关键词搜索框，保留板块筛选和分页；空关键词仍走原公共分页接口。
- `ArticleDetail.jsx` 增加收藏数展示和收藏/取消收藏按钮；未登录时禁用收藏操作。
- `MyArticles.jsx` 扩展为“我的文章 / 我的收藏”两个简单视图，不引入路由或状态管理库。
- 保持现有 React/Vite 单页字符串状态切换模式。

## 测试清单

- [x] 用户可以收藏已发布文章。
- [x] 用户不能收藏草稿、待审核、驳回、下架文章。
- [x] 用户不能重复收藏同一篇文章。
- [x] 用户可以取消自己的收藏。
- [x] 我的收藏只返回当前用户收藏的 `PUBLISHED` 文章。
- [x] 下架文章不会出现在我的收藏列表。
- [x] 搜索只返回 `PUBLISHED` 文章。
- [x] 搜索支持 title/content 关键词。
- [x] 搜索支持 categoryId 筛选。
- [x] 未登录用户不能收藏文章。
- [x] 普通公开搜索不需要登录。

## 验证命令

后端完整测试：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

结果：通过，66 个测试全部通过。

前端构建：

```powershell
cd frontend
npm run build
```

结果：通过，Vite 成功构建 94 个模块。

## 风险和待确认事项

- M1、M2、M3 均尚未提交，后续提交或交接时需要明确区分。
- 实时统计 `favoriteCount` 比新增计数字段更可靠，但列表中会产生额外 count 查询；毕业设计规模下可接受。
- 如果未来需要展示“已收藏但下架”的历史记录，可在后续用户中心模块增加筛选；本轮默认不展示。

## 阶段

### 阶段 1：接手与计划

- [x] 执行 Git 前置检查。
- [x] 阅读毕业设计总览、进度、M1、M2 文档。
- [x] 阅读 Article、ArticleService、ArticleController、UserArticleController、AdminArticleController、ArticleServiceTest、AdminArticleControllerTest。
- [x] 阅读前端 articleApi、ArticleList、ArticleDetail、MyArticles、AdminPanel。
- [x] 创建 M3 文档目录和计划文件。

### 阶段 2：后端实现

- [x] 新增 Flyway V5。
- [x] 新增 ArticleFavorite Entity / Mapper。
- [x] 实现收藏、取消收藏、我的收藏、搜索服务方法。
- [x] 新增接口并更新登录拦截路径。
- [x] 文章响应补充 `favoriteCount`。

### 阶段 3：后端测试

- [x] 补充 ArticleServiceTest 覆盖收藏和搜索核心业务。
- [x] 补充 ArticleControllerTest 覆盖未登录收藏失败、公开搜索可访问。
- [x] 运行后端测试。

### 阶段 4：前端实现

- [x] 增加 API 封装。
- [x] 文章列表增加搜索框和板块组合筛选。
- [x] 详情页增加收藏操作和收藏数展示。
- [x] 我的页面增加收藏列表。
- [x] 运行前端构建。

### 阶段 5：文档收尾

- [x] 更新 M3 findings/progress。
- [x] 更新毕业设计进度。
- [x] 汇总修改文件、接口、迁移、测试和风险。

