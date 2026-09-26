# AstrNest 插件对接 API 指南

面向希望在插件/扩展中登录并上传图片的场景，本文描述最小可用的 HTTP 接口与鉴权方式。

## 基础信息
- **网关根路径**：生产环境与前端同源，走 nginx 反向代理的 `/api` 路径（如 `https://your-domain/api`）；本地开发可用 `http://localhost:8081` 直连后端
- **鉴权头**：`Authorization`
  - **主通道（推荐）**：`Authorization: Bearer <JWT>`，JWT 为登录接口返回的 `token`
  - **兼容通道**：`Authorization: Basic <base64(username:password)>`，为便于已有插件迁移而保留
- **Content-Type**：表单上传使用 `multipart/form-data`
- **超时**：前端默认 15s，可在插件端自行控制
- **会话有效期**：JWT 默认 30 天不活动过期（服务端 `chenxi.passport.access-token-days` 可配），过期后重新调用登录接口获取新 token；不存在"长期缓存"的会话

## 登录获取 Token
- **接口**：`POST /api/auth/login`
- **Body**（JSON）：
  - `username`：用户名或邮箱
  - `password`：密码
- **响应示例**：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJteXVzZXIiLCJ1aWQiOjEyfQ.xxxx",
  "profile": {
    "id": 12,
    "username": "myuser",
    "displayName": "辰汐用户",
    "email": "user@example.com",
    "avatarUrl": "https://...",
    "roles": ["USER"]
  },
  "tokenType": "Bearer",
  "expiresIn": 259200
}
```
- **字段说明**：
  - `token`：纯 JWT 字符串（HS256 签名，无前缀）
  - `tokenType`：固定 `Bearer`
  - `expiresIn`：有效期（秒），默认 259200（72 小时）
- **插件侧用法**：后续请求在 Header 加 `Authorization: Bearer <token>`（即 `tokenType + ' ' + token`）。不要长期缓存 token：到期（`expiresIn`）或收到 `401` 后重新登录即可。旧版接口曾返回 `Basic xxx` 形式的 token，若插件仍持有旧格式，可原样放入 `Authorization` 继续使用（兼容通道），但建议尽快改为 JWT 流程。

## 上传图片（单/多文件）
- **接口**：`POST /api/uploads`
- **鉴权**：需要登录，Header 里带 `Authorization: Bearer <JWT>`（或兼容的 Basic）
- **参数**：`multipart/form-data`
  - `files`: 多个文件字段（数组），字段名固定为 `files`
  - `tags`: 可选，数组，示例 `tags=nature&tags=travel`
- **示例 cURL**：
```bash
curl -X POST "${API_BASE:-http://localhost:8081}/api/uploads" \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "files=@/path/to/photo1.jpg" \
  -F "files=@/path/to/photo2.png" \
  -F "tags=astronomy" \
  -F "tags=nebula"
```
- **响应字段（`UploadResponse`）要点**：
  - `fileName` / `originalFileName`
  - `objectKey`: 存储键，可用于直链访问
  - `publicUrl`: 对外公开访问 URL（若资源公开）
  - `thumbnailUrl`: 缩略图 URL（如有生成）
  - `embedUrl`: Markdown/HTML 嵌入用 URL
  - `size`: 字节数
  - `uploadedAt`: 上传时间戳
  - `publicAccessible`: 是否公开
  - `likeCount` / `invokeCount`: 点赞、调用次数
  - `tags`: 绑定标签列表

## 获取文件
- **接口**：`GET /api/uploads/{objectKey}`
- **鉴权**：公开资源无需；私有资源需具备权限或正确的直链策略。
- **响应**：二进制文件流，`Content-Disposition: inline`。

## 错误码与处理
- `401/403`：未登录、token 过期或权限不足；重新登录获取新 `token` 并更新 `Authorization`
- `400`：参数或文件校验失败（大小、类型等）
- `429`：频率/配额受限（如有启用）
- `5xx`：服务器错误，建议稍后重试

## 安全建议
- 全程使用 HTTPS 部署，避免在明文通道传输 `token`
- JWT 自带过期时间（默认 72 小时），插件端按 `expiresIn` 判断即可，无需自行维护失效时间戳，也不要长期缓存 token
- 退出/切换账号时，删除本地 `token` 与缓存的个人信息
- 如需长周期、无人值守的访问，优先申请/使用平台的 API Key 能力（后端提供 `/api/user/api-keys` 进行自助管理）
