# AstrNest 更新日志

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
