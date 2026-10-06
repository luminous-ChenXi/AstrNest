# 直接部署

> [!WARNING]
> **时效说明（2026-10）**：本文档转自文档站早期版本，端口已订正为实际的 **8081**。
> 管理员创建已改由**六步可视化安装向导**承担（文中 `ASTRNEST_ADMIN_*` 等环境变量方式已废弃），
> 部署细节以仓库根目录 **README.md** 与 `docs/audit-report-2026-10-05.md` 为准，本文仅作架构参考。


直接部署方式适合开发环境或需要深度定制的场景，通过手动配置和启动各个组件。

## 前置要求

- **Java 21**: OpenJDK 21 或 Temurin 21
- **Node.js 18+**: 用于前端构建
- **MySQL 5.7+/8.0**: 数据库服务
- **Maven**: 项目自带 Maven Wrapper，无需单独安装

## 部署步骤

### 1. 环境准备

```bash
# 检查 Java 版本
java -version

# 检查 Node.js 版本
node --version

# 检查 MySQL 版本
mysql --version
```

### 2. 数据库初始化

```bash
# Linux/macOS
mysql -u root -p < backend/db/init.sql

# Windows（使用 init_windows.sql）
mysql -u root -p < backend/db/init_windows.sql
```

或手动创建数据库：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 创建专用用户
CREATE USER IF NOT EXISTS 'astrnest'@'localhost' IDENTIFIED BY 'astrnestPass!';
CREATE USER IF NOT EXISTS 'astrnest'@'%' IDENTIFIED BY 'astrnestPass!';

-- 授予权限
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'localhost';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';
FLUSH PRIVILEGES;
```

### 3. 配置环境变量

创建 `.env` 文件（基于 `.env.example`）：

```bash
# 数据库配置
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=astrnest
MYSQL_USER=astrnest
MYSQL_PASSWORD=astrnestPass!

# 后端配置
ASTRNEST_DB_URL=jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
ASTRNEST_DB_USERNAME=astrnest
ASTRNEST_DB_PASSWORD=astrnestPass!
ASTRNEST_STORAGE_ROOT=./storage/upload
ASTRNEST_STORAGE_PUBLIC=/upload
ASTRNEST_ADMIN_USERNAME=admin
ASTRNEST_ADMIN_PASSWORD=ChangeMe_Admin123!
ASTRNEST_ADMIN_DISPLAY=超级管理员
ASTRNEST_ADMIN_EMAIL=admin@example.com

# 前端配置
VITE_API_BASE_URL=http://localhost:8081
VITE_PUBLIC_ASSET_BASE=http://localhost:8081/upload
VITE_SITE_NAME=AstrNest
```

### 4. 启动后端服务

```bash
# 进入后端目录
cd backend

# 使用 Maven Wrapper 启动（推荐）
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run

# 或者构建后运行
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

后端服务将在 http://localhost:8081 启动。

### 5. 启动前端服务

```bash
# 新终端，进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 或者构建后预览
npm run build
npm run preview
```

前端服务将在 http://localhost:5173 启动。

## 验证部署

访问以下地址验证服务是否正常：

- **前端界面**: http://localhost:5173
- **后端 API**: http://localhost:8081
- **API 文档**: http://localhost:8081/swagger-ui/index.html
- **健康检查**: http://localhost:8081/actuator/health

## 默认管理员账号

- **用户名**: `admin`
- **密码**: `ChangeMe_Admin123!`

## Windows 特殊说明

### 中文乱码问题

如果看到日志里中文变成乱码，请设置 UTF-8 编码：

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
```

### 数据库初始化

Windows 用户请使用 `backend/db/init_windows.sql`，该脚本移除了 `DELIMITER` 等 Windows/Navicat 不兼容的语法。

## 服务管理

### 使用 PM2 管理进程（生产环境）

```bash
# 安装 PM2
npm install -g pm2

# 启动后端服务
cd backend
./mvnw clean package
pm2 start "java -jar target/backend-0.0.1-SNAPSHOT.jar" --name "astrnest-backend"

# 启动前端服务（构建后的静态文件）
cd frontend
npm run build
pm2 serve dist 5173 --name "astrnest-frontend"

# 查看服务状态
pm2 status
pm2 save
```

### 使用 systemd 管理服务（Linux）

创建后端服务文件 `/etc/systemd/system/astrnest-backend.service`：

```ini
[Unit]
Description=AstrNest Backend Service
After=network.target

[Service]
Type=simple
User=astrnest
WorkingDirectory=/opt/astrnest/backend
ExecStart=/usr/bin/java -jar target/backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable astrnest-backend
sudo systemctl start astrnest-backend
```

## 常见问题

### 端口冲突

如果端口被占用，可以修改配置：

```bash
# 后端端口修改
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# 前端端口修改（开发模式）
npm run dev -- --port=3000
```

### 数据库连接失败

检查 MySQL 服务状态和连接配置：

```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql

# 检查防火墙设置
sudo ufw status
```

### 内存不足

增加 JVM 内存配置：

```bash
export JAVA_OPTS="-Xmx2g -Xms1g"
./mvnw spring-boot:run
```

### Maven 权限错误

```bash
# 清理并重新构建
rm -rf target
./mvnw clean compile
```

## 优势与限制

### 优势

- 完全控制各个组件
- 便于调试和开发
- 灵活配置和定制
- 适合学习和理解系统

### 限制

- 部署复杂度较高
- 需要手动管理服务
- 不适合大规模生产环境（建议使用 Docker Compose）

选择直接部署方式，您可以完全掌控 AstrNest 的每一个组件，适合深度定制和开发调试。
