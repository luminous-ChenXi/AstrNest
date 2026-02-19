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
