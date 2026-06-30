# M6 管理员数据统计看板任务计划

## M6 目标

实现管理员数据统计看板，提供社区系统核心运营数据的后台汇总展示，补齐毕业设计中的管理端可视化统计能力和社区治理闭环展示能力。

本轮只做基础统计和简单看板，不做复杂 BI、大屏图表、定时统计任务、导出报表、权限菜单系统或后续 M7/M8 功能。

## 范围边界

- 只做 M6：管理员数据统计看板。
- 保留 M1、M2、M3、M4、M5 未提交改动，不删除、不回滚、不覆盖、不 stash。
- 不在 `main` 分支开发，当前分支保持为 `graduation-design`。
- 不修改历史 Flyway 脚本 `V1` 至 `V7`。
- 优先使用现有表和 Redis 实时聚合统计；除非确有必要，不新增统计缓存表。
- 不引入 Spring Security、Spring Cloud、Kafka、复杂 RBAC、Redux、zustand、React Router、ECharts 或复杂 UI 组件库。
- 继续使用现有 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation 和 React/Vite 简单结构。
- 不改变 M2 审核/下架语义，不改变 M5 举报处理只记录结果的语义。

## 统计口径

### 用户统计

- `total`：`user_info` 表总数。
- `normalUsers`：`role = USER` 用户数。
- `admins`：`role = ADMIN` 用户数。
- `enabled`：`status = 1` 用户数。
- `disabled`：`status = 0` 用户数。

### 文章统计

- `total`：`article` 表总数。
- `draft`、`pending`、`published`、`rejected`、`offline`：按 `article.status` 分组统计。
- `todayCreated`：`create_time` 大于等于当天零点的文章数。
- `last7DaysCreated`：`create_time` 大于等于最近 7 天起点的文章数。

### 评论统计

- `total`：`comment` 表总数。
- `todayCreated`：当天新增评论数。
- `last7DaysCreated`：最近 7 天新增评论数。

### 板块统计

- `total`：`category` 表总数。
- `enabled`：`status = 1` 板块数。
- `disabled`：`status = 0` 板块数。

### 收藏与通知统计

- `favoriteStats.total`：`article_favorite` 表总数。
- `notificationStats.total`：`notification` 表总数。
- `notificationStats.unread`：`read_status = 0` 通知数。

### 举报治理统计

- `total`：`content_report` 表总数。
- `pending`、`resolved`、`rejected`：按 `content_report.status` 统计。
- `todayCreated`：当天新增举报数。
- `last7DaysCreated`：最近 7 天新增举报数。

### 热门文章

- 优先复用现有 `ArticleService` / `RedisService` 的热门文章逻辑。
- 如果 Redis 不可用或无热门数据，返回空数组，不影响其他统计返回。
- 热门文章只展示后端现有热门逻辑允许展示的文章，不绕过 M2 的 `PUBLISHED` 限制。

## 后端接口方案

- 新增接口：`GET /api/admin/dashboard/summary`
- 路径位于 `/api/admin/**`，由现有 `AdminInterceptor` 保护。
- 普通用户和未登录用户不能访问。
- 返回统一 `Result<AdminDashboardSummaryResponse>`。
- 不返回 `password` 等敏感字段。
- 统计失败时由 Controller 捕获异常并返回明确错误信息。

计划新增：

- `src/main/java/com/bitforum/dto/AdminDashboardSummaryResponse.java`
- `src/main/java/com/bitforum/dto/DashboardUserStats.java`
- `src/main/java/com/bitforum/dto/DashboardArticleStats.java`
- `src/main/java/com/bitforum/dto/DashboardCommentStats.java`
- `src/main/java/com/bitforum/dto/DashboardCategoryStats.java`
- `src/main/java/com/bitforum/dto/DashboardFavoriteStats.java`
- `src/main/java/com/bitforum/dto/DashboardNotificationStats.java`
- `src/main/java/com/bitforum/dto/DashboardReportStats.java`
- `src/main/java/com/bitforum/dto/DashboardHotArticle.java`
- `src/main/java/com/bitforum/service/AdminDashboardService.java`
- `src/main/java/com/bitforum/controller/AdminDashboardController.java`

## 前端兼容方案

- 在现有 `AdminPanel.jsx` 中新增“数据看板”页签。
- 在 `adminApi.js` 中新增 dashboard summary API 封装。
- 使用现有 CSS 风格实现统计卡片、状态分组和热门文章列表。
- 不引入 React Router、Redux、zustand、ECharts 或 UI 组件库。
- 管理员未登录或无权限时沿用现有错误提示方式。
- 不重写管理员面板整体布局。

## 测试清单

- [x] 管理员可以获取 dashboard summary。
- [x] 普通用户不能访问 dashboard 接口。
- [x] 未登录用户不能访问 dashboard 接口。
- [x] 用户统计正确。
- [x] 文章状态统计正确。
- [x] 今日和最近 7 天文章统计正确。
- [x] 评论统计正确。
- [x] 板块状态统计正确。
- [x] 收藏总数统计正确。
- [x] 通知总数和未读数统计正确。
- [x] 举报状态统计正确。
- [x] Redis 热门文章为空时接口仍可正常返回。
- [x] 前端管理员面板能展示数据看板并通过构建。

## 验证命令

后端聚焦测试：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn "-Dtest=AdminDashboardControllerTest,AdminDashboardServiceTest" test
```

后端编译：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn -DskipTests compile
```

前端构建：

```powershell
cd frontend
npm run build
```

后端完整测试：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

## 风险和待确认事项

- 当前 M1-M5 均未提交，M6 必须增量实现，提交前需要用户确认。
- 统计使用实时 count，在大数据量场景性能不如缓存表；毕业设计规模下优先选择简单可靠方案。
- 热门文章依赖 Redis，如果 Redis 无数据或测试中 mock 返回空，接口应仍然成功。
- 今日和最近 7 天统计按应用服务器本地日期计算。

## 阶段状态

| 阶段 | 状态 | 验证 |
| --- | --- | --- |
| 接手检查与文档阅读 | 已完成 | git 检查、阅读总览/进度/M1-M5 文档 |
| 创建 M6 计划文档 | 已完成 | 创建 `docs/graduation/m6-dashboard/` |
| 后端 DTO、Service、Controller | 已完成 | 编译、聚焦测试 |
| 后端测试补充 | 已完成 | 聚焦测试、完整 `mvn test` |
| 前端接入 | 已完成 | `npm run build` |
| 文档收尾 | 已完成 | 更新进度和 M6 progress |

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
| `apply_patch` 初次按会话根目录创建文件 | 新增 M6 文件后运行聚焦测试未找到新增测试类 | 将本轮新增 M6 文件移动到 `spring_code/bit-forum-spring` 正确项目目录 |
| Surefire 未匹配新增测试 | 首次聚焦测试在错误目录文件状态下运行 | 修正文件位置后重跑，测试被正常编译和执行 |
| 测试用户名超过本地库字段长度 | 服务层聚焦测试插入较长用户名 | 将测试用户名缩短为固定短前缀加 8 位 UUID |
| 今日统计断言使用昨天测试数据 | 服务层聚焦测试日期统计断言失败 | 将应计入今日的测试数据创建时间改为当天 |
