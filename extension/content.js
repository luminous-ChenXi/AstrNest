// Content Script - 注入页面，提供上传浮窗功能

(function() {
  'use strict';

  // 防止重复注入
  if (window.__chenxiExtensionInjected) return;
  window.__chenxiExtensionInjected = true;

  const API_BASE_URL = 'https://luminouschenxi.net';

  // 当前认证状态
  let currentAuthStatus = { isAuthenticated: false, profile: null };
  let uploadPermission = null;

  // 创建上传浮窗
  function createUploadWidget() {
    const widget = document.createElement('div');
    widget.id = 'chenxi-upload-widget';
    widget.innerHTML = `
      <div class="chenxi-widget-container">
        <div class="chenxi-widget-header">
          <div class="chenxi-widget-title">
            <img src="${chrome.runtime.getURL('icons/icon-32.png')}" alt="logo" class="chenxi-widget-logo">
            <span>辰汐图床</span>
          </div>
          <div class="chenxi-widget-actions">
            <button class="chenxi-btn chenxi-btn-minimize" title="最小化">−</button>
            <button class="chenxi-btn chenxi-btn-close" title="关闭">×</button>
          </div>
        </div>
        <div class="chenxi-widget-body">
          <div class="chenxi-user-section" id="chenxi-user-section">
            <div class="chenxi-user-info">
              <img src="${API_BASE_URL}/assets/img/avatar.png" alt="avatar" class="chenxi-avatar" id="chenxi-avatar">
              <div class="chenxi-user-details">
                <span class="chenxi-username" id="chenxi-username">未登录</span>
                <span class="chenxi-user-status" id="chenxi-user-status">点击登录</span>
              </div>
            </div>
          </div>
          <div class="chenxi-upload-area" id="chenxi-upload-area">
            <div class="chenxi-upload-placeholder">
              <svg class="chenxi-upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="17 8 12 3 7 8"></polyline>
                <line x1="12" y1="3" x2="12" y2="15"></line>
              </svg>
              <p>拖拽图片到这里</p>
              <p class="chenxi-upload-hint">或点击选择文件</p>
            </div>
            <input type="file" id="chenxi-file-input" multiple accept="image/*,video/*" hidden>
          </div>
          <div class="chenxi-upload-list" id="chenxi-upload-list"></div>
          <div class="chenxi-widget-footer">
            <button class="chenxi-btn chenxi-btn-primary" id="chenxi-btn-upload" disabled>开始上传</button>
            <button class="chenxi-btn chenxi-btn-secondary" id="chenxi-btn-gallery">我的图库</button>
          </div>
        </div>
        <div class="chenxi-widget-minimized" id="chenxi-widget-minimized" style="display: none;">
          <img src="${chrome.runtime.getURL('icons/icon-32.png')}" alt="辰汐图床">
        </div>
      </div>
    `;

    document.body.appendChild(widget);
    setupWidgetEvents(widget);
    updateAuthStatus();
    checkUploadPermission();

    return widget;
  }

  // 检查上传权限
  async function checkUploadPermission() {
    try {
      const response = await chrome.runtime.sendMessage({ action: 'checkUploadPermission' });
      uploadPermission = response;

      // 更新上传区域的提示
      const uploadHint = document.querySelector('.chenxi-upload-hint');
      if (uploadHint && response.maxFilesPerUpload) {
        uploadHint.textContent = `或点击选择文件 (最多 ${response.maxFilesPerUpload} 个)`;
      }
    } catch (error) {
      console.error('Failed to check upload permission:', error);
    }
  }

  // 设置浮窗事件
  function setupWidgetEvents(widget) {
    const container = widget.querySelector('.chenxi-widget-container');
    const uploadArea = widget.querySelector('#chenxi-upload-area');
    const fileInput = widget.querySelector('#chenxi-file-input');
    const minimizeBtn = widget.querySelector('.chenxi-btn-minimize');
    const closeBtn = widget.querySelector('.chenxi-btn-close');
    const minimizedView = widget.querySelector('#chenxi-widget-minimized');
    const uploadBtn = widget.querySelector('#chenxi-btn-upload');
    const galleryBtn = widget.querySelector('#chenxi-btn-gallery');
    const userSection = widget.querySelector('#chenxi-user-section');

    let selectedFiles = [];
    let isMinimized = false;

    // 拖拽上传
    uploadArea.addEventListener('dragover', (e) => {
      e.preventDefault();
      uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
      uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
      e.preventDefault();
      uploadArea.classList.remove('dragover');
      handleFiles(e.dataTransfer.files);
    });

    uploadArea.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
      handleFiles(e.target.files);
      fileInput.value = '';
    });

    // 最小化
    minimizeBtn.addEventListener('click', () => {
      isMinimized = true;
      widget.querySelector('.chenxi-widget-body').style.display = 'none';
      widget.querySelector('.chenxi-widget-header').style.display = 'none';
      minimizedView.style.display = 'flex';
      container.classList.add('minimized');
    });

    // 从最小化恢复
    minimizedView.addEventListener('click', () => {
      isMinimized = false;
      widget.querySelector('.chenxi-widget-body').style.display = 'flex';
      widget.querySelector('.chenxi-widget-header').style.display = 'flex';
      minimizedView.style.display = 'none';
      container.classList.remove('minimized');
    });

    // 关闭
    closeBtn.addEventListener('click', () => {
      widget.style.display = 'none';
      showFloatingButton();
    });

    // 上传按钮
    uploadBtn.addEventListener('click', () => {
      if (selectedFiles.length > 0) {
        startUpload(selectedFiles);
      }
    });

    // 图库按钮
    galleryBtn.addEventListener('click', () => {
      chrome.runtime.sendMessage({ action: 'openGallery' });
    });

    // 用户头像点击 - 登录/查看资料
    userSection.addEventListener('click', () => {
      if (currentAuthStatus.isAuthenticated) {
        window.open(`${API_BASE_URL}/user/profile`, '_blank');
      } else {
        chrome.runtime.sendMessage({ action: 'openLoginPage' });
      }
    });

    // 处理文件选择
    async function handleFiles(files) {
      // 检查上传权限
      if (!uploadPermission) {
        await checkUploadPermission();
      }

      // 检查是否允许上传
      if (uploadPermission && !uploadPermission.allowed) {
        showToast(uploadPermission.message || '未登录用户不允许上传，请先登录', 'error');
        // 显示登录提示
        showLoginPrompt();
        return;
      }

      const validFiles = Array.from(files).filter(file =>
        file.type.startsWith('image/') || file.type.startsWith('video/')
      );

      if (validFiles.length === 0) {
        showToast('请选择图片或视频文件', 'error');
        return;
      }

      // 检查文件数量限制
      const maxFiles = uploadPermission?.maxFilesPerUpload || 30;
      const currentCount = selectedFiles.length;
      const newCount = validFiles.length;

      if (currentCount + newCount > maxFiles) {
        const remaining = maxFiles - currentCount;
        if (remaining <= 0) {
          showToast(`已达到最大上传数量限制 (${maxFiles} 个文件)`, 'error');
          return;
        }
        showToast(`最多还能添加 ${remaining} 个文件`, 'warning');
        selectedFiles = [...selectedFiles, ...validFiles.slice(0, remaining)];
      } else {
        selectedFiles = [...selectedFiles, ...validFiles];
      }

      updateFileList();
    }

    // 显示登录提示
    function showLoginPrompt() {
      const modal = document.createElement('div');
      modal.className = 'chenxi-modal';
      modal.innerHTML = `
        <div class="chenxi-modal-content" style="max-width: 360px;">
          <div class="chenxi-modal-header">
            <h3>需要登录</h3>
            <button class="chenxi-btn chenxi-btn-close-modal">×</button>
          </div>
          <div class="chenxi-modal-body" style="text-align: center; padding: 24px;">
            <svg class="chenxi-upload-icon" viewBox="0 0 24 24" fill="none" stroke="#ff6b9d" stroke-width="2" style="width: 64px; height: 64px; margin-bottom: 16px;">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <p style="color: #4a4a6a; margin-bottom: 20px; font-size: 14px;">
              未登录用户不允许上传<br>
              请先登录后再上传图片
            </p>
            <button class="chenxi-btn chenxi-btn-primary" id="chenxi-login-now" style="width: 100%;">立即登录</button>
            <button class="chenxi-btn chenxi-btn-secondary" id="chenxi-cancel-login" style="width: 100%; margin-top: 10px;">取消</button>
          </div>
        </div>
      `;

      document.body.appendChild(modal);

      modal.querySelector('.chenxi-btn-close-modal').addEventListener('click', () => {
        modal.remove();
      });

      modal.querySelector('#chenxi-cancel-login').addEventListener('click', () => {
        modal.remove();
      });

      modal.querySelector('#chenxi-login-now').addEventListener('click', () => {
        modal.remove();
        chrome.runtime.sendMessage({ action: 'openLoginPage' });
      });

      modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
      });
    }

    // 更新文件列表
    function updateFileList() {
      const list = widget.querySelector('#chenxi-upload-list');
      uploadBtn.disabled = selectedFiles.length === 0;

      if (selectedFiles.length === 0) {
        list.innerHTML = '';
        return;
      }

      list.innerHTML = selectedFiles.map((file, index) => `
        <div class="chenxi-file-item" data-index="${index}">
          <div class="chenxi-file-preview">
            ${file.type.startsWith('image/')
              ? `<img src="${URL.createObjectURL(file)}" alt="${file.name}">`
              : `<div class="chenxi-video-icon">🎬</div>`
            }
          </div>
          <div class="chenxi-file-info">
            <span class="chenxi-file-name" title="${file.name}">${file.name}</span>
            <span class="chenxi-file-size">${formatFileSize(file.size)}</span>
          </div>
          <button class="chenxi-btn chenxi-btn-remove" data-index="${index}" title="移除">×</button>
          <div class="chenxi-file-progress" id="progress-${index}">
            <div class="chenxi-progress-bar"></div>
          </div>
        </div>
      `).join('');

      // 绑定移除按钮事件
      list.querySelectorAll('.chenxi-btn-remove').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const index = parseInt(btn.dataset.index);
          selectedFiles.splice(index, 1);
          updateFileList();
        });
      });
    }

    // 开始上传
    async function startUpload(files) {
      // 再次检查权限
      if (!uploadPermission || !uploadPermission.allowed) {
        await checkUploadPermission();
        if (!uploadPermission || !uploadPermission.allowed) {
          showToast(uploadPermission?.message || '未登录用户不允许上传，请先登录', 'error');
          showLoginPrompt();
          return;
        }
      }

      uploadBtn.disabled = true;
      uploadBtn.textContent = '上传中...';

      try {
        // 转换文件为base64
        const fileData = await Promise.all(files.map(async (file, index) => {
          const base64 = await fileToBase64(file);
          updateProgress(index, 30);
          return {
            name: file.name,
            type: file.type,
            data: base64
          };
        }));

        updateProgress(0, 50);

        // 发送给background处理上传
        const response = await chrome.runtime.sendMessage({
          action: 'uploadFiles',
          files: fileData
        });

        if (response.success) {
          handleUploadSuccess(response.data);
        } else {
          // 处理特定错误
          if (response.error?.includes('未登录') || response.error?.includes('请先登录')) {
            showLoginPrompt();
          }
          throw new Error(response.error);
        }
      } catch (error) {
        showToast(`上传失败: ${error.message}`, 'error');
      } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = '开始上传';
        selectedFiles = [];
        updateFileList();
      }
    }

    // 处理上传成功
    function handleUploadSuccess(data) {
      const results = data.results || [];
      const urls = results.map(r => r.url).filter(Boolean);

      if (urls.length > 0) {
        copyToClipboard(urls[0]);
        showToast(`上传成功！已复制 ${urls.length} 个链接`, 'success');
        showUploadResults(results);
      }
    }

    // 显示上传结果
    function showUploadResults(results) {
      const modal = document.createElement('div');
      modal.className = 'chenxi-modal';
      modal.innerHTML = `
        <div class="chenxi-modal-content">
          <div class="chenxi-modal-header">
            <h3>上传成功</h3>
            <button class="chenxi-btn chenxi-btn-close-modal">×</button>
          </div>
          <div class="chenxi-modal-body">
            ${results.map((result, i) => `
              <div class="chenxi-result-item">
                <img src="${result.thumbnailUrl || result.url}" alt="${result.filename}" class="chenxi-result-thumb">
                <div class="chenxi-result-links">
                  <input type="text" value="${result.url}" readonly class="chenxi-link-input" id="link-${i}">
                  <button class="chenxi-btn chenxi-btn-copy" data-link="${result.url}">复制</button>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      `;

      document.body.appendChild(modal);

      modal.querySelector('.chenxi-btn-close-modal').addEventListener('click', () => {
        modal.remove();
      });

      modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
      });

      modal.querySelectorAll('.chenxi-btn-copy').forEach(btn => {
        btn.addEventListener('click', () => {
          copyToClipboard(btn.dataset.link);
          btn.textContent = '已复制';
          setTimeout(() => btn.textContent = '复制', 2000);
        });
      });
    }

    // 更新进度
    function updateProgress(index, percent) {
      const progressBar = widget.querySelector(`#progress-${index} .chenxi-progress-bar`);
      if (progressBar) {
        progressBar.style.width = `${percent}%`;
      }
    }
  }

  // 显示浮动按钮
  function showFloatingButton() {
    if (document.getElementById('chenxi-floating-btn')) return;

    const btn = document.createElement('button');
    btn.id = 'chenxi-floating-btn';
    btn.className = 'chenxi-floating-btn';
    btn.innerHTML = `<img src="${chrome.runtime.getURL('icons/icon-32.png')}" alt="辰汐图床">`;
    btn.title = '打开辰汐图床上传';

    btn.addEventListener('click', () => {
      const widget = document.getElementById('chenxi-upload-widget');
      if (widget) {
        widget.style.display = 'block';
      } else {
        createUploadWidget();
      }
      btn.remove();
    });

    document.body.appendChild(btn);
  }

  // 更新认证状态
  function updateAuthStatus() {
    chrome.runtime.sendMessage({ action: 'getAuthStatus' }, (response) => {
      const avatar = document.getElementById('chenxi-avatar');
      const username = document.getElementById('chenxi-username');
      const status = document.getElementById('chenxi-user-status');

      if (!avatar || !username || !status) return;

      currentAuthStatus = response || { isAuthenticated: false, profile: null };

      if (response && response.isAuthenticated && response.profile) {
        const profile = response.profile;
        avatar.src = profile.avatarUrl || `${API_BASE_URL}/assets/img/avatar.png`;
        username.textContent = profile.displayName || profile.username;
        status.textContent = '已登录';
        status.classList.add('logged-in');
      } else {
        avatar.src = `${API_BASE_URL}/assets/img/avatar.png`;
        username.textContent = '未登录';
        status.textContent = '点击登录';
        status.classList.remove('logged-in');
      }
    });
  }

  // 工具函数：文件转Base64
  function fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  // 工具函数：格式化文件大小
  function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  // 工具函数：复制到剪贴板
  async function copyToClipboard(text) {
    try {
      await navigator.clipboard.writeText(text);
    } catch (err) {
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

  // 显示提示
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `chenxi-toast chenxi-toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('show');
    }, 10);

    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  // 监听来自background的消息
  chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'getAllImages') {
      const images = Array.from(document.querySelectorAll('img'))
        .filter(img => img.src && !img.src.startsWith('data:'))
        .map(img => img.src);

      if (images.length > 0) {
        let widget = document.getElementById('chenxi-upload-widget');
        if (!widget) {
          widget = createUploadWidget();
        }
        widget.style.display = 'block';
        showToast(`发现 ${images.length} 张图片，请在上传区域选择`, 'info');
      } else {
        showToast('当前页面没有找到图片', 'error');
      }

      sendResponse({ count: images.length });
      return true;
    }

    if (request.action === 'authStatusChanged') {
      // 认证状态变化，更新UI
      updateAuthStatus();
      checkUploadPermission();
      sendResponse({ received: true });
      return true;
    }

    if (request.action === 'updateAuthStatus') {
      updateAuthStatus();
      checkUploadPermission();
      sendResponse({ received: true });
      return true;
    }
  });

  // 初始化
  function init() {
    showFloatingButton();

    // 监听页面可见性变化，更新认证状态
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        updateAuthStatus();
        checkUploadPermission();
      }
    });

    // 定期检查认证状态（每30秒）
    setInterval(() => {
      updateAuthStatus();
      checkUploadPermission();
    }, 30000);
  }

  // 等待页面加载完成
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
