import { computed, ref } from 'vue'
import { fetchPublicGallery } from '../services/gallery'

export const usePublicGallery = ({ pageSize = 24 } = {}) => {
  const galleryItems = ref([])
  const loading = ref(false)
  const errorMessage = ref('')
  const totalElements = ref(0)
  const page = ref(0)
  const totalPages = ref(1)
  const lastUpdated = ref('')
  const guestLikeEnabled = ref(true)
  const assetBase = (import.meta.env.VITE_PUBLIC_ASSET_BASE || '').replace(/\/$/, '')
  const seenIds = new Set()

  const formattedTotal = computed(() => (totalElements.value ? totalElements.value.toLocaleString('zh-CN') : '--'))

  const decorateItem = (item = {}) => ({
    ...item,
    isLiked: Boolean(item.likedByMe ?? item.isLiked),
    latestLike: item.latestLike || null,
    publicAccessible: item.publicAccessible !== false,
    mediaCategory: item.mediaCategory || 'IMAGE',
    ownerDisplayName: item.ownerDisplayName || '匿名用户',
    tags: Array.isArray(item.tags) ? item.tags : [],
  })

  const mapItems = (items = []) => items.map(decorateItem)

  const resolvePublicUrl = (item) => {
    if (!item) return ''
    const link = item.publicUrl
    if (link && /^https?:\/\//i.test(link)) return link
    if (assetBase && item.objectKey) {
      const normalizedKey = String(item.objectKey).replace(/^\/+/, '')
      return `${assetBase}/${normalizedKey}`
    }
    return link || ''
  }

  const resolvePosterUrl = (item) => {
    if (!item || !item.thumbnailUrl) return ''
    if (/^https?:\/\//i.test(item.thumbnailUrl)) return item.thumbnailUrl
    if (assetBase) {
      const normalizedKey = String(item.thumbnailUrl).replace(/^\/+/, '')
      return `${assetBase}/${normalizedKey}`
    }
    return item.thumbnailUrl
  }

  const formatDate = (value) => {
    if (!value) return '时间未知'
    const date = new Date(value)
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }

  const limitTags = (tags = []) => tags.slice(0, 3)

  async function loadGallery(targetPage = 0, { reset = false } = {}) {
    if (loading.value) return
    if (!reset && targetPage >= totalPages.value && galleryItems.value.length) return
    if (reset) {
      galleryItems.value = []
      totalElements.value = 0
      page.value = 0
      totalPages.value = 1
      seenIds.clear()
    }
    loading.value = true
    errorMessage.value = ''
    try {
      const response = await fetchPublicGallery({ page: Math.max(targetPage, 0), size: pageSize })
      const normalizedItems = mapItems(response.items || [])
      const uniqueItems = normalizedItems.filter((item) => {
        if (!item?.id) return true
        if (seenIds.has(item.id)) return false
        seenIds.add(item.id)
        return true
      })
      galleryItems.value = reset ? uniqueItems : [...galleryItems.value, ...uniqueItems]
      totalElements.value = response.totalElements ?? galleryItems.value.length
      totalPages.value = Math.max(response.totalPages || totalPages.value || 1, 1)
      page.value = response.page ?? targetPage
      guestLikeEnabled.value = response.guestLikeEnabled ?? true
      lastUpdated.value = new Date().toLocaleString('zh-CN', { hour12: false })
    } catch (error) {
      console.error('加载公开图库失败', error)
      errorMessage.value = error?.response?.data?.message || '加载公开图库失败，请稍后重试'
    } finally {
      loading.value = false
    }
  }

  const loadMore = () => {
    if (page.value + 1 < totalPages.value) {
      loadGallery(page.value + 1)
    }
  }

  return {
    galleryItems,
    loading,
    errorMessage,
    totalElements,
    page,
    totalPages,
    lastUpdated,
    formattedTotal,
    resolvePublicUrl,
    resolvePosterUrl,
    formatDate,
    limitTags,
    guestLikeEnabled,
    loadGallery,
    loadMore,
  }
}
