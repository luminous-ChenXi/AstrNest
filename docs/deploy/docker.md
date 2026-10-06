# Docker 部署指南

> [!WARNING]
> **时效说明（2026-10）**：本文档转自文档站早期版本，端口已订正为实际的 **8081**。
> 管理员创建已改由**六步可视化安装向导**承担（文中 `ASTRNEST_ADMIN_*` 等环境变量方式已废弃），
> 部署细节以仓库根目录 **README.md** 与 `docs/audit-report-2026-10-05.md` 为准，本文仅作架构参考。


Docker 部署是 AstrNest 推荐的部署方式之一，适合快速部署和容器化环境。

## 前置要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **至少 2GB 内存**
- **至少 5GB 磁盘空间**

## 快速开始

### 1. 准备部署目录

```bash
# 克隆项目
git clone <repository-url>
cd AstrNest

# 复制环境变量模板
cp .env.example .env
```

### 2. 配置环境变量

编辑 `.env` 文件，配置必要的参数：

```bash
# 数据库配置（必须修改）
MYSQL_ROOT_PASSWORD=YourStrongRootPass!
MYSQL_DATABASE=astrnest
MYSQL_USER=astrnest
MYSQL_PASSWORD=YourStrongDbPass!

# 后端配置（必须修改）
ASTRNEST_DB_URL=jdbc:mysql://mysql:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
ASTRNEST_DB_USERNAME=astrnest
ASTRNEST_DB_PASSWORD=YourStrongDbPass!
ASTRNEST_STORAGE_ROOT=/storage/upload
ASTRNEST_STORAGE_PUBLIC=/upload
ASTRNEST_STORAGE_STRATEGY=LOCAL

# 管理员配置（必须修改）
ASTRNEST_ADMIN_USERNAME=admin
ASTRNEST_ADMIN_PASSWORD=YourStrongAdminPass!
ASTRNEST_ADMIN_DISPLAY=超级管理员
ASTRNEST_ADMIN_EMAIL=admin@your-domain.com

# 站点域名配置
PUBLIC_SITE_URL=https://your-domain.com
PUBLIC_ASSET_URL=https://your-domain.com/upload
BACKEND_API_PUBLIC_URL=https://api.your-domain.com
ASTRNEST_ASSET_DOMAIN=

# 前端配置
VITE_API_BASE_URL=http://backend:8081
VITE_PUBLIC_ASSET_BASE=http://backend:8081/upload
VITE_SITE_NAME=AstrNest

# AI 内容审核配置（可选）
ASTRNEST_AI_TENCENT_SECRET_ID=
ASTRNEST_AI_TENCENT_SECRET_KEY=
ASTRNEST_AI_TENCENT_REGION=ap-beijing
ASTRNEST_AI_TENCENT_BUCKET=

# 邮件服务配置（可选）
SMTP_HOST=smtp.your-domain.com
SMTP_PORT=587
SMTP_USERNAME=your-email@your-domain.com
SMTP_PASSWORD=your-email-password
```

> ⚠️ **安全提示**：请务必修改所有默认密码，使用强密码！

### 3. 启动服务

```bash
# 启动所有服务
docker compose --env-file .env up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 4. 验证部署

```bash
# 健康检查
curl http://localhost:8081/actuator/health

# 预期响应
{"status":"UP"}
```

## 服务架构

Docker Compose 部署包含以下服务：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | astrnest-mysql | 3306 (内部) | 数据库服务 |
| Backend | astrnest-backend | 8081 | Spring Boot 应用，提供 REST API |
| Frontend | astrnest-backend | 80 | Vue 3 前端界面，Nginx 静态文件服务 |

### 数据持久化

默认配置下，数据会持久化到以下位置：

- **MySQL 数据**：Docker volume `mysql_data`
- **上传文件**：`./storage/upload` 目录

## 访问服务

部署完成后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://your-server-ip 或 http://localhost | Web UI |
| 后端 API | http://your-server-ip:8081 | REST API |
| API 文档 | http://your-server-ip:8081/swagger-ui/index.html | Swagger UI |
| 健康检查 | http://your-server-ip:8081/actuator/health | 服务状态 |

## 默认管理员账号

- **用户名**: `admin`
- **密码**: `ChangeMe_Admin123!`（如果你在 .env 中修改了 `ASTRNEST_ADMIN_PASSWORD`，则使用新密码）

> ⚠️ **重要**：首次登录后请立即修改默认密码！

## 高级配置

### 自定义端口

修改 `docker-compose.yml` 中的端口映射：

```yaml
services:
  backend:
    ports:
      - "8081:8081"  # 宿主机端口:容器端口
  
  frontend:
    ports:
      - "8081:80"    # 宿主机端口:容器端口
