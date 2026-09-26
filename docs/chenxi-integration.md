# 辰汐通行证（Chenxi Passport）登录集成说明

AstrNest 支持使用"辰汐通行证"账号一键登录本站。该功能为**可选增强**，默认**完全关闭**（`chenxi.passport.enabled=false`），不影响任何现有功能——本地注册/登录、API Key、访客上传等行为与未集成时完全一致。

> 本文同时覆盖与通行证同批规划的**正版授权校验**（`chenxi.license.*`）与**统计上报**（`chenxi.stats.*`）两个 N2 占位项的口径。

## 这是什么

辰汐通行证是辰汐生态的统一身份服务（闭源主站维护）。AstrNest 作为生态内的开源图床/媒体管理站点，通过它实现"一个辰汐账号，登录所有站点"。

## 为什么是标准 OAuth 2.1 / OIDC + PKCE

本站**没有**对接任何通行证私有接口，而是完整实现标准 OIDC **授权码模式 + PKCE（S256）**：

- 通行证侧要求 PKCE S256 强制启用（`plain` 会被拒绝），授权码即使被拦截也无法单独换 token；
- AstrNest 是**公共客户端（public client）**：浏览器中无法安全保存密钥，因此整个功能**没有任何 client_secret**，全部依赖 PKCE 保护授权码交换；
- 换 token 在**本站后端**完成（服务端交换）：浏览器只经手 `code + codeVerifier`，通行证 access_token 全程不出现在浏览器，也不依赖身份源的 CORS 配置；
- 好处是双向的：本站既能接辰汐通行证，也能无缝切换/并存于任何标准 OIDC 提供商（Keycloak、Auth0、Logto 等）。

## 协议交互流程

```
浏览器                          AstrNest 后端                      辰汐通行证
  │  1. GET /api/auth/sso/config                                           │
  │ ────────────────────────>│  （返回 issuer/client_id 等公开门牌信息）      │
  │  2. 生成 code_verifier/state/nonce（sessionStorage，一次性）              │
  │  3. 跳转授权页                                                            │
  │ ───────────────────────────────────────────────────────────────────────> │
  │  4. 用户登录并授权，重定向回 /auth/sso/callback?code=...&state=...          │
  │ <─────────────────────────────────────────────────────────────────────── │
  │  5. 校验 state，POST /api/auth/sso/exchange {code, codeVerifier}           │
  │ ────────────────────────>│  6. POST {issuer}/oauth2/token（PKCE 换取）    │
  │                          │ ────────────────────────────────────────────> │
  │                          │  7. POST {issuer}/oauth2/introspect（自省）    │
  │                          │  8. GET {issuer}/userinfo（拉取 claims）       │
  │                          │ ────────────────────────────────────────────> │
  │                          │  9. 按 sso_sub 匹配/创建影子账号，签发本站 JWT   │
  │ 10. 拿到与本地登录完全一致结构的响应，写本地登录态，跳回来源页                  │
  │ <────────────────────────│                                               │
```

会话始终由**本站自己的 JWT** 承载；通行证的 access_token 是不透明的，本站自省+拉取 userinfo 后**用完即弃**，不缓存、不刷新。

## 令牌 30 天滑动 vs 授权按年：解耦设计

两条生命周期刻意**互不绑定**：

| 口径 | 本站会话令牌（`chenxi.passport.access-token-days`） | 辰汐生态授权（`chenxi.license.*`，N2） |
| --- | --- | --- |
| 计量方式 | **30 天不活动过期**（默认） | **按年**授权 |
| 刷新方式 | **滑动刷新**：每次携带有效令牌访问，若剩余有效期不足一半，后端签发新 token 放入 `X-AstrNest-Refreshed-Token` 响应头，前端滚动替换本地会话 | 缓存期内（`cache-days`）免校验，到期前静默续验 |
| 过期后果 | 活跃用户永不掉线；完全 30 天不活动才需要重新登录 | 超离线宽限期仅展示横幅提醒，**绝不锁数据** |

即：通行证授权是否有效，不影响本站已签发 JWT 的使用；本站令牌续期，也不需要重新走通行证授权。

## 后端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/sso/config` | 返回登录配置（`enabled/issuer/clientId/redirectUri/scopes` + authorize/token 端点），无论是否启用都可访问。公共客户端没有密钥，这些都是可公开的"门牌"信息 |
| POST | `/api/auth/sso/exchange` | 请求体 `{code, codeVerifier}`（推荐，服务端交换）或遗留 `{accessToken}`；后端完成换 token、自省、userinfo、影子账号后，返回与本地登录**完全一致**的响应结构 |
| GET | `/api/license/status` | 授权状态摘要（公开只读，默认 `enabled=false`；N2 占位） |

## 配置项（application.yml → `chenxi.*`）

### 通行证登录 `chenxi.passport.*`

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `chenxi.passport.enabled` | `CHENXI_PASSPORT_ENABLED` | `false` | 是否启用通行证登录（默认关闭） |
| `chenxi.passport.issuer` | `CHENXI_PASSPORT_ISSUER` | 空 | 通行证签发方地址；本地联调填通行证本机地址 |
| `chenxi.passport.client-id` | `CHENXI_PASSPORT_CLIENT_ID` | 空 | 在通行证注册的客户端 ID |
| `chenxi.passport.redirect-uri` | `CHENXI_PASSPORT_REDIRECT_URI` | 空 | 回调地址，必须与通行证注册的**完全一致** |
| `chenxi.passport.scopes` | `CHENXI_PASSPORT_SCOPES` | `openid,profile` | 授权范围 |
| `chenxi.passport.access-token-days` | `CHENXI_PASSPORT_ACCESS_TOKEN_DAYS` | `30` | 本地令牌不活动过期天数（滑动刷新） |

