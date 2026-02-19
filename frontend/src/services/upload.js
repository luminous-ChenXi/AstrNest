import http from './http'

const inferExtension = (file) => {
  if (file?.name && file.name.includes('.')) {
    return file.name.substring(file.name.lastIndexOf('.'))
  }
  if (file?.type) {
    const subtype = file.type.split('/')[1]
    if (subtype) {
      return `.${subtype}`
    }
  }
  return '.png'
}

const isClipboardFile = (file) => {
  if (!file?.name) return true
  const trimmed = file.name.trim().toLowerCase()
  return !trimmed || trimmed === 'blob'
}

const randomDigits = (length = 8) => {
  let result = ''
  for (let i = 0; i < length; i += 1) {
    result += Math.floor(Math.random() * 10)
  }
  return result
}

const renameClipboardFile = (file) => {
  const extension = inferExtension(file)
  const newName = `luminouscx${randomDigits(8)}${extension}`
  return new File([file], newName, { type: file.type || 'image/png' })
}

export const uploadFiles = async (files, tags = []) => {
  if (!files?.length) {
    throw new Error('请选择至少一个文件')
  }
  const normalized = files.map((file) => {
    if (!isClipboardFile(file)) {
      return file
    }
    return renameClipboardFile(file)
  })
  const formData = new FormData()
  for (const file of normalized) {
    formData.append('files', file, file.name)
  }
  if (Array.isArray(tags)) {
    tags
      .map((tag) => (typeof tag === 'string' ? tag.trim() : ''))
      .filter((tag) => tag?.length)
      .forEach((tag) => formData.append('tags', tag))
  }
  const { data } = await http.post('/api/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export const fetchUploadLimits = async () => {
  const { data } = await http.get('/api/upload/limits')
  return data
}
