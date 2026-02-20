<template>
  <div class="gallery-page">
    <!-- 动态背景 -->
    <div class="gallery-bg">
      <div class="bg-orb orb-purple"></div>
      <div class="bg-orb orb-pink"></div>
      <div class="bg-orb orb-blue"></div>
      <div class="bg-grid"></div>
    </div>

    <!-- 导航栏 -->
    <UserNavbar />

    <!-- Hero Section -->
    <section class="hero-section" v-lazy-animate="{ fromY: 30, duration: 0.8 }">
      <div class="container">
        <div class="hero-content">
          <div class="hero-tag">
            <span class="tag-pulse"></span>
            <span class="tag-text">已收录 {{ formattedTotalImages }} 张图片</span>
          </div>
          <h1 class="hero-title">
            Search<br>
            <span class="title-highlight">查找图片</span>
          </h1>
          <p class="hero-desc">
            灵感枯竭？来这里逛逛。输入关键词，或者随便点几个标签，
            说不定就有新想法蹦出来。
          </p>
          
          <!-- 搜索框 -->
          <div class="search-box" id="gallery-search">
            <div class="search-input-wrapper">
              <Search class="search-icon" />
              <input
                v-model="keyword"
                type="search"
                maxlength="20"
                placeholder="搜标签、标题、创作者..."
                class="search-input"
                @keyup.enter.exact="submitSearch"
              />
              <button 
                type="button" 
                class="search-btn" 
                :disabled="searchLoading" 
                @click="submitSearch"
              >
                <Loader2 v-if="searchLoading" class="animate-spin" />
                <span v-else>搜一下</span>
              </button>
            </div>
            <p v-if="keywordWarning" class="search-warning">{{ keywordWarning }}</p>
            <p v-else-if="searchSummary" class="search-summary">{{ searchSummary }}</p>
          </div>

          <!-- 快捷操作 -->
          <div class="hero-actions">
            <button type="button" class="btn-primary" :disabled="searchLoading" @click="submitSearch">
              <Sparkles class="btn-icon" />
              {{ searchLoading ? '找图中...' : '开始找图' }}
            </button>
            <RouterLink to="/user" class="btn-ghost">
              <Upload class="btn-icon" />
              我也传一张
            </RouterLink>
          </div>

          <p class="sync-time">刚刚更新 · {{ lastUpdated || '同步中' }}</p>
        </div>

        <!-- Hero 视觉区 - 精选图集卡片 -->
        <div v-if="heroTiles.length > 0" class="hero-visual">
          <div 
            v-for="(tile, index) in heroTiles" 
            :key="tile.id"
            class="hero-tile"
            :class="`tile-${index + 1}`"
            v-lazy-animate="{ fromY: 40, delay: index * 0.15, duration: 0.7 }"
            @click="goToAlbum(tile.pathSlug)"
            role="button"
            tabindex="0"
            @keydown.enter="goToAlbum(tile.pathSlug)"
          >
            <!-- 图集封面图片 -->
            <div class="tile-cover-image" :style="{ background: tile.gradient }">
              <img
                v-if="tile.randomCoverUrl"
                :src="tile.randomCoverUrl"
                :alt="tile.title"
                class="cover-img"
                loading="lazy"
                @error="handleCoverError"
              />
            </div>
            <div class="tile-overlay"></div>
            <div class="tile-content">
              <span class="tile-tag">{{ tile.tag }}</span>
              <h3 class="tile-title">{{ tile.title }}</h3>
              <p class="tile-meta">{{ tile.meta }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 热门标签 -->
    <section class="tags-section" v-lazy-animate="{ fromY: 20, duration: 0.6 }">
      <div class="container">
        <div class="tags-header">
          <span class="tags-label">热门标签</span>
          <span class="tags-hint">点一下，快速筛选</span>
        </div>
        <div class="tags-cloud">
          <button
            v-for="(tag, index) in popularTags"
            :key="tag"
            type="button"
            class="tag-pill"
            :class="{ active: keyword === tag }"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="keyword = tag; submitSearch()"
          >
            {{ tag }}
          </button>
        </div>
      </div>
    </section>

    <!-- 主内容区 -->
    <div class="container">
      <main class="main-layout">
        <!-- 瀑布流内容 -->
        <section ref="galleryFeedRef" class="feed-section" v-lazy-animate="{ fromY: 30, duration: 0.7 }">
          <div class="feed-header asymmetric">
            <div class="feed-header-left">
              <span class="section-eyebrow">最新上传</span>
              <h3 class="feed-title">
                {{ isSearchActive ? `找到 ${searchResults.length} 张` : `共 ${formattedTotal} 张` }}
              </h3>
            </div>
            <div class="filter-chips">
              <button
                v-for="option in filterOptions"
                :key="option.id"
                type="button"
                class="filter-chip"
                :class="{ active: activeFilter === option.id }"
                @click="setFilter(option.id)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- 搜索状态 -->
          <div v-if="isSearchActive" class="search-status">
            <div class="status-info">
              <p class="status-tag">"{{ lastSubmittedKeyword || keyword }}"</p>
              <p class="status-count">
                找到 {{ searchTotalElements || searchResults.length || 0 }} 张图
              </p>
            </div>
            <button type="button" class="btn-clear" @click="clearSearch">
              <X class="btn-icon-sm" />
              清除
            </button>
          </div>

          <!-- 错误提示 -->
          <div v-if="searchError || errorMessage" class="alert-error">
            <AlertCircle class="alert-icon" />
            {{ searchError || errorMessage }}
          </div>

          <!-- 骨架屏 -->
          <div v-if="showSkeleton" class="masonry-skeleton">
            <div v-for="i in 12" :key="`sk-${i}`" class="skeleton-item" :style="{ height: `${200 + Math.random() * 150}px` }">
              <div class="skeleton-shimmer"></div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="isEmpty" class="empty-state">
            <div class="empty-icon">
              <ImageOff class="icon-lg" />
            </div>
            <h3 class="empty-title">暂无内容</h3>
            <p class="empty-desc">
              {{ isSearchActive ? '检索到的作品暂无可展示内容，可尝试清除搜索或调整筛选条件。' : '暂无公开图片，或过滤条件过于严格。' }}
            </p>
            <button v-if="isSearchActive" type="button" class="btn-primary" @click="clearSearch">
              清除搜索
            </button>
          </div>

          <!-- 图片网格 -->
          <div v-else class="masonry-grid">
            <article 
              v-for="(item, index) in displayedItems" 
              :key="item.id" 
              class="gallery-card"
              :style="getCardStyle(item)"
              v-lazy-animate="{ fromY: 30, delay: (index % 8) * 0.05, duration: 0.6 }"
            >
              <div class="card-media" @click="openModal(item)">
                <template v-if="item.mediaCategory === 'VIDEO'">
                  <video 
                    class="media-video" 
                    :poster="resolvePosterUrl(item) || undefined" 
                    preload="metadata" 
                    playsinline 
                    muted
                  >
                    <source :src="resolvePublicUrl(item)" />
                  </video>
                  <span class="video-badge">
                    <Play class="badge-icon" />
                    VIDEO
                  </span>
                </template>
                <template v-else>
                  <img 
                    :src="resolvePublicUrl(item)" 
                    :alt="item.fileName" 
                    class="media-image" 
                    loading="lazy"
                    @error="handleImageError"
                  />
                </template>
                
                <!-- 悬浮操作 -->
                <div class="media-overlay">
                  <button type="button" class="overlay-btn" @click.stop="copyLink(resolvePublicUrl(item))">
                    <Copy class="btn-icon-sm" />
                  </button>
                </div>
              </div>

              <div class="card-footer">
                <button type="button" class="author-btn" @click.stop="goToUserProfile(item.ownerId)">
                  <img v-if="item.ownerAvatarUrl" :src="item.ownerAvatarUrl" alt="avatar" loading="lazy" />
                  <span v-else class="author-initial">{{ (item.ownerDisplayName || 'U').slice(0, 1).toUpperCase() }}</span>
                  <span class="author-name">{{ item.ownerDisplayName || '匿名' }}</span>
                </button>
                <div class="card-stats">
                  <button 
                    type="button" 
                    class="stat-btn"
                    :class="{ liked: item.isLiked }"
                    @click.stop="handleLike(item, $event)"
                  >
                    <Heart class="stat-icon" :class="{ 'fill-current': item.isLiked }" />
                    <span>{{ formatNumber(item.likeCount || 0) }}</span>
                  </button>
                  <span class="stat-item">
                    <Eye class="stat-icon" />
                    <span>{{ formatNumber(item.invokeCount || 0) }}</span>
                  </span>
                </div>
              </div>
            </article>
          </div>

          <!-- 加载更多 -->
          <div class="load-more">
            <button
              type="button"
              class="btn-load"
              :disabled="!hasMore || isLoadingMore"
              @click="handleLoadMore"
            >
              <Loader2 v-if="isLoadingMore" class="animate-spin" />
              <Filter v-else class="btn-icon-sm" />
              <span>{{ loadMoreText }}</span>
            </button>
            <button v-if="isSearchActive" type="button" class="btn-back" @click="clearSearch">
              <LayoutGrid class="btn-icon-sm" />
              退出搜索，回到推荐
            </button>
          </div>
        </section>

        <!-- 侧边栏 -->
        <aside class="sidebar">
          <!-- 创作者榜 -->
          <section class="sidebar-card spotlight" v-lazy-animate="{ fromY: 25, delay: 0.1 }">
            <div class="card-header">
              <span class="card-eyebrow">创作者榜</span>
              <h3 class="card-title">人气达人</h3>
            </div>
            <div v-if="creatorSpotlight.length" class="creator-list">
              <div 
                v-for="creator in creatorSpotlight" 
                :key="creator.id"
                class="creator-item"
                @click="goToUserProfile(creator.id)"
              >
                <span class="creator-rank" :class="`rank-${creator.rank}`">{{ creator.rank }}</span>
                <div class="creator-avatar">
                  <img v-if="creator.avatar" :src="creator.avatar" :alt="creator.name" loading="lazy" />
                  <span v-else>{{ creator.initial }}</span>
                </div>
                <div class="creator-info">
                  <p class="creator-name">{{ creator.name }}</p>
                  <p class="creator-handle">{{ creator.handle }}</p>
                </div>
                <span class="creator-stat">{{ creator.stat }}</span>
              </div>
            </div>
            <div v-else class="creator-empty">
              <Users class="empty-icon-sm" />
              <p>暂无公开创作者</p>
            </div>
          </section>

          <!-- 热搜标签 -->
          <section class="sidebar-card" v-lazy-animate="{ fromY: 25, delay: 0.2 }">
            <div class="card-header">
              <span class="card-eyebrow">热搜标签</span>
              <h3 class="card-title">大家都在搜</h3>
            </div>
            <div class="hashtag-cloud">
              <button
                v-for="tag in trendingHashtags"
                :key="tag"
                type="button"
                class="hashtag-btn"
                @click="selectHashtag(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </section>

          <!-- 发布 CTA -->
          <section class="sidebar-card cta-card" v-lazy-animate="{ fromY: 25, delay: 0.3 }">
            <div class="cta-content">
              <span class="card-eyebrow">创作空间</span>
              <h3 class="cta-title">立即上传你的灵感作品</h3>
              <p class="cta-desc">开启专属的 AstrNest 相册，为作品打造更专业的展示舞台。</p>
            </div>
            <button type="button" class="btn-primary w-full" @click="router.push('/user')">
              <Upload class="btn-icon" />
              立即发布
            </button>
          </section>
        </aside>
      </main>
    </div>

    <!-- 预览模态框 -->
    <GalleryPreviewModal
      :visible="Boolean(activeItem)"
      :item="activeItem"
      :disable-like="shouldDisableLikeButton(activeItem)"
      :like-active="Boolean(activeItem?.isLiked)"
      :like-label="activeItem?.isLiked ? '取消点赞' : '点赞'"
      :guest-like-notice="guestLikeNotice"
      :latest-liker-label="latestLikerLabel"
      :show-latest-liker-button="false"
      :resolve-public-url="resolvePublicUrl"
      :resolve-poster-url="resolvePosterUrl"
      :format-date="formatDateTime"
      :format-bytes="formatBytes"
      @close="closeModal"
      @copy="handleModalCopy"
      @like="handleModalLike"
      @open-uploader="handleModalUploader"
      @open-album="handleModalAlbum"
    />

    <ChenxiGlobalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Copy, Eye, Filter, Heart, LayoutGrid, Loader2, Search, 
  Sparkles, Upload, X, ArrowRight, AlertCircle, ImageOff, 
  Play, Users 
} from 'lucide-vue-next'
import ChenxiGlobalFooter from '../../components/common/ChenxiGlobalFooter.vue'
import UserNavbar from '../../components/common/UserNavbar.vue'
import GalleryPreviewModal from '../../components/public/GalleryPreviewModal.vue'
import { usePublicGallery } from '../../composables/usePublicGallery'
import { likeImage, unlikeImage, searchGalleryByTag } from '../../services/gallery'
import { useAuthStore } from '../../stores/auth'
import { albumApi } from '../../api/album'
import '../../assets/styles/chenxi-interactions.css'

