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
  - Tokens expire after **30 days of inactivity by default** (sliding refresh on active use), configurable via `chenxi.passport.access-token-days` (env `CHENXI_PASSPORT_ACCESS_TOKEN_DAYS`).
  - In production you must set the signing key `astrnest.jwt.secret` (env `ASTRNEST_JWT_SECRET`; use a long random string and keep it out of the repository).
- **HTTP Basic (kept for compatibility)**: still available for API plugins/scripts (e.g. Typora, PicGo custom uploaders), alongside API Key authentication.

## Quick Start

Whichever route you choose, the first launch ends in the same **visual install wizard** that creates the tables and the initial administrator. Pick one of two paths:

| Route | For whom | Characteristics |
| --- | --- | --- |
| **[Five-Minute Deployment (Docker Compose)](#five-minute-deployment-docker-compose)** | Anyone with a server and Docker | Three commands to bring up the full stack (MySQL/backend/frontend), wizard to finish |
| **[Manual Deployment (Jar + Nginx)](#manual-deployment-java-21--mysql-8--nginx)** | Hosting panels / bare metal / existing MySQL & Nginx | Full control; Nginx serves image links statically (recommended for production) |

> **Docs split**: this section = quick start and deployment routes; **[CONFIG_GUIDE.md](CONFIG_GUIDE.md)** = the full configuration reference (per-variable details, file paths, database initialization, object storage switching, Windows troubleshooting, Nginx/CDN advanced usage). All commands below have been verified against the repository: the Maven Wrapper lives at `backend/mvnw` (`mvnw.cmd` on Windows), Compose services are `mysql` / `backend` / `frontend`, backend port `8081`, frontend container port `80`.

### Five-Minute Deployment (Docker Compose)

**Prerequisites**: a server with 2 vCPU / 4 GB RAM or better; Docker 20.10+ with the compose v2 plugin; ports 80/443 open.

**Step 1: Clone and configure environment variables**

```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest
cp .env.example .env
vi .env
```

Required edits in `.env` (Compose refuses to start when password variables are missing — a deliberate guard against weak credentials):

| Variable | Description |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | MySQL root password and application account password |
| `ASTRNEST_DB_PASSWORD` | Backend JDBC password, **keep it identical to `MYSQL_PASSWORD`** |
| `ASTRNEST_JWT_SECRET` | Required in production: generate with `openssl rand -base64 64`. Without it the backend generates a temporary key on every restart, **forcing everyone to log in again**. The variable is not explicitly passed through in the compose template, but `env_file` injects the whole `.env` into the container, so defining it there is enough |
| `PUBLIC_SITE_URL` / `PUBLIC_ASSET_URL` / `BACKEND_API_PUBLIC_URL` | Replace with your domain, e.g. `https://img.example.com` and `https://img.example.com/upload` |

Also worth checking: `ASTRNEST_ADMIN_*` (admin placeholder info), `VITE_SITE_NAME`, `SMTP_*` (can also be configured in the admin panel after installation).

**Step 2: Start the stack**

```bash
docker compose --env-file .env up -d
docker compose logs -f backend    # Ready once you see “Started ...”; Ctrl+C to exit
```

> The first start builds both images locally (pulling Maven/npm dependencies), which can take a few to a dozen-plus minutes; subsequent starts are fast. Containers: `mysql` (3306, bound to 127.0.0.1 only), `backend` (8081, bound to 127.0.0.1 only), `frontend` (80, public).

**Step 3: Open the site and complete the six-step install wizard**

Visit `http://your-server-ip/` (or your domain). Until installation completes, every page redirects to `/install`. The six steps:

1. **Environment Checks**: database connection and version (MySQL >= 8.0 recommended), table structure, local storage writability, Java runtime, FFmpeg (missing is a warning only, affecting video thumbnails). Fix any red blocking item first (usually the database), then hit "Re-check".
2. **Database Configuration**: choose "local" or "remote" database, fill in host/port/name/user/password and click **"Test Connection"** — success echoes the MySQL version and charset; failure shows a categorized reason, with an optional "try to create the database" checkbox.
   - Compose deployment: host is `mysql` (the service name), database `astrnest`, user `astrnest`, password = `MYSQL_PASSWORD` from your `.env`.
   - This step only confirms connectivity and never rewrites the runtime connection (which comes from `.env`); the wizard shows a warning banner if the form differs from the runtime connection.
3. **Initialize**: one-click table creation (runs the wizard-specific script `backend/db/install-schema.sql`, idempotent). On the Compose route the MySQL container already created the tables via `init.sql` on first start, so this step is skipped automatically; on manual deployments it actually creates them.
4. **Site Configuration**: everything is optional (defaults apply). Options: open email registration / registration email verification / login two-factor (TOTP) / guest uploads / per-file upload limit / asset acceleration domain. Note that "registration email verification" depends on SMTP readiness — better enabled after installation.
5. **Create Administrator**: pick a username and a **required email**, set a password — you can use the built-in generator for a 16-character strong password. **The plaintext is shown only once, copy and store it immediately**, then confirm it a second time. This is the single entry point for the initial account.
6. **Finish**: writes the anti-re-install lock and shows a summary (site URL / admin account / switch states / SMTP readiness), then brings you into the site.

**Step 4: First thing after installation — secure the site in the admin panel**

Log in as the administrator and go to **Admin → Security Settings** (`/admin/security-settings`):

1. **Configure SMTP**: first go to Admin → Mail Settings (`/admin/mail-settings`), fill in SMTP host/port/account/password-sender, turn the "enable" switch on, and **send a test mail** to verify delivery (see "[Post-Install Configuration](#post-install-configuration-security--mail)" below).
2. **Enable the two webmaster security switches as needed**: **registration email verification** (new sign-ups require an email code) and **login two-factor (TOTP)** (all logins require a dynamic code). The former requires working SMTP first.

**Step 5 (strongly recommended in production): front the stack with a host Nginx for image links**

The `frontend` container only proxies `/api/` — it does **not** serve `/upload/**`, so image direct links would land on the SPA page by default; the backend's 8081 is bound to 127.0.0.1 only. In production, add a host Nginx following the **Nginx sample** in "Manual Deployment" below: serve `/upload/` statically from the mounted `./storage/upload` directory on the host, and proxy pages and `/api/` to the container's port 80.

### Manual Deployment (Java 21 + MySQL 8 + Nginx)

**Requirements**: Java 21, MySQL 8.0+, Node.js 18+ (build time only), Nginx; optional FFmpeg (video thumbnails).

**1) Create the database and grant rights** (as root; or run the bundled `init-admin.py` / `init-admin.sh` / `init-admin-cn.bat` for an interactive walkthrough):

```sql
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'astrnest'@'%' IDENTIFIED BY 'your-strong-password';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';
FLUSH PRIVILEGES;
```

> When the backend runs on the same host, the connection may be identified as `localhost` (`'%'` does not match localhost); add a matching `'astrnest'@'localhost'` grant. For error 1044/42000 see CONFIG_GUIDE section 4.2.

**2) Build and start the backend**:

