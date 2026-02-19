import http from '../services/http'

export const albumApi = {
  // 创建图集
  createAlbum(data) {
    return http.post('/api/albums', data)
  },

  // 获取我的图集列表
  getMyAlbums(params = {}) {
    return http.get('/api/albums', { params })
  },

  // 获取图集详情（通过UUID）
  getAlbumDetail(albumUuid) {
    return http.get(`/api/albums/${albumUuid}`)
  },

  // 获取公开图集详情（通过路径标识）
  getAlbumByPathSlug(pathSlug) {
    return http.get(`/api/albums/public/${pathSlug}`)
  },

  // 获取图集中的所有图片（通过路径标识）
  getAlbumMedias(pathSlug) {
    return http.get(`/api/albums/public/${pathSlug}/medias`)
  },

  // 更新图集
  updateAlbum(albumUuid, data) {
    return http.put(`/api/albums/${albumUuid}`, data)
  },

  // 删除图集
  deleteAlbum(albumUuid) {
    return http.delete(`/api/albums/${albumUuid}`)
  },

  // 添加图片到图集
  addMediaToAlbum(albumUuid, mediaUuid) {
    return http.post(`/api/albums/${albumUuid}/medias`, { mediaUuid })
  },

  // 从图集移除图片
  removeMediaFromAlbum(albumUuid, mediaUuid) {
    return http.delete(`/api/albums/${albumUuid}/medias/${mediaUuid}`)
  },

  // 获取公开图集随机图片URL（新路径：/api/albums/random/{pathSlug}）
  getPublicAlbumUrl(pathSlug) {
    return `${import.meta.env.VITE_API_BASE_URL || ''}/api/albums/random/${pathSlug}`
  },

  // 获取首页Featured图集（最受欢迎的公开图集）
  getFeaturedAlbums() {
    return http.get('/api/albums/featured')
  },

  // 获取可添加到图集的图片列表
  getAvailableMedias(albumUuid, params = {}) {
    return http.get(`/api/albums/${albumUuid}/available-medias`, { params })
  }
}

export default albumApi
