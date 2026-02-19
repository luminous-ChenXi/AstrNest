import http from './http'
import { extractVideoCover, isVideoFile } from '../utils/videoCoverExtractor'

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

export const uploadFiles = async (files, tags = [], onProgress = null) => {
  if (!files?.length) {
    throw new Error('请选择至少一个文件')
  }

  // 1. 处理文件名（剪贴板文件重命名）
  const normalized = files.map((file) => {
    if (!isClipboardFile(file)) {
      return file
    }
    return renameClipboardFile(file)
  })

  // 2. 提取视频封面
  const videoFiles = normalized.filter(isVideoFile)
  const coverMap = new Map()

  if (videoFiles.length > 0) {
    onProgress?.({ stage: 'extracting', current: 0, total: videoFiles.length })

    for (let i = 0; i < videoFiles.length; i++) {
      const videoFile = videoFiles[i]
      try {
        const coverFile = await extractVideoCover(videoFile)
        coverMap.set(videoFile.name, coverFile)
      } catch (error) {
        console.warn(`提取视频封面失败: ${videoFile.name}`, error)
      }
      onProgress?.({ stage: 'extracting', current: i + 1, total: videoFiles.length })
    }
  }

  // 3. 构建 FormData
  const formData = new FormData()

  // 添加原始文件
  for (const file of normalized) {
    formData.append('files', file, file.name)
  }

  // 添加视频封面（与视频文件名关联）
  for (const [videoName, coverFile] of coverMap) {
    formData.append('videoCovers', coverFile, coverFile.name)
    formData.append('videoCoverMapping', videoName)
  }

  // 添加标签
  if (Array.isArray(tags)) {
    tags
      .map((tag) => (typeof tag === 'string' ? tag.trim() : ''))
      .filter((tag) => tag?.length)
      .forEach((tag) => formData.append('tags', tag))
  }

  onProgress?.({ stage: 'uploading', current: 0, total: 100 })

  const { data } = await http.post('/api/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress?.({ stage: 'uploading', current: percent, total: 100 })
      }
    }
  })

  return data
}

export const fetchUploadLimits = async () => {
  const { data } = await http.get('/api/upload/limits')
  return data
}
