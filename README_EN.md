<h1>AstrNest</h1>
<div>
A modern image hosting and content governance platform built with Spring Boot 3.4.1 + MySQL backend and Vue 3 + Vite frontend, providing complete media upload, storage, management, sharing and API integration solutions.

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
  <a href="https://github.com/luminous-ChenXi/astrnest/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/license-GPL%20v3-blue.svg" alt="license">
  </a>
  <a href="https://github.com/luminous-ChenXi/astrnest/releases">
    <img src="https://img.shields.io/github/release/luminous-ChenXi/astrnest.svg" alt="GitHub release">
  </a>
</p>
<p align="center">
  <a href="./README.md">简体中文</a> · English
</p>
<p align="center">
  <a href="#quick-start">Quick Start</a> · 
  <a href="https://luminouschenxi.com">blog</a> · 
  <a href="#tech-stack">Tech Stack</a> · 
  <a href="https://discord.gg/hBsqcfwC9Q">Discord</a> · 
  <a href="https://github.com/luminous-ChenXi/astrnest">GitHub</a>
</p>

</div>
<img src="./templates/1728x2304.png" width = "300" height = "400" alt="AstrNest" align=right />
  <div align="center">

  # AstrNest

  _Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3._

  唯有青春与梦想不可辜负~！

  </div>
</div>

---

## Core Features | 核心特性

- **Modern Architecture**: Full-stack technology stack with Spring Boot 3.4.1 + Vue 3 + Vite 5
- **Multi-role Permissions**: Support for multi-level permission management system including administrators and regular users
- **Multi-cloud Storage**: Built-in Local, Alibaba Cloud OSS, Tencent COS, Qiniu Kodo, Huawei OBS, Kingsoft KS3, Upyun USS, OneDrive/SharePoint and generic S3 drivers, configurable with one click; S3-compatible drivers default to 5GB threshold triggering 25MB multipart upload, supporting CDN/CNAME, accelerated domains, PathStyle and pre-signed direct upload credentials
- **Intelligent Content Review**: Integrated with Tencent Cloud Data万象 (COS CI) image review and tagging services, automatically backfills AI decisions/tags/RequestId, combined with error code documentation for friendly prompts, with double guarantee of manual review
- **Complete API Ecosystem**: RESTful API + API key authentication + Web management interface
- **Flexible Storage Solutions**: Support for local storage and cloud object storage (OSS/COS)
- **Enterprise-level Security**: Spring Security 6 + JWT authentication + Content Security Policy
- **Member Governance & Quotas**: Admin "Member List" supports viewing avatars/quotas/total likes, and can adjust daily upload and total storage quotas with one click, switch admin/user/guest roles
- **Responsive Design**: Tailwind CSS + Element Plus modern UI components
- **Real-time Monitoring**: System operation status monitoring and operation log auditing
- **Email Service**: Integrated email templates and verification code sending functions, default preset Alibaba Cloud email SMTP

<p align="center">
  <b>🎨 Theme Preview</b>
</p>

<table align="center">
  <tr>
    <td align="center" width="50%">
      <img src="./templates/dark.png" width="100%" alt="Dark Theme" />
      <br>
      <sub><b>🌙 Dark Mode</b></sub>
    </td>
    <td align="center" width="50%">
      <img src="./templates/light.png" width="100%" alt="Light Theme" />
      <br>
      <sub><b>☀️ Light Mode</b></sub>
    </td>
  </tr>
</table>

---

⚠️ **Important License Notice** ⚠️

## License Warning | GPL v3 开源协议警示

**This project is open sourced under the GNU General Public License v3 (GPL v3)**

### Legal Statement
- **Any use, modification, or distribution of this code must comply with the GPL v3 license**
- **Derivative works based on this project must also be open sourced under the GPL v3 license**
- **Prohibited from using this code in closed-source commercial projects**
- **Prohibited from removing copyright information and license statements**

### Warning to Bad Developers
**Please note: The following actions will constitute infringement and may face legal liability:**
- ❌ Privately modifying the license or removing copyright notices
- ❌ Using the code in closed-source commercial products without open sourcing
- ❌ Claiming the code as your own original work
- ❌ Circumventing GPL license requirements for distributing derivative works

### Your Obligations
✅ Retain original copyright and license information  
✅ Modifications based on this project must also be open sourced  
✅ Source code must be provided when distributing  
✅ Clearly identify modification content and modifiers

**Violation of the GPL license will result in legal liability, please respect the open source spirit!**

---

## Acknowledgments | 致谢

### Open Source Component Compliance Statement

This project is built based on many excellent open source components, thank you to the open source community for their contributions! The following lists the main dependency components and their license information by frontend/backend for compliance review:

#### Frontend (/frontend/package.json)
- **Vue 3** [`vue@^3.5.24`](https://github.com/vuejs/core) - [MIT License](https://github.com/vuejs/core/blob/main/LICENSE)
- **Vue Router** [`vue-router@^4.6.3`](https://github.com/vuejs/router) - [MIT License](https://github.com/vuejs/router/blob/main/LICENSE)
- **Pinia** [`pinia@^3.0.4`](https://github.com/vuejs/pinia) - [MIT License](https://github.com/vuejs/pinia/blob/main/LICENSE)
- **Element Plus** [`element-plus@^2.8.6`](https://github.com/element-plus/element-plus) - [MIT License](https://github.com/element-plus/element-plus/blob/dev/LICENSE)
- **Element Plus Icons** [`@element-plus/icons-vue@^2.3.1`](https://github.com/element-plus/element-plus-icons) - [MIT License](https://github.com/element-plus/element-plus-icons/blob/main/LICENSE)
- **Vite** [`vite@^5.4.10`](https://github.com/vitejs/vite) - [MIT License](https://github.com/vitejs/vite/blob/main/LICENSE)
- **Vue Plugin** [`@vitejs/plugin-vue@^5.1.4`](https://github.com/vitejs/vite-plugin-vue) - [MIT License](https://github.com/vitejs/vite-plugin-vue/blob/main/LICENSE)
- **Tailwind CSS** [`tailwindcss@^3.4.17`](https://github.com/tailwindlabs/tailwindcss) - [MIT License](https://github.com/tailwindlabs/tailwindcss/blob/master/LICENSE)
- **Autoprefixer** [`autoprefixer@^10.4.22`](https://github.com/postcss/autoprefixer) - [MIT License](https://github.com/postcss/autoprefixer/blob/main/LICENSE)
- **PostCSS** [`postcss@^8.5.6`](https://github.com/postcss/postcss) - [MIT License](https://github.com/postcss/postcss/blob/main/LICENSE)
- **Axios** [`axios@^1.7.7`](https://github.com/axios/axios) - [MIT License](https://github.com/axios/axios/blob/main/LICENSE)
- **Day.js** [`dayjs@^1.11.13`](https://github.com/iamkun/dayjs) - [MIT License](https://github.com/iamkun/dayjs/blob/dev/LICENSE)
- **DOMPurify** [`dompurify@^3.3.0`](https://github.com/cure53/DOMPurify) - [Apache License 2.0](https://github.com/cure53/DOMPurify/blob/main/LICENSE)
- **Marked** [`marked@^12.0.2`](https://github.com/markedjs/marked) - [MIT License](https://github.com/markedjs/marked/blob/master/LICENSE)
- **GSAP** [`gsap@^3.12.5`](https://github.com/greensock/GSAP) - [Standard 'No Charge' License](https://github.com/greensock/GSAP/blob/master/LICENSE)
- **Lucide Icons** [`lucide-vue-next@^0.555.0`](https://github.com/lucide-icons/lucide) - [ISC License](https://github.com/lucide-icons/lucide/blob/main/LICENSE)
- **Plyr** [`plyr`](https://github.com/sampotts/plyr) - [MIT License](https://github.com/sampotts/plyr/blob/master/LICENSE.md)

#### Backend (/backend/pom.xml)
- **Spring Boot 3.4.1** - [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
  - spring-boot-starter-web
  - spring-boot-starter-security  
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - spring-boot-starter-mail
  - spring-boot-starter-actuator
  - spring-boot-starter-test
- **SpringDoc OpenAPI** [`springdoc-openapi-starter-webmvc-ui@2.7.0`](https://github.com/springdoc/springdoc-openapi) - [Apache License 2.0](https://github.com/springdoc/springdoc-openapi/blob/master/LICENSE)
- **Jackson** [`jackson-datatype-jsr310`](https://github.com/FasterXML/jackson) - [Apache License 2.0](https://github.com/FasterXML/jackson/blob/master/LICENSE)
- **MySQL Connector/J** - [GPL License with FOSS License Exception](https://github.com/mysql/mysql-connector-j/blob/release/8.x/LICENSE)
- **AWS SDK S3** [`software.amazon.awssdk:s3@2.25.58`](https://github.com/aws/aws-sdk-java-v2) - [Apache License 2.0](https://github.com/aws/aws-sdk-java-v2/blob/master/LICENSE.txt)
- **Alibaba Cloud OSS SDK** [`com.aliyun.oss:aliyun-sdk-oss@3.17.4`](https://github.com/aliyun/aliyun-oss-java-sdk) - [Apache License 2.0](https://github.com/aliyun/aliyun-oss-java-sdk/blob/master/LICENSE)
- **Tencent Cloud COS SDK** [`com.qcloud:cos_api@5.6.255.1`](https://github.com/tencentyun/cos-java-sdk-v5) - [MIT License](https://github.com/tencentyun/cos-java-sdk-v5/blob/master/LICENSE)
- **Upyun Java SDK** [`com.upyun:java-sdk@4.2.3`](https://github.com/upyun/java-sdk) - [MIT License](https://github.com/upyun/java-sdk/blob/master/LICENSE)
- **Lombok** - [MIT License](https://github.com/projectlombok/lombok/blob/master/LICENSE)
- **Spring Boot Configuration Processor** - [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
- **H2 Database** (test) - [MPL 2.0 / EPL 1.0](https://github.com/h2database/h2database/blob/master/LICENSE.txt)
- **Spring Security Test** (test) - [Apache License 2.0](https://github.com/spring-projects/spring-security/blob/main/LICENSE.txt)

#### Disclaimer
- The above license information is based on the latest information from each component's official repository
- Specific versions may change with project updates
- When using this project, please ensure compliance with all dependency component license requirements
- It is recommended to conduct detailed license compliance review before commercial use

---

## Tech Stack | 技术栈

### Backend
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Database**: MySQL 5.7+/8.0
- **Security**: Spring Security 6
- **Documentation**: SpringDoc OpenAPI 3
- **AI Review SDK**: Tencent Cloud COS CI (`com.qcloud:cos_api`)
- **Build**: Maven Wrapper

### Frontend
- **Framework**: Vue 3.5.24
- **Build**: Vite 5.4.10
- **Routing**: Vue Router 4
- **State Management**: Pinia 3
- **UI Components**: Element Plus 2.8.6
- **Styling**: Tailwind CSS 3
- **Icons**: Lucide Vue, Element Plus Icons

---

## Project Structure | 项目结构

```
astrnest/
├─ backend/      # Spring Boot service: REST API, authentication, content review, API key management
├─ frontend/     # Vue 3 + Vite SPA: dashboard, upload center, security console, API integration
└─ storage/      # (Runtime generated) local storage directory, can be changed to OSS/COS via configuration
```

---

## API Documentation | API 接口文档概览

### Swagger / OpenAPI
- Online documentation entry: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- Authentication: Token obtained after login placed in `Authorization: Bearer <token>`, some interfaces require admin privileges.

---

## Quick Start | 快速开始

### Environment Requirements
- **JDK 21** (Recommended: OpenJDK 21 / Temurin)
- **Node.js 18+** and npm 9+
  - If npm using Tsinghua mirror returns 404, please execute `npm config set registry https://registry.npmjs.org/` first, and if necessary `npm cache clean --force` before installation.
- **MySQL 8.0**, and create `astrnest` database and authorized account
- **FFmpeg 4.4+** (for local video thumbnail generation, executable file path can be specified via `astrnest.video-thumbnail.ffmpeg-path`)
- Optional: `curl`, `pm2`, Nginx and other deployment auxiliary tools

### One-click Start (Development Environment)

1. **Clone Project**
```bash
git clone <repository-url>
cd AstrNest
```

2. **Database Initialization**
```bash
mysql -u root -p < backend/db/init.sql
```
This script will automatically:
- Create `astrnest` database and `astrnest/astrnestPass!` dedicated account, and grant database-level privileges;
- Create/verify core tables including `users`, `roles`, `user_roles`, `api_keys`, `upload_records`, `upload_likes`, `system_config`, `content_policy`, `user_login_events`, `interactions`, and create `media`, `media_tags` views for BI convenience;
- Add fields/indexes/constraints to old databases, add user profile fields (role, status, two-factor, JSON settings, quota/usage), media metadata fields (UUID, hash, type, dimensions, AI description, statistics, JSON metadata, 20MB CHECK constraint), tag source and confidence, etc.;
- Preset empty SMTP (`smtp.example.com` + `no-reply@example.com`, disabled by default) and system administrator email placeholder to avoid exposing real credentials;
- Insert default roles, super administrator account (`admin` / `chenxi123`) and system configuration/content policies (including AI review switches, Tencent Cloud keys, detection scenarios and threshold defaults).
> Can be safely executed repeatedly, script uses `IF NOT EXISTS` / `ON DUPLICATE KEY` / `INFORMATION_SCHEMA` detection, will not destroy existing data.

3. **Start Backend Service**
```bash
cd backend
./mvnw spring-boot:run
```

4. **Start Frontend Service** (New Terminal)
```bash
cd frontend
npm install
npm run dev
```

5.5) **Access Application**
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui/index.html

**Default Administrator**: `admin` / `chenxi123` (Please be sure to modify).

> If you want to directly see configuration details (environment variables, file paths, initialization scripts, FFmpeg, storage switching, etc.), please jump to `CONFIG_GUIDE.md`.

### Deployment Overview
- **Docker Compose (Recommended)**: Copy `.env.example` → `.env`, fill in database/domain/SMTP/storage, then execute:
```bash
docker compose --env-file .env up -d
```
- **Traditional Deployment**: `backend` package `./mvnw clean package && java -jar target/backend-0.0.1-SNAPSHOT.jar`; `frontend` run `npm run build` and then hand over `dist/` to Nginx/CDN.

For more detailed environment variables, Nginx reverse proxy, CDN/object storage switching, please check `CONFIG_GUIDE.md`.

---

## Problem Solving | 问题解决

| Question | Solution |
| --- | --- |
| CORS Error | Ensure the CORS whitelist in backend `SecurityConfig` includes the current frontend domain, or supplement `Access-Control-*` headers at the deployment entry layer (Nginx). |
| Database Authentication Failed | Check if `spring.datasource.username/password` is consistent with MySQL user authorization. |
| API Upload 401 | `POST /api/uploads` requires login as admin or carrying valid `X-API-Key`. Create keys in Security Center/API page. |
| Static Resource Access Failure | If switching object storage, remember to sync configure `astrnest.storage.local.public-base-url` or set asset domain in backend, and ensure CDN/bucket permissions are correct. |
| Email Sending Failed | Check if email configuration is correct, including SMTP server, port, username and password |
| Verification Code Verification Failed | Ensure verification code is within validity period and entered correctly |

### Common Scripts
- Backend Development: `cd backend && ./mvnw spring-boot:run`

---

## Installation & Configuration Quick Reference | 安装配置速查

| Step | Required Content | Target File |
| --- | --- | --- |
| 1. Fill `.env` | `MYSQL_*` (database host/port/database name/account/password), `ASTRNEST_DB_*` (backend JDBC address and account), `ASTRNEST_ADMIN_*` (admin username/password/email), `ASTRNEST_STORAGE_*` (upload root path and `/upload` prefix), `PUBLIC_SITE_URL`/`PUBLIC_ASSET_URL`/`BACKEND_API_PUBLIC_URL`, `ASTRNEST_ASSET_DOMAIN`, `VITE_API_BASE_URL`/`VITE_PUBLIC_ASSET_BASE`, `SMTP_*` | Root directory `.env` (shared by `docker-compose.yml`, Spring Boot, Vite) |
| 2. Review `docker-compose.yml` | `services.mysql.environment.*` (database name/account/password), `services.backend.environment.*` (database URL, admin info, storage path, domain variables), `services.frontend.environment.*` (`VITE_API_BASE_URL` / `VITE_PUBLIC_ASSET_BASE`) | `docker-compose.yml` (built-in `env_file: .env`, can also override directly in `environment`) |
| 3. Local/IDE fallback config | `spring.datasource.*`, `astrnest.storage.*`, `astrnest.admin.*` | `backend/src/main/resources/application.yml` |
| 4. Configure FFmpeg thumbnails | `astrnest.video-thumbnail.*` (or `ASTRNEST_VIDEO_THUMBNAIL_*` environment variables: switch, FFmpeg path, screenshot time, quality, output directory) | `backend/src/main/resources/application.yml` |
| 5. Initialization script | `CREATE DATABASE/USER`, `GRANT`, `INSERT INTO users` (admin), `INSERT INTO chenxi_mail_config` (SMTP), `UPDATE system_config.asset_domain` | `backend/db/init.sql` |
| 6. Frontend environment | `VITE_API_BASE_URL` (frontend access backend), `VITE_PUBLIC_ASSET_BASE` (copy direct link), `VITE_SITE_NAME` | `frontend/.env.development` / `.env.production` / `.env.example` |
| 7. Detailed guide | Full parameter descriptions, direct link proxy examples, checklists | [CONFIG_GUIDE.md](CONFIG_GUIDE.md) |

> ✅ Recommended sequence: Copy `.env.example` → Fill database/admin/domain/SMTP → Run `mysql -u root -p < backend/db/init.sql` → `docker compose up -d`. Any parameter updates should sync `.env` with `backend/db/init.sql` to avoid inconsistent admin or SMTP values.

---

## Core Function Modules | 核心功能模块

### 1. User Management | 用户管理
- **User Registration & Login**: Support username/password login and Chenxi authentication system
- **Profile Management**: Users can view and modify personal information
- **Password Security**: Support password changes and security settings
- **Login History**: Record user login IP and time information
- **Member List**: Admin backend provides dark glass-style "directory" view, showing avatars, emails, signatures, storage usage, total likes, can adjust daily upload and space quotas in real-time and switch roles/delete users

### 2. Image Upload & Storage | 图片上传与存储
- **Multi-format Support**: Support common image formats (JPG, PNG, GIF, WEBP, etc.)
- **Batch Upload**: Support multiple files uploaded simultaneously
- **Storage Strategy**: Configurable local storage or cloud object storage
- **Upload Limits**: Support file size, type, quantity limits

### 3. Content Management | 内容管理
- **Media Management**: Users can view, delete, modify visibility
- **Batch Operations**: Support batch delete images
- **Like Feature**: Users can like images
- **Visibility Control**: Support public/private image settings

### 4. Security Management | 安全管理
- **Permission Control**: Role-based permission management system
- **Content Review**: Automatic detection and manual review of违规 content
- **Domain Whitelist**: Control image reference domains
- **API Key Management**: Support API call authentication and quota management

### 5. System Management | 系统管理
- **User Management**: Admins can manage user roles and restrictions
- **System Configuration**: Dynamically configure system parameters (including Tencent Cloud AI intelligent review, tag thresholds, footer, cleanup policies, etc.)
- **Monitoring Statistics**: Real-time monitoring of system operation status
- **Operation Logs**: Record all key operations

### 6. Email Service | 邮件服务
- **Email Templates**: Support custom email templates
- **Verification Code Sending**: Integrated email verification code function
- **Configuration Management**: Flexible email server configuration


### 7. Public Gallery | 公共图库
- **Public Display**: Support display and browsing of public images, can jump to uploader or recent liker detail pages
- **Like Interaction**: Both members/visitors can like, visitors need to include `X-Chenxi-Visitor` fingerprint header with request; admins can disable "visitor likes" in system configuration
- **Real-time Feedback**: Pop-up display of recent likes (including visitor prompts), convenient for quick governance
- **Paged Browsing**: Support paged display of large numbers of images

---

## System Architecture | 系统架构

AstrNest adopts a front-end and back-end separation architecture:

- **Frontend**: Vue 3 + Vite + Element Plus + Tailwind CSS
- **Backend**: Spring Boot 3 + Spring Security + JPA + MySQL
- **Authentication**: JWT Token + API Key dual authentication mechanism
- **Storage**: Local storage + cloud object storage extension support
- **Security**: Role-based permission control + content security policy

---

## Development Roadmap | 开发路线图

- [ ] Support for more cloud storage providers (Alibaba Cloud OSS, Tencent Cloud COS, etc.)
- [ ] Image compression and format conversion features
- [ ] Image watermark adding features
- [ ] Image batch processing tools
- [ ] Mobile application development
- [ ] Third-party login integration

---

## License | 许可证

This project is open sourced under the [GNU General Public License v3 (GPL v3)](LICENSE).

![GPL-v3](https://www.gnu.org/graphics/gplv3-127x51.png)

## Security | 安全

For security-related issues, please see [SECURITY.md](SECURITY.md).

---

## Contributing | 贡献指南

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for detailed process.

---

## Issue Reporting | 问题报告

If you encounter problems, please:
1. Check [FAQ](#常见问题)
2. Search [GitHub Issues](https://github.com/your-repo/astrnest/issues)
3. Create a new Issue

---

## FAQ | 常见问题

| Question | Solution |
| --- | --- |
| CORS Error | Ensure the CORS whitelist in backend `SecurityConfig` includes the current frontend domain, or supplement `Access-Control-*` headers at the deployment entry layer (Nginx). |
| Database Authentication Failed | Check if `spring.datasource.username/password` is consistent with MySQL user authorization. |
| API Upload 401 | `POST /api/uploads` requires login as admin or carrying valid `X-API-Key`. Create keys in Security Center/API page. |
| Static Resource Access Failure | If switching object storage, remember to sync configure `astrnest.storage.local.public-base-url` or set asset domain in backend, and ensure CDN/bucket permissions are correct. |
| Email Sending Failed | Check if email configuration is correct, including SMTP server, port, username and password |
| Verification Code Verification Failed | Ensure verification code is within validity period and entered correctly |

---

## Contact | 联系方式

- **Issue Feedback**: Submit via GitHub Issues
- **Security Reports**: chenxi@luminouschenxi.net
- **Discord**: [Join our community](https://discord.gg/hBsqcfwC9Q)
---

⭐ If this project is helpful to you, please give us a Star!