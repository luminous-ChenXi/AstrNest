# AstrNest 更新日志

## [Unreleased] - 2026-10-07

### 审计收尾：文档分发 / Flyway 方案 / 预览图口径

- **部署文档入库（P1-15）**：文档站的五篇部署指南（原 `AstrNest-docs/docs/zh/deploy/`，
  该目录被 .gitignore 忽略、从不随仓库分发）收录进 `docs/deploy/`——端口全线订正为实际
  **8081**、示例管理员密码中性化、每篇顶部加时效声明（`ASTRNEST_ADMIN_*` 环境变量方式已被
  安装向导取代）；新增目录 README 说明权威顺序（README.md 为准）
- **Flyway 迁移方案（P2-24）**：新增 `docs/flyway-migration-plan.md` 评估稿——
  四通道现状梳理、V1 基线 + 增量迁移 + 存量库 baseline 切换、SchemaAlignmentRunner 退役路径、
  风险清单（H2/MySQL 方言、安装向导时序）与验收清单；**仅方案未动代码**，实施需确认切换窗口
- **相册预览图 uuid 口径对齐（复查点④）**：公开相册详情与登录态相册详情两处，
  非属主的 `previewImageUuids` 现按「公开∧非违规」过滤，与列表/medias 接口同口径
- 收尾验证：后端 49 测试全绿；前端 build 通过、lint 0 error

## [Unreleased] - 2026-10-06 (5)

### 业务链路二轮复查修复（图片权限矩阵 + 注册登录链路）

- **【高】公开相册私图直链泄露**：`GET /api/albums/{albumUuid}` 此前对公开相册整包返回全部媒体
  的 publicUrl/thumbnailUrl——任意登录用户拿到 albumUuid 即可获得相册内所有私图/违规图直链，
  击穿"私图靠 UUID 不可枚举保护"不变量；现按 owner||admin 旁路 + 逐图公开∧非违规过滤，
  与公开相册端点同构
- **【高】SSO 影子账号永久锁死**：找回密码可命中 SSO 影子号改密并 +tokenVersion，
  而 SSO 登录签发的是 ver=0 令牌 → 该 SSO 用户从此每次登录全部 401；
  现找回密码拒绝非 local 账号，SSO 签发改带真实令牌版本
- **【高】账号级防爆破被变体绕过**：锁定键只 trim 不小写，`User`/`user`/邮箱 变体各算独立窗口；
  统一小写归一
- **【中】热门图片与画廊口径对齐**：top3 此前不过滤相册公开性，私有相册的名称/slug 会经
  匿名热榜外泄；改 JPQL 子查询（无相册 ∨ 相册公开），并修正 limit 参数失效
- **【中】公开档案统计口径**：uploadCount/storageBytes 不再把私图/违规图计入匿名可见的统计
- **【中】批量建号/BCrypt DoS 防护**：/register 与 /login 加进程内限流（10/30 次/分/IP）；
  LoginRequest 密码加 64 位上限（防超大密码体反复触发 BCrypt(12)）
- **【中】用户名白名单三处归一**：注册 4-32 白名单为准，安装向导 3→4 对齐，SSO sanitize
  剥除白名单外字符（中文/元字符不再能经 SSO 绕过入库）
- **评论功能定论**：全仓库从未有过评论链路（点赞 upload_likes 是唯一互动形态）；
  正式移除 `interactions` 表残留（install-schema/init.sql/init_windows.sql 三处）；
  顺带修复 init.sql `users.email NOT NULL` 与实体可空、注册允许无邮箱的漂移
  （否则 init.sql 库上无邮箱单步注册必失败）
- **前端**：注册页用户名校验对齐 4-32；发码成功即清一次性 captchaToken（60s 重发不再必 400）；
  验证码文案去掉写死的"5 分钟"；check-email 参数 encodeURIComponent；
  会话过期时间优先用后端 expiresIn（丢弃写死 30 天）

## [Unreleased] - 2026-10-06 (4)

### 收尾清理与隐私增强

- **移除 init-admin 脚本族（P1-17）**：`init-admin.bat/.cn.bat/.sh/.py` 整体删除——
  Windows 全量初始化分支空密码、Linux 分支密码错位、脚本改写仓库 SQL 污染版本库等缺陷，
  其能力已被六步安装向导完整覆盖；README/README_EN/CONFIG_GUIDE/.env.example/db SQL 注释同步更新，
  管理员创建唯一入口收敛为安装向导（兜底：首个注册用户自动 ADMIN）
- **JPEG EXIF/GPS 隐私剥离（P3）**：新增 `JpegExifStripper`——上传落盘前标记段级剥离
  APP1（EXIF/XMP，含 GPS 定位）、APP13（IPTC）、COM（注释），保留 JFIF/ICC，
  不重编码无画质损失，解析异常 fail-open 保留原文件；主上传与视频封面两条路径都接入，
  新增 `JpegExifStripperTest`（5 例）——测试 44 → 49 全绿

## [Unreleased] - 2026-10-06 (3)

### 批次五：核心链路回归测试 + 探针 H2 兼容

- **新增 `AuthFlowIntegrationTest`（5 例，MockMvc + H2 全链路）**：首个注册用户 ADMIN /
  后续 USER、用户名唯一拒绝、公开档案不含 email（P0-2 回归）、令牌版本 +1 后旧 JWT 被拒
  （P0-6 回归）、改密接口自增版本并换哈希——后端测试 39 → 44 全绿
- **安装状态探针 H2 兼容**：`SELECT VERSION()` 是 MySQL 专属函数，H2 下探针把内存库误判为
  "数据库不可达"→ 全站 503；改为 `SELECT 1` 通用可达性探测 + 版本函数按方言降级（H2VERSION）；
  `information_schema` 查表补 `PUBLIC` schema 兼容（MySQL 无 PUBLIC，互不干扰）
- 该修复让此前"只有 MySQL 才能跑测试"的隐性约束解除，CI 的 H2 口径完整成立

## [Unreleased] - 2026-10-06 (2)

### 批次四：CI/CD 工程化

- **修复 CI 后端测试必红（P1-14，GitHub 上 commit 的 ❌）**：CI 曾注入 MySQL 连接串但
  测试配置硬编码 H2 驱动，互相矛盾——删除 MySQL service 与环境变量注入，测试回归 H2 口径
  （与 README 一致；真 MySQL 验证由 Docker 冒烟承担）
- **前端 lint 进 CI**：`.eslintrc.cjs`（legacy）+ ESLint 9 组合本来就是坏的（`--ignore-path` 已被
  移除），迁移为 `eslint.config.mjs`（flat config，flat/essential + js.recommended + prettier
  skip-formatting，browser/node/chrome globals），并修复既有代码的全部 lint error
  （`.native` 修饰符 ×3、嵌入页 `<\/script>` 改插值拼接、删除引用未定义变量的死函数 activateChannel）
- **新增 Release 工作流（P1-16）**：推送 `v*` tag 自动产出——后端 jar、前端 dist zip、扩展 zip
  附加到 GitHub Release，并发布 GHCR 镜像（backend/frontend，语义化版本 + latest）
- **CI 杂项**：Node 18（已 EOL）→ 22；`setup-java` 自带 maven 缓存替代手动 cache；
  workflow 级 `permissions: contents: read` + concurrency 取消过时任务；docker 构建加 gha 缓存、
  镜像命名统一为 astrnest-*
- **删除死工作流 deploy-docs.yml**：它依赖被 .gitignore 忽略的 `AstrNest-docs/`（fresh clone 必失败，
  审计 P1-15）；文档站仍可本地构建

## [Unreleased] - 2026-10-06

### 批次三余项：内容与展示链路

- **缩略图双前缀修复**：图片/视频列表的 `thumbnailUrl` 出现 `/upload/upload/...`——
  库里存的是公开路径，组装响应时又被当 objectKey 拼了一次 `/upload` 前缀；
  用户端与公开画廊两处同步修复
- **热门图集下沉数据库（P2-13）**：此前捞全部公开图集+媒体+上传记录内存求和排序，
  改原生 SQL 聚合（`SUM(CASE...)` + `GROUP BY` + Top 3），规模化不再性能退化
- **公告乐观锁（P2-12）**：`announcements.version` + JPA `@Version`，
  管理员并发编辑由"后提交者静默覆盖"改为后提交者收到 409；schema 双通道 + 存量对齐
- **jsoup 服务端清洗（P1-11）**：新增 `HtmlSanitizer`——
  footer HTML 走 jsoup 白名单（剥 script/事件属性）；公告 Markdown 源整体 HTML 转义
  （纯字符串处理不破坏 Markdown 结构，代价是公告不再支持内嵌原始 HTML）；
  用户资料 avatarUrl/website 走协议白名单（http/https/站内相对路径，
  `javascript:`/`data:` 一律拒绝入库）——服务端纵深不再全押前端 DOMPurify
- **测试**：新增 `HtmlSanitizerTest`（6），后端测试 33 → 39 全绿

## [Unreleased] - 2026-10-05 (4)

### 修复冒烟测试发现的问题

- **token_version 默认值缺失**：dev 库的 users 表由 Hibernate ddl-auto 建出，
  `token_version` 无默认值导致安装向导的原生 INSERT 在严格模式下报 1364；
  实体补 `columnDefinition = "BIGINT NOT NULL DEFAULT 0"`，
  SchemaAlignmentRunner 对存量库统一 `MODIFY ... DEFAULT 0` 兜底
  （Docker MySQL 8.4 + 全新库安装向导全链路实测通过）

## [Unreleased] - 2026-10-05 (3)

### 上传与统计链路（审计批次三·首批）

- **浏览量原子自增**：`recordFetch` 先读后写在并发下丢更新，改 `UPDATE ... SET invoke_count = invoke_count + 1`
  原子语句；计数失败不影响媒体访问（对齐 AlbumRepository 既有范式）
- **访客上传复活（P1-8）**：`POST /api/uploads` 放行匿名，由控制器内 `guestUploadEnabled` 开关 +
  IP 配额裁决——此前安全链要求必须登录，安装向导里的「允许访客上传」是完全不可达的死功能
- **随机图短链 N+1 修复**：逐条 `findByMediaUuid` 改 `findByMediaUuidIn` 批量取回可见性
- **视频嵌入违规拦截（P2-16）**：`/embed/video/{uuid}` 拒绝违规（violation）与未公开视频，
  此前 UUID 泄露即可绕过列表层过滤嵌播任意状态视频

## [Unreleased] - 2026-10-05 (2)

### 认证链路加固（审计批次二：P0-6 + P1 系列）

- **JWT 服务端吊销（P0-6）**：users 表新增 `token_version`，JWT 携带 `ver` claim，
  `JwtAuthenticationFilter` 与数据库实时比对——**改密码/找回密码后该用户全部旧令牌立即失效**
  （含可能已被盗的会话）；schema 三通道同步（install-schema.sql / init.sql / SchemaAlignmentRunner），
  新增 `ChenxiUserDetails` 携带版本信息
- **验证码/链接令牌哈希入库（P1-2）**：邮箱验证码、注册链接令牌、图形验证码改为只存 SHA-256
  （列宽 6/16→64 双通道扩容），明文仅随邮件/图片出现一次；比较改 `MessageDigest.isEqual` 恒定时间
- **邮箱枚举收敛（P1-3）**：注册发码/找回发码对不存在/已存在邮箱一律返回中性文案不再报错；
  `check-email` 与验证码图片端点加进程内滑动窗口限流（10/20 次/分/IP，新增 `InMemoryRateLimiter`）
- **爆破锁定补漏（P1-4/P1-5）**：注册/找回密码成功不再清零 IP 维度锁定（堵住"注册小号自助解锁"旁路），
  新增 `recordRegistrationSuccess` 只清账号维度；注册失败接入 `recordRegisterFailure`（原死代码）；
  `CAPTCHA_IP` 锁（10 次失败锁 24h）纳入评估（原先只写不读）
- **游客点赞去客户端头（P1-19）**：访客身份一律服务端 IP+UA 指纹派生，不再信任自报
  `X-Chenxi-Visitor` 头（随机换头即可无限刷赞并操纵热门排行）
- **注册用户名白名单（P1-20）**：`^[A-Za-z0-9_.-]+$`，与安装向导同一口径
- **数据保鲜（P1-5）**：新增 `SystemDataHousekeeping` 每日 04:30 清理邮件验证码（>1 天）、
  验证码票据（>1 天）、图集访问日志（>30 天）、安全日志（>90 天）与过期上传记录（按 autoCleanupDays），
  顺带清理限流器空窗口——此前四张表只增不删

## [Unreleased] - 2026-10-05

### 安全止血（第二轮全链路审计 P0 修复，报告见 docs/audit-report-2026-10-05.md）

- **视频封面校验补口**：`videoCovers` 封面文件此前完全绕过 `ChenxiMediaInspector`（任意扩展名/内容
  可落盘并被同源直出，存储型 XSS 面）；现封面与主文件走同一校验管道（扩展名/MIME/魔数/大小），
  校验不通过自动回退 FFmpeg 截帧
