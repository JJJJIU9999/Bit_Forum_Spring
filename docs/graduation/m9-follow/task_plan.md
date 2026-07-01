# M9 关注与粉丝任务计划

## M9 目标

在现有比特论坛社区交流平台上补充基础用户关注关系，支持登录用户关注其他用户、取消关注、查看关注列表和粉丝列表，并在 M7 公开用户主页中展示关注数、粉丝数以及当前登录用户是否已关注该主页用户。

## 当前接手状态

- 当前分支：`graduation-design`
- 最新提交：`5321630 fix(frontend): align admin table action borders`
- 当前工作区：非干净状态，M1-M8 均有未提交改动，必须保留
- 当前最新 Flyway 迁移：`V9__add_user_profile_fields.sql`
- 本轮只做 M9，不做 M10-M12 或其他后续增强
- 不自动提交，不 stash，不回滚，不覆盖既有未提交改动

## 范围边界

- 只实现基础关注与粉丝能力。
- 不做私信、动态流、推荐算法、黑名单、关注分组、复杂隐私设置、消息推送增强、文件上传、定时任务或 Actuator。
- 不引入 Spring Security、Spring Cloud、Kafka、Elasticsearch 或复杂 RBAC。
- 不引入 React Router、Redux、Zustand 或复杂 UI 组件库。
- 不修改历史 Flyway 脚本 `V1` 到 `V9`。
- 不改变 M2 审核/下架、M5 举报处理、M6 dashboard、M7 用户主页既有字段含义。

## 数据库迁移方案

新增迁移：

```text
src/main/resources/db/migration/V10__add_user_follow.sql
```

新增表：

```text
user_follow
```

字段和约束：

- `id BIGINT PRIMARY KEY AUTO_INCREMENT`
- `follower_id BIGINT NOT NULL`：关注者用户 ID
- `following_id BIGINT NOT NULL`：被关注用户 ID
- `created_at DATETIME DEFAULT CURRENT_TIMESTAMP`
- 唯一约束：`uk_user_follow_follower_following (follower_id, following_id)`
- 索引：`idx_user_follow_follower_id (follower_id)`
- 索引：`idx_user_follow_following_id (following_id)`

业务约束由 Service 层保证：

- 用户不能关注自己。
- 同一用户不能重复关注同一目标用户。
- 取消关注只能取消自己的关注关系。
- 目标用户不存在时返回明确业务错误。
- 禁用用户先沿用 M7 简单策略：公开主页可查看，关注操作只做用户存在性校验和当前登录用户拦截器校验；如后续限制目标禁用用户，需要另行记录原因。

## 后端接口方案

新增：

- `UserFollow` Entity
- `UserFollowMapper`
- `UserFollowService`
- `UserFollowController`
- `FollowUserResponse`
- `FollowStatsResponse`

接口：

- `POST /api/users/{userId}/follow`：当前登录用户关注目标用户，需要登录。
- `DELETE /api/users/{userId}/follow`：当前登录用户取消关注目标用户，需要登录。
- `GET /api/users/{userId}/followers?pageNum=1&pageSize=10`：公开分页查看粉丝列表。
- `GET /api/users/{userId}/following?pageNum=1&pageSize=10`：公开分页查看关注列表。
- `GET /api/users/{userId}/follow-stats`：公开查询关注数、粉丝数、当前登录用户是否已关注；若能自然合并到 M7 公开主页，也可作为辅助接口保留。

## JWT 登录拦截方案

继续使用现有 `LoginInterceptor` 和 `WebMvcConfig` 显式路径配置，不引入 Spring Security。

需要加入登录拦截的写接口：

- `/api/users/*/follow`

公开接口不加入登录拦截：

- `/api/users/{userId}/followers`
- `/api/users/{userId}/following`
- `/api/users/{userId}/follow-stats`
- `/api/users/{userId}/profile`

为了在公开主页中判断当前登录用户是否已关注，可在 `UserProfileController` 中可选读取 `Authorization: Bearer <token>`，解析成功时计算 `followedByCurrentUser`，未登录或 token 无效时返回 `false`，不改变公开访问语义。

## 与 M7 用户主页的联动方案

