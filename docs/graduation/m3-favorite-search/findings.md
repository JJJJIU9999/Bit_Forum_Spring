# M3 收藏与基础搜索调查记录

## 接手检查

- 当前分支：`graduation-design`。
- 最近提交：`2061d18 Merge branch 'phase-2-react-frontend'`。
- 工作区状态：存在 M1、M2 未提交改动，包含后端、前端、测试、迁移和 `docs/graduation/` 文档。
- `git diff --stat`：16 个已跟踪文件变更，约 `1353 insertions(+), 204 deletions(-)`，另有 M1、M2 新增文件未跟踪。
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

## 代码调查发现

- `ArticleService` 已经有状态常量，可直接复用 `STATUS_PUBLISHED` 判断收藏和搜索范围。
- `pageArticles` 已支持 `categoryId`，搜索可沿用其 QueryWrapper 风格。
- `findPublishedById` 是收藏、详情、浏览、点赞等公开可见性判断的合适入口。
- `UserArticleController` 当前挂在 `/api/user/articles`，M3 新增了同级 `UserFavoriteController` 处理 `/api/user/favorites`。
- `WebMvcConfig` 需要把 `/api/article/favorite` 和 `/api/user/favorites` 加入登录拦截；`/api/article/search` 保持公开。
- 前端当前没有 React Router，入口由 `App.jsx` 的 `activePage` 字符串控制，M3 继续沿用。
- `ArticleList.jsx` 已有板块筛选和分页状态，适合增加关键词搜索。
- `ArticleDetail.jsx` 已有点赞/取消点赞操作区，适合并列增加收藏/取消收藏。
- `MyArticles.jsx` 当前只展示作者文章，M3 扩展为“我的文章 / 我的收藏”切换。

## 实现记录

- 使用独立表 `article_favorite`，不修改 `article` 表计数字段。
- 收藏写入前确认文章存在且状态为 `PUBLISHED`。
- 取消收藏按 `user_id + article_id` 删除，避免影响其他用户。
- 我的收藏查询使用 `article_favorite` 与 `article` 关联，只筛选当前用户和 `PUBLISHED`。
- 搜索接口走公开 `/api/article/search`，不加入登录拦截。
- 收藏数通过 `ArticleFavoriteMapper.selectCount` 填充到 `Article.favoriteCount`。
- 删除文章时同步删除相关收藏记录，避免孤立收藏数据。

## 验证发现

- 首次 `mvn test` 失败原因是新增测试数据标题超过 `article.title` 的 50 字符限制，不是业务实现错误。
- 缩短测试标题后重新运行完整后端测试通过。
- `npm run build` 通过，说明前端 M3 JSX 和样式能正常构建。