const router = useRouter()
const auth = useAuthStore()

const MAX_TAG_QUERY_LENGTH = 20
const DANGEROUS_INPUT_PATTERN = /[<>"'`;]|(script|javascript:|onerror|onload)/i
const searchPageSize = 24

const keyword = ref('')
const keywordWarning = ref('')
const activeFilter = ref('all')

const searchResults = ref([])
const searchLoading = ref(false)
const searchError = ref('')
const searchPage = ref(0)
const searchTotalPages = ref(0)
const searchTotalElements = ref(0)
const lastSubmittedKeyword = ref('')
const searchSeenIds = new Set()
const galleryFeedRef = ref(null)
const totalImages = ref(0)

// Featured 图集数据
const featuredAlbums = ref([])
const featuredAlbumsLoading = ref(false)

const filterOptions = [
  { id: 'all', label: '综合' },
  { id: 'image', label: '图片' },
  { id: 'video', label: '视频' },
  { id: 'popular', label: '按热度' },
]

// 热门标签
const popularTags = [
  '风景', '美食', '旅行', '摄影', '设计', 
  '生活', '萌宠', '插画', '建筑', '人像',
  '夜景', '自然', '城市', '艺术', '科技'
]

// 渐变占位图生成
const gradientPlaceholders = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
]

