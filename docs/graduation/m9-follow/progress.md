# M9 关注与粉丝进度记录

## 2026-07-01

### Phase 1：接手检查与文档创建

- **Status:** complete
- 已执行前置检查：
  - `git branch --show-current`：`graduation-design`
  - `git status --short`：工作区非干净，包含 M1-M8 未提交改动
  - `git log -1 --oneline`：`5321630 fix(frontend): align admin table action borders`
  - `git diff --stat`：确认当前已有文档、前端、后端、测试等未提交改动
- 已核对迁移目录，当前最新为 `V9__add_user_profile_fields.sql`。
- 已阅读毕业设计总览、进度、M1-M8 文档和 audit-fixes 进度记录。
- 已阅读 M7/M9 相关后端与前端代码入口：
  - `WebMvcConfig`
  - `LoginInterceptor`
  - `UserProfileController`
  - `UserService`
  - `User`
  - `PublicUserProfileResponse`
  - `frontend/src/api/userApi.js`
  - `frontend/src/pages/PublicUserProfile.jsx`
  - `frontend/src/App.jsx`
- 已创建：
  - `docs/graduation/m9-follow/task_plan.md`
  - `docs/graduation/m9-follow/findings.md`
  - `docs/graduation/m9-follow/progress.md`

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `git branch --show-current` | 通过，当前为 `graduation-design` |
| `git log -1 --oneline` | 通过，最新提交为 `5321630 fix(frontend): align admin table action borders` |
| `git status --short` | 已确认工作区非干净，保留 M1-M8 未提交改动 |
| `git diff --stat` | 已确认现有改动范围 |
| `mvn "-Dtest=UserFollowServiceTest,UserFollowIntegrationTest,UserFollowControllerTest,UserServiceTest,UserProfileControllerTest,OpenApiControllerTest" test` | 通过，36 个测试全部通过 |
| `mvn -DskipTests compile` | 通过 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，151 个测试全部通过 |
| `npm run build` | 通过，Vite 构建 117 个模块 |

### Phase 2：后端模型与迁移

- **Status:** complete
- 新增 `V10__add_user_follow.sql`。
- 新增 `UserFollow`、`UserFollowMapper`、`FollowStatsResponse`、`FollowUserResponse`。
- 新增 `UserFollowService`，实现关注、取消关注、统计和列表查询。

### Phase 3：后端接口与 M7 联动

- **Status:** complete
- 新增 `UserFollowController`，提供关注、取消关注、粉丝列表、关注列表和关注统计接口。
- 更新 `WebMvcConfig`，将 `/api/users/*/follow` 加入登录拦截。
- 扩展 `PublicUserProfileResponse` 和 `UserService.getPublicProfile`。
- 更新 `UserProfileController`，公开主页可选读取 Bearer Token 并返回 `followedByCurrentUser`。

### Phase 4：测试

- **Status:** complete
- 新增 `UserFollowServiceTest`。
- 新增 `UserFollowIntegrationTest`，验证真实 Mapper SQL 的关注/粉丝分页和统计。
- 新增 `UserFollowControllerTest`。
- 扩展 `UserServiceTest`、`UserProfileControllerTest` 和 `OpenApiControllerTest`。

### Phase 5：前端联动

- **Status:** complete
- 更新 `frontend/src/api/userApi.js`。
- 更新 `frontend/src/pages/PublicUserProfile.jsx`。
- 更新 `frontend/src/App.jsx` 和 `frontend/src/style.css`。

### Phase 6：文档收尾

- **Status:** complete
- 更新 `docs/graduation/毕业设计文档总览.md`。
- 更新 `docs/graduation/毕业设计进度.md`。
- 记录 M9 新接口、V10 迁移和验证结果。

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 附件和中文文档在当前 PowerShell 输出中显示乱码 | 按交接要求、文件路径和代码现状交叉核对，不重写既有中文文档 |
| 首次聚焦测试在沙箱内无法访问 Maven Central | 使用网络权限重跑后通过 |
| 前端首次补丁因旧文件中文上下文显示差异未匹配 | 改为小块补丁并替换 `PublicUserProfile.jsx` 实现 |
