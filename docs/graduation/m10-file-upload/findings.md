# M10 文件上传调研记录

## 接手结论

- 当前分支为 `graduation-design`，最近提交为 `5321630 fix(frontend): align admin table action borders`。
- 工作区包含 M1-M9 未提交改动，属于预期状态；M10 必须在此基础上增量实现。
- 当前迁移目录最新为 `V10__add_user_follow.sql`，M10 数据库变化应新增 `V11__add_file_upload_fields.sql`。
- M7 已复用 `user_info.avatar` 作为头像 URL 字符串；M10 不需要新增头像字段。
- M8 已引入 springdoc-openapi，新上传接口需要加入 OpenAPI 注解并扩展文档测试。
- M9 已将关注写接口加入 `WebMvcConfig` 显式登录拦截；M10 上传写接口也需要手动加入。

## 设计决策

| 决策 | 理由 |
| --- | --- |
| 使用本地 `uploads/` 目录 | 满足毕业设计演示和本轮基础上传目标，避免对象存储/CDN 复杂度 |
| 使用 UUID 作为最终文件名 | 避免原始文件名冲突和路径穿越风险 |
| 只允许 jpeg/png/webp | 覆盖头像和文章封面常见图片格式，减少安全面 |
| 头像 2MB、封面 5MB | 与交接要求一致，保持简单明确的业务限制 |
| `coverUrl` 直接作为文章字段返回 | 现有列表、详情、我的文章和管理员文章多处直接返回 `Article`，字段自然带出，减少 DTO 重构 |
| 前端保留 URL 输入框 | 兼容 M7 已有头像 URL 和文章封面手工 URL 调试场景 |

## 代码调研结论

- `WebMvcConfig` 仅通过 `addPathPatterns` 显式保护写接口；M10 上传接口必须加入 `/api/upload/avatar` 和 `/api/upload/article-cover`。
- `Article` 当前无 `coverUrl` 字段，MyBatis-Plus 可通过驼峰下划线映射 `cover_url`。
- `ArticleService` 当前 `publish`、`saveDraft`、`updateDraft`、`update` 方法集中处理文章写入；可保留旧重载以减少既有测试和调用改动。
- `ArticleController` 当前从 DTO 读取标题、内容、板块；M10 只需把 `coverUrl` 透传给服务层。
- `OpenApiControllerTest` 已断言 M9 路径存在；M10 可追加上传路径断言。
- 测试配置使用本机 MySQL 和 Flyway，不是 H2；新增 V11 会在完整测试时实际迁移。
- 前端 `request.js` 已统一补 Authorization，上传 API 可直接复用同一 Axios 实例。
- `PublishArticle.jsx` 当前只有标题、内容、板块；需要新增封面 URL、文件选择、上传和预览。
- `MyArticles.jsx` 当前个人资料只支持头像 URL；我的文章列表没有完整编辑页，M10 可在草稿/驳回卡片上增加轻量封面更新区，复用 `updateArticle`。
- `ArticleCard.jsx` 和 `ArticleDetail.jsx` 无封面展示逻辑；可在有 `coverUrl` 时插入图片，无封面时保持现有布局。

## 实现结果

- 新增 `POST /api/upload/avatar` 和 `POST /api/upload/article-cover`，均需要登录。
- 新增 `V11__add_file_upload_fields.sql`，为 `article` 增加 `cover_url`。
- 上传文件保存到 `uploads/avatar/` 和 `uploads/article-cover/`，通过 `/uploads/**` 静态路径访问。
- `uploads/` 已加入 `.gitignore`，避免提交本地用户上传文件。
- 头像上传成功后写回个人资料表单的 `avatar` 字段，继续通过 `PUT /api/user/profile` 保存。
- 文章封面上传成功后写回 `coverUrl`，发布、保存草稿、更新草稿和文章更新都会把 `coverUrl` 传给后端。
- 文章卡片和文章详情在存在 `coverUrl` 时展示封面图片；没有 `coverUrl` 时保持原布局。
- M11 Redis 定时落库和 M12 Actuator 健康检查未开始。

## 上传失败复盘

- 浏览器选择较大的头像图片时，Spring Boot 默认 multipart 上限约为 1MB，请求会在进入 `FileUploadController` 前被框架拒绝，前端只能显示通用“服务器内部错误”。
- 已在 `application.yml` 和测试配置中设置 `spring.servlet.multipart.max-file-size=6MB`、`max-request-size=6MB`，让头像 2MB、封面 5MB 的业务限制由 `FileUploadService` 返回明确错误。
- 补充 `FileUploadIntegrationTest`，验证超过头像业务限制但小于框架上限的图片会返回 `上传文件大小超过限制`，而不是框架 500。
- 头像上传成功后预览破图的原因是开发环境下 Vite 只代理了 `/api`，没有代理 `/uploads`。浏览器访问 `localhost:5173/uploads/...png` 时拿到的是 Vite 的 `index.html`，不是后端图片。已在 `frontend/vite.config.js` 增加 `/uploads` 代理到 `http://localhost:8080`。
- `wallhaven-jew8oq.png` 大小约 6.43MB，超过头像 2MB 和封面 5MB 业务限制；前端已在 `uploadApi.js` 发请求前校验文件大小，并在 `request.js` 为 multipart 网络中断返回上传相关提示，避免误提示“后端未启动”。
