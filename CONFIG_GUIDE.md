面向运维/开发的落地说明：列出必须填的环境变量、文件路径、初始化脚本与部署要点。建议按顺序完成，最后再启动 Docker Compose 或本地进程。

<img src="./templates/2364x1773.png" width = "400" height = "300" alt="AstrNest" align=right />
<div align="center">

## AstrNest 安装与配置指南

面向运维/开发的落地说明：列出必须填的环境变量、文件路径、初始化脚本与部署要点。建议按顺序完成，最后再启动 Docker Compose 或本地进程。有多种部署方式，可以根据需要选取最适合自己的。有的人可能会问，为什么不使用像wordpress一样可视化初始页面的安装方法，首先就是安全性问题，因为wordpress的安装页面是暴露在公网上的，任何人均可以访问，这就意味着任何人均可以安装wordpress，包括一些不怀好意的人。而AstrNest的安装页面是隐藏在登录页面后面的，只有登录了才能访问，这就意味着只有管理员才可以安装AstrNest。
WordPress的安装流程（存在安全隐患）：
1. 访问 /wp-admin/install.php
2. 检查 wp-config.php 是否存在
3. 不存在 → 显示安装页面
4. 输入数据库信息、管理员账户
5. 安装完成后删除 install.php 吗？❌ 不删除！

安全问题：
- 攻击者可以删除 wp-config.php 文件 → 重新触发安装
- 攻击者可以猜测 install.php 路径 → 尝试重装
- 安装后页面依然存在 → 可能被利用

</div>


### 1. 快速清单
| 项目 | 必填内容 | 修改位置 |
| --- | --- | --- |
| 数据库 | 主机、端口、库名、账号、密码 | `.env` / `backend/src/main/resources/application.yml` / `docker-compose.yml` (`mysql`/`backend`) / `backend/db/init.sql` |
| 管理员 | 用户名、初始密码、昵称、邮箱 | `.env` + `backend/db/init.sql` (`INSERT INTO users ...`) |
| 上传路径 | 本地磁盘目录、对外访问前缀 | `.env` (`ASTRNEST_STORAGE_ROOT`/`ASTRNEST_STORAGE_PUBLIC`)，必要时 Nginx 反代 `/upload/**` |
| 站点域名 | 站点主页、图片直链、API 域名 | `.env` (`PUBLIC_SITE_URL`/`PUBLIC_ASSET_URL`/`BACKEND_API_PUBLIC_URL`/`ASTRNEST_ASSET_DOMAIN`)，后台“系统配置”里的 `asset_domain` |
| 腾讯云 AI 审核 | SecretId/SecretKey、Region、Bucket、场景、阈值 | 后台「系统配置 > AI 智能审核」或 `.env` 中的 `ASTRNEST_AI_*` 兜底 |
| JVM 参数 | 如 `-Xms512m -Xmx512m` | `.env` / `docker-compose.yml`（透传给 Backend 容器） |
| 前端 API 地址 | 后端访问地址 | `frontend/.env.*` 或构建命令行 `VITE_API_BASE_URL` |
| 图片直链 | CDN/域名或相对路径 | `VITE_PUBLIC_ASSET_BASE`，或后台 `asset_domain` |
| 邮件服务 | SMTP Host/Port/账号/授权码/发件人 | 后台「设置 > 邮件服务」，或 `backend/db/init.sql` 中的 `chenxi_mail_config` 段 |

### 1.1 文件路径速查
| 文件 | 关键参数 | 适用场景 |
| --- | --- | --- |
| `.env`（根目录） | `MYSQL_*`、`ASTRNEST_DB_*`、`ASTRNEST_STORAGE_*`、`ASTRNEST_ADMIN_*`、`PUBLIC_SITE_URL`、`PUBLIC_ASSET_URL`、`BACKEND_API_PUBLIC_URL`、`ASTRNEST_ASSET_DOMAIN`、`VITE_API_BASE_URL`、`VITE_PUBLIC_ASSET_BASE`、`SMTP_*`、`ASTRNEST_AI_*` | Docker Compose 与本地 shell 的统一入口 |
| `docker-compose.yml` | `services.mysql.environment.*`、`services.backend.environment.*`、`services.frontend.environment.*` | 如需为特定环境写死参数，可在对应服务的 `environment` 覆盖 `.env` |
| `backend/src/main/resources/application.yml` | `spring.datasource.*`、`astrnest.storage.*`、`astrnest.admin.*` | 裸机运行或 IDE 调试的兜底配置（可被环境变量覆盖） |
| `backend/db/init.sql` | 建库/授权、管理员、SMTP 占位、系统配置 | 初始化数据库结构与默认数据，支持重复执行补列 |
| `frontend/.env.development` / `.env.production` | `VITE_API_BASE_URL`、`VITE_PUBLIC_ASSET_BASE`、`VITE_SITE_NAME` | 控制前端请求后端与直链拼接 |
| `frontend/src/services/http.js` | axios baseURL | 如不依赖 `.env`，可直接写死后端地址 |

