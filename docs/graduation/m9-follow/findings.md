# M9 关注与粉丝调研记录

## 接手结论

- 当前分支为 `graduation-design`，不是 `main`。
- 最新提交为 `5321630 fix(frontend): align admin table action borders`。
- 工作区包含 M1-M8 未提交改动，M9 必须保留这些改动并增量实现。
- 当前最新 Flyway 迁移为 `V9__add_user_profile_fields.sql`，M9 如需数据库变更只能新增 `V10__add_user_follow.sql`。

## 已读文档结论

- M1-M8 已完成并验证，但仍未提交。
- M7 新增了用户公开主页、当前用户资料维护和公开文章列表，是 M9 的联动入口。
- M8 新增 springdoc OpenAPI 配置和 controller/DTO 注解，M9 新增接口也应补充基础注解。
- audit-fixes 中 V8 来源是举报待处理唯一约束，不属于 M9，不应重做。

## 代码现状

- `WebMvcConfig` 通过 `addPathPatterns` 显式配置登录拦截路径。
- `LoginInterceptor` 会解析 Bearer Token，校验用户存在且启用，并把 `userId` 写入 request attribute。
- `UserProfileController` 当前提供：
  - `GET /api/user/profile`
  - `PUT /api/user/profile`
  - `GET /api/users/{userId}/profile`
  - `GET /api/users/{userId}/articles`
- `UserService.getPublicProfile(userId)` 当前返回公开用户资料、已发布文章数、收藏数。
- `PublicUserProfileResponse` 当前不包含关注统计，需要增量字段。
- 前端 `PublicUserProfile.jsx` 当前拉取公开资料和公开文章列表，适合在该页面最小补充关注按钮和列表。

## 设计决策

| 决策 | 理由 |
| --- | --- |
| 新增独立 `user_follow` 表 | 关注关系是用户间多对多关系，独立表最清晰 |
| 使用 Service 层校验自关注和重复关注 | 保持业务错误信息明确，同时数据库唯一约束兜底 |
| 列表接口公开访问 | 符合 M9 要求，且不改变 M7 公开主页语义 |
| 写接口加入 LoginInterceptor | 延续现有 JWT + Interceptor 权限方案 |
| 公开主页可选解析 token | 匿名仍可访问，登录时可得到 `followedByCurrentUser` |
| 不新增通知 | 用户要求不做消息推送增强，避免扩展 M4 语义 |

## 风险

- 当前工作区已有大量未提交文件，修改同名文件时必须只做 M9 增量。
- 公开主页 token 可选解析不能让无效 token 破坏公开访问。
- controller 测试使用 `@MockitoBean`，需要 mock `findById` 以通过登录拦截器。
- 文档中文在当前 shell 输出中显示乱码，编辑时需避免大范围重写既有文档。