const getPlaceholderGradient = (id) => {
  const index = id ? id.toString().split('').reduce((a, b) => a + parseInt(b), 0) % gradientPlaceholders.length : 0
  return gradientPlaceholders[index]
}

const defaultHeroTiles = [
  {
    id: 'fallback-hero-1',
    title: '旅途纪事 · 山海之间',
    tag: '图集',
    meta: '辰汐内容团队 · 1.2k 喜欢',
    cover: null,
    gradient: gradientPlaceholders[0],
  },
  {
    id: 'fallback-hero-2',
    title: '夜色霓虹慢闪',
    tag: '视频集',
    meta: '城市漫游者 · 980 喜欢',
    cover: null,
    gradient: gradientPlaceholders[1],
  },
  {
    id: 'fallback-hero-3',
    title: '日常器物的温度',
    tag: '生活方式',
    meta: '慢生活档案 · 860 喜欢',
    cover: null,
    gradient: gradientPlaceholders[2],
  },
]

const fallbackStories = [
  {
    id: 'story-1',
    title: '夜色霓虹合集',
    author: '辰汐官方',
    badge: '人气精选',
    likes: '1.2k',
    cover: null,
    gradient: gradientPlaceholders[3],
  },
  {
    id: 'story-2',
    title: '胶片里的晨光',
    author: '胶片造梦局',
    badge: '胶片',
    likes: '986',
    cover: null,
    gradient: gradientPlaceholders[4],
  },
  {
    id: 'story-3',
    title: '慢生活家务记',
    author: '生活方式研究所',
    badge: '生活方式',
    likes: '742',
    cover: null,
    gradient: gradientPlaceholders[5],
  },
  {
    id: 'story-4',
    title: '城市建筑之美',
    author: '建筑摄影师',
    badge: '建筑',
    likes: '628',
    cover: null,
    gradient: gradientPlaceholders[6],
  },
]

const hasDangerousChars = (value = '') => DANGEROUS_INPUT_PATTERN.test(value)
const sanitizeKeyword = (value = '') => value.trim().replace(/\s+/g, ' ')
const buildKeywordWarning = (value = '') => {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (trimmed.length > MAX_TAG_QUERY_LENGTH) {
    return `标签长度需在 ${MAX_TAG_QUERY_LENGTH} 个字符以内`
  }
  if (hasDangerousChars(trimmed)) {
    return '检测到危险字符，已阻止搜索'
  }
  return ''
}
const reportSuspiciousInput = (value) => {
  console.warn('[AstrNest] Suspicious search input blocked:', value)
}

const {
  galleryItems,
  loading,
  errorMessage,
  page,
  totalPages,
  lastUpdated,
  formattedTotal,
  resolvePublicUrl,
  resolvePosterUrl,
  guestLikeEnabled,
  loadGallery,
  loadMore,
} = usePublicGallery({ pageSize: 24 })

const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatNumber = (num) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

watch(
  keyword,
  (value) => {
    keywordWarning.value = buildKeywordWarning(value || '')
    if (!value?.trim()) {
      searchError.value = ''
    }
  },
  { immediate: true }
)

const isSearchActive = computed(() => Boolean(lastSubmittedKeyword.value) || searchLoading.value)
const activeSourceItems = computed(() => (isSearchActive.value ? searchResults.value : galleryItems.value))

// 格式化数字显示
const formattedTotalImages = computed(() => {
  const num = totalImages.value
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
})

const displayedItems = computed(() => {
  let items = [...activeSourceItems.value]
  if (activeFilter.value === 'image') {
    items = items.filter((item) => item.mediaCategory !== 'VIDEO')
  } else if (activeFilter.value === 'video') {
    items = items.filter((item) => item.mediaCategory === 'VIDEO')
  } else if (activeFilter.value === 'popular') {
    items = [...items].sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
  }
  const term = keyword.value.trim().toLowerCase()
  const shouldApplyLocalFilter = Boolean(term) && !isSearchActive.value
  if (shouldApplyLocalFilter) {
    items = items.filter((item) => {
      const title = (item.fileName || '').toLowerCase()
      const author = (item.ownerDisplayName || '').toLowerCase()
      return title.includes(term) || author.includes(term)
    })
  }
  return items
})

const searchVisibleCount = computed(() => (isSearchActive.value ? displayedItems.value.length : 0))
const isSearchResultEmpty = computed(
  () => Boolean(lastSubmittedKeyword.value) && isSearchActive.value && !searchLoading.value && searchVisibleCount.value === 0
)
const isEmpty = computed(() => !displayedItems.value.length && !showSkeleton.value)
const showSkeleton = computed(() => 
  (loading.value && !galleryItems.value.length) || 
  (isSearchActive.value && searchLoading.value && !searchResults.value.length)
)

const totalViews = computed(() => galleryItems.value.reduce((sum, item) => sum + (item.invokeCount || 0), 0))
const totalLikes = computed(() => galleryItems.value.reduce((sum, item) => sum + (item.likeCount || 0), 0))

const searchSummary = computed(() => {
  if (!lastSubmittedKeyword.value) return ''
  if (searchLoading.value) return '正在检索匹配的公开及个人作品…'
  const backendTotal = searchTotalElements.value || searchResults.value.length
  if (searchVisibleCount.value > 0) {
    return `已展示 ${searchVisibleCount.value} 条与 "${lastSubmittedKeyword.value}" 相关的作品`
  }
  if (backendTotal > 0) return `检索到 ${backendTotal} 条相关作品，但当前筛选未找到可展示内容`
  return `暂无与 "${lastSubmittedKeyword.value}" 相关的作品`
})

const heroStats = computed(() => [
  { label: '公开作品', value: formattedTotal.value || '—' },
  { label: '累计浏览', value: numberFormatter.format(totalViews.value) },
  { label: '点赞互动', value: numberFormatter.format(totalLikes.value) },
])

