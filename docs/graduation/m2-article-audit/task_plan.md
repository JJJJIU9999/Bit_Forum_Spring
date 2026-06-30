# M2 文章草稿、审核、发布、下架状态流转执行计划

## 目标
将文章从“直接发布”升级为完整内容审核流程，支持 `DRAFT`、`PENDING`、`PUBLISHED`、`REJECTED`、`OFFLINE` 五种状态。

## 范围边界
- 本轮只做 M2：文章草稿、审核、发布、下架状态流转。
- 保留 M1 未提交改动，不删除、不回滚、不覆盖、不 stash。
- 不修改历史 Flyway 脚本 `V1`、`V2`、`V3`。
- 不提前实现 M3 或后续模块。
- 不引入 Spring Security、Spring Cloud、Elasticsearch、Kafka、复杂 RBAC、Redux、Zustand。
- 继续使用 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation。

## 数据库迁移方案
- 新增 `V4__add_article_audit_status.sql`。
- `article` 表新增 `status` 字段，已有文章迁移为 `PUBLISHED`，字段最终为 `NOT NULL`。
- 新增 `article_audit_record` 表，记录文章审核操作。
- 建议字段：`id`、`article_id`、`auditor_id`、`audit_status`、`reason`、`create_time`。

## 后端接口方案
- 用户接口：
  - `POST /api/article/draft`：保存草稿。
  - `PUT /api/article/draft`：更新草稿或被驳回文章后保存。
  - `POST /api/article/submit?articleId=1`：提交审核。
  - `GET /api/user/articles?pageNum=1&pageSize=10&status=PENDING`：作者查看自己的文章。
- 管理员接口：
  - `GET /api/admin/article/audit/page?status=PENDING&pageNum=1&pageSize=10`：审核列表。
  - `PUT /api/admin/article/audit/approve?articleId=1`：审核通过。
  - `PUT /api/admin/article/audit/reject`：审核驳回并记录原因。
  - `PUT /api/admin/article/offline`：下架文章。
- 兼容策略：
  - 保留 `POST /api/article/publish`。
  - M2 中将其语义调整为“提交审核”，返回状态为 `PENDING`，同步更新前端文案和文档。

## 前端兼容方案
- 保持 React/Vite 简单结构。
- 将“发布文章”主要文案调整为“提交审核”。
- 支持保存草稿、提交审核、作者文章状态查看。
- 管理员面板增加审核列表、通过、驳回、下架操作。

## 测试清单
- 用户可以保存草稿。
- 草稿不会出现在公共文章列表。
- 用户提交后状态变为 `PENDING`。
- 管理员审核通过后状态变为 `PUBLISHED`。
- 管理员驳回时必须记录原因。
- 普通用户不能审核文章。
- 下架文章不会出现在公共查询和搜索中。
- 已有文章迁移后默认 `PUBLISHED`。

## 验证命令
```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

```powershell
cd frontend
npm run build
```

## 风险和待确认事项
- M1、M2 改动都未提交，后续提交或交接时需要明确区分。
- `/api/article/publish` 已改为“提交审核”，旧调用方如果期望直接公开发布，需要同步认知。
- RabbitMQ 文章发布消息已移动到管理员审核通过后发送。
- 下架文章会清理 Redis 文章数据；如果未来需要保留下架前热度，需要另行设计归档字段。

## 阶段
### 阶段 1：接手和代码盘点
- [x] 执行 Git 前置检查。
- [x] 阅读毕业设计总览、进度和 M1 文档。
- [x] 阅读当前 Article、ArticleService、ArticleController、AdminArticleController、ArticlePublishRequest、ArticleServiceTest、AdminArticleControllerTest。

### 阶段 2：后端设计和迁移
- [x] 设计 `article.status` 和 `article_audit_record`。
- [x] 新增 Flyway V4。
- [x] 新增审核记录 Entity / Mapper / DTO。

### 阶段 3：后端接口实现
- [x] 实现草稿、更新草稿、提交审核、作者文章列表。
- [x] 实现管理员审核列表、通过、驳回、下架。
- [x] 限制公开文章列表、详情、热门文章只展示 `PUBLISHED`。
- [x] 调整 `/api/article/publish` 兼容语义。

### 阶段 4：测试
- [x] 补充后端服务和控制器测试。
- [x] 运行后端测试。

### 阶段 5：前端和文档
- [x] 更新前端文案和页面。
- [x] 运行前端构建。
- [x] 更新毕业设计进度和 M2 文档。

## 完成状态
M2 已完成实现和验证，尚未提交。
