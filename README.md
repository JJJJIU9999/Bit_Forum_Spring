# 比特讲坛 — 轻量级论坛系统

## 项目简介

这是一个讨论 AI 新技术的论坛系统，用来让用户交流 Agent、LLM、Token 等前沿话题。个人学习项目，从零开始跟着"比特讲坛"路线图搭建，涵盖 Java 后端主流技术栈。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.4.5 |
| 构建 | Maven |
| ORM | MyBatis-Plus 3.5.9 + Lombok |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3 |
| 认证 | JWT + BCrypt |
| 部署 | Docker + Docker Compose |

开发过程中曾用多线程 + 线程池实现异步，后升级为 RabbitMQ 消息队列，实现了更好的解耦和消息持久化。

## 功能清单

- 用户注册 / 登录（BCrypt 密码加密 + JWT 令牌认证）
- 文章发布、列表、详情
- 文章浏览量统计（Redis String INCR）
- 文章点赞 / 取消点赞（Redis Set 去重，防止重复点赞）
- 热门文章排行（加权公式：浏览量 + 点赞数 × 3）
- 文章编辑、删除（权限校验：只能操作自己的文章）
- 评论发布、按文章查询
- JWT 拦截器自动认证，写操作受保护
- 全局异常处理 + 参数校验（@Valid）
- RabbitMQ 异步通知（文章发布后异步处理）
- Docker Compose 一键启动全栈

## 数据库表

| 表名 | 说明 |
|------|------|
| `user_info` | 用户（id, username, password, avatar, create_time） |
| `article` | 文章（id, title, content, user_id, view_count, like_count, create_time） |
| `comment` | 评论（id, content, user_id, article_id, parent_comment_id, create_time） |

## 项目结构

```
src/main/java/com/bitforum/
├── BitForumSpringApplication.java    ← 启动类
├── common/
│   ├── Result.java                   ← 统一返回格式 {code, message, data}
│   └── HotArticle.java               ← 热门文章 DTO
├── config/
│   ├── RabbitMQConfig.java           ← 消息队列声明
│   └── WebMvcConfig.java             ← 拦截器注册
├── controller/
│   ├── UserController.java           ← 用户接口
│   ├── ArticleController.java        ← 文章接口
│   └── CommentController.java        ← 评论接口
├── entity/
│   ├── User.java
│   ├── Article.java
│   └── Comment.java
├── exception/
│   └── GlobalExceptionHandler.java   ← 全局异常处理
├── interceptor/
│   └── LoginInterceptor.java         ← JWT 认证拦截器
├── mapper/
│   ├── UserMapper.java
│   ├── ArticleMapper.java
│   └── CommentMapper.java
├── service/
│   ├── UserService.java              ← 注册、登录
│   ├── ArticleService.java           ← 文章发布、查询
│   ├── CommentService.java           ← 评论发布、查询
│   ├── RedisService.java             ← 浏览量、点赞、排行
│   └── NotificationListener.java     ← RabbitMQ 消费者
└── util/
    └── JwtUtil.java                  ← JWT 生成、解析、验证
```

## 快速启动

### 前置要求

- Docker Desktop

### 步骤

```bash
# 1. 克隆项目
git clone <仓库地址>
cd bit-forum-spring

# 2. 一键启动（MySQL + Redis + RabbitMQ + Spring Boot）
docker compose up
```

首次启动会自动拉取镜像并构建，启动后访问 `http://localhost:8080`。

### 测试

**方式一：Thunder Client / Postman**

1. `POST http://localhost:8080/api/user/register` — 注册一个账号（Body 选 form，填 `username` 和 `password`）
2. `POST http://localhost:8080/api/user/login` — 登录，返回的 JSON 中复制 `data.token` 的值
3. `POST http://localhost:8080/api/article/publish` — 发布文章：
   - Headers 加 `Authorization: Bearer <第 2 步的 token>`
   - Body 选 JSON，填 `{"title":"测试文章","content":"Hello Docker"}`
4. `GET http://localhost:8080/api/article/listAll` — 查看所有文章，确认刚才发的文章已入库
5. `GET http://localhost:8080/api/article/view?articleId=1` — 提高浏览量
6. `POST http://localhost:8080/api/article/like` — 点赞（需带 Token，参数 `articleId=1`）
7. `GET http://localhost:8080/api/article/hot` — 查看热门排行

**方式二：curl**

```bash
# 注册
curl -X POST http://localhost:8080/api/user/register \
  -d "username=test&password=1234"

# 登录
curl -X POST http://localhost:8080/api/user/login \
  -d "username=test&password=1234"

# 发布文章（<TOKEN> 替换为上一步返回的 token）
curl -X POST http://localhost:8080/api/article/publish \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"title":"Hello","content":"我的第一篇文章"}'
```

## API 接口文档

**通用说明：** 返回格式为 `{"code":200,"message":"操作成功","data":{...}}`。需要认证的接口在 Header 加 `Authorization: Bearer <Token>`。

### 用户

| 方法 | URL | 认证 | 参数 | 说明 |
|------|-----|------|------|------|
| POST | `/api/user/register` | 否 | `username` `password` | 注册 |
| POST | `/api/user/login` | 否 | `username` `password` | 登录，返回 JWT Token |

### 文章

| 方法 | URL | 认证 | 参数 | 说明 |
|------|-----|------|------|------|
| POST | `/api/article/publish` | 是 | Body JSON `{"title":"...","content":"..."}` | 发布文章 |
| GET | `/api/article/listAll` | 否 | — | 全部文章列表 |
| GET | `/api/article/detail` | 否 | `articleId` | 文章详情（含浏览量、点赞数） |
| GET | `/api/article/view` | 否 | `articleId` | 浏览量 +1 |
| PUT | `/api/article/update` | 是 | `articleId` `newTitle` `newContent` | 编辑文章（仅作者） |
| DELETE | `/api/article/delete` | 是 | `articleId` | 删除文章（仅作者） |
| POST | `/api/article/like` | 是 | `articleId` | 点赞 |
| POST | `/api/article/unlike` | 是 | `articleId` | 取消点赞 |
| GET | `/api/article/hot` | 否 | — | 热门排行（公式：浏览量 + 点赞 × 3） |

### 评论

| 方法 | URL | 认证 | 参数 | 说明 |
|------|-----|------|------|------|
| POST | `/api/comment/publish` | 是 | `articleId` `content` | 发布评论 |
| GET | `/api/comment/listAll` | 否 | `articleId` | 某篇文章的评论列表 |

### curl 示例

```bash
# 注册
curl -X POST http://localhost:8080/api/user/register \
  -d "username=test&password=1234"

# 登录
curl -X POST http://localhost:8080/api/user/login \
  -d "username=test&password=1234"

# 发布文章（<TOKEN> 替换为上一步返回的 token）
curl -X POST http://localhost:8080/api/article/publish \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"title":"Hello","content":"我的第一篇文章"}'
```
