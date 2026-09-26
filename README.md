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
  <a href="#quick-start">快速开始</a> · 
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
  - Token 默认有效期 **72 小时**，可通过 `astrnest.jwt.ttl-hours`（环境变量 `ASTRNEST_JWT_TTL_HOURS`）调整。
  - 生产环境必须配置签名密钥 `astrnest.jwt.secret`（环境变量 `ASTRNEST_JWT_SECRET`，请使用随机长字符串并妥善保管，勿提交仓库）。
- **HTTP Basic（兼容保留）**：仍可用于 API 插件/脚本调用（如 Typora、PicGo 等自定义上传插件），与 API Key 认证并行支持。

## Quick Start | 快速开始（开发）

这里以Ubuntu本地开发为例：

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

> **管理员账号说明**：初始化 SQL **不再预置管理员账号**。推荐通过**安装向导**（`/install`，见下文）在首次部署时创建初始管理员；也可以使用仓库自带的 `init-admin.py` / `init-admin.sh` / `init-admin.bat` 脚本创建（或重置）管理员；作为无向导环境的兜底，**第一个完成注册的用户会自动成为管理员**。

> 如果你想直接看配置细节（环境变量、文件路径、初始化脚本、FFmpeg、存储切换等），请跳转 `CONFIG_GUIDE.md`。

### 安装向导 | Install Wizard（全新部署推荐）

AstrNest 内置了类似 WordPress 的**可视化安装向导**。全新部署（数据库为空库）后打开站点，会自动跳转到 `http://your-domain.com/install`，引导你完成四步初始化：

1. **环境检测**：自动检查数据库连接与版本（推荐 MySQL >= 8.0）、数据表结构、本地存储目录可写性、Java 运行时、FFmpeg（缺失仅警告）。有致命问题时给出排查提示，可点击"重新检测"。
2. **安装数据库**：一键执行向导专用脚本 `backend/db/install-schema.sql` 建表并写入默认角色/配置（幂等，重复执行不破坏已有数据）。
3. **创建管理员**：设置初始管理员账号（ADMIN 角色、BCrypt 加密密码、不限上传配额），这是系统的唯一初始账号入口。
4. **完成**：写入安装完成标记并进入站点。

两种部署路径对比：

| 部署路径 | 数据库表结构 | 初始管理员 |
| --- | --- | --- |
| **Docker Compose**（挂载 `init.sql` 自动初始化） | 由 `backend/db/init.sql` 在 MySQL 容器首次启动时自动创建 | 首个注册用户自动成为 ADMIN，或用 `init-admin` 脚本创建 |
| **手动 / 宝塔 / 裸 Jar 部署**（未跑过初始化 SQL） | 打开站点进入向导，第 2 步一键建表（`install-schema.sql`） | 向导第 3 步创建（推荐），或首个注册用户自动 ADMIN |

- 向导地址：`http://your-domain.com/install`（前后端任一入口均可，未安装时访问任意页面都会被引导到向导）。
- **安装后再次访问 `/install`**：会显示"系统已安装"并拒绝重新初始化；`/api/install` 的写接口在安装完成后一律返回 403，防止重放调用。
- 注意：向导不负责写数据库连接参数——Spring Boot 应用必须先能连上 MySQL（通过 `ASTRNEST_DB_URL` / `ASTRNEST_DB_USERNAME` / `ASTRNEST_DB_PASSWORD` 等配置）才能启动，向导负责的是**建表与初始账号**。检测项含义与失败排查见 `CONFIG_GUIDE.md` 第 4.3 节。

### 部署概览
- **Docker Compose（推荐）**：复制 `.env.example` → `.env`，填好数据库/域名/SMTP/存储，再执行：
```bash
docker compose --env-file .env up -d
```
- **传统部署**：`backend` 打包 `./mvnw clean package && java -jar target/backend-0.0.1-SNAPSHOT.jar`；`frontend` 运行 `npm run build` 后将 `dist/` 交给 Nginx/CDN。

更详细的环境变量、Nginx 反代、CDN/对象存储切换请查看 `CONFIG_GUIDE.md`。

### 生产配置要点 | Production Notes
- **`ASTRNEST_TRUSTED_PROXY`**（默认 `false`）：当后端部署在 Nginx 等反向代理之后时设为 `true`，后端才会信任并解析 `X-Real-IP` / `X-Forwarded-For`，日志审计与限流才能拿到真实客户端 IP；对应配置键 `astrnest.security.trusted-proxy`。
- **`astrnest.jwt.secret` / `astrnest.jwt.ttl-hours`**：JWT 签名密钥与 Token 有效期（默认 72 小时），生产必须显式配置 secret。
- **Nginx `client_max_body_size`**：示例反代配置已放开请求体限制，需与后端 `spring.servlet.multipart.max-file-size` 保持匹配，否则大图上传会被 Nginx 拦截（413）。
- **Docker Compose 端口绑定**：`docker-compose.yml` 中数据库与后端端口默认仅绑定 `127.0.0.1`，生产建议通过 Nginx 反代对外提供服务，不要将数据库/后端端口直接暴露公网。
- **SSO 单点登录**：详见下方「SSO 单点登录（外部身份源）」。

### SSO 单点登录（外部身份源，默认关闭）

AstrNest 支持通过 **OAuth 2.1 / OIDC「授权码 + PKCE(S256)」** 对接外部身份源实现统一登录，兼容"辰汐通行证"及任何标准 OAuth 2.1/OIDC 提供方（示例 issuer：`https://passport.example.com`）。功能**默认关闭**（`astrnest.sso.enabled=false`），关闭时前端不显示入口、后端接口返回明确错误，对现有本地登录/注册/API Key 零影响。

- **登录流程**：前端跳转身份源授权页（PKCE）→ 身份源回调 `/auth/sso/callback` → 授权码换 `access_token` → `POST /api/auth/sso/exchange` 由后端回站自省校验（`{issuer}/oauth2/introspect`）并签发本地 JWT（与本地登录响应一致）。
- **影子账号**：首次 SSO 登录自动建档（`identity_source=passport`、`sso_sub` 关联身份源）；昵称/头像/邮箱每次登录以身份源为准同步，本地**只读**（不可改资料、不可改密，提示"请在身份源侧修改"）。
- **身份源侧需注册回调地址**：`https://<your-domain>/auth/sso/callback`。

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `astrnest.sso.enabled` | `ASTRNEST_SSO_ENABLED` | `false` | 是否启用 SSO 登录 |
| `astrnest.sso.issuer` | `ASTRNEST_SSO_ISSUER` | 空 | 身份源签发方地址，如 `https://passport.example.com` |
| `astrnest.sso.client-id` | `ASTRNEST_SSO_CLIENT_ID` | 空 | 在身份源侧注册的 public client id |
| `astrnest.sso.redirect-uri` | `ASTRNEST_SSO_REDIRECT_URI` | 空 | 回调地址，须与身份源侧注册完全一致 |
| `astrnest.sso.scopes` | —（yml 配置） | `openid, profile, email` | 授权 scope |
| `astrnest.sso.introspect-timeout-seconds` / `userinfo-timeout-seconds` | —（yml 配置） | `5` | 回站自省/用户信息请求超时（秒） |

## Problem Solving | 问题解决

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
1. 查看 [常见问题](#常见问题)
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