- **公开用户档案不再返回邮箱**：`GET /api/public/users/{id}`（匿名可达）移除 email 字段，
  前端公开主页同步移除 mailto 展示——防止未认证批量收割注册邮箱
- **对象 key 统一「日期目录 + 随机文件名」**：本地与 S3/OSS/又拍/OneDrive 全部后端不再用
  「年/月/原始文件名」——对象存储 put 覆盖语义下同名文件会跨用户互相覆盖，且直链可枚举；
  原始文件名仍保存在 upload_records 与 API 响应中用于展示（新增 `StorageObjectKeys` 统一生成）
- **移除 HTTP Basic 兼容通道**：Basic 认证失败不经过防爆破锁定，等于给暴力破解留无限速旁路；
  机器场景由 API Key 承担，Swagger 文档同步改为 Bearer + X-API-Key 双方案
- **JWT 密钥生产强制**：prod profile 下 `astrnest.jwt.secret` 缺失或不足 32 字符直接拒绝启动
  （此前只打 WARN 并用临时随机密钥——重启全员掉线）；`.env.example` 补 `ASTRNEST_JWT_SECRET`，
  compose 注入并加 `:?` 空值断言
- **安装向导令牌保护**：部署时可配置 `CHENXI_INSTALL_TOKEN`，向导全部写操作必须携带
  `X-Chenxi-Install-Token` 请求头，防止公网部署窗口期被陌生人抢注第一个用户（自动 ADMIN）接管站点；
  安装页新增令牌输入框，留空=不启用（本地/内网部署兼容）
- **快赢项**：500 响应不再回显内部异常消息；`Content-Disposition` filename 走 RFC 6266 编码；
  SVG 魔数兼容 `<?xml` 声明（带声明的合法 SVG 不再被拒）；关闭 OSIV；删除 `.env.example` 的
  SMTP 死配置与 compose 的 `ASTRNEST_ADMIN_*` 死变量；compose MySQL 健康检查 + backend
  `depends_on: service_healthy`（消除首启竞态）；Dockerfile 健康检查 start-period 5s→60s、jar 名通配化
- **测试**：新增 `ChenxiMediaInspectorTest`（6）、`StorageObjectKeysTest`（6）、`InstallTokenGuardTest`（4），
  后端测试 17 → 33 全绿

## [Unreleased] - 2026-09-27

### 安装向导对齐与 N1 管理员私密保障

- 安装向导对齐统一规范六步流程：环境检测 → 数据库配置 → 初始化 → 站点配置 → 创建管理员 → 完成；
  新增「数据库配置」连接确认步与「站点初始配置」步（开放注册/访客上传/单文件上限/加速域名，可跳过）
- 管理员密码支持一键生成 16 位强密码（含大小写/数字/符号），明文仅生成时展示一次 + 二次输入确认；
  日志、API 响应、完成页一律不回显；`init-admin.bat` 不再在完成输出中打印明文密码
- 防重装升级为四重防护：DB 完成标记 + `storage/install.lock` 锁文件 + 向导写端点 403 + 前端路由跳转；
  安装完成按钮强制跳转登录页

### 辰汐通行证登录收口（chenxi.passport.*）

- 配置命名空间由 `astrnest.sso.*` 收口为 `chenxi.passport.*`，默认关闭、关闭时行为与现状一致；
  scopes 默认收紧为 `openid,profile`；docker-compose 同步透传 `CHENXI_PASSPORT_*` 环境变量
- exchange 接口支持 OIDC 授权码 + PKCE（`{code, codeVerifier}`，服务端完成换 token，浏览器不接触通行证
  token），保留遗留 access_token 直换路径兼容旧前端；前端 sso.js 切换为服务端交换
- 本地令牌对齐 `access-token-days: 30` 不活动过期：剩余有效期不足一半时经
  `X-AstrNest-Refreshed-Token` 响应头滑动续期，前端自动滚动会话

### 授权与统计 SDK 骨架（N2 占位）

- 新增 license 包：`LicenseClient`（启动 + 定时校验 verify-url，缓存 `cache-days`，离线 `offline-grace-days`
  内放行，超期仅横幅提示**绝不锁数据**）、`StatsReporter`（默认关闭空实现）、配置绑定与 DTO，
  verify/report 协议只到接口层；新增公开只读端点 `GET /api/license/status` 与前端提示横幅

### 文档

