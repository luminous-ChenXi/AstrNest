<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Heart, Eye, Share2, Download, Copy, ExternalLink, Image, Sparkles, Plus, Settings } from 'lucide-vue-next'
import { albumApi } from '../../api/album'
import { useSystemStore } from '../../stores/system'
import { useAuthStore } from '../../stores/auth'
import GalleryPreviewModal from '../../components/public/GalleryPreviewModal.vue'

const route = useRoute()
const router = useRouter()
const systemStore = useSystemStore()
const auth = useAuthStore()

const album = ref(null)
const medias = ref([])
const loading = ref(false)
const currentImageIndex = ref(0)
const showLightbox = ref(false)

// 预览弹窗相关
const previewVisible = ref(false)
const previewItem = ref(null)

// 添加图片相关
const showAddMediaDialog = ref(false)
const availableMedias = ref([])
const loadingAvailable = ref(false)
const selectedMediaUuids = ref([])
const addingMedia = ref(false)

// 编辑图集相关
const showEditDialog = ref(false)
const editingAlbum = ref({
  name: '',
  description: '',
  isPublic: true
})
const savingAlbum = ref(false)

// 瀑布流列数
const columnCount = ref(4)

// 根据屏幕宽度调整列数
const updateColumnCount = () => {
  if (typeof window === 'undefined') return
  const width = window.innerWidth
  if (width < 640) columnCount.value = 2
  else if (width < 1024) columnCount.value = 3
  else if (width < 1280) columnCount.value = 4
  else columnCount.value = 5
}

// 将图片分配到各列（瀑布流算法）
const columns = computed(() => {
  // 如果没有媒体数据，返回空列数组
  if (!medias.value || medias.value.length === 0) {
    return Array.from({ length: columnCount.value }, () => [])
  }

  const cols = Array.from({ length: columnCount.value }, () => [])
  const colHeights = Array(columnCount.value).fill(0)

  medias.value.forEach((media, index) => {
    if (!media) return // 跳过无效的媒体数据

    // 找到最短的列
    const shortestCol = colHeights.indexOf(Math.min(...colHeights))
    cols[shortestCol].push({ ...media, originalIndex: index })
    // 累加高度（使用宽高比估算）
    const aspectRatio = media.height && media.width ? media.height / media.width : 1
    colHeights[shortestCol] += aspectRatio
  })

  return cols
})

const normalizedDomain = computed(() => {
  const domain = systemStore.config?.assetDomain
  if (!domain) {
    // 确保在浏览器环境中
    return typeof window !== 'undefined' ? window.location.origin : ''
  }
  return domain.replace(/\/$/, '')
})

// 随机图片URL（用于分享）- 使用当前站点域名，而非CDN域名
const randomImageUrl = computed(() => {
  if (!album.value) return ''
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/api/albums/random/${album.value.pathSlug}`
})

// 图集详情页URL
const albumDetailUrl = computed(() => {
  if (!album.value) return ''
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/album/${album.value.pathSlug}`
})