> 建议：先 `cp .env.example .env`，逐项填好 → 调整 `application.yml`、`init.sql`、`frontend/.env.production`，再启动。

### 1.2 重要安全与必填配置（请优先落实）
- **CORS 跨域配置**：必须配置允许的前端域名，否则浏览器会阻止 API 请求。
  - 修改文件：`backend/src/main/java/com/chenxi/astrnest/config/CorsProperties.java`
  - 默认已包含：`http://localhost:5173`、`http://127.0.0.1:5173`
  - 局域网开发：添加 `http://192.168.x.x:5173`（替换为你的局域网 IP）
  - 生产环境：添加 `https://yourdomain.com`、`https://www.yourdomain.com`
  - ⚠️ **生产环境严禁使用 `*`**，必须显式指定允许的域名
- **数据库**：`ASTRNEST_DB_URL`、`ASTRNEST_DB_USERNAME`、`ASTRNEST_DB_PASSWORD`；可选池参数：`ASTRNEST_DB_MIN_IDLE`、`ASTRNEST_DB_MAX_POOL_SIZE`。
- **管理员账号**：通过 `init-admin.bat` (Windows) 或 `init-admin.sh` (Linux/macOS) 脚本设置，自动加密并写入数据库初始化文件。
- **上传/存储**：`ASTRNEST_STORAGE_ROOT`、`ASTRNEST_STORAGE_PUBLIC`；切换云存储时填写对应密钥（COS/OSS/S3/Upyun/OneDrive 等）到环境变量，勿写入代码。
- **邮件发信**：`SMTP_HOST`、`SMTP_PORT`、`SMTP_USERNAME`、`SMTP_PASSWORD`、`SMTP_FROM`（建议使用授权码/应用专用密码）。
- **API/直链域名**：`PUBLIC_SITE_URL`、`PUBLIC_ASSET_URL`、`BACKEND_API_PUBLIC_URL`、`ASTRNEST_ASSET_DOMAIN`、前端 `VITE_API_BASE_URL`、`VITE_PUBLIC_ASSET_BASE`。
- **AI 审核密钥**：`ASTRNEST_AI_TENCENT_SECRET_ID`、`ASTRNEST_AI_TENCENT_SECRET_KEY`、`ASTRNEST_AI_REGION` 等，放环境变量。
- **JVM**：`JAVA_OPTS`（内存/GC 可按部署环境设置）。

> 密钥、数据库口令、邮件授权码等一律放环境变量或密钥管理服务（KMS/Vault），不要提交到代码库或镜像。

### 1.3 环境变量设置指南（以 `ASTRNEST_DB_PASSWORD=ChangeMe_123!` 为例）
- **.env 的作用**：Compose/脚本统一入口，供 docker compose 与 shell 读取。模板是 `.env.example`，实际生效的是 `.env`，需填入真实值（与 MySQL 实例一致），同时保持 `MYSQL_PASSWORD` 与 `ASTRNEST_DB_PASSWORD` 一致。
- **Windows PowerShell（当前会话）**：`$Env:ASTRNEST_DB_PASSWORD="ChangeMe_123!"`，然后在同一终端启动后端。
- **Windows 永久环境变量**：控制面板 → 系统 → 高级系统设置 → 环境变量，新增/修改用户或系统变量 `ASTRNEST_DB_PASSWORD`。
- **Docker Compose**：在 `.env`（非 `.env.example`）写 `ASTRNEST_DB_PASSWORD=ChangeMe_123!`，并确保 `MYSQL_PASSWORD` 匹配 MySQL 实际密码；保存后 `docker compose --env-file .env up -d`。
- **Linux/macOS**：`export ASTRNEST_DB_PASSWORD="ChangeMe_123!"`，随后在同一会话运行后端。