- 新增 `docs/chenxi-integration.md`：配置表、OIDC 流程、令牌 30 天滑动与授权按年的解耦设计、N2 预留说明；
  CONFIG_GUIDE / README 同步 `chenxi.*` 配置键

### 协议与合规

- 协议追加非商用附加条款（禁商用+署名），清理废弃分仓引用

---

## [Unreleased] - 2026-02-27

### 更新主题：前端暗色主题适配与移动端优化

---

### 一、主题切换器优化

**修改文件：**
- `frontend/src/components/common/ThemeSwitcher.vue`
- `frontend/src/composables/useTheme.js`

**变更内容：**
1. **手机端隐藏主题切换器** - 添加 `hidden md:flex` 类，在移动端（<768px）自动隐藏
2. **默认自动跟随系统** - 添加 `initialValue: 'auto'`，默认根据用户设备系统主题自动切换明暗模式

---

### 二、用户页面暗色主题适配

**修改文件：**
- `frontend/src/views/user/UserImagesView.vue`
- `frontend/src/views/user/UserProfileView.vue`
- `frontend/src/views/user/UserSecurityView.vue`
- `frontend/src/views/user/UserApiManagerView.vue`

**变更内容：**
将所有硬编码颜色替换为 CSS 变量，支持明暗主题切换：

| 原硬编码 | 替换为 |
|---------|--------|
| `text-white/80` | `text-body-secondary` |
| `text-white/70` | `text-body-muted` |
| `text-white/60` | `text-body-soft` |
| `text-white/50`, `text-white/40` | `text-body-faint` |
| `text-white` | `text-body-primary` |
| `bg-white/5` | `bg-surface-overlay` |
| `bg-black/30`, `bg-black/20` | `bg-surface-strong` |
| `border-white/10`, `border-white/15` | `border-body` |

**涉及页面：**
- 仪表盘（UserHomeView.vue）
- 媒体管理（UserImagesView.vue）
- 资料信息（UserProfileView.vue）
- 安全设置（UserSecurityView.vue）
- API 接口管理（UserApiManagerView.vue）

---

### 三、移动端布局优化

**修改文件：**
- `frontend/src/views/user/UserHomeView.vue`
- `frontend/src/layouts/UserLayout.vue`

**变更内容：**
1. **统计卡片** - 手机端改为 3 列紧凑布局，字体和间距适配
2. **配额卡片** - 圆角和间距响应式调整
3. **上传区域** - 添加上传图标，按钮和间距更紧凑
4. **最近上传** - 图片高度手机端 `h-36`，桌面端 `h-48`
5. **整体间距** - 所有区域 padding、margin 响应式调整

---

### 四、下拉菜单主题适配

**修改文件：**
- `frontend/src/assets/styles/chenxi-interactions.css`
- `frontend/src/layouts/UserLayout.vue`

**变更内容：**
1. **背景色** - 从 `rgba(17, 25, 40, 0.95)` 改为 `var(--panel-overlay)`
2. **边框** - 从 `rgba(255, 255, 255, 0.1)` 改为 `var(--border-soft)`
3. **文字颜色** - 使用 `var(--text-body-secondary)` 等变量
4. **悬停效果** - 使用 `var(--color-bg-strong)` 和 `var(--color-text-primary)`

---

### 五、修复汇总

| 问题 | 修复方式 |
|-----|---------|
| 暗色主题字体看不见 | 所有硬编码颜色替换为 CSS 变量 |
| 手机端主题切换器突兀 | 添加 `hidden md:flex` 隐藏 |
| 用户页面无主题适配 | 5 个用户页面全部使用变量类 |
| 下拉菜单无主题适配 | 样式文件使用 CSS 变量 |

---

### 测试建议

1. 在系统设置中切换明暗主题，验证所有页面正常显示
2. 使用手机浏览器访问，验证主题切换器已隐藏
3. 验证用户头像下拉菜单在明暗主题下的显示效果
4. 检查所有按钮、输入框、卡片的边框和背景色

---

## [2026-02-24] - 移动端适配与主题系统优化

### 功能更新概览

本次更新主要围绕**移动端适配性优化**和**主题系统一致性**两大主题，解决了手机端组件显示异常、暗色主题字体丢失等问题，同时全面规范化CSS变量使用，提升代码可维护性。

### 详细更新内容

#### 1. 移动端适配性改进