const fetchAlbumDetail = async () => {
  const pathSlug = route.params.pathSlug
  if (!pathSlug) {
    ElMessage.error('图集路径不能为空')
    router.push('/')
    return
  }

  loading.value = true
  try {
    // 获取图集详情
    const res = await albumApi.getAlbumByPathSlug(pathSlug)
    album.value = res.data

    // 获取图集中的所有图片
    const mediaRes = await albumApi.getAlbumMedias(pathSlug)
    medias.value = mediaRes.data || []
  } catch (error) {
    if (error.response?.status === 404) {
      ElMessage.error('图集不存在')
    } else if (error.response?.status === 403) {
      const errorMsg = error.response?.data?.message || '此图集为私有图集或没有权限，无法访问'
      ElMessage.error(errorMsg)
    } else {
      ElMessage.error('获取图集失败')
    }
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 打开添加图片对话框
const openAddMediaDialog = async () => {
  if (!album.value?.canAddMedia) {
    ElMessage.warning('只有图集创建者才能添加图片')
    return
  }
  showAddMediaDialog.value = true
  selectedMediaUuids.value = []
  await fetchAvailableMedias()
}

// 获取可添加的图片列表
const fetchAvailableMedias = async () => {
  if (!album.value?.albumUuid) return
  loadingAvailable.value = true
  try {
    const res = await albumApi.getAvailableMedias(album.value.albumUuid)
    availableMedias.value = res.data?.content || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '获取图片列表失败')
  } finally {
    loadingAvailable.value = false
  }
}

// 添加选中的图片到图集
const addSelectedMedias = async () => {
  if (selectedMediaUuids.value.length === 0) {
    ElMessage.warning('请至少选择一张图片')
    return
  }
  addingMedia.value = true
  try {
    for (const mediaUuid of selectedMediaUuids.value) {
      await albumApi.addMediaToAlbum(album.value.albumUuid, mediaUuid)
    }
    ElMessage.success(`成功添加 ${selectedMediaUuids.value.length} 张图片`)
    showAddMediaDialog.value = false
    // 刷新图集
    await fetchAlbumDetail()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '添加图片失败')
  } finally {
    addingMedia.value = false
  }
}

// 打开编辑图集对话框
const openEditDialog = () => {
  if (!album.value?.canEdit) {
    ElMessage.warning('没有权限编辑此图集')
    return
  }
  editingAlbum.value = {
    name: album.value.name,
    description: album.value.description || '',
    isPublic: album.value.isPublic
  }
  showEditDialog.value = true
}

// 保存图集修改
const saveAlbumEdit = async () => {
  if (!editingAlbum.value.name?.trim()) {
    ElMessage.warning('请输入图集名称')
    return
  }
  savingAlbum.value = true
  try {
    await albumApi.updateAlbum(album.value.albumUuid, editingAlbum.value)
    ElMessage.success('图集更新成功')
    showEditDialog.value = false
    // 刷新图集
    await fetchAlbumDetail()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新图集失败')
  } finally {
    savingAlbum.value = false
  }
}

// 切换图片选择状态
const toggleMediaSelection = (mediaUuid) => {
  const index = selectedMediaUuids.value.indexOf(mediaUuid)
  if (index > -1) {
    selectedMediaUuids.value.splice(index, 1)
  } else {
    selectedMediaUuids.value.push(mediaUuid)
  }
}

const openLightbox = (index) => {
  currentImageIndex.value = index
  showLightbox.value = true
}

const closeLightbox = () => {
  showLightbox.value = false
}

const nextImage = () => {
  currentImageIndex.value = (currentImageIndex.value + 1) % medias.value.length
}

const prevImage = () => {
  currentImageIndex.value = (currentImageIndex.value - 1 + medias.value.length) % medias.value.length
}

// 打开预览弹窗
const openPreview = (media) => {
  previewItem.value = {
    ...media,
    publicUrl: media.imageLink,
    ownerDisplayName: album.value?.username || '未知用户',
    ownerId: album.value?.userId,
    album: {
      name: album.value?.name,
      pathSlug: album.value?.pathSlug
    }
  }
  previewVisible.value = true
}

const closePreview = () => {
  previewVisible.value = false
  previewItem.value = null
}

const handlePreviewCopy = (url) => {
  navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制')
}

const handleOpenAlbum = (albumData) => {
  closePreview()
  if (albumData?.pathSlug) {
    router.push(`/album/${albumData.pathSlug}`)
  }
}

const copyRandomUrl = () => {
  if (medias.value.length === 0) {
    ElMessage.warning('图集中暂无图片，无法复制链接')
    return
  }
  navigator.clipboard.writeText(randomImageUrl.value)
  ElMessage.success('随机图片链接已复制')
}

const handleGetRandomImage = () => {
  if (medias.value.length === 0) {
    ElMessage.warning('图集中暂无图片，请先上传图片')
    return
  }
  // 在新标签页打开随机图片
  window.open(randomImageUrl.value, '_blank')
}

