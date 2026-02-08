import http from './http'

export const fetchPublicAnnouncements = (params = {}) => {
  return http.get('/api/public/announcements', { params })
}

export const fetchPublicAnnouncementDetail = (id) => {
  return http.get(`/api/public/announcements/${id}`)
}

export const fetchAdminAnnouncements = (params = {}) => {
  return http.get('/api/admin/announcements', { params })
}

export const fetchAdminAnnouncementDetail = (id) => {
  return http.get(`/api/admin/announcements/${id}`)
}

export const createAnnouncement = (payload) => {
  return http.post('/api/admin/announcements', payload)
}

export const updateAnnouncement = (id, payload) => {
  return http.put(`/api/admin/announcements/${id}`, payload)
}

export const deleteAnnouncement = (id) => {
  return http.delete(`/api/admin/announcements/${id}`)
}
