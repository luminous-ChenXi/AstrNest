# AstrNest 全链路审计报告（第二轮调查）

> 审计日期：2026-10-05
> 审计对象：`main` 分支 `d7edcd0`（Spring Boot 3.4.13 / Java 21 / MySQL，后端 287 个 Java 文件；Vue 3.5 + Vite 前端 55 个文件；浏览器扩展）
> 审计方式：5 路并行代码走查（认证 / 上传存储 / 内容与统计 / CI-CD / 安全横切面）+ 本机构建测试实测 + 全部 P0 结论人工逐条复核源码
> 用途：供站长逐条审阅后决定修复批次。每个问题都有编号（如 `P0-1`）与 `文件:行号` 定位，可直接引用。

---

## 0. 测试与构建基线（本轮实测结果）

| 项目 | 结果 | 说明 |
|---|---|---|
| 后端 `./mvnw test` | ✅ BUILD SUCCESS | **17 个测试 / 5 个测试类全部通过**（AdminUploadServiceTest 2、TotpServiceTest 6、UploadStaticResourceConfigTest 3、SystemConfigServiceSmtpTest 5、contextLoads 1） |
| 前端 `npm run build` | ✅ 6.4s 构建成功 | 但 `vendor-ui` 单 chunk 887KB（gzip 286KB），超 800KB 警告 |
| 测试覆盖面 | ❌ 严重不足 | 全项目只有 5 个测试类；注册/登录/2FA/上传/配额/权限（IDOR）/安装向导等核心链路**零测试**。CI 里跑的 17 个测试对回归几乎无保护力 |

基线日志中直接暴露两个已确认的问题：JWT 密钥未配置时只打 WARN（`JwtTokenService.java:52-58`，实测日志原文"已生成临时随机密钥"）；Spring Security 官方警告 `/upload/**` 不应使用 `web.ignoring()`（`SecurityConfig.java:162`）。

---

## 1. 第一轮调查回顾（2026-09-16 大治理，已完成项）

第一轮治理（git 历史重建前后的 `ee0d505`、`62fb0f5` 及后续提交）已完成并验证的修复：

- 路径穿越：`StoragePathGuard`（key 黑名单 + resolve/normalize/startsWith 双校验）接入所有读路径；`PublicAssetController` 对 URI 先解码再校验（防 `%2E%2E`）；静态资源映射用 `ResourceHandlerUtils.isResourceUnderLocation`。
- 认证：JWT（jjwt，HS256，`purpose=2fa` 过渡令牌隔离）替换 Base64 Basic token；`/api/monitor/**` 收口 ADMIN；BCrypt 强度 10→12。
- TOTP 2FA：手写 RFC 6238 完全正确（±1 窗口、常量时间比较、`lastUsedCounter` 防重放、还原码 BCrypt 哈希+用后即焚）——这是全项目质量最高的模块。
- 安装向导：四重防重装锁（DB 标记 + `storage/install.lock` + 写端点 403 + 前端路由）、`InstallGuardFilter` 未安装拦 503、无内置 admin。
- 收口类：云密钥/AI 密钥掩码、SMTP 密码不回显、SVG 出口 CSP sandbox + nosniff（`MediaServingHeaderFilter`）、CORS 默认空 origin、docker 端口绑 127.0.0.1、全局安全响应头（nosniff/XFO/Referrer-Policy/HSTS）、CVE 驱动依赖升级（pom 依赖面干净，无 log4j/旧 commons 类直依赖）。
- 后续补充：注册邮箱验证开关 + 登录 2FA 强制开关（站长可配）、管理端删除同步物理文件（`401a9a1`）、媒体直链 500 修复、索引规整。

**第一轮遗留的运维待办（仍未做）**：GitHub `main` 是受保护分支，干净历史在 `main-clean`；需在 GitHub 关闭分支保护 → `git push --force origin main` → 删除 `main-clean`。**旧泄露历史在 force push 前仍然可访问，其中含旧密码哈希**，建议尽快处理并轮换相关密码。

**本轮结论**：第一轮治理的"点"质量普遍很高（模块内实现讲究），但第二轮在**链路编排层**发现了一批第一轮没有覆盖到的问题——包括 6 个 P0。

---

## 2. P0 — 必须立即修（6 项）

### P0-1 视频封面（videoCovers）完全绕过文件校验 → 任意文件上传 / 存储型 XSS
- **位置**：`upload/UploadService.java:157-160`（调用点）、`:393-402`（`storeFrontendCover` 直接 `storageService.store(coverFile, context)`，无任何 `mediaInspector.inspect()`）；对比主文件路径 `:146-150` 有完整校验。
- **影响**：上传请求里 `videoCovers[]` 参数可以传 `.html`/任意扩展名/任意内容文件，直接落盘并被同源 inline 服务（`/upload/**` 匿名可读）。SVG 有 CSP sandbox 兜底，但 **HTML 没有任何响应头防护**，`<script>` 直链打开即执行——登录用户可对任意访客实施存储型 XSS，可盗 JWT。
- **业界范式**（OWASP File Upload Cheat Sheet）：所有用户提供的字节流一律走同一校验管道（扩展名白名单 + MIME + 魔数），不存在"封面就免检"的例外；服务端生成存储文件名（见 P0-3）。

