import http from './http'

export const searchTags = async ({ keyword = '', limit = 30 } = {}) => {
  const { data } = await http.get('/api/tags', {
    params: { keyword, limit },
  })
  return data || []
}

export const createTag = async ({ name, description }) => {
  const { data } = await http.post('/api/tags', {
    name,
    description,
  })
  return data
}
