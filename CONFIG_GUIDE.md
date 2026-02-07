# AstrNest 安装与配置指南

面向首次接手该项目的运维/开发者，本文罗列所有必须手工填充的参数，并指出需要修改的文件路径。按照本文逐项完成后，再启动 Docker Compose 或本地进程即可得到可访问的 `/upload/{yyyy}/{MM}/...` 图片直链与 20MB 上传限制。

## 1. 快速清单

| 项目 | 必填内容 | 修改文件/位置 |
| --- | --- | --- |
| 数据库连接 | 主机、端口、库名、账号、密码 | `.env` / `backend/src/main/resources/application.yml` / `docker-compose.yml` (`mysql`、`backend` 服务环境变量) / `backend/db/init.sql` |
| 管理员账号 | 用户名、初始密码、昵称、邮箱 | `.env` + `backend/db/init.sql` (`INSERT INTO users ...`) |
| 上传路径 | 本地磁盘目录、对外访问前缀 | `.env` (`ASTRNEST_STORAGE_ROOT`/`ASTRNEST_STORAGE_PUBLIC`)，必要时在 Nginx 中反代 `/upload/**` 到后端 |
| 站点域名 | 前端站点地址、图片直链域名、API 域名 | `.env` (`PUBLIC_SITE_URL`/`PUBLIC_ASSET_URL`/`BACKEND_API_PUBLIC_URL`)，后台“系统配置”里的 `asset_domain` |
| 腾讯云 AI 审核 | SecretId、SecretKey、Region、Bucket、检测场景、违规/复核/标签阈值 | 后台「系统配置 > AI 智能审核」，或在 `.env` 中使用 `ASTRNEST_AI_*` 变量作为密钥兜底 |
| JVM 参数 | `JAVA_OPTS`（如 `-Xms512m -Xmx512m`） | `.env` / `docker-compose.yml`（将透传给 Backend 容器） |
| 后端地址 | 前端访问的 API 地址 | `frontend/.env` 或编译期传入 `VITE_API_BASE_URL` |
| 图片访问地址 | 未配置 `asset_domain` 时默认 `https://{当前域名}/upload/{yyyy}/{MM}/文件` | Nginx/网关 `/upload/**` 反向代理到 `backend:8080`，或在 `.env` 中给 `ASTRNEST_ASSET_DOMAIN`/`VITE_PUBLIC_ASSET_BASE` 明确域名 |
| 邮件服务 | SMTP Host/Port/账号/授权码/发件人 | 启动后在后台「设置 > 邮件服务」里填写，或在 `backend/db/init.sql` 的 `chenxi_mail_config` 段改占位值 |

### 1.1 文件路径速查

| 文件路径 | 需确认/修改的参数 | 适用场景 |
| --- | --- | --- |
| `.env`（根目录） | `MYSQL_*`、`ASTRNEST_DB_*`、`ASTRNEST_STORAGE_*`、`ASTRNEST_ADMIN_*`、`PUBLIC_SITE_URL`、`PUBLIC_ASSET_URL`、`BACKEND_API_PUBLIC_URL`、`ASTRNEST_ASSET_DOMAIN`、`VITE_API_BASE_URL`、`VITE_PUBLIC_ASSET_BASE`、`VITE_SITE_NAME`、`SMTP_*`、`ASTRNEST_AI_*` | Docker Compose 及本地 shell 的统一入口，`docker compose --env-file .env up -d` 会读取这些值 |
| `docker-compose.yml` | `services.mysql.environment.*`（数据库名/账号/密码）、`services.backend.environment.*`（后端连接串、管理员、存储路径）、`services.frontend.environment.*`（前端访问后端/图片地址） | 如需为某环境写死参数，可直接在对应服务的 `environment` 块覆盖 `.env` |
| `backend/src/main/resources/application.yml` | `spring.datasource.*`、`astrnest.storage.*`、`astrnest.admin.*` | 裸机运行或 IDE 调试时的兜底配置，所有键均可被环境变量覆盖 |
| `backend/db/init.sql` | `CREATE DATABASE/USER`、`GRANT`、`INSERT INTO users`、`INSERT INTO chenxi_mail_config`、`UPDATE system_config` | 初始化数据库结构、管理员信息与 SMTP 占位；部署前请替换库名/账号、管理员邮箱与密码哈希 |
| `frontend/.env.development` / `.env.production` / `.env.example` | `VITE_API_BASE_URL`、`VITE_PUBLIC_ASSET_BASE`、`VITE_SITE_NAME` | 控制前端向哪台后端发起请求以及生成图片直链的域名/路径 |
| `frontend/src/services/http.js` | `axios.create({ baseURL: ... })` | 若不依赖 `.env`，可在此处直接写死后端请求地址 |

