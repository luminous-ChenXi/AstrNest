# AstrNest 媒体管理系统

基于 **Spring Boot 3.4.1 + MySQL** 的后端与 **Vue 3 + Vite** 前端构建的现代化图床与内容治理平台，提供完整的媒体上传、存储、管理、分享和API集成解决方案。

<p align="center">
   <a href="https://github.com/vuejs/core">
     <img src="https://img.shields.io/badge/vue-3.5.24-brightgreen.svg" alt="vue">
   </a>
   <a href="https://github.com/element-plus/element-plus">
     <img src="https://img.shields.io/badge/element--plus-2.8.6-brightgreen.svg" alt="element-plus">
   </a>
   <a href="https://spring.io/projects/spring-boot">
     <img src="https://img.shields.io/badge/spring--boot-3.4.1-brightgreen.svg" alt="spring-boot">
   </a>
   <a href="https://github.com/your-username/astrnest/blob/master/LICENSE">
     <img src="https://img.shields.io/github/license/mashape/apistatus.svg" alt="license">
   </a>
   <a href="https://github.com/your-username/astrnest/releases">
     <img src="https://img.shields.io/github/release/your-username/astrnest.svg" alt="GitHub release">
   </a>
 </p>

<img src="Demo/1728x2304.png" width = "300" height = "400" alt="AstrNest" align=right />
<div align="center">

# AstrNest

_Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3._

 唯有青春与梦想不可辜负~！

</div>

---


## 🚀 核心特性

- **现代化架构**: Spring Boot 3.4.1 + Vue 3 + Vite 5 全栈技术栈
- **多角色权限**: 支持管理员、普通用户等多级权限管理体系
- **多云存储**: 内置 Local、阿里云 OSS、腾讯 COS、七牛 Kodo、华为 OBS、金山 KS3、又拍云 USS、OneDrive/SharePoint 以及通用 S3 驱动，可在配置中一键切换；S3 兼容驱动默认 5GB 阈值触发 25MB 分片上传，支持 CDN/CNAME、加速域、PathStyle 以及预签名直传凭证
- **智能内容审查**: 接入腾讯云数据万象（COS CI）图片审核与标签服务，自动回填 AI 决策/标签/RequestId，并结合错误码文档输出友好提示，配合人工复核双重保障
- **完整的API生态**: RESTful API + API密钥认证 + Web管理界面
- **灵活的存储方案**: 支持本地存储与云对象存储（OSS/COS）
- **企业级安全**: Spring Security 6 + JWT认证 + 内容安全策略
- **成员治理与配额**: 管理端“成员列表”支持查看头像/配额/点赞总数，并可一键调整每日上传与总存储额度、切换管理员/用户/游客角色
- **访客互动能力**: 公共图库可配置“访客点赞”，支持访客 token（`X-Chenxi-Visitor`）与最近点赞展示，也可随时禁用
- **响应式设计**: Tailwind CSS + Element Plus 现代化UI组件
- **实时监控**: 系统运行状态监控与操作日志审计
- **邮件服务**: 集成邮件模板与验证码发送功能，默认预置阿里云邮局 SMTP

## 📦 技术栈

### 后端
- **框架**: Spring Boot 3.4.1
- **语言**: Java 21
- **数据库**: MySQL 5.7+/8.0
- **安全**: Spring Security 6
- **文档**: SpringDoc OpenAPI 3
- **AI 审核 SDK**: Tencent Cloud COS CI (`com.qcloud:cos_api`)
- **构建**: Maven Wrapper

### 前端
- **框架**: Vue 3.5.24
- **构建**: Vite 5.4.10
- **路由**: Vue Router 4
- **状态管理**: Pinia 3
- **UI组件**: Element Plus 2.8.6
- **样式**: Tailwind CSS 3
- **图标**: Lucide Vue, Element Plus Icons

## 📁 项目结构

```
astrnest/
├─ backend/      # Spring Boot 服务：REST API、鉴权、内容审查、API 密钥管理
├─ frontend/     # Vue 3 + Vite 单页应用：仪表盘、上传中心、安全控制台、API 集成
└─ storage/      # （运行时生成）本地存储目录，可通过配置改为 OSS/COS
```

## 🔌 API 接口文档概览

### Swagger / OpenAPI
- 在线文档入口：`/swagger-ui/index.html`
- OpenAPI JSON：`/v3/api-docs`
- 认证方式：登录后获取的 Token 放入 `Authorization: Bearer <token>`，部分接口需管理员权限。

## ⚡ 快速开始

### 环境要求
- **JDK 21**（推荐使用 OpenJDK 21 / Temurin）
- **Node.js 18+** 与 npm 9+
  - 若 npm 使用清华源出现 404，请先执行 `npm config set registry https://registry.npmjs.org/`，必要时 `npm cache clean --force` 再安装。
