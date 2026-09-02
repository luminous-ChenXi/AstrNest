import http from './http'

export const fetchAdminUploads = (params) => http.get('/api/admin/uploads', { params })
export const updateAdminVisibility = (id, payload) => http.put(`/api/admin/uploads/${id}/visibility`, payload)
export const updateAdminViolation = (id, payload) => http.put(`/api/admin/uploads/${id}/violation`, payload)
export const deleteAdminUpload = (id) => http.delete(`/api/admin/uploads/${id}`)
