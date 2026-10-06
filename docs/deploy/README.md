# 部署文档（docs/deploy）

本目录收录文档站早期的五篇部署指南（原 `AstrNest-docs/docs/zh/deploy/`，该目录此前被
.gitignore 忽略、不随仓库分发，见审计报告 P1-15），已做两处订正：

1. 端口全线订正为实际的 **8081**（原文档误写 8080）；
2. 示例管理员密码中性化。

## 权威顺序

1. **README.md**（仓库根目录）：部署步骤与配置的当前权威来源；
2. 本目录五篇：架构与 nginx/反代/COS+CDN 思路仍有效，但环境变量口径（如 `ASTRNEST_ADMIN_*`）
   已被可视化安装向导取代，按各文件顶部的时效说明理解。

## 目录

- [index.md](./index.md) —— 部署路线总览
- [docker.md](./docker.md) —— Docker Compose 部署
- [direct.md](./direct.md) —— 手动直连部署（jar + nginx）
- [recommended.md](./recommended.md) —— 推荐生产架构（nginx 反代 + 同域分流）
- [serverless.md](./serverless.md) —— 前端静态托管（COS+CDN）参考
