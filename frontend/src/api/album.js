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

  // 获取图集详情
  getAlbumDetail(albumUuid) {
    return http.get(`/api/albums/${albumUuid}`)
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

  // 获取公开图集随机图片URL
  getPublicAlbumUrl(pathSlug) {
    return `${import.meta.env.VITE_API_BASE_URL || ''}/picture/${pathSlug}`
  }
}

export default albumApi
