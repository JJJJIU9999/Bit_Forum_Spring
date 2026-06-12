# Task Plan

## Goal

推进比特论坛二阶段提升：先完成后端管理员权限基础，再补管理员接口、测试和 React 演示页面。

## Phases

| Phase | Status | Output |
| --- | --- | --- |
| 1. 生成二阶段计划书 | complete | 已生成 `docs/phase2-plan/项目二阶段提升计划.md` |
| 2. 新增 role/status | complete | 新增 V2 迁移、User 字段、注册默认值、登录禁用校验和测试 |
| 3. 新增 AdminInterceptor | complete | 保护 `/api/admin/**`，校验登录、status 和 role |
| 4. 管理员文章管理接口 | complete | 支持管理员分页查看文章、删除任意文章，并复用统一清理逻辑 |
| 5. 管理员评论管理接口 | complete | 支持管理员分页查看评论、删除任意评论 |
| 6. 管理员用户接口 | complete | 支持用户分页、启用和禁用，用户列表不返回 password |
| 7. 管理员测试补强 | complete | 已覆盖越权、禁用、自禁用、脱敏和删除清理 |
| 8. 后端收口检查 | complete | 已补充 README 管理员接口和方式 A 管理员账号创建流程 |
| 9. React 前端演示 | pending | 增加普通用户端和管理员端演示页面 |

## Decisions

- 二阶段从后端权限基础开始，不先做 React。
- 管理员账号初始化采用方式 A：先注册普通用户，再手动 SQL 更新 `role=ADMIN`、`status=1`，不在代码里硬编码管理员账号。
- 登录时对 `status=0` 用户返回通用登录失败，避免暴露账号状态。

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 路径不存在 | 使用默认 `.codex/skills` 路径运行 | 本环境技能位于 `.agents/skills`，本次无历史规划文件，继续执行 |
| 首次 `mvn test` 失败 | 沙盒内 Maven 无法访问 Central，提示 `Permission denied: getsockopt` | 使用已批准的提权 Maven 测试命令重跑，通过 |
| 嵌套 PowerShell 跑 `mvn test` 失败 | `powershell -Command` 中再次调用 `powershell -Command`，环境变量没有正确传给 Maven | 改为在当前 PowerShell 中直接设置 `$env:*` 后运行 `mvn test`，测试通过 |
