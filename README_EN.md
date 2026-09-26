# AstrNest Media Management System
<div>

Modern full-stack media management platform built with Spring Boot 3.4.1 and Vue 3. Multi-cloud storage, AI content moderation, complete API ecosystem.

<p align="center">
  <a href="https://github.com/vuejs/core">
    <img src="https://img.shields.io/badge/vue-3.5.24-brightgreen.svg?style=flat-square&logo=vue.js" alt="vue">
  </a>
  <a href="https://github.com/element-plus/element-plus">
    <img src="https://img.shields.io/badge/element--plus-2.8.6-brightgreen.svg?style=flat-square&logo=element" alt="element-plus">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/spring--boot-3.4.1-brightgreen.svg?style=flat-square&logo=spring" alt="spring-boot">
  </a>
  <a href="https://github.com/luminous-ChenXi/astrnest/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/license-GPL--3.0%20with%20Additional%20Terms%20%28Non--Commercial%29-blue.svg?style=flat-square" alt="license">
  </a>
  <a href="https://github.com/luminous-ChenXi/astrnest/releases">
    <img src="https://img.shields.io/github/release/luminous-ChenXi/astrnest.svg?style=flat-square" alt="GitHub release">
  </a>
  <a href="https://coderabbit.ai">
    <img src="https://img.shields.io/coderabbit/prs/github/luminous-ChenXi/AstrNest?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews" alt="CodeRabbit Reviews">
  </a>
</p>
<p align="center">
  <a href="./README.md">简体中文</a> · English
</p>
<p align="center">
  <a href="#quick-start">Quick Start</a> · 
  <a href="https://luminouschenxi.com">Blog</a> · 
  <a href="#tech-stack">Tech Stack</a> · 
  <a href="#acknowledgments">Acknowledgments</a> ·   
  <a href="https://discord.gg/hBsqcfwC9Q">Discord</a> · 
  <a href="./backend">Backend</a>
</p>

</div>