// 获取图集随机封面图片 URL
const getAlbumRandomCoverUrl = (pathSlug) => {
  if (!pathSlug) return ''
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'https://luminouschenxi.net'
  // 添加时间戳参数防止缓存
  const timestamp = Date.now()
  return `${baseUrl}/api/albums/random/${pathSlug}?t=${timestamp}`
}

const heroTiles = computed(() => {
  // 优先使用 featuredAlbums（真实图集数据）
  if (featuredAlbums.value.length > 0) {
    return featuredAlbums.value.map((album, index) => {
      return {
        id: album.albumUuid ?? `album-${index}`,
        title: album.name || `图集 #${index + 1}`,
        tag: '图集',
        meta: `${album.username || '匿名创作者'} · ${numberFormatter.format(album.totalLikes || 0)} 喜欢`,
        randomCoverUrl: getAlbumRandomCoverUrl(album.pathSlug),
        gradient: getPlaceholderGradient(album.albumUuid || album.id || index),
        pathSlug: album.pathSlug, // 用于点击跳转
        isAlbum: true, // 标记这是图集
      }
    })
  }
  // 如果没有图集数据，返回空数组（不显示卡片）
  return []
})

const featuredStories = computed(() => {
  const mapped = galleryItems.value.slice(0, 6).map((item, index) => ({
    id: item.id ?? `featured-${index}`,
    title: item.fileName || `精选作品 #${index + 1}`,
    author: item.ownerDisplayName || '匿名创作者',
    badge: item.mediaCategory === 'VIDEO' ? '动态' : '图集',
    likes: numberFormatter.format(item.likeCount || 0),
    cover: resolvePublicUrl(item),
    gradient: getPlaceholderGradient(item.id),
  }))
  return mapped.length ? mapped : fallbackStories
})

const creatorSpotlight = computed(() => {
  if (!galleryItems.value.length) return []
  const map = new Map()
  galleryItems.value.forEach((item) => {
    const ownerId = item.ownerId
    if (!ownerId) return
    const name = item.ownerDisplayName || '匿名创作者'
    const avatar = item.ownerAvatarUrl || ''
    const likeCount = item.likeCount || 0
    const viewCount = item.invokeCount || 0
    const existing = map.get(ownerId) || {
      id: ownerId,
      name,
      handle: `@${name.slice(0, 12)}`,
      avatar,
      likeCount: 0,
      viewCount: 0,
    }
    existing.likeCount += likeCount
    existing.viewCount += viewCount
    if (!existing.avatar && avatar) existing.avatar = avatar
    if (!existing.name && name) {
      existing.name = name
      existing.handle = `@${name.slice(0, 12)}`
    }
    map.set(ownerId, existing)
  })

  return Array.from(map.values())
    .sort((a, b) => (b.likeCount - a.likeCount) || (b.viewCount - a.viewCount))
    .slice(0, 3)
    .map((creator, index) => ({
      ...creator,
      rank: index + 1,
      initial: (creator.name || 'U').slice(0, 1).toUpperCase(),
      stat: `${numberFormatter.format(creator.likeCount)} 喜欢`,
    }))
})

