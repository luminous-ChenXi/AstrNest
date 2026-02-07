import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const VISITOR_TOKEN_STORAGE_KEY = 'chenxi-visitor-token'

const ensureVisitorToken = () => {
  if (typeof window === 'undefined') {
    return null
  }
  let token = localStorage.getItem(VISITOR_TOKEN_STORAGE_KEY)
  if (!token) {
    const randomSource = window.crypto?.randomUUID ? window.crypto.randomUUID() : Math.random().toString(36).slice(2)
    token = `chenxi-${randomSource}`
    localStorage.setItem(VISITOR_TOKEN_STORAGE_KEY, token)
  }
  return token
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (!config.headers) {
    config.headers = {}
  }
  if (auth.token) {
    config.headers.Authorization = auth.token
  }
  const visitorToken = ensureVisitorToken()
  if (visitorToken) {
    config.headers['X-Chenxi-Visitor'] = visitorToken
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      console.error('API error', error.response.status, error.response.data)
      if (error.response.status === 401) {
        const auth = useAuthStore()
        auth.logout()
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
      }
    } else {
      console.error('Network error', error.message)
    }
    return Promise.reject(error)
  }
)

export default http
