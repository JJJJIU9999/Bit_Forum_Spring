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
