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

  // 返回新的批次响应格式
  return data
}

/**
 * 分批上传文件，避免 Nginx 413 错误
 * 每批大约 20MB（考虑视频封面等额外数据）
 * @param {File[]} files - 文件列表
 * @param {string[]} tags - 标签列表
 * @param {Function} onProgress - 进度回调
 * @returns {Promise<{uploaded: Array, skipped: Array, message: string}>}
 */
export const uploadFilesBatch = async (files, tags = [], onProgress = null) => {
  if (!files?.length) {
    throw new Error('请选择至少一个文件')
  }

  // 分批策略：每批总大小不超过 20MB（预留空间给封面和表单数据）
  const BATCH_SIZE_LIMIT = 20 * 1024 * 1024 // 20MB
  const batches = []
  let currentBatch = []
  let currentBatchSize = 0

  for (const file of files) {
    const fileSize = file?.size || 0

    // 如果当前批次加上这个文件会超限，且当前批次不为空，则创建新批次
    if (currentBatchSize + fileSize > BATCH_SIZE_LIMIT && currentBatch.length > 0) {
      batches.push(currentBatch)
      currentBatch = [file]
      currentBatchSize = fileSize
    } else {
      currentBatch.push(file)
      currentBatchSize += fileSize
    }
  }

  // 添加最后一批
  if (currentBatch.length > 0) {
    batches.push(currentBatch)
  }

  // 如果只有一批，直接上传
  if (batches.length === 1) {
    const result = await uploadFiles(files, tags, onProgress)
    // 兼容新旧响应格式
    if (result && Array.isArray(result.uploaded)) {
      return result
    }
    if (Array.isArray(result)) {
      return {
        uploaded: result,
        skipped: [],
        message: `全部 ${result.length} 个文件上传成功`
      }
    }
    return result
  }

  // 多批次上传
  const allUploaded = []
  const allSkipped = []

  for (let i = 0; i < batches.length; i++) {
    const batch = batches[i]
    const batchProgress = {
      stage: 'uploading',
      current: Math.round((i / batches.length) * 100),
      total: 100
    }
    onProgress?.(batchProgress)

    try {
      const result = await uploadFiles(batch, tags, (progress) => {
        // 将单批进度映射到总进度
        if (progress.stage === 'uploading') {
          const batchPercent = progress.current / 100
          const totalPercent = ((i + batchPercent) / batches.length) * 100
          onProgress?.({
            stage: 'uploading',
            current: Math.round(totalPercent),
            total: 100
          })
        } else {
          onProgress?.(progress)
        }
      })

      // 处理响应 - 兼容新旧格式
      if (result && Array.isArray(result.uploaded)) {
        allUploaded.push(...result.uploaded)
        allSkipped.push(...(result.skipped || []))
      } else if (Array.isArray(result)) {
        allUploaded.push(...result)
      }
    } catch (error) {
      // 如果某一批失败，记录错误但继续上传其他批次
      console.error(`第 ${i + 1} 批上传失败:`, error)
      allSkipped.push(...batch.map(f => ({
        fileName: f.name,
        reason: error?.response?.data?.message || '上传失败'
      })))
    }
  }

  // 构建最终消息
  let message
  if (allSkipped.length === 0) {
    message = `全部 ${allUploaded.length} 个文件上传成功（分 ${batches.length} 批）`
  } else if (allUploaded.length === 0) {
    message = '上传失败，所有文件未能上传，请检查文件大小或网络连接'
  } else {
    message = `部分文件上传成功，共上传 ${allUploaded.length} 个文件，${allSkipped.length} 个文件失败（分 ${batches.length} 批）`
  }

  return {
    uploaded: allUploaded,
    skipped: allSkipped,
    message
  }
}

export const fetchUploadLimits = async () => {
  const { data } = await http.get('/api/upload/limits')
  return data
}