const trendingHashtags = computed(() => {
  const tags = new Set()
  galleryItems.value.forEach((item) => {
    const candidates = (item.fileName || '').split(/[\s·、#]+/).filter((word) => word && word.length <= 8)
    candidates.slice(0, 2).forEach((word) => tags.add(`#${word}`))
  })
  if (!tags.size) {
    return ['#星云摄影', '#AI旅拍', '#胶片感', '#夜色霓虹', '#极简设计', '#城市浪潮']
  }
  return Array.from(tags).slice(0, 8)
})

const guestLikeNotice = computed(() =>
  !auth.isAuthenticated && !guestLikeEnabled.value
    ? '管理员暂时关闭访客点赞，登录即可继续互动'
    : ''
)

const latestLikerLabel = computed(() => {
  if (!activeItem.value) return '暂无记录'
  const latest = activeItem.value.latestLike
  if (latest) {
    if (latest.guest) return '访客'
    return latest.displayName || '站内用户'
  }
  if (auth.isAuthenticated && activeItem.value.isLiked) {
    return auth.profile?.username || '您'
  }
  return '暂无记录'
})

const hasMore = computed(() => (isSearchActive.value ? searchHasMore.value : page.value + 1 < totalPages.value))
const isLoadingMore = computed(() =>
  isSearchActive.value ? searchLoading.value && searchResults.value.length > 0 : loading.value && galleryItems.value.length > 0
)

const loadMoreText = computed(() => {
  if (!hasMore.value) return isSearchActive.value ? '已展示全部匹配结果' : '已展示全部图片'
  if (isLoadingMore.value) return isSearchActive.value ? '检索中' : '加载中'
  return isSearchActive.value ? '加载更多匹配项' : '加载更多'
})

const setFilter = (filterId) => {
  activeFilter.value = filterId
}

const activateChannel = (channelId) => {
  activeChannel.value = channelId
  const target = channelTabs.find((channel) => channel.id === channelId)
  if (target?.filter) {
    setFilter(target.filter)
  }
}

const selectHashtag = (tag) => {
  keyword.value = tag.replace('#', '')
  submitSearch()
}

const getFeaturedStyle = (story) => {
  if (story.cover) {
    return {
      backgroundImage: `linear-gradient(160deg, rgba(11, 13, 23, 0.2), rgba(11, 13, 23, 0.75)), url(${story.cover})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  return {
    background: story.gradient,
  }
}

const getCardStyle = (item) => {
  const width = Number(item?.width || item?.imageWidth)
  const height = Number(item?.height || item?.imageHeight)
  if (width > 0 && height > 0) {
    // 移动端使用更小的跨度以适应屏幕
    const isMobile = window.innerWidth <= 768
    const spanBase = isMobile ? 8 : 10
    return {
      gridRow: `span ${Math.ceil((height / width) * spanBase)}`,
    }
  }
  // 默认高度：如果没有宽高信息，使用固定高度
  const isMobile = window.innerWidth <= 768
  return {
    gridRow: isMobile ? 'span 16' : 'span 20',
  }
}

const handleImageError = (e) => {
  const target = e.target
  target.style.background = 'linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%)'
  target.style.minHeight = '200px'
  target.style.display = 'flex'
  target.style.alignItems = 'center'
  target.style.justifyContent = 'center'
  // 显示错误图标
  target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="%23999" stroke-width="2"%3E%3Crect x="3" y="3" width="18" height="18" rx="2" ry="2"/%3E%3Ccircle cx="8.5" cy="8.5" r="1.5"/%3E%3Cpath d="M21 15l-5-5L5 21"/%3E%3C/svg%3E'
  target.style.objectFit = 'scale-down'
}

// 处理封面图片加载错误
const handleCoverError = (e) => {
  const target = e.target
  // 加载失败时显示默认的渐变背景图
  target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect width="400" height="300" fill="url(%23grad)"/%3E%3Cdefs%3E%3ClinearGradient id="grad" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" style="stop-color:%23667eea;stop-opacity:1" /%3E%3Cstop offset="100%25" style="stop-color:%23764ba2;stop-opacity:1" /%3E%3C/linearGradient%3E%3C/defs%3E%3C/svg%3E'
}

const resolveAspectRatio = (item) => {
  const width = Number(item?.width || item?.imageWidth || item?.meta?.width)
  const height = Number(item?.height || item?.imageHeight || item?.meta?.height)
  if (width > 0 && height > 0) {
    return `${width} / ${height}`
  }
  return '1 / 1'
}

const resetSearchState = () => {
  searchResults.value = []
  searchPage.value = 0
  searchTotalPages.value = 0
  searchTotalElements.value = 0
  searchSeenIds.clear()
}

const normalizeSearchItem = (item = {}) => {
  const mediaRaw = (item.mediaCategory || item.fileType || '').toString().toUpperCase()
  const normalizedMedia = mediaRaw === 'VIDEO' ? 'VIDEO' : 'IMAGE'
  return {
    ...item,
    mediaCategory: normalizedMedia,
    ownerDisplayName: item.ownerDisplayName || item.userDisplayName || '匿名创作者',
    ownerAvatarUrl: item.ownerAvatarUrl || item.userAvatarUrl || '',
    ownerId: item.ownerId || item.userId,
    likeCount: item.likeCount ?? 0,
    invokeCount: item.invokeCount ?? 0,
    isLiked: typeof item.isLiked === 'boolean' ? item.isLiked : Boolean(item.likedByMe),
    tags: Array.isArray(item.tags) ? item.tags : [],
    publicAccessible: item.publicAccessible !== false,
    latestLike: item.latestLike || null,
  }
}

const performGallerySearch = async (targetPage = 0, { reset = false } = {}) => {
  const sanitized = sanitizeKeyword(keyword.value)
  if (!sanitized) {
    searchError.value = '请输入标签关键词后再搜索'
    ElMessage.info('请输入标签关键词后再搜索')
    return
  }
  if (sanitized.length > MAX_TAG_QUERY_LENGTH) {
    const message = `标签长度需在 ${MAX_TAG_QUERY_LENGTH} 个字符以内`
    searchError.value = message
    ElMessage.warning(message)
    return
  }
  if (hasDangerousChars(sanitized)) {
    const message = '检测到危险字符，已阻止搜索'
    searchError.value = message
    reportSuspiciousInput(sanitized)
    ElMessage.error(message)
    return
  }
  if (searchLoading.value) return
  if (reset) resetSearchState()
  
  searchLoading.value = true
  searchError.value = ''
  try {
    const response = await searchGalleryByTag({
      keyword: sanitized,
      page: Math.max(targetPage, 0),
      size: searchPageSize,
    })
    const normalizedItems = (response.items || []).map(normalizeSearchItem)
    const uniqueItems = normalizedItems.filter((item) => {
      if (!item?.id) return false
      if (searchSeenIds.has(item.id)) return false
      searchSeenIds.add(item.id)
      return true
    })
    searchResults.value = reset ? uniqueItems : [...searchResults.value, ...uniqueItems]
    searchPage.value = response.page ?? targetPage
    searchTotalPages.value = Math.max(response.totalPages ?? searchTotalPages.value ?? 0, 1)
    searchTotalElements.value = response.totalElements ?? searchResults.value.length
    lastSubmittedKeyword.value = sanitized
    if (typeof response.guestLikeEnabled === 'boolean') {
      guestLikeEnabled.value = response.guestLikeEnabled
    }
  } catch (error) {
    console.error('搜索图库失败', error)
    searchError.value = error?.response?.data?.message || '搜索失败，请稍后重试'
  } finally {
    searchLoading.value = false
  }
}

const submitSearch = () => {
  performGallerySearch(0, { reset: true })
}

const searchHasMore = computed(() => searchPage.value + 1 < searchTotalPages.value)

const loadMoreSearch = () => {
  if (searchLoading.value) return
  if (!searchHasMore.value) return
  performGallerySearch(searchPage.value + 1)
}

const clearSearch = () => {
  keyword.value = ''
  lastSubmittedKeyword.value = ''
  keywordWarning.value = ''
  searchError.value = ''
  resetSearchState()
}

const scrollToFeedSection = () => {
  if (galleryFeedRef.value?.scrollIntoView) {
    galleryFeedRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const handleExploreAll = () => {
  clearSearch()
  scrollToFeedSection()
}

const handleLoadMore = () => {
  if (isSearchActive.value) {
    loadMoreSearch()
  } else {
    loadMore()
  }
}

const activeItem = ref(null)

const handleModalCopy = (url) => {
  copyLink(url)
}

const handleModalLike = ({ event, item }) => {
  if (!item) return
  handleLike(item, event)
}

const handleModalUploader = (item) => {
  if (!item) return
  goToUserProfile(item.ownerId)
}

const handleModalAlbum = (album) => {
  if (!album?.pathSlug) return
  // 关闭弹窗并跳转到图集详情页
  closeModal()
  router.push(`/album/${album.pathSlug}`)
}

const openModal = (item) => {
  activeItem.value = item
}

const closeModal = () => {
  activeItem.value = null
}

const formatDateTime = (value) => {
  if (!value) return '时间未知'
  const date = new Date(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let sizeValue = bytes
  let unitIndex = 0
  while (sizeValue >= 1024 && unitIndex < units.length - 1) {
    sizeValue /= 1024
    unitIndex += 1
  }
  return `${sizeValue.toFixed(sizeValue >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

const applyLikeResult = (target, payload) => {
  if (!target || !payload) return
  if (typeof payload.likeCount === 'number') {
    target.likeCount = payload.likeCount
  }
  if (typeof payload.liked === 'boolean') {
    target.isLiked = payload.liked
  }
  target.latestLike = payload.latestLike || null
}

const ensureLikeAllowed = () => {
  if (!auth.isAuthenticated && !guestLikeEnabled.value) {
    ElMessage.warning('管理员已关闭访客点赞，请登录后操作')
    return false
  }
  return true
}

const shouldDisableLikeButton = (item) => {
  if (!item) return true
  if (auth.isAuthenticated) return false
  return !guestLikeEnabled.value && !item.isLiked
}

async function handleLike(item, event) {
  if (!item || !ensureLikeAllowed()) return
  const isLiked = item.isLiked
  try {
    if (event?.target) {
      const button = event.target.closest('.stat-btn')
      if (button) {
        button.classList.add('like-animation')
        setTimeout(() => button.classList.remove('like-animation'), 300)
      }
    }

    if (isLiked) {
      const data = await unlikeImage(item.id)
      applyLikeResult(item, data)
      ElMessage.success('已取消点赞')
    } else {
      const data = await likeImage(item.id)
      applyLikeResult(item, data)
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞操作失败', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

function goToUserProfile(userId) {
  if (!userId) {
    ElMessage.info('该用户暂无公开主页')
    return
  }
  closeModal()
  router.push({ name: 'public-user-profile', params: { userId } })
}

// 跳转到图集详情页
function goToAlbum(pathSlug) {
  if (!pathSlug) {
    ElMessage.warning('图集链接无效')
    return
  }
  router.push({ name: 'album-detail', params: { pathSlug } })
}

// 加载 Featured 图集
const loadFeaturedAlbums = async () => {
  featuredAlbumsLoading.value = true
  try {
    const response = await albumApi.getFeaturedAlbums()
    featuredAlbums.value = response.data || []
  } catch (error) {
    console.error('加载Featured图集失败', error)
    featuredAlbums.value = []
  } finally {
    featuredAlbumsLoading.value = false
  }
}

const copyLink = async (link) => {
  if (!link) {
    ElMessage.warning('暂无可复制的链接')
    return
  }
  try {
    if (navigator?.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(link)
    } else {
      throw new Error('CLIPBOARD_NOT_SUPPORTED')
    }
    ElMessage.success('链接已复制')
  } catch (error) {
    console.error('复制失败', error)
    ElMessage.error('复制失败，请稍后重试')
  }
}

onMounted(() => {
  loadGallery(0, { reset: true })
  loadFeaturedAlbums()
})
</script>

<style scoped>
/* 页面背景 */
.gallery-page {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  position: relative;
}

.gallery-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
  animation: orb-float 25s ease-in-out infinite;
}

.orb-purple {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, rgba(127, 123, 255, 0.5), rgba(127, 123, 255, 0.1));
  top: -200px;
  left: -100px;
  animation-delay: 0s;
}

.orb-pink {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, rgba(255, 95, 143, 0.4), rgba(255, 95, 143, 0.08));
  top: 30%;
  right: -150px;
  animation-delay: -8s;
}

.orb-blue {
  width: 450px;
  height: 450px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.35), rgba(59, 130, 246, 0.05));
  bottom: 20%;
  left: 10%;
  animation-delay: -16s;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(127, 123, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(127, 123, 255, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  opacity: 0.5;
}

@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.05); }
  66% { transform: translate(-30px, 20px) scale(0.98); }
}

/* 容器 */
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
  position: relative;
  z-index: 1;
}

/* Hero Section */
.hero-section {
  padding: 120px 0 60px;
}

.hero-content {
  max-width: 600px;
}

/* 新标签样式 */
.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(249, 168, 200, 0.12);
  border: 1px solid rgba(249, 168, 200, 0.25);
  border-radius: 999px;
  margin-bottom: 1.5rem;
  width: fit-content;
}

.tag-pulse {
  width: 8px;
  height: 8px;
  background: #F9A8C8;
  border-radius: 50%;
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
}

.tag-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: #E87A9F;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.1;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 1rem;
}

.title-highlight {
  color: #F9A8C8;
}

.hero-desc {
  font-size: 1.1rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-bottom: 2rem;
  max-width: 480px;
}

/* 搜索框 */
.search-box {
  margin-bottom: 1.5rem;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 1.25rem;
  width: 20px;
  height: 20px;
  color: var(--text-soft);
  z-index: 2;
}

.search-input {
  width: 100%;
  padding: 1rem 7rem 1rem 3.5rem;
  border-radius: 16px;
  border: 2px solid var(--border-soft);
  background: var(--glass-bg);
  backdrop-filter: blur(10px);
  font-size: 1rem;
  color: var(--color-text-primary);
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-brand-primary);
  box-shadow: 0 0 0 4px rgba(127, 123, 255, 0.1);
}

