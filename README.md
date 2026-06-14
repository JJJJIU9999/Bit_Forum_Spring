# 比特论坛 - Spring Boot 轻量级论坛系统

## 项目简介

这是一个基于 Spring Boot 3 的轻量级论坛系统，核心功能包括用户注册登录、文章发布、评论、浏览量统计、点赞去重、热门排行、RabbitMQ 异步通知和 Docker Compose 部署。

项目从 JDBC 版本升级到 Spring Boot 版本，重点练习了接口开发、前后端联调、MySQL 持久化、Redis 缓存设计、RabbitMQ 异步解耦、Docker 部署和常见质量问题修复。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Java 17 |
| 框架 | Spring Boot 3.4.5 |
| ORM | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8.0 |
| 数据库迁移 | Flyway |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3 |
| 认证 | JWT + BCrypt |
| 参数校验 | Jakarta Validation |
| 测试 | JUnit 5 + Spring Boot Test + Mockito |
| 部署 | Docker + Docker Compose |
| 前端演示 | React + Vite + Axios |

## 核心功能

- 用户注册、登录：使用 BCrypt 存储密码哈希，登录成功后返回 JWT。
- 接口安全：注册接口返回 `UserResponse`，不直接返回 `User` 实体和密码字段。
- JWT 认证：写操作通过拦截器校验 `Authorization: Bearer <token>`。
- 管理员权限：`/api/admin/**` 接口会校验 JWT、账号状态和 `ADMIN` 角色。
- 文章管理：发布、编辑、删除、详情、列表、分页查询。
- 数据一致性：浏览、点赞、评论前校验文章存在；删除文章时清理评论和 Redis 数据。
- 评论功能：发布评论、按文章查询评论。
- Redis 浏览量：使用 String 记录文章浏览量。
- Redis 点赞去重：使用 Set 防止同一用户重复点赞。
- Redis 热门排行：使用 ZSet 按热度分数维护热门文章。
- RabbitMQ 异步通知：文章发布后发送结构化消息 `ArticlePublishMessage`。
- RabbitMQ 可靠性：生产者 Confirm/Returns、消费者手动 ACK、Redis 幂等、DLX/DLQ 失败兜底。
- Docker Compose：一键启动 MySQL、Redis、RabbitMQ 和后端服务。

## 项目结构

```text
src/main/java/com/bitforum/
├── common/                 # 统一返回结果、热门文章返回对象
├── config/                 # Web、JWT、MyBatis-Plus、RabbitMQ 配置
├── controller/             # 用户、文章、评论接口
├── dto/                    # Request / Response DTO
├── entity/                 # 数据库实体
├── exception/              # 全局异常处理
├── interceptor/            # JWT 登录拦截器
├── mapper/                 # MyBatis-Plus Mapper
├── message/                # RabbitMQ 消息对象
├── service/                # 核心业务逻辑
└── util/                   # JWT 工具类
```

## 快速启动

### 方式一：Docker Compose 启动完整环境

前置要求：

- Docker Desktop 已启动。
- 本机 `8080`、`3306`、`6379`、`5672`、`15672` 端口没有被占用。

启动命令：

```powershell
cd D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring
docker compose up --build
```

启动后访问：

| 服务 | 地址 |
| --- | --- |
| 后端接口 | `http://localhost:8080` |
| RabbitMQ 管理台 | `http://localhost:15672` |
| MySQL 宿主机端口 | `localhost:3306` |
| Redis 宿主机端口 | `localhost:6379` |

RabbitMQ 管理台账号密码从本地 `.env` 读取：

```text
见 `.env.example`
```

说明：

- Compose 内部后端服务通过 `mysql:3306` 访问 MySQL。
- 宿主机访问 Compose 里的 MySQL 使用 `localhost:3306`。
- 运行 Docker Compose 前，先参考 `.env.example` 创建本地 `.env`，不要提交真实 `.env`。
- Flyway 会根据 `src/main/resources/db/migration` 下的脚本初始化数据库结构。

### 方式二：本地 Maven 启动后端

前置要求：

- 本机已启动 MySQL、Redis、RabbitMQ。
- MySQL 中存在 `bit_forum` 数据库。
- `application.yml` 中的连接配置和本机环境一致。

启动命令：

```powershell
cd D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring
mvn spring-boot:run
```

运行测试：

```powershell
mvn test
```

### 方式三：启动 React 前端演示页