### P0-2 匿名公开接口返回全站用户邮箱（PII 泄露 + 邮箱枚举）
- **位置**：`admin/user/AdminUserService.java:93-106`（`publicProfile` 把 `user.getEmail()` 塞进响应）、`user/dto/PublicUserProfileResponse.java`（第 3 个字段就是 email）、`config/SecurityConfig.java:95`（`GET /api/public/users/**` permitAll）、`user/PublicUserController.java`。
- **影响**：未认证遍历用户 id 即可收割全站注册邮箱（撞库、钓鱼原料），同时构成账号枚举。
- **业界范式**：公开档案只含 displayName/avatar/统计数；邮箱属于 PII，任何公开端点不得输出（ASVS 2.1/隐私最小化）。

### P0-3 云存储 objectKey = `年/月/原始文件名`，跨用户同名互相覆盖
- **位置**：`storage/s3/AbstractS3StorageHandler.java:260-266`（`buildObjectKey`）、`storage/ali/AliyunOssStorageHandler.java:97-103`、`storage/upyun/UpyunStorageHandler.java:129-135`、`storage/onedrive/OnedriveStorageHandler.java:389-394`；对象 put 为覆盖语义，无碰撞检测、无用户维度。
- **影响**：任何用户上传与目标同名的文件（如 `photo.jpg`）即可**覆盖他人当月已上传的同名文件**——内容被静默替换（图片可被换成任意内容，含诈骗二维码），属完整性破坏。本地盘靠 exists+时间戳缓解，但 `LocalStorageHandler.java:68` 的 `REPLACE_EXISTING` + `:141-155` 的 exists-then-copy 仍有并发竞态。
- **业界范式**：objectKey 用 `{uuid}.{ext}` 或内容哈希命名；原始文件名只作为元数据 / `Content-Disposition` 展示。这也是图片直链可枚举问题的根（见 P1-4）。

### P0-4 安装向导"抢注"竞态：公网可达即可接管全新站点
- **位置**：`install/InstallController.java:66-120`（`/api/install/database`、`/admin`、`/finish` 均匿名，唯一守卫 `ensureWizardUnlocked()` 只在装完后生效）、`install/InstallStatusService.java:144-180`（installed 唯一判据 = users 表有用户）、`chenxi/auth/ChenxiAuthService.java:110-116`（**首个注册用户自动 ADMIN**）。
- **影响**：站长部署后、走完向导前的窗口期，任何能访问站点的人可匿名：建表 → 用自己的邮箱/密码抢注第一个用户（自动成为 ADMIN）→ `finish` 写锁永久占坑。`/api/install/site-config` 在建管理员后、finish 前同样是匿名可写窗口。
- **业界范式**（WordPress 安装页思路）：安装写端点要求一次性安装密钥（部署时 env 注入 `ASTRNEST_INSTALL_TOKEN`，向导第一步要求填写），或安装完成前仅允许本机/内网访问。

### P0-5 HTTP Basic 兼容通道绕过全部防爆破锁定
- **位置**：`config/SecurityConfig.java:111`（`.httpBasic(Customizer.withDefaults())` 全局生效）；失败计数只在 `user/AuthService.java:78-82`（仅 `/api/auth/login` JSON 登录路径）写入 `AuthProtectionService`；Basic 认证失败不经过任何锁定逻辑。
- **影响**：攻击者拿到用户名后，对**任意**受保护接口塞 `Authorization: Basic base64(user:pass)` 头即可无限速暴力破解密码——第一轮建的用户+IP 双维度锁定、365 天拉黑全部被旁路。且 `SecurityConfig` 的 CSRF 已禁用 + Basic 弹窗在浏览器场景仍可被跨站触发。
- **业界范式**：纯 Bearer/JWT 架构应移除 `httpBasic()`（OpenApiConfig.java:29-30 把它文档化为"兼容通道"，但 API Key 已覆盖脚本场景）；若必须保留，至少让 Basic 失败同样计入 `AuthProtectionService` 并默认关闭。

