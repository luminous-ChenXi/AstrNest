// Background Service Worker - 处理插件后台逻辑

const API_BASE_URL = 'https://luminouschenxi.net';

// 认证相关存储键（与前端保持一致）
const TOKEN_KEY = 'astrnest_auth_token';
const PROFILE_KEY = 'astrnest_auth_profile';
const EXPIRES_AT_KEY = 'astrnest_auth_expires_at';
const VISITOR_TOKEN_KEY = 'chenxi-visitor-token';

// 系统配置缓存
let systemConfigCache = null;
let systemConfigCacheTime = 0;
const SYSTEM_CONFIG_CACHE_TTL = 5 * 60 * 1000; // 5分钟缓存

// 初始化上下文菜单
chrome.runtime.onInstalled.addListener(() => {
  // 上传选中图片
  chrome.contextMenus.create({
    id: 'upload-image',
    title: '上传到辰汐图床',
    contexts: ['image']
  });

  // 上传页面所有图片
  chrome.contextMenus.create({
    id: 'upload-all-images',
    title: '上传页面所有图片到辰汐图床',
    contexts: ['page']
  });
});

// 处理右键菜单点击
chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId === 'upload-image' && info.srcUrl) {
    uploadImageFromUrl(info.srcUrl);
  } else if (info.menuItemId === 'upload-all-images') {
    chrome.tabs.sendMessage(tab.id, { action: 'getAllImages' });
  }
});

// 从URL上传图片
async function uploadImageFromUrl(imageUrl) {
  try {
    // 获取图片数据
    const response = await fetch(imageUrl);
    const blob = await response.blob();
    const filename = imageUrl.split('/').pop() || 'image.png';
    const file = new File([blob], filename, { type: blob.type });

    // 执行上传
    const result = await uploadFile(file);

    // 复制链接到剪贴板
    await chrome.runtime.sendMessage({
      action: 'copyToClipboard',
      text: result.url
    });

    showNotification('上传成功', '图片链接已复制到剪贴板');
  } catch (error) {
    console.error('Upload failed:', error);
    showNotification('上传失败', error.message);
  }
}

// 获取访客Token（与前端保持一致）
function getVisitorToken() {
  return new Promise((resolve) => {
    chrome.storage.local.get([VISITOR_TOKEN_KEY], (result) => {
      if (result[VISITOR_TOKEN_KEY]) {
        resolve(result[VISITOR_TOKEN_KEY]);
      } else {
        // 生成新的访客token
        const token = 'chenxi-' + Math.random().toString(36).slice(2) + Date.now().toString(36);
        chrome.storage.local.set({ [VISITOR_TOKEN_KEY]: token }, () => {
          resolve(token);
        });
      }
    });
  });
}