前置要求：

- 后端已启动在 `http://localhost:8080`。
- 前端依赖已安装。

第一次进入前端目录时安装依赖：

```powershell
cd D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
npm install
```

启动前端开发服务器：

```powershell
cd D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

前端演示页覆盖登录注册、文章列表/详情/发布、点赞、评论、热门文章和管理员页面。

详细启动顺序、演示账号和浏览器验证流程见：

[React Frontend Demo Guide](./docs/phase2-frontend-plan/frontend-demo-guide.md)

### 创建管理员账号（方式 A：手动 SQL）

当前项目不会在代码里硬编码管理员账号。推荐先通过注册接口创建一个普通用户，再手动把这个用户提升为管理员：

```http
POST /api/user/register
Content-Type: application/json
```

```json
{
  "username": "admin_user",
  "password": "123456"
}
```

然后连接 MySQL，只修改这个用户的 `role/status`：

```sql
USE bit_forum;

UPDATE user_info
SET role = 'ADMIN',
    status = 1
WHERE username = 'admin_user';

SELECT id, username, role, status
FROM user_info
WHERE username = 'admin_user';
```

这样密码仍然由注册流程使用 BCrypt 加密保存，SQL 只负责授予管理员身份。

## 接口验证流程

下面请求体都使用 JSON。需要登录的接口必须在 Header 中携带：

```text
Authorization: Bearer <token>
```

### 1. 注册

```http
POST /api/user/register
Content-Type: application/json
```

```json
{
  "username": "testuser",
  "password": "123456"
}
```

注册成功后返回用户基础信息，不返回密码哈希。

### 2. 登录

```http
POST /api/user/login
Content-Type: application/json
```

```json
{
  "username": "testuser",
  "password": "123456"
}
```

登录成功后从 `data.token` 中复制 JWT。

### 3. 发布文章

```http
POST /api/article/publish
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "title": "第一篇文章",
  "content": "这是文章内容"
}
```

发布成功后会保存文章，并发送 RabbitMQ 异步通知消息。

### 4. 查看分页文章

```http
GET /api/article/page?pageNum=1&pageSize=10
```

分页结果中包含 `records`、`total`、`pages`、`current`、`size` 等信息。

### 5. 查看文章详情

```http
GET /api/article/detail?articleId=1
```

### 6. 浏览文章

```http
GET /api/article/view?articleId=1
```

浏览前会先校验文章存在，然后 Redis 浏览量加 1，热门分数加 1。

### 7. 点赞文章

```http
POST /api/article/like?articleId=1
Authorization: Bearer <token>
```

点赞前会先校验文章存在，然后 Redis Set 记录用户点赞，热门分数加 3。

### 8. 查看热门文章

```http
GET /api/article/hot
```

热门排行从 Redis ZSet 读取，不再遍历 MySQL 全量文章计算。

### 9. 发布评论

```http
POST /api/comment/publish
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "articleId": 1,
  "content": "这是一条评论"
}
```

评论前会先校验文章存在，避免产生脏评论。

## API 总览

### 用户接口

| 方法 | 地址                 | 是否登录 | 请求方式  |
| ---- | -------------------- | -------- | --------- |
| POST | `/api/user/register` | 否       | JSON Body |
| POST | `/api/user/login`    | 否       | JSON Body |

### 文章接口

| 方法   | 地址                                         | 是否登录 | 说明                   |
| ------ | -------------------------------------------- | -------- | ---------------------- |
| POST   | `/api/article/publish`                       | 是       | 发布文章               |
| PUT    | `/api/article/update`                        | 是       | 修改文章，仅作者可操作 |
| DELETE | `/api/article/delete?articleId=1`            | 是       | 删除文章，仅作者可操作 |
| GET    | `/api/article/listAll`                       | 否       | 查询全部文章           |
| GET    | `/api/article/page?pageNum=1&pageSize=10`    | 否       | 分页查询文章           |
| GET    | `/api/article/detail?articleId=1`            | 否       | 查询文章详情           |
| GET    | `/api/article/view?articleId=1`              | 否       | 浏览文章               |
| POST   | `/api/article/like?articleId=1`              | 是       | 点赞                   |
| POST   | `/api/article/unlike?articleId=1`            | 是       | 取消点赞               |
| GET    | `/api/article/hot`                           | 否       | 热门排行               |

文章更新请求体：

```json
{
  "articleId": 1,
  "title": "修改后的标题",
  "content": "修改后的内容"
}
```

### 评论接口

| 方法 | 地址                                    | 是否登录 | 说明         |
| ---- | --------------------------------------- | -------- | ------------ |
| POST | `/api/comment/publish`                  | 是       | 发布评论     |
| GET  | `/api/comment/listAll?articleId=1`      | 否       | 查询文章评论 |

### 管理员接口

管理员接口都需要在 Header 中携带管理员 JWT：

```text
Authorization: Bearer <admin-token>
```

| 方法   | 地址                                             | 权限要求 | 说明 |
| ------ | ------------------------------------------------ | -------- | ---- |
| GET    | `/api/admin/health`                              | 管理员   | 验证管理员权限链路 |
| GET    | `/api/admin/article/page?pageNum=1&pageSize=10`  | 管理员   | 分页查询文章 |
| DELETE | `/api/admin/article/delete?articleId=1`          | 管理员   | 删除任意文章，并清理评论和 Redis 数据 |
| GET    | `/api/admin/comment/page?pageNum=1&pageSize=10`  | 管理员   | 分页查询评论 |
| DELETE | `/api/admin/comment/delete?commentId=1`          | 管理员   | 删除任意评论 |
| GET    | `/api/admin/user/page?pageNum=1&pageSize=10`     | 管理员   | 分页查询用户，响应不包含 `password` |
| PUT    | `/api/admin/user/disable?userId=2`               | 管理员   | 禁用用户，不能禁用当前管理员自己 |
| PUT    | `/api/admin/user/enable?userId=2`                | 管理员   | 启用用户 |

## RabbitMQ 链路说明

文章发布后的异步链路：

```text
ArticleService.publish
  -> RabbitTemplate.convertAndSend(article.exchange, article.publish, ArticlePublishMessage)
  -> article.publish.queue
  -> NotificationListener.handlePublish