### P0-6 JWT 无吊销机制：改密码/盗号后无法踢掉旧会话
- **位置**：`security/jwt/JwtTokenService.java:59-60`（TTL 30 天）、`security/jwt/JwtAuthenticationFilter.java:91-105`（响应头滑动续期）；全项目无 jti/会话表/黑名单、无 `/logout` 端点；改密码只换哈希不失效旧 token（`user/UserPortalService.java:189-201`）。
- **影响**：token 一旦泄露（配合 P0-1 的 XSS 即可拿到），受害者改密码也无效，攻击者可续航 30 天。ASVS 3.3 明确要求"会话可终止"。
- **业界范式**：用户表加 `token_version`（int），JWT 带 `ver` claim，过滤器比对不一致即 401；改密码/踢下线时 `token_version+1`。改动小（一个字段 + 两处代码），建议与 `/logout`（前端删 token）一起补。

---

## 3. P1 — 高优先级（上线前应修）

### 认证链路
- **P1-1 JWT 密钥部署陷阱**：`astrnest.jwt.secret` 不存在于任何 yml（`application.yml`/`application-prod.yml` 均无，已实测确认），`.env.example` 也没有该变量（grep 无结果，已实测确认）。照模板部署的用户会静默落在"临时随机密钥"模式——**每次重启全员掉线**，多实例部署时各节点密钥不同互不相认，且只有一条 WARN。建议：`.env.example`/docker-compose 显式补 `ASTRNEST_JWT_SECRET` 并加 `:?` 空值断言；**prod profile 下缺失直接启动失败**（fail-fast），参照 compose 里 MySQL 密码的做法（`docker-compose.yml:10-13`）。
- **P1-2 验证码/链接令牌明文入库**：邮箱验证码、注册链接令牌明文存 `chenxi_email_token.code`（`chenxi/auth/ChenxiEmailToken.java:32-34`，`ChenxiAuthService.java:154`）；TOTP 密钥明文 Base32 存库（`security/totp/UserTotp.java:30-33`）；SMTP 授权码明文入库（`chenxi/mail/ChenxiMailConfig.java:31-32`）。对比 TOTP 还原码都做了 BCrypt，标准不一。业界范式：验证码/令牌入库前 SHA-256 哈希（比对时哈希后比较）；TOTP 密钥与 SMTP 凭证应用对称加密（密钥来自 env）或至少标注风险。DB 泄露场景下这是二次纵深。
- **P1-3 邮箱枚举三连**：① `GET /api/auth/chenxi/check-email` 匿名无限流返回 `available` 布尔（`ChenxiAuthController.java:97-101`）；② 注册发码报"该邮箱已绑定账号"（`ChenxiAuthService.java:46-48`）；③ 找回密码报"未找到该邮箱对应的账号"（`ChenxiAuthService.java:56-58`）。业界范式：check-email 加限流或改在前端注册提交时顺带校验；发码/找回一律返回中性文案（"若该邮箱存在，验证码已发送"）。
- **P1-4 锁定可被"注册成功"洗掉**：`/register` 与 `/password/reset` 成功后调用 `recordLoginSuccess`，把该 IP 的 `IP_ONLY` 失败计数整体清零（`ChenxiAuthController.java:76,93` → `security/bruteforce/AuthProtectionService.java:50-53`）。开放注册期间，攻击者在被锁 IP 上自助注册小号即可解锁继续爆破。建议：`recordLoginSuccess` 的清零只对**同一用户名**生效；或注册/重置成功只清 `USER_IP` 维度不清 `IP_ONLY`。
- **P1-5 captcha 锁是死代码、注册失败不计数**：`CAPTCHA_IP` 维度只写不清从不评估（`AuthProtectionService.java:74-88` 写入 vs `:36-62` 只评估 USER_IP/IP_ONLY，已 grep 确认无评估点）；`recordRegisterFailure`（`:64-67`）全仓库零调用。`POST /api/auth/chenxi/captcha` 匿名且无限流，每调用写一行 `chenxi_captcha_ticket` 且**全项目无任何清理任务**（@Scheduled 全仓只有 license/stats 两处）——可匿名灌爆数据库。建议：把 CAPTCHA_IP 纳入评估、注册失败接入 `recordRegisterFailure`、给 `chenxi_captcha_ticket`/`chenxi_email_token`/`security_logs`/`album_access_logs` 四张表补定时清理。
- **P1-6 SSO 自省缺 `aud`/`iss` 校验，且建号不受注册开关约束**：introspect 只看 `active`+`sub`（`passport/SsoIdentityService.java:86-91`、`passport/PassportClient.java:85-101`）——身份源发给其他 client 的有效 token 也能换本地身份；影子账号创建无 `registrationEnabled` 检查（`SsoIdentityService.java:110-144`），关注册挡不住 SSO 进人。建议：校验 introspect 返回体的 `client_id`/`aud` 与配置一致；SSO 建号开关独立成配置项并在文档写明。

