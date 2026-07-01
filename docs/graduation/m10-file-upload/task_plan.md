# M10 文件上传任务计划

## M10 目标

实现基础本地文件上传能力，支持头像上传和文章封面上传，并与 M7 个人资料、M2 文章发布/草稿/审核流程增量联动。

## 当前接手状态

- 当前分支：`graduation-design`
- 最近提交：`5321630 fix(frontend): align admin table action borders`
- 当前工作区：非干净状态，包含 M1-M9 大量未提交改动，必须保留
- 当前最新 Flyway 迁移：`V10__add_user_follow.sql`
- 本轮只做 M10，不做 M11、M12 或其他后续增强
- 不自动提交，不 push，不 stash，不删除、回滚或覆盖既有未提交改动

## 范围边界

- 只实现本地文件上传、静态访问、头像表单联动和文章封面字段联动。
- 不做对象存储、CDN、图片裁剪、图片压缩、水印、秒传、断点续传、富文本图片上传。
- 不做私信、动态流、推荐算法、黑名单、关注分组。
- 不做 Redis 定时落库、Actuator、Spring Security、Spring Cloud、Elasticsearch、Kafka。
- 不引入 React Router、Redux、Zustand 或新 UI 组件库。
- 不修改历史 Flyway 脚本 `V1` 到 `V10`。

## 后端方案

- 新增 `V11__add_file_upload_fields.sql`，为 `article` 增加 `cover_url`。
- 新增 `FileUploadResponse`、`FileUploadService`、`FileUploadController`。
- 接口继续使用 `Result<T>`，通过现有 `LoginInterceptor` 显式拦截：
  - `POST /api/upload/avatar`
  - `POST /api/upload/article-cover`
- 文件保存到项目运行目录下：
  - `uploads/avatar/`
  - `uploads/article-cover/`
- 静态访问路径：
  - `/uploads/avatar/{uuid}.{ext}`
  - `/uploads/article-cover/{uuid}.{ext}`
- 允许图片类型：`image/jpeg`、`image/png`、`image/webp`。
- 限制大小：头像 2MB，文章封面 5MB。
- 使用 UUID 生成最终文件名，保留安全扩展名，路径 normalize 防止路径穿越。
- `.gitignore` 加入 `uploads/`，避免提交本地上传文件。

## 文章封面联动方案

- `Article` 增加 `coverUrl`。
- `ArticlePublishRequest`、`ArticleDraftRequest`、`ArticleDraftUpdateRequest`、`ArticleUpdateRequest` 增加 `coverUrl`。
- `ArticleService.createArticle(...)`、`updateDraft(...)`、`update(...)` 保存或更新 `coverUrl`。
- 不改变 M2 的提交审核、保存草稿、审核通过/驳回、下架语义。

## 前端方案

- 新增 `frontend/src/api/uploadApi.js`，使用 `FormData` 字段名 `file`。
- 在 `MyArticles.jsx` 的个人资料页签中新增头像文件选择、上传按钮和预览；上传成功后写入现有 `avatar` 字段，再复用保存资料接口。
- 在 `PublishArticle.jsx` 增加文章封面 URL、文件选择、上传按钮和预览；提交审核和保存草稿时传递 `coverUrl`。
- 在 `MyArticles.jsx` 编辑草稿/被驳回文章时支持查看和更新 `coverUrl`。
- 在 `ArticleCard.jsx` 和 `ArticleDetail.jsx` 有 `coverUrl` 时展示封面，无封面时保持原布局。
- CSS 仅做必要增量，避免大范围重做前端。

## 测试清单

- 未登录不能上传。
- 空文件上传失败。
- 非图片类型上传失败。
- 超过大小限制上传失败。
- 合法头像上传成功并返回 `/uploads/avatar/...`。
- 合法封面上传成功并返回 `/uploads/article-cover/...`。
- 原始文件名中的路径不能影响最终保存路径。
- 文章发布、草稿保存、草稿更新和文章更新能保存 `coverUrl`。
- `/v3/api-docs` 包含 M10 上传接口。
- 完整 `mvn test`、后端编译、前端构建、`git diff --check` 通过或记录可接受告警。

## 阶段计划

### Phase 1：接手检查与代码调研

- [x] 执行 git 分支、状态、最近提交、diff 统计和迁移目录检查。
- [x] 阅读毕业设计总览、进度、M7、M8、M9 文档。
- [x] 阅读 M10 相关后端和前端现有代码。

### Phase 2：后端上传能力

- [x] 新增 V11 迁移和 `coverUrl` 字段映射。
- [x] 新增上传 DTO、Service、Controller。
- [x] 更新 `WebMvcConfig` 登录拦截和静态资源映射。
- [x] 更新 `.gitignore`。

### Phase 3：后端联动与测试

- [x] 文章发布、草稿、更新流程保存 `coverUrl`。
- [x] 补充上传服务、控制器、文章服务和 OpenAPI 测试。

### Phase 4：前端联动

- [x] 新增上传 API 封装。
- [x] 个人资料头像上传联动。
- [x] 文章发布/草稿/编辑封面上传联动。
- [x] 列表卡片和详情展示封面。
- [x] 补充必要 CSS。

### Phase 5：验证与文档收尾

- [x] 运行聚焦测试。
- [x] 运行完整后端测试。
- [x] 运行后端编译。
- [x] 运行前端构建。
- [x] 运行 `git diff --check`。
- [x] 更新总览、进度和 M10 文档。

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 附件第一次按默认编码读取出现乱码 | 使用 `Get-Content -Encoding UTF8` 重新读取 |
| 首次聚焦测试在沙箱内无法访问 Maven Central | 使用网络权限重跑同一条聚焦测试后通过 |
