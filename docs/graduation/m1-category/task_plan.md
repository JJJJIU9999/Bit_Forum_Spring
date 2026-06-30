# M1 板块分类执行计划

## 目标
实现板块分类模块，让文章具备所属板块，支持公共板块列表、按板块筛选文章、发布文章选择板块，以及管理员板块管理。

## 当前阶段
阶段 5：文档与验证

## 范围边界
- 允许修改：M1 相关后端代码、测试、前端演示代码、Flyway 新迁移、毕业设计文档。
- 不修改历史 Flyway 脚本。
- 不引入 Spring Security、Spring Cloud、Elasticsearch、Kafka、复杂 RBAC、Redux、Zustand。
- 保持现有 JWT + Interceptor 权限方案。
- 保持现有简单 React/Vite 前端结构。

## 阶段

### 阶段 1：项目盘点
- [x] 阅读现有文章、管理员、拦截器、DTO、Service、测试和前端页面。
- [x] 确认新增字段和接口如何兼容现有代码。
- 状态：已完成

### 阶段 2：后端实现
- [x] 新增 Flyway migration。
- [x] 新增 Category Entity/Mapper/DTO/Service。
- [x] 新增公共 CategoryController。
- [x] 新增 AdminCategoryController。
- [x] 修改 Article 发布和分页查询支持 `categoryId`。
- [x] 文章响应补充板块字段。
- 状态：已完成

### 阶段 3：测试
- [x] 新增 CategoryServiceTest。
- [x] 新增 AdminCategoryControllerTest。
- [x] 补充 ArticleServiceTest 中板块校验。
- [x] 尽量运行与 M1 相关的后端测试。
- 状态：已完成

### 阶段 4：前端
- [x] 新增 category API。
- [x] 文章列表支持板块筛选。
- [x] 发布文章支持选择板块。
- [x] 管理员面板支持板块管理。
- 状态：已完成

### 阶段 5：文档与验证
- [x] 更新 M1 计划、调查和进度文档。
- [x] 运行 `mvn test` 或说明环境阻塞。
- [x] 运行 `npm run build`。
- [x] 汇总改动和待确认事项。
- 状态：已完成

## 决策记录
| 决策 | 原因 |
| --- | --- |
| 使用 `category` 表和 `article.category_id` | 符合用户给定 M1 方案，且对现有文章结构影响最小。 |
| 保留现有 `/api/article/publish` 路径 | M1 不做审核语义调整，避免提前进入 M2。 |

## 错误记录
| 问题 | 处理 |
| --- | --- |
| PowerShell 解析 `-Dtest=...,...` 失败 | 用引号包住 Maven 测试选择器后重跑。 |
| 嵌套 PowerShell 命令提前展开 `$env:JWT_SECRET` | 改为在当前 PowerShell 会话内设置环境变量并重跑 `mvn test`。 |
