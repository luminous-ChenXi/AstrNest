// Popup 页面脚本

const API_BASE_URL = 'https://luminouschenxi.net';

// DOM 元素
const userCard = document.getElementById('user-card');
const userAvatar = document.getElementById('user-avatar');
const userName = document.getElementById('user-name');
const userStatus = document.getElementById('user-status');
const uploadArea = document.getElementById('upload-area');
const fileInput = document.getElementById('file-input');
const loginModal = document.getElementById('login-modal');
const loginForm = document.getElementById('login-form');
const loginBtn = document.getElementById('login-btn');
const cancelLogin = document.getElementById('cancel-login');

// 初始化
async function init() {
  await updateUserInfo();
  setupEventListeners();
}

// 更新用户信息
async function updateUserInfo() {
  try {
    const response = await chrome.runtime.sendMessage({ action: 'getAuthStatus' });

    if (response && response.isAuthenticated && response.profile) {
      const profile = response.profile;
      userAvatar.src = profile.avatarUrl || `${API_BASE_URL}/assets/img/avatar.png`;
      userName.textContent = profile.displayName || profile.username;
      userStatus.textContent = '已登录';
      userStatus.classList.add('logged-in');
    } else {
      userAvatar.src = `${API_BASE_URL}/assets/img/avatar.png`;
      userName.textContent = '未登录';
      userStatus.textContent = '点击登录';
      userStatus.classList.remove('logged-in');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
}

// 设置事件监听
function setupEventListeners() {
  // 用户卡片点击
  userCard.addEventListener('click', async () => {
    const response = await chrome.runtime.sendMessage({ action: 'getAuthStatus' });

    if (response && response.isAuthenticated) {
      // 已登录，打开个人资料页
      chrome.tabs.create({ url: `${API_BASE_URL}/user/profile` });
    } else {
      // 未登录，显示登录框
      showLoginModal();
    }
  });

  // 上传区域点击
  uploadArea.addEventListener('click', () => fileInput.click());

  // 文件选择
  fileInput.addEventListener('change', handleFileSelect);

  // 拖拽上传
  uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.style.borderColor = '#667eea';
    uploadArea.style.background = 'rgba(102, 126, 234, 0.05)';
  });

  uploadArea.addEventListener('dragleave', () => {
    uploadArea.style.borderColor = '';
    uploadArea.style.background = '';
  });

  uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.style.borderColor = '';
    uploadArea.style.background = '';
    handleFiles(e.dataTransfer.files);
  });

  // 快捷操作
  document.getElementById('action-upload').addEventListener('click', () => {
    fileInput.click();
  });

  document.getElementById('action-gallery').addEventListener('click', () => {
    chrome.runtime.sendMessage({ action: 'openGallery' });
  });

  document.getElementById('action-screenshot').addEventListener('click', async () => {
    try {
      // 获取当前活动标签页
      const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

      // 执行截图
      const dataUrl = await chrome.tabs.captureVisibleTab(null, { format: 'png' });

      // 转换为文件并上传
      const response = await fetch(dataUrl);
      const blob = await response.blob();
      const file = new File([blob], `screenshot-${Date.now()}.png`, { type: 'image/png' });

      await uploadFile(file);
    } catch (error) {
      showNotification('截图失败', error.message);
    }
  });

  // 底部链接
  document.getElementById('link-website').addEventListener('click', (e) => {
    e.preventDefault();
    chrome.tabs.create({ url: API_BASE_URL });
  });

  document.getElementById('link-help').addEventListener('click', (e) => {
    e.preventDefault();
    chrome.tabs.create({ url: `${API_BASE_URL}/help` });
  });

  document.getElementById('link-feedback').addEventListener('click', (e) => {
    e.preventDefault();
    chrome.tabs.create({ url: `${API_BASE_URL}/feedback` });
  });

  // 登录表单
  loginForm.addEventListener('submit', handleLogin);
  cancelLogin.addEventListener('click', hideLoginModal);

  // 点击遮罩关闭登录框
  loginModal.addEventListener('click', (e) => {
    if (e.target === loginModal) hideLoginModal();
  });
}

// 处理文件选择
function handleFileSelect(e) {
  handleFiles(e.target.files);
  e.target.value = ''; // 清空以便重复选择相同文件
}

// 处理文件
async function handleFiles(files) {
  const validFiles = Array.from(files).filter(file =>
    file.type.startsWith('image/') || file.type.startsWith('video/')
  );

  if (validFiles.length === 0) {
    showNotification('请选择图片或视频文件', 'error');
    return;
  }

  // 逐个上传
  for (const file of validFiles) {
    await uploadFile(file);
  }
}

// 上传单个文件
async function uploadFile(file) {
  const originalBtnText = loginBtn.textContent;

  try {
    showNotification('正在上传...', 'info');

    // 转换文件为base64
    const base64 = await fileToBase64(file);

    // 发送给background处理
    const response = await chrome.runtime.sendMessage({
      action: 'uploadFiles',
      files: [{
        name: file.name,
        type: file.type,
        data: base64
      }]
    });

    if (response.success) {
      const results = response.data.results || [];
      if (results.length > 0) {
        const url = results[0].url;
        await copyToClipboard(url);
        showNotification('上传成功！链接已复制', 'success');
      }
    } else {
      throw new Error(response.error);
    }
  } catch (error) {
    showNotification(`上传失败: ${error.message}`, 'error');
  }
}

// 处理登录
async function handleLogin(e) {
  e.preventDefault();

  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  if (!username || !password) {
    showNotification('请输入用户名和密码', 'error');
    return;
  }

  loginBtn.disabled = true;
  loginBtn.innerHTML = '<span class="loading"></span>';

  try {
    const response = await chrome.runtime.sendMessage({
      action: 'login',
      credentials: { username, password }
    });

    if (response.success) {
      hideLoginModal();
      await updateUserInfo();
      showNotification('登录成功', 'success');
    } else {
      throw new Error(response.error);
    }
  } catch (error) {
    showNotification(error.message, 'error');
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = '登录';
  }
}

// 显示登录框
function showLoginModal() {
  loginModal.classList.add('active');
  document.getElementById('login-username').focus();
}

// 隐藏登录框
function hideLoginModal() {
  loginModal.classList.remove('active');
  loginForm.reset();
}

// 文件转Base64
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// 复制到剪贴板
async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  }
}

// 显示通知
function showNotification(message, type = 'info') {
  chrome.notifications.create({
    type: 'basic',
    iconUrl: 'icons/icon-128.png',
    title: type === 'success' ? '成功' : type === 'error' ? '错误' : '提示',
    message
  });
}

// 启动
init();
