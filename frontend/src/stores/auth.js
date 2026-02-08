import { defineStore } from 'pinia'

const TOKEN_KEY = 'astrnest_auth_token'
const PROFILE_KEY = 'astrnest_auth_profile'
const EXPIRES_AT_KEY = 'astrnest_auth_expires_at'
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000

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
    },
    pruneIfExpired() {
      if (this.expiresAt && this.expiresAt <= Date.now()) {
        this.logout()
        return true
      }
      return false
    },
    logout() {
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
