import http from './http'

export const fetchAdminUsers = () => http.get('/api/admin/users')

export const updateAdminUserLimits = (id, payload) => http.put(`/api/admin/users/${id}/limits`, payload)

export const updateAdminUserRole = (id, payload) => http.put(`/api/admin/users/${id}/role`, payload)

export const deleteAdminUser = (id) => http.delete(`/api/admin/users/${id}`)

/** 重置指定用户的 2FA（TOTP）绑定（管理员操作，下次登录重新强制绑定） */
export const resetUserTotp = (id) => http.put(`/api/admin/users/${id}/2fa/reset`)
