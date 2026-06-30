# M5 举报与内容治理任务计划

## 目标

实现基础举报与内容治理能力：普通用户可举报已发布文章和有效评论，管理员可分页查看举报并处理为“成立”或“驳回”。本轮只做 M5，不做敏感词、自动审核、封禁处罚、复杂风控或后续模块。

## 范围边界

- 支持举报对象：文章、评论。
- 用户端：提交文章举报、提交评论举报、查看自己的举报记录。
- 管理端：按状态分页查看举报、处理举报成立、驳回举报。
- 处理语义：本轮优先只记录处理结果，不强制自动下架文章或删除评论，避免与 M2/M4 审核/通知语义强耦合。
- 权限方案：沿用现有 JWT + Interceptor，不引入 Spring Security、复杂 RBAC 或新中间件。
- 前端方案：沿用现有 React/Vite 简单结构，不引入 React Router、Redux、zustand 或 UI 组件库。

## 数据库迁移方案

- 新增 Flyway `V7__add_content_report.sql`。
- 新增 `content_report` 表，保存举报人、举报对象、对象作者、原因、状态、处理人、处理说明、创建/更新时间、处理时间。
- 不修改历史迁移 `V1` 至 `V6`。
- 重复待处理举报通过业务查询 `PENDING` 记录控制，不做全局唯一约束。

## 后端接口方案

- `POST /api/user/reports/article`：当前用户举报已发布且非本人文章。
- `POST /api/user/reports/comment`：当前用户举报有效评论，评论所属文章必须存在且为已发布。
- `GET /api/user/reports?pageNum=1&pageSize=10`：当前用户分页查看自己的举报记录。
- `GET /api/admin/reports?pageNum=1&pageSize=10&status=PENDING`：管理员按状态分页查看举报。
- `PUT /api/admin/reports/resolve`：管理员将待处理举报置为 `RESOLVED` 并记录处理说明。
- `PUT /api/admin/reports/reject`：管理员将待处理举报置为 `REJECTED` 并记录处理说明。

## 前端兼容方案

- 在文章详情页增加举报文章入口。
- 在评论列表每条评论增加举报评论入口。
- 在“我的”相关页面增加“我的举报”列表。
- 在管理员面板增加举报处理页签，支持查看、成立、驳回。
- 保持现有页面结构和样式密度，仅做必要表单、状态和提示。

## 测试清单

- 用户可以举报他人的已发布文章。
- 用户不能举报自己的文章。
- 用户不能举报非已发布文章。
- 用户不能重复提交同一对象的待处理举报。
- 已处理举报后允许再次举报同一对象。
- 用户可以举报他人的评论。
- 用户不能举报自己的评论。
- 评论所属文章不存在或非 `PUBLISHED` 时举报失败。
- 用户只能查看自己的举报记录。
- 管理员可按状态分页查看举报。
- 管理员可处理举报为 `RESOLVED` 并记录处理人和说明。
- 管理员可驳回举报为 `REJECTED` 并记录处理人和说明。
- 已处理举报不能重复处理。
- 普通用户不能访问管理员举报接口。
- 未登录用户不能提交举报或查看自己的举报记录。

## 验证命令

后端：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

前端：

```powershell
cd frontend
npm run build
```

## 风险和待确认事项

- 当前工作区已有 M1-M4 大量未提交改动，本轮必须只增量接入 M5，不能重置或覆盖已有实现。
- M5 不新增举报通知类型，处理结果通过举报列表体现。
- 如现有测试数据或拦截器路径与 M5 接口冲突，以现有项目模式为准做最小调整。

## 阶段状态

| 阶段 | 状态 | 验证 |
| --- | --- | --- |
| 接手检查与文档阅读 | 已完成 | git 检查、阅读 M1-M4 文档 |
| 后端模型、迁移、接口 | 已完成，已验证 | 编译和后端测试 |
| 后端测试补充 | 已完成，已验证 | `mvn test` |
| 前端接入 | 已完成，已验证 | `npm run build` |
| 文档收尾 | 已完成 | `git diff --check -- docs/graduation` |

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
| 嵌套 PowerShell 设置环境变量产生 `=root` 等噪声 | `powershell.exe -Command "$env:...; mvn -DskipTests compile"` | 编译实际通过；后续改用当前 PowerShell 直接设置 `$env:` 后运行 Maven |
