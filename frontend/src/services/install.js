import axios from 'axios'

// 独立的轻量 axios 实例：安装向导运行在系统尚未就绪阶段，
// 不能复用全局 http 实例（其拦截器会因 401/403 触发登出与跳转、弹出全局错误消息）
const installHttp = axios.create({
  // 同源相对路径：生产由 nginx 反代 /api，开发由 vite proxy 转发
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000,
})

const extractMessage = (error, fallback) => {
  const data = error?.response?.data
  if (data && typeof data === 'object' && data.message) return data.message
  if (typeof data === 'string' && data) return data
  if (error?.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  if (!error?.response) return '无法连接服务器，请确认后端已启动'
  return fallback
}

export const getInstallError = (error, fallback = '请求失败') => extractMessage(error, fallback)

/** GET /api/install/status → { installed, schemaState, finished, locked, checks:[{id,name,passed,warning,detail}] } */
export const fetchInstallStatus = async () => {
  const { data } = await installHttp.get('/api/install/status', { timeout: 10000 })
  return data
}

/** POST /api/install/database → 建表摘要（建表数/跳过项） */
export const runDatabaseInstall = async () => {
  const { data } = await installHttp.post('/api/install/database', {}, { timeout: 120000 })
  return data
}

/** POST /api/install/site-config → { success, message }（站点配置步骤，可跳过不调用） */
export const saveSiteConfig = async (payload) => {
  const { data } = await installHttp.post('/api/install/site-config', payload, { timeout: 30000 })
  return data
}

/** POST /api/install/admin → { username, email, displayName, role } */
export const createInstallAdmin = async (payload) => {
  const { data } = await installHttp.post('/api/install/admin', payload, { timeout: 30000 })
  return data
}

/** POST /api/install/finish → { success, installedAt, message } */
export const finishInstall = async () => {
  const { data } = await installHttp.post('/api/install/finish', {}, { timeout: 30000 })
  return data
}