### 上传与存储链路
- **P1-7 私有文件直链匿名可达 + 文件名可枚举**：三条媒体通路（`/upload/**` 静态映射且被 `web.ignoring()` 完全踢出安全链 `SecurityConfig.java:162`；`GET /api/uploads/**` permitAll `:93`；`/api/public/assets/**` permitAll `:104`）都不校验属主/可见性；记录默认 `publicAccessible=false`（`UploadRecordService.java:62`）只是列表层过滤；画廊接口还直接返回 objectKey（`gallery/PublicGalleryService.java:266`）。配合 `年/月/原始文件名` 的可预测 key，"私密"图只要 URL 泄露一次就永久裸奔。业界范式：私有读走短时效签名 URL；直链层至少校验 `publicAccessible`（`/api/uploads/{key}` 是现成的 Controller，加一次 DB 查询即可，`/upload/**` 静态映射建议收编进 Controller）。最小改动方案：默认上传即公开（图床语义）+ 明确文档"私密=仅列表隐藏"，并把 objectKey 随机化（P0-3）作为实际防线。
- **P1-8 访客上传是死功能**：`UploadController.java:52-59` 完整实现了 `guestUploadEnabled` 开关与 IP 配额检查，但 `SecurityConfig.java:106` 要求 `POST /api/uploads/**` 必须有 ADMIN/API_CLIENT/USER 角色——匿名请求在过滤器层就 401，控制器逻辑永远不可达。安装向导里的"允许访客上传"开关无效。建议二选一：给 `POST /api/uploads` 增加匿名放行分支（在 `UploadController` 内靠 `checkGuestUploadPermission` 把关）；或删掉 `GuestUploadService` 与相关配置，别留假开关。
- **P1-9 配额 TOCTOU + API Key 上传绕过用户配额**：`enforceUserQuotas`（`UploadService.java:77,270-311`）SELECT 检查与 INSERT 落库之间无锁无原子扣减，并发可超额；且"每日上限"实为**终身总数**（`countTotalForUser` 不过滤日期，`UploadRecordService.java:157-159`）；API Key 上传时 `resolveUser()` 返回 null 完全跳过用户配额（`UploadService.java:355-364`），仅受 API Key 自身配额约束。业界范式：`UPDATE users SET quota_used = quota_used + ? WHERE id=? AND quota_used+? <= quota_max` 原子扣减；或 SELECT ... FOR UPDATE；明确"日上限"语义并修正实现。
- **P1-10 删除链路静默孤儿**：`deleteStoredFile` 吞掉一切异常只打 log（`UploadRecordService.java:204-210`），DB 记录照删；又拍云删除是 fire-and-forget（`UpyunStorageHandler.java:71-75`）；存储策略切换后旧 provider `enabled=false` 会让删除必败 → 静默孤儿对象仍可直链访问。**视频封面缩略图（`thumbnailStoragePath`）永不删除**；`album_media` 行不随记录清理。建议：删除失败重试队列或至少把失败记录落一张对账表；`deleteRecord` 同步删 thumbnailStoragePath；物理删除失败时 DB 记录延迟删除（标记 pending_delete）。

### 内容与统计链路
- **P1-11 唯一富文本字段（公告 contentMarkdown）后端零清洗**：`AnnouncementService.applyRequest`（`:95-153`）直接入库，渲染全靠前端 DOMPurify（`PublicAnnouncementDetailView.vue:222` 等三处 v-html）。任何绕过网页的 API 消费方（未来 App/RSS）都会中招；admin 账号被盗即存储型 XSS。本轮提交 `a203029` 的"XSS 清洗补口"实际只覆盖了标签（tag）字段，且用的是黑名单正则（`ChenxiTagService.java:25-28`，会误杀正常撇号/分号文本，不可推广）。用户资料 `avatarUrl/website/signature/location`（`UpdateProfileRequest.java` 仅 @Size）无 scheme 白名单，`javascript:` 可入库。业界范式：引入 jsoup `Safelist`（或 OWASP java-html-sanitizer）做服务端清洗：footer HTML 与公告正文入库前清洗；avatarUrl/website 校验必须 http(s) 开头；**不要**用黑名单正则当通用方案。
- **P1-12 SVG CSP sandbox 不覆盖云存储直链**：`config/MediaServingHeaderFilter.java:30-34` 只对 `/upload/`、`/api/uploads/`、`/api/public/assets/` 三个本地前缀生效；策略切到 OSS/COS/S3/OneDrive/又拍后 publicUrl 指向云厂商域名，SVG 直链无任何脚本中和。业界范式：上传时禁止 SVG 或强制服务端重编码（最彻底）；云存储方案下依赖 CDN/桶配置加响应头，并在文档写明这条部署要求。
- **P1-13 浏览量统计三重问题**：① `recordFetch` 先读后写非原子（`UploadRecordService.java:170-178`），高并发丢更新——项目内 `AlbumRepository.java:30-32` 已有正确范式（`SET accessCount = accessCount + 1`）却没用在 invokeCount 上；② 无任何防刷，匿名脚本刷 `/upload/xxx` 每次 +1，且每个媒体 GET 触发一次同步 DB 写（写放大 DoS，`storage/UploadInvokeCounterFilter.java:35-55`）；③ `publicBaseUrl` 配成 http(s)（云存储外链）时计数器直接跳过 → **云存储图片浏览量恒为 0**（`UploadInvokeCounterFilter.java:57-63`）。图集 `accessCount` 同样可刷且 `album_access_logs` 记了 IP 却只写不读（`AlbumAccessLogRepository.java:14-20` 全仓库零调用）。业界范式：原子自增 + 内存聚合缓冲定期批量落库（抗写放大）+ 按 (IP|UA哈希, 媒体, 时间窗) 去重（album_access_logs 就是现成骨架，补上唯一约束即可）；云存储场景在图片详情/画廊页计数而非直链计数。

