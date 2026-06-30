# M5 举报与内容治理进度记录

## 当前状态

M5 已完成举报与内容治理的后端、前端、测试补充和文档更新，尚未提交。

## 2026-06-29

- 创建 M5 文档目录和三份规划文件。
- 记录接手检查结果：当前分支为 `graduation-design`，最近提交为 `2061d18 Merge branch 'phase-2-react-frontend'`，工作区存在 M1-M4 未提交改动。
- 已阅读毕业设计总览、毕业设计进度和 M1-M4 任务文档，确认 M5 只做举报与处理记录，不改变 M1-M4 已完成语义。
- 已阅读后端 Controller、Service、Entity、Mapper、Interceptor 和部分测试，确认 M5 可按现有 `Result<T>` + MyBatis-Plus + 显式拦截路径模式实现。
- 新增 `V7__add_content_report.sql`。
- 新增 `ContentReport`、`ContentReportMapper`、举报请求 DTO、处理请求 DTO、`ContentReportService`、`UserReportController`、`AdminReportController`。
- `WebMvcConfig` 已加入 `/api/user/reports` 和 `/api/user/reports/**` 登录拦截。
- 新增 `ContentReportServiceTest`、`UserReportControllerTest`、`AdminReportControllerTest`。
- 后端编译 `mvn -DskipTests compile` 已通过；嵌套 PowerShell 写法产生过环境变量展开噪声，后续改用直接 PowerShell 命令设置环境变量。
- 聚焦测试 `mvn "-Dtest=ContentReportServiceTest,UserReportControllerTest,AdminReportControllerTest" test` 已通过，21 个测试全部通过。
- 前端新增 `reportApi.js`，文章详情页增加举报文章/评论入口，“我的”页增加“我的举报”，管理员面板增加“举报处理”。
- 前端 `npm run build` 已通过，Vite 构建 97 个模块。
- 完整后端测试 `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` 已通过，106 个测试全部通过。
- 更新毕业设计进度文档。

## 新增接口

- `POST /api/user/reports/article`
- `POST /api/user/reports/comment`
- `GET /api/user/reports?pageNum=1&pageSize=10`
- `GET /api/admin/reports?pageNum=1&pageSize=10&status=PENDING`
- `PUT /api/admin/reports/resolve`
- `PUT /api/admin/reports/reject`

## 验证记录

| 命令 | 结果 |
| --- | --- |
| `mvn -DskipTests compile` | 通过，68 个源码文件编译成功 |
| `mvn "-Dtest=ContentReportServiceTest,UserReportControllerTest,AdminReportControllerTest" test` | 通过，21 个测试全部通过 |
| `npm run build` | 通过，Vite 构建 97 个模块 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，106 个测试全部通过 |

## 风险与待确认

- M1-M5 仍全部处于未提交状态，提交前需要用户确认。
- M5 当前只记录举报处理结果，不自动下架文章或删除评论；如果后续希望“举报成立后联动治理动作”，应在后续模块或单独任务中明确规则。
- 本轮没有新增举报通知类型，举报人通过“我的举报”查看处理状态。