- **MySQL 8.0**，并已创建 `astrnest` 数据库以及有权限的账号
- **FFmpeg 4.4+**（用于本地生成视频首帧缩略图，可通过 `astrnest.video-thumbnail.ffmpeg-path` 指定执行文件）
- 可选：`curl`、`pm2`、Nginx 等部署辅助工具

### 一键启动（开发环境）

1. **克隆项目**
```bash
git clone <repository-url>
cd AstrNest
```

2. **数据库初始化**
```bash
mysql -u root -p < backend/db/init.sql
```
该脚本会自动：
- 创建 `astrnest` 数据库与 `astrnest/astrnestPass!` 专用账号，并授予库级权限；
- 创建/校验 `users`、`roles`、`user_roles`、`api_keys`、`upload_records`、`upload_likes`、`system_config`、`content_policy`、`user_login_events`、`interactions` 等核心表，并落盘 `media`、`media_tags` 视图方便 BI；
- 对旧库补齐字段/索引/约束，新增用户画像字段（角色、状态、双因素、JSON 设置、配额/用量）、媒体元数据字段（UUID、hash、类型、尺寸、AI 描述、统计、JSON metadata、20MB CHECK 约束）、标签来源与置信度等；
- 预置空 SMTP（`smtp.example.com` + `no-reply@example.com`，默认禁用）与系统管理员邮箱占位，避免泄露真实凭据；
- 插入默认角色、超级管理员账号（`admin` / `chenxi123`）以及系统配置/内容策略（含 AI 审核开关、腾讯云密钥、检测场景与阈值的默认值）。
> 可以放心重复执行，脚本采用 `IF NOT EXISTS` / `ON DUPLICATE KEY` / `INFORMATION_SCHEMA` 检测，不会破坏既有数据。

3. **启动后端服务**
```bash
cd backend
./mvnw spring-boot:run
```

4. **启动前端服务**（新终端）
```bash
cd frontend
npm install
npm run dev
```

5. **访问应用**
- 前端地址：http://localhost:5173
- 后端地址：http://localhost:8080
- API文档：http://localhost:8080/swagger-ui.html

### 默认管理员账号
- 用户名：`admin`
- 密码：`chenxi123`

## 🧩 安装配置速查

| 步骤 | 必填内容 | 触达文件 |
| --- | --- | --- |
| 1. 填写 `.env` | `MYSQL_*`（数据库主机/端口/库名/账号/密码）、`ASTRNEST_DB_*`（后端 JDBC 地址与账号）、`ASTRNEST_ADMIN_*`（管理员用户名/密码/邮箱）、`ASTRNEST_STORAGE_*`（上传根路径与 `/upload` 前缀）、`PUBLIC_SITE_URL`/`PUBLIC_ASSET_URL`/`BACKEND_API_PUBLIC_URL`、`ASTRNEST_ASSET_DOMAIN`、`VITE_API_BASE_URL`/`VITE_PUBLIC_ASSET_BASE`、`SMTP_*` | 根目录 `.env`（`docker-compose.yml`、Spring Boot、Vite 共用） |
| 2. 审核 `docker-compose.yml` | `services.mysql.environment.*`（库名/账号/密码）、`services.backend.environment.*`（数据库 URL、管理员信息、存储路径、域名变量）、`services.frontend.environment.*`（`VITE_API_BASE_URL` / `VITE_PUBLIC_ASSET_BASE`） | `docker-compose.yml`（已内置 `env_file: .env`，也可直接在 `environment` 中覆盖） |
| 3. 本地/IDE 兜底配置 | `spring.datasource.*`、`astrnest.storage.*`、`astrnest.admin.*` | `backend/src/main/resources/application.yml` |
| 4. 配置 FFmpeg 缩略图 | `astrnest.video-thumbnail.*`（或 `ASTRNEST_VIDEO_THUMBNAIL_*` 环境变量：开关、FFmpeg 路径、截图时间、质量、输出目录） | `backend/src/main/resources/application.yml` |
| 5. 初始化脚本 | `CREATE DATABASE/USER`、`GRANT`、`INSERT INTO users`（管理员）、`INSERT INTO chenxi_mail_config`（SMTP）、`UPDATE system_config.asset_domain` | `backend/db/init.sql` |
| 6. 前端环境 | `VITE_API_BASE_URL`（前端访问后端）、`VITE_PUBLIC_ASSET_BASE`（复制直链）、`VITE_SITE_NAME` | `frontend/.env.development` / `.env.production` / `.env.example` |
| 7. 详细指引 | 全量参数描述、直链代理示例、检查清单 | [CONFIG_GUIDE.md](CONFIG_GUIDE.md) |

