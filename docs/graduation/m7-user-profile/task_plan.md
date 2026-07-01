# M7 用户主页与个人资料任务计划

## M7 目标

实现用户主页与个人资料能力，让系统具备用户身份展示、个人内容聚合和资料维护能力。本模块只做基础用户主页和个人资料，不做关注/粉丝、私信、文件上传、头像文件存储、复杂隐私设置、用户等级积分、Swagger 或后续模块。

## 范围边界

- 当前登录用户可以查看和修改自己的基础资料。
- 支持资料字段：`avatar`、`nickname`、`bio`。
- 不允许通过资料接口修改 `role`、`status`、`password`。
- 用户资料响应不返回 `password`。
- 支持查看任意用户公开主页。
- 支持分页查看某个用户已发布文章，只返回 `PUBLISHED`。
- 前端只在现有 React/Vite 简单结构上增量扩展，不引入 React Router、Redux、zustand 或新 UI 组件库。
- 不重构 M1-M6，不改变审核、下架、举报、dashboard 等既有语义。

## 数据库方案

- 当前 `user_info` 已有 `avatar`，可继续使用头像 URL 字符串。
- 当前迁移目录已经存在 `V8__add_pending_report_unique_key.sql`，若当前表不存在 `nickname`、`bio`，新增迁移 `V9__add_user_profile_fields.sql`。
- 迁移字段建议：
  - `nickname VARCHAR(50) NULL`
  - `bio VARCHAR(255) NULL`
- 不修改历史 Flyway 脚本 V1-V8。

## 后端接口方案

- `GET /api/user/profile`：当前登录用户查看自己的资料，需要登录。
- `PUT /api/user/profile`：当前登录用户修改自己的 `avatar`、`nickname`、`bio`，需要登录。
- `GET /api/users/{userId}/profile`：公开查看用户主页，不返回密码。
- `GET /api/users/{userId}/articles?pageNum=1&pageSize=10`：分页查看用户公开文章，只返回 `PUBLISHED`。
- 继续使用现有 `LoginInterceptor`、`Result<T>`、MyBatis-Plus、Jakarta Validation 风格。

## 前端兼容方案

- 新增个人资料 API 封装。
- 在“我的”相关页面增量增加个人资料视图或页签。
- 在文章详情页增加查看作者主页入口。
- 新增公开用户主页视图，继续使用现有 `activePage` 字符串切换，不引入路由库。
- 保留现有整体布局、后台表格样式和用户刚调整过的前端细节。

## 测试清单

- 登录用户可以查看自己的资料。
- 未登录用户不能访问 `/api/user/profile`。
- 登录用户可以更新 `avatar`、`nickname`、`bio`。
- 资料更新不能修改 `role`、`status`、`password`。
- 用户资料响应不返回 `password`。
- 可以查看公开用户主页。
- 公开用户主页响应不返回 `password`。
- 公开主页文章数量只统计 `PUBLISHED`。
- 用户公开文章分页只返回 `PUBLISHED`。
- 用户不存在时返回明确错误信息。
- 如果新增 V9 迁移，完整 `mvn test` 必须通过 Flyway 校验。
- 前端 `npm run build` 通过。

## 验证命令

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile
```

```powershell
cd frontend
npm run build
```

## 风险和待确认事项

- 需要先核对当前 `user_info` 表和 `User` 实体是否已有 `nickname`、`bio`。
- 公开禁用用户主页采用“可查询基础资料并标明 status”的简单策略，除非现有代码已有不同约定。
- 前端只做 M7 必要入口和页面，避免无关 UI 重构。
