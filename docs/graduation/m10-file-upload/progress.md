# M10 文件上传进度记录

## 2026-07-01

### Phase 1：接手检查与文档创建

- **Status:** complete
- 已执行前置检查：
  - `git branch --show-current`：`graduation-design`
  - `git status --short`：工作区非干净，包含 M1-M9 未提交改动
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git diff --stat`：确认当前已有文档、前端、后端、测试等未提交改动
  - 迁移目录当前包含 `V1` 至 `V10__add_user_follow.sql`
- 已阅读毕业设计总览、进度、M7、M8、M9 文档。
- 已创建：
  - `docs/graduation/m10-file-upload/task_plan.md`
  - `docs/graduation/m10-file-upload/findings.md`
  - `docs/graduation/m10-file-upload/progress.md`
- 已阅读 M10 相关后端和前端代码入口：
  - `WebMvcConfig`
  - `Article`
  - `ArticleService`
  - `ArticleController`
  - 文章发布/草稿/更新 DTO
  - `OpenApiControllerTest`
  - `ArticleServiceTest`
  - `frontend/src/api/request.js`
  - `frontend/src/api/articleApi.js`
  - `frontend/src/pages/PublishArticle.jsx`
  - `frontend/src/pages/MyArticles.jsx`
  - `frontend/src/components/ArticleCard.jsx`
  - `frontend/src/pages/ArticleDetail.jsx`
  - `frontend/src/style.css`

### Phase 2：后端模型、迁移与上传能力

- **Status:** complete
- 新增 `V11__add_file_upload_fields.sql`，为 `article` 增加 `cover_url`。
- 新增 `FileUploadResponse`、`FileUploadService`、`FileUploadController`。
- `POST /api/upload/avatar` 和 `POST /api/upload/article-cover` 已接入 `LoginInterceptor`。
- `WebMvcConfig` 新增 `/uploads/**` 静态资源映射。
- `.gitignore` 已加入 `uploads/`。

### Phase 3：后端联动与测试

- **Status:** complete
- `Article`、文章发布/草稿/更新 DTO 和 `ArticleService` 已支持 `coverUrl`。
- `ArticleController` 发布、保存草稿、更新草稿、更新文章时透传 `coverUrl`。
- 新增 `FileUploadServiceTest` 和 `FileUploadControllerTest`。
- 扩展 `ArticleServiceTest`、`ArticleControllerTest` 和 `OpenApiControllerTest`。

### Phase 4：前端联动

- **Status:** complete
- 新增 `frontend/src/api/uploadApi.js`。
- `PublishArticle.jsx` 支持封面 URL、封面上传、预览，并在提交审核/保存草稿时传递 `coverUrl`。
- `MyArticles.jsx` 的个人资料页签支持头像上传，并把返回 URL 写入现有头像 URL 输入框。
- `MyArticles.jsx` 的草稿/驳回文章支持封面上传、封面 URL 编辑和保存封面。
- `ArticleCard.jsx` 和 `ArticleDetail.jsx` 在有 `coverUrl` 时展示封面。
- `style.css` 补充上传行、封面预览和移动端布局样式。

### Phase 5：验证与文档收尾

- **Status:** complete
- 聚焦测试通过，48 个测试全部通过。
- 完整后端测试通过，164 个测试全部通过。
- 后端编译通过。
- 前端构建通过，Vite 构建 118 个模块。
- `git diff --check` 通过，仅有 LF/CRLF warning，无实际 whitespace error。
- 已更新总览、进度和 M10 文档。

### 上传失败修复

- **Status:** complete
- 复现确认：使用临时测试用户和 68 字节 PNG 直接请求 `POST /api/upload/avatar` 成功，返回 `/uploads/avatar/...png`。
- 定位原因：浏览器选择的头像文件很可能超过 Spring Boot 默认 multipart 1MB 上限，请求未进入业务 Controller，导致前端显示通用内部错误。
- 已调整 `src/main/resources/application.yml` 和 `src/test/resources/application.yml`：
  - `spring.servlet.multipart.max-file-size: 6MB`
  - `spring.servlet.multipart.max-request-size: 6MB`
- 新增 `FileUploadIntegrationTest`，确保超过头像业务限制的文件返回明确业务错误。
- 复现确认：`localhost:5173/uploads/avatar/...png` 曾返回 `text/html` 和 Vite `index.html`，而 `localhost:8080/uploads/avatar/...png` 返回 `image/png`。
- 已更新 `frontend/vite.config.js`，把 `/uploads` 代理到后端 `http://localhost:8080`。
- 修复后 `localhost:5173/uploads/avatar/...png` 返回 `image/png`。
- 针对 6.43MB 的 `wallhaven-jew8oq.png`，已在前端上传前校验头像 2MB、封面 5MB 限制。
- `request.js` 已为 multipart 的 Network Error 返回上传相关提示，不再误导为后端未启动。
- `GlobalExceptionHandler` 已兜住 `MaxUploadSizeExceededException`，框架层超限时返回明确上传大小错误。

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `git branch --show-current` | 通过，当前为 `graduation-design` |
| `git log -1 --oneline` | 通过，最新提交为 `5321630 fix(frontend): align admin table action borders` |
| `git status --short` | 已确认工作区非干净，保留 M1-M9 未提交改动 |
| `git diff --stat` | 已确认现有改动范围 |
| `Get-ChildItem -Path src\main\resources\db\migration ...` | 当前最新为 `V10__add_user_follow.sql` |
| `mvn "-Dtest=FileUploadServiceTest,FileUploadControllerTest,ArticleServiceTest,ArticleControllerTest,OpenApiControllerTest" test` | 通过，48 个测试全部通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，164 个测试全部通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile` | 通过 |
| `npm run build` | 通过，Vite 构建 118 个模块 |
| `git diff --check` | 通过，仅有 LF/CRLF warning |
| `curl.exe -F file=@... POST /api/upload/avatar` | 通过，68 字节 PNG 上传成功 |
| `Invoke-WebRequest http://localhost:5173/uploads/avatar/...png` | 通过，返回 `image/png` |

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 附件第一次按默认编码读取出现乱码 | 使用 `Get-Content -Encoding UTF8` 重新读取 |
| 首次聚焦测试在沙箱内无法访问 Maven Central，提示 `Permission denied: getsockopt` | 使用网络权限重跑同一条聚焦测试后通过 |
| 浏览器上传较大头像时显示“服务器内部错误” | 提高 Spring multipart 框架上限到 6MB，让业务层返回头像 2MB/封面 5MB 的明确错误 |
| 上传成功后头像预览破图 | Vite 开发服务器补充 `/uploads` 代理到 Spring Boot 后端 |
| 上传超大图片时前端误报后端未启动 | 前端发请求前做文件大小校验，并优化 multipart Network Error 文案 |
