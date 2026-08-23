# React 前端 Docker 化进度

## 2026-07-20

- 完成工作区、当前分支、Compose、Vite 与依赖检查。
- 确认采用独立 Nginx 前端容器，不把静态文件塞入 Spring Boot JAR。
- 新增前端 Node 22 + Nginx 多阶段 Dockerfile、同源代理与 React Router 回退配置。
- Compose 新增 `frontend` 服务，并为后端上传目录增加 `uploads-data` 命名卷。
- `npm test` 通过：3 个测试文件、4 个测试全部通过。
- `npm run build` 通过：Vite 成功生成生产静态文件。
- `docker compose config --quiet` 通过。
- `docker compose up --build -d` 通过，前端镜像和五个服务均已启动。
- 首页 `http://localhost/` 返回 200。
- React 深层路由 `http://localhost/articles/1` 返回 200，Nginx 回退配置有效。
- 经 Nginx 访问 `/api/article/page` 返回 200，并成功读取数据库中的文章。
- `/uploads` 请求已由 Nginx 转发至后端；不存在的测试文件返回后端 JSON 错误响应。
- 后端健康检查 `http://localhost:8080/actuator/health` 返回 200。
- `git diff --check` 通过；两份既有的未跟踪学习文档未被修改。

## 验证记录

| 项目 | 结果 |
| --- | --- |
| 当前分支 | `feat/frontend-refactor` |
| 当前提交 | `bfbfcce` |
| 用户端口改动 | 保留 `3307:3306` |
| 前端镜像 | `bit-forum-spring-frontend:latest` |
| 前端容器 | `bit-forum-spring-frontend-1`，端口 `80:80` |
| 服务状态 | `frontend`、`app`、`mysql`、`redis`、`rabbitmq` 全部为 `Up` |
| 数据库迁移 | Flyway 已校验 11 个迁移，当前版本 11 |

## 错误记录

| 错误 | 原因 | 处理 |
| --- | --- | --- |
| `npm test` 无法解析 `/@fs/D:/...` | Vitest 在沙箱映射目录运行，但测试路径仍指向真实工作区 | 在真实工作目录以提升权限重跑，不修改测试代码 |
