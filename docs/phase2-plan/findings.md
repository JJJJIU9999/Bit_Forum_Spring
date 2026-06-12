# Findings

## Source Document Summary

用户提供的二阶段路线文档核心目标：

- 增加 React 前端演示页面，让论坛项目可以完整现场演示。
- 增加管理员权限模块，让后端具备权限隔离和内容管理能力。
- 补充管理员相关自动化测试，覆盖越权、禁用、脱敏、删除等风险点。
- 更新 README、演示稿和简历描述，把项目成果转化为面试表达。

## Backend Upgrade Scope

- `user_info` 增加 `role` 和 `status`。
- 登录流程校验 `status`。
- `/api/admin/**` 使用管理员拦截器校验 JWT、用户状态和角色。
- 管理员支持文章管理、评论管理、用户启用/禁用。
- 普通用户仍保持现有权限：只能管理自己的文章。

## Frontend Upgrade Scope

- 新增 `frontend/`。
- React + Vite + Axios + TailwindCSS。
- 普通用户页面：登录、注册、文章列表、文章详情、发布文章、点赞、评论、热门文章。
- 管理员页面：文章管理、评论管理、用户管理、启用/禁用。

## Learning Priorities

- 重点理解后端权限校验，而不是只做页面。
- 重点理解禁用用户、旧 token、后端权限和前端显示控制的区别。
- 测试要证明风险点，不只是证明接口能返回 200。

## Phase 2 Implementation Notes

- `role` 当前使用字符串：`USER`、后续 `ADMIN`。
- `status` 当前使用整数：`1` 表示正常，`0` 表示禁用。
- 禁用用户登录返回通用登录失败，不区分“密码错”和“账号禁用”，减少账号状态泄露。
- 当前只在登录时校验 `status`。如果用户已持有旧 token，后续需要在 `AdminInterceptor` 或登录拦截链路中再次查库校验，才能让禁用立即影响已登录用户。
- 管理员接口现在统一走 `/api/admin/**`，由 `AdminInterceptor` 拦截。
- `AdminInterceptor` 不只解析 token，而是根据 token 中的 `userId` 再查数据库，确保 `role/status` 使用最新值。
- 管理员访问规则为：无 token 返回 401；有 token 但用户不存在、禁用或不是 ADMIN 返回 403；`role=ADMIN` 且 `status=1` 才放行。
- 管理员文章接口路径为 `/api/admin/article`，目前支持分页和删除。
- 管理员删除文章不校验作者归属，但必须和普通作者删除复用同一套清理流程。
- 统一清理流程为：删除文章评论 -> 删除文章记录 -> 删除 Redis 浏览量、点赞集合和热榜成员。
- 管理员评论接口路径为 `/api/admin/comment`，目前支持分页和删除。
- 评论删除只删除 comment 表记录，不调整文章热度；当前热榜分数只由浏览和点赞驱动。
- 删除不存在评论时返回业务失败，避免后台误以为删除成功。
- 管理员用户接口路径为 `/api/admin/user`，目前支持分页、禁用和启用。
- 用户列表必须走 `AdminUserResponse`，不能直接返回 `User` 实体，否则 password 会被序列化。
- 禁用用户只修改 `status`，不修改 `role`；管理员不能禁用自己，避免当前后台账号被锁死。
- 被禁用用户不能重新登录；旧 token 是否立即失效取决于对应接口是否像 `AdminInterceptor` 一样回查数据库状态。
- 后端收口检查确认：新增管理员接口已统一记录到 README，路径保持 `/api/admin/**`，后续 React 前端可以直接按 README 的接口表对接。
- 管理员账号创建采用方式 A：先调用注册接口创建普通用户，再执行 SQL 更新 `role='ADMIN'`、`status=1`。这样不会绕过 BCrypt 密码加密流程，也不需要新增初始化代码。
- 敏感信息扫描结果：当前只发现 `.env.example` 中的占位值和文档/测试里的示例密码，没有发现真实密钥或真实数据库密码。