> 建议：生产环境统一用环境变量/密钥管理器注入数据库口令，避免直接写入 `application.yml`。

### 2. 环境变量示例 (.env)
```bash
cp .env.example .env
# 编辑 .env 后被 docker compose / shell 读取
```
重点与默认值（可根据实际修改）：
- `MYSQL_ROOT_PASSWORD` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD`
- `ASTRNEST_DB_URL=jdbc:mysql://mysql:3306/astrnest?...`（Docker 下可用服务名 `mysql`，裸机改真实主机）
- `ASTRNEST_STORAGE_ROOT=/storage/upload`、`ASTRNEST_STORAGE_PUBLIC=/upload`
- `ASTRNEST_ASSET_DOMAIN`、`PUBLIC_SITE_URL`、`PUBLIC_ASSET_URL`、`BACKEND_API_PUBLIC_URL`
- `ASTRNEST_ADMIN_*`（用户名/密码/邮箱/展示名）
- `ASTRNEST_AI_*`（腾讯云审核密钥/阈值兜底）
- `ASTRNEST_MULTIPART_MAX_FILE_SIZE` / `ASTRNEST_MULTIPART_MAX_REQUEST_SIZE`（保持 ≤20MB，匹配 DB CHECK）
- `JAVA_OPTS`（可选 JVM 参数）
- `SMTP_*`（与 init.sql 中占位值一致或启动后在后台填写）
- `VITE_API_BASE_URL` / `VITE_PUBLIC_ASSET_BASE` / `VITE_SITE_NAME`

国内镜像 404：npm 请切官方源 `npm config set registry https://registry.npmjs.org/`；Maven 需确保使用官方或可用镜像。

### 3. 后端配置 (application.yml)
- 位置：`backend/src/main/resources/application.yml`
- 所有键可被同名环境变量覆盖，示例：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: astrnest
    password: ChangeMe_123!
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

astrnest:
  storage:
    strategy: LOCAL
    local:
      root: /storage/upload
      public-base-url: /upload
  admin:
    username: admin
    password: ChangeMe_123!
    display-name: 超级管理员
    email: admin@example.com
```
> `public-base-url` 需以 `/upload` 开头（相对路径）；如要 CDN 域名，请在后台“系统配置”填写 `asset_domain`。

### 4. 数据库初始化与管理员工具

AstrNest 提供了自动化脚本来简化数据库初始化和管理员账号配置。

#### 4.0 推荐使用自动化脚本（强烈推荐）

**Windows 用户**：
```bash
# 方式1：双击运行（推荐）
init-admin-cn.bat

