import { defineStore } from 'pinia'

const TOKEN_KEY = 'astrnest_auth_token'
const PROFILE_KEY = 'astrnest_auth_profile'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: (() => {
      const cached = localStorage.getItem(PROFILE_KEY)
      return cached ? JSON.parse(cached) : null
    })(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.profile?.roles?.includes('ADMIN') ?? false,
    displayName: (state) => state.profile?.displayName || state.profile?.username || '未登录',
  },
  actions: {
    setSession(token, profile) {
      this.token = token
      this.profile = profile
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
    },
    updateProfile(profile) {
      this.profile = profile
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PROFILE_KEY)
    },
  },
})