扩展 `PublicUserProfileResponse`，只新增字段，不改变已有字段含义：

- `followingCount`
- `followerCount`
- `followedByCurrentUser`

扩展 `UserService.getPublicProfile` 或新增重载，让公开主页可在有当前用户 ID 时返回关注状态。未登录访问时 `followedByCurrentUser=false`。

## 前端最小改动方案

- 在 `frontend/src/api/userApi.js` 增加关注、取消关注、关注列表、粉丝列表 API 封装。
- 在 `frontend/src/pages/PublicUserProfile.jsx` 展示关注数、粉丝数和关注/已关注/取消关注按钮。
- 当前登录用户访问自己的主页时不显示可操作关注按钮。
- 在公开用户主页中增加简单的关注列表和粉丝列表视图，继续使用 `activePage` 字符串切换方式，不引入路由库。
- 仅补充必要 CSS，保留现有布局和视觉密度。

## 测试清单

- 不能关注自己。
- 不能重复关注同一用户。
- 可以取消自己的关注。
- 未关注时取消关注返回合理业务错误。
- 粉丝数和关注数统计正确。
- 粉丝列表分页正确。
- 关注列表分页正确。
- 公开用户主页返回关注统计字段。
- 未登录不能关注或取消关注。
- `/v3/api-docs` 仍可访问，新接口不破坏 M8。
- 完整 `mvn test` 通过。
- 前端 `npm run build` 通过。

## 验证命令

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn "-Dtest=UserFollowServiceTest,UserFollowIntegrationTest,UserFollowControllerTest,UserServiceTest,UserProfileControllerTest,OpenApiControllerTest" test
```

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile
```

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

```powershell
cd frontend
npm run build
```

## 风险和待确认事项

- 当前 M1-M8 均未提交，M9 必须只做增量修改。
- `LoginInterceptor` 目前只负责显式路径，新增写接口必须手动加入。
- 公开主页需要在不破坏匿名访问的前提下读取可选 token。
- `user_follow.created_at` 命名不同于现有多数表的 `create_time`，按用户需求采用 `created_at`，实体字段需明确映射。
- 关注列表/粉丝列表返回公开用户信息，不返回 `password`。

## 阶段计划

### Phase 1：接手检查与文档创建

- [x] 执行 git 分支、状态、最新提交、diff 统计检查。
- [x] 核对最新 Flyway 迁移为 `V9__add_user_profile_fields.sql`。
- [x] 阅读毕业设计总览、进度、M1-M8 计划/进度和 audit-fixes 记录。
- [x] 创建 M9 文档目录和三份文档。

### Phase 2：后端模型与迁移

- [x] 新增 V10 迁移。
- [x] 新增 Entity / Mapper / DTO。
- [x] 实现 UserFollowService。

### Phase 3：后端接口与 M7 联动

- [x] 新增 UserFollowController 和 OpenAPI 注解。
- [x] 更新 WebMvcConfig 登录拦截路径。
- [x] 扩展 PublicUserProfileResponse 和公开主页返回。

### Phase 4：后端测试

- [x] 新增 UserFollowServiceTest。
- [x] 新增 UserFollowControllerTest。
- [x] 扩展 UserProfileControllerTest 和 OpenApiControllerTest。

### Phase 5：前端最小联动

- [x] 扩展 userApi.js。
- [x] 扩展 PublicUserProfile.jsx。
- [x] 补充必要 CSS。

### Phase 6：验证与文档收尾

- [x] 运行聚焦测试。
- [x] 运行后端编译。
- [x] 运行完整后端测试。
- [x] 运行前端构建。
- [x] 更新总览、进度和 M9 progress。

## 错误记录

| 问题 | 处理 |
| --- | --- |
| 附件和部分中文文档在 PowerShell 输出中显示为乱码 | 结合文件路径、已知交接内容和当前代码状态继续核对，避免改动既有中文文案语义 |
| 首次聚焦测试在沙箱内无法访问 Maven Central | 使用网络权限重跑同一条聚焦测试后通过 |
| 前端首次补丁因旧文件中文上下文显示差异未匹配 | 改为小块补丁并替换 `PublicUserProfile.jsx` 实现 |
