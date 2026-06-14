# Frontend Demo Data Design

## Goal

Create a small, clean data set that can support the React frontend demo:

- Login and register.
- Article list and detail.
- Publish article.
- Like and unlike.
- Comment list and comment publish.
- Hot article ranking.
- Admin article/comment/user management.
- Disabled user login failure.

## Files

```text
docs/phase2-frontend-plan/demo-seed-data.sql
docs/phase2-frontend-plan/demo-redis-seed.txt
```

These files are local demo helpers. They are not Flyway migrations and should not run automatically in production.

## Demo Accounts

All demo accounts use password:

```text
123456
```

| ID | Username | Role | Status | Purpose |
| --- | --- | --- | --- | --- |
| 1 | `admin_demo` | `ADMIN` | 1 | Admin dashboard demo |
| 2 | `writer_liu` | `USER` | 1 | Normal author demo |
| 3 | `reader_chen` | `USER` | 1 | Normal reader/commenter demo |
| 4 | `banned_user` | `USER` | 0 | Disabled login failure demo |

## Article Design

| ID | Title | Author | Purpose |
| --- | --- | --- | --- |
| 1 | Spring Boot 登录鉴权流程 | writer_liu | JWT/login explanation |
| 2 | Redis 点赞去重怎么做 | writer_liu | Like/unlike demo |
| 3 | RabbitMQ 异步通知演示 | writer_liu | Project highlight article |
| 4 | 管理员后台能做什么 | admin_demo | Admin permission explanation |
| 5 | 前后端联调常见问题 | reader_chen | Troubleshooting content |
| 6 | 这篇文章用于删除演示 | reader_chen | Admin delete demo |

## Comment Design

The seed has comments across articles 1-6. Article 6 includes a comment specifically for admin comment deletion demo.

## Redis Demo Data

MySQL stores the article records, but the backend reads these runtime counters from Redis:

```text
article:{id}:views
article:{id}:likes
article:hot
```

So after resetting MySQL data, seed Redis too if you want the hot article page and detail counters to look realistic.

## Suggested Demo Flow

1. Login as `writer_liu / 123456`.
2. Open article list.
3. Open article detail.
4. Like article 2.
5. Comment on article 1.
6. Publish a new article.
7. Open hot article ranking.
8. Login as `admin_demo / 123456`.
9. Delete article 6.
10. Delete comment 8.
11. Disable `reader_chen`.
12. Try logging in as `banned_user / 123456` to show disabled-login behavior.