.search-input::placeholder {
  color: var(--text-soft);
}

.search-btn {
  position: absolute;
  right: 0.5rem;
  padding: 0.6rem 1.25rem;
  border-radius: 12px;
  border: none;
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-btn:hover:not(:disabled) {
  background: var(--color-brand-primary);
  color: white;
}

.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.search-warning {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #ef4444;
}

.search-summary {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

/* 按钮 */
.hero-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1.5rem;
  border-radius: 12px;
  background: #F9A8C8;
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(249, 168, 200, 0.35);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  background: #EC8DAD;
  box-shadow: 0 8px 30px rgba(249, 168, 200, 0.45);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 深色主题 */
.dark .btn-primary {
  background: #E87A9F;
  box-shadow: 0 4px 20px rgba(232, 122, 159, 0.35);
}

.dark .btn-primary:hover:not(:disabled) {
  background: #EC8DAD;
  box-shadow: 0 8px 30px rgba(232, 122, 159, 0.45);
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1.5rem;
  border-radius: 12px;
  background: transparent;
  border: 1.5px solid rgba(249, 168, 200, 0.4);
  color: #E87A9F;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

.btn-ghost:hover {
  border-color: #F9A8C8;
  color: #F9A8C8;
  background: rgba(249, 168, 200, 0.08);
}

/* 深色主题 */
.dark .btn-ghost {
  border-color: rgba(232, 122, 159, 0.4);
  color: #F9A8C8;
}

.dark .btn-ghost:hover {
  border-color: #F9A8C8;
  color: #F9A8C8;
  background: rgba(232, 122, 159, 0.1);
}

.btn-text {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-text:hover {
  color: var(--color-brand-primary);
  background: rgba(127, 123, 255, 0.05);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.btn-icon-sm {
  width: 16px;
  height: 16px;
}

/* 统计 */
.hero-stats {
  display: flex;
  gap: 2rem;
  margin-bottom: 1rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.sync-time {
  font-size: 0.8rem;
  color: var(--text-soft);
}

/* Hero Visual */
.hero-visual {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 45%;
  height: 500px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 1rem;
}

.hero-tile {
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-tile:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
}

.tile-1 {
  grid-row: span 2;
}

/* 图集封面图片容器 */
.tile-cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.5s ease;
}

.hero-tile:hover .cover-img {
  transform: scale(1.05);
}

.tile-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 30%, rgba(0, 0, 0, 0.7) 100%);
  z-index: 1;
}

.tile-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1.5rem;
  color: white;
  z-index: 2;
}

.tile-tag {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  opacity: 0.8;
  margin-bottom: 0.5rem;
  display: block;
}

.tile-title {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
  line-height: 1.3;
}

.tile-meta {
  font-size: 0.8rem;
  opacity: 0.7;
}

/* 热门标签区域 */
.tags-section {
  padding: 2rem 0;
}

.tags-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.tags-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.tags-hint {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.tag-pill {
  padding: 0.6rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(249, 168, 200, 0.3);
  background: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: tag-pop 0.4s ease-out backwards;
}

@keyframes tag-pop {
  0% { opacity: 0; transform: scale(0.8) translateY(10px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

.tag-pill:hover {
  background: rgba(249, 168, 200, 0.15);
  border-color: rgba(249, 168, 200, 0.5);
  transform: translateY(-2px);
}

.tag-pill.active {
  background: #F9A8C8;
  border-color: #F9A8C8;
  color: white;
}

/* 精选合集 */
.featured-section {
  padding: 4rem 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 1.5rem;
}

.section-eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin-bottom: 0.5rem;
  display: block;
}

.section-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.featured-card {
  aspect-ratio: 4/5;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.featured-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
}

.featured-card:nth-child(1) {
  grid-column: span 2;
  grid-row: span 2;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.8) 100%);
}

.card-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1.5rem;
  color: white;
}

.card-badge {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  opacity: 0.8;
  margin-bottom: 0.5rem;
  display: block;
}

.card-title {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.card-author {
  font-size: 0.85rem;
  opacity: 0.8;
  margin-bottom: 0.75rem;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
}

.stat-icon {
  width: 14px;
  height: 14px;
}

/* 主布局 */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 2rem;
  padding: 3rem 0;
}

/* Feed Section */
.feed-section {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 24px;
  padding: 1.5rem;
  backdrop-filter: blur(10px);
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-soft);
}

.feed-header.asymmetric {
  align-items: center;
}

.feed-header-left {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.feed-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.search-indicator {
  color: var(--color-text-secondary);
  font-weight: 400;
}

.filter-chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 0.5rem 1rem;
  border-radius: 999px;
  border: 1px solid var(--chip-border);
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

.filter-chip.active {
  border-color: var(--color-brand-primary);
  background: rgba(127, 123, 255, 0.12);
  color: var(--color-brand-primary);
}

/* 搜索状态 */
.search-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background: rgba(127, 123, 255, 0.05);
  border: 1px dashed rgba(127, 123, 255, 0.3);
  border-radius: 12px;
  margin-bottom: 1rem;
}

.status-tag {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-brand-primary);
}