### CI/CD 与部署链路
- **P1-14 CI 后端测试配置自相矛盾**：`.github/workflows/ci.yml:49-53` 注入 `SPRING_DATASOURCE_URL=jdbc:mysql://...`（环境变量优先级高于 yml，会替换 URL），而 `backend/src/test/resources/application.yml:3-13` 硬编码 `org.h2.Driver` + `H2Dialect` → H2 驱动连 MySQL URL，CI 里 backend-test 大概率必红；MySQL service 纯属摆设（本地实测走 H2 正常）。建议：删掉 MySQL service 与环境变量注入，承认 H2；或引入 Testcontainers 真连 MySQL（推荐，能测出 H2 兼容性问题）。
- **P1-15 部署文档站不随仓库分发**：`.github/workflows/deploy-docs.yml:6-8,40,46,54` 依赖 `AstrNest-docs/`，但 `.gitignore:8` 明确忽略该目录（git ls-files 为 0 个文件）——fresh clone 后该工作流必失败，五篇部署文档（`zh/deploy/*.md`）用户根本拿不到；且文档站内端口全线写 8080，实际是 8081（`application.yml:2`）。建议：`AstrNest-docs/` 移出 .gitignore 并跟踪，或删掉 deploy-docs.yml；端口全文订正。
- **P1-16 无任何 Release 产物**：`ci.yml:104,111` 只构建不推送，README.md:325 承认无 tag→Release 工作流——所有用户被迫本机从源码构建（README.md:183 自认首启构建要十几分钟），"五分钟部署"名不副实。业界范式：tag 触发 `docker/metadata-action` + `build-push-action` 推 GHCR（多架构 + gha 缓存），`softprops/action-gh-release` 附 jar / dist zip / 扩展 zip。这是把部署体验拉平的前提。
- **P1-17 init-admin 脚本族（3 脚本 + 2 SQL 模板）多处断裂**：① `init-admin.bat:189` 在 Full init 分支使用只在 Tables-only 分支赋值的 `DB_PASS` → 展开为空密码；② `init-admin.py:140-151` Linux 分支只建库，应用账号密码实际是 `init.sql:8` 硬编码的 `CHANGE_ME_STRONG_PASSWORD`，脚本结尾却打印用户输入的密码（`:488-490`）→ 按提示配必连不上库；③ 两脚本的正则目标（SQL 文件中的管理员 INSERT 行）在现行 SQL 文件里已不存在，永远走降级路径；④ 脚本把管理员 BCrypt 哈希**写回仓库内 SQL 文件**（`init-admin.py:247`、`init-admin.bat:162-170`）→ 用户日后 git add 就把 hash 提交进 fork。建议：安装向导已覆盖其功能，**整体废弃此脚本族**（文档改为只指向向导），或修复上述四点。
- **P1-18 Docker 前端容器不托管 `/upload/**`，默认部署图片直链是坏的**：`frontend/nginx.conf:48-54` 只反代 `/api/`，直链落回 SPA 返回 HTML，README.md:207 要求用户必配宿主 Nginx 才完整可用——与"五分钟部署"直接冲突。compose 里 `VITE_*` 运行时环境变量全是无效配置（Vite 变量构建时注入，`frontend/Dockerfile` 无对应 ARG，`docker-compose.yml:90-93` 是幻觉配置）。建议：前端镜像 nginx.conf 增加 `/upload/` location（挂载同一存储卷）或 compose 里去掉 VITE_* 改 `build.args`。

### 其他 P1
- **P1-19 游客点赞可无限刷、污染两个热门排行**：点赞身份优先采信客户端自报的 `X-Chenxi-Visitor` 头（`gallery/PublicGalleryLikeService.java:155-162`），随机 token 即换身份；POST 无频控。热门图片与 Featured 图集都按 likeCount 排序，全可操纵。建议：删除"信任客户端头"逻辑，一律服务端派生（IP+UA 哈希）；点赞接口加 IP 频控。
- **P1-20 注册用户名无字符白名单**：`chenxi/auth/dto/RegisterAccountRequest.java:17` 只限长度 4-32，空格/HTML 元字符/emoji 均可入库；安装向导却用严格白名单 `^[A-Za-z0-9_.-]{3,32}$`（`install/InstallSetupService.java:191-218`），两套标准。建议注册复用安装向导的正则。