# 方式2：命令行执行
python init-admin.py
```

**Linux / macOS 用户**：
```bash
# 添加执行权限后运行
chmod +x init-admin.sh
./init-admin.sh
```

**脚本功能（4 步流程）**：
1. **自动检测系统**：根据操作系统（Windows/Linux/macOS）自动选择对应的 SQL 文件
2. **Root 账号配置**：输入 MySQL root 账号信息，测试连接
3. **应用用户配置**：
   - 自动检查 `astrnest` 用户是否存在
   - 如果已存在，提供 3 个选项：
     - 使用现有用户（验证密码）
     - 重新设置密码（修改现有用户密码）
     - 退出
   - 使用 root 账号创建数据库和 `astrnest` 用户
4. **管理员账号配置**：设置管理员用户名、邮箱、密码
5. **SQL 初始化**：
   - 自动使用 bcrypt 加密管理员密码
   - **Windows**：更新 `init_windows.sql`（不包含 CREATE USER/GRANT）
   - **Linux/macOS**：更新 `init.sql`（包含完整权限设置）
   - 使用 `astrnest` 用户执行 SQL 文件，创建表结构和初始数据

**平台差异处理**：
- **Windows**：脚本自动创建数据库和用户（因为 init_windows.sql 不包含 CREATE USER/GRANT）
- **Linux/macOS**：SQL 文件中已包含 CREATE USER/GRANT，脚本只创建数据库
- **Ubuntu 24+**：自动创建 Python 虚拟环境（系统禁止在 base 环境安装包）

**脚本优势**：
- ✅ 自动处理用户存在性检查和密码验证
- ✅ 自动创建数据库和应用用户
- ✅ 密码自动 bcrypt 加密
- ✅ 使用应用用户（而非 root）执行 SQL，更安全
- ✅ 交互式引导，无需手动编辑 SQL 文件

#### 4.1 手动执行 SQL 文件（备选方案）

如果自动化脚本无法使用，可以手动执行 SQL 文件：

**Linux / macOS**：
```bash
mysql -u root -p < backend/db/init.sql
```

**Windows（Navicat / GUI 导入兼容说明）**
在 Windows 上使用 Navicat “导入 SQL 文件”时，可能会因为以下原因失败：
- 权限不足（`CREATE USER` / `GRANT` / `FLUSH PRIVILEGES` 需要高权限账号）；
- `DELIMITER` / 多语句 / `PREPARE ... EXECUTE ...` 动态 SQL 在某些导入模式下会被拆分导致语法错误。

为此项目已提供 **Windows/Navicat 友好版脚本**：`backend/db/init_windows.sql`。
- 该脚本用于“新库初始化”更稳定（移除了大量动态迁移段、触发器改为不依赖 `DELIMITER` 的写法）。
- **注意**：`init_windows.sql` 默认不做 `CREATE USER/GRANT`，避免普通连接账号导入时报错；因此需要你额外手工授权（见 4.3）。

**推荐导入流程（Navicat）**：
1) 用 `root`（或 DBA）在 Navicat 先手动创建数据库 `astrnest`（字符集 `utf8mb4`）。
2) 选中库 `astrnest` 后导入并执行 `backend/db/init_windows.sql`。
3) 如导入时报“没有 CREATE VIEW / CREATE TRIGGER 权限”，请用更高权限账号执行导入，或临时注释掉 `CREATE VIEW` / `CREATE TRIGGER` 段。

#### 4.2 常见启动报错：1044 / 42000（账号能登录但无库权限）
如果你在命令行能 `mysql -u astrnest -p` 登录，但后端启动报错：
- `SQL Error: 1044, SQLState: 42000`
- `Access denied for user 'astrnest'@'localhost' to database 'astrnest'`

说明：**账号/密码正确，但 `astrnest@host` 没有 `astrnest.*` 的权限**。用 `root`（或 DBA）执行下面 SQL：

```sql
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'astrnest'@'localhost' IDENTIFIED BY 'ChangeMe_123!';
CREATE USER IF NOT EXISTS 'astrnest'@'%' IDENTIFIED BY 'ChangeMe_123!';

GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'localhost';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';

