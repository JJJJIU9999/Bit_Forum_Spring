# M2 文章审核流进度

## 当前状态
M2 已完成实现和验证，尚未提交。

## 已完成
- 执行 `git branch --show-current`，确认当前分支为 `graduation-design`。
- 执行 `git status --short`，确认工作区存在 M1 未提交改动，需要保留。
- 执行 `git log -1 --oneline`，最近提交为 `2061d18 Merge branch 'phase-2-react-frontend'`。
- 执行 `git diff --stat`，记录 M1 已跟踪文件变更概况。
- 阅读毕业设计总览、毕业设计进度、M1 task_plan、findings、progress。
- 创建 `docs/graduation/m2-article-audit/`。
- 创建 M2 `task_plan.md`、`findings.md`、`progress.md`。
- 阅读 Article、ArticleService、ArticleController、AdminArticleController、ArticlePublishRequest、ArticleServiceTest、AdminArticleControllerTest。
- 初步确定 `/api/article/publish` 兼容为“提交审核”，审核通过时再发送 RabbitMQ 消息。
- 新增 `V4__add_article_audit_status.sql`。
- 新增文章审核记录 Entity / Mapper 和 M2 请求 DTO。
- 完成后端服务层状态流转、用户接口、管理员接口和拦截器路径改造。
- 补充 ArticleServiceTest 和 AdminArticleControllerTest。
- 完成前端投稿页文案调整、保存草稿按钮、“我的文章”状态列表和管理员审核/下架操作。
- 更新 README、毕业设计进度和 M2 文档。

## 验证记录
| 命令 | 结果 |
| --- | --- |
| `git branch --show-current` | `graduation-design` |
| `git status --short` | 存在 M1 未提交改动和新增文件 |
| `git log -1 --oneline` | `2061d18 Merge branch 'phase-2-react-frontend'` |
| `git diff --stat` | 12 个已跟踪文件变更，约 528 行新增、137 行删除 |
| `python "$env:USERPROFILE\.codex\skills\planning-with-files\scripts\session-catchup.py" (Get-Location)` | 失败，`.codex` 路径下脚本不存在 |
| `python "$env:USERPROFILE\.agents\skills\planning-with-files\scripts\session-catchup.py" (Get-Location)` | 通过，未输出未同步上下文 |
| `mvn -DskipTests compile` | 首次在沙箱内失败，Maven Central 网络访问被拦截 |
| `mvn -DskipTests compile` | 提权后通过，53 个源码文件编译成功 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 首次失败，测试中 `RabbitTemplate.convertAndSend` 的 Mockito matcher 命中重载歧义 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，58 个测试全部通过 |
| `npm run build` | 通过，Vite 构建 94 个模块 |

## 错误处理记录
| 问题 | 处理 |
| --- | --- |
| Maven Central 网络访问被沙箱拦截 | 使用提权命令重跑编译 |
| `convertAndSend` 测试 matcher 重载歧义 | 将第三个 matcher 从 `any()` 改为 `any(ArticlePublishMessage.class)` |

## 下一步
- 等待用户确认是否提交。
- 后续进入 M3 前继续保留 M1、M2 未提交改动，除非用户明确要求提交。
