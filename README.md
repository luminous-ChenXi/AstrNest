# AstrNest 媒体管理系统
<div>

现代全栈媒体管理平台，基于 Spring Boot 3.4.1 和 Vue 3 构建。多云存储、AI内容审查、完整API生态。

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
  简体中文 · <a href="./README_EN.md">English</a>
</p>
<p align="center">
  <a href="#quick-start--快速开始">快速开始</a> · 
  <a href="https://luminouschenxi.com">博客</a> · 
  <a href="#tech-stack">技术栈</a> · 
  <a href="#acknowledgments">致谢</a> ·   
  <a href="https://discord.gg/hBsqcfwC9Q">Discord</a> · 
  <a href="./backend">后端部分</a>
</p>

</div>

> 🪺 **辰汐生态（Chenxi Ecosystem）** 成员项目 —— 与 [LuomiNest](https://github.com/LuminousCX/LuomiNest)（桌面 AI 陪伴）、[LuomiBlog](https://github.com/luminous-ChenXi/LuomiBlog)（AI 知识库博客）、Teachenxi（学习陪伴 App）同属辰汐生态。本项目的角色：开源图床与媒体管理平台（多云存储、AI 内容审查）。

<img src="./templates/1728x2304.png" width = "300" height = "400" alt="AstrNest" align=right />
<div align="center">

# AstrNest

_Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3._

 唯有青春与梦想不可辜负~！

</div>

---

## Core Features | 核心特性

- **现代化架构**: Spring Boot 3.4.1 + Vue 3 + Vite 5 全栈技术栈
- **多角色权限**: 支持管理员、普通用户等多级权限管理体系
- **多云存储**: 内置 Local、阿里云 OSS、腾讯 COS、七牛 Kodo、华为 OBS、金山 KS3、又拍云 USS、OneDrive/SharePoint 以及通用 S3 驱动，可在配置中一键切换；S3 兼容驱动默认 5GB 阈值触发 25MB 分片上传，支持 CDN/CNAME、加速域、PathStyle 以及预签名直传凭证
- **智能内容审查**: 接入腾讯云数据万象（COS CI）图片审核与标签服务，自动回填 AI 决策/标签/RequestId，并结合错误码文档输出友好提示，配合人工复核双重保障
- **完整的API生态**: RESTful API + API密钥认证 + Web管理界面
- **灵活的存储方案**: 支持本地存储与云对象存储（OSS/COS）
- **安全管理**: Spring Security 6 + JWT认证 + 内容安全策略
- **成员治理与配额**: 管理端“成员列表”支持查看头像/配额/点赞总数，并可一键调整每日上传与总存储额度、切换管理员/用户/游客角色
- **响应式设计**: 现代化UI组件库，支持PC端、移动端、平板端等多端适配
- **实时监控**: 系统运行状态监控与操作日志审计
- **邮件服务**: 集成邮件模板与验证码发送功能，默认预置阿里云邮局 SMTP
## License Warning | GPL-3.0 with Additional Terms (Non-Commercial) 协议警示
### ⚠️ **重要许可提醒** ⚠️

**本项目采用 GNU General Public License v3 (GPL-3.0) 协议开源，并追加非商用附加条款（禁商用 + 署名）；与 GPL-3.0 冲突处以附加条款为准，全文见 [LICENSE](LICENSE)**

### 四条红线：
- ✅ **自由部署**：按本协议部署、运行本软件无需另行许可
- ✅ **自由修改发布**：可自由修改、发布，但衍生作品必须同样以 "GPL-3.0 + 本附加条款" 开源，分发时提供源代码
- ❌ **禁止商用**：未经版权人事先书面授权，不得将本软件或其衍生作品用于任何商业用途（包括但不限于出售、付费服务/SaaS 收费、商业产品捆绑、广告变现）
- ❌ **必须署名**：衍生作品或引用本软件代码的作品，必须保留原始版权声明与本协议全文，并在显著位置（关于页/README/文档）注明原始项目名称与仓库地址

### 对不良开发人员的警告：
**请注意：以下行为将构成侵权并可能面临法律责任：**
- ❌ 私自修改协议或移除版权声明
- ❌ 将代码用于任何商业用途（含闭源商业产品与商业服务）
- ❌ 声称代码为自己原创
- ❌ 绕过本协议要求分发衍生作品

**此项目开发周期3个月，开发时间较长，由辰汐（ChenXi）自主开发，其开发过程艰辛；违反协议将面临法律追责，请尊重开源精神！**

## Tech Stack | 技术栈

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

## System Architecture | 系统架构

### 架构图
AstrNest 采用前后端分离架构：

- **前端**: Vue 3 + Vite + Element Plus + Tailwind CSS
- **后端**: Spring Boot 3 + Spring Security + JPA + MySQL
- **认证**: JWT Token + API Key 双重认证机制
- **存储**: 本地存储 + 云对象存储扩展支持
- **安全**: 基于角色的权限控制 + 内容安全策略

### 项目结构

```
astrnest/
├─ backend/      # Spring Boot 服务：REST API、鉴权、内容审查、API 密钥管理
├─ frontend/     # Vue 3 + Vite 单页应用：仪表盘、上传中心、安全控制台、API 集成
└─ storage/      # （运行时生成）本地存储目录，可通过配置改为 OSS/COS
```

## API Documentation | API 接口文档概览

### Swagger / OpenAPI
- 在线文档入口：`/swagger-ui/index.html`
- OpenAPI JSON：`/v3/api-docs`
- 认证方式：登录后获取的 Token 放入 `Authorization: Bearer <token>`，部分接口需管理员权限。

### 认证契约 | Authentication Contract
- **JWT（推荐）**：调用 `POST /api/auth/login` 登录成功后返回 JWT，后续请求携带 `Authorization: Bearer <token>`。
  - Token 默认 **30 天不活动过期**（活跃使用自动滑动续期），可通过 `chenxi.passport.access-token-days`（环境变量 `CHENXI_PASSPORT_ACCESS_TOKEN_DAYS`）调整。
  - 生产环境必须配置签名密钥 `astrnest.jwt.secret`（环境变量 `ASTRNEST_JWT_SECRET`，请使用随机长字符串并妥善保管，勿提交仓库）。
- **HTTP Basic（兼容保留）**：仍可用于 API 插件/脚本调用（如 Typora、PicGo 等自定义上传插件），与 API Key 认证并行支持。

## Quick Start | 快速开始

无论哪条路线，装完都会进入同一个**可视化安装向导**完成建表与初始管理员创建。按你的场景二选一：

| 路线 | 适合谁 | 特点 |
| --- | --- | --- |
| **[五分钟部署（Docker Compose）](#五分钟部署docker-compose)** | 有台服务器、装了 Docker 的站长 | 三条命令起全套（MySQL/后端/前端），向导收尾 |
| **[手动部署（Jar + Nginx）](#手动部署java-21--mysql-8--nginx)** | 宝塔 / 裸机 / 已有 MySQL 与 Nginx 的站长 | 全程掌控，Nginx 静态直服图片直链（生产推荐） |

> **文档分工**：本节 = 快速上手与部署路线；**[CONFIG_GUIDE.md](CONFIG_GUIDE.md)** = 全量配置参考（环境变量逐项、文件路径、数据库初始化、对象存储切换、Windows 专项排障、Nginx/CDN 进阶）。下文所有命令均已与仓库实际内容核对：Maven Wrapper 位于 `backend/mvnw`（Windows 用 `mvnw.cmd`），Compose 服务名为 `mysql` / `backend` / `frontend`，后端端口 `8081`、前端容器端口 `80`。

### 五分钟部署（Docker Compose）

**准备**：一台 2C4G 起步的服务器；安装 Docker 20.10+ 与 compose v2 插件；防火墙放行 80/443。

**第 1 步：克隆仓库并配置环境变量**

```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest
cp .env.example .env
vi .env
```

`.env` 必改项（密码类变量缺失时 `docker compose` 会直接报错拒绝启动，防止带着弱口令上线）：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | MySQL root 密码与应用账号密码 |
| `ASTRNEST_DB_PASSWORD` | 后端连库密码，**保持与 `MYSQL_PASSWORD` 一致** |
| `ASTRNEST_JWT_SECRET` | 生产必须：用 `openssl rand -base64 64` 生成随机长字符串。未配置时后端每次重启生成临时密钥，**重启后所有人需重新登录**。该变量不在 compose 模板显式透传之列，但 `env_file` 会把 `.env` 全量注入容器，直接写入 `.env` 即可生效 |
| `PUBLIC_SITE_URL` / `PUBLIC_ASSET_URL` / `BACKEND_API_PUBLIC_URL` | 换成你的域名，如 `https://img.example.com` 与 `https://img.example.com/upload` |

建议顺手核对：`ASTRNEST_ADMIN_*`（管理员占位信息）、`VITE_SITE_NAME`、`SMTP_*`（也可装完后在管理后台填）。

**第 2 步：启动全套服务**

```bash
docker compose --env-file .env up -d
docker compose logs -f backend    # 看到 “Started ...” 即就绪，Ctrl+C 退出
```

> 首次启动会在本机构建前后端镜像（拉取 Maven/npm 依赖），可能需要几分钟到十几分钟，之后很快。容器分工：`mysql`（3306，仅绑 127.0.0.1）、`backend`（8081，仅绑 127.0.0.1）、`frontend`（80，对外）。

**第 3 步：浏览器打开站点，走完六步安装向导**

访问 `http://服务器IP/`（或你的域名），未安装时任意页面都会自动跳转到 `/install`。六步流程：

1. **环境检测**：自动检查数据库连接与版本（推荐 MySQL >= 8.0）、数据表结构、本地存储目录可写性、Java 运行时、FFmpeg（缺失仅警告，只影响视频缩略图）。红色致命项需先修复（通常是数据库连不上），可点「重新检测」。
2. **数据库配置**：选择「本机数据库」或「远程数据库」，填写主机/端口/库名/账号/密码后点**「测试连接」**——成功回显 MySQL 版本与字符集，失败给出分类原因，可勾选「尝试创建数据库」后重试。
   - Compose 部署：主机填 `mysql`（容器服务名），库名 `astrnest`、账号 `astrnest`、密码即 `.env` 里的 `MYSQL_PASSWORD`。
   - 这一步只做连接确认，不回写运行时连接串（运行时连库以 `.env` 为准）；若表单与运行时连接串不一致，向导会黄条提醒。
3. **初始化**：一键建表（执行向导专用脚本 `backend/db/install-schema.sql`，幂等，重复执行不破坏已有数据）。Compose 路线的 MySQL 容器首启已由 `init.sql` 建好表，此步自动跳过；手动路线则在此真正建表。
4. **站点配置**：可全部跳过（保持系统默认）。可选项：开放邮箱注册 / 注册邮箱验证 / 登录二步验证（TOTP）/ 允许访客上传 / 单文件上传上限 / 资源加速域名。注意「注册邮箱验证」依赖 SMTP 就绪，建议装完后再开。
5. **创建管理员**：填写用户名与**必填邮箱**，设置密码——可用内置生成器生成 16 位强密码，**明文只在此一次性展示，请立即复制保存**，并按要求二次输入确认。这是系统的唯一初始账号入口。
6. **完成**：写入防重装锁并展示汇总（站点地址 / 管理员账号 / 开关状态 / SMTP 是否就绪），随后进入站点。

**第 4 步：装完第一件事——进管理端配好安全项**

用管理员登录后，到 **管理端 → 安全设置**（`/admin/security-settings`）：

1. **配置 SMTP**：先到 管理端 → 邮件设置（`/admin/mail-settings`）填好 SMTP 主机/端口/账号/授权码/发件人，打开「启用」开关，并**发送测试邮件**确认收得到（详见下文「[安装后配置](#安装后配置安全与邮件)」）。
2. **按需打开两个站长安全开关**：**注册邮箱验证**（新注册需邮箱验证码激活）与**登录二步验证（TOTP）**（全员登录需动态码）。前者必须先配好 SMTP。

**第 5 步（生产强烈建议）：最外层架宿主 Nginx 处理图片直链**

Compose 的 `frontend` 容器只反代 `/api/`，**不托管 `/upload/**`**，直链图片默认会落到 SPA 页面；`backend` 的 8081 又只绑 127.0.0.1 不对外。生产环境请按下面「手动部署」的 **Nginx 样例**加一层宿主 Nginx：`/upload/` 静态直服宿主机 `./storage/upload` 挂载目录，页面与 `/api/` 反代到容器 80 端口。

### 手动部署（Java 21 + MySQL 8 + Nginx）

**环境要求**：Java 21、MySQL 8.0+、Node.js 18+（仅构建前端用）、Nginx；可选 FFmpeg（视频缩略图）。

**1) 建库与授权**（root 执行；或直接运行仓库根目录的 `init-admin.py` / `init-admin.sh` / `init-admin-cn.bat` 交互式完成建库+管理员）：

```sql
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'astrnest'@'%' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';
FLUSH PRIVILEGES;
```

> 后端与 MySQL 同机时连接可能被识别为 `localhost`（`'%'` 不含 localhost），需再补 `'astrnest'@'localhost'` 同样授权；报错 1044/42000 的完整排查见 CONFIG_GUIDE 4.2 节。

**2) 构建并启动后端**：

```bash
cd backend
./mvnw clean package              # Windows 用 .\mvnw.cmd
ASTRNEST_DB_URL='jdbc:mysql://127.0.0.1:3306/astrnest?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
ASTRNEST_DB_USERNAME=astrnest \
ASTRNEST_DB_PASSWORD='你的强密码' \
ASTRNEST_STORAGE_ROOT=/var/lib/astrnest/upload \
ASTRNEST_JWT_SECRET="$(openssl rand -base64 64)" \
ASTRNEST_TRUSTED_PROXY=true \
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

- 生产建议用 systemd 托管（上述变量写进 unit 的 `Environment=`），并提前建好可写存储目录：`mkdir -p /var/lib/astrnest/upload && chown -R astrnest:astrnest /var/lib/astrnest`——进程对存储目录无写权限会报 `AccessDeniedException` 起不来（CONFIG_GUIDE 12.2.2）。
- `ASTRNEST_TRUSTED_PROXY=true` 仅在 Nginx 反代之后开启，后端才能记录真实客户端 IP（限流/审计依赖它）。

**3) 构建前端**：

```bash
cd frontend
npm ci
npm run build        # 产物在 dist/
```

把 `dist/` 上传到服务器（示例 `/var/www/astrnest/dist`）。`.env.production` 默认 `VITE_API_BASE_URL=`（空 = 同源反代），前后端同域名部署**无需改动**；分域名部署才改成 API 完整地址。

**4) Nginx 样例（可直接复制；重点看 `/upload/` 静态直服）**

> **为什么 `/upload/**` 要交给 Nginx 静态直服**：图片直链是图床最高频的流量。让直链打到后端（`:8081/upload/**`）在裸机直跑场景下可能返回 500，且长期占用 Java 进程资源；**官方规避方案**就是让 Nginx 直接服务存储目录——上传仍走 `/api/`，读取全部由 Nginx 承担。

```nginx
server {
    listen 80;
    server_name imgbed.example.com;

    # 与后端 ASTRNEST_MULTIPART_MAX_FILE_SIZE 匹配，否则大图上传被 Nginx 拦成 413（设 0 为不限制）
    client_max_body_size 100m;

    # ---- 前端 SPA ----
    root /var/www/astrnest/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # ---- 图片直链：Nginx 静态直服（官方推荐）----
    # 约定 ASTRNEST_STORAGE_ROOT=/var/lib/astrnest/upload 时：
    #   URL /upload/2026/09/x.jpg -> 磁盘 /var/lib/astrnest/upload/2026/09/x.jpg
    # 注意 root 填存储根目录的【父目录】；^~ 防止被下方其他正则 location 抢走
    # Docker Compose 场景：root 改为宿主机仓库下的 storage 目录（如 root /opt/AstrNest/storage;）
    location ^~ /upload/ {
        root /var/lib/astrnest;
        expires 30d;
        add_header Cache-Control "public" always;
        add_header X-Content-Type-Options "nosniff" always;
        # SVG 与后端直出同口径：追加 CSP sandbox 防存储型 XSS
        location ~* \.svg$ {
            add_header Cache-Control "public" always;
            add_header X-Content-Type-Options "nosniff" always;
            add_header Content-Security-Policy "sandbox" always;
        }
    }

    # ---- 后端 API（上传也走这里：放宽超时，兼容慢速大上传与长连接/SSE 类响应）----
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        proxy_request_buffering off;   # 边收边转发，避免大文件在 Nginx 落盘双份
    }

    # ---- 可选：Swagger / 健康检查 ----
    location /swagger-ui/     { proxy_pass http://127.0.0.1:8081; }
    location = /v3/api-docs   { proxy_pass http://127.0.0.1:8081; }
    location = /actuator/health { proxy_pass http://127.0.0.1:8081; }
}
```

HTTPS/HTTP2 与安全响应头样例见 [CONFIG_GUIDE.md](CONFIG_GUIDE.md) 第 13.2 节，在上述 server 块基础上叠加 443 监听即可。

**5) 初始化与管理员**：后端起来后（空库）浏览器打开站点 → 自动进入上文**六步安装向导**（本路线第 3 步「初始化」会真正建表，其余步骤相同）。不习惯向导也可以 `mysql -u root -p astrnest < backend/db/init.sql` 建表后用 `init-admin` 脚本创建管理员；兜底：**第一个完成注册的用户自动成为管理员**（初始化 SQL 不再预置任何管理员）。

**6) 升级**：`git pull` → 重新构建前后端 → 重启后端并覆盖 `dist/` → 如表结构有变更，重复执行 `backend/db/init.sql`（幂等补列，不破坏已有数据）。

> 进阶部署：管理前端也可以打包上传到腾讯云 COS、经 CDN 分发，与后端同域按路径分流——见下文「[管理前端 CDN 部署（COS + CDN）](#管理前端-cdn-部署cos--cdn)」。

### 从源码到上线：CI/CD 链路

仓库自带两条 GitHub Actions 工作流，理解它们也方便站长在自建环境（Gitea/Jenkins 等）复刻同等的发布校验：

| 工作流 | 触发时机 | 干什么 |
| --- | --- | --- |
| `.github/workflows/ci.yml` | push 到 `main`/`develop`；PR 到 `main` | ① `backend-test`：起 MySQL 8 service + JDK 21（Temurin），跑 `backend/mvnw test`；② `frontend-build`：Node 18，`npm ci` + `npm run build` 并上传 `dist/` 产物；③ `docker-build`：仅 `main` 分支、前两项通过后，冒烟构建前后端 Docker 镜像（**不推送**任何 registry） |
| `.github/workflows/deploy-docs.yml` | `AstrNest-docs/**` 变更 push 到 `main`，或手动触发（workflow_dispatch） | VitePress 构建文档站（`AstrNest-docs`）并发布到 GitHub Pages |

**tag 发版流程**：仓库目前**没有** tag → Release 的自动工作流；打 tag 仅作版本标记，Release 与产物上传由维护者手动完成。若你 fork 后需要自动发版，可在 `ci.yml` 的 `docker-build` 基础上加 `docker/build-push-action` 推送到自己的镜像仓库，或用 `softprops/action-gh-release` 在 tag push 时附上 jar 与 `dist/`。

**站长自建环境的等价校验**（提交代码前或部署前跑一遍，与 CI 同效果）：

```bash
cd backend && ./mvnw test                 # 等价 backend-test；测试默认走 H2 内存库，本地无需装 MySQL
cd frontend && npm ci && npm run build    # 等价 frontend-build
docker compose --env-file .env build      # 等价 docker-build（只构建不启动）
```

### 管理前端 CDN 部署（COS + CDN）

**适用性结论**：AstrNest 前端是纯 SPA（Vue 3 + Vite，无 SSR），构建产物只有 `index.html` + 带内容 hash 的 `assets/`，**完全适用「COS 静态托管 + CDN 分发」**。本节给出与「[手动部署](#手动部署java-21--mysql-8--nginx)」互补的进阶路线：管理前端打包上传腾讯云 COS 桶，CDN 与 Java 后端**同域名、按路径分流**。流量不大的站点用上文两条默认路线（Nginx 直服 `dist/`）即可，本节按需选用。

> 本节只是部署方案，不改变仓库默认行为：`frontend/` 源码仅有「路由 base」一处一行级适配点（见第 3 步），其余全部通过构建环境变量与云端配置完成。

#### 1) 总体架构（文字版）

```text
用户浏览器
   │  同一域名（如 https://img.example.com）
   ▼
腾讯云 CDN（边缘节点，挂 HTTPS 证书）
   │  按 URL 路径分流
   ├─ /admin/*   → 回源 COS 桶（管理前端 dist：index.html + assets/，纯静态对象）
   ├─ /api/*     → 回源源站 Nginx → 127.0.0.1:8081（Java 后端；CDN 不缓存）
   ├─ /upload/*  → 回源源站 Nginx（图片直链静态直服，可长缓存）
   └─ 其余路径    → 源站 Nginx（Swagger / actuator 等，同「手动部署」样例）
```

关键点：**同域不同路径**。`/admin/*` 与 `/api/*` 共用一个域名，浏览器视角完全同源——`VITE_API_BASE_URL` 保持留空（同源相对路径）即可，没有 CORS 问题，登录态（JWT）行为与现在完全一致。

#### 2) 路径分流的两层做法

**做法 A（推荐先跑通）：CDN 只挂一个源站 = 源站 Nginx，分流在 Nginx 完成。**

```nginx
# 源站 Nginx：CDN 的唯一回源地址（在「手动部署」第 4 步的 server 块中追加）
server {
    listen 80;
    server_name imgbed.example.com;
    client_max_body_size 100m;

    # /api/* → Java 后端（CDN 侧对 /api/* 配置不缓存）
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        proxy_request_buffering off;
    }

    # /upload/* → 图片直链静态直服（同「手动部署」第 4 步样例，此处省略）

    # /admin/* → COS 桶（桶开「静态网站」，走静态网站端点，自带索引/错误文档能力）
    location ^~ /admin/ {
        proxy_pass http://astrnest-admin-1250000000.cos-website.ap-guangzhou.myqcloud.com;
        proxy_set_header Host astrnest-admin-1250000000.cos-website.ap-guangzhou.myqcloud.com;
        proxy_intercept_errors on;
        error_page 404 /admin/index.html;    # SPA 路由回退（见第 5 小节）
    }
}
```

**做法 B（进阶）：腾讯云 CDN 控制台「规则引擎」按路径直连不同源站**——`/admin/*` → COS 源站（选 COS 源并开启「回源鉴权」可支持私有读桶），`/api/*` 与其余路径 → 源站 Nginx。配置要点：

- 规则自上而下匹配，`/admin/*`、`/api/*` 必须放在通配兜底规则**之前**；
- 每条规则的「回源 Host」要与对应源站匹配（COS 源站填桶端点 Host）；
- 走 COS 回源鉴权（私有读桶）时静态网站端点不可用，SPA 回退改由 CDN 的 404 错误页/错误码处理承担（具体功能以控制台实际可见项为准）。

#### 3) 构建与上传

**base 路径**：`frontend/vite.config.js` 已内置 CDN 支持——`base: process.env.VITE_ASSETS_BASE_URL || '/'`（文件内注释即为此场景而写）。同域子路径方案设为 `/admin/`（**末尾必须带斜杠**），构建后 `index.html` 引用的资源变为 `/admin/assets/[name]-[hash].js`，正好落在 `/admin/*` 分流规则内：

```bash
cd frontend
VITE_ASSETS_BASE_URL=/admin/ npm run build
```

**路由适配（唯一一处一行改动）**：`src/router/index.js` 目前是 `createWebHistory()`（默认 base `/`）。子路径部署时，登录守卫会把未登录访问重定向到 `/`——这个 URL 不在 `/admin/*` 内，会在 CDN 上 404。请改成标准 Vite 写法，让路由 base 跟随构建 base：

```js
history: createWebHistory(import.meta.env.BASE_URL)
```

改完后 SPA 内所有跳转（`/login`、`/gallery`、`/admin/*`……）的实际 URL 都带 `/admin` 前缀，全部命中分流规则。API 侧无需任何改动：`VITE_API_BASE_URL` 保持留空，请求仍走同域 `/api/*`。

> 注意：`public/` 目录的文件会原样复制进 `dist/`，若代码里以根绝对路径（如 `/images/...`）引用它们，Vite 的 base **不会**重写这类运行时字符串——要么把引用改为 `import.meta.env.BASE_URL` 拼接，要么为该前缀单独加一条回源 COS 的分流规则。

**上传与发版脚本**（示例用腾讯云 COSCLI `coscli`，括号内附旧版 `coscmd` 等价命令；桶名/地域请替换）：

```bash
#!/usr/bin/env bash
set -euo pipefail
BUCKET=astrnest-admin-1250000000           # 桶名（含 APPID）
cd frontend

# 1) 构建（base 指向 /admin/）
VITE_ASSETS_BASE_URL=/admin/ npm run build

# 2) 先同步带 hash 的静态资源（增量，只传新增；旧版本保留在桶里天然支持回滚）
coscli sync dist/assets/ cos://$BUCKET/admin/assets/
coscli sync dist/images/ cos://$BUCKET/admin/images/

# 3) 入口 HTML 最后传：新资源全部就位后再覆盖 index.html，即完成原子切换
coscli cp dist/index.html cos://$BUCKET/admin/index.html     # coscmd: coscmd put dist/index.html admin/index.html

# 4) 刷新 CDN：控制台「缓存刷新 → 目录刷新」填 https://你的域名/admin/
#    （或调 API：目录刷新 PurgePathsCache / URL 刷新 PurgeUrlsCache）
```

> 顺序即原子性：assets 先行（新增对象不影响旧页面）→ `index.html` 最后覆盖 → 刷 CDN。回滚 = 把桶内 `admin/index.html` 换回旧内容（旧 assets 仍在桶中，直接可用）。

#### 4) 缓存策略

| 对象 | Cache-Control | 说明 |
| --- | --- | --- |
| `admin/index.html`（入口与 SPA 回退页） | `no-cache`（或 `max-age=60`） | 发版立即生效的「开关」，每次都向源校验 |
| `/admin/assets/*`（文件名带内容 hash） | `public, max-age=31536000, immutable` | 内容不变，缓存一年也不怕 |
| `/admin/images/*` 等公共静态资源 | `public, max-age=604800` | 低频变更 |
| `/api/*` | 不缓存（CDN 缓存规则排除该路径） | 接口实时性 |
| `/upload/*` 图片直链 | `public, max-age=2592000` | 直链内容不可变，CDN 加速收益最大 |

策略可通过 COS 对象元数据（`Cache-Control`）或 CDN「缓存规则」配置，二选一（CDN 侧优先级更高、便于统一调整）。**每次发版都必须刷新 CDN**（至少刷新 `/admin/` 目录），否则边缘节点可能继续返回旧 `index.html`。

#### 5) SPA 路由回退（history 模式 404 问题）

vue-router 是 history 模式，`/admin/dashboard`、`/admin/users` 这类路由没有对应实体对象，直接访问会 404。三种做法三选一：

- **COS 静态网站错误页**：桶开启「静态网站」→ 错误文档设为 `admin/index.html`（走静态网站端点时最省事，做法 A 的 Nginx 样例已顺带生效）；
- **源站 Nginx**：`proxy_intercept_errors on;` + `error_page 404 /admin/index.html;`（见做法 A 样例）；
- **腾讯云 CDN**：控制台「高级配置 → 自定义错误页」把 404 指向 `https://同域名/admin/index.html`（或用规则引擎的错误码处理，以控制台实际功能为准）。

#### 6) 安全注意

- **桶权限二选一**：① 私有读 + CDN 回源鉴权（配合做法 B 的 COS 源站鉴权，此时静态网站端点不可用）；② 公有读 + 防盗链（Referer 黑白名单），并保持默认关闭「列举对象」权限（静态网站端点只能按 key 读取具体对象，列不了目录）。**切勿公有读写**。
- **HTTPS**：CDN 加速域名挂证书（免费证书或自有证书）并开启强制 HTTPS；回源可保持 HTTP 或开启「回源跟随协议」。
- **同域的安全性质**：管理前端与 `/api` 同域，没有 CORS 配置面；但 JWT 存 localStorage 的 **XSS 窃取面并不因换部署方式而消失**——本方案的收益主要是静态资源供应链完全自主可控，管理端仍建议保持严格 CSP 与最小权限 API Key（见 [SECURITY.md](SECURITY.md)）。
- Vite 生产构建已自动移除 `console`/`debugger`（`vite.config.js` 的 `drop` 配置），不额外泄露调试信息。

#### 7) 与现有部署章节的关系

- 「五分钟部署（Docker Compose）」「手动部署（Java 21 + MySQL 8 + Nginx）」仍是默认推荐路线（Nginx/容器直服 `dist/`），本节是**可选进阶**，后端与 Nginx 安全配置完全共用；
- 从本节切回默认路线：`VITE_ASSETS_BASE_URL` 恢复 `/` 重新构建，Nginx `root` 指回 `dist/` 即可；
- CI/CD 一节的 `frontend-build` 工作流会上传 `dist/` 产物 artifact，可直接作为本节上传 COS 的产物来源。

### 安装后配置（安全与邮件）

以下操作都在管理后台完成，每次新装站点建议先做这一节。

**SMTP 邮件服务**（管理端 → 邮件设置，`/admin/mail-settings`）

| 字段 | 说明 |
| --- | --- |
| SMTP 服务器 / 端口 | 如 `smtp.example.com`；常用端口 465（SSL）/ 587（TLS）/ 25（不加密） |
| SMTP 账号 / 授权码 | 邮箱账号与服务商授权码（授权码 ≠ 邮箱登录密码） |
| 加密方式 | `ssl` / `tls` / `none` |
| 发件人邮箱 / 名称 | 对外展示的发件身份 |

- 填好后打开「启用」开关保存，再点**「发送测试邮件」**填自己的邮箱验证能收到——这一步通了，再去开「注册邮箱验证」。
- **SMTP 就绪判定**（后端口径）：「启用」开关打开 + SMTP 服务器已填（非 `smtp.example.com` 占位）+ 发件人已填 + 授权码已填（非 `CHANGE_ME` 占位）。缺任何一项，管理端安全设置页就显示未就绪，「注册邮箱验证」开关无法开启。
- **内网无鉴权中继 / MailPit**（本地或内网调试推荐）：主机填中继地址、端口如 1025、加密方式选「不加密」、账号与授权码**随便填非空值**（如 `mailpit`，中继不校验），发件人照填——即可满足就绪判定并打通测试邮件。Docker 用户可加一个 [axllent/mailpit](https://github.com/axllent/mailpit) 容器，用它的 Web 界面直接看信。

**注册邮箱验证开关**（管理端 → 安全设置，`/admin/security-settings`）

- 开启后，新用户注册需输入邮箱验证码激活；是否允许自注册由「开放邮箱注册」另行控制（关闭时仅管理员可建号）。
- **依赖 SMTP**：SMTP 未就绪时前端拒绝保存该开关；安装向导第 4 步的同名开关同理。

**登录二步验证开关（TOTP）**（同一页面）

- 开启后**所有用户**下次登录都会被要求绑定/验证 TOTP（Google Authenticator、Aegis、1Password 等任何标准验证器）：
  - 已绑定用户：输入验证器上的 **6 位动态码**；验证器丢失可改用绑定时保存的 **8 位还原码**。
  - 未绑定用户（含被强制开启的存量用户）：登录时进入强制绑定流程——扫描二维码（标准 otpauth 协议）→ 输入 6 位码确认 → 系统展示 **10 个一次性还原码，仅此一次，务必保存**。
- **用户验证器与还原码全丢了**：管理员在成员管理（`/admin/users`）对该用户执行「重置二步验证」（`PUT /api/admin/users/{id}/2fa/reset`），该用户下次登录重新走绑定流程。

### 开发环境搭建（源码调试）

这里以 Ubuntu 本地开发为例：

1) 克隆与依赖
```bash
git clone https://github.com/luminous-ChenXi/AstrNest.git
cd AstrNest
```
2) 初始化数据库（本地 MySQL）
```bash
mysql -u root -p < backend/db/init.sql
```

> **⚠️ 重要提醒**：启动后端前，请确保数据库配置正确（推荐全部通过环境变量注入，参见 `.env.example`）：
> - 数据库URL：`jdbc:mysql://localhost:3306/astrnest`
> - 用户名：`astrnest`（可通过环境变量 `ASTRNEST_DB_USERNAME` 覆盖）
> - 密码：通过环境变量 `ASTRNEST_DB_PASSWORD` 注入，请勿提交到仓库

3) 启动后端
```bash
cd backend
./mvnw spring-boot:run
```
4) 启动前端（新终端）
```bash
cd frontend
npm install
npm run dev
```
> **⚠️ 重要提醒**：
> - 如果 `npm install` 出现权限错误（如 `EACCES` 或 `permission denied`），通常是因为 npm 全局目录权限问题。解决方法：
>   1. 修改 npm 全局目录权限：`sudo chown -R $(whoami) ~/.npm`
>   2. 或使用 npx 运行：`npx npm install`
>   3. 或清除 npm 缓存后重试：`npm cache clean --force && npm install`
>   4. 如果以上方法都无效，考虑使用 `sudo npm install` 安装依赖（但不推荐）。
> - 如果使用镜像源出现 404 错误，建议切换回官方源：`npm config set registry https://registry.npmjs.org/`
> - 如果 `npm run dev` 启动时出现 `EACCES: permission denied, mkdir '.../node_modules/.vite/...'` 错误，说明 `node_modules` 目录权限不足。解决方法：
>   1. 修复 `node_modules` 目录权限：`sudo chown -R $(whoami) node_modules`
>   2. 或直接删除后重新安装：`rm -rf node_modules && npm install`

5) 访问
- 前端：http://localhost:5175
- 后端：http://localhost:8081
- API 文档：http://localhost:8081/swagger-ui/index.html

> **⚠️ 重要提醒**：
> - 如果显示“网络连接错误”，请检查后端服务是否已启动，端口是否正确；检查`application.yml`中的端口配置是否与实际一致；检查防火墙是否放行该端口；检查是否有其他服务占用该端口；检查是否开启了代理（如 Nginx）；检查代理配置是否正确；
> - 如果显示“404 Not Found”，请检查前端项目是否已正确构建，且 `dist/` 目录下的文件是否存在。
> - 如果显示“CORS 错误”，请检查后端 `SecurityConfig` 中的 CORS 配置是否允许当前前端域名。
> - 如果显示“403 Forbidden”，请检查当前用户角色是否有访问该接口的权限。
> - 如果显示“500 Internal Server Error”，请检查后端日志，查找具体错误信息。

> 如果你想直接看配置细节（环境变量、文件路径、初始化脚本、FFmpeg、存储切换等），请跳转 `CONFIG_GUIDE.md`。

### 安装向导 | Install Wizard

AstrNest 内置类似 WordPress 的**可视化安装向导**，六步流程（环境检测 → 数据库配置 → 初始化 → 站点配置 → 创建管理员 → 完成）的逐步说明见上文「五分钟部署 · 第 3 步」。两种部署路径在向导中的差异：

| 部署路径 | 数据库表结构 | 初始管理员 |
| --- | --- | --- |
| **Docker Compose**（挂载 `init.sql` 自动初始化） | 由 `backend/db/init.sql` 在 MySQL 容器首次启动时自动创建，向导「初始化」步自动跳过 | 向导第 5 步创建（推荐），或首个注册用户自动 ADMIN |
| **手动 / 宝塔 / 裸 Jar 部署**（未跑过初始化 SQL） | 向导第 3 步一键建表（`install-schema.sql`，幂等） | 向导第 5 步创建（推荐），或首个注册用户自动 ADMIN |

- 向导地址：`http://your-domain.com/install`（未安装时访问任意页面都会被引导到向导）。
- **安装后再次访问 `/install`**：显示「系统已安装」并拒绝重新初始化；`/api/install` 的写接口在安装完成后一律返回 403，防止重放调用。
- **装到一半卡住**：向导提供「重置安装状态」入口（`POST /api/install/reset`），仅「未完成站点」（users 表不存在或没有用户）可调用；更多安装类 FAQ 见下文。
- 注意：向导不负责写数据库连接参数——Spring Boot 应用必须先能连上 MySQL（通过 `ASTRNEST_DB_URL` / `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD` 等配置）才能启动，向导负责的是**建表与初始账号**。检测项含义与失败排查见 `CONFIG_GUIDE.md` 第 4.3 节。

### 生产配置要点 | Production Notes
- **`ASTRNEST_TRUSTED_PROXY`**（默认 `false`）：当后端部署在 Nginx 等反向代理之后时设为 `true`，后端才会信任并解析 `X-Real-IP` / `X-Forwarded-For`，日志审计与限流才能拿到真实客户端 IP；对应配置键 `astrnest.security.trusted-proxy`。
- **`astrnest.jwt.secret`**：JWT 签名密钥，生产必须显式配置（`ASTRNEST_JWT_SECRET`），否则每次重启生成临时密钥、所有人被强制下线；Token 有效期由 `chenxi.passport.access-token-days` 控制（默认 30 天不活动过期）。
- **图片直链**：生产让 Nginx 静态直服 `/upload/**`（样例见上文手动部署），并把「系统配置 → 资源加速域名（`asset_domain`）」或前端 `VITE_PUBLIC_ASSET_BASE` 指向该域名。
- **Nginx `client_max_body_size`**：需与后端 `spring.servlet.multipart.max-file-size` 保持匹配，否则大图上传会被 Nginx 拦截（413）。
- **Docker Compose 端口绑定**：`docker-compose.yml` 中数据库与后端端口默认仅绑定 `127.0.0.1`，生产建议通过 Nginx 反代对外提供服务，不要将数据库/后端端口直接暴露公网。
- **SSO 单点登录**：详见下方「SSO 单点登录（外部身份源）」。

### SSO 单点登录（外部身份源，默认关闭）

AstrNest 支持通过 **OAuth 2.1 / OIDC「授权码 + PKCE(S256)」** 对接外部身份源实现统一登录，兼容"辰汐通行证"及任何标准 OAuth 2.1/OIDC 提供方（示例 issuer：`https://passport.example.com`）。功能**默认关闭**（`chenxi.passport.enabled=false`），关闭时前端不显示入口、后端接口返回明确错误，对现有本地登录/注册/API Key 零影响。详见 `docs/chenxi-integration.md`。

- **登录流程**：前端跳转身份源授权页（PKCE）→ 身份源回调 `/auth/sso/callback` → 将 `code + codeVerifier` 提交 `POST /api/auth/sso/exchange`（服务端交换，浏览器不接触通行证 token）→ 后端回站自省校验（`{issuer}/oauth2/introspect`）并签发本地 JWT（与本地登录响应一致）。
- **影子账号**：首次 SSO 登录自动建档（`identity_source=passport`、`sso_sub` 关联身份源）；昵称/头像/邮箱每次登录以身份源为准同步，本地**只读**（不可改资料、不可改密，提示"请在身份源侧修改"）。
- **身份源侧需注册回调地址**：`https://<your-domain>/auth/sso/callback`。

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `chenxi.passport.enabled` | `CHENXI_PASSPORT_ENABLED` | `false` | 是否启用通行证登录 |
| `chenxi.passport.issuer` | `CHENXI_PASSPORT_ISSUER` | 空 | 身份源签发方地址，如 `https://passport.example.com` |
| `chenxi.passport.client-id` | `CHENXI_PASSPORT_CLIENT_ID` | 空 | 在身份源侧注册的 public client id |
| `chenxi.passport.redirect-uri` | `CHENXI_PASSPORT_REDIRECT_URI` | 空 | 回调地址，须与身份源侧注册完全一致 |
| `chenxi.passport.scopes` | `CHENXI_PASSPORT_SCOPES` | `openid,profile` | 授权 scope |
| `chenxi.passport.access-token-days` | `CHENXI_PASSPORT_ACCESS_TOKEN_DAYS` | `30` | 本地令牌不活动过期天数（滑动刷新） |

## FAQ | 常见问题

### 安装向导问题（装不上 / 锁死 / 想重装）

- **向导打不开、一直「无法连接服务器」**：后端没起来。先看后端日志——数据库连不上时 Spring Boot 无法启动，向导页面无从谈起。向导的前提是「应用已启动、库为空」。
- **装到一半卡住，想重走流程**：向导页面提供**「重置安装状态」**入口（`POST /api/install/reset`），清理 `install.lock` 与数据库完成标记后可回到第一步；仅「未完成站点」（users 表不存在或没有用户）可调用。
- **已装完想重装**：两步——① 清空数据库（删库重建，或清空全部业务表）；② 删除防重装锁 `install.lock`（位于存储根目录的父目录，默认 `/storage/install.lock`；Docker Compose 场景该文件在 backend 容器内，`docker compose down` 后重新 `up -d` 重建容器即随之清除，`mysql_data` 数据卷是否保留按需决定）。重启后重新打开 `/install`。
- **装完再访问 `/install`**：显示「系统已安装」并拒绝重新初始化；`/api/install` 写接口一律 403，防止重放调用。

### 图片直链 404 或 500

- **直链返回 500（裸机 / 宝塔直跑 jar，直链打到 `:8081/upload/**`）**：官方规避方案是让 **Nginx 静态直服 `/upload/**`**（样例见上文「手动部署 · Nginx 样例」的 `location ^~ /upload/`），把读取流量从后端挪到 Nginx。注意 `root` 指向的是 `ASTRNEST_STORAGE_ROOT` 的**父目录**（`root + /upload/...` 拼出完整磁盘路径），且 Nginx 进程对该目录有读权限。
- **直链 404 或返回网页（Docker Compose，直链打到 80 端口）**：`frontend` 容器的 Nginx 只反代 `/api/`，**不托管 `/upload/**`**。解决方案同上——宿主 Nginx 直服宿主机 `./storage/upload` 挂载目录；或把后台「系统配置 → 资源加速域名（`asset_domain`）」与前端 `VITE_PUBLIC_ASSET_BASE` 指向真正能服务文件的域名 / CDN。
- **自查清单**：文件确实存在于 `ASTRNEST_STORAGE_ROOT/{yyyy}/{MM}/` 下；直链域名与 `asset_domain` / `VITE_PUBLIC_ASSET_BASE` 一致；`docker compose` 场景确认宿主机挂载目录里能看到文件（`ls ./storage/upload`）。

### 邮箱验证收不到信

1. 先在管理端「邮件设置」点**「发送测试邮件」**：收不到说明 SMTP 本身不通——核对服务器/端口/加密方式/授权码（授权码 ≠ 邮箱登录密码），并查看后端日志中 mail / SMTP 相关异常。
2. 「注册邮箱验证」开关开着但发不出验证码：到管理端「安全设置」看 SMTP 是否显示**未就绪**——检查「启用」开关是否打开、主机/发件人/授权码是否仍是 `smtp.example.com` / `CHANGE_ME` 占位值。
3. 内网 / 本地环境：云厂商常封 25 端口，公网 SMTP 也可能被拦截；改用 465/587，或用内网中继 / MailPit（见「安装后配置」）。
4. 测试邮件能收到、验证码收不到：查垃圾箱；确认注册邮箱拼写无误；验证码有时效，过期请重发。

### 其他常见问题

| 问题 | 处理建议 |
| --- | --- |
| CORS 报错 | 确保后端 `SecurityConfig` 中的 CORS 白名单包含当前前端域名，或在部署入口层（Nginx）补充 `Access-Control-*` 头。 |
| 数据库认证失败 | 检查 `spring.datasource.username/password` 与 MySQL 用户授权是否一致。 |
| API 上传 401 | `POST /api/uploads` 需要登录管理员或携带有效 `X-API-Key`。在安全中心/API 页面创建密钥。 |
| 静态资源访问不到 | 若切换对象存储，记得同步配置 `astrnest.storage.local.public-base-url` 或在后台设置资产域名，并确保 CDN/桶权限正确。 |
| 邮件发送失败 | 检查邮件配置是否正确，包括SMTP服务器、端口、用户名和密码 |
| 验证码验证失败 | 确保验证码在有效期内，且输入正确 |
| 数据库连接超时 | 检查 `spring.datasource.hikari.connection-timeout` 配置（默认 30000ms），确保网络稳定或适当调高超时时间 |

### 常用脚本
- 后端开发：`cd backend && ./mvnw spring-boot:run`
- 后端测试：`cd backend && ./mvnw test`
- 前端开发：`cd frontend && npm run dev`
- 前端构建：`cd frontend && npm run build`

### API 快速示例
```bash
# 登录
curl -X POST "http://localhost:8081/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"<你的用户名>","password":"<你的密码>"}'

# 上传（API Key）
curl -X POST "http://localhost:8081/api/uploads" \
  -H "X-API-Key: <your-api-key>" \
  -F "file=@/path/to/image.jpg"
```

## Development Roadmap | 开发路线图
- ~~添加图片人工审核功能，支持手动审核图片违规情况~~ ✅
- ~~添加AI图片违规查询功能，支持批量查询图片违规情况~~ ✅
- ~~完善图片Tag标签功能，支持批量添加、删除、搜索~~ ✅
- ~~支持更多云存储提供商（阿里云OSS、腾讯云COS等）~~ ✅
- [ ] 代码小白友好型初始化页面
- [ ] 图片压缩和格式转换功能
- [ ] 图片水印添加功能
- [ ] 图片批量处理工具
- [ ] 移动端应用开发
- [ ] 第三方登录集成

## Acknowledgments | 致谢

### 开源组件合规声明
本项目基于众多优秀的开源组件构建，感谢开源社区的贡献！以下是按前端/后端列出主要依赖组件及其许可证信息，便于合规排查：

#### 前端（参考/frontend/package.json）
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

#### 后端（参考/backend/pom.xml）
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
- **阿里云 OSS SDK** [`com.aliyun.oss:aliyun-sdk-oss@3.17.4`](https://github.com/aliyun/aliyun-oss-java-sdk) - [Apache License 2.0](https://github.com/aliyun/aliyun-oss-java-sdk/blob/master/LICENSE)
- **腾讯云 COS SDK** [`com.qcloud:cos_api@5.6.255.1`](https://github.com/tencentyun/cos-java-sdk-v5) - [MIT License](https://github.com/tencentyun/cos-java-sdk-v5/blob/master/LICENSE)
- **又拍云 Java SDK** [`com.upyun:java-sdk@4.2.3`](https://github.com/upyun/java-sdk) - [MIT License](https://github.com/upyun/java-sdk/blob/master/LICENSE)
- **Lombok** - [MIT License](https://github.com/projectlombok/lombok/blob/master/LICENSE)
- **Spring Boot Configuration Processor** - [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
- **H2 Database** (test) - [MPL 2.0 / EPL 1.0](https://github.com/h2database/h2database/blob/master/LICENSE.txt)
- **Spring Security Test** (test) - [Apache License 2.0](https://github.com/spring-projects/spring-security/blob/main/LICENSE.txt)

#### 免责声明
- 以上许可证信息基于各组件官方仓库的最新信息
- 具体版本可能随项目更新而变化
- 使用本项目时请确保遵守所有依赖组件的许可证要求
- 建议在商业使用前进行详细的许可证合规审查


## Contributing | 贡献指南

欢迎贡献代码！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细流程。

## Issue Reporting | 问题报告

如遇问题，请：
1. 查看 [常见问题](#faq--常见问题)
2. 搜索 [GitHub Issues](https://github.com/your-repo/astrnest/issues)
3. 创建新的 Issue（同时欢迎你能够提供宝贵建议！）
4. 查看[联系方式](#contact)

## License | 许可证

本项目基于 [GPL-3.0 with Additional Terms (Non-Commercial)](LICENSE) 开源（附加非商用条款：禁商用 + 署名）。

![GPL-v3](https://www.gnu.org/graphics/gplv3-127x51.png)

## Security | 安全

安全相关问题请查看 [SECURITY.md](SECURITY.md)。


## Contact | 联系方式

- **问题反馈**: 通过GitHub Issues提交
- **邮箱**: chenxi@luminouschenxi.net
- **Discord**: [LuminousChenxi](https://discord.gg/hBsqcfwC9Q)

---
求点赞！求关注！求投喂！
⭐ 如果这个项目对您有帮助，请给我们一个 Star！

> 若受本项目启发，欢迎附上出处链接。/ If this project inspires your work, a link back is appreciated.
