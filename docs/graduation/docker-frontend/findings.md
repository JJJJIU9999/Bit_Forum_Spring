# React 前端 Docker 化发现

- 根目录 Dockerfile 已使用 Maven + JRE 多阶段构建，仅负责 Spring Boot。
- 当前 Compose 已编排 MySQL、Redis、RabbitMQ 和 `app`，用户已将 MySQL 宿主机端口改为 `3307`。
- 前端 Axios 默认请求相对路径 `/api`，上传资源使用 `/uploads`；适合由同源 Nginx 反向代理，无需修改业务代码。
- 前端使用 BrowserRouter，Nginx 必须配置 `try_files ... /index.html`。
- Vite 7 需要较新的 Node 运行时，前端构建阶段使用 Node 22。
- 后端上传目录默认解析为 `/app/uploads`；Compose 增加命名卷，避免重建 `app` 时丢失头像和封面。
- 当前工作区另有两份未跟踪学习文档，必须保持不动。
- 实际请求 `/api/article/page` 成功返回 11 篇文章，说明前端代理、后端和当前 Docker MySQL 数据链路已经连通。
- Docker Compose 构建会把 React 生产文件放入前端镜像，不会在项目目录额外生成一个可直接发送的压缩包。
