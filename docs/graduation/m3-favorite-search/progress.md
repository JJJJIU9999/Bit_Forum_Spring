# M3 收藏与基础搜索进度

## 当前状态

M3 已完成实现和验证，尚未提交。

## 已完成

- 执行 `git branch --show-current`，确认当前分支为 `graduation-design`。
- 执行 `git status --short`，确认工作区存在 M1、M2 未提交改动，需要保留。
- 执行 `git log -1 --oneline`，最近提交为 `2061d18 Merge branch 'phase-2-react-frontend'`。
- 执行 `git diff --stat`，记录当前 M1、M2 已跟踪文件变更概况。
- 执行 planning session catchup，未发现额外待同步上下文。
- 阅读毕业设计总览、毕业设计进度、M1 文档、M2 文档。
- 阅读后端文章实体、服务、控制器、用户文章控制器、管理员文章控制器和相关测试。
- 阅读前端文章 API、文章列表、文章详情、我的文章、管理员面板和样式。
- 创建 `docs/graduation/m3-favorite-search/`。
- 创建 M3 `task_plan.md`、`findings.md`、`progress.md`。
- 新增 `V5__add_article_favorite.sql`。
- 新增 `ArticleFavorite` 和 `ArticleFavoriteMapper`。
- `Article` 响应补充 `favoriteCount`。
- `ArticleService` 新增收藏、取消收藏、我的收藏分页和基础搜索逻辑。
- `ArticleController` 新增收藏、取消收藏、搜索接口。
- 新增 `UserFavoriteController`。
- `WebMvcConfig` 增加收藏和我的收藏登录拦截路径。
- `ArticleServiceTest` 补充收藏和搜索业务测试。
- 新增 `ArticleControllerTest` 覆盖未登录收藏失败和公开搜索可访问。
- 前端 `articleApi.js` 增加收藏、取消收藏、我的收藏和搜索 API。
- 前端 `ArticleList.jsx` 增加关键词搜索和板块组合筛选。
- 前端 `ArticleDetail.jsx` 增加收藏数展示、收藏和取消收藏操作。
- 前端 `MyArticles.jsx` 增加“我的收藏”视图。
- 前端 `App.jsx` 更新学习步骤。
- 前端 `style.css` 补充搜索表单和收藏指标样式。
- 更新毕业设计进度和 M3 文档。

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `git branch --show-current` | `graduation-design` |
| `git status --short` | 存在 M1、M2、M3 未提交改动和新增文件 |
| `git log -1 --oneline` | `2061d18 Merge branch 'phase-2-react-frontend'` |
| `git diff --stat` | 初始接手时 16 个已跟踪文件变更，约 `1353 insertions(+), 204 deletions(-)` |
| `python C:\Users\10603\.agents\skills\planning-with-files\scripts\session-catchup.py D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring` | 无额外输出 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` | 通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 首次失败：新增测试标题超过 50 字符限制 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，66 个测试全部通过 |
| `npm run build` | 通过，Vite 构建 94 个模块 |

## 错误处理记录

| 问题 | 处理 |
| --- | --- |
| 新增测试标题过长导致 MySQL 报 `Data too long for column 'title'` | 缩短测试标题后重跑完整测试，通过 |

## 下一步

- 等待用户确认是否提交。
- 后续进入 M4 前继续保留 M1、M2、M3 未提交改动，除非用户明确要求提交。