---

## 4. P2 — 应修（不阻塞上线但影响安全/稳定）

| 编号 | 问题 | 位置与要点 |
|---|---|---|
| P2-1 | 500 响应回显内部异常消息 | `common/GlobalExceptionHandler.java:136-140` 返回 `"服务器内部错误: " + ex.getMessage()`，可能带 SQL 片段/路径/类名。改为通用文案，详情只进日志 |
| P2-2 | `/actuator/metrics` 对任意认证用户开放 | prod 暴露 health,metrics（`application-prod.yml:32-36`），SecurityConfig 只 permitAll health/info，metrics 落入 `authenticated()` → 任何 USER 可读运行时指标。收紧为 ADMIN 或关闭 metrics |
| P2-3 | 默认 profile 是 dev | `application.yml:8`：裸 jar 不设 `SPRING_PROFILES_ACTIVE` 时 swagger 匿名 + DEBUG 日志 + `ddl-auto:update`。prod 应为默认或无 profile 时 fail-fast |
| P2-4 | 匿名可探测安装状态详情 | `GET /api/install/status` 匿名返回 DB 版本/表数量/存储绝对路径/Java 版本/SMTP host（`InstallStatusService.java:210-337`），且 `database/test` 响应回显运行时 `spring.datasource.url`（`InstallDatabaseTestService.java:49,66-68,169`） |
| P2-5 | install/database/test 匿名 SSRF | 未锁定时可对任意 `host:port` 发起 MySQL 连接并按结果分类回显（内网端口扫描器），host 未做白名单（`InstallDatabaseTestService.java:205-212`）。配合 P0-4 一起修：加安装令牌 + host 限制 |
| P2-6 | 云凭证明文存 DB | 存储策略 profile 的 secretKey/password/refreshToken 明文落 `config_json`（`storage/profile/StorageStrategyService.java:235-241`，读时掩码）。业界为 KMS/加密存储；至少文档声明风险 |
| P2-7 | 登录用户名枚举（时序侧信道） | 不存在用户时跳过 BCrypt，响应时间可分辨。加哑哈希填充（对不存在用户执行一次假 matches） |
| P2-8 | 验证码比较非常量时间 | `equalsIgnoreCase`（`ChenxiAuthService.java:224`、`ChenxiCaptchaService.java:74`）；TOTP 反而做了常量时间。统一改 MessageDigest.isEqual |
| P2-9 | `confirmTwoFactorSetup` 失败不计入防爆破 | `AuthService.java:103-110` 不捕获不 recordLoginFailure，绑定确认可无限重试（有密码门，风险中低） |
| P2-10 | 2FA 过渡令牌不复查账号状态 | `AuthService.java:142-148`：5 分钟内账号被禁用仍可换发正式 JWT |
| P2-11 | API Key 认证无失败限制 + 配额非原子 | `ApiKeyAuthenticationFilter.java:48-56` 失败直接 401 无锁定，持 publicId 可无限打 BCrypt（CPU 放大 DoS）；`enforceQuota` 读-改-写丢计数（`ApiKeyService.java:226-241`） |
| P2-12 | 公告无乐观锁并发互覆 | `Announcement.java` 无 @Version，`AnnouncementService.update:45-52` 盲写 |
| P2-13 | Featured 全表内存排序 | `AlbumService.java:476-533` 捞全部公开图集内存求和 likeCount 排序，应下沉 DB 聚合 |
| P2-14 | 随机图短链 N+1 | `AlbumService.java:238-244` 逐媒体 findByMediaUuid |
| P2-15 | 自动清理死代码 | `autoCleanupDays` 与 `deleteExpiredRecords`（`UploadRecordService.java:193-201`）无任何调度调用 |
| P2-16 | VideoEmbed 不校验违规/可见状态 | `VideoEmbedController.java:31-41` 按 mediaUuid 渲染，UUID 泄露即可嵌播任意状态视频 |
| P2-17 | 大小限制口径矛盾 | DB 配置视频上限默认 100MB（`SystemConfig.java:27`）vs Spring multipart 全局 20MB 先行拒绝（`application.yml:10-12`），用户配了也没用 |
| P2-18 | 标记违规不可逆 | 取消违规不恢复已删物理文件（`UploadRecordService.java:107-127`） |
| P2-19 | IP 信任配置风险 | `trusted-proxy=true` 时 XFF/X-Real-IP 首值完全信任（`common/ClientIpResolver.java:36-46`），代理未清洗头时可伪造 IP 绕过锁定/恶意拉黑他人 365 天。文档需强调代理必须清洗头 |
| P2-20 | 无 SPA CSP 响应头 | `GlobalSecurityHeaderFilter.java:41-46` 只有 nosniff/XFO/Referrer-Policy/HSTS，HTML 页无 Content-Security-Policy，DOMPurify 失效即裸奔。建议加 `default-src 'self'` 起步的 CSP |
| P2-21 | Content-Disposition 未编码 | `UploadController.java:90` `"inline; filename=" + filename` 未做 RFC 6266 编码，特殊文件名可炸头 |
| P2-22 | `looksLikeSvg` 拒绝合法 XML 声明 | 只认裸 `<svg` 前缀（`ChenxiMediaInspector.java:233-236`），带 `<?xml` 的合法 SVG 被拒（功能 bug） |
| P2-23 | 同文件 inspect 两次 | `UploadService.java:93` 与 `:146` 重复执行魔数校验（性能瑕疵） |
| P2-24 | Schema 四通道漂移 | `init.sql`(78KB) / `install-schema.sql`(26KB，头部自认漂移风险) / `init_windows.sql` / `SchemaAlignmentRunner` 各自为政；README.md:312 拿建库脚本当迁移脚本；`alter_upload_record_add_dimensions.sql` 教建的索引被 `SchemaAlignmentRunner.java:127-129` 当反面教材删除。业界范式：Flyway 单一迁移通道 |
| P2-25 | compose 首启竞态 + 健康检查过短 | `docker-compose.yml:30-31` 无 `condition: service_healthy`；`backend/Dockerfile:40` HEALTHCHECK start-period=5s 对 JVM 远远不够；`:26` 硬编码 jar 文件名 |
| P2-26 | `.env.example` 死配置/误导 | `SMTP_*` 六变量无任何消费者（后端 SMTP 存 DB）；`VITE_API_BASE_URL=http://localhost:8081` 与前端"留空同源"建议矛盾；compose 残留无消费者的 `ASTRNEST_ADMIN_*`（`docker-compose.yml:43-46`） |
| P2-27 | 根目录/仓库卫生 | `backend/package-lock.json`（92 字节空文件）与 `backend/tmp` 误提交；`frontend/.htaccess` 放在 `frontend/` 根不进 dist（应移 `frontend/public/`）；`frontend/.env.production`/`.env.development` 被 gitignore 导致文档指引落空 |
| P2-28 | 前端子路径部署需改源码 | `frontend/src/router/index.js:8` 裸 `createWebHistory()`，README.md:408-412 让每个用户手改源码。直接写 `createWebHistory(import.meta.env.BASE_URL)` |
| P2-29 | open-in-view 默认开启 | 测试日志 WARN 确认；显式设 `spring.jpa.open-in-view=false` |