> ✅ 建议顺序：复制 `.env.example` → 填写数据库/管理员/域名/SMTP → 运行 `mysql -u root -p < backend/db/init.sql` → `docker compose up -d`。任何参数更新都请同步 `.env` 与 `backend/db/init.sql`，避免管理员或 SMTP 值不一致。

## 🎯 核心功能模块

### 1. 用户管理
- **用户注册与登录**：支持用户名密码登录和辰汐认证系统
- **个人资料管理**：用户可以查看和修改个人信息
- **密码安全**：支持密码修改和安全设置
- **登录历史**：记录用户登录IP和时间信息
- **成员列表**：管理后台提供暗色玻璃风“directory”视图，展示头像、邮箱、签名、占用存储、点赞总数，可实时调整每日上传与空间配额并切换角色/删除用户

### 2. 图片上传与存储
- **多格式支持**：支持常见图片格式（JPG、PNG、GIF、WEBP等）
- **批量上传**：支持多文件同时上传
- **存储策略**：可配置本地存储或云对象存储
- **上传限制**：支持文件大小、类型、数量限制

### 3. 内容管理
- **媒体管理**：用户可查看、删除、修改可见性
- **批量操作**：支持批量删除图片
- **点赞功能**：用户可对图片进行点赞
- **可见性控制**：支持公开/私有图片设置

### 4. 安全管理
- **权限控制**：基于角色的权限管理系统
- **内容审查**：自动检测和人工审核违规内容
- **域名白名单**：控制图片引用域名
- **API密钥管理**：支持API调用认证和配额管理

### 5. 系统管理
- **用户管理**：管理员可管理用户角色和限制
- **系统配置**：动态配置系统参数（含腾讯云 AI 智能审核、标签阈值、页脚、清理策略等）
- **监控统计**：实时监控系统运行状态
- **操作日志**：记录所有关键操作

### 6. 邮件服务
- **邮件模板**：支持自定义邮件模板
- **验证码发送**：集成邮件验证码功能
- **配置管理**：灵活的邮件服务器配置


### 7. 公共图库
- **公共展示**：支持公开图片的展示和浏览，可跳转上传者或最近点赞者详情页
- **点赞互动**：会员/访客皆可点赞，访客需随请求附带 `X-Chenxi-Visitor` 指纹头；管理员可在系统配置中关闭“访客点赞”
- **实时反馈**：弹窗展示最近点赞（含访客提示），便于快速治理
- **分页浏览**：支持大量图片的分页展示

## 基础配置
### 后端配置 (`backend/src/main/resources/application.yml`)
| 配置项 | 说明 |
| --- | --- |
| `spring.datasource.*` | MySQL 数据库连接配置，默认使用 `jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai` |
| `astrnest.storage.local.*` | 本地存储根目录、对外路径（默认 `/storage/upload` -> `/upload`）；若切换 OSS/COS 可在 `astrnest.storage.*` 下配置 |
| `astrnest.admin.*` | 首次启动自动创建的超级管理员信息（用户名/密码/展示名/邮箱） |
| `astrnest.api-key.*` | API 密钥请求头名称、默认日调用配额 |

可通过环境变量覆盖，如：
```bash
export ASTRNEST_DB_URL="jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
export ASTRNEST_ADMIN_PASSWORD="StrongerPass123!"
```

### 存储策略切换
```yaml
astrnest:
  storage:
    strategy: TENCENT_COS  # LOCAL / ALIYUN_OSS / TENCENT_COS / QINIU_KODO / UPYUN_USS / HUAWEI_OBS / KS3 / S3_COMPATIBLE / ONEDRIVE
    local:
      root: ../storage/upload
      public-base-url: /upload
    oss:
      enabled: true
      endpoint: https://oss-cn-shanghai.aliyuncs.com
      bucket: astrnest-prod
      access-key: ${ALIYUN_ACCESS_KEY}
      secret-key: ${ALIYUN_SECRET_KEY}
      cdn-host: https://cdn.luminouschenxi.net
    cos:
      enabled: true
      endpoint: https://cos.ap-shanghai.myqcloud.com
      region: ap-shanghai
      bucket: astrnest-1250000000
      access-key: ${TENCENT_SECRET_ID}
      secret-key: ${TENCENT_SECRET_KEY}
      path-style: true
      accelerate: false
      multipart-threshold-mb: 5120   # ≥5GB 触发分片
      part-size-mb: 25
      cdn-host: https://cos-cdn.luminouschenxi.net
    upyun:
      enabled: false
      bucket: astrnest
      operator: ${UPYUN_OPERATOR}
      password: ${UPYUN_OPERATOR_PASSWORD}
      endpoint: https://v0.api.upyun.com
      cdn-host: https://static.luminouschenxi.net
    s3:
      enabled: false
      endpoint: https://s3.us-east-1.amazonaws.com
      region: us-east-1
      bucket: astrnest-archive
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
    onedrive:
      enabled: false
      drive-type: business   # personal / business
      tenant-id: ${AZURE_TENANT_ID}
      client-id: ${AZURE_CLIENT_ID}
      client-secret: ${AZURE_CLIENT_SECRET}
      drive-id: ${ONEDRIVE_DRIVE_ID}
      site-id: ${SHAREPOINT_SITE_ID}
      refresh-token: ${ONEDRIVE_REFRESH_TOKEN}
      redirect-uri: https://login.microsoftonline.com/common/oauth2/nativeclient
      base-url: https://graph.microsoft.com/v1.0
```