> 🪺 A member of the **Chenxi Ecosystem (辰汐生态)** — alongside [LuomiNest](https://github.com/LuminousCX/LuomiNest) (desktop AI companion), [LuomiBlog](https://github.com/luminous-ChenXi/LuomiBlog) (AI knowledge-base blog), and Teachenxi (learning companion app). This project's role: an open-source image hosting and media management platform (multi-cloud storage, AI content moderation).

<img src="./templates/1728x2304.png" width = "300" height = "400" alt="AstrNest" align=right />
<div align="center">

# AstrNest

_Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3._

Only youth and dreams are worth living for!

</div>

---

## Core Features

- **Modern Architecture**: Spring Boot 3.4.1 + Vue 3 + Vite 5 full-stack technology stack
- **Multi-role Permissions**: Supports multi-level permission management system for administrators, regular users, and more
- **Multi-cloud Storage**: Built-in Local, Alibaba Cloud OSS, Tencent COS, Qiniu Kodo, Huawei OBS, Kingsoft KS3, Upyun USS, OneDrive/SharePoint, and generic S3 drivers. Switch between them with one click in configuration. S3-compatible drivers default to 5GB threshold triggering 25MB chunked uploads, supporting CDN/CNAME, accelerated domains, PathStyle, and presigned direct upload credentials
- **Intelligent Content Moderation**: Integrated with Tencent Cloud Data Lake (COS CI) image moderation and tagging services, automatically populating AI decisions/tags/RequestId, and providing user-friendly prompts based on error code documentation, combined with manual review for double protection
- **Complete API Ecosystem**: RESTful API + API key authentication + Web management interface
- **Flexible Storage Solutions**: Supports local storage and cloud object storage (OSS/COS)
- **Security Management**: Spring Security 6 + JWT authentication + Content Security Policy
- **Member Governance & Quotas**: Admin "Member List" supports viewing avatars/quotas/total likes, and allows one-click adjustment of daily upload and total storage quotas, switching between admin/user/guest roles
- **Responsive Design**: Modern UI component library, supporting PC, mobile, tablet, and other multi-device adaptation
- **Real-time Monitoring**: System operation status monitoring and operation log auditing
- **Email Service**: Integrated email templates and verification code sending functionality, with Alibaba Cloud Mail SMTP pre-configured by default

## License Warning | GPL-3.0 with Additional Terms (Non-Commercial)
### ⚠️ **Important License Reminder** ⚠️

**This project is open-sourced under the GNU General Public License v3 (GPL-3.0) with Additional Terms (Non-Commercial + Attribution); in case of conflict with GPL-3.0, the Additional Terms prevail. See the full text in [LICENSE](LICENSE)**

### Four Red Lines:
- ✅ **Freedom to Deploy**: deploying and running this software under this license requires no further permission
- ✅ **Freedom to Modify and Redistribute**: modifications and redistribution are allowed, but derivative works must be licensed under the same terms ("GPL-3.0 + these Additional Terms") with source code provided
- ❌ **No Commercial Use**: you may not use this software or derivative works for any commercial purpose (including but not limited to selling, paid services/SaaS, commercial bundling, or advertising monetization) without prior written authorization from the copyright holder
- ❌ **Attribution Required**: any derivative work or work that incorporates code from this software must retain the original copyright notice and this license in full, and must credit the original project name and repository URL in a prominent location (About page / README / documentation)

### Warning to Unethical Developers:
**Please note: The following actions constitute infringement and may face legal liability:**
- ❌ Privately modifying the license or removing copyright notices
- ❌ Using the code for any commercial purpose (including closed-source commercial products and commercial services)
- ❌ Claiming the code as your own original work
- ❌ Circumventing this license when distributing derivative works

**This project has a development cycle of 3 months, developed independently by ChenXi. The development process was arduous; violating the license will result in legal consequences. Please respect the open-source spirit!**

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Database**: MySQL 5.7+/8.0
- **Security**: Spring Security 6
- **Documentation**: SpringDoc OpenAPI 3
- **AI Moderation SDK**: Tencent Cloud COS CI (`com.qcloud:cos_api`)
- **Build**: Maven Wrapper

### Frontend
- **Framework**: Vue 3.5.24
- **Build**: Vite 5.4.10
- **Routing**: Vue Router 4
- **State Management**: Pinia 3
- **UI Components**: Element Plus 2.8.6
- **Styling**: Tailwind CSS 3
- **Icons**: Lucide Vue, Element Plus Icons

## System Architecture

### Architecture Diagram
AstrNest adopts a front-end and back-end separation architecture:

- **Frontend**: Vue 3 + Vite + Element Plus + Tailwind CSS
- **Backend**: Spring Boot 3 + Spring Security + JPA + MySQL
- **Authentication**: JWT Token + API Key dual authentication mechanism
- **Storage**: Local storage + cloud object storage extension support
- **Security**: Role-based access control + content security policy

### Project Structure

```
astrnest/
├─ backend/      # Spring Boot service: REST API, authentication, content moderation, API key management
├─ frontend/     # Vue 3 + Vite single-page application: dashboard, upload center, security console, API integration
└─ storage/      # (Generated at runtime) Local storage directory, can be changed to OSS/COS via configuration
```

## API Documentation Overview

### Swagger / OpenAPI
- Online documentation entry: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- Authentication method: Token obtained after login placed in `Authorization: Bearer <token>`, some interfaces require admin privileges.

### Authentication Contract
- **JWT (recommended)**: `POST /api/auth/login` returns a JWT on success; send it as `Authorization: Bearer <token>` on subsequent requests.
  - Tokens are valid for **72 hours by default**, configurable via `astrnest.jwt.ttl-hours` (env `ASTRNEST_JWT_TTL_HOURS`).
  - In production you must set the signing key `astrnest.jwt.secret` (env `ASTRNEST_JWT_SECRET`; use a long random string and keep it out of the repository).
- **HTTP Basic (kept for compatibility)**: still available for API plugins/scripts (e.g. Typora, PicGo custom uploaders), alongside API Key authentication.

## Quick Start (Development)

Here is an example of local development on Ubuntu:

1) Clone and dependencies
```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest
```
2) Initialize database (local MySQL)
```bash
mysql -u root -p < backend/db/init.sql
```