FLUSH PRIVILEGES;
```

> 说明：MySQL 用户是按 `用户@Host` 匹配的，常见坑是只授权了 `astrnest@'%'`，但实际连接被识别成 `astrnest@'localhost'`（或反过来）。

验证方式：
```sql
SHOW GRANTS FOR 'astrnest'@'localhost';
SHOW GRANTS FOR 'astrnest'@'%';
USE astrnest;
SHOW TABLES;
```

- 可重复执行（脚本使用 `IF NOT EXISTS` / `ON DUPLICATE KEY` 补列）。

### 5. 前端环境文件
- `frontend/.env.development`、`.env.production` 或命令行注入：
```env
VITE_API_BASE_URL=https://api.example.com
VITE_PUBLIC_ASSET_BASE=https://cdn.example.com/upload
VITE_SITE_NAME=AstrNest
```

### 6. 存储与直链
- 本地存储默认写入 `${ASTRNEST_STORAGE_ROOT}/{yyyy}/{MM}/文件`，对外路径 `${ASTRNEST_STORAGE_PUBLIC}/{yyyy}/{MM}/...`
- 反向代理示例（Nginx）：
```nginx
location /upload/ {
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8080/upload/;
}
```
- CDN/自定义域：后台 `asset_domain` 或环境变量 `ASTRNEST_ASSET_DOMAIN`；前端复制直链由 `VITE_PUBLIC_ASSET_BASE` 控制。
- 切换对象存储：在 `astrnest.storage.*` 下填写对应节点（oss/cos/s3/upyun/onedrive 等）并提供密钥/桶/域名。

### 7. AI 审核
- 管理后台：系统配置 → AI 智能审核，填写 SecretId/SecretKey、Region、Bucket、检测场景、拦截/复核/标签阈值。
- 环境变量兜底：`ASTRNEST_AI_*`（当数据库未填时读取）。
- 官方参考：<https://cloud.tencent.com/document/product/460/37318>

### 8. 视频缩略图（FFmpeg）
- 配置键：`astrnest.video-thumbnail.*` 或 `ASTRNEST_VIDEO_THUMBNAIL_*`（enabled、ffmpeg-path、capture-offset、quality、timeout-seconds、cover-directory、max-load-average）。
- 安装建议：
  - Debian/Ubuntu：`sudo apt-get install -y ffmpeg`
  - CentOS/RHEL：`sudo yum install -y ffmpeg`（或 `dnf`）
  - macOS：`brew install ffmpeg`
  - Windows：下载官方 build，将 `bin` 加入 PATH；或者使用 `chocolatey` / `winget` 安装（<https://community.chocolatey.org/packages/ffmpeg>；`choco install ffmpeg`、`winget install ffmpeg`）

#### 8.1 常见日志：找不到 ffmpeg（不影响服务启动）
如果日志出现类似：
- `Cannot run program "ffmpeg": error=2, No such file or directory`

说明后端在生成视频封面/缩略图时找不到 `ffmpeg`，一般**不影响后端启动**，但会导致视频封面生成失败。
- 解决方案 A（推荐）：安装 ffmpeg 并确保命令行可直接执行 `ffmpeg`；
- 解决方案 B：如果你暂时不需要视频封面，设置环境变量 `ASTRNEST_VIDEO_THUMBNAIL_ENABLED=false`。

### 9. 邮件设置
- 启动后：后台「设置 > 邮件服务」填 SMTP 主机/端口/账号/授权码/发件人。
- 若希望脚本写入真实值：编辑 `backend/db/init.sql` 中 `INSERT INTO chenxi_mail_config` 段。

### 10. Docker Compose 部署
1) `cp .env.example .env` 并填好所有占位。
2) 执行：
```bash
docker compose --env-file .env up -d
```
3) 默认行为：启动 MySQL、导入 `init.sql`、启动 backend(8080) 与 frontend(80)，挂载 `./storage/upload`。
4) 如需 JVM/调试参数：在 `.env` 设置 `JAVA_OPTS`，Compose 会透传。

### 11. 传统部署
- 后端：`cd backend && ./mvnw clean package && java -jar target/backend-0.0.1-SNAPSHOT.jar`(bash)
`cd backend ; ./mvnw clean package ; java -jar target/backend-0.0.1-SNAPSHOT.jar`(PowerShell)
- 前端：`cd frontend && npm run build`，将 `dist/` 交给 Nginx/CDN。

#### 11.1 Windows 启动命令
Windows 下建议使用 Maven Wrapper：
- 编译/运行：`cd backend && .\mvnw.cmd spring-boot:run`
- 打包：`cd backend && .\mvnw.cmd clean package`

> 说明：如果直接使用 `mvn` 报 “不是内部或外部命令”，说明你本机没有安装 Maven 或未加入 PATH；使用项目自带的 `mvnw.cmd` 可以避免这个问题。

#### 11.2 Windows 控制台中文日志乱码（UTF-8 编码）
如果你看到日志里中文变成类似 `鏈娴...` 的乱码，通常不是业务问题，而是 **终端编码与 Java 输出编码不一致**。

**推荐做法（Windows Terminal / PowerShell）**：
1) 先把当前会话切到 UTF-8：
```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
```
2) 再启动后端：
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**IDE 控制台（IntelliJ IDEA）**：
- 将 `Settings > Editor > File Encodings` 设置为 UTF-8；
- 运行配置（Run/Debug Configuration）里确保控制台编码为 UTF-8（不同版本位置略有差异）。

> 示例：这条乱码日志的原文一般是 `未检测到激活的存储策略，沿用配置文件默认值: LOCAL`。

### 12. 收尾检查
- `.env`、`application.yml`、`frontend/.env*` 均已替换真实地址/账号。
- `backend/db/init.sql`（或 Windows 下的 `backend/db/init_windows.sql`）中管理员邮箱/SMTP/资产域名已调整。
- 访问健康检查：`http://{server}:8080/actuator/health` 应为 `UP`；`/upload/{yyyy}/{MM}/...` 能返回 200/404（不应 502）。

