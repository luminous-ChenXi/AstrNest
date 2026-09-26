# AstrNest 图床浏览器扩展

AstrNest 浏览器扩展，支持快速上传图片到自部署的 AstrNest 站点。

## 功能特性

- **上传浮窗**：在任意网页显示上传浮窗，支持拖拽上传
- **用户头像**：显示当前登录用户头像，点击可跳转登录/个人资料
- **右键上传**：右键点击图片可直接上传
- **截图上传**：支持网页截图并上传
- **链接复制**：上传成功后自动复制图片链接到剪贴板

## 安装方法

### 开发者模式安装

1. 打开 Chrome 浏览器，访问 `chrome://extensions/`
2. 开启右上角的「开发者模式」
3. 点击「加载已解压的扩展程序」
4. 选择 `extension` 文件夹

### Chrome 应用商店

（待发布）

## 部署配置（自部署必读）

扩展默认连接本地开发环境 `http://localhost:8081`。指向自己的服务器时：

### 1. 修改服务器地址（弹窗内即可完成）

点击扩展图标，在弹窗底部「服务器地址」输入框填入你的后端地址（如 `https://your-domain.com`）后保存。该配置存储在 `chrome.storage.sync`，无需改代码、无需重新加载扩展。

### 2. 调整 manifest.json 的域名白名单（JSON 不支持注释，故在此说明）

若使用 HTTPS 域名访问你的站点，建议同步修改 `manifest.json`：

- `host_permissions`：追加 `"https://your-domain.com/*"`，否则后台脚本无法直接 `fetch` 你的站点；
- `externally_connectable.matches`：追加 `"https://your-domain.com/*"`，允许站点页面通过 `chrome.runtime.sendMessage` 与扩展通信（用于登录态同步）；
- 页面登录态同步的来源白名单同时受扩展内 `allowedOrigins` 配置控制（见下文「与主站对接」），默认仅含扩展当前配置的后端地址来源。

修改 `manifest.json` 后需在 `chrome://extensions/` 点击扩展的「重新加载」。

## 文件结构

```
extension/
├── manifest.json      # 插件配置
├── background.js      # Service Worker 后台脚本（type: module）
├── config.js          # 共享配置（后端地址 / 站点白名单的读取与保存）
├── content.js         # 内容脚本（注入页面）
├── content.css        # 内容脚本样式
├── popup.html         # 弹出窗口
├── popup.js           # 弹出窗口脚本
├── offscreen.html     # 离屏文档（剪贴板操作）
├── icons/             # 图标文件
│   ├── icon-16.png
│   ├── icon-32.png
│   ├── icon-48.png
│   └── icon-128.png
└── README.md
```

`create-icons.ps1` / `create-logo-icons.ps1` / `generate-icons.js` / `icon-generator.html` 是图标生成工具脚本，贡献者可用它们从源图重新生成 `icons/`。

## 与主站对接

### 认证同步

插件通过 `chrome.storage.local` 存储用户认证信息：
- `astrnest_auth_token`: JWT Token
- `astrnest_auth_profile`: 用户信息
- `astrnest_auth_expires_at`: 过期时间

### API 接口

- 登录: `POST /api/auth/login`
- 上传: `POST /api/uploads`（访客上传开关跟随站点「系统配置」的游客上传设置）
- 图库: `GET /api/gallery`

### 主站登录同步

主站登录成功后，可通过 `chrome.runtime.sendMessage` 将 token 同步到插件（仅 `externally_connectable` 与 `allowedOrigins` 白名单内的站点会被接受）：

```javascript
chrome.runtime.sendMessage(
  extensionId,
  {
    action: 'syncAuth',
    data: { token, profile }
  },
  response => console.log(response)
);
```

白名单存储在 `chrome.storage.sync`：

- `apiBaseUrl`: 后端地址（默认 `http://localhost:8081`）
- `allowedOrigins`: 允许同步登录态 / 执行页面注入的站点来源列表（JSON 数组，默认为 `apiBaseUrl` 对应的来源）

## 开发说明

### 技术栈

- Manifest V3
- Chrome Extension API
- Vanilla JavaScript

### 权限说明

- `storage`: 存储用户认证信息与服务器配置
- `activeTab`: 获取当前标签页信息
- `contextMenus`: 右键菜单
- `notifications`: 通知提示
- `scripting`: 向白名单站点注入登录态同步脚本
- `offscreen`: 剪贴板操作（Chrome 109+，通过 `offscreen.html`）

上传浮窗需要全站注入（`content_scripts.matches: ["<all_urls>"]`），以便在任意网页拖拽/右键上传；`web_accessible_resources` 仅暴露扩展自身图标（`icons/*`）。

## 许可证

本项目遵循仓库根目录的 [LICENSE](../LICENSE)：GPL-3.0 及附加条款（禁商用 + 署名）。
