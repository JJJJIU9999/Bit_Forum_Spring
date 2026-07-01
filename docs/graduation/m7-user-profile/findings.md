# M7 用户主页与个人资料调研记录

## 接手状态

- 当前分支：`graduation-design`。
- 最新提交：`5321630 fix(frontend): align admin table action borders`。
- 接手时工作区干净，`git status --short` 无输出，`git diff --stat` 无输出。

## 已知边界

- M1-M6 已完成并验证。
- 本轮只做 M7 用户主页与个人资料。
- 不自动提交，等待用户确认。

## 数据库发现

- 当前迁移目录实际已经包含 `V8__add_pending_report_unique_key.sql`。
- 如果 M7 需要新增 `nickname`、`bio`，应新增 `V9__add_user_profile_fields.sql`，不能复用或修改已有 V8。
- 目前已发现 `user_info` 有 `avatar` 字段，尚未发现 `nickname`、`bio` 字段。

## 实现记录

- 新增 `V9__add_user_profile_fields.sql`，为 `user_info` 增加 `nickname`、`bio`。
- 新增当前用户资料查询/更新接口和公开用户主页接口。
- 新增公开用户文章分页接口，只返回 `PUBLISHED`。
- 前端新增“我的-个人资料”页签、公开用户主页页面和文章详情作者主页入口。
- 不引入 React Router、Redux、zustand、文件上传或新 UI 组件库。

## 验证记录

- 聚焦后端测试通过：`mvn "-Dtest=UserServiceTest,UserProfileControllerTest,ArticleServiceTest" test`，共 40 个测试。
- 前端构建通过：`npm run build`，Vite 构建 117 个模块。
- 后端编译通过：`mvn -DskipTests compile`。
- 完整后端测试通过：`mvn test`，共 130 个测试。