### 12.1 ⚠️ 重要坑点：Windows 本地开发环境变量配置不生效

#### 12.1.0 问题现象
在 Windows 本地使用 `./mvnw spring-boot:run` 启动后端时，发现：
- 修改了 `.env` 文件，但配置不生效
- 修改了 `application.yml`，但某些配置仍不生效
- 登录时提示密码错误，即使确认了密码配置正确

#### 12.1.1 根本原因

**1. `.env` 文件不会被 Spring Boot 自动读取**
```
.env 文件只适用于 Docker Compose！
Spring Boot 不会自动加载项目根目录的 .env 文件
```

**2. `application.yml` 中的语法错误**
```yaml
# ❌ 错误的写法（带单引号）
password: ${ASTRNEST_DB_PASSWORD:'ChangeMe_123!'}

# ✅ 正确的写法（不带单引号）
password: ${ASTRNEST_DB_PASSWORD:ChangeMe_123!}
```

**3. 配置优先级问题**
Spring Boot 配置优先级（从高到低）：
```
1. 环境变量（最高优先级）
   ↓
2. application.yml 中的默认值
   ↓
3. 其他配置文件
```

#### 12.1.2 解决方案

**方案 A：使用 PowerShell 环境变量（推荐，临时生效）**
```powershell
# 在启动后端的同一个 PowerShell 窗口中设置
$Env:ASTRNEST_ADMIN_USERNAME="admin"
$Env:ASTRNEST_ADMIN_PASSWORD="ChangeMe_123!"
$Env:ASTRNEST_DB_PASSWORD="ChangeMe_123!"

# 然后启动后端
cd backend
./mvnw spring-boot:run
```
> ⚠️ **重要**：必须在**同一个 PowerShell 窗口**中设置环境变量并启动后端！

**方案 B：使用 Windows 系统环境变量（永久生效）**
1. 控制面板 → 系统 → 高级系统设置 → 环境变量
2. 添加用户变量：
   - `ASTRNEST_ADMIN_USERNAME` = `admin`
   - `ASTRNEST_ADMIN_PASSWORD` = `ChangeMe_123!`
   - `ASTRNEST_DB_PASSWORD` = `ChangeMe_123!`
3. 重启 PowerShell 或 IDE 使环境变量生效

**方案 C：直接修改 application.yml（兜底配置）**
```yaml
astrnest:
  admin:
    username: admin
    password: ChangeMe_123!  # 直接写死，不通过环境变量
```

#### 12.1.3 不同场景的推荐做法

| 场景 | 推荐方案 | 说明 |
|------|---------|------|
| Windows 本地开发 | 方案 A 或 B | 使用环境变量，避免修改代码 |
| Linux/macOS 本地开发 | `export` 环境变量 | 与 Windows 类似 |
| Docker 部署 | `.env` 文件 | Docker Compose 会自动读取 |
| IDE 调试 | 方案 B | 设置系统环境变量后重启 IDE |

#### 12.1.4 验证配置是否生效

启动后端后，查看控制台日志：
```
# 应该能看到类似输出
Admin user initialized: admin
```

如果仍有问题，检查：
1. 数据库中 `users` 表的 `password` 字段
2. 后端启动日志是否有异常
3. 确认环境变量设置和启动后端在同一个终端窗口

### 12.2 常见"能跑但日志报错"的说明（排障速查）
#### 12.2.1 `No converter for ApiErrorResponse with preset Content-Type 'video/mp4'`
如果你在日志里看到类似：
- `HttpMessageNotWritableException: No converter for [class ...ApiErrorResponse] with preset Content-Type 'video/mp4'`

含义：你请求的是视频/资源（响应本来应该是 `video/mp4` 或二进制流），但中途抛异常后框架想返回 JSON 错误体；由于响应头 `Content-Type` 已经是 `video/mp4`，就会出现“无法用 mp4 的 Content-Type 写 JSON”这种报错。