---

## 5. P3 / 代码卫生（顺手清理）

1. 死字段/死代码：`ChenxiCaptchaTicket.expectedOffset/tolerance`（滑块验证码遗留）、`ChenxiEmailToken.captchaToken` 存了不读、`recordRegisterFailure`/`deleteExpiredRecords`/`AlbumAccessLogRepository` 三个查询方法。
2. dev profile 打开 Security DEBUG（`application-dev.yml:19`），误用于生产会泄露认证细节。
3. `AlbumService.getAvailableMediasForAlbum:374` 仅 owner 可用（admin 反而 403），与其它接口 owner||admin 口径不一；`removeMediaFromAlbum:201-227` 不校验 media 归属；公告删除静默幂等与图集 404 口径不一；公告 `author` 可被请求体伪造（admin-only，低危）。
4. 标签并发创建可能撞 `uq_tags_name` 唯一键导致上传失败（`ChenxiTagService.resolveTags:59-96`）。
5. `Files.probeContentType` 平台差异（Windows/Linux 对 svg/无扩展名行为不一致，`UploadController.java:94-104`）。
6. LIKE 通配符未转义（`ChenxiTagRepository.java:17`，功能性小问题）。
7. CI 杂项：Node 18 已 EOL；无前端 lint job（`lint:check` 脚本存在但 CI 不跑）；无扩展打包 job；无 dependency-review/CodeQL；docker-build 无 gha 缓存且镜像命名不一致（`astrnest-backend` vs `imgbed-frontend`）；workflow 无 `permissions:` 声明与 concurrency。
8. 前端 `vendor-ui` chunk 887KB 超警告线，可做 manualChunks 拆分。
9. 图片 EXIF/GPS 元数据完全不剥离、无服务端重编码（隐私面：用户照片定位信息直接外泄）。业界图床普遍重编码或至少剥离 EXIF。
10. AI 审核依赖公网可回源 URL（`ai/TencentAiService.java:35-44`），本地部署未配 assetDomain 时静默禁用——文档应写明。

---

## 6. 修好的部分（保持现状，别动坏）

