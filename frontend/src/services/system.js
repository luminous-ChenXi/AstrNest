import http from './http'

export const getSystemConfig = () => http.get('/api/system/public-config')

export const fetchSystemConfig = async () => {
  const { data } = await http.get('/api/admin/system-config')
  return data
}

export const updateSystemConfig = async (payload) => {
  const { data } = await http.put('/api/admin/system-config', payload)
  return data
}

export const fetchSystemInsights = async () => {
  const { data } = await http.get('/api/admin/system-config/insights')
  return data
}

/** 管理端「注册与登录安全」设置：两开关 + SMTP 就绪状态（仅 ADMIN） */
export const fetchSecuritySettings = () => http.get('/api/admin/security-settings')

export const updateSecuritySettings = (payload) => http.put('/api/admin/security-settings', payload)