处理建议（按优先级）：
- 先看触发该报错的请求路径：通常是访问视频直链或资源接口（例如 `/upload/**` 或 `/api/uploads/**` 等）。
- 确认资源文件确实存在、存储路径配置正确（本地存储是否挂载到正确目录）。
- 若你是在浏览器/播放器里访问，尽量避免手动强行设置 `Accept: video/mp4` 后再去请求会返回 JSON 的接口。

> 这类报错通常不影响服务启动，但会导致那一次请求返回失败。

#### 12.2.2 `AccessDeniedException: /storage`（本地存储目录无权限导致后端起不来）
现象：后端启动时在日志里出现 `java.nio.file.AccessDeniedException: /storage`，随后 Tomcat 启动失败。原因是默认本地存储根目录是 `/storage/upload`，进程无写权限。

快速修复步骤（裸机/BT 面板场景）：
1) 选择可写目录（示例）：`/www/wwwroot/AstrNest/storage/upload`
2) 赋权并创建目录：
```bash
mkdir -p /www/wwwroot/AstrNest/storage/upload
chown -R www:www /www/wwwroot/AstrNest/storage
chmod 775 /www/wwwroot/AstrNest/storage
```
3) 告诉后端用新目录：
   - 设置环境变量（推荐）：`export ASTRNEST_STORAGE_ROOT=/www/wwwroot/AstrNest/storage/upload`
   - 或在 `backend/src/main/resources/application.yml` 把 `astrnest.storage.local.root` 改为上面的路径后重新打包
4) 重启后端服务，再检查日志确认不再出现 `AccessDeniedException`。

### 13. Nginx 反向代理与静态加速示例
#### 13.1 基本反代（前端 + 后端 + 上传）
```nginx
# 前端 SPA（dist 部署）
server {
    listen 80;
    server_name example.com;
    root /var/www/astrnest/dist;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 上传直链透传后端（若使用本地存储）
    location /upload/ {
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://127.0.0.1:8080/upload/;
        expires 7d;
        add_header Cache-Control "public";
    }

    # 后端 API
    location /api/ {
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_pass http://127.0.0.1:8080/api/;
    }

    # OpenAPI / Swagger
    location /swagger-ui/ {
        proxy_pass http://127.0.0.1:8080/swagger-ui/;
    }
}
```
#### 13.2 HTTPS/HTTP2 与常用优化
```nginx
server {
    listen 443 ssl http2;
    server_name example.com;
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
    add_header Referrer-Policy strict-origin-when-cross-origin;
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()";

    gzip on;
    gzip_types text/plain text/css application/json application/javascript application/xml image/svg+xml;
    gzip_vary on;

    # 其余 location 同 13.1，可复用
}
```

### 14. CDN / 直链域名配置
- 在后台“系统配置”填写 `asset_domain`（如 `https://cdn.example.com`），前端复制直链会自动拼成 `asset_domain + /upload/{yyyy}/{MM}/...`。
- 如使用前端环境变量控制：设置 `VITE_PUBLIC_ASSET_BASE=https://cdn.example.com/upload`。
- Nginx 只需把 `/upload/` 回源到后端或对象存储源站；如走 OSS/COS，请在 CDN 源站指向对应桶域名或自建反代。

### 15. 对象存储配置示例
#### 15.1 阿里云 OSS（application.yml 片段）
```yaml
astrnest:
  storage:
    strategy: ALIYUN_OSS
    oss:
      enabled: true
      endpoint: https://oss-cn-shanghai.aliyuncs.com
      bucket: astrnest-prod
      access-key: ${ALIYUN_ACCESS_KEY}
      secret-key: ${ALIYUN_SECRET_KEY}
      cdn-host: https://cdn.example.com   # 可选
```
> CDN 生效后，前端建议同时设置 `VITE_PUBLIC_ASSET_BASE=https://cdn.example.com/upload`。

#### 15.2 腾讯云 COS
```yaml
astrnest:
  storage:
    strategy: TENCENT_COS
    cos:
      enabled: true
      endpoint: https://cos.ap-shanghai.myqcloud.com
      region: ap-shanghai
      bucket: astrnest-1250000000
      access-key: ${TENCENT_SECRET_ID}
      secret-key: ${TENCENT_SECRET_KEY}
      path-style: true
      accelerate: false
      multipart-threshold-mb: 5120
      part-size-mb: 25
      cdn-host: https://cos-cdn.example.com   # 可选
```

