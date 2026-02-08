import http from './http'

export const fetchOverview = () => http.get('/api/user/overview')
export const fetchUploads = (params) => http.get('/api/user/uploads', { params })
export const fetchUploadDetail = (id) => http.get(`/api/user/uploads/${id}`)
export const toggleUploadLike = (id) => http.post(`/api/user/uploads/${id}/like`)
export const updateUploadVisibility = (id, payload) => http.put(`/api/user/uploads/${id}/visibility`, payload)
export const deleteUpload = (id) => http.delete(`/api/user/uploads/${id}`)
export const deleteUploadsBatch = (ids) => http.post('/api/user/uploads/batch-delete', { ids })
export const fetchProfile = () => http.get('/api/user/profile')
export const updateProfile = (payload) => http.put('/api/user/profile', payload)
export const changePassword = (payload) => http.post('/api/user/security/password', payload)
export const fetchSecuritySettings = () => http.get('/api/user/security/settings')
