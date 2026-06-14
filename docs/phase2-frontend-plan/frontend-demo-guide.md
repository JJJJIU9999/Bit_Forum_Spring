# React Frontend Demo Guide

## Purpose

This document records the local startup and browser demo flow for the phase 2 React frontend.

Frontend learning and development notes should stay in:

```text
docs/phase2-frontend-plan/
```

Do not write frontend progress into the old backend phase plan directory.

## Project Locations

Backend project root:

```text
D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring
```

Frontend project root:

```text
D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
```

## Startup Order

Start infrastructure first:

```text
MySQL
Redis
RabbitMQ
```

Then start the backend:

```cmd
cd /d D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring
run-local.cmd
```

If you do not use `run-local.cmd`, set these variables before `mvn spring-boot:run`:

```cmd
set SPRING_DATASOURCE_PASSWORD=<your-mysql-password>
set SPRING_RABBITMQ_PASSWORD=<your-rabbitmq-password>
set JWT_SECRET=<32-byte-minimum-jwt-secret>
mvn spring-boot:run
```

Then start the frontend in another terminal:

```cmd
cd /d D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
npm run dev
```

Open:

```text
http://localhost:5173
```

## First-Time Frontend Setup

Run this only when `node_modules` does not exist or dependencies changed:

```cmd
cd /d D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
npm install
```

Run this to verify the frontend can build:

```cmd
cd /d D:\ClaudeCode\BitFrom\spring_code\bit-forum-spring\frontend
npm run build
```

## Demo Accounts

The demo seed data uses this password for all demo accounts:

```text
123456
```

| Username | Role | Status | Use |
| --- | --- | --- | --- |
| `admin_demo` | `ADMIN` | Enabled | Admin page demo |
| `writer_liu` | `USER` | Enabled | Publish, like, comment demo |
| `reader_chen` | `USER` | Enabled | Normal user demo |
| `banned_user` | `USER` | Disabled | Disabled login demo |

Seed files:

```text
docs/phase2-frontend-plan/demo-seed-data.sql
docs/phase2-frontend-plan/demo-redis-seed.txt
```

## Browser Demo Flow

1. Login as `writer_liu / 123456`.
2. Open article list.
3. Open article detail and confirm view count changes.
4. Like and unlike an article.
5. Publish a comment and confirm it appears below the article.
6. Publish a new article and confirm the list refreshes.
7. Open hot articles and compare them with Redis ZSet data.
8. Logout.
9. Login as `admin_demo / 123456`.
10. Open 管理.
11. Check article, comment, and user tabs.
12. Delete a safe demo article or comment.
13. Disable and enable a non-admin user.
14. Logout.
15. Login as a normal user and confirm the admin page shows a permission error.

## What Each Frontend Step Teaches

| Step | Frontend concept |
| --- | --- |
| React + Vite skeleton | `index.html`, `main.jsx`, `App.jsx`, Vite dev server |
| Directory structure | Project root, `src`, pages, API modules, CSS |
| Axios request layer | `baseURL`, proxy, interceptors, JWT injection |
| Login/register | Form state, submit handling, `localStorage` |
| Article list/detail/publish | `useEffect`, page state, parent-child callbacks |
| Like/comment/hot articles | Multiple API calls in one page, Redis-backed data |
| Admin pages | Role-protected requests, tabs, table state, refresh after action |

## Common Problems

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| Frontend shows `Network Error` | Backend is not running on `8080` | Start backend first, then refresh frontend |
| `npm run build passed` fails | `passed` was typed as an extra argument | Use exactly `npm run build` |
| Backend fails with JWT weak key | `JWT_SECRET` was not set or is too short | Use a secret at least 32 bytes long |
| Admin page permission error | Current token is not an enabled admin user | Login as `admin_demo` |
| Browser still shows old behavior | Frontend dev server or backend was not restarted after code changes | Restart the relevant server |

## Current Known Lightweight Limitation

The frontend demo intentionally stays simple:

- No React Router.
- No global state library.
- No production-grade token security.
- No full table component library.

The backend remains the source of truth for authentication and authorization.