##### 1.1 公开图片组件显示修复
- **问题描述**：手机端主页公开图片组件丢失，网格布局在移动端显示异常
- **优化后**：使用响应式网格 `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`，移动端单列显示
- **实现位置**：`frontend/src/components/public/PublicGalleryGrid.vue`

##### 1.2 导航栏移动端适配
- **问题描述**：移动端导航菜单背景色硬编码，暗色主题下显示异常
- **优化后**：统一使用 `var(--glass-bg)` 和 `var(--border-soft)`，自动适配主题
- **实现位置**：`frontend/src/views/PublicLandingView.vue`

##### 1.3 用户布局标签页优化
- **问题描述**：暗色主题下用户中心标签页文字对比度不足，难以辨识
- **优化后**：使用 `var(--color-text-primary)` 和 `var(--color-text-secondary)` 确保可读性
- **实现位置**：`frontend/src/layouts/UserLayout.vue`

#### 2. 主题系统规范化

##### 2.1 硬编码颜色值清理
- **问题描述**：大量使用 `#F9A8C8`、`#E87A9F`、`rgba(255,255,255,x)` 等硬编码颜色
- **优化后**：统一替换为CSS变量：
  - 主色调 → `var(--color-brand-primary)`
  - 强调色 → `var(--color-brand-accent)`
  - 背景色 → `var(--glass-bg)` / `var(--chip-bg)`
  - 边框色 → `var(--glass-border)` / `var(--chip-border)`
- **影响范围**：10+ 组件文件，50+ 处样式定义

##### 2.2 认证卡片主题适配
- **问题描述**：认证卡片使用硬编码白色文字，暗色主题下不可见
- **优化后**：使用 `text-body-primary`、`text-body-muted` 等语义化类名
- **实现位置**：`frontend/src/components/chenxi/ChenxiAuthCard.vue`

##### 2.3 主题切换器优化
- **问题描述**：主题切换按钮使用硬编码背景色，需要单独定义暗色主题样式
- **优化后**：使用 `var(--color-bg-strong)`、`var(--border-soft)`，简化代码结构
- **实现位置**：`frontend/src/components/common/ThemeSwitcher.vue`

#### 3. 响应式布局增强

##### 3.1 痛点卡片网格适配
- **优化前**：固定3列网格，移动端显示拥挤
- **优化后**：移动端单列 `@media (max-width: 768px) { grid-template-columns: 1fr; }`
- **实现位置**：`frontend/src/views/PublicLandingView.vue`

##### 3.2 统计区域适配
- **优化内容**：统计数字颜色统一使用 `var(--color-brand-primary)`
- **优化内容**：统计卡片背景使用 `var(--chip-bg)`，确保主题一致性

### 前端变更文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `frontend/src/views/PublicLandingView.vue` | 修改 | 修复导航栏、痛点卡片、信任区域、联系区域等样式，移除硬编码颜色 |
| `frontend/src/components/public/PublicGalleryGrid.vue` | 修改 | 优化响应式网格布局，修复分页面板样式 |
| `frontend/src/layouts/UserLayout.vue` | 修改 | 修复标签页暗色主题样式 |
| `frontend/src/views/user/UserHomeView.vue` | 修改 | 修复配额卡片暗色主题样式 |
| `frontend/src/components/chenxi/ChenxiAuthCard.vue` | 修改 | 替换硬编码白色文字为CSS变量 |
| `frontend/src/components/common/ThemeSwitcher.vue` | 修改 | 使用CSS变量简化主题适配逻辑 |

### 代码规范改进

#### CSS变量使用规范
```css
/* 优化前 - 硬编码颜色 */
.pain-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(249, 168, 200, 0.2);
}

/* 优化后 - CSS变量 */
.pain-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}
```

#### 主题适配简化
```css
/* 优化前 - 需要单独定义暗色主题 */
.landing-header {
  background: rgba(255, 255, 255, 0.7);
}
.dark .landing-header {
  background: rgba(5, 6, 12, 0.7);
}

/* 优化后 - 变量自动适配 */
.landing-header {
  background: var(--glass-bg);
}
```

### 用户体验改进

#### 移动端浏览体验
- 公开图片网格自适应屏幕宽度
- 导航菜单在暗色主题下清晰可见
- 所有卡片组件正确显示背景和边框

#### 暗色主题一致性
- 所有文字使用语义化颜色变量，确保对比度
- 背景色使用玻璃态效果，提升视觉层次
- 品牌色统一使用主色调变量，保持视觉一致性
