# 🚀 部署指南

> [!WARNING]
> **时效说明（2026-10）**：本文档转自文档站早期版本，端口已订正为实际的 **8081**。
> 管理员创建已改由**六步可视化安装向导**承担（文中 `ASTRNEST_ADMIN_*` 等环境变量方式已废弃），
> 部署细节以仓库根目录 **README.md** 与 `docs/audit-report-2026-10-05.md` 为准，本文仅作架构参考。


AstrNest 提供多种部署方式，您可以根据自己的需求选择最适合的方案：

## 部署方式对比

| 部署方式 | 适用场景 | 复杂度 | 推荐指数 | 状态 |
|---------|---------|--------|----------|------|
| [推荐部署](./recommended.md) | 生产环境、团队使用 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ 可用 |
| [Docker 部署](./docker.md) | 容器化部署、快速测试 | ⭐⭐ | ⭐⭐⭐⭐ | ✅ 可用 |
| [直接部署](./direct.md) | 开发环境、学习测试 | ⭐⭐⭐ | ⭐⭐⭐ | ✅ 可用 |
| [Serverless 部署](./serverless.md) | 云原生、弹性伸缩 | ⭐⭐⭐⭐ | ⭐⭐ | 🚧 开发中 |

## 环境要求

### Docker 部署（推荐）
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **至少 2GB 内存**
- **至少 5GB 磁盘空间**

### 传统部署
- **Java**: JDK 21+
- **Node.js**: 18+
- **MySQL**: 5.7+/8.0+
- **内存**: 至少 2GB
- **磁盘空间**: 至少 5GB

## 快速选择

- **新手用户**: 强烈推荐使用 [快速部署](./recommended.md)，简单快捷、生产就绪
- **生产环境**: 强烈推荐 [推荐部署](./recommended.md)，稳定可靠、易于维护
- **开发调试**: 可以使用 [直接部署](./direct.md) 或 Docker 部署
- **快速体验**: 使用 [Docker 部署](./docker.md)

## 部署前准备

无论选择哪种部署方式，都需要先完成以下准备工作：

### 1. 克隆项目

```bash
git clone <repository-url>
cd AstrNest
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，配置必要参数
# 必须修改：数据库密码、管理员密码、站点域名
```

### 3. 关键配置项

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `ChangeThisRootPass!` |
| `MYSQL_PASSWORD` | MySQL 应用密码 | `ChangeThisDbPass!` |
| `ASTRNEST_ADMIN_PASSWORD` | 管理员密码 | `StrongAdminPass123!` |
| `PUBLIC_SITE_URL` | 站点域名 | `https://yourdomain.com` |
| `VITE_API_BASE_URL` | API 地址 | `http://localhost:8081` |

> ⚠️ **安全提示**：请勿使用默认密码，生产环境务必使用强密码！

## 部署流程概览

### Docker Compose 部署流程

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件

# 2. 启动服务
docker compose --env-file .env up -d

# 3. 验证部署
curl http://localhost:8081/actuator/health
```

### 传统部署流程

```bash
# 1. 初始化数据库
mysql -u root -p < backend/db/init.sql

# 2. 启动后端
cd backend
./mvnw spring-boot:run

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

## 默认访问地址

部署完成后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://localhost | Web UI |
| 后端 API | http://localhost:8081 | REST API |
| API 文档 | http://localhost:8081/swagger-ui/index.html | Swagger UI |
| 健康检查 | http://localhost:8081/actuator/health | 服务状态 |

## 默认管理员账号

- **用户名**: `admin`
- **密码**: `ChangeMe_Admin123!`

> ⚠️ **重要**：首次登录后请立即修改默认密码！

## 生产环境检查清单

部署到生产环境前，请确认以下事项：

- [ ] 修改了所有默认密码（数据库、管理员）
- [ ] 配置了正确的站点域名
- [ ] 启用了 HTTPS/SSL
- [ ] 配置了邮件服务
- [ ] 配置了 AI 内容审核（可选）
- [ ] 配置了数据备份策略
- [ ] 配置了监控告警
- [ ] 配置了防火墙规则

## 获取帮助

如果在部署过程中遇到问题：

1. 查看 [常见问题解答](/faq)
2. 查看 [安装配置指南](/installation-guide)
3. 查看 [API 文档](/api/)
4. 在 GitHub Issues 提问

---

选择适合您的部署方式开始使用 AstrNest！
