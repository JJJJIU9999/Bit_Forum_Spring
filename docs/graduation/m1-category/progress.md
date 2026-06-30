# M1 板块分类进度

## 当前状态
M1 板块分类已完成实现和验证。

## 已完成
- 创建并切换到 `graduation-design` 分支。
- 创建 M1 计划文件。
- 阅读现有文章 Controller/Service/DTO/Entity。
- 阅读管理员 Controller/Interceptor 和相关测试。
- 阅读前端文章 API、管理员 API、文章列表和发布页。
- 新增 `category` 表和 `article.category_id` 的 Flyway 迁移，已有文章迁移到默认板块。
- 新增 Category Entity、Mapper、DTO、Service、公共接口和管理员接口。
- 文章发布必须传入启用板块，文章分页支持按板块筛选，文章响应补充 `categoryName`。
- 管理员可分页查看、新建、修改、启用、禁用和删除无文章板块。
- 前端新增板块 API，文章列表支持板块筛选，发布文章支持选择板块，管理员面板新增板块管理页签。
- 更新前端样式，补齐板块筛选、选择框、管理员内联表单和多操作按钮布局。

## 验证记录
| 命令 | 结果 |
| --- | --- |
| `mvn -DskipTests compile` | 通过 |
| `mvn -Dtest=CategoryServiceTest,AdminCategoryControllerTest,ArticleServiceTest test` | PowerShell 将未加引号的逗号解析为参数列表，命令未执行测试；改用引号重跑。 |
| `mvn "-Dtest=CategoryServiceTest,AdminCategoryControllerTest,ArticleServiceTest" test` | 通过，12 个测试全部通过；测试启动时 RabbitMQ 5672 未运行，仍有连接拒绝日志。 |
| `npm run build` | 通过，Vite 成功构建前端产物。 |
| `C:\windows\System32\WindowsPowerShell\v1.0\powershell.exe -Command "$env:...; mvn test"` | 失败，外层 PowerShell 提前展开 `$env:JWT_SECRET`，导致 `JwtUtil` 初始化失败。 |
| `$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test` | 通过，46 个测试全部通过。 |

## 待办
- 进入 M2 前先确认本地 MySQL、Redis、RabbitMQ 仍处于可用状态。
- 后续模块如果继续扩展文章发布流程，需要保留“禁用板块不能发布新文章，已有文章仍可浏览”的语义。
