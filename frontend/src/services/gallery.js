import http from './http'

export const fetchPublicGallery = async ({ page = 0, size = 9 } = {}) => {
  const { data } = await http.get('/api/gallery/public', {
    params: { page, size },
  })
  return data
}

export const likeImage = async (imageId) => {
  const { data } = await http.post(`/api/gallery/${imageId}/like`)
  return data
}

export const unlikeImage = async (imageId) => {
  const { data } = await http.delete(`/api/gallery/${imageId}/like`)
  return data
}

export const fetchPublicGalleryMetrics = async () => {
  const { data } = await http.get('/api/gallery/public/metrics')
  return data
}

export const searchGalleryByTag = async ({ keyword = '', page = 0, size = 21 } = {}) => {
  const normalizedKeyword = typeof keyword === 'string' ? keyword.trim() : ''
  const { data } = await http.get('/api/gallery/search', {
    params: { keyword: normalizedKeyword, page, size },
  })
  return data
}

export const fetchTopLikedImages = async (limit = 3) => {
  const { data } = await http.get('/api/gallery/public/top-liked', {
    params: { limit },
  })
  return data
}
