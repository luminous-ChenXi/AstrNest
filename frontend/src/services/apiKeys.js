import http from './http'

// Admin endpoints
export const fetchAdminApiKeys = (params) => http.get('/api/keys', { params })
export const fetchAdminApiKeyDashboard = () => http.get('/api/keys/dashboard')
export const fetchAdminApiKeyOwners = () => http.get('/api/keys/owners')
export const createAdminApiKey = (payload) => http.post('/api/keys', payload)
export const updateAdminApiKeyStatus = (id, active) => http.put(`/api/keys/${id}/status`, { active })
export const updateAdminApiKeyLimits = (id, payload) => http.put(`/api/keys/${id}/quota`, payload)
export const resetAdminApiKey = (id) => http.post(`/api/keys/${id}/reset`)
export const deleteAdminApiKey = (id) => http.delete(`/api/keys/${id}`)

// User endpoints
export const fetchUserApiKeys = () => http.get('/api/user/api-keys')
export const createUserApiKey = (payload) => http.post('/api/user/api-keys', payload)
export const updateUserApiKeyStatus = (id, active) => http.put(`/api/user/api-keys/${id}/status`, { active })
export const resetUserApiKey = (id) => http.post(`/api/user/api-keys/${id}/reset`)
export const deleteUserApiKey = (id) => http.delete(`/api/user/api-keys/${id}`)