```

可靠性设计：

- 生产者开启 `publisher-confirm-type: correlated`，确认消息是否到达 Broker。
- 生产者开启 `publisher-returns: true` 和 `mandatory: true`，确认消息是否成功路由到队列。
- 消息体使用 `ArticlePublishMessage`，包含 `messageId`、`articleId`、`userId`、`title`、`publishTime`。
- 消费者使用手动 ACK，成功后 `basicAck`，失败后 `basicNack(requeue=false)`。
- 使用 Redis Set 记录已处理的 `messageId`，避免重复消费。
- 普通队列配置 DLX，失败消息进入 `article.publish.dlq`，由 DLQ 监听器记录日志，方便后续排查和补偿。

## 面试演示流程

建议 5 分钟演示顺序：

1. 运行 `docker compose up --build`，说明 MySQL、Redis、RabbitMQ、后端服务由 Compose 编排。
2. 注册用户，强调注册响应不返回密码哈希。
3. 登录拿到 JWT，说明写接口通过拦截器校验登录态。
4. 发布文章，观察数据库有文章，RabbitMQ 消费者有日志。
5. 浏览文章和点赞文章，说明 Redis String、Set、ZSet 分别承担浏览量、点赞去重、热门排行。
6. 发布评论，说明评论前校验文章存在。
7. 运行 `mvn test`，说明核心业务已经有自动化测试覆盖。

详细讲稿见：[5分钟项目演示稿.md](./docs/interview/5分钟项目演示稿.md)。

面试前最终检查见：[面试前最终改进清单.md](./docs/interview/面试前最终改进清单.md)。

## 常用排查命令

```powershell
# 查看 Compose 解析后的最终配置
docker compose config

# 启动并重新构建后端镜像
docker compose up --build

# 查看容器状态
docker compose ps

# 单独查看后端日志
docker compose logs -f app

# 运行自动化测试
mvn test
```

## 项目亮点总结

面试中可以这样概括：

> 我做了一个 Spring Boot 论坛项目，从 JDBC 版升级到 Spring Boot 版，实现了用户登录、文章发布、评论、Redis 点赞和浏览量、Redis ZSet 热门排行、RabbitMQ 异步通知以及 Docker Compose 部署。后续我重点补强了安全、数据一致性、接口规范、自动化测试和消息可靠性，比如注册接口不返回密码哈希、JWT 密钥外置、浏览点赞评论前校验文章存在、RabbitMQ 使用 Confirm/Returns、手动 ACK、幂等消费和 DLQ 处理失败消息。