// 获取系统配置
async function getSystemConfig() {
  const now = Date.now();
  if (systemConfigCache && (now - systemConfigCacheTime) < SYSTEM_CONFIG_CACHE_TTL) {
    return systemConfigCache;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/system/config/public`);
    if (response.ok) {
      systemConfigCache = await response.json();
      systemConfigCacheTime = now;
      return systemConfigCache;
    }
  } catch (error) {
    console.error('Failed to fetch system config:', error);
  }
  return null;
}

// 检查上传权限
async function checkUploadPermission(isAuthenticated) {
  const config = await getSystemConfig();

  // 如果获取不到配置，默认允许上传（兼容旧版本）
  if (!config) {
    return { allowed: true };
  }

  // 已登录用户始终允许上传
  if (isAuthenticated) {
    return {
      allowed: true,
      maxFilesPerUpload: config.maxFilesPerUpload || 30,
      maxUploadMb: config.maxUploadMb || 20,
      maxVideoUploadMb: config.maxVideoUploadMb || 100
    };
  }

  // 未登录用户检查访客上传开关
  // 注意：后端配置中 guestUploadEnabled 为 false 表示禁止访客上传
  const guestUploadEnabled = config.guestUploadEnabled !== undefined ? config.guestUploadEnabled : false;

  if (!guestUploadEnabled) {
    return {
      allowed: false,
      reason: 'guest_forbidden',
      message: '未登录用户不允许上传，请先登录'
    };
  }

  return {
    allowed: true,
    maxFilesPerUpload: config.maxFilesPerUpload || 30,
    maxUploadMb: config.maxUploadMb || 20,
    maxVideoUploadMb: config.maxVideoUploadMb || 100
  };
}

// 上传文件到服务器
async function uploadFile(file) {
  const authStatus = await getAuthStatus();
  const visitorToken = await getVisitorToken();

  // 检查上传权限
  const permission = await checkUploadPermission(authStatus.isAuthenticated);
  if (!permission.allowed) {
    throw new Error(permission.message);
  }

  const formData = new FormData();
  formData.append('files', file);

  const headers = {
    'X-Chenxi-Visitor': visitorToken
  };

  if (authStatus.token) {
    headers.Authorization = authStatus.token.startsWith('Basic ') ? authStatus.token : `Basic ${authStatus.token}`;
  }

  const response = await fetch(`${API_BASE_URL}/api/uploads`, {
    method: 'POST',
    headers,
    body: formData
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));

    // 处理特定错误码
    if (response.status === 403) {
      throw new Error(error.message || '未登录用户不允许上传，请先登录');
    }
    if (response.status === 401) {
      // Token过期，清除本地认证信息
      await handleLogout();
      throw new Error('登录已过期，请重新登录');
    }
    if (response.status === 413) {
      throw new Error(`文件大小超过限制，最大允许 ${permission.maxUploadMb || 20}MB`);
    }
    if (response.status === 400 && error.message?.includes('数量')) {
      throw new Error(error.message || `上传图片数量超限，单次最多允许 ${permission.maxFilesPerUpload || 30} 个文件`);
    }

    throw new Error(error.message || `上传失败 (${response.status})`);
  }

  const data = await response.json();
  return data.results?.[0] || data;
}

// 显示通知
function showNotification(title, message) {
  chrome.notifications.create({
    type: 'basic',
    iconUrl: 'icons/icon-128.png',
    title,
    message
  });
}

// 处理来自content script和popup的消息
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  switch (request.action) {
    case 'uploadFiles':
      handleUploadFiles(request.files)
        .then(result => sendResponse({ success: true, data: result }))
        .catch(error => sendResponse({ success: false, error: error.message }));
      return true;

    case 'getAuthStatus':
      getAuthStatus()
        .then(status => sendResponse(status))
        .catch(() => sendResponse({ isAuthenticated: false }));
      return true;

    case 'login':
      handleLogin(request.credentials)
        .then(result => sendResponse({ success: true, data: result }))
        .catch(error => sendResponse({ success: false, error: error.message }));
      return true;

    case 'logout':
      handleLogout()
        .then(() => sendResponse({ success: true }))
        .catch(error => sendResponse({ success: false, error: error.message }));
      return true;

    case 'openLoginPage':
      chrome.tabs.create({ url: `${API_BASE_URL}/?login=1` });
      sendResponse({ success: true });
      return false;

    case 'openGallery':
      chrome.tabs.create({ url: `${API_BASE_URL}/gallery` });
      sendResponse({ success: true });
      return false;

    case 'copyToClipboard':
      copyToClipboard(request.text)
        .then(() => sendResponse({ success: true }))
        .catch(error => sendResponse({ success: false, error: error.message }));
      return true;

    case 'checkUploadPermission':
      getAuthStatus()
        .then(status => checkUploadPermission(status.isAuthenticated))
        .then(permission => sendResponse(permission))
        .catch(error => sendResponse({ allowed: false, message: error.message }));
      return true;

    case 'syncAuthFromWebsite':
      // 从网站同步认证信息
      syncAuthFromWebsite(request.data)
        .then(result => sendResponse(result))
        .catch(error => sendResponse({ success: false, error: error.message }));
      return true;
  }
});

// 处理文件上传
async function handleUploadFiles(files) {
  const authStatus = await getAuthStatus();
  const visitorToken = await getVisitorToken();

  // 检查上传权限
  const permission = await checkUploadPermission(authStatus.isAuthenticated);
  if (!permission.allowed) {
    throw new Error(permission.message);
  }

  // 检查文件数量
  if (files.length > permission.maxFilesPerUpload) {
    throw new Error(`上传图片数量超限，单次最多允许 ${permission.maxFilesPerUpload} 个文件，请分批上传`);
  }

  const formData = new FormData();
  files.forEach(file => {
    const blob = base64ToBlob(file.data, file.type);
    formData.append('files', blob, file.name);
  });

  const headers = {
    'X-Chenxi-Visitor': visitorToken
  };

  if (authStatus.token) {
    headers.Authorization = authStatus.token.startsWith('Basic ') ? authStatus.token : `Basic ${authStatus.token}`;
  }

  const response = await fetch(`${API_BASE_URL}/api/uploads`, {
    method: 'POST',
    headers,
    body: formData
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));

    // 处理特定错误码
    if (response.status === 403) {
      throw new Error(error.message || '未登录用户不允许上传，请先登录');
    }
    if (response.status === 401) {
      await handleLogout();
      throw new Error('登录已过期，请重新登录');
    }
    if (response.status === 413) {
      throw new Error(`文件大小超过限制，最大允许 ${permission.maxUploadMb || 20}MB`);
    }
    if (response.status === 400 && error.message?.includes('数量')) {
      throw new Error(error.message || `上传图片数量超限，单次最多允许 ${permission.maxFilesPerUpload} 个文件`);
    }

    throw new Error(error.message || `上传失败 (${response.status})`);
  }

  return await response.json();
}

// Base64转Blob
function base64ToBlob(base64, type) {
  const binary = atob(base64.split(',')[1]);
  const array = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    array[i] = binary.charCodeAt(i);
  }
  return new Blob([array], { type });
}

// 获取认证状态
async function getAuthStatus() {
  const { [TOKEN_KEY]: token, [PROFILE_KEY]: profile, [EXPIRES_AT_KEY]: expiresAt } =
    await chrome.storage.local.get([TOKEN_KEY, PROFILE_KEY, EXPIRES_AT_KEY]);

  const isExpired = expiresAt && Number(expiresAt) <= Date.now();
  const isAuthenticated = Boolean(token) && !isExpired;

  return {
    isAuthenticated,
    token,
    profile: profile ? JSON.parse(profile) : null,
    expiresAt: Number(expiresAt) || 0
  };
}

// 处理登录
async function handleLogin(credentials) {
  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || '登录失败');
  }

  const data = await response.json();

  // 存储认证信息（与前端使用相同的key和格式）
  const expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000; // 30天
  await chrome.storage.local.set({
    [TOKEN_KEY]: data.token,
    [PROFILE_KEY]: JSON.stringify(data.profile),
    [EXPIRES_AT_KEY]: String(expiresAt)
  });

  // 广播登录状态变化
  broadcastAuthChange(true, data.profile);

  return data;
}

// 处理登出
async function handleLogout() {
  await chrome.storage.local.remove([TOKEN_KEY, PROFILE_KEY, EXPIRES_AT_KEY]);
  broadcastAuthChange(false, null);
}

// 从网站同步认证信息
async function syncAuthFromWebsite(data) {
  const { token, profile } = data;

  if (!token) {
    return { success: false, error: 'No token provided' };
  }

  // 验证token有效性
  try {
    const response = await fetch(`${API_BASE_URL}/api/user/profile`, {
      headers: {
        Authorization: token.startsWith('Basic ') ? token : `Basic ${token}`
      }
    });

    if (!response.ok) {
      return { success: false, error: 'Invalid token' };
    }

    // Token有效，存储认证信息
    const expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
    await chrome.storage.local.set({
      [TOKEN_KEY]: token,
      [PROFILE_KEY]: JSON.stringify(profile),
      [EXPIRES_AT_KEY]: String(expiresAt)
    });

    broadcastAuthChange(true, profile);
    return { success: true };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

// 广播认证状态变化给所有标签页
function broadcastAuthChange(isAuthenticated, profile) {
  chrome.tabs.query({}, (tabs) => {
    tabs.forEach(tab => {
      if (tab.id) {
        chrome.tabs.sendMessage(tab.id, {
          action: 'authStatusChanged',
          data: { isAuthenticated, profile }
        }).catch(() => {
          // 忽略无法发送消息的标签页（非内容脚本页面）
        });
      }
    });
  });
}

// 复制到剪贴板
async function copyToClipboard(text) {
  try {
    await chrome.offscreen.createDocument({
      url: 'offscreen.html',
      reasons: ['CLIPBOARD'],
      justification: '复制上传链接到剪贴板'
    });

    await chrome.runtime.sendMessage({
      target: 'offscreen',
      action: 'copy',
      text
    });

    setTimeout(() => {
      chrome.offscreen.closeDocument();
    }, 1000);
  } catch (error) {
    console.log('Offscreen API not available, using fallback');
  }
}

// 监听来自主站的消息（用于主站登录后同步token到插件）
chrome.runtime.onMessageExternal.addListener((request, sender, sendResponse) => {
  // 验证来源
  const allowedOrigins = [
    'https://luminouschenxi.net',
    'https://www.luminouschenxi.net'
  ];

  if (!allowedOrigins.includes(sender.origin)) {
    sendResponse({ success: false, error: 'Unauthorized origin' });
    return false;
  }

  if (request.action === 'syncAuth') {
    const { token, profile } = request.data;

    if (!token) {
      sendResponse({ success: false, error: 'No token provided' });
      return false;
    }

    const expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;

    chrome.storage.local.set({
      [TOKEN_KEY]: token,
      [PROFILE_KEY]: JSON.stringify(profile),
      [EXPIRES_AT_KEY]: String(expiresAt)
    }).then(() => {
      broadcastAuthChange(true, profile);
      sendResponse({ success: true });
    }).catch(error => {
      sendResponse({ success: false, error: error.message });
    });

    return true;
  }

  if (request.action === 'getAuthStatus') {
    getAuthStatus().then(status => {
      sendResponse(status);
    });
    return true;
  }

  if (request.action === 'logout') {
    handleLogout().then(() => {
      sendResponse({ success: true });
    });
    return true;
  }

  return false;
});

// 监听标签页更新，尝试从页面获取认证信息
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status === 'complete' && tab.url?.includes('luminouschenxi.net')) {
    // 检查 chrome.scripting API 是否可用
    if (typeof chrome.scripting === 'undefined' || !chrome.scripting.executeScript) {
      console.log('Scripting API not available');
      return;
    }

    // 向页面注入脚本获取localStorage中的认证信息
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        return {
          token: localStorage.getItem('astrnest_auth_token'),
          profile: localStorage.getItem('astrnest_auth_profile'),
          expiresAt: localStorage.getItem('astrnest_auth_expires_at')
        };
      }
    }).then((results) => {
      if (results && results[0] && results[0].result) {
        const { token, profile, expiresAt } = results[0].result;
        if (token && profile) {
          // 同步到插件存储
          chrome.storage.local.set({
            [TOKEN_KEY]: token,
            [PROFILE_KEY]: profile,
            [EXPIRES_AT_KEY]: expiresAt || String(Date.now() + 30 * 24 * 60 * 60 * 1000)
          });
        }
      }
    }).catch((error) => {
      // 忽略错误（可能是特殊页面无法执行脚本）
      console.log('Failed to execute script:', error);
    });
  }
});

console.log('辰汐图床插件 Background Service Worker 已启动');
