import http from './http'

export const listStorageStrategies = () =>
  http.get('/api/admin/storage/strategies').then((response) => response.data)

export const createStorageStrategy = (payload) =>
  http.post('/api/admin/storage/strategies', payload).then((response) => response.data)

export const updateStorageStrategy = (id, payload) =>
  http.put(`/api/admin/storage/strategies/${id}`, payload).then((response) => response.data)

export const deleteStorageStrategy = (id) =>
  http.delete(`/api/admin/storage/strategies/${id}`).then((response) => response?.data)

export const activateStorageStrategy = (id) =>
  http.post(`/api/admin/storage/strategies/${id}/activate`).then((response) => response.data)
