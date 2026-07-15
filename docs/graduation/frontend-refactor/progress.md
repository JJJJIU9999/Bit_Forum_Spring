# 前端整体重构进度

## 2026-07-12

- 新建工作分支 `feat/frontend-refactor`，基线为 `d4d4503`。
- 确认工作区仅有两份未跟踪学习文档，保持不动。
- 建立前端重构任务计划和审查结论。
- 下一步：更新毕业设计顶层状态文档，然后实现登录角色字段、正式路由和基础视觉系统。

## 验证记录

| 项目 | 结果 |
| --- | --- |
| 分支创建 | `feat/frontend-refactor` 已创建 |
| 既有未跟踪文件 | 保留，未修改 |

## 实施完成记录

- 登录接口的 `LoginResponse` 新增 `role`，前端以它控制管理后台入口；后端 `AdminInterceptor` 仍是最终权限校验。
- 由内存视图切换改为 React Router：社区、文章、用户主页、登录注册、发布、通知、个人中心和管理后台均可由 URL 直接打开；受保护页面使用 `redirectTo` 返回原地址。
- 全站改为暖色编辑社区视觉：纸张米白背景、森林绿主操作、砖红强调色；社区首页使用文章主栏与侧栏，管理端使用桌面侧栏和移动抽屉。
- 修复 ArticleCard 子内容重复渲染；为页面增加跳过导航、可见焦点、live region、正确的分页导航语义，并强化确认弹窗的键盘体验。
- 新增 `react-router`、`lucide-react`、Vitest 与 Testing Library；未引入 UI 组件库、全局状态库、图表库或数据库迁移。

## 最终验证

| 命令 | 结果 |
| --- | --- |
| `npm test` | 通过，3 个测试文件、4 个测试。 |
| `npm run build` | 通过，Vite 生产构建完成（1884 个模块）。 |
| `mvn -Dtest=UserControllerTest test` | 通过，3 个测试；覆盖登录响应中的 `role`。 |
| `mvn test` | 通过，176 个测试、0 failure、0 error。 |
| `git diff --check` | 通过。 |

未提交、未推送；两份既有未跟踪学习文档继续保留。