> 📦 **建议做法**：先复制 `.env.example` 为 `.env`，确认每一项都贴合目标环境，再依次调整 `application.yml`、`init.sql`、`frontend/.env.production`。

## 2. 环境变量样例 (.env)

```bash
cp .env.example .env
# 编辑 .env 后生效在 docker compose / shell 中
```

`.env` 同时被 `docker-compose.yml`、`backend/src/main/resources/application.yml` 以及前端构建脚本读取，务必在启动前替换掉所有占位值：
- 先写对数据库主机、端口、库名、账号、密码，保证 `MYSQL_*` 与 `ASTRNEST_DB_*` 指向同一个实例；
- 再设置管理员用户名/密码/邮箱，以便初始化脚本创建正确的超级管理员；
- 根据部署拓扑写入站点域名、图片访问域名、后端公开地址，并在 Nginx 或负载均衡层做好 `/upload/**` 映射；
- 如需发邮件，提前准备好 SMTP 主机、端口与授权码；
- 最后把前端访问后端的 `VITE_API_BASE_URL`、图片直链 `VITE_PUBLIC_ASSET_BASE` 调整为公网域名。

重点变量：

- `MYSQL_ROOT_PASSWORD` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD`：`mysql` 容器初始化所需的库名与账号密码，也方便 DBA 直接照抄到现有 MySQL 主机。
- `ASTRNEST_DB_URL` / `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD`：后端 JDBC 连接串、账号、密码；Docker 模式下可直接写 `jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?...`，裸机部署时改为真实主机。
- `ASTRNEST_STORAGE_ROOT`：容器内/宿主机映射的真实目录，示例 `./storage/upload:/storage/upload`。系统会自动在目录下创建 `YYYY/MM` 子目录。
- `ASTRNEST_STORAGE_PUBLIC`：对外路径前缀，默认为 `/upload`，因此直链为 `https://站点/upload/{yyyy}/{MM}/{文件名}`。
- `ASTRNEST_ASSET_DOMAIN` + `PUBLIC_SITE_URL` / `PUBLIC_ASSET_URL` / `BACKEND_API_PUBLIC_URL`：若要把资产托管到 CDN，请在这里写实际域名，并在 Nginx/Caddy 中增加 `/upload/**` 反代；`PUBLIC_SITE_URL` 用于在邮件等场景拼接站点主页。
- `ASTRNEST_AI_TENCENT_SECRET_ID` / `ASTRNEST_AI_TENCENT_SECRET_KEY` / `ASTRNEST_AI_TENCENT_REGION` / `ASTRNEST_AI_TENCENT_BUCKET` / `ASTRNEST_AI_TENCENT_DETECT_SCENES` / `ASTRNEST_AI_BLOCK_CONFIDENCE` / `ASTRNEST_AI_REVIEW_CONFIDENCE` / `ASTRNEST_AI_LABEL_MIN_CONFIDENCE`：当后台「系统配置」里未填写这些字段时，服务会读取该组环境变量，便于在容器中安全注入密钥与默认阈值。
- `ASTRNEST_MULTIPART_MAX_FILE_SIZE` / `ASTRNEST_MULTIPART_MAX_REQUEST_SIZE`：必须保持 `20MB` 以内，否则会与数据库 `CHECK (size <= 20971520)` 冲突。
- `JAVA_OPTS`：可选，覆写容器内 `java` 启动参数，例如 `-Xms512m -Xmx512m -XX:+UseZGC`，Dockerfile 入口脚本会统一读取该变量。
- `ASTRNEST_ADMIN_*`：首次启动自动落库的管理员信息；密码落库前会以 bcrypt 存储。
- `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_SECURE_TYPE` / `SMTP_FROM_*`：与 init.sql 中的 `chenxi_mail_config` 占位值保持一致，方便后续导入。
- `VITE_API_BASE_URL` / `VITE_PUBLIC_ASSET_BASE` / `VITE_SITE_NAME`：前端构建期注入，用于 axios、复制直链按钮与浏览器标签标题。