.status-count {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
}

.btn-clear {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: transparent;
  color: #ef4444;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-clear:hover {
  background: rgba(239, 68, 68, 0.1);
}

/* 错误提示 */
.alert-error {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #ef4444;
  margin-bottom: 1rem;
}

.alert-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 骨架屏 */
.masonry-skeleton {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.skeleton-item {
  border-radius: 16px;
  background: linear-gradient(90deg, var(--chip-bg) 25%, var(--border-soft) 50%, var(--chip-bg) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  position: relative;
  overflow: hidden;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: var(--chip-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.icon-lg {
  width: 40px;
  height: 40px;
  color: var(--text-soft);
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 0.5rem;
}

.empty-desc {
  font-size: 0.95rem;
  color: var(--color-text-secondary);
  max-width: 400px;
  margin-bottom: 1.5rem;
}

/* 瀑布流网格 */
.masonry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  grid-auto-rows: 10px;
}

.gallery-card {
  border-radius: 16px;
  overflow: hidden;
  background: var(--color-bg-primary);
  border: 1px solid var(--border-soft);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.gallery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.card-media {
  position: relative;
  overflow: hidden;
  cursor: zoom-in;
}

.media-image,
.media-video {
  width: 100%;
  height: auto;
  display: block;
  transition: transform 0.5s ease;
}

.gallery-card:hover .media-image,
.gallery-card:hover .media-video {
  transform: scale(1.05);
}

.video-badge {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.75rem;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 999px;
  color: white;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.badge-icon {
  width: 12px;
  height: 12px;
}

.media-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  opacity: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 0.75rem;
  transition: opacity 0.3s ease;
}

.gallery-card:hover .media-overlay {
  opacity: 1;
}

.overlay-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  color: var(--color-text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.overlay-btn:hover {
  background: white;
  transform: scale(1.1);
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
}

.author-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.75rem 0.25rem 0.25rem;
  border-radius: 999px;
  border: none;
  background: var(--chip-bg);
  cursor: pointer;
  transition: all 0.2s ease;
}

.author-btn:hover {
  background: var(--border-soft);
}

.author-btn img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author-initial {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-name {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  border: none;
  background: var(--chip-bg);
  color: var(--color-text-secondary);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.stat-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.stat-btn.liked {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.stat-btn.liked .stat-icon {
  fill: currentColor;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

/* 加载更多 */
.load-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding-top: 2rem;
  border-top: 1px solid var(--border-soft);
  margin-top: 1.5rem;
}

.btn-load {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 2rem;
  border-radius: 12px;
  border: 1.5px solid var(--border-soft);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-load:hover:not(:disabled) {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
  background: rgba(127, 123, 255, 0.05);
}

.btn-load:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  color: var(--color-brand-primary);
}

/* 侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 1.25rem;
  backdrop-filter: blur(10px);
}

.sidebar-card.spotlight {
  background: linear-gradient(135deg, rgba(127, 123, 255, 0.08), rgba(255, 95, 143, 0.05));
}

.sidebar-card.cta-card {
  background: linear-gradient(135deg, rgba(127, 123, 255, 0.12), rgba(59, 130, 246, 0.08));
}

.card-header {
  margin-bottom: 1rem;
}

.card-eyebrow {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin-bottom: 0.25rem;
  display: block;
}

.card-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 创作者列表 */
.creator-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.creator-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.creator-item:hover {
  background: rgba(127, 123, 255, 0.05);
}

.creator-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-secondary);
  background: var(--chip-bg);
}

.creator-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: white;
}

.creator-rank.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: white;
}

.creator-rank.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: white;
}

.creator-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.9rem;
  font-weight: 600;
}

.creator-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-info {
  flex: 1;
  min-width: 0;
}

.creator-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.creator-handle {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.creator-stat {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-brand-primary);
}

.creator-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--color-text-secondary);
}

.empty-icon-sm {
  width: 24px;
  height: 24px;
  opacity: 0.5;
}

/* 热搜标签 */
.hashtag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.hashtag-btn {
  padding: 0.5rem 0.875rem;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--chip-bg);
  color: var(--color-text-secondary);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hashtag-btn:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
  background: rgba(127, 123, 255, 0.08);
}

/* CTA 卡片 */
.cta-content {
  margin-bottom: 1rem;
}

.cta-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 0.5rem;
}