- TOTP 全模块（RFC 6238 正确 + 防重放 + 还原码 BCrypt + 常量时间比较）。
- API Key：57 字符表 SecureRandom ≈233bit 熵、BCrypt 入库、掩码回显、`ROLE_API_CLIENT` 最小权限（仅 `POST /api/uploads/**`）。
- `StoragePathGuard` 路径穿越防线（所有读路径接入 + URI 解码二次校验 + 静态映射双保险）。
- 2FA `purpose=2fa` 过渡令牌双向隔离设计（`JwtTokenService.java:82-103` + `JwtAuthenticationFilter.java:58-60`）。
- SSO 影子账号随机密码（杜绝本地口令通道）、introspect fail-closed、PKCE 服务端换 token。
- JWT role 不进 token、每请求实时加载用户（改角色/禁用即时生效）。
- compose 的 `:?` 密码断言、MySQL 绑 127.0.0.1、CORS 默认空 origin、`trusted-proxy` 默认 false。
- 管理端删除同步物理文件（`401a9a1` 已修）；`/embed/video` 页服务端全字段转义干净。
- 原子自增的正确范式已存在于 `AlbumRepository.java:30-32`（修 invokeCount 时照抄即可）。

---

## 7. 修复路线图建议（供审阅后分批执行）

**批次一：止血（P0 全部 + 最疼的 P1，预计一次会话可完成）**
1. P0-1 封面走 `ChenxiMediaInspector` 校验管道（最小 diff：`storeFrontendCover` 前调 `mediaInspector.inspect` + 白名单限定图片扩展名）。
2. P0-2 `PublicUserProfileResponse` 删 email 字段（前端 `UserHomeView` 等如有引用同步删）。
3. P0-3 objectKey 改 `uuid.ext` 命名（本地 + 4 个云 handler 的 `buildObjectKey`/等价函数），原文件名进 `UploadRecord.originalName` 已有字段，展示用。
4. P0-5 删 `httpBasic()`（一行）。
5. P1-1 `.env.example` + docker-compose 补 `ASTRNEST_JWT_SECRET`（带生成指引注释 + `:?` 断言）；prod profile 缺失时启动失败。
6. P2-1 500 改通用文案（一行）。
7. P0-4 安装写端点加一次性安装令牌（env `ASTRNEST_INSTALL_TOKEN`，为空时保持现状兼容升级用户）。

**批次二：认证链路加固（P0-6 + P1-1~P1-6、P1-19、P1-20）**
- token_version 吊销机制 + `/logout`；验证码哈希入库 + 常量时间比较；check-email 限流 + 中性文案；`recordLoginSuccess` 清零逻辑收窄；CAPTCHA_IP 纳入评估 + 注册失败计数 + 四张日志表清理任务；SSO aud 校验；username 白名单；游客点赞去客户端头。

**批次三：上传/统计/内容（P1-7~P1-13、P1-18、P2-11~P2-18）**
- objectKey 随机化的配套（P0-3 完成后此条自动缓解）；配额原子化；删除对账 + 缩略图同步删；jsoup 服务端清洗接入公告/footer/用户资料 URL 字段；invokeCount 原子自增 + 聚合缓冲 + 去重；云存储场景浏览量口径修正；访客上传开关决策（放行或删除）；前端镜像补 `/upload/`。

**批次四：工程化（P1-14~P1-17、P2-24~P2-29 + P3）**
- CI 修测试配置 + Release 工作流（GHCR 镜像 + jar/zip 产物）+ lint/扫描 job；Flyway 引入与 schema 四通道合一；文档站入库 + 端口订正；init-admin 脚本族废弃；router base / .htaccess / 死配置清理。

**批次五：测试补齐（配合以上每批次同步写）**
- 优先补：注册→验证码→写库全链路（MockMvc/H2）、登录+2FA+锁定、上传 inspector（含封面绕过回归）、配额并发、公开端点不含 email 断言、admin 端点权限矩阵、直链可见性。目标先把 5 个测试类扩到覆盖 P0/P1 修复点的回归网。

---

## 8. 本轮核实方式说明

- 5 路走查代理共读源码约 340+ 文件次；**全部 6 个 P0、P1-1/P1-4/P1-14/P2-1 由我本人二次读源码逐行确认**（SecurityConfig、UploadService、AdminUserService、ChenxiAuthController、GlobalExceptionHandler、InstallController、JwtTokenService、UploadController、AbstractS3StorageHandler、PublicUserProfileResponse、ci.yml、test application.yml、.env.example grep）。
- 后端 `mvnw test`（17/17 通过）与前端 `npm run build`（成功）在本机实测。
- 未做动态渗透测试（未起服务打真实请求）；P0-4/P2-5 的竞态类问题基于代码路径推演，置信度高但建议修复后用真实环境复验一次。
