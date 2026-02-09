<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Heart, Eye, Share2, Download, Copy, ExternalLink, Image } from 'lucide-vue-next'
import { albumApi } from '../../api/album'
import { useSystemStore } from '../../stores/system'

const route = useRoute()
const router = useRouter()
const systemStore = useSystemStore()

const album = ref(null)
const medias = ref([])
const loading = ref(false)
const currentImageIndex = ref(0)
const showLightbox = ref(false)

// 瀑布流列数
const columnCount = ref(4)

// 根据屏幕宽度调整列数
const updateColumnCount = () => {
  const width = window.innerWidth
  if (width < 640) columnCount.value = 2
  else if (width < 1024) columnCount.value = 3
  else if (width < 1280) columnCount.value = 4
  else columnCount.value = 5
}

// 将图片分配到各列（瀑布流算法）
const columns = computed(() => {
  const cols = Array.from({ length: columnCount.value }, () => [])
  const colHeights = Array(columnCount.value).fill(0)

  medias.value.forEach((media, index) => {
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
  if (!domain) return window.location.origin
  return domain.replace(/\/$/, '')
})

// 随机图片URL（用于分享）
const randomImageUrl = computed(() => {
  if (!album.value) return ''
  return `${normalizedDomain.value}/picture/${album.value.pathSlug}`
})

// 图集详情页URL
const albumDetailUrl = computed(() => {
  if (!album.value) return ''
  return `${window.location.origin}/album/${album.value.pathSlug}`
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
      ElMessage.error('该图集为私有，无法访问')
    } else {
      ElMessage.error('获取图集失败')
    }
    router.push('/')
  } finally {
    loading.value = false
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

const copyRandomUrl = () => {
  navigator.clipboard.writeText(randomImageUrl.value)
  ElMessage.success('随机图片链接已复制')
}

const copyDetailUrl = () => {
  navigator.clipboard.writeText(albumDetailUrl.value)
  ElMessage.success('图集页面链接已复制')
}

const goBack = () => {
  router.back()
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
  window.addEventListener('resize', updateColumnCount)
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
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex flex-wrap items-center gap-3 mt-4">
          <a
            :href="randomImageUrl"
            target="_blank"
            class="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-full hover:bg-indigo-700 transition-colors"
          >
            <ExternalLink class="w-4 h-4" />
            获取随机图片
          </a>

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
            class="group relative bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-lg transition-all duration-300 cursor-pointer"
            @click="openLightbox(media.originalIndex)"
          >
            <!-- 图片 -->
            <div class="relative overflow-hidden">
              <img
                :src="media.thumbnailUrl || media.imageLink"
                :alt="media.fileName"
                class="w-full h-auto object-cover transform group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
              />

              <!-- 悬停遮罩 -->
              <div class="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors duration-300"></div>

              <!-- 图片信息 -->
              <div class="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                <p class="text-white text-xs truncate">{{ media.fileName }}</p>
                <p class="text-white/70 text-xs">{{ (media.size / 1024).toFixed(1) }} KB</p>
              </div>
            </div>

            <!-- 底部信息栏 -->
            <div class="p-3 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Heart class="w-4 h-4 text-gray-400" />
                <span class="text-xs text-gray-500">{{ formatNumber(media.likes || 0) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Eye class="w-4 h-4 text-gray-400" />
                <span class="text-xs text-gray-500">{{ formatNumber(media.views || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && medias.length === 0" class="flex flex-col items-center justify-center py-20">
      <div class="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <Image class="w-10 h-10 text-gray-400" />
      </div>
      <p class="text-gray-500 text-lg">图集中暂无图片</p>
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
  </div>
</template>

<style scoped>
.album-detail-page {
  /* 图片瀑布流：简洁、卡片式、圆角 */
}

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
