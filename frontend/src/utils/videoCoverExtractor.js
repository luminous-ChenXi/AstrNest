/**
 * 视频封面提取工具
 * 从视频文件中提取第一帧作为封面图片
 */

/**
 * 从视频文件中提取封面
 * @param {File} videoFile - 视频文件
 * @param {number} timeInSeconds - 截取时间点（秒），默认第1秒
 * @returns {Promise<File>} - 返回封面图片文件
 */
export const extractVideoCover = (videoFile, timeInSeconds = 1) => {
  return new Promise((resolve, reject) => {
    if (!videoFile || !videoFile.type.startsWith('video/')) {
      reject(new Error('请提供有效的视频文件'))
      return
    }

    const video = document.createElement('video')
    const canvas = document.createElement('canvas')
    const objectUrl = URL.createObjectURL(videoFile)

    video.preload = 'metadata'
    video.muted = true
    video.playsInline = true

    video.onloadedmetadata = () => {
      // 设置截取时间点
      const seekTime = Math.min(timeInSeconds, video.duration || timeInSeconds)
      video.currentTime = seekTime
    }

    video.onseeked = () => {
      try {
        // 设置 canvas 尺寸为视频原始尺寸
        canvas.width = video.videoWidth || 640
        canvas.height = video.videoHeight || 480

        const ctx = canvas.getContext('2d')
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

        // 转换为 blob
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              URL.revokeObjectURL(objectUrl)
              reject(new Error('封面提取失败'))
              return
            }

            // 生成封面文件名
            const videoName = videoFile.name.replace(/\.[^/.]+$/, '')
            const coverFileName = `${videoName}_cover.jpg`

            // 创建 File 对象
            const coverFile = new File([blob], coverFileName, {
              type: 'image/jpeg',
              lastModified: Date.now()
            })

            URL.revokeObjectURL(objectUrl)
            resolve(coverFile)
          },
          'image/jpeg',
          0.9
        )
      } catch (error) {
        URL.revokeObjectURL(objectUrl)
        reject(error)
      }
    }

    video.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('视频加载失败'))
    }

    video.src = objectUrl
  })
}

/**
 * 批量提取视频封面
 * @param {File[]} videoFiles - 视频文件数组
 * @param {Function} onProgress - 进度回调 (current, total)
 * @returns {Promise<Map<string, File>>} - 返回视频文件名到封面文件的映射
 */
export const extractVideoCovers = async (videoFiles, onProgress = null) => {
  const coverMap = new Map()
  const total = videoFiles.length

  for (let i = 0; i < videoFiles.length; i++) {
    const videoFile = videoFiles[i]
    try {
      const coverFile = await extractVideoCover(videoFile)
      coverMap.set(videoFile.name, coverFile)
    } catch (error) {
      console.warn(`提取视频封面失败: ${videoFile.name}`, error)
    }

    if (onProgress) {
      onProgress(i + 1, total)
    }
  }

  return coverMap
}

/**
 * 检查文件是否为视频
 * @param {File} file - 文件对象
 * @returns {boolean}
 */
export const isVideoFile = (file) => {
  if (!file) return false
  return file.type?.startsWith('video/') ||
    /\.(mp4|webm|mov|ogv|avi|mkv)$/i.test(file.name)
}

export default {
  extractVideoCover,
  extractVideoCovers,
  isVideoFile
}
