import axios from 'axios'
import { ElMessage } from 'element-plus'

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

// 延迟导入 auth store 避免循环依赖
const getAuthStore = async () => {
  const { useAuthStore } = await import('../stores/auth.js')
  return useAuthStore()
}

http.interceptors.request.use(async (config) => {
  const auth = await getAuthStore()
  auth.pruneIfExpired()
  if (!config.headers) {
    config.headers = {}
  }
  if (auth.isAuthenticated && auth.token) {
    config.headers.Authorization = auth.token
    auth.touchSession()
  }
  const visitorToken = ensureVisitorToken()
  if (visitorToken) {
    config.headers['X-Chenxi-Visitor'] = visitorToken
  }
  return config
})

const formatErrorMessage = (error) => {
  if (error?.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  if (!error?.response) return '网络异常，请检查连接'
  const { status, data } = error.response
  const backendMsg = data?.message || data?.error || ''
  if (status >= 500) return backendMsg ? `服务器错误 (${status})：${backendMsg}` : `服务器错误 (${status})`
  if (status === 404) return backendMsg ? `资源不存在：${backendMsg}` : '资源不存在'
  if (status === 401 || status === 403) return backendMsg || '未登录或无权限'
  return backendMsg ? `请求失败：${backendMsg}` : `请求失败 (${status})`
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const url = error?.config?.url || ''
    const isAuthAttempt = url.includes('/api/auth/login') || url.includes('/api/auth/register')
    const message = formatErrorMessage(error)

    // 对登录/注册请求，交由调用方处理错误，不弹出全局消息、不跳转
    if (!isAuthAttempt && message) {
      ElMessage.error(message)
    }

    if (error?.response) {
      console.error('API error', error.response.status, error.response.data)
      if (!isAuthAttempt && (error.response.status === 401 || error.response.status === 403)) {
        const auth = await getAuthStore()
        auth.logout()
        if (!window.location.search.includes('login=1')) {
          window.location.href = '/?login=1'
        }
      }
    } else {
      console.error('Network error', error?.message)
    }
    return Promise.reject(error)
  }
)

export default http