```

### 数据持久化配置

确保重要数据持久化存储：

```yaml
services:
  mysql:
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backup:/backup
  
  backend:
    volumes:
      - ./storage/upload:/storage/upload
      - ./logs:/app/logs

volumes:
  mysql_data:
    driver: local
```

### 资源限制

配置容器资源限制：

```yaml
services:
  mysql:
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
  
  backend:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

### JVM 参数配置

在 `.env` 中配置 JVM 参数：

```bash
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

## 运维管理

### 服务管理命令

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重启单个服务
docker compose restart backend

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs [service-name]
docker compose logs -f backend

# 进入容器
docker compose exec backend bash
docker compose exec mysql mysql -u root -p
```

### 备份与恢复

#### 数据库备份

```bash
# 备份数据库
docker compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} astrnest > backup/astrnest-$(date +%Y%m%d).sql

# 恢复数据库
docker compose exec -i mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} astrnest < backup/astrnest-backup.sql
```

#### 文件备份

```bash
# 备份上传文件
cp -r storage/upload backup/upload-$(date +%Y%m%d)

# 恢复上传文件
cp -r backup/upload-20240101 storage/upload
```

### 更新服务

```bash
# 拉取最新代码
git pull origin main

# 重新构建镜像
docker compose build --no-cache

# 重启服务
docker compose down
docker compose up -d
```

## 监控与日志

### 查看容器状态

```bash
# 查看所有容器状态
docker stats

# 查看容器资源使用情况
docker container stats

# 查看容器详细信息
docker inspect astrnest-backend
```

### 日志管理

```bash
# 实时查看日志
docker compose logs -f backend

# 查看特定时间段的日志
docker compose logs --since="2024-01-01" backend

# 导出日志到文件
docker compose logs backend > backend-$(date +%Y%m%d).log
```

## 安全配置

### 修改默认密码

部署后立即修改默认管理员密码：

1. 登录系统后通过界面修改
2. 或在 `.env` 中修改 `ASTRNEST_ADMIN_PASSWORD` 后重启服务

### 配置 SSL/TLS

使用反向代理配置 HTTPS：

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /path/to/ssl/cert.pem;
    ssl_certificate_key /path/to/ssl/key.pem;
    
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /api/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /upload/ {
        proxy_pass http://localhost:8081/upload/;
        proxy_set_header Host $host;
        expires 7d;
        add_header Cache-Control "public";
    }
}
```

## 故障排除

### 容器启动失败

```bash
# 查看详细错误信息
docker compose logs [service-name]

# 检查端口冲突
netstat -tulpn | grep 8081

# 检查资源限制
docker stats
```

### 数据库连接问题

```bash
# 检查 MySQL 容器状态
docker compose logs mysql

# 测试数据库连接
docker compose exec mysql mysql -u astrnest -p${MYSQL_PASSWORD} -e "SHOW DATABASES;"
```

### 文件权限问题

```bash
# 检查挂载目录权限
ls -la ./storage/upload

# 修改目录权限
sudo chown -R $USER:$USER ./storage
```

## 优势与特点

### 优势

- **一键部署**: 简化复杂的部署流程，一条命令启动所有服务
- **环境隔离**: 每个服务运行在独立容器中，互不干扰
- **易于扩展**: 支持水平扩展和负载均衡
- **版本控制**: 便于回滚和版本管理
- **生产就绪**: 适合企业级生产环境

### 特点

- **自动故障恢复**: 容器异常时自动重启
- **资源管理**: 可配置 CPU 和内存限制
- **日志集中**: 统一的日志管理
- **网络隔离**: 安全的内部网络通信
- **数据持久化**: 重要数据持久化存储，容器重启不丢失

选择 Docker 部署方式，您可以获得稳定可靠的生产环境部署方案，适合团队协作和企业级应用。

## 下一步

部署成功后，建议：

1. [配置 AI 内容审核](/installation-guide#ai-内容审核配置)
2. [配置邮件服务](/installation-guide#邮件设置)
3. [配置对象存储](/installation-guide#对象存储配置示例)
4. [查看使用指南](/use/webui)
