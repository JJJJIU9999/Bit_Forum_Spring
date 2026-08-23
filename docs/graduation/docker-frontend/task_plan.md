# React 前端 Docker 化计划

## 目标

将现有 React + Vite 前端构建为 Nginx 镜像，并由 Docker Compose 与 Spring Boot、MySQL、Redis、RabbitMQ 一起启动。

## 边界

- 保留用户已修改的 MySQL 宿主机端口 `3307`。
- 不修改 React 页面和业务接口。
- 不修改 `.env`，不新增 Flyway 迁移。
- 不删除、暂存或覆盖 `docs/learning/` 下的未跟踪文档。

## 阶段

| 阶段 | 状态 | 验证 |
| --- | --- | --- |
| 1. 现场检查与方案确认 | 完成 | Git 状态、Compose、Vite 请求路径已核对 |
| 2. 前端镜像与 Nginx 配置 | 完成 | `npm test`、`npm run build` |
| 3. Compose 集成 | 完成 | `docker compose config --quiet` 通过 |
| 4. 实际构建与端到端验证 | 完成 | 镜像构建、容器启动、首页、深层路由、API 代理均通过 |
| 5. 文档与差异收口 | 完成 | `git diff --check` 通过，工作区已复核 |

## 成功标准

- `docker compose up --build -d` 能启动 `frontend`。
- `http://localhost/` 返回 React 页面。
- React Router 深链接刷新可回退到 `index.html`。
- `/api` 与 `/uploads` 由 Nginx 转发到 `app:8080`。
- 不再需要单独运行 `npm run dev`。