.cta-desc {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.w-full {
  width: 100%;
  justify-content: center;
}

/* 深色主题 */
.dark .tag-text {
  color: #F9A8C8;
}

.dark .title-highlight {
  color: #F9A8C8;
}

.dark .tag-pill {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(249, 168, 200, 0.25);
}

.dark .tag-pill:hover {
  background: rgba(249, 168, 200, 0.15);
  border-color: rgba(249, 168, 200, 0.4);
}

.dark .tag-pill.active {
  background: #E87A9F;
  border-color: #E87A9F;
}

.dark .hero-tag {
  background: rgba(232, 122, 159, 0.12);
  border-color: rgba(232, 122, 159, 0.25);
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .hero-visual {
    display: none;
  }

  .main-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .gallery-page {
    min-height: 100vh;
    overflow-x: hidden;
  }

  .hero-section {
    padding: 80px 0 32px;
    min-height: auto;
  }

  .hero-content {
    max-width: 100%;
    padding: 0 0.5rem;
  }

  .hero-title {
    font-size: 1.75rem;
    line-height: 1.3;
    margin-bottom: 0.75rem;
  }

  .hero-desc {
    font-size: 0.9375rem;
    line-height: 1.6;
    margin-bottom: 1.5rem;
  }

  .hero-tag {
    margin-bottom: 1rem;
    padding: 0.375rem 0.875rem;
    font-size: 0.8125rem;
  }

  .search-box {
    margin-bottom: 1.25rem;
  }

  .search-input-wrapper {
    height: 52px;
  }

  .search-input {
    padding: 0.75rem 5rem 0.75rem 2.75rem;
    font-size: 0.9375rem;
  }

  .search-btn {
    right: 0.375rem;
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
  }

  .hero-actions {
    flex-direction: column;
    gap: 0.75rem;
    margin-bottom: 1.5rem;
  }

  .hero-actions .btn-primary,
  .hero-actions .btn-ghost {
    width: 100%;
    justify-content: center;
    padding: 0.875rem 1.5rem;
  }

  .sync-time {
    font-size: 0.8125rem;
  }

  /* Tags section mobile optimization */
  .tags-section {
    padding: 1.5rem 0;
  }

  .tags-header {
    flex-direction: column;
    gap: 0.5rem;
    margin-bottom: 1rem;
  }

  .tags-label {
    font-size: 0.875rem;
  }

  .tags-hint {
    font-size: 0.75rem;
  }

  .tags-cloud {
    gap: 0.5rem;
  }

  .tag-pill {
    padding: 0.5rem 0.875rem;
    font-size: 0.8125rem;
  }

  /* Feed section mobile optimization */
  .feed-section {
    padding: 1rem 0.5rem;
  }

  .feed-header {
    flex-direction: column;
    gap: 0.875rem;
    align-items: flex-start;
    margin-bottom: 1.25rem;
  }

  .feed-header-left {
    width: 100%;
  }

  .section-eyebrow {
    font-size: 0.6875rem;
    margin-bottom: 0.25rem;
  }

  .feed-title {
    font-size: 1.125rem;
  }

  .filter-chips {
    width: 100%;
    overflow-x: auto;
    flex-wrap: nowrap;
    gap: 0.5rem;
    padding-bottom: 0.25rem;
    -webkit-overflow-scrolling: touch;
  }

  .filter-chip {
    flex-shrink: 0;
    padding: 0.5rem 0.875rem;
    font-size: 0.8125rem;
  }

  /* Search status mobile */
  .search-status {
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
    padding: 0.875rem;
  }

  .status-tag {
    font-size: 0.9375rem;
  }

  .status-count {
    font-size: 0.8125rem;
  }

  /* Masonry grid mobile - ensure visibility */
  .masonry-grid {
    display: grid !important;
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 0.75rem;
    grid-auto-rows: 8px;
  }

  .gallery-card {
    border-radius: 12px;
    break-inside: avoid;
    page-break-inside: avoid;
  }

  .card-media {
    min-height: 120px;
  }

  .media-image,
  .media-video {
    width: 100%;
    height: auto;
    min-height: 120px;
    object-fit: cover;
  }

  /* Empty state mobile */
  .empty-state {
    padding: 2.5rem 1rem;
  }

  .empty-title {
    font-size: 1.125rem;
  }

  .empty-desc {
    font-size: 0.875rem;
    padding: 0 0.5rem;
  }

  /* Alert mobile */
  .alert-error {
    padding: 0.875rem;
    font-size: 0.875rem;
  }

  /* Skeleton mobile */
  .masonry-skeleton {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.75rem;
  }

  .skeleton-item {
    border-radius: 12px;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0 0.75rem;
  }

  .hero-section {
    padding: 72px 0 24px;
  }

  .hero-content {
    padding: 0;
  }

  .hero-title {
    font-size: 1.5rem;
    line-height: 1.25;
  }

  .hero-desc {
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .search-box {
    margin-bottom: 1rem;
  }

  .search-input-wrapper {
    height: 48px;
  }

  .search-input {
    padding: 0.625rem 4.5rem 0.625rem 2.5rem;
    font-size: 0.875rem;
  }

  .search-icon {
    left: 0.875rem;
    width: 18px;
    height: 18px;
  }

  .search-btn {
    right: 0.25rem;
    padding: 0.4375rem 0.875rem;
    font-size: 0.8125rem;
  }

  .hero-actions .btn-primary,
  .hero-actions .btn-ghost {
    padding: 0.75rem 1.25rem;
    font-size: 0.875rem;
  }

  /* Single column layout for very small screens */
  .masonry-grid {
    grid-template-columns: 1fr !important;
    gap: 0.625rem;
    grid-auto-rows: 6px;
  }

  .gallery-card {
    border-radius: 10px;
  }

  .card-content {
    padding: 0.625rem;
  }

  .card-title {
    font-size: 0.8125rem;
  }

  .card-meta {
    font-size: 0.6875rem;
  }

  .masonry-skeleton {
    grid-template-columns: 1fr;
    gap: 0.625rem;
  }

  .feed-section {
    padding: 0.75rem 0;
  }

  /* Tags section for small screens */
  .tags-section {
    padding: 1.25rem 0;
  }

  .tags-cloud {
    gap: 0.375rem;
  }

  .tag-pill {
    padding: 0.4375rem 0.75rem;
    font-size: 0.75rem;
  }

  /* Filter chips for small screens */
  .filter-chips {
    gap: 0.375rem;
  }

  .filter-chip {
    padding: 0.4375rem 0.75rem;
    font-size: 0.75rem;
  }
}

/* 动画 */
@keyframes like-animation {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.like-animation {
  animation: like-animation 0.3s ease;
}

/* CSS 变量 */
:root {
  --glass-bg: rgba(255, 255, 255, 0.03);
  --glass-border: rgba(255, 255, 255, 0.06);
  --border-soft: rgba(255, 255, 255, 0.08);
  --chip-bg: rgba(255, 255, 255, 0.05);
  --chip-border: rgba(255, 255, 255, 0.1);
  --chip-text: rgba(255, 255, 255, 0.7);
  --text-soft: rgba(255, 255, 255, 0.5);
}
</style>