> 如需接入腾讯云数据万象内容审核/标签，请在管理后台填写 Secret 及 Bucket，并参照官方文档 <https://cloud.tencent.com/document/product/460/37318>。在自动化环境下，可仅通过 `ASTRNEST_AI_*` 环境变量注入密钥，后端会在系统配置缺值时自动读取。

### 2.1 国内镜像源 404（清华源）处理
- **npm/yarn/pnpm**：若使用清华 npm 源出现 404，请切回官方源再安装依赖：
  - `npm config set registry https://registry.npmjs.org/`
  - 如已安装失败，可执行 `npm cache clean --force` 后重试。
- **Maven**：若本地/CI 配置了清华 Maven 源导致 404，请在 `~/.m2/settings.xml` 暂时移除清华镜像，或改用官方 `https://repo.maven.apache.org/maven2`（或阿里云公共仓 `https://maven.aliyun.com/repository/public`）。
- **Docker 构建**：确保镜像内未强制写死清华源，否则会在封闭网络下拉取失败，可在构建前导出环境变量 `REGISTRY=https://registry.npmjs.org/` 或在 `settings.xml`/`.npmrc` 写入官方源。

## 3. 后端配置 (application.yml)

- 位置：`backend/src/main/resources/application.yml`
- 默认已经引用上述环境变量；若需写死，可以改为纯字符串：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: astrnest
    password: astrnestPass!
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
    password: chenxi123
    display-name: 超级管理员
    email: admin@example.com
```

> **重要**：`public-base-url` 只能使用相对路径（以 `/upload` 开头），否则 Spring 的静态资源映射不起作用；若要使用 CDN 域名，请在后台“系统配置”中填写 `asset_domain`。

## 4. 数据库初始化 (backend/db/init.sql)

- 覆盖内容：建库、账号、所有表结构、视图、索引，以及默认数据。
- 必改项：
  - 脚本顶部 `CREATE DATABASE` 段，若要换库名或账号密码请同步修改。
  - `INSERT INTO users ... VALUES (1, 'admin', '<bcrypt>', ...)`：可替换为自己生成的 bcrypt 密码，或保持默认 `chenxi123`。
  - `INSERT INTO chenxi_mail_config ... VALUES (...)`：写入真实 SMTP 主机/账号/授权码，或保持默认并在后台再修改。
- 新增能力：
  - `upload_records` 自动补齐 `media_uuid`、`file_hash`、`width/height/duration`、`ai_description`、`thumbnail_url`、`ai_decision`、`ai_label_snapshot`、`ai_error_code`/`ai_error_message`/`ai_request_id`、`view/download/report_count`、`metadata JSON` 等字段，并附带 `CHECK (size <= 20971520)`；脚本还会生成 `media` / `media_tags` 视图与 `interactions` 行为表。
  - `tags` + `upload_record_tags` 加入 `type/source/confidence/created_at`，便于区分用户标签与 AI 标签。
  - `system_config` 自带 AI 审核字段（Tencent Secret、Bucket、Region、检测场景、违规/复核/标签阈值、HTML 页脚），初始化脚本会自动补列并写入默认场景 `web,camera,album,news`。

执行方式：
```bash
mysql -u root -p < backend/db/init.sql
```

## 5. 前端环境文件

- 位置：`frontend/.env.development` / `frontend/.env.production`（可自建），亦可在构建命令行注入。
- 推荐内容：

```env
VITE_API_BASE_URL=https://api.luminouschenxi.net
VITE_PUBLIC_ASSET_BASE=https://cdn.luminouschenxi.net/upload
VITE_SITE_NAME=AstrNest
```

## 6. 上传与静态资源

1. **目录结构**：`LocalStorageHandler` 会把每个文件写入 `${ASTRNEST_STORAGE_ROOT}/{yyyy}/{MM}/文件名`。
2. **默认访问**：若 `asset_domain` 为空，后台会返回 `/upload/{yyyy}/{MM}/文件名`，浏览器会基于当前域名访问。
3. **反向代理**：需要在 Nginx/网关层补上一段：

```nginx
location /upload/ {
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8080/upload/;
}
```

4. **CDN/自定义域**：在后台 -> 系统配置填入 `https://cdn.xxx.com`，或在数据库 `system_config.asset_domain` 中直接写；API 会把图片 URL 改为 `https://cdn.xxx.com/upload/{...}`。
5. **视频缩略图 (ffmpeg)**：
   - 后端依赖 ffmpeg 生成视频封面，未安装时会跳过并在日志中提示 `Cannot run program "ffmpeg"`；前端将回退为 `<video>` 首帧占位。
   - 可通过环境变量或配置文件设置：`astrnest.video-thumbnail.ffmpeg-path`（指向可执行文件），`astrnest.video-thumbnail.enabled=true/false`，`astrnest.video-thumbnail.max-load-average=8.0`（系统平均负载高于该值则自动跳过生成，0/负数表示不限制），`astrnest.video-thumbnail.capture-offset`、`quality`、`timeout-seconds`、`cover-directory`。
   - 部署建议：在宿主机安装 ffmpeg 或挂载容器内二进制，并按需调整 `ffmpeg-path` 与 `max-load-average` 以避免高负载导致宕机。若使用第三方存储/CDN，缩略图/视频直链会复用 `asset_domain`/`VITE_PUBLIC_ASSET_BASE` 规则。