### 正版授权 `chenxi.license.*`（N2 占位，服务端未实现）

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `chenxi.license.enabled` | `CHENXI_LICENSE_ENABLED` | `false` | 正版授权校验总开关 |
| `chenxi.license.verify-url` | `CHENXI_LICENSE_VERIFY_URL` | 空 | 授权校验端点（N2 协议确定后填写） |
| `chenxi.license.cache-days` | `CHENXI_LICENSE_CACHE_DAYS` | `7` | 校验结果本地缓存天数 |
| `chenxi.license.offline-grace-days` | `CHENXI_LICENSE_OFFLINE_GRACE_DAYS` | `3` | 离线宽限天数（超期仅横幅提示） |

### 统计上报 `chenxi.stats.*`（N2 占位）

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `chenxi.stats.enabled` | `CHENXI_STATS_ENABLED` | `false` | 统计上报总开关（false 时为空实现，不收集任何数据） |
| `chenxi.stats.report-url` | `CHENXI_STATS_REPORT_URL` | 空 | 上报端点（N2 协议确定后填写） |

启用示例（生产环境只需设置环境变量，docker-compose 已透传同名变量）：

```bash
CHENXI_PASSPORT_ENABLED=true
CHENXI_PASSPORT_CLIENT_ID=astrnest-web
CHENXI_PASSPORT_REDIRECT_URI=https://your-site.com/auth/sso/callback
```

## 在通行证侧注册客户端

在辰汐闭源主站的 `PassportClientSeeder` 中注册一个公共客户端：

- **client_id**：建议使用 `astrnest-web`
- **client_secret**：留空（公共客户端）
- **redirect_uri**（必须逐字符一致，生产环境追加正式域名）：
  - `http://localhost:5175/auth/sso/callback`（前端开发服务器）
- **授权范围**：`openid profile`
- **PKCE**：强制 S256

## 影子账号与数据库

- 影子账号机制：通行证用户首次登录时在 `users` 表自动建档——`sso_sub`（唯一索引 `uk_users_sso_sub`）关联身份源唯一标识，`identity_source=passport` 标记来源；用户名冲突自动追加后缀，邮箱冲突时用 `<sub>@sso.local` 占位；密码为随机 UUID 的 BCrypt 哈希，**设计上即无法用密码登录**；
- 每次登录顺带同步昵称/头像/邮箱（以身份源为准，本地只读）；
- **全新安装**：无需任何操作，安装向导的 `install-schema.sql` 与启动期 `SchemaAlignmentRunner` 都会补齐 `sso_sub` / `identity_source` 列与唯一索引；
- 影子账号角色为 USER，配额与邮箱注册的普通用户一致。

## 安全说明

- **仓库中没有任何密钥**：公共客户端只有公开的 `client_id`，不存在需要保密的配置；
- **吊销/封禁**：本站侧将影子账号 `active` 置为 0 即拒绝其登录；通行证侧吊销授权则直接无法完成 OAuth 流程；
- **防 CSRF / 重放**：回调 `state` 一次性校验；`code_verifier` 读取后立即从 sessionStorage 清除；
- **防滥用**：exchange 接口按 IP 限流（每分钟 10 次），与本地登录的防爆破锁互不耦合；
- **fail-closed**：自省请求任何失败（网络/超时/解析）都按凭证无效处理。

## N2 预留说明（有意不做的事）

- **授权校验服务端不做**：`chenxi.license.*` 仅落地 SDK 骨架——启用时启动 + 每 6 小时调用 verify-url，结果缓存 `cache-days`，离线 `offline-grace-days` 内放行；**任何状态都不会锁数据**，超期仅由 `/api/license/status` 驱动前端横幅提醒。verify 请求/响应协议只到接口层（`LicenseVerifyRequest` / `LicenseVerifyResult`），最终协议由"辰汐授权服务"在 N2 确定；
- **统计上报服务端不做**：`chenxi.stats.*` 默认关闭即空实现；协议同样只到接口层（`StatsReportRequest`）；
- 不做通行证 token 的刷新/缓存：会话由本站 JWT 承载，通行证 token 用完即弃；
- 不提供"已有本地账号绑定通行证"的 UI：如需绑定，管理员可在数据库中为该账号补填 `sso_sub`；
- 未做账号注销联动：通行证侧注销不影响本站已签发的 JWT（按不活动过期自然失效）。

## 相关代码

| 位置 | 说明 |
| --- | --- |
| `backend/src/main/java/com/chenxi/astrnest/passport/` | 配置、服务与控制器（`ChenxiPassportProperties` / `PassportClient` / `SsoIdentityService` / `SsoAuthController`） |
| `backend/src/main/java/com/chenxi/astrnest/license/` | 授权/统计 SDK 骨架（`ChenxiLicenseProperties` / `LicenseClient` / `StatsReporter` / `LicenseStatusController`） |
| `backend/src/main/java/com/chenxi/astrnest/security/jwt/` | 30 天不活动过期 + 滑动续期（`JwtTokenService` / `JwtAuthenticationFilter`） |
| `frontend/src/services/sso.js` | PKCE 工具（Web Crypto S256、state/nonce、startChenxiLogin、服务端交换） |
| `frontend/src/services/license.js` + `components/common/LicenseBanner.vue` | 授权状态获取与横幅 |
| `docs/chenxi-integration.md` | 本文 |
