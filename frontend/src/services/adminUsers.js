import http from './http'

export const fetchAdminUsers = () => http.get('/api/admin/users')

export const updateAdminUserLimits = (id, payload) => http.put(`/api/admin/users/${id}/limits`, payload)

export const updateAdminUserRole = (id, payload) => http.put(`/api/admin/users/${id}/role`, payload)

export const deleteAdminUser = (id) => http.delete(`/api/admin/users/${id}`)