> **⚠️ Important Reminder**: Before starting the backend, ensure the database configuration is correct (prefer injecting everything via environment variables, see `.env.example`):
> - Database URL: `jdbc:mysql://localhost:3306/astrnest`
> - Username: `astrnest` (can be overridden via environment variable `ASTRNEST_DB_USERNAME`)
> - Password: injected via the `ASTRNEST_DB_PASSWORD` environment variable; never commit real credentials

3) Start backend
```bash
cd backend
./mvnw spring-boot:run
```
4) Start frontend (new terminal)
```bash
cd frontend
npm install
npm run dev
```
> **⚠️ Important Reminders**:
> - If `npm install` encounters permission errors (such as `EACCES` or `permission denied`), it's usually due to npm global directory permission issues. Solutions:
>   1. Modify npm global directory permissions: `sudo chown -R $(whoami) ~/.npm`
>   2. Or use npx to run: `npx npm install`
>   3. Or clear npm cache and retry: `npm cache clean --force && npm install`
>   4. If the above methods don't work, consider using `sudo npm install` to install dependencies (not recommended).
> - If using a mirror source results in 404 errors, it's recommended to switch back to the official source: `npm config set registry https://registry.npmjs.org/`
> - If `npm run dev` startup shows `EACCES: permission denied, mkdir '.../node_modules/.vite/...'` error, it means the `node_modules` directory has insufficient permissions. Solutions:
>   1. Fix `node_modules` directory permissions: `sudo chown -R $(whoami) node_modules`
>   2. Or delete and reinstall directly: `rm -rf node_modules && npm install`

5) Access
- Frontend: http://localhost:5175
- Backend: http://localhost:8081
- API Documentation: http://localhost:8081/swagger-ui/index.html

> **⚠️ Important Reminders**:
> - If "Network Connection Error" is displayed, please check if the backend service has started, if the port is correct; check if the port configuration in `application.yml` matches the actual one; check if the firewall has allowed that port; check if other services are occupying that port; check if a proxy (such as Nginx) is enabled; check if the proxy configuration is correct;
> - If "404 Not Found" is displayed, please check if the frontend project has been built correctly and if the files in the `dist/` directory exist.
> - If "CORS Error" is displayed, please check if the CORS configuration in the backend `SecurityConfig` allows the current frontend domain.
> - If "403 Forbidden" is displayed, please check if the current user role has permission to access that interface.
> - If "500 Internal Server Error" is displayed, please check the backend logs to find specific error information.

> **Admin Account Note**: The initialization SQL **no longer ships a preset admin account**. The recommended way is to create the initial administrator through the **Install Wizard** (`/install`, see below) on first deployment; you can also create (or reset) an admin with the bundled `init-admin.py` / `init-admin.sh` / `init-admin.bat` scripts. As a fallback for environments without the wizard, **the first user to register automatically becomes the administrator**.

> If you want to see configuration details directly (environment variables, file paths, initialization scripts, FFmpeg, storage switching, etc.), please jump to `CONFIG_GUIDE.md`.

### Install Wizard (Recommended for Fresh Deployments)

AstrNest ships a **WordPress-style visual install wizard**. After a fresh deployment (empty database), opening the site automatically redirects to `http://your-domain.com/install`, guiding you through four steps:

1. **Environment Checks**: database connection and version (MySQL >= 8.0 recommended), table structure, local storage directory writability, Java runtime, FFmpeg (missing is a warning only). Fatal problems block progression and include troubleshooting hints, with a "Re-check" button.
2. **Install Database**: one-click execution of the wizard-specific script `backend/db/install-schema.sql` to create tables and seed default roles/config (idempotent; re-running never damages existing data).
3. **Create Administrator**: set up the initial admin account (ADMIN role, BCrypt-hashed password, unlimited upload quota). This is the single entry point for the initial account.
4. **Finish**: writes the completion marker and brings you to the site.

Comparison of the two deployment paths:

| Deployment Path | Database Schema | Initial Administrator |
| --- | --- | --- |
| **Docker Compose** (mounts `init.sql` for auto-initialization) | Created automatically by `backend/db/init.sql` on the MySQL container's first start | First registered user becomes ADMIN automatically, or use the `init-admin` script |
| **Manual / hosting-panel / bare-jar deployment** (init SQL never executed) | Open the site to enter the wizard; step 2 creates all tables (`install-schema.sql`) | Created in wizard step 3 (recommended), or the first registered user becomes ADMIN |

- Wizard URL: `http://your-domain.com/install` (any entry point works; until installation completes, every page redirects to the wizard).
- **Re-visiting `/install` after installation** shows "System already installed" and refuses to re-initialize; the `/api/install` write endpoints always return 403 once installed, preventing replay calls.
- Note: the wizard does not write database connection settings — the Spring Boot application must already be able to connect to MySQL (via `ASTRNEST_DB_URL` / `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD`, etc.) before it can start; the wizard handles **table creation and the initial account**. See CONFIG_GUIDE.md section 4.3 for check-item meanings and troubleshooting.

### Deployment Overview
- **Docker Compose (Recommended)**: Copy `.env.example` → `.env`, fill in database/domain/SMTP/storage, then execute:
```bash
docker compose --env-file .env up -d
```
- **Traditional Deployment**: `backend` package `./mvnw clean package && java -jar target/backend-0.0.1-SNAPSHOT.jar`; `frontend` run `npm run build` then hand over `dist/` to Nginx/CDN.

For more detailed environment variables, Nginx reverse proxy, CDN/object storage switching, please see `CONFIG_GUIDE.md`.

### Production Notes
- **`ASTRNEST_TRUSTED_PROXY`** (default `false`): set to `true` when the backend runs behind a reverse proxy (Nginx etc.), so the backend trusts and parses `X-Real-IP` / `X-Forwarded-For` and audit logs / rate limiting see the real client IP; config key: `astrnest.security.trusted-proxy`.
- **`astrnest.jwt.secret` / `astrnest.jwt.ttl-hours`**: JWT signing key and token TTL (default 72 hours); the secret must be explicitly configured in production.
- **Nginx `client_max_body_size`**: the sample reverse-proxy config already raises the request body limit; keep it in sync with `spring.servlet.multipart.max-file-size`, otherwise large uploads fail with 413.
- **Docker Compose port binding**: the database and backend ports in `docker-compose.yml` are bound to `127.0.0.1` only; expose the service through a reverse proxy in production instead of publishing these ports directly.
- **SSO single sign-on**: see "SSO Single Sign-On (External Identity Provider)" below.

### SSO Single Sign-On (External Identity Provider, disabled by default)

AstrNest supports unified login through external identity providers using **OAuth 2.1 / OIDC "authorization code + PKCE(S256)"**, compatible with "Chenxi Passport" and any standard OAuth 2.1/OIDC provider (example issuer: `https://passport.example.com`). The feature is **disabled by default** (`astrnest.sso.enabled=false`); when disabled the frontend hides the entry and backend endpoints return explicit errors, with zero impact on existing local login/registration/API keys.

- **Login flow**: the frontend redirects to the provider's authorize endpoint (PKCE) → the provider calls back `/auth/sso/callback` → the code is exchanged for an `access_token` → `POST /api/auth/sso/exchange` introspects the token against the issuer (`{issuer}/oauth2/introspect`) and issues a local JWT identical to a local login response.
- **Shadow account**: the first SSO login creates a shadow account (`identity_source=passport`, linked via `sso_sub`); nickname/avatar/email are synced from the identity provider on each login and are **read-only** locally (profile and password changes are rejected).
- **Register the callback URL on the provider side**: `https://<your-domain>/auth/sso/callback`.

| Config key | Env variable | Default | Description |
| --- | --- | --- | --- |
| `astrnest.sso.enabled` | `ASTRNEST_SSO_ENABLED` | `false` | Enable SSO login |
| `astrnest.sso.issuer` | `ASTRNEST_SSO_ISSUER` | empty | Issuer base URL, e.g. `https://passport.example.com` |
| `astrnest.sso.client-id` | `ASTRNEST_SSO_CLIENT_ID` | empty | Public client id registered at the provider |
| `astrnest.sso.redirect-uri` | `ASTRNEST_SSO_REDIRECT_URI` | empty | Callback URL, must match the provider registration exactly |
| `astrnest.sso.scopes` | — (yml only) | `openid, profile, email` | Requested scopes |
| `astrnest.sso.introspect-timeout-seconds` / `userinfo-timeout-seconds` | — (yml only) | `5` | Introspection / userinfo request timeout (seconds) |

