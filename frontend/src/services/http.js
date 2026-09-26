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
  // 同源相对路径：生产由 nginx 反代 /api，开发由 vite proxy 转发
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
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
    if (auth.token.startsWith('Basic ')) {
      // 旧版 "Basic xxx" 整串 token：原样发送，平滑过渡（下次登录自动换成 JWT）
      config.headers.Authorization = auth.token
    } else {
      // 新版纯 JWT：按 tokenType（默认 Bearer）拼接
      const tokenType = auth.tokenType || 'Bearer'
      config.headers.Authorization = tokenType ? `${tokenType} ${auth.token}` : auth.token
    }
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

  // 413 文件过大错误 - 人性化提示
  if (status === 413) {
    // 判断是 Nginx 层还是应用层返回的 413
    const isNginxError = typeof data === 'string' && data.includes('<html>')
    if (isNginxError) {
      return '文件总大小超限！请减少同时上传的文件数量，或分批上传（建议每批不超过 10-15 张图片）'
    }
    return '单个文件太大啦！请压缩图片或视频后再试，建议单张图片不超过 5MB，视频不超过 100MB'
  }

  if (status >= 500) return backendMsg ? `服务器错误 (${status})：${backendMsg}` : `服务器错误 (${status})`
  if (status === 404) return backendMsg ? `资源不存在：${backendMsg}` : '资源不存在'
  if (status === 401 || status === 403) return backendMsg || '未登录或无权限'
  return backendMsg ? `请求失败：${backendMsg}` : `请求失败 (${status})`
}

http.interceptors.response.use(
  async (response) => {
    // 令牌滑动续期：后端认证成功且剩余有效期不足一半时，通过该响应头下发新 JWT
    const refreshedToken = response?.headers?.['x-astrnest-refreshed-token']
    if (refreshedToken) {
      try {
        const auth = await getAuthStore()
        if (auth.isAuthenticated) {
          auth.refreshSession(refreshedToken)
        }
      } catch (error) {
        // 续期失败不影响当前请求
      }
    }
    return response
  },
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
        // 清理本地失效凭据，避免反复带无效 Authorization 触发 401 循环
        auth.logout()
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        if (!window.location.search.includes('login=1')) {
          window.location.href = `/?login=1&redirect=${redirect}`
        }
      }
    } else {
      console.error('Network error', error?.message)
    }
    return Promise.reject(error)
  }
)

export default http
