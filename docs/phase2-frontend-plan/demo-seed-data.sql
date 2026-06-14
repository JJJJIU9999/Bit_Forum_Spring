-- Bit Forum frontend demo data.
-- Use only for local demo reset. Do not put this file into Flyway migration.
--
-- Demo password for all users: 123456
-- BCrypt hash generated with Spring Security BCryptPasswordEncoder.

USE bit_forum;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE comment;
TRUNCATE TABLE article;
TRUNCATE TABLE user_info;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO user_info (id, username, password, avatar, role, status, create_time) VALUES
(1, 'admin_demo', '$2a$10$d1PGdOivvVC.eOwb4VElUeiP7SJD54MwN3utJxtfmGx6h17Nxscqy', NULL, 'ADMIN', 1, '2026-06-01 09:00:00'),
(2, 'writer_liu', '$2a$10$d1PGdOivvVC.eOwb4VElUeiP7SJD54MwN3utJxtfmGx6h17Nxscqy', NULL, 'USER', 1, '2026-06-01 09:20:00'),
(3, 'reader_chen', '$2a$10$d1PGdOivvVC.eOwb4VElUeiP7SJD54MwN3utJxtfmGx6h17Nxscqy', NULL, 'USER', 1, '2026-06-01 09:40:00'),
(4, 'banned_user', '$2a$10$d1PGdOivvVC.eOwb4VElUeiP7SJD54MwN3utJxtfmGx6h17Nxscqy', NULL, 'USER', 0, '2026-06-01 10:00:00');

INSERT INTO article (id, title, content, user_id, view_count, like_count, create_time, update_time) VALUES
(1, 'Spring Boot 登录鉴权流程', '这篇文章用于演示 JWT 登录后的接口访问流程。用户登录成功后，前端会把 token 保存到 localStorage，后续发布文章、点赞和评论时自动带上 Authorization 请求头。', 2, 0, 0, '2026-06-02 10:00:00', '2026-06-02 10:00:00'),
(2, 'Redis 点赞去重怎么做', '点赞功能不是简单地把 like_count 加一，而是把用户 ID 放进 Redis Set。这样同一个用户多次点击点赞按钮，也只会在集合里保留一份记录。', 2, 0, 0, '2026-06-02 11:00:00', '2026-06-02 11:00:00'),
(3, 'RabbitMQ 异步通知演示', '文章发布后，后端会发送 ArticlePublishMessage 到 RabbitMQ。消费者收到消息后处理通知逻辑，并使用手动 ACK 和 Redis 幂等记录降低重复消费风险。', 2, 0, 0, '2026-06-02 12:00:00', '2026-06-02 12:00:00'),
(4, '管理员后台能做什么', '管理员接口统一放在 /api/admin/** 下，后端通过 AdminInterceptor 校验 JWT、账号状态和 ADMIN 角色。前端隐藏按钮只是体验优化，真正权限必须在后端。', 1, 0, 0, '2026-06-02 13:00:00', '2026-06-02 13:00:00'),
(5, '前后端联调常见问题', '如果前端出现 Network Error，优先检查后端 8080 是否启动、Vite 代理是否配置、请求路径是否以 /api 开头。', 3, 0, 0, '2026-06-02 14:00:00', '2026-06-02 14:00:00'),
(6, '这篇文章用于删除演示', '管理员页面可以删除任意文章；普通用户只能删除自己的文章。删除文章时还要同步清理评论和 Redis 中的浏览、点赞、热榜数据。', 3, 0, 0, '2026-06-02 15:00:00', '2026-06-02 15:00:00');

INSERT INTO comment (id, content, user_id, article_id, parent_comment_id, create_time) VALUES
(1, '登录成功后再发请求，前端就不用每个页面手动拼 token 了。', 3, 1, 0, '2026-06-03 09:00:00'),
(2, '这个流程适合面试演示，能把 JWT 和拦截器讲清楚。', 1, 1, 0, '2026-06-03 09:10:00'),
(3, 'Redis Set 做点赞去重比直接改数据库字段更适合这个演示。', 1, 2, 0, '2026-06-03 10:00:00'),
(4, '取消点赞时从 Set 里移除当前 userId 就可以了。', 3, 2, 0, '2026-06-03 10:10:00'),
(5, 'RabbitMQ 这一篇可以作为项目亮点放在热门文章里。', 3, 3, 0, '2026-06-03 11:00:00'),
(6, '管理员后台要重点展示普通用户不能越权。', 2, 4, 0, '2026-06-03 12:00:00'),
(7, '我遇到过端口没启动导致的 Network Error。', 2, 5, 0, '2026-06-03 13:00:00'),
(8, '这条评论用于管理员删除评论演示。', 3, 6, 0, '2026-06-03 14:00:00');

ALTER TABLE user_info AUTO_INCREMENT = 5;
ALTER TABLE article AUTO_INCREMENT = 7;
ALTER TABLE comment AUTO_INCREMENT = 9;
