# Progress

## 2026-06-11

- 读取用户粘贴的二阶段提升路线文档。
- 首次默认编码读取出现乱码，已用 UTF-8 重新读取成功。
- 确认当前仓库文档目录位于 `docs/project`、`docs/interview`、`docs/learning`。
- 开始生成 `docs/project/项目二阶段提升计划书.md`。
- 已生成 `docs/project/项目二阶段提升计划书.md`。
- 已校验文件可读取，当前变更仅包含文档和规划记录文件，没有修改业务代码。
- 开始二阶段第一个实现任务：新增 `role/status`。
- 新增 `V2__add_user_role_status.sql`，为 `user_info` 增加 `role` 和 `status` 字段。
- 更新 `User` 实体，新增 `role`、`status` 字段。
- 更新 `UserService.register`，新注册用户默认 `role=USER`、`status=1`。
- 更新 `UserService.login`，只有 `status=1` 且密码匹配才允许登录。
- 新增 `UserServiceTest`，覆盖注册默认角色/状态，以及禁用用户不能登录。
- 首次 `mvn test` 因沙盒网络限制无法解析 Maven parent POM；提权重跑后通过。
- 最终验证：`mvn test` 通过，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。
- 继续完成二阶段管理员权限拦截任务。
- 新增 `AdminInterceptor`，拦截 `/api/admin/**`，校验 Authorization、JWT、数据库中的用户状态和角色。
- 新增 `AdminController.health`，作为最小管理员接口，用于验证权限链路。
- 更新 `WebMvcConfig`，注册 `AdminInterceptor`。
- 更新 `UserService`，暴露 `ROLE_ADMIN`、`STATUS_ENABLED` 常量，并新增 `findById` 给管理员拦截器回查用户。
- 新增 `AdminControllerTest`，覆盖未登录、普通用户、禁用管理员、正常管理员四种访问结果。
- 验证结果：`mvn test` 通过，`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。
- 继续完成管理员文章管理接口。
- 新增 `AdminArticleController`，提供 `GET /api/admin/article/page` 和 `DELETE /api/admin/article/delete?articleId=...`。
- 更新 `ArticleService`，新增 `deleteByAdmin`，并抽取 `deleteArticleWithRelatedData`，让普通作者删除和管理员删除复用同一套评论/文章/Redis 清理逻辑。
- 新增 `AdminArticleControllerTest`，覆盖管理员分页、管理员删除、文章不存在、普通用户越权访问管理员删除接口。
- 更新 `ArticleServiceTest`，新增管理员删除文章时清理评论和 Redis 的服务层测试。
- 将 `ArticleServiceTest` 中的 `@MockBean` 替换为 `@MockitoBean`，避免 Spring Boot 3.4 的废弃警告。
- 验证结果：`mvn test` 通过，`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- 继续完成管理员评论管理接口。
- 新增 `AdminCommentController`，提供 `GET /api/admin/comment/page` 和 `DELETE /api/admin/comment/delete?commentId=...`。
- 更新 `CommentService`，新增 `pageComments` 和 `deleteByAdmin`。
- 新增 `AdminCommentControllerTest`，覆盖管理员分页、管理员删除、评论不存在、普通用户越权访问管理员评论删除接口。
- 更新 `CommentServiceTest`，覆盖管理员删除已有评论，以及删除不存在评论返回 false。
- 验证结果：`mvn test` 通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- 继续完成管理员用户管理接口。
- 新增 `AdminUserResponse`，后台用户列表只返回 userId、username、avatar、role、status、createTime，不返回 password。
- 新增 `AdminUserController`，提供 `GET /api/admin/user/page`、`PUT /api/admin/user/disable`、`PUT /api/admin/user/enable`。
- 更新 `UserService`，新增 `pageUsers`、`disableUser`、`enableUser` 和 `UserStatusUpdateResult`。
- 禁用用户时使用 `AdminInterceptor` 写入的当前管理员 userId，禁止管理员禁用自己。
- 更新 `UserServiceTest`，覆盖用户列表 DTO、禁用自己、禁用普通用户、启用用户。
- 新增 `AdminUserControllerTest`，覆盖用户列表脱敏、禁用、启用、自禁用保护、普通用户越权访问。
- 验证结果：`mvn test` 通过，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。

## 2026-06-12

- 开始后端收口检查，暂不启动 React 前端。
- 使用实际技能路径 `C:\Users\10603\.agents\skills\planning-with-files\scripts\session-catchup.py` 完成 session catchup；默认 `.codex/skills` 路径不存在的问题已记录在 `task_plan.md`。
- 核对 Controller 路由，确认管理员接口集中在 `/api/admin/**`，普通用户接口保持原路径。
- 核对 `WebMvcConfig` 和 `AdminInterceptor`，确认管理员接口会校验 JWT、数据库中的 `status` 和 `role`。
- 核对 `V2__add_user_role_status.sql`、`UserService` 和 `AdminUserResponse`，确认 `role/status`、禁用登录、用户列表脱敏逻辑已覆盖。
- 扫描敏感信息，当前只发现 `.env.example` 占位值和文档/测试示例密码，没有发现真实密钥。
- 更新 `README.md`，补充管理员接口总览和方式 A 管理员账号创建流程。
- 更新二阶段计划记录，把“后端收口检查”标记为完成。
- 首次收口测试因为嵌套 PowerShell 导致环境变量没有正确传入 Maven，`JwtUtil` 初始化失败；已改用当前 PowerShell 直接设置环境变量。
- 最终验证：`mvn test` 通过，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。
