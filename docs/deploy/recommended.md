# 推荐部署指南

> [!WARNING]
> **时效说明（2026-10）**：本文档转自文档站早期版本，端口已订正为实际的 **8081**。
> 管理员创建已改由**六步可视化安装向导**承担（文中 `ASTRNEST_ADMIN_*` 等环境变量方式已废弃），
> 部署细节以仓库根目录 **README.md** 与 `docs/audit-report-2026-10-05.md` 为准，本文仅作架构参考。


本指南提供多种部署方式，包括传统服务器部署、宝塔面板部署、Docker Compose 部署等，以及详细的云存储配置说明。

## 目录

- [前置准备](#前置准备)
- [方式一：传统服务器部署](#方式一传统服务器部署)
- [方式二：宝塔面板部署](#方式二宝塔面板部署)
- [方式三：Docker Compose 部署](#方式三docker-compose-部署)
- [数据库导入方法](#数据库导入方法)
- [腾讯云配置指南](#腾讯云配置指南)
- [阿里云 OSS 配置](#阿里云-oss-配置)
- [华为 OBS 配置](#华为-obs-配置)
- [其他云存储配置](#其他云存储配置)

---

## 前置准备

### 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 后端运行环境 |
| Node.js | 18+ | 前端构建环境 |
| MySQL | 5.7+/8.0 | 数据库服务 |
| Nginx | 1.20+ | 反向代理（推荐） |
| Maven | 3.8+ | 后端构建（或使用 Wrapper） |

### 服务器要求

- **最低配置**：2核4G内存，50GB存储
- **推荐配置**：4核8G内存，100GB存储
- **操作系统**：Ubuntu 20.04+/CentOS 7+/Debian 11+

---

## 方式一：传统服务器部署

### 1. 前后端打包

#### 后端打包

```bash
# 进入后端目录
cd backend

# 使用 Maven Wrapper 打包（推荐）
./mvnw clean package -DskipTests

# 或使用系统 Maven
mvn clean package -DskipTests

# 打包完成后，jar 文件位于 target/ 目录
ls target/*.jar
# 输出：target/backend-0.0.1-SNAPSHOT.jar
```

#### 前端打包

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 生产环境打包
npm run build

# 打包完成后，静态文件位于 dist/ 目录
ls dist/
```

### 2. 服务器部署步骤

#### 2.1 上传文件到服务器

```bash
# 创建应用目录
mkdir -p /opt/astrnest
mkdir -p /opt/astrnest/storage/upload

# 上传后端 jar 包
scp target/backend-0.0.1-SNAPSHOT.jar root@your-server:/opt/astrnest/

# 上传前端静态文件
scp -r dist/* root@your-server:/opt/astrnest/frontend/

# 上传数据库初始化脚本
scp backend/db/init.sql root@your-server:/opt/astrnest/
```

#### 2.2 配置 systemd 服务

创建后端服务文件 `/etc/systemd/system/astrnest-backend.service`：

```ini
[Unit]
Description=AstrNest Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=astrnest
WorkingDirectory=/opt/astrnest
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="ASTRNEST_DB_URL=jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
Environment="ASTRNEST_DB_USERNAME=astrnest"
Environment="ASTRNEST_DB_PASSWORD=your_db_password"
Environment="ASTRNEST_STORAGE_ROOT=/opt/astrnest/storage/upload"
Environment="ASTRNEST_STORAGE_PUBLIC=/upload"
Environment="ASTRNEST_ADMIN_PASSWORD=your_admin_password"
ExecStart=/usr/bin/java -jar /opt/astrnest/backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
# 重载 systemd
systemctl daemon-reload

# 启动后端服务
systemctl start astrnest-backend

# 设置开机自启
systemctl enable astrnest-backend

# 查看状态
systemctl status astrnest-backend
```

#### 2.3 Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /opt/astrnest/frontend;
        try_files $uri $uri/ /index.html;
        index index.html;
    }
    
    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 上传文件访问
    location /upload/ {
        alias /opt/astrnest/storage/upload/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # Swagger API 文档
    location /swagger-ui/ {
        proxy_pass http://127.0.0.1:8081/swagger-ui/;
    }
    
    # WebSocket 支持（如需要）
    location /ws/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

启用配置：

```bash
ln -s /etc/nginx/sites-available/astrnest /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

---

## 方式二：宝塔面板部署

### 1. 安装宝塔面板

```bash
# CentOS
yum install -y wget && wget -O install.sh https://download.bt.cn/install/install_6.0.sh && sh install.sh ed8484bec

# Ubuntu/Debian
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && sudo bash install.sh ed8484bec
```

### 2. 安装必要软件

在宝塔面板中安装：
- Nginx 1.20+
- MySQL 8.0
- Java 项目管理器（安装 JDK 21）

### 3. 创建网站

1. 登录宝塔面板
2. 点击「网站」→「添加站点」
3. 填写域名，选择 PHP 版本为「纯静态」
4. 创建数据库（记录数据库名、用户名、密码）

### 4. 部署后端

#### 方法一：使用 Java 项目管理器

1. 安装 Java 项目管理器插件
2. 点击「Java 项目」→「添加项目」
3. 项目类型选择「Spring Boot」
4. 填写项目信息：
   - 项目路径：`/www/wwwroot/astrnest/backend`
   - 启动文件：`backend-0.0.1-SNAPSHOT.jar`
   - 端口：`8081`
   - JDK 版本：`21`

#### 方法二：手动部署

```bash
# 上传 jar 包到 /www/wwwroot/astrnest/backend/

# 创建启动脚本 /www/wwwroot/astrnest/start.sh
#!/bin/bash
export ASTRNEST_DB_URL="jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
export ASTRNEST_DB_USERNAME="your_db_user"
export ASTRNEST_DB_PASSWORD="your_db_password"
export ASTRNEST_STORAGE_ROOT="/www/wwwroot/astrnest/storage/upload"
cd /www/wwwroot/astrnest/backend
nohup java -jar backend-0.0.1-SNAPSHOT.jar > ../logs/app.log 2>&1 &
```

### 5. 部署前端

```bash
# 在本地构建前端
cd frontend
npm install
npm run build

# 将 dist 目录内容上传到宝塔
# 目标路径：/www/wwwroot/your-domain.com/
```

### 6. 配置反向代理

在宝塔面板中：
1. 点击网站 → 设置 → 反向代理
2. 添加反向代理：
   - 代理名称：`backend`
   - 目标 URL：`http://127.0.0.1:8081`
   - 发送域名：`$host`

### 7. 配置伪静态

```nginx
location / {
    try_files $uri $uri/ /index.html;
}

location /api/ {
    proxy_pass http://127.0.0.1:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /upload/ {
    alias /www/wwwroot/astrnest/storage/upload/;
    expires 30d;
}
```

---

## 方式三：Docker Compose 部署

### 1. 准备环境变量文件

创建 `.env` 文件：

```bash
cp .env.example .env
```

编辑 `.env`：

```bash
# 数据库配置
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=astrnest
MYSQL_USER=astrnest
MYSQL_PASSWORD=your_db_password

# 后端配置
ASTRNEST_DB_URL=jdbc:mysql://mysql:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
ASTRNEST_DB_USERNAME=astrnest
ASTRNEST_DB_PASSWORD=your_db_password
ASTRNEST_STORAGE_ROOT=/storage/upload
ASTRNEST_STORAGE_PUBLIC=/upload
ASTRNEST_ADMIN_PASSWORD=your_admin_password

# 站点配置
PUBLIC_SITE_URL=https://your-domain.com
VITE_API_BASE_URL=http://backend:8081
```

### 2. 启动服务

```bash
docker compose --env-file .env up -d
```

### 3. 查看日志

```bash
# 所有服务日志
docker compose logs -f

# 后端日志
docker compose logs -f backend

# 数据库日志
docker compose logs -f mysql
```

---

## 数据库导入方法

### Linux/macOS

```bash
mysql -u root -p < backend/db/init.sql
```

### Windows

使用 `init_windows.sql`（移除了 DELIMITER 语法，兼容 Navicat）：

```bash
mysql -u root -p < backend/db/init_windows.sql
```

### 手动导入（Navicat）

1. 用 root 账号连接 MySQL
2. 创建数据库 `astrnest`（字符集 utf8mb4）
3. 右键数据库 → 运行 SQL 文件 → 选择 `init_windows.sql`

### 常见权限问题

如果报错 `Access denied for user 'astrnest'@'localhost'`：

```sql
-- 用 root 执行
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'astrnest'@'localhost' IDENTIFIED BY 'your_password';
CREATE USER IF NOT EXISTS 'astrnest'@'%' IDENTIFIED BY 'your_password';

GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'localhost';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';

FLUSH PRIVILEGES;
```

---

## 腾讯云配置指南

### 1. 注册腾讯云账号

1. 访问 [腾讯云官网](https://cloud.tencent.com/)
2. 点击右上角「免费注册」
3. 选择注册方式（微信/QQ/邮箱/企业微信）
4. 完成实名认证（个人/企业）

### 2. 开通对象存储 COS

1. 登录腾讯云控制台
2. 搜索「对象存储」→ 进入 COS 控制台
3. 点击「创建存储桶」
4. 配置存储桶：
   - **名称**：`astrnest-images`（全局唯一）
   - **地域**：选择离用户最近的地区（如 ap-beijing）
   - **访问权限**：私有读写（推荐）或公有读私有写
   - **版本控制**：按需开启

### 3. 获取 API 密钥

1. 点击右上角头像 → 「访问管理」
2. 左侧菜单「访问密钥」→ 「API 密钥管理」
3. 点击「新建密钥」
4. 记录 `SecretId` 和 `SecretKey`（**Key 只显示一次，请妥善保存**）

### 4. 配置内容审核（可选）

1. 在 COS 控制台 → 数据万象 CI
2. 开通「内容审核」服务
3. 创建审核策略，配置敏感内容检测规则
4. 获取审核服务的 SecretId/SecretKey（可与 COS 共用）

### 5. AstrNest 配置

在 `.env` 或 `application.yml` 中配置：

```yaml
astrnest:
  storage:
    strategy: TENCENT_COS
    cos:
      enabled: true
      endpoint: https://cos.ap-beijing.myqcloud.com
      region: ap-beijing
      bucket: astrnest-images-1250000000  # 格式：名称-APPID
      access-key: ${TENCENT_SECRET_ID}
      secret-key: ${TENCENT_SECRET_KEY}
      path-style: false
      accelerate: false
```

或在 `.env`：

```bash
ASTRNEST_STORAGE_STRATEGY=TENCENT_COS
TENCENT_SECRET_ID=your-secret-id
TENCENT_SECRET_KEY=your-secret-key
TENCENT_REGION=ap-beijing
TENCENT_BUCKET=astrnest-images-1250000000
```

---

## 阿里云 OSS 配置

### 1. 开通 OSS 服务

1. 登录 [阿里云控制台](https://www.aliyun.com/)
2. 搜索「对象存储 OSS」
3. 点击「创建 Bucket」
4. 配置：
   - **Bucket 名称**：`astrnest-images`（全局唯一）
   - **地域**：选择最近的地域（如 oss-cn-hangzhou）
   - **存储类型**：标准存储
   - **读写权限**：私有（推荐）

### 2. 获取 AccessKey

1. 点击右上角头像 → 「AccessKey 管理」
2. 创建 AccessKey
3. 记录 `AccessKey ID` 和 `AccessKey Secret`

### 3. AstrNest 配置

```yaml
astrnest:
  storage:
    strategy: ALIYUN_OSS
    oss:
      enabled: true
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      bucket: astrnest-images
      access-key: ${ALIYUN_ACCESS_KEY_ID}
      secret-key: ${ALIYUN_ACCESS_KEY_SECRET}
      cdn-host: https://cdn.your-domain.com  # 可选，配置 CDN 加速域名
```

---

## 华为 OBS 配置

### 1. 开通 OBS 服务

1. 登录 [华为云控制台](https://www.huaweicloud.com/)
2. 搜索「对象存储服务 OBS」
3. 创建桶：
   - **桶名称**：`astrnest-images`
   - **区域**：选择最近的区域
   - **存储类别**：标准存储

### 2. 获取 AK/SK

1. 右上角头像 → 「我的凭证」
2. 「访问密钥」→ 「新增访问密钥」
3. 下载 CSV 文件保存 AK/SK

### 3. AstrNest 配置

```yaml
astrnest:
  storage:
    strategy: HUAWEI_OBS
    obs:
      enabled: true
      endpoint: https://obs.cn-north-4.myhuaweicloud.com
      bucket: astrnest-images
      access-key: ${HUAWEI_ACCESS_KEY}
      secret-key: ${HUAWEI_SECRET_KEY}
```

---

## 其他云存储配置

### 又拍云 USS

```yaml
astrnest:
  storage:
    strategy: UPYUN_USS
    upyun:
      enabled: true
      bucket: your-bucket
      operator: your-operator
      password: your-operator-password
      endpoint: https://v0.api.upyun.com
      cdn-host: https://your-domain.b0.upaiyun.com
```

### 七牛云 Kodo

```yaml
astrnest:
  storage:
    strategy: QINIU_KODO
    qiniu:
      enabled: true
      bucket: your-bucket
      access-key: your-ak
      secret-key: your-sk
      domain: https://your-domain.qiniudn.com
```

### 通用 S3 兼容存储

适用于 MinIO、AWS S3、Ceph 等：

```yaml
astrnest:
  storage:
    strategy: S3_COMPATIBLE
    s3:
      enabled: true
      endpoint: https://s3.amazonaws.com
      region: us-east-1
      bucket: your-bucket
      access-key: your-access-key
      secret-key: your-secret-key
      path-style: false
```

---

## 密钥安全管理

### 环境变量方式（推荐）

```bash
# 写入 ~/.bashrc 或 ~/.zshrc
export TENCENT_SECRET_ID=your-secret-id
export TENCENT_SECRET_KEY=your-secret-key

# 应用生效
source ~/.bashrc
```

### Docker Secrets（生产环境）

```yaml
# docker-compose.yml
secrets:
  cos_secret_id:
    file: ./secrets/cos_secret_id.txt
  cos_secret_key:
    file: ./secrets/cos_secret_key.txt

services:
  backend:
    secrets:
      - cos_secret_id
      - cos_secret_key
```

### 密钥轮换建议

1. 定期（3-6个月）更换 API 密钥
2. 使用子账号/临时凭证，限制权限范围
3. 开启操作日志审计
4. 不要在代码仓库中硬编码密钥

---

## 故障排查

### 后端无法启动

```bash
# 检查端口占用
netstat -tulpn | grep 8081

# 检查日志
tail -f /opt/astrnest/logs/app.log

# 检查数据库连接
mysql -u astrnest -p -e "SELECT 1"
```

### 前端无法访问 API

1. 检查 Nginx 反向代理配置
2. 检查后端服务是否运行
3. 检查 CORS 配置
4. 查看浏览器开发者工具 Network 面板

### 上传失败

1. 检查存储目录权限：`chmod 755 /opt/astrnest/storage/upload`
2. 检查磁盘空间：`df -h`
3. 检查文件大小限制（默认 20MB）
4. 查看后端错误日志

---

## 性能优化建议

1. **启用 CDN**：配置腾讯云/阿里云 CDN 加速静态资源
2. **数据库优化**：添加索引、配置连接池
3. **缓存策略**：配置 Nginx 缓存、浏览器缓存
4. **Gzip 压缩**：启用 Nginx gzip 压缩
5. **HTTP/2**：配置 Nginx 支持 HTTP/2

---

如需更多帮助，请参考：
- [安装配置指南](/installation-guide)
- [API 文档](/api/)
- [常见问题](/faq)
