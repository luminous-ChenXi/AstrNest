# 辰汐图床浏览器插件

AstrNest 浏览器扩展，支持快速上传图片到 luminouschenxi.net。

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

## 文件结构

```
extension/
├── manifest.json      # 插件配置
├── background.js      # Service Worker 后台脚本
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

## 与主站对接

### 认证同步

插件通过 `chrome.storage.local` 存储用户认证信息：
- `astrnest_auth_token`: JWT Token
- `astrnest_auth_profile`: 用户信息
- `astrnest_auth_expires_at`: 过期时间

### API 接口

- 登录: `POST /api/auth/login`
- 上传: `POST /api/uploads`
- 图库: `GET /api/gallery`

### 主站登录同步

主站登录成功后，可通过 `chrome.runtime.sendMessage` 将 token 同步到插件：

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

## 开发说明

### 技术栈

- Manifest V3
- Chrome Extension API
- Vanilla JavaScript

### 权限说明

- `storage`: 存储用户认证信息
- `activeTab`: 获取当前标签页信息
- `contextMenus`: 右键菜单
- `notifications`: 通知提示
- `offscreen`: 剪贴板操作（Chrome 109+）

## 许可证

MIT