### 6.1 ffmpeg 安装方法与注意事项
- **Linux (Debian/Ubuntu)**：`sudo apt-get update && sudo apt-get install -y ffmpeg`
- **Linux (CentOS/RHEL/Alma/Rocky)**：`sudo yum install -y epel-release && sudo yum install -y ffmpeg`（如无官方源可改用 `dnf`）。
- **macOS (Homebrew)**：`brew install ffmpeg`
- **Windows**：从 <https://www.gyan.dev/ffmpeg/builds/> 或 <https://ffmpeg.org/download.html> 下载 release，解压后将 `bin` 目录加入系统 `PATH`。
- **容器内安装**（如需在自建镜像中启用缩略图）：在 Dockerfile 里追加 `RUN apt-get update && apt-get install -y ffmpeg`（或对应发行版的包管理器），并确保二进制路径与 `astrnest.video-thumbnail.ffmpeg-path` 一致。
- **验证**：运行 `ffmpeg -version` 确认可执行。
- **性能与安全提示**：
  - 生成缩略图会占用 CPU/IO，高负载环境请调低 `astrnest.video-thumbnail.max-load-average` 或临时关闭 `astrnest.video-thumbnail.enabled`。
  - 若使用非 root 账号运行，请保证 ffmpeg 所在路径可执行，且存储目录可写。

## 7. 邮件设置

- 启动后登录后台进入「设置 > 邮件服务」，填入 SMTP 主机、端口、授权码、发件人地址。
- 若希望初始化脚本就写入真实值，可在 `backend/db/init.sql` 中的 `INSERT INTO chenxi_mail_config` 把占位字符串替换为生产凭据（脚本默认禁用该配置）。

## 8. 检查“最终收尾”

1. `.env`、`application.yml`、`frontend/.env*` 中是否均已替换为真实地址/账号。
2. `backend/db/init.sql` 中的管理员邮箱、SMTP、`UPDATE upload_records` 均已符合实际。
3. (可选) 使用 `docker compose --env-file .env up -d` 先启动一遍，访问：
   - `http://{server}:8080/actuator/health` → `UP`
   - `http://{server}/upload/{yyyy}/{MM}/demo.png` → 返回 200 或 404（取决于文件是否存在，但不能 502/302 回首页）。

完成上述步骤后，即可按 README 的“快速开始 / Docker 部署”进行启动。祝使用顺利 🛠️。
