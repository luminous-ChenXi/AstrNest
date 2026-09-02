# AstrNest 插件对接 API 指南

面向希望在插件/扩展中登录并上传图片的场景，本文描述最小可用的 HTTP 接口与鉴权方式。

## 基础信息
- **网关根路径**：`VITE_API_BASE_URL` 环境变量决定，默认 `http://localhost:8080`
- **鉴权头**：`Authorization`，值为登录接口返回的 `token`（示例：`Basic bXl1c2VyOnBhc3N3b3Jk`）
- **Content-Type**：表单上传使用 `multipart/form-data`
- **超时**：前端默认 15s，可在插件端自行控制
- **会话有效期**：滑动 30 天：登录后若 30 天内无登录或 API 请求（如上传）则过期，任意成功请求会刷新 30 天窗口

## 登录获取 Token
- **接口**：`POST /api/auth/login`
- **Body**（JSON）：
  - `username`：用户名或邮箱
  - `password`：密码
- **响应示例**：
```json
{
  "token": "Basic bXl1c2VyOnBhc3N3b3Jk",
  "profile": {
    "id": 12,
    "username": "myuser",
    "displayName": "辰汐用户",
    "email": "user@example.com",
    "avatarUrl": "https://...",
    "roles": ["USER"]
  }
}
```
- **插件侧用法**：缓存 `token`，后续请求在 Header 加 `Authorization: <token>`。建议同存一个 30 天失效时间戳，过期后强制重新登录。

## 上传图片（单/多文件）
- **接口**：`POST /api/uploads`
- **鉴权**：需要登录，Header 里带 `Authorization`
- **参数**：`multipart/form-data`
  - `files`: 多个文件字段（数组），字段名固定为 `files`
  - `tags`: 可选，数组，示例 `tags=nature&tags=travel`
- **示例 cURL**：
```bash
curl -X POST "${API_BASE:-http://localhost:8080}/api/uploads" \
  -H "Authorization: ${TOKEN}" \
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
- `401/403`：未登录或权限不足；重新登录并更新 `Authorization`
- `400`：参数或文件校验失败（大小、类型等）
- `429`：频率/配额受限（如有启用）
- `5xx`：服务器错误，建议稍后重试

## 安全建议
- 全程使用 HTTPS 部署，避免在明文通道传输 `token`
- 插件侧将 `token` 与失效时间一并存储，过期自动清理
- 退出/切换账号时，删除本地 `token` 与缓存的个人信息
- 如需长周期访问，优先申请/使用平台的 API Key 能力（后端提供 `/api/user/api-keys` 进行自助管理）