const copyDetailUrl = () => {
  navigator.clipboard.writeText(albumDetailUrl.value)
  ElMessage.success('图集页面链接已复制')
}

const goBack = () => {
  // 如果有历史记录，返回上一页；否则跳转到首页
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

onMounted(() => {
  fetchAlbumDetail()
  updateColumnCount()
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', updateColumnCount)
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateColumnCount)
  }
})
</script>

<template>
  <div class="album-detail-page min-h-screen bg-gray-50">
    <!-- 顶部导航栏 -->
    <header class="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-gray-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-14">
          <button
            @click="goBack"
            class="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <ArrowLeft class="w-5 h-5" />
            <span class="text-sm font-medium">返回</span>
          </button>

          <h1 v-if="album" class="text-lg font-semibold text-gray-900 truncate max-w-xs sm:max-w-md">
            {{ album.name }}
          </h1>

          <div class="flex items-center gap-2">
            <button
              @click="copyRandomUrl"
              class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-full transition-all"
              title="复制随机图片链接"
            >
              <Share2 class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
    </div>

    <!-- 图集信息头部 -->
    <div v-else-if="album" class="bg-white border-b border-gray-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div class="flex-1">
            <h1 class="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">
              {{ album.name }}
            </h1>
            <p v-if="album.description" class="text-gray-500 text-sm sm:text-base">
              {{ album.description }}
            </p>
          </div>

          <div class="flex items-center gap-4 text-sm text-gray-500">
            <div class="flex items-center gap-1">
              <Image class="w-4 h-4" />
              <span>{{ formatNumber(album.mediaCount || medias.length) }} 张图片</span>
            </div>
            <div class="flex items-center gap-1">
              <Eye class="w-4 h-4" />
              <span>{{ formatNumber(album.accessCount || 0) }} 次访问</span>
            </div>
            <div v-if="!album.isPublic" class="flex items-center gap-1 text-amber-600">
              <span class="px-2 py-0.5 bg-amber-100 rounded text-xs">私密</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex flex-wrap items-center gap-3 mt-4">
          <button
            @click="handleGetRandomImage"
            :disabled="medias.length === 0"
            :class="[
              'inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-full transition-colors',
              medias.length === 0
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                : 'bg-indigo-600 text-white hover:bg-indigo-700'
            ]"
          >
            <ExternalLink class="w-4 h-4" />
            {{ medias.length === 0 ? '暂无图片' : '获取随机图片' }}
          </button>

          <button
            @click="copyRandomUrl"
            class="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 text-sm font-medium rounded-full hover:bg-gray-200 transition-colors"
          >
            <Copy class="w-4 h-4" />
            复制随机链接
          </button>

          <button
            @click="copyDetailUrl"
            class="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 text-sm font-medium rounded-full hover:bg-gray-200 transition-colors"
          >
            <Share2 class="w-4 h-4" />
            分享图集
          </button>

          <!-- 图集创建者专属按钮 -->
          <template v-if="album.canAddMedia">
            <button
              @click="openAddMediaDialog"
              class="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 text-white text-sm font-medium rounded-full hover:bg-emerald-700 transition-colors"
            >
              <Plus class="w-4 h-4" />
              添加图片
            </button>

            <button
              @click="openEditDialog"
              class="inline-flex items-center gap-2 px-4 py-2 bg-amber-600 text-white text-sm font-medium rounded-full hover:bg-amber-700 transition-colors"
            >
              <Settings class="w-4 h-4" />
              编辑图集
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- 瀑布流图片展示 -->
    <div v-if="!loading && medias.length > 0" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="flex gap-4">
        <div
          v-for="(column, colIndex) in columns"
          :key="colIndex"
          class="flex-1 flex flex-col gap-4"
        >
          <div
            v-for="media in column"
            :key="media.uuid"
            class="group relative bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer"
            @click="openPreview(media)"
          >
            <!-- 图片容器 - 小红书风格 -->
            <div class="relative overflow-hidden rounded-t-2xl">
              <img
                :src="media.thumbnailUrl || media.imageLink"
                :alt="media.fileName"
                class="w-full h-auto object-cover transform group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
              />

              <!-- 悬停遮罩 -->
              <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-300"></div>
            </div>

            <!-- 底部信息栏 - 小红书风格 -->
            <div class="p-3 rounded-b-2xl">
              <!-- 标题/描述 -->
              <p class="text-sm text-gray-800 font-medium line-clamp-2 mb-2 min-h-[2.5rem]">
                {{ media.fileName?.replace(/\.[^/.]+$/, '') || '未命名图片' }}
              </p>

              <!-- 上传者和互动数据 -->
              <div class="flex items-center justify-between">
                <!-- 左侧：上传者头像和名称 -->
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 rounded-full bg-gradient-to-br from-indigo-400 to-purple-400 flex items-center justify-center text-white text-xs font-medium">
                    {{ (album?.username || 'U').charAt(0).toUpperCase() }}
                  </div>
                  <span class="text-xs text-gray-500 truncate max-w-[80px]">{{ album?.username || '未知用户' }}</span>
                </div>

                <!-- 右侧：点赞和浏览 -->
                <div class="flex items-center gap-3">
                  <div class="flex items-center gap-1">
                    <Heart class="w-3.5 h-3.5 text-gray-400" />
                    <span class="text-xs text-gray-500">{{ formatNumber(media.likes || 0) }}</span>
                  </div>
                  <div class="flex items-center gap-1">
                    <Eye class="w-3.5 h-3.5 text-gray-400" />
                    <span class="text-xs text-gray-500">{{ formatNumber(media.views || 0) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && medias.length === 0" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
      <div class="flex flex-col items-center justify-center">
        <!-- 装饰性背景 -->
        <div class="relative mb-8">
          <div class="absolute inset-0 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-full blur-xl opacity-60 scale-150"></div>
          <div class="relative w-32 h-32 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-full flex items-center justify-center border border-indigo-100">
            <Image class="w-14 h-14 text-indigo-300" />
          </div>
          <!-- 装饰点 -->
          <div class="absolute -top-2 -right-2 w-6 h-6 bg-yellow-200 rounded-full opacity-60"></div>
          <div class="absolute -bottom-1 -left-3 w-4 h-4 bg-pink-200 rounded-full opacity-60"></div>
        </div>

        <!-- 主标题 -->
        <h3 class="text-2xl font-bold text-gray-800 mb-3">图集还是空的</h3>

        <!-- 描述文字 -->
        <p class="text-gray-500 text-center max-w-md mb-2">
          这个图集还没有添加任何图片
        </p>
        <p class="text-gray-400 text-sm text-center max-w-md mb-8">
          图集作者稍后会添加精彩图片，敬请期待～
        </p>

        <!-- 提示卡片 -->
        <div class="bg-gradient-to-r from-indigo-50 to-purple-50 rounded-2xl p-6 max-w-md w-full border border-indigo-100">
          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-white rounded-xl flex items-center justify-center shadow-sm flex-shrink-0">
              <Sparkles class="w-5 h-5 text-indigo-500" />
            </div>
            <div>
              <h4 class="font-semibold text-gray-800 mb-1">你可以</h4>
              <ul class="text-sm text-gray-600 space-y-2">
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 bg-indigo-400 rounded-full"></span>
                  复制图集链接分享给朋友
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 bg-indigo-400 rounded-full"></span>
                  稍后回来查看新上传的图片
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 bg-indigo-400 rounded-full"></span>
                  浏览其他精彩的图集
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex items-center gap-4 mt-8">
          <button
            @click="copyDetailUrl"
            class="inline-flex items-center gap-2 px-6 py-3 bg-white border border-gray-200 text-gray-700 font-medium rounded-full hover:bg-gray-50 hover:border-gray-300 transition-all"
          >
            <Copy class="w-4 h-4" />
            分享图集
          </button>
          <button
            v-if="album?.canAddMedia"
            @click="openAddMediaDialog"
            class="inline-flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white font-medium rounded-full hover:bg-emerald-700 transition-all"
          >
            <Plus class="w-4 h-4" />
            添加图片
          </button>
          <button
            v-else-if="!auth.isAuthenticated"
            @click="goBack"
            class="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white font-medium rounded-full hover:bg-indigo-700 transition-all"
          >
            <ArrowLeft class="w-4 h-4" />
            返回上一页
          </button>
          <button
            v-else
            @click="$router.push('/user/images')"
            class="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white font-medium rounded-full hover:bg-indigo-700 transition-all"
          >
            <ExternalLink class="w-4 h-4" />
            前往上传图片
          </button>
        </div>

        <!-- 提示信息 -->
        <div v-if="!album?.canAddMedia && auth.isAuthenticated" class="mt-6 text-center">
          <p class="text-gray-500 text-sm">
            你还没有上传过图片？
            <router-link to="/user/images" class="text-indigo-600 hover:text-indigo-700 font-medium">
              前往上传图片
            </router-link>
          </p>
        </div>
      </div>
    </div>

    <!-- 图片灯箱 -->
    <Teleport to="body">
      <div
        v-if="showLightbox"
        class="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
        @click="closeLightbox"
      >
        <!-- 关闭按钮 -->
        <button
          @click="closeLightbox"
          class="absolute top-4 right-4 p-2 text-white/70 hover:text-white transition-colors z-10"
        >
          <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <!-- 上一张 -->
        <button
          v-if="medias.length > 1"
          @click.stop="prevImage"
          class="absolute left-4 top-1/2 -translate-y-1/2 p-2 text-white/70 hover:text-white transition-colors z-10"
        >
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <!-- 下一张 -->
        <button
          v-if="medias.length > 1"
          @click.stop="nextImage"
          class="absolute right-4 top-1/2 -translate-y-1/2 p-2 text-white/70 hover:text-white transition-colors z-10"
        >
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <!-- 当前图片 -->
        <img
          v-if="medias[currentImageIndex]"
          :src="medias[currentImageIndex].imageLink"
          :alt="medias[currentImageIndex].fileName"
          class="max-w-[90vw] max-h-[90vh] object-contain"
          @click.stop
        />

        <!-- 图片计数 -->
        <div class="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/70 text-sm">
          {{ currentImageIndex + 1 }} / {{ medias.length }}
        </div>
      </div>
    </Teleport>

    <!-- 添加图片对话框 -->
    <Teleport to="body">
      <div
        v-if="showAddMediaDialog"
        class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
        @click="showAddMediaDialog = false"
      >
        <div
          class="bg-white rounded-2xl max-w-4xl w-full max-h-[80vh] flex flex-col"
          @click.stop
        >
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between p-6 border-b border-gray-100">
            <h3 class="text-xl font-bold text-gray-900">添加图片到图集</h3>
            <button
              @click="showAddMediaDialog = false"
              class="p-2 text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 对话框内容 -->
          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="loadingAvailable" class="flex items-center justify-center py-12">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <span class="ml-3 text-gray-500">加载中...</span>
            </div>

            <div v-else-if="availableMedias.length === 0" class="text-center py-12">
              <Image class="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <p class="text-gray-500 mb-2">没有可添加的图片</p>
              <p class="text-gray-400 text-sm mb-6">你已经将所有图片添加到此图集，或者还没有上传图片</p>
              <router-link
                to="/user/images"
                class="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-full hover:bg-indigo-700 transition-colors"
                @click="showAddMediaDialog = false"
              >
                <Plus class="w-4 h-4" />
                前往上传图片
              </router-link>
            </div>

            <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              <div
                v-for="media in availableMedias"
                :key="media.mediaUuid"
                class="relative group cursor-pointer"
                @click="toggleMediaSelection(media.mediaUuid)"
              >
                <div
                  class="aspect-square rounded-xl overflow-hidden border-2 transition-all"
                  :class="selectedMediaUuids.includes(media.mediaUuid) ? 'border-indigo-600 ring-2 ring-indigo-200' : 'border-gray-200 hover:border-gray-300'"
                >
                  <img
                    :src="media.thumbnailUrl || media.publicUrl"
                    :alt="media.fileName"
                    class="w-full h-full object-cover"
                  />
                </div>
                <!-- 选中标记 -->
                <div
                  v-if="selectedMediaUuids.includes(media.mediaUuid)"
                  class="absolute top-2 right-2 w-6 h-6 bg-indigo-600 rounded-full flex items-center justify-center"
                >
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <p class="mt-2 text-xs text-gray-500 truncate">{{ media.fileName }}</p>
              </div>
            </div>
          </div>

          <!-- 对话框底部 -->
          <div class="flex items-center justify-between p-6 border-t border-gray-100">
            <span class="text-sm text-gray-500">
              已选择 {{ selectedMediaUuids.length }} 张图片
            </span>
            <div class="flex items-center gap-3">
              <button
                @click="showAddMediaDialog = false"
                class="px-4 py-2 text-gray-700 font-medium hover:bg-gray-100 rounded-full transition-colors"
              >
                取消
              </button>
              <button
                @click="addSelectedMedias"
                :disabled="selectedMediaUuids.length === 0 || addingMedia"
                :class="[
                  'px-6 py-2 font-medium rounded-full transition-colors',
                  selectedMediaUuids.length === 0 || addingMedia
                    ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                    : 'bg-indigo-600 text-white hover:bg-indigo-700'
                ]"
              >
                <span v-if="addingMedia" class="flex items-center gap-2">
                  <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  添加中...
                </span>
                <span v-else>添加到图集</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 编辑图集对话框 -->
    <Teleport to="body">
      <div
        v-if="showEditDialog"
        class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
        @click="showEditDialog = false"
      >
        <div
          class="bg-white rounded-2xl max-w-lg w-full"
          @click.stop
        >
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between p-6 border-b border-gray-100">
            <h3 class="text-xl font-bold text-gray-900">编辑图集</h3>
            <button
              @click="showEditDialog = false"
              class="p-2 text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 对话框内容 -->
          <div class="p-6 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">图集名称</label>
              <input
                v-model="editingAlbum.name"
                type="text"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
                placeholder="请输入图集名称"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">图集描述</label>
              <textarea
                v-model="editingAlbum.description"
                rows="3"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all resize-none"
                placeholder="请输入图集描述（可选）"
              ></textarea>
            </div>

            <div class="flex items-center gap-3">
              <input
                id="isPublic"
                v-model="editingAlbum.isPublic"
                type="checkbox"
                class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
              />
              <label for="isPublic" class="text-sm text-gray-700">
                公开图集
                <span class="text-gray-400 text-xs ml-1">（所有人都可以访问）</span>
              </label>
            </div>
          </div>

          <!-- 对话框底部 -->
          <div class="flex items-center justify-end gap-3 p-6 border-t border-gray-100">
            <button
              @click="showEditDialog = false"
              class="px-4 py-2 text-gray-700 font-medium hover:bg-gray-100 rounded-full transition-colors"
            >
              取消
            </button>
            <button
              @click="saveAlbumEdit"
              :disabled="savingAlbum"
              :class="[
                'px-6 py-2 font-medium rounded-full transition-colors',
                savingAlbum
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-indigo-600 text-white hover:bg-indigo-700'
              ]"
            >
              <span v-if="savingAlbum" class="flex items-center gap-2">
                <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                保存中...
              </span>
              <span v-else>保存修改</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 图片预览弹窗 -->
    <GalleryPreviewModal
      :visible="previewVisible"
      :item="previewItem"
      @close="closePreview"
      @copy="handlePreviewCopy"
      @open-album="handleOpenAlbum"
    />
  </div>
</template>

<style scoped>
/* 瀑布流动画 */
.group {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式调整 */
@media (max-width: 640px) {
  .album-detail-page {
    padding-bottom: env(safe-area-inset-bottom);
  }
}
</style>