#### 15.3 通用 S3 兼容
```yaml
astrnest:
  storage:
    strategy: S3_COMPATIBLE
    s3:
      enabled: true
      endpoint: https://s3.us-east-1.amazonaws.com
      region: us-east-1
      bucket: astrnest-archive
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
      path-style: true
```
> 若供应商要求 virtual-host 模式，关闭 `path-style` 并确保域名已做 CNAME。

#### 15.4 又拍云 USS
```yaml
astrnest:
  storage:
    strategy: UPYUN_USS
    upyun:
      enabled: true
      bucket: astrnest
      operator: ${UPYUN_OPERATOR}
      password: ${UPYUN_OPERATOR_PASSWORD}
      endpoint: https://v0.api.upyun.com
      cdn-host: https://static.example.com
```

#### 15.5 OneDrive / SharePoint
```yaml
astrnest:
  storage:
    strategy: ONEDRIVE
    onedrive:
      enabled: true
      drive-type: business   # personal / business
      tenant-id: ${AZURE_TENANT_ID}
      client-id: ${AZURE_CLIENT_ID}
      client-secret: ${AZURE_CLIENT_SECRET}
      drive-id: ${ONEDRIVE_DRIVE_ID}
      site-id: ${SHAREPOINT_SITE_ID}
      refresh-token: ${ONEDRIVE_REFRESH_TOKEN}  # 个人盘常用
      base-url: https://graph.microsoft.com/v1.0
```
> `drive-id` 优先级高于 `site-id`；未配置 refresh token 时企业盘可用 client credentials 获取 Token。

### 16. CORS 跨域配置详解

AstrNest 使用 Spring Security CORS 配置处理跨域请求，必须正确配置否则浏览器会阻止前端访问后端 API。

#### 16.1 配置文件位置
```
backend/src/main/java/com/chenxi/astrnest/config/CorsProperties.java
```

#### 16.2 默认配置
```java
private List<String> allowedOrigins = List.of(
    "http://localhost:5173",
    "http://127.0.0.1:5173"
);
```

#### 16.3 常见场景配置

**本地开发（局域网访问）**：
```java
private List<String> allowedOrigins = List.of(
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://192.168.1.100:5173"  // 替换为你的局域网 IP
);
```

**生产环境（HTTPS）**：
```java
private List<String> allowedOrigins = List.of(
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "https://luminouschenxi.net",
    "https://www.luminouschenxi.net"
);
```

**多环境通用（开发便利，不推荐生产）**：
```java
private List<String> allowedOrigins = List.of(
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://192.168.1.100:5173",
    "https://luminouschenxi.net",
    "https://www.luminouschenxi.net",
    "http://0.0.0.0:5173"  // 允许所有 IP（仅开发环境）
);
```

#### 16.4 配置后必须重启后端
修改 `CorsProperties.java` 后，必须重启后端服务才能生效：
```bash
cd backend
./mvnw spring-boot:run
```

#### 16.5 常见错误
- **错误信息**：`Access to XMLHttpRequest at '...' from origin '...' has been blocked by CORS policy`
- **原因**：前端域名不在 `allowedOrigins` 列表中
- **解决**：将前端访问地址添加到 `CorsProperties.java` 的 `allowedOrigins` 列表

⚠️ **安全警告**：生产环境严禁使用 `*` 或 `http://0.0.0.0:5173`，必须显式指定允许的 HTTPS 域名！

### 17. 前端/后端分域部署示例
- **前端域名**：`https://console.example.com`（托管 dist）
- **API 域名**：`https://api.example.com`（反代后端）
- **直链/CDN 域名**：`https://cdn.example.com/upload`
- 前端环境：
```env
VITE_API_BASE_URL=https://api.example.com
VITE_PUBLIC_ASSET_BASE=https://cdn.example.com/upload
VITE_SITE_NAME=AstrNest
```
- 后端系统配置：`asset_domain=https://cdn.example.com`
- Nginx：前端站点只需静态托管；API 与 /upload 由 api 域名回源后端或对象存储。

完成以上补充后，可直接按本文示例配置并上线。
