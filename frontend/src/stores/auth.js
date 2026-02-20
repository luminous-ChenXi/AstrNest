import { defineStore } from 'pinia'

const TOKEN_KEY = 'astrnest_auth_token'
const PROFILE_KEY = 'astrnest_auth_profile'
const EXPIRES_AT_KEY = 'astrnest_auth_expires_at'
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000

// Chrome Extension ID - 使用固定的扩展ID
// 注意：需要在manifest.json中设置key来生成固定的extension ID
const CHENXI_EXTENSION_ID = 'luminouschenxi-astrnest-extension'

// 获取扩展ID（支持动态获取）
function getExtensionId() {
  // 尝试从meta标签获取
  const metaId = document.querySelector('meta[name="chenxi-extension-id"]')?.content
  if (metaId) return metaId

  // 尝试从window对象获取
  if (window.__CHENXI_EXTENSION_ID__) {
    return window.__CHENXI_EXTENSION_ID__
  }

  return CHENXI_EXTENSION_ID
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: (() => {
      const cached = localStorage.getItem(PROFILE_KEY)
      return cached ? JSON.parse(cached) : null
    })(),
    expiresAt: Number(localStorage.getItem(EXPIRES_AT_KEY)) || 0,
  }),
  getters: {
    isExpired: (state) => Boolean(state.expiresAt) && state.expiresAt <= Date.now(),
    isAuthenticated: (state) => Boolean(state.token) && (!state.expiresAt || state.expiresAt > Date.now()),
    isAdmin: (state) => state.profile?.roles?.includes('ADMIN') ?? false,
    displayName: (state) => state.profile?.displayName || state.profile?.username || '未登录',
  },
  actions: {
    setSession(token, profile) {
      const expiresAt = Date.now() + SESSION_TTL_MS
      this.token = token
      this.profile = profile
      this.expiresAt = expiresAt
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
      localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))

      // 同步认证信息到浏览器插件
      this.syncAuthToExtension(token, profile)

      // 广播登录事件（用于同一页面的其他组件）
      window.dispatchEvent(new CustomEvent('chenxi-auth-change', {
        detail: { isAuthenticated: true, token, profile }
      }))
    },

    // 同步认证信息到浏览器插件
    syncAuthToExtension(token, profile) {
      // 方法1: 使用 chrome.runtime.sendMessage (Manifest V3)
      if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage) {
        try {
          const extensionId = getExtensionId()

          // 尝试发送给插件（使用外部消息）
          chrome.runtime.sendMessage(
            extensionId,
            {
              action: 'syncAuth',
              data: { token, profile, timestamp: Date.now() }
            },
            (response) => {
              if (chrome.runtime.lastError) {
                console.log('Extension sync (method 1):', chrome.runtime.lastError.message)
                // 尝试方法2
                this.syncAuthViaPostMessage(token, profile)
                return
              }
              if (response && response.success) {
                console.log('Auth synced to extension successfully (method 1)')
              }
            }
          )
        } catch (error) {
          console.log('Extension sync method 1 failed:', error)
          this.syncAuthViaPostMessage(token, profile)
        }
      } else {
        this.syncAuthViaPostMessage(token, profile)
      }
    },

    // 方法2: 使用 postMessage 广播到所有窗口（备用方案）
    syncAuthViaPostMessage(token, profile) {
      try {
        // 广播认证状态变化
        window.postMessage({
          source: 'chenxi-website',
          action: 'syncAuth',
          data: { token, profile, timestamp: Date.now() }
        }, '*')
        console.log('Auth broadcast via postMessage')
      } catch (error) {
        console.log('PostMessage sync failed:', error)
      }
    },

    // 从插件获取认证状态
    async getAuthFromExtension() {
      return new Promise((resolve) => {
        if (typeof chrome === 'undefined' || !chrome.runtime || !chrome.runtime.sendMessage) {
          resolve(null)
          return
        }

        try {
          const extensionId = getExtensionId()
          chrome.runtime.sendMessage(
            extensionId,
            { action: 'getAuthStatus' },
            (response) => {
              if (chrome.runtime.lastError) {
                resolve(null)
                return
              }
              resolve(response)
            }
          )
        } catch (error) {
          resolve(null)
        }
      })
    },

    // 通知插件登出
    notifyExtensionLogout() {
      if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage) {
        try {
          const extensionId = getExtensionId()
          chrome.runtime.sendMessage(
            extensionId,
            { action: 'logout' },
            () => {
              // 忽略错误
            }
          )
        } catch (error) {
          // 忽略错误
        }
      }

      // 广播登出事件
      window.dispatchEvent(new CustomEvent('chenxi-auth-change', {
        detail: { isAuthenticated: false }
      }))

      // PostMessage 备用
      window.postMessage({
        source: 'chenxi-website',
        action: 'logout'
      }, '*')
    },

    touchSession() {
      if (!this.token) return
      const expiresAt = Date.now() + SESSION_TTL_MS
      this.expiresAt = expiresAt
      localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))
    },
    updateProfile(profile) {
      this.profile = profile
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))

      // 同步更新到扩展
      if (this.token) {
        this.syncAuthToExtension(this.token, profile)
      }
    },
    pruneIfExpired() {
      if (this.expiresAt && this.expiresAt <= Date.now()) {
        this.logout()
        return true
      }
      return false
    },
    logout() {
      // 通知扩展
      this.notifyExtensionLogout()

      this.token = ''
      this.profile = null
      this.expiresAt = 0
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PROFILE_KEY)
      localStorage.removeItem(EXPIRES_AT_KEY)
      if (typeof sessionStorage !== 'undefined') {
        sessionStorage.removeItem(TOKEN_KEY)
        sessionStorage.removeItem(PROFILE_KEY)
        sessionStorage.removeItem(EXPIRES_AT_KEY)
      }
    },
  },
})

// 监听来自扩展的消息（用于扩展主动请求同步）
if (typeof window !== 'undefined') {
  window.addEventListener('message', (event) => {
    // 只接受来自扩展的消息
    if (event.data && event.data.source === 'chenxi-extension') {
      if (event.data.action === 'requestAuthStatus') {
        // 扩展请求当前认证状态
        const token = localStorage.getItem(TOKEN_KEY)
        const profile = localStorage.getItem(PROFILE_KEY)

        event.source.postMessage({
          source: 'chenxi-website',
          action: 'authStatusResponse',
          data: {
            token,
            profile: profile ? JSON.parse(profile) : null,
            timestamp: Date.now()
          }
        }, event.origin)
      }
    }
  })
}
