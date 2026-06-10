# Spring Boot 论坛项目总结

## 项目概况

这是一个基于 Spring Boot 3 + MyBatis-Plus 的轻量级论坛项目，项目路径为：

```text
spring_code/bit-forum-spring
```

项目主要功能包括：用户注册登录、JWT 登录认证、文章发布/修改/删除/查询、评论发布/查询、Redis 浏览量与点赞、热门文章排行、RabbitMQ 异步通知、Flyway 数据库初始化、Docker Compose 一键部署。

技术栈：

```text
Java 17
Spring Boot 3.4.5
MyBatis-Plus
MySQL 8
Redis
RabbitMQ
JWT
BCrypt
Flyway
Docker / Docker Compose
Maven
JUnit 5
```

## 核心业务模块

### 用户模块

涉及文件：

```text
UserController
UserService
User
UserResponse
JwtUtil
JwtProperties
LoginInterceptor
```

用户注册时使用 BCryptPasswordEncoder 对密码进行哈希，不保存明文密码。登录成功后使用 JWT 生成 token。

项目之前注册接口直接返回 User 实体，可能把 password 哈希返回给前端。现在已新增 UserResponse DTO，注册接口返回 DTO，不再暴露密码字段。

JWT 之前密钥硬编码在 JwtUtil 中，现在已经改成读取 application.yml 中的配置，并通过 JwtProperties 管理。JwtUtil 已经从静态工具类改成 Spring Bean，由 UserController 和 LoginInterceptor 注入使用。

### 文章模块

涉及文件：

```text
ArticleController
ArticleService
Article
ArticleMapper
```

文章支持发布、修改、删除、查询详情、查询全部、浏览、点赞、取消点赞、热门排行。

发布文章后会向 RabbitMQ 发送消息。删除文章时已加 @Transactional，并串联清理评论、删除文章、清理 Redis 数据。

之前浏览、点赞、取消点赞可以对不存在的文章操作，现在已经在 ArticleController 中补了文章存在校验：先查 MySQL，如果文章不存在，不再写 Redis。

### 评论模块

涉及文件：

```text
CommentController
CommentService
Comment
CommentMapper
```

评论支持发布和查询。

之前评论可以发布到不存在的文章下，现在 CommentService.publish 会先通过 ArticleMapper 查询文章是否存在，文章不存在则返回失败。

删除文章时也会通过 CommentService.deleteByArticleId 删除对应评论。

### Redis 模块

涉及文件：

```text
RedisService
```

Redis 当前用于：

```text
String：文章浏览量 article:{id}:views
Set：文章点赞用户集合 article:{id}:likes
ZSet：热门文章排行 article:hot
```

点赞使用 Redis Set 保存用户 ID，天然去重。也就是同一个用户重复点赞同一篇文章，点赞数不会重复增加。

当前正在补充 RedisServiceTest，用来验证同一个用户对同一篇文章点赞两次，点赞数量仍然是 1，并且 hasLiked 返回 true。测试前后会清理 Redis key，避免测试数据污染环境。

### RabbitMQ 模块

涉及文件：

```text
RabbitMQConfig
NotificationListener
ArticleService
```

文章发布后，ArticleService.publish 会通过 RabbitTemplate.convertAndSend 向队列发送消息。NotificationListener 监听消息，用于模拟异步通知。

这个设计主要体现：主流程保存文章，同步完成；通知逻辑异步处理，降低主流程耦合。

### 异常处理模块

涉及文件：

```text
GlobalExceptionHandler
```

之前全局异常处理可能直接返回 e.getMessage()，有信息泄露风险。

现在已改成服务端记录详细日志，客户端返回统一错误提示。这样可以避免把数据库错误、内部类名、SQL 异常等敏感信息暴露给前端。

### 数据库初始化模块

涉及文件：

```text
pom.xml
application.yml
src/main/resources/db/migration/V1__init_schema.sql
```

项目已引入 Flyway：

```text
flyway-core
flyway-mysql
```

新增了 V1__init_schema.sql，用于初始化 user_info、article、comment 三张表和基础索引。

为了让 Flyway 接管已有本地数据库，application.yml 中配置了：

```yaml
spring:
  flyway:
    baseline-on-migrate: true
```

Flyway 的作用是管理数据库结构版本，让新环境可以自动建表，也方便后续记录数据库结构变更。

### Docker 部署模块

涉及文件：

```text
Dockerfile
docker-compose.yml
.dockerignore
maven-settings.xml
```

Dockerfile 已从“依赖本机 target jar”改成多阶段构建。

第一阶段使用：

```text
maven:3.9.9-eclipse-temurin-17
```

在容器中从源码打包项目。

第二阶段使用：

```text
eclipse-temurin:17-jre
```

运行最终 jar。

这样后端镜像不再依赖本机提前执行 mvn package，更接近真正的一键构建。

Docker Compose 编排了：

```text
MySQL
Redis
RabbitMQ
Spring Boot app
```

MySQL 宿主机端口从 3306:3306 改成：

```yaml
3307:3306
```

原因是避免和本机 MySQL 的 3306 端口冲突。后端容器内部仍然通过 mysql:3306 连接数据库。

MySQL JDBC URL 增加了：

```text
allowPublicKeyRetrieval=true
```

用于解决 MySQL 8 在容器环境中可能出现的 Public Key Retrieval is not allowed 问题。

Docker Compose 已验证可以启动，四个服务都是 Up：

```text
app
mysql
redis
rabbitmq
```

后端日志出现过 RabbitMQ Connection refused，但这是 app 比 RabbitMQ 启动得更快导致的短暂重连，后面已经成功连接 RabbitMQ。日志中出现 Started BitForumSpringApplication 和 Created new connection，说明服务最终启动成功。

## 已完成的项目增强

1. 注册接口不返回密码哈希，改用 UserResponse DTO。
2. JWT 密钥从硬编码改为配置外置。
3. JwtUtil 改成 Spring Bean。
4. 登录拦截器改为注入 JwtUtil。
5. 全局异常不再直接返回 e.getMessage()。
6. 浏览、点赞、取消点赞前校验文章存在。
7. 评论发布前校验文章存在。
8. 删除文章时清理评论和 Redis 数据。
9. 删除文章流程加事务。
10. 新增 Flyway 数据库初始化脚本。
11. Dockerfile 改成多阶段构建。
12. Docker Compose 能启动 MySQL、Redis、RabbitMQ、后端服务。
13. 新增 Redis 点赞去重测试思路和测试类。

## 当前面试主线

项目可以这样介绍：

```text
我做了一个基于 Spring Boot 3 + MyBatis-Plus 的轻量级论坛系统，实现了用户注册登录、JWT 认证、文章发布、评论、Redis 浏览量和点赞、热门排行、RabbitMQ 异步通知，以及 Docker Compose 部署。

后续我重点不是继续堆新技术，而是围绕安全性、数据一致性、部署可信度和测试覆盖做项目补强。例如注册接口不返回密码哈希、JWT 密钥配置外置、异常信息不泄露、浏览/点赞/评论前校验文章存在、删除文章时清理评论和 Redis、使用 Flyway 管理数据库初始化、使用 Docker 多阶段构建实现更可靠的一键启动。
```

## 接下来建议继续做

下一阶段是 M4 自动化测试，优先补这些测试：

```text
注册登录测试
Redis 点赞去重测试
发布文章测试
非作者删除文章失败测试
评论不存在文章失败测试
```

测试目标不是追求数量，而是覆盖最容易被面试官追问的核心业务规则。