## Problem Solving

| Problem | Suggested Solution |
| --- | --- |
| CORS Error | Ensure the CORS whitelist in the backend `SecurityConfig` includes the current frontend domain, or supplement `Access-Control-*` headers at the deployment entry layer (Nginx). |
| Database Authentication Failure | Check if `spring.datasource.username/password` is consistent with MySQL user authorization. |
| API Upload 401 | `POST /api/uploads` requires logged-in admin or valid `X-API-Key`. Create keys on the Security Center/API page. |
| Static Resources Not Accessible | If switching to object storage, remember to sync configure `astrnest.storage.local.public-base-url` or set the asset domain in the backend, and ensure CDN/bucket permissions are correct. |
| Email Sending Failure | Check if email configuration is correct, including SMTP server, port, username, and password |
| Verification Code Validation Failure | Ensure the verification code is within the validity period and entered correctly |
| Database Connection Timeout | Check `spring.datasource.hikari.connection-timeout` configuration (default 30000ms), ensure network stability or appropriately increase timeout |

### Common Scripts
- Backend development: `cd backend && ./mvnw spring-boot:run`
- Backend testing: `cd backend && ./mvnw test`
- Frontend development: `cd frontend && npm run dev`
- Frontend build: `cd frontend && npm run build`

### API Quick Examples
```bash
# Login
curl -X POST "http://localhost:8081/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"<your-username>","password":"<your-password>"}'

# Upload (API Key)
curl -X POST "http://localhost:8081/api/uploads" \
  -H "X-API-Key: <your-api-key>" \
  -F "file=@/path/to/image.jpg"
```

## Development Roadmap
- ~~Add manual image review function, support manual review of image violations~~ ✅
- ~~Add AI image violation query function, support batch query of image violations~~ ✅
- ~~Improve image Tag label function, support batch add, delete, search~~ ✅
- ~~Support more cloud storage providers (Alibaba Cloud OSS, Tencent Cloud COS, etc.)~~ ✅
- [ ] Beginner-friendly initialization page
- [ ] Image compression and format conversion function
- [ ] Image watermark adding function
- [ ] Image batch processing tool
- [ ] Mobile application development
- [ ] Third-party login integration

## Acknowledgments

### Open Source Component Compliance Statement
This project is built upon numerous excellent open-source components. Thanks to the open-source community for their contributions! Below are the main dependency components listed by frontend/backend with their license information for compliance review:

#### Frontend (refer to /frontend/package.json)
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

#### Backend (refer to /backend/pom.xml)
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
- Specific versions may change as the project updates
- When using this project, please ensure compliance with all dependency component license requirements
- It is recommended to conduct detailed license compliance review before commercial use


## Contributing

Welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for detailed process.

## Issue Reporting

If you encounter problems, please:
1. Check [FAQ](#faq)
2. Search [GitHub Issues](https://github.com/your-repo/astrnest/issues)
3. Create a new Issue (and welcome your valuable suggestions!)
4. Check [Contact](#contact)

## License

This project is open-sourced under [GPL-3.0 with Additional Terms (Non-Commercial)](LICENSE) (non-commercial + attribution additional terms).

![GPL-v3](https://www.gnu.org/graphics/gplv3-127x51.png)

## Security

For security-related issues, please see [SECURITY.md](SECURITY.md).


## Contact

- **Issue Feedback**: Submit via GitHub Issues
- **Email**: chenxi@luminouschenxi.net
- **Discord**: [LuminousChenxi](https://discord.gg/hBsqcfwC9Q)

---
Please like! Please follow! Please support!
⭐ If this project helps you, please give us a Star!

> 若受本项目启发，欢迎附上出处链接。/ If this project inspires your work, a link back is appreciated.
