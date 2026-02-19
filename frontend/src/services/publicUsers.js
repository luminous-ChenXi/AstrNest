import http from './http'

export const fetchPublicUserProfile = (userId) => http.get(`/api/public/users/${userId}`)
