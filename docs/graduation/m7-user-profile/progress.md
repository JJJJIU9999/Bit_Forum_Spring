# M7 用户主页与个人资料进度

## 2026-06-30

- 创建 M7 文档目录。
- 初始化任务计划、调研记录和进度文档。
- 完成 git 接手检查。
- 核对数据库迁移目录，确认已有 V8；M7 如需新增资料字段将使用 V9。
- 新增后端资料 DTO、控制器、服务方法、登录拦截路径、公开作者文章查询和 V9 迁移。
- 新增用户资料控制器/服务测试，以及公开作者文章只返回已发布文章的服务测试。
- 运行聚焦后端测试：`mvn "-Dtest=UserServiceTest,UserProfileControllerTest,ArticleServiceTest" test` 通过，共 40 个测试。
- 新增前端个人资料 API、当前用户资料页签、公开用户主页页面和文章详情作者主页入口。
- 运行前端构建：`npm run build` 通过，Vite 构建 117 个模块。
- 运行后端编译：`mvn -DskipTests compile` 通过。
- 运行完整后端测试：`mvn test` 通过，共 130 个测试。
- 启动前端开发服务器：`http://127.0.0.1:5173/`。

## 新增接口

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/users/{userId}/profile`
- `GET /api/users/{userId}/articles?pageNum=1&pageSize=10`

## 新增 DTO / Service / Controller

- `UserProfileUpdateRequest`
- `UserProfileResponse`
- `PublicUserProfileResponse`
- `UserProfileController`
- `UserService#getCurrentProfile`
- `UserService#updateProfile`
- `UserService#getPublicProfile`
- `ArticleService#pagePublishedArticlesByUser`

## 数据库迁移

- 新增 `V9__add_user_profile_fields.sql`。
- 本轮没有修改历史迁移 V1-V8。
