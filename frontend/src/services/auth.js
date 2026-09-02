import http from './http'

export const login = (payload) => http.post('/api/auth/login', payload)