> 本地策略默认写入 `/storage/upload/{yyyy}/{MM}/文件`，Spring 会把同路径映射到 `/upload/{yyyy}/{MM}/...`，若 `system_config.asset_domain` 留空则等价于“当前域名 + /upload/**”。
> 七牛 Kodo / 华为 OBS / 金山 KS3 依旧沿用 S3 配置；又拍云 USS 现在直接读取 `upyun` 节点中的 bucket/operator/password 并自动生成 REST 签名，建议同时配置 CDN 域名；若需使用 OneDrive / SharePoint，请在 `onedrive` 节点填好 driveId（或 siteId）及 Azure AD 凭证，`drive-type` 为 `personal` 时可提供 refresh token，`business` 则可仅凭 client credentials 获取 Token。
>
> - 又拍云：`operator` 为控制台中的操作员，`password` 为该操作员密码，`endpoint` 支持 v0/v1/v2/v3 官方接入点，自带自动建目录与 CDN 回源。
> - OneDrive：`drive-id` 优先于 `site-id`，若只配置 SharePoint 站点会自动解析；`refresh-token` 适用于个人盘，企业盘可直接使用租户 + client credentials；可按需调整 `redirect-uri` 以匹配 Azure 应用设置。

### 前端配置 (`frontend`)
- 通过环境变量 `VITE_API_BASE_URL` 指定后端地址，默认 `http://localhost:8080`。
- 通过 `VITE_PUBLIC_ASSET_BASE` 指出图片直链（例如 `https://cdn.luminouschenxi.net/upload`），未设置时自动跟随 `VITE_API_BASE_URL`。
- 运行 `npm run dev` 时可临时传入：
  ```bash
  VITE_API_BASE_URL="http://localhost:8080" npm run dev
  ```

### 🎬 视频缩略图（FFmpeg）
- 新增 `astrnest.video-thumbnail.*` 配置项（或 `ASTRNEST_VIDEO_THUMBNAIL_*` 环境变量）用来控制缩略图生成，涵盖 `enabled`、`ffmpeg-path`、`capture-offset`（ISO8601 Duration，如 `PT1S` → 1 秒）、`quality`、`timeout-seconds`、`cover-directory`（默认为 `cover`，最终输出路径类似 `storage/upload/cover/2025/12/video_cover.jpg`）。
- 后端会在本地执行 `ffmpeg -ss <capture-offset> -i <video> -vframes 1 -q:v <quality>` 提取首帧，成功后将图片写入上述目录并回填 `upload_records.thumbnail_url` 与 `thumbnail_storage_path`，前端直接读取 `thumbnailUrl` 作为 `<video>` 的 `poster`。
- 请确保服务器已安装 FFmpeg（Ubuntu 可执行 `sudo apt install ffmpeg`），或将 `ffmpeg` 可执行文件路径写入 `ASTRNEST_VIDEO_THUMBNAIL_FFMPEG`。
- 若生成失败，系统会自动降级为使用视频直链，必要时可在 `storage/upload/cover` 中预置占位图。

## 后端：构建与启动
| 场景 | 命令 |
| --- | --- |
| 安装依赖 | 无需手动安装 Maven，直接使用项目自带 `./mvnw` |
| 本地开发启动 | `cd backend && ./mvnw spring-boot:run`
| 单元测试 | `./mvnw test`
| 生产构建 | `./mvnw clean package`（产物 `target/backend-0.0.1-SNAPSHOT.jar`） |
| 生产运行 | `java -jar target/backend-0.0.1-SNAPSHOT.jar` |

> 注意：首次启动会根据 JPA 实体自动建表。若使用自签名证书或远程 MySQL，记得在 JDBC URL 中追加 `allowPublicKeyRetrieval=true`、`useSSL=false/true` 等参数。

## 前端：构建与启动
| 场景 | 命令 |
| --- | --- |
| 安装依赖 | `cd frontend && npm install`
| 开发模式 | `npm run dev`（默认监听 `http://localhost:5173`） |
| 生产构建 | `npm run build`（生成 `dist/` 静态资源） |
| 构建预览 | `npm run preview` |

打包后的 `dist/` 可：
1. 直接由 Vite 预览或任何静态服务器（Nginx、Caddy、OSS）托管；
2. 复制至后端的静态目录或由 Nginx 反向代理到后端 API。

## 推荐启动流程
1. **准备数据库**：在 MySQL 中创建数据库与账号，并授权：
   ```sql
   CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4;
   CREATE USER 'astrnest'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'localhost';
   FLUSH PRIVILEGES;
   ```
2. **配置后端**：在 `application.yml` 或环境变量中填入数据库连接与管理员信息。
3. **启动后端**：`cd backend && ./mvnw spring-boot:run`，确认 `http://localhost:8080/actuator/health` 为 `UP`。
4. **启动前端**：
   ```bash
   cd frontend
   npm install
   VITE_API_BASE_URL="http://localhost:8080" npm run dev
   ```
5. **访问前端**：浏览器打开 `http://localhost:5173`，即可体验仪表盘、上传中心、权限与 API 集成页面。

## 数据扩展字段与初始化脚本
- `backend/db/init.sql` 现已整合「建库/建表」「列补齐」「默认数据」三步，可直接在全新服务器执行，同样支持在旧库上重复运行补齐字段。
- 自动创建的核心表：`users`、`roles`、`user_roles`、`api_keys`、`upload_records`、`upload_likes`、`system_config`、`content_policy`、`user_login_events`。
- 自动补齐 / 新增的字段：
  - `users`：`nickname`（用户昵称）、`login_ip_history`（最近十次登录 IP 快照）、`last_login_ip`、`last_login_at` 等审计字段。
  - `upload_records`：补齐 `storage_path/image_link/file_name` 基础字段，并新增 `media_uuid`、`file_hash`、`media_type`、`width/height/duration`、`title/description/ai_description`、`thumbnail_url`、`is_sensitive`、`review_score/review_notes`、`view_count/download_count/report_count`、`metadata JSON`、`version` 等，配套 `20MB` CHECK 约束与全文索引；同步生成 `media` 视图与 `media_tags` 视图，对外即 `media` 设计稿。
- 额外建表：`upload_likes`（访客点赞）、`interactions`（like/favorite/view/download）等行为流水。

### 核心 Schema 对齐
- **users**：包含 `username/email/password_hash`、`nickname/avatar_url/bio`、`role/status`、`storage_quota_bytes/used_storage`、`last_login_ip/last_login_at`、`email_verified/two_factor_enabled`、`settings JSON` 等字段，并创建 `idx_email`、`idx_status` 索引，满足 1GB 默认配额/双因素等需求。
- **upload_records + media 视图**：底层表继续服务后端代码，额外提供 `media_uuid (CHAR(36))`、`file_hash`、`media_type ENUM`、`width/height/duration`、`ai_description`、`storage_provider/path`、`access_url/thumbnail_url`、`review_status/review_score/notes`、`ai_decision/ai_label_snapshot/ai_error_code/ai_error_message/ai_request_id`、`is_public/is_sensitive`、`like/view/download/report_count`、`metadata JSON`、`version` 等字段，并加上 `CHECK (size <= 20971520)`；`media` 视图将其映射为文档中描述的 `media` 表结构，便于按 UUID 查询。
- **tags**：具备 `name/slug/type/media_count` 字段与 `idx_name`、`idx_type` 索引，用于区分用户/系统/AI 标签。
- **upload_record_tags + media_tags 视图**：关联表含 `source`（user/system/ai）、`confidence`、`created_at`，并在视图中暴露为 `media_tags`，与期望的多对多结构一致（级联删除）。
- **system_config**：补齐 `custom_footer_html` 以及腾讯云 AI 审核相关列（启用开关、SecretId/SecretKey、Region、Bucket、检测场景、违规/复核/标签阈值），运行脚本即可自动创建并填入默认场景。
- **interactions**：记录 `user_id`、`media_id`、`type`（like/favorite/view/download）、`client_ip`、`user_agent`、`created_at`，并通过 `UNIQUE idx_user_media_type` 防重复，符合行为流水设计。
- **api_keys**：具备 `public_id`、`secret_hash`、`masked_key`、`prefix`、`scopes JSON`、`daily_limit`、`used_today`、`status`、`expires_at` 等列，并设置 `idx_prefix`、`idx_status_expires` 支撑密钥识别与配额管理。

> 若已存在旧库，可先备份再执行该脚本。脚本内部通过 `INFORMATION_SCHEMA` 检测列是否存在，因此重复执行也不会报错。

## 🚀 生产部署

### Docker 部署（推荐）

1. **复制并填写环境变量**：把根目录 `.env.example` 复制为 `.env`，逐项填入数据库、管理员、邮箱、域名、存储路径等；同样可在裸机部署时 `source .env`。
2. **直接使用 Docker Compose**
```bash
docker compose --env-file .env up -d
```

> `docker-compose.yml` 已声明 `env_file: .env`，因此在项目根目录执行 `docker compose up -d` 也会自动载入同名文件。

该命令会自动：
- 构建后端和前端镜像
- 启动 MySQL 8.0 数据库并加载 `backend/db/init.sql`
- 挂载 `./storage/upload` 到容器 `/storage/upload`，对外暴露 `/upload/{yyyy}/{MM}/...`
- 启动后端服务（端口 8080）
- 启动前端服务（端口 80）
> 如需调整 JVM 内存或添加调试参数，可在 `.env` 中设置 `JAVA_OPTS`，`docker-compose` 会将其传入容器并由 Dockerfile 的 `ENTRYPOINT` 统一执行。

2. **docker-compose.yml 详情**
```yaml
# 先执行 cp .env.example .env 并填写真实的数据库/域名/SMTP/管理员参数
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: astrnest-mysql
    env_file:
      - ./.env
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-astrnestRoot!}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-astrnest}
      MYSQL_USER: ${MYSQL_USER:-astrnest}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-astrnestPass!}
    command: ["mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_general_ci"]
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "3306:3306"
    networks:
      - astrnest-network
    restart: unless-stopped

  backend:
    build: ./backend
    container_name: astrnest-backend
    env_file:
      - ./.env
    depends_on:
      - mysql
    environment:
      ASTRNEST_DB_URL: ${ASTRNEST_DB_URL:-jdbc:mysql://mysql:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai}
      ASTRNEST_DB_USERNAME: ${ASTRNEST_DB_USERNAME:-astrnest}
      ASTRNEST_DB_PASSWORD: ${ASTRNEST_DB_PASSWORD:-astrnestPass!}
      ASTRNEST_STORAGE_ROOT: ${ASTRNEST_STORAGE_ROOT:-/storage/upload}
      ASTRNEST_STORAGE_PUBLIC: ${ASTRNEST_STORAGE_PUBLIC:-/upload}
      ASTRNEST_ASSET_DOMAIN: ${ASTRNEST_ASSET_DOMAIN:-}
      PUBLIC_SITE_URL: ${PUBLIC_SITE_URL:-https://luminouschenxi.net}
      PUBLIC_ASSET_URL: ${PUBLIC_ASSET_URL:-https://luminouschenxi.net/upload}
      BACKEND_API_PUBLIC_URL: ${BACKEND_API_PUBLIC_URL:-https://api.luminouschenxi.net}
      ASTRNEST_STORAGE_STRATEGY: ${ASTRNEST_STORAGE_STRATEGY:-LOCAL}
      ASTRNEST_ADMIN_USERNAME: ${ASTRNEST_ADMIN_USERNAME:-admin}
      ASTRNEST_ADMIN_PASSWORD: ${ASTRNEST_ADMIN_PASSWORD:-chenxi123}
      ASTRNEST_ADMIN_DISPLAY: ${ASTRNEST_ADMIN_DISPLAY:-超级管理员}
      ASTRNEST_ADMIN_EMAIL: ${ASTRNEST_ADMIN_EMAIL:-admin@example.com}
      ASTRNEST_API_KEY_HEADER: ${ASTRNEST_API_KEY_HEADER:-X-API-Key}
      ASTRNEST_API_DEFAULT_QUOTA: ${ASTRNEST_API_DEFAULT_QUOTA:-1000}
      ASTRNEST_MULTIPART_MAX_FILE_SIZE: ${ASTRNEST_MULTIPART_MAX_FILE_SIZE:-20MB}
      ASTRNEST_MULTIPART_MAX_REQUEST_SIZE: ${ASTRNEST_MULTIPART_MAX_REQUEST_SIZE:-20MB}
      ASTRNEST_AI_TENCENT_SECRET_ID: ${ASTRNEST_AI_TENCENT_SECRET_ID:-}
      ASTRNEST_AI_TENCENT_SECRET_KEY: ${ASTRNEST_AI_TENCENT_SECRET_KEY:-}
      ASTRNEST_AI_TENCENT_REGION: ${ASTRNEST_AI_TENCENT_REGION:-ap-beijing}
      ASTRNEST_AI_TENCENT_BUCKET: ${ASTRNEST_AI_TENCENT_BUCKET:-}
      ASTRNEST_AI_TENCENT_DETECT_SCENES: ${ASTRNEST_AI_TENCENT_DETECT_SCENES:-web,camera,album,news}
      ASTRNEST_AI_BLOCK_CONFIDENCE: ${ASTRNEST_AI_BLOCK_CONFIDENCE:-90}
      ASTRNEST_AI_REVIEW_CONFIDENCE: ${ASTRNEST_AI_REVIEW_CONFIDENCE:-60}
      ASTRNEST_AI_LABEL_MIN_CONFIDENCE: ${ASTRNEST_AI_LABEL_MIN_CONFIDENCE:-60}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
    ports:
      - "8080:8080"
    volumes:
      - ./storage/upload:${ASTRNEST_STORAGE_ROOT:-/storage/upload}
    networks:
      - astrnest-network
    restart: unless-stopped

  frontend:
    build: ./frontend
    container_name: astrnest-frontend
    env_file:
      - ./.env
    depends_on:
      - backend
    environment:
      VITE_API_BASE_URL: ${VITE_API_BASE_URL:-http://localhost:8080}
      VITE_PUBLIC_ASSET_BASE: ${VITE_PUBLIC_ASSET_BASE:-http://localhost:8080/upload}
      VITE_SITE_NAME: ${VITE_SITE_NAME:-AstrNest}
    ports:
      - "80:80"
    networks:
      - astrnest-network
    restart: unless-stopped

volumes:
  mysql_data:

networks:
  astrnest-network:
    driver: bridge
```

### 传统部署

1. **后端部署**
```bash
cd backend
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

2. **前端部署**
```bash
cd frontend
npm run build
# 将 dist/ 目录部署到 Nginx 或 CDN
```

## 🔧 API 使用示例

### 用户认证
```bash
# 用户登录
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"chenxi123"}'

# 获取用户概览（需要认证）
curl -X GET "http://localhost:8080/api/user/overview" \
  -H "Authorization: Bearer <token>"
```

### 图片上传（使用API密钥）
```bash
# 上传图片
curl -X POST "http://localhost:8080/api/uploads" \
  -H "X-API-Key: <your-api-key>" \
  -F "file=@/path/to/image.jpg"
```

### 获取图片信息
```bash
# 获取用户上传列表
curl -X GET "http://localhost:8080/api/user/uploads?page=0&size=10" \
  -H "Authorization: Bearer <token>"

# 获取公共图库
curl -X GET "http://localhost:8080/api/gallery/public"
```

### 公共图库访客点赞
```bash
# 访客点赞需在前端生成/复用 visitor token，并通过 X-Chenxi-Visitor 头传递
curl -X POST "http://localhost:8080/api/gallery/3/like" \
  -H "X-Chenxi-Visitor: 7d6f4b1e87904f6a8f7b3f4b2d90f7a1" \
  -H "User-Agent: AstrNestPublicGallery/1.0" \
  -H "Content-Type: application/json"
```
若管理员关闭“访客点赞”，此接口会返回 403，提示需登录后再操作。

### 管理操作
```bash
# 获取系统监控信息
curl -X GET "http://localhost:8080/api/monitor/overview" \
  -H "Authorization: Bearer <admin-token>"

# 获取API密钥列表
curl -X GET "http://localhost:8080/api/keys" \
  -H "Authorization: Bearer <admin-token>"
```

## 🔧 配置说明

> 📘 建议先阅读新增的 [CONFIG_GUIDE.md](CONFIG_GUIDE.md)，按步骤拷贝/修改根目录 `.env.example`、`backend/src/main/resources/application.yml`、`backend/db/init.sql`、`frontend/.env` 等文件。

### 环境变量配置

#### 后端环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `ASTRNEST_DB_URL` | 数据库连接URL | `jdbc:mysql://localhost:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai` |
| `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD` | 数据库账号/密码 | `astrnest` / `astrnestPass!` |
| `ASTRNEST_STORAGE_ROOT` | 本地存储根目录（容器内路径） | `/storage/upload` |
| `ASTRNEST_STORAGE_PUBLIC` | 上传对外基础路径 | `/upload` |
| `ASTRNEST_STORAGE_STRATEGY` | 存储策略 | `LOCAL` |
| `ASTRNEST_ADMIN_USERNAME` | 超级管理员用户名 | `admin` |
| `ASTRNEST_ADMIN_PASSWORD` | 超级管理员密码 | `chenxi123` |
| `ASTRNEST_ADMIN_DISPLAY` | 超级管理员显示名 | `超级管理员` |
| `ASTRNEST_ADMIN_EMAIL` | 超级管理员邮箱 | `admin@example.com` |
| `ASTRNEST_MULTIPART_MAX_FILE_SIZE` / `ASTRNEST_MULTIPART_MAX_REQUEST_SIZE` | 单文件/单请求大小上限 | `20MB` |
| `ASTRNEST_API_KEY_HEADER` | API密钥请求头名称 | `X-API-Key` |
| `ASTRNEST_API_DEFAULT_QUOTA` | API默认日调用配额 | `1000` |
| `ASTRNEST_ASSET_DOMAIN` | （可选）强制拼接的静态域名 | 空字符串，表示沿用 `/upload/**` |
| `ASTRNEST_AI_TENCENT_SECRET_ID` / `ASTRNEST_AI_TENCENT_SECRET_KEY` / `ASTRNEST_AI_TENCENT_REGION` / `ASTRNEST_AI_TENCENT_BUCKET` / `ASTRNEST_AI_TENCENT_DETECT_SCENES` / `ASTRNEST_AI_BLOCK_CONFIDENCE` / `ASTRNEST_AI_REVIEW_CONFIDENCE` / `ASTRNEST_AI_LABEL_MIN_CONFIDENCE` | 在系统配置未填写密钥/阈值时兜底读取，方便在容器或 CI/CD 中安全注入腾讯云 AI 审核参数 | 空字符串或默认阈值（90/60/60） |

#### 前端环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `VITE_API_BASE_URL` | 后端API基础URL | `http://localhost:8080` |
| `VITE_PUBLIC_ASSET_BASE` | 图片直链基准（用于拼接复制按钮） | `http://localhost:8080/upload` |
| `VITE_SITE_NAME` | 前端标题/展示名称 | `AstrNest` |

### AI 审核配置
- **配置入口**：管理后台 → 系统配置 → AI 智能审核。可在此开启/关闭腾讯云违规检测与标签入库，设置 SecretId/SecretKey、Region、Bucket、检测场景以及拦截/复核/标签置信度阈值。
- **环境变量兜底**：若不希望在数据库中保存密钥，可在 `.env` 中填写 `ASTRNEST_AI_*` 变量，后端会在配置缺省时读取这些值。
- **官方参考**：腾讯云数据万象图片审核与标签接口详见 [文档](https://cloud.tencent.com/document/product/460/37318)，需保证 `asset_domain` 或 `ASTRNEST_ASSET_DOMAIN` 指向腾讯云可访问的公网地址。

### 存储配置

支持本地存储和对象存储（OSS/COS），可通过 `backend/src/main/resources/application.yml` 中的 `astrnest.storage` 配置进行切换。

- **本地存储**：默认启用，落盘到 `astrnest.storage.local.root`（默认 `/storage/upload`）
- **对象存储**：在 `astrnest.storage` 下分别配置 `oss` / `cos` / `s3` / `upyun` / `onedrive` 节点即可切换

## 🤝 贡献指南

欢迎贡献代码！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细流程。

## 🐛 问题报告

如遇问题，请：
1. 查看 [常见问题](#常见问题)
2. 搜索 [GitHub Issues](https://github.com/your-repo/astrnest/issues)
3. 创建新的 Issue

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

## 🔒 安全

安全相关问题请查看 [SECURITY.md](SECURITY.md)。

---

## 常见问题

| 问题 | 处理建议 |
| --- | --- |
| CORS 报错 | 确保后端 `SecurityConfig` 中的 CORS 白名单包含当前前端域名，或在部署入口层（Nginx）补充 `Access-Control-*` 头。 |
| 数据库认证失败 | 检查 `spring.datasource.username/password` 与 MySQL 用户授权是否一致。 |
| API 上传 401 | `POST /api/uploads` 需要登录管理员或携带有效 `X-API-Key`。在安全中心/API 页面创建密钥。 |
| 静态资源访问不到 | 若切换对象存储，记得同步配置 `astrnest.storage.local.public-base-url` 或在后台设置资产域名，并确保 CDN/桶权限正确。 |
| 邮件发送失败 | 检查邮件配置是否正确，包括SMTP服务器、端口、用户名和密码 |
| 验证码验证失败 | 确保验证码在有效期内，且输入正确 |

## 📞 联系方式

- **问题反馈**: 通过GitHub Issues提交
- **安全报告**: chenxi@luminouschenxi.net

---

## 📊 系统架构

AstrNest 采用前后端分离架构：

- **前端**: Vue 3 + Vite + Element Plus + Tailwind CSS
- **后端**: Spring Boot 3 + Spring Security + JPA + MySQL
- **认证**: JWT Token + API Key 双重认证机制
- **存储**: 本地存储 + 云对象存储扩展支持
- **安全**: 基于角色的权限控制 + 内容安全策略

## 🔄 开发路线图

- [ ] 支持更多云存储提供商（阿里云OSS、腾讯云COS等）
- [ ] 图片压缩和格式转换功能
- [ ] 图片水印添加功能
- [ ] 图片批量处理工具
- [ ] 移动端应用开发
- [ ] 第三方登录集成

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！
