# M4 通知中心执行计划

## M4 目标

实现站内通知中心，让用户可以看到评论、点赞、收藏、审核通过、审核驳回、文章下架等与自己相关的关键事件提醒，补齐社区系统的互动反馈闭环。

本轮优先做站内通知，不做 WebSocket 实时推送，不做短信、邮件、App 推送。

## 范围边界

- 本轮只做 M4：通知中心。
- 保留 M1、M2、M3 未提交改动，不删除、不回滚、不覆盖、不 stash。
- 不在 `main` 分支开发，当前分支保持为 `graduation-design`。
- 不修改历史 Flyway 脚本 `V1`、`V2`、`V3`、`V4`、`V5`。
- 数据库变更只新增 `V6` 迁移。
- 不引入 WebSocket、Spring Security、Spring Cloud、Kafka、复杂 RBAC、Redux、Zustand 或复杂 UI 组件库。
- 继续使用现有 JWT + Interceptor、MyBatis-Plus、Flyway、Redis、RabbitMQ、`Result<T>`、Jakarta Validation 和 React/Vite 简单结构。
- 现有评论、点赞、收藏、审核、下架接口保持可用。
- 不提前实现 M5 或后续模块。

## 数据库迁移方案

新增 Flyway 脚本：

```text
src/main/resources/db/migration/V6__add_notification.sql
```

新增表：

```text
notification
```

建议字段：

- `id BIGINT PRIMARY KEY AUTO_INCREMENT`
- `receiver_id BIGINT NOT NULL`
- `sender_id BIGINT NULL`
- `type VARCHAR(32) NOT NULL`
- `title VARCHAR(100) NOT NULL`
- `content VARCHAR(500) NOT NULL`
- `article_id BIGINT NULL`
- `read_status TINYINT NOT NULL DEFAULT 0`
- `create_time DATETIME DEFAULT CURRENT_TIMESTAMP`
- `update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`

索引：

- `idx_notification_receiver_read_create (receiver_id, read_status, create_time)`
- `idx_notification_article_id (article_id)`
- `idx_notification_type (type)`

## 后端接口方案

新增实体、Mapper、Service、Controller：

- `Notification`
- `NotificationMapper`
- `NotificationService`
- `UserNotificationController`

新增接口：

- `GET /api/user/notifications?pageNum=1&pageSize=10`
- `GET /api/user/notifications/unread-count`
- `PUT /api/user/notifications/read?notificationId=1`
- `PUT /api/user/notifications/read-all`

接口约束：

- 通知接口全部需要登录。
- 用户只能查看自己的通知。
- 用户只能标记自己的通知已读。
- 一键全部已读只处理当前用户的未读通知。
- 通知列表默认按 `create_time DESC` 排序。

通知创建接入点：

- 评论他人已发布文章时，给文章作者生成 `COMMENT` 通知。
- 点赞他人已发布文章且点赞成功时，给文章作者生成 `LIKE` 通知。
- 收藏他人已发布文章且收藏成功时，给文章作者生成 `FAVORITE` 通知。
- 管理员审核通过文章时，给作者生成 `AUDIT_APPROVED` 通知。
- 管理员驳回文章时，给作者生成 `AUDIT_REJECTED` 通知，内容包含驳回原因。
- 管理员下架文章时，给作者生成 `ARTICLE_OFFLINE` 通知，内容包含下架原因。

本轮优先采用同步写 `notification` 表，不新增通知队列，避免为了 M4 重构现有 RabbitMQ。

## 前端兼容方案

- 保持现有 React/Vite 简单结构，不引入 React Router、Redux、Zustand 或 UI 组件库。
- 在现有顶部导航增加“通知”入口。
- 新增通知 API 封装。
- 新增通知中心页面，展示通知类型、标题、内容、关联文章、已读状态、创建时间。
- 支持单条标记已读。
- 支持全部已读。
- 在导航入口展示未读数量。
- 不重写现有整体布局，不影响文章列表、详情、搜索、收藏、我的文章和管理员面板。

## 测试清单

- [x] 评论他人文章后，文章作者收到评论通知。
- [x] 评论自己的文章，不生成通知。
- [x] 点赞他人文章后，文章作者收到点赞通知。
- [x] 重复点赞不重复生成通知。
- [x] 收藏他人文章后，文章作者收到收藏通知。
- [x] 收藏自己的文章，不生成通知。
- [x] 管理员审核通过后，文章作者收到审核通过通知。
- [x] 管理员驳回后，文章作者收到驳回通知，通知内容包含或关联原因。
- [x] 管理员下架后，文章作者收到下架通知，通知内容包含或关联原因。
- [x] 用户只能分页查看自己的通知。
- [x] 未读数量只统计当前用户未读通知。
- [x] 用户只能标记自己的通知已读。
- [x] 一键全部已读只影响当前用户。
- [x] 未登录用户不能访问通知接口。

## 验证命令

后端完整测试：

```powershell
$env:SPRING_DATASOURCE_PASSWORD='root'; $env:SPRING_RABBITMQ_PASSWORD='guest'; $env:JWT_SECRET='bit-forum-local-test-secret-minimum-32-bytes'; mvn test
```

前端构建：

```powershell
cd frontend
npm run build
```

## 风险和待确认事项

- M1、M2、M3 均尚未提交，M4 需要严格增量实现，避免误改前置模块语义。
- 如果当前代码已有通知相关雏形，需要先确认是否复用；不能为了 M4 强行重构 RabbitMQ。
- 点赞通知必须只在点赞成功时创建，重复点赞或取消点赞不能重复创建通知。
- 收藏通知依赖 M3 的唯一约束和业务校验，只在收藏成功后创建。
- 审核驳回和下架通知内容需要包含原因，但不新增复杂消息模板系统。

## 阶段

### 阶段 1：接手与计划

- [x] 执行 Git 前置检查。
- [x] 阅读毕业设计总览、进度、M1、M2、M3 文档。
- [x] 创建 M4 文档目录和计划文件。
- [x] 阅读通知、RabbitMQ、文章、评论、收藏、审核相关后端代码。
- [x] 阅读当前后端测试。
- [x] 阅读前端文章和评论相关代码。

### 阶段 2：后端实现

- [x] 新增 Flyway V6。
- [x] 新增 Notification Entity / Mapper / Service / Controller。
- [x] 更新登录拦截路径。
- [x] 接入评论、点赞、收藏、审核通过、驳回、下架通知创建。

### 阶段 3：后端测试

- [x] 补充服务层和控制器测试。
- [x] 运行后端测试。

### 阶段 4：前端实现

- [x] 增加通知 API。
- [x] 增加通知中心页面和导航入口。
- [x] 运行前端构建。

### 阶段 5：文档收尾

- [x] 更新 M4 findings/progress。
- [x] 更新毕业设计进度。
- [x] 汇总接口、迁移、测试、验证结果和风险。