```bash
cd backend
./mvnw clean package              # Windows: .\mvnw.cmd
ASTRNEST_DB_URL='jdbc:mysql://127.0.0.1:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
ASTRNEST_DB_USERNAME=astrnest \
ASTRNEST_DB_PASSWORD='your-strong-password' \
ASTRNEST_STORAGE_ROOT=/var/lib/astrnest/upload \
ASTRNEST_JWT_SECRET="$(openssl rand -base64 64)" \
ASTRNEST_TRUSTED_PROXY=true \
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

- For production, manage it with systemd (put the variables in the unit's `Environment=`) and create the writable storage directory first: `mkdir -p /var/lib/astrnest/upload && chown -R astrnest:astrnest /var/lib/astrnest` — without write access the backend fails to start with `AccessDeniedException` (CONFIG_GUIDE 12.2.2).
- `ASTRNEST_TRUSTED_PROXY=true` only when behind a reverse proxy, so audit logs and rate limiting see real client IPs.

**3) Build the frontend**:

```bash
cd frontend
npm ci
npm run build        # output in dist/
```

Upload `dist/` to the server (example: `/var/www/astrnest/dist`). `.env.production` defaults to `VITE_API_BASE_URL=` (empty = same-origin proxy), so same-domain deployments need **no changes**; only split-domain deployments need the full API URL.

**4) Nginx sample (copy-paste ready; the `/upload/` block matters most)**

> **Why `/upload/**` should be served statically by Nginx**: image direct links are an image host's hottest traffic. Pointing them at the backend (`:8081/upload/**`) can return 500 on bare-metal deployments and keeps Java busy; the **official mitigation** is to let Nginx serve the storage directory directly — uploads still go through `/api/`, while all reads are handled by Nginx.

```nginx
server {
    listen 80;
    server_name imgbed.example.com;

    # Match the backend's ASTRNEST_MULTIPART_MAX_FILE_SIZE, or large uploads fail with 413 (0 = unlimited)
    client_max_body_size 100m;

    # ---- Frontend SPA ----
    root /var/www/astrnest/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # ---- Image direct links: static serving by Nginx (official recommendation) ----
    # Assuming ASTRNEST_STORAGE_ROOT=/var/lib/astrnest/upload:
    #   URL /upload/2026/09/x.jpg -> disk /var/lib/astrnest/upload/2026/09/x.jpg
    # root must be the PARENT directory of the storage root; ^~ keeps other regex locations away
    # Docker Compose: point root at the storage directory inside your repo clone (e.g. root /opt/AstrNest/storage;)
    location ^~ /upload/ {
        root /var/lib/astrnest;
        expires 30d;
        add_header Cache-Control "public" always;
        add_header X-Content-Type-Options "nosniff" always;
        # Same hardening as backend-served files: CSP sandbox on SVG prevents stored XSS
        location ~* \.svg$ {
            add_header Cache-Control "public" always;
            add_header X-Content-Type-Options "nosniff" always;
            add_header Content-Security-Policy "sandbox" always;
        }
    }

    # ---- Backend API (uploads included: relaxed timeouts for slow large uploads and long-lived/SSE-style responses) ----
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        proxy_request_buffering off;   # stream through instead of double-buffering large uploads on disk
    }

    # ---- Optional: Swagger / health check ----
    location /swagger-ui/     { proxy_pass http://127.0.0.1:8081; }
    location = /v3/api-docs   { proxy_pass http://127.0.0.1:8081; }
    location = /actuator/health { proxy_pass http://127.0.0.1:8081; }
}
```

For HTTPS/HTTP2 and security headers see CONFIG_GUIDE.md section 13.2; layer them onto the server block above with a 443 listener.

**5) Initialization and the administrator**: with the backend running against an empty database, open the site in a browser → the **six-step wizard** described above appears (here step 3 "Initialize" actually creates the tables; everything else is identical). Alternatively run `mysql -u root -p astrnest < backend/db/init.sql` and create the admin with an `init-admin` script; as a last resort, **the first user to register automatically becomes the administrator** (the init SQL ships no preset admin).

**6) Upgrades**: `git pull` → rebuild both ends → restart the backend and replace `dist/` → if the schema changed, re-run `backend/db/init.sql` (idempotent column additions, never destroys data).

### From Source to Production: the CI/CD Pipeline

The repository ships two GitHub Actions workflows; understanding them helps you replicate the same release checks on self-hosted CI (Gitea, Jenkins, etc.):

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `.github/workflows/ci.yml` | push to `main`/`develop`; PRs to `main` | ① `backend-test`: MySQL 8 service + JDK 21 (Temurin), runs `backend/mvnw test`; ② `frontend-build`: Node 18, `npm ci` + `npm run build`, uploads `dist/` as an artifact; ③ `docker-build`: on `main` only, after both pass, smoke-builds the backend and frontend Docker images (**pushes nowhere**) |
| `.github/workflows/deploy-docs.yml` | push to `main` touching `AstrNest-docs/**`, or manual (`workflow_dispatch`) | Builds the VitePress documentation site (`AstrNest-docs`) and publishes it to GitHub Pages |

**Tag release flow**: the repository currently has **no** tag → Release automation; tags only mark versions, while Releases and artifacts are published manually by the maintainers. If your fork needs automated releases, extend `ci.yml`'s `docker-build` with `docker/build-push-action` to push to your own registry, or add `softprops/action-gh-release` on tag pushes to attach the jar and `dist/`.

**Equivalent checks for self-hosted environments** (run before committing or deploying — same effect as CI):

```bash
cd backend && ./mvnw test                 # = backend-test; tests default to the in-memory H2 database, no local MySQL needed
cd frontend && npm ci && npm run build    # = frontend-build
docker compose --env-file .env build      # = docker-build (build only, no start)
```

### Post-Install Configuration (Security & Mail)

All of the following happens in the admin panel; do this right after every fresh install.

**SMTP mail service** (Admin → Mail Settings, `/admin/mail-settings`)

| Field | Description |
| --- | --- |
| SMTP server / port | e.g. `smtp.example.com`; common ports 465 (SSL) / 587 (TLS) / 25 (no encryption) |
| SMTP account / password | mailbox account and the provider's app-specific password (not your mailbox login password) |
| Encryption | `ssl` / `tls` / `none` |
| Sender email / name | the outgoing identity shown to recipients |

- Save with the "enable" switch on, then use **"Send test mail"** with your own address — only once this works should you enable registration email verification.
- **Readiness check** (backend semantics): the "enable" switch is on + SMTP host filled (not the `smtp.example.com` placeholder) + sender filled + password filled (not the `CHANGE_ME` placeholder). Missing any of these shows "SMTP not ready" on the Security Settings page and blocks the registration-verification switch.
- **Intranet no-auth relay / MailPit** (recommended for local testing): set the relay host, e.g. port 1025, encryption "none", and **any non-empty** account/password values (e.g. `mailpit` — relays don't check them), plus a sender address. That satisfies the readiness check and lets the test mail through. Docker users can add an [axllent/mailpit](https://github.com/axllent/mailpit) container and read mail in its web UI.

**Registration email verification switch** (Admin → Security Settings, `/admin/security-settings`)

- When on, new sign-ups must activate with an email verification code; whether self-registration is allowed at all is controlled separately by "open email registration" (off = admin-created accounts only).
- **Depends on SMTP**: the panel refuses to save the switch while SMTP is not ready; the same-named option in wizard step 4 behaves identically.

**Login two-factor switch (TOTP)** (same page)

- When on, **every user** is asked to bind/verify TOTP at their next login (any standard authenticator: Google Authenticator, Aegis, 1Password...):
  - Already-bound users: enter the **6-digit code** from the app; if the device is lost, use one of the **8-digit recovery codes** saved at bind time.
  - Not-yet-bound users (including existing users caught by the new switch): login enters a forced bind flow — scan the QR code (standard otpauth) → confirm with a 6-digit code → the site shows **10 one-time recovery codes, displayed once only; store them safely**.
- **Authenticator and recovery codes both lost**: an administrator triggers "reset two-factor" for that user in member management (`/admin/users`, `PUT /api/admin/users/{id}/2fa/reset`); the user re-binds at the next login.

### Development Setup (source code)

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

> If you want to see configuration details directly (environment variables, file paths, initialization scripts, FFmpeg, storage switching, etc.), please jump to `CONFIG_GUIDE.md`.

### Install Wizard

AstrNest ships a **WordPress-style visual install wizard**; the six steps (Environment Checks → Database Configuration → Initialize → Site Configuration → Create Administrator → Finish) are described step by step in "Five-Minute Deployment, Step 3" above. How the two deployment routes differ inside the wizard:

| Deployment Path | Database Schema | Initial Administrator |
| --- | --- | --- |
| **Docker Compose** (mounts `init.sql` for auto-initialization) | Created by `backend/db/init.sql` on the MySQL container's first start; the wizard's "Initialize" step is skipped automatically | Created in wizard step 5 (recommended), or the first registered user becomes ADMIN |
| **Manual / hosting-panel / bare-jar deployment** (init SQL never executed) | Wizard step 3 creates all tables (`install-schema.sql`, idempotent) | Created in wizard step 5 (recommended), or the first registered user becomes ADMIN |

- Wizard URL: `http://your-domain.com/install` (until installation completes, every page redirects to the wizard).
- **Re-visiting `/install` after installation** shows "System already installed" and refuses to re-initialize; the `/api/install` write endpoints always return 403 once installed, preventing replay calls.
- **Stuck mid-install**: the wizard offers a "reset install state" action (`POST /api/install/reset`), available only for unfinished sites (users table missing or empty). See the FAQ below.
- Note: the wizard does not write database connection settings — the Spring Boot application must already be able to connect to MySQL (via `ASTRNEST_DB_URL` / `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD`, etc.) before it can start; the wizard handles **table creation and the initial account**. See CONFIG_GUIDE.md section 4.3 for check-item meanings and troubleshooting.

### Production Notes
- **`ASTRNEST_TRUSTED_PROXY`** (default `false`): set to `true` when the backend runs behind a reverse proxy (Nginx etc.), so the backend trusts and parses `X-Real-IP` / `X-Forwarded-For` and audit logs / rate limiting see the real client IP; config key: `astrnest.security.trusted-proxy`.
- **`astrnest.jwt.secret`**: JWT signing key; must be explicitly configured in production (`ASTRNEST_JWT_SECRET`), otherwise a temporary key is generated on every restart and everyone is logged out. Token lifetime is controlled by `chenxi.passport.access-token-days` (default: 30 days of inactivity).
- **Image direct links**: in production serve `/upload/**` statically from Nginx (sample above under Manual Deployment) and point "System Config → asset acceleration domain (`asset_domain`)" or the frontend `VITE_PUBLIC_ASSET_BASE` at that domain.
- **Nginx `client_max_body_size`**: keep it in sync with `spring.servlet.multipart.max-file-size`, otherwise large uploads fail with 413.
- **Docker Compose port binding**: the database and backend ports in `docker-compose.yml` are bound to `127.0.0.1` only; expose the service through a reverse proxy in production instead of publishing these ports directly.
- **SSO single sign-on**: see "SSO Single Sign-On (External Identity Provider)" below.

### SSO Single Sign-On (External Identity Provider, disabled by default)

AstrNest supports unified login through external identity providers using **OAuth 2.1 / OIDC "authorization code + PKCE(S256)"**, compatible with "Chenxi Passport" and any standard OAuth 2.1/OIDC provider (example issuer: `https://passport.example.com`). The feature is **disabled by default** (`chenxi.passport.enabled=false`); when disabled the frontend hides the entry and backend endpoints return explicit errors, with zero impact on existing local login/registration/API keys. See `docs/chenxi-integration.md` for details.

- **Login flow**: the frontend redirects to the provider's authorize endpoint (PKCE) → the provider calls back `/auth/sso/callback` → the frontend submits `code + codeVerifier` to `POST /api/auth/sso/exchange` (**server-side exchange**: the backend performs the PKCE token swap, the browser never touches the provider's token) → the backend introspects the result against the issuer (`{issuer}/oauth2/introspect`) and issues a local JWT identical to a local login response.
- **Shadow account**: the first SSO login creates a shadow account (`identity_source=passport`, linked via `sso_sub`); nickname/avatar/email are synced from the identity provider on each login and are **read-only** locally (profile and password changes are rejected).
- **Register the callback URL on the provider side**: `https://<your-domain>/auth/sso/callback`.

| Config key | Env variable | Default | Description |
| --- | --- | --- | --- |
| `chenxi.passport.enabled` | `CHENXI_PASSPORT_ENABLED` | `false` | Enable SSO login |
| `chenxi.passport.issuer` | `CHENXI_PASSPORT_ISSUER` | empty | Issuer base URL, e.g. `https://passport.example.com` |
| `chenxi.passport.client-id` | `CHENXI_PASSPORT_CLIENT_ID` | empty | Public client id registered at the provider |
| `chenxi.passport.redirect-uri` | `CHENXI_PASSPORT_REDIRECT_URI` | empty | Callback URL, must match the provider registration exactly |
| `chenxi.passport.scopes` | `CHENXI_PASSPORT_SCOPES` | `openid,profile` | Requested scopes |
| `chenxi.passport.access-token-days` | `CHENXI_PASSPORT_ACCESS_TOKEN_DAYS` | `30` | Local token inactivity lifetime (sliding refresh) |

## FAQ

### Install wizard (won't install / locked / want a reinstall)

- **Wizard won't open, keeps saying "cannot connect to server"**: the backend isn't running. Check the backend logs first — when the database is unreachable Spring Boot cannot start and there is no wizard at all. The wizard requires "app started, database empty".
- **Stuck mid-install, want to start over**: the wizard page offers a **"reset install state"** action (`POST /api/install/reset`) that clears `install.lock` and the database completion marker and returns to step one; available only for unfinished sites (users table missing or empty).
- **Already installed, want a reinstall**: two steps — ① empty the database (drop and recreate, or truncate all business tables); ② delete the anti-re-install lock `install.lock` (in the parent directory of the storage root, `/storage/install.lock` by default; with Docker Compose the file lives inside the backend container, so `docker compose down` followed by `up -d` removes it — keep or drop the `mysql_data` volume as you see fit). Restart and reopen `/install`.
- **Visiting `/install` after installation**: shows "System already installed" and refuses to re-initialize; the `/api/install` write endpoints always return 403.

### Image direct links return 404 or 500

- **Direct link returns 500 (bare metal / hosting panel running the jar, link hits `:8081/upload/**`)**: the official mitigation is to serve `/upload/**` statically from **Nginx** (see the `location ^~ /upload/` sample under Manual Deployment above), moving read traffic off the backend. Note that `root` must point at the **parent** directory of `ASTRNEST_STORAGE_ROOT` (`root + /upload/...` forms the full disk path) and Nginx needs read permission there.
- **Direct link 404 or returns a web page (Docker Compose, link hits port 80)**: the `frontend` container's Nginx only proxies `/api/` and does **not** serve `/upload/**`. Fix it the same way — host Nginx serving the mounted `./storage/upload` directory — or point "System Config → asset acceleration domain (`asset_domain`)" and the frontend `VITE_PUBLIC_ASSET_BASE` at a domain/CDN that actually serves the files.
- **Checklist**: the file really exists under `ASTRNEST_STORAGE_ROOT/{yyyy}/{MM}/`; the link domain matches `asset_domain` / `VITE_PUBLIC_ASSET_BASE`; on Compose, verify `ls ./storage/upload` shows the files on the host.

### Verification emails never arrive

1. First click **"Send test mail"** in Admin → Mail Settings: if that fails, SMTP itself is broken — verify server/port/encryption/app-password (the app password is not your mailbox login password) and check the backend logs for mail/SMTP errors.
2. The verification switch is on but no codes are sent: check Admin → Security Settings for **SMTP not ready** — the "enable" switch may be off, or host/sender/password still hold the `smtp.example.com` / `CHANGE_ME` placeholders.
3. Intranet / local environments: cloud providers often block port 25 and public SMTP may be throttled; switch to 465/587 or use an intranet relay / MailPit (see Post-Install Configuration).
4. Test mail arrives but codes don't: check the spam folder and the recipient address spelling; codes expire, request a new one.

### Other common issues

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
