// 扩展共享配置（popup / content script / background 共用）
// 以普通脚本方式加载，挂载到全局 self 上（service worker 与页面环境均可用）。
(function (global) {
  'use strict';

  // 默认后端地址：本地开发环境。自部署用户请在扩展弹窗设置中修改，
  // 或阅读 extension/README.md 调整 manifest.json 的 host_permissions。
  var DEFAULT_API_BASE_URL = 'http://localhost:8081';

  // chrome.storage 键名
  var API_BASE_URL_KEY = 'apiBaseUrl';
  var ALLOWED_ORIGINS_KEY = 'allowedOrigins';

  function normalizeBaseUrl(url) {
    return String(url || '').trim().replace(/\/+$/, '');
  }

  function toOrigin(url) {
    try {
      return new URL(normalizeBaseUrl(url)).origin;
    } catch (e) {
      return '';
    }
  }

  // 读取当前使用的后端地址（chrome.storage.sync，随用户配置文件同步）
  function getApiBaseUrl() {
    return new Promise((resolve) => {
      try {
        chrome.storage.sync.get([API_BASE_URL_KEY], (data) => {
          var stored = data && data[API_BASE_URL_KEY];
          var value = normalizeBaseUrl(stored);
          resolve(value || DEFAULT_API_BASE_URL);
        });
      } catch (e) {
        resolve(DEFAULT_API_BASE_URL);
      }
    });
  }

  // 保存后端地址；空值恢复默认
  function setApiBaseUrl(url) {
    var value = normalizeBaseUrl(url);
    return new Promise((resolve, reject) => {
      var payload = {};
      if (value) {
        payload[API_BASE_URL_KEY] = value;
      } else {
        payload[API_BASE_URL_KEY] = DEFAULT_API_BASE_URL;
      }
      chrome.storage.sync.set(payload, () => {
        if (chrome.runtime.lastError) {
          reject(new Error(chrome.runtime.lastError.message));
          return;
        }
        resolve(payload[API_BASE_URL_KEY]);
      });
    });
  }

  // 默认允许与扩展通信/注入同步的站点（与默认后端地址保持一致）
  function defaultAllowedOrigins() {
    var origin = toOrigin(DEFAULT_API_BASE_URL);
    return origin ? [origin] : [];
  }

  // 读取 onMessageExternal / 页面注入同步用的域名白名单（storage 可配置）
  function getAllowedOrigins() {
    return new Promise((resolve) => {
      try {
        chrome.storage.sync.get([ALLOWED_ORIGINS_KEY], (data) => {
          var stored = data && data[ALLOWED_ORIGINS_KEY];
          if (Array.isArray(stored) && stored.length > 0) {
            resolve(stored.map(toOrigin).filter(Boolean));
            return;
          }
          resolve(defaultAllowedOrigins());
        });
      } catch (e) {
        resolve(defaultAllowedOrigins());
      }
    });
  }

  global.AstrNestExtConfig = {
    DEFAULT_API_BASE_URL: DEFAULT_API_BASE_URL,
    API_BASE_URL_KEY: API_BASE_URL_KEY,
    ALLOWED_ORIGINS_KEY: ALLOWED_ORIGINS_KEY,
    normalizeBaseUrl: normalizeBaseUrl,
    toOrigin: toOrigin,
    getApiBaseUrl: getApiBaseUrl,
    setApiBaseUrl: setApiBaseUrl,
    defaultAllowedOrigins: defaultAllowedOrigins,
    getAllowedOrigins: getAllowedOrigins
  };
})(typeof self !== 'undefined' ? self : globalThis);
