<template>
  <div class="space-y-6">
    <div v-if="errorMessage" class="rounded-2xl border border-brand-accent/30 bg-brand-accent/10 px-4 py-3 text-sm text-brand-accent">
      {{ errorMessage }}
    </div>

    <div v-if="loading && !galleryItems.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <div v-for="item in 6" :key="item" class="animate-pulse rounded-3xl border border-white/5 bg-white/5 p-4">
        <div class="h-40 rounded-2xl bg-white/5"></div>
        <div class="mt-4 h-4 w-2/3 rounded-full bg-white/10"></div>
        <div class="mt-2 h-4 w-1/3 rounded-full bg-white/10"></div>
      </div>
    </div>

    <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <article
        v-for="item in galleryItems"
        :key="item.id"
        class="chenxi-image-card group flex cursor-pointer flex-col gap-4 rounded-3xl border border-white/10 bg-white/5 p-4"
        @click="openModal(item)"
      >
        <div class="relative overflow-hidden rounded-2xl">
          <template v-if="item.mediaCategory === 'VIDEO'">
            <video
              class="h-48 w-full object-cover transition duration-500 group-hover:scale-105"
              :poster="resolvePosterUrl(item) || undefined"
              preload="metadata"
              muted
              playsinline
            >
              <source :src="resolvePublicUrl(item)" />
            </video>
            <span class="absolute left-3 top-3 rounded-full bg-black/60 px-3 py-1 text-xs text-white/80">短视频</span>
          </template>
          <template v-else>
            <img
              :src="resolvePublicUrl(item)"
              :alt="item.fileName"
              class="h-48 w-full object-cover transition duration-500 group-hover:scale-105"
              loading="lazy"
            />
          </template>
          <div class="pointer-events-none absolute inset-0 bg-gradient-to-t from-surface-body/70 via-transparent to-transparent" />
          <div class="absolute inset-x-0 bottom-0 flex items-center justify-between p-3 text-xs text-white/80">
              <span
                class="inline-flex items-center gap-1 rounded-full px-3 py-1 text-[11px] uppercase tracking-[0.35em]"
                :class="item.publicAccessible ? 'bg-white/10 text-white/70' : 'bg-rose-500/30 text-rose-100'"
              >
                {{ item.publicAccessible ? 'Public' : 'Private' }}
              </span>
              <button

              type="button"
              class="chenxi-like-btn inline-flex items-center gap-1 text-white/70 transition hover:text-white"
              :class="{ 'text-brand-accent': item.isLiked }"
              @click.stop="handleLike(item, $event)"
            >
              <ThumbsUp class="h-3.5 w-3.5" :class="{ 'fill-current': item.isLiked }" />
              {{ item.likeCount || 0 }}
            </button>
          </div>
          <button
            type="button"
            class="absolute right-3 top-3 inline-flex items-center justify-center rounded-full border border-white/20 bg-surface-body/80 p-2 text-white/70 transition hover:text-white"
            @click.stop="copyLink(resolvePublicUrl(item))"
          >
            <Copy class="h-4 w-4" />
          </button>
        </div>
        <div>
          <p class="text-sm font-semibold text-white">
            {{ item.fileName }}
          </p>
          <p class="text-xs text-white/60">
            {{ item.ownerDisplayName }} · {{ formatDate(item.uploadedAt) }}
          </p>
        </div>
      </article>
    </div>

    <div class="gallery-pagination-panel" v-if="totalElements > 0 || galleryItems.length">
      <div class="pagination-info">
        <span>当前第 <strong>{{ currentPage }}</strong> / {{ totalPages }}</span>
        <span>共 {{ totalElements }} 张</span>
        <span class="pagination-size">
          每页
          <el-select v-model="pageSize" size="small" class="page-size-select" @change="changePageSize">
            <el-option v-for="option in pageSizeOptions" :key="option" :label="`${option} 张`" :value="option" />
          </el-select>
          张
        </span>
      </div>
      <div class="pagination-actions">
        <el-button size="small" class="ghost-btn" :disabled="currentPage === 1" @click="goToPreviousPage">上一页</el-button>
        <el-button size="small" class="ghost-btn" :disabled="currentPage === totalPages" @click="goToNextPage">下一页</el-button>
        <div class="jump-control">
          <span>跳转到</span>
          <el-input
            v-model="jumpPageInput"
            size="small"
            class="jump-input"
            type="number"
            placeholder="页码"
            @keyup.enter.native="applyJump"
          />
          <el-button size="small" type="primary" plain @click="applyJump">确认</el-button>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div v-if="activeItem" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4 py-8" @click.self="closeModal">
        <div class="relative w-full max-w-3xl rounded-3xl border border-white/10 bg-surface-panel/95 p-6 text-white shadow-card">
          <button
            type="button"
            class="absolute right-4 top-4 rounded-full border border-white/10 bg-white/5 p-2 text-white/60 transition hover:text-white"
            @click="closeModal"
          >
            <X class="h-4 w-4" />
          </button>
          <div class="grid gap-6 lg:grid-cols-2">
            <div class="space-y-4">
              <template v-if="activeItem && activeItem.mediaCategory === 'VIDEO'">
                <video
                  class="h-72 w-full rounded-2xl object-cover"
                  controls
                  playsinline
                  preload="metadata"
                  :poster="resolvePosterUrl(activeItem) || undefined"
                >
                  <source :src="resolvePublicUrl(activeItem)" />
                </video>
              </template>
              <template v-else>
                <img
                  :src="resolvePublicUrl(activeItem)"
                  :alt="activeItem.fileName"
                  class="h-72 w-full rounded-2xl object-cover"
                  loading="lazy"
                />
              </template>
              <div class="flex flex-wrap gap-3 text-xs text-white/70">
                <span class="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1">
                  <CalendarClock class="h-3.5 w-3.5" />
                  {{ formatDate(activeItem.uploadedAt) }}
                </span>
                <span class="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1">
                  <ThumbsUp class="h-3.5 w-3.5" :class="{ 'text-brand-accent fill-current': activeItem.isLiked }" />
                  {{ activeItem.likeCount || 0 }} 喜欢
                </span>
                <span class="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1">
                  <Eye class="h-3.5 w-3.5 text-brand-primary" />
                  {{ activeItem.invokeCount || 0 }} 次引用
                </span>
              </div>
            </div>
            <div class="space-y-4">
              <div>
                <p class="text-xs uppercase tracking-[0.35em] text-white/50">文件信息</p>
                <p class="mt-2 text-xl font-semibold text-white">{{ activeItem.fileName }}</p>
                <p class="text-sm text-white/60">由 {{ activeItem.ownerDisplayName }} 上传</p>
              </div>
              <ul class="space-y-3 text-sm text-white/70">
                <li class="flex items-center gap-2">
                  <FileImage class="h-4 w-4 text-brand-primary" />
                  <span>{{ formatBytes(activeItem.size) }}</span>
                </li>
                <li class="flex items-center gap-2">
                  <LinkIcon class="h-4 w-4 text-brand-accent" />
                  <span class="truncate">{{ resolvePublicUrl(activeItem) }}</span>
                </li>
                <li class="flex items-center gap-2">
                  <Layers class="h-4 w-4 text-brand-emerald" />
                  <span>对象键：{{ activeItem.objectKey }}</span>
                </li>
              </ul>
              <div class="rounded-2xl border border-white/5 bg-white/5 p-4">
                <p class="text-xs uppercase tracking-[0.35em] text-white/50">上传者</p>
                <button
                  type="button"
                  class="mt-3 flex w-full items-center gap-3 rounded-2xl border border-white/10 bg-black/20 px-3 py-2 text-left transition hover:border-brand-primary hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="!activeItem.ownerId"
                  @click="goToUserProfile(activeItem.ownerId)"
                >
                  <img
                    v-if="activeItem.ownerAvatarUrl"
                    :src="activeItem.ownerAvatarUrl"
                    alt="uploader avatar"
                    class="h-10 w-10 rounded-full border border-white/20 object-cover"
                  />
                  <div
                    v-else
                    class="flex h-10 w-10 items-center justify-center rounded-full border border-white/20 bg-white/10 text-sm font-semibold text-white/70"
                  >
                    {{ activeItem.ownerDisplayName ? activeItem.ownerDisplayName.slice(0, 1) : 'U' }}
                  </div>
                  <div class="flex-1">
                    <p class="text-sm font-semibold text-white">{{ activeItem.ownerDisplayName }}</p>
                    <p class="text-xs text-white/60">查看用户详情</p>
                  </div>
                  <ChevronRight class="h-4 w-4 text-white/60" />
                </button>
              </div>
              <div class="space-y-2 rounded-2xl border border-white/5 bg-white/5 p-4">
                <p class="text-sm font-medium text-white">点赞统计</p>
                <div class="flex items-center gap-3 text-xs text-white/70">
                  <User class="h-3.5 w-3.5" />
                  <div class="flex items-center gap-1">
                    <span>最近点赞：</span>
                    <button
                      v-if="hasLatestLikerProfile(activeItem)"
                      type="button"
                      class="underline decoration-dotted decoration-brand-primary text-brand-primary/90 hover:text-brand-primary"
                      @click="openLatestLikerProfile(activeItem)"
                    >
                      {{ getLikeDisplayName(activeItem) }}
                    </button>
                    <span v-else>{{ getLikeDisplayName(activeItem) }}</span>
                  </div>
                </div>
                <button
                  type="button"
                  class="chenxi-like-btn mt-3 inline-flex items-center gap-2 rounded-2xl border border-white/20 px-4 py-2 text-sm font-semibold text-white/80 transition hover:border-white hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                  :class="{ 'border-brand-accent text-brand-accent': activeItem.isLiked }"
                  :disabled="shouldDisableLikeButton(activeItem)"
                  @click="handleLike(activeItem, $event)"
                >
                  <ThumbsUp class="h-4 w-4" :class="{ 'fill-current': activeItem.isLiked }" />
                  {{ activeItem.isLiked ? '取消点赞' : '点赞' }}
                </button>
                <p
                  v-if="!auth.isAuthenticated && !guestLikeEnabled"
                  class="mt-2 text-[11px] text-rose-100/80"
                >
                  管理员暂时关闭 访客点赞，登录即可继续互动
                </p>
              </div>
              <div class="flex flex-wrap gap-3">
                <a
                  :href="resolvePublicUrl(activeItem)"
                  target="_blank"
                  rel="noopener"
                  class="inline-flex flex-1 items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent px-4 py-3 text-sm font-semibold text-white"
                >
                  <ExternalLink class="h-4 w-4" />
                  新标签打开
                </a>
                <button
                  type="button"
                  class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/20 px-4 py-3 text-sm font-semibold text-white/80 transition hover:border-white hover:text-white"
                  @click="copyLink(resolvePublicUrl(activeItem))"
                >
                  <Copy class="h-4 w-4" />
                  复制链接
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  CalendarClock,
  ChevronRight,
  Copy,
  ExternalLink,
  Eye,
  FileImage,
  Layers,
  LinkIcon,
  PlayCircle,
  X,
  ThumbsUp,
  User,
} from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import { fetchPublicGallery, likeImage, unlikeImage } from '../../services/gallery'
import '../../assets/styles/chenxi-interactions.css'

const auth = useAuthStore()
const galleryItems = ref([])
const totalElements = ref(0)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSizeOptions = [21, 42, 63]
const pageSize = ref(21)
const jumpPageInput = ref('')
const queuedPage = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const activeItem = ref(null)
const guestLikeEnabled = ref(true)
const router = useRouter()
const assetBase = (import.meta.env.VITE_PUBLIC_ASSET_BASE || '').replace(/\/$/, '')

const decorateItem = (item = {}) => ({
  ...item,
  isLiked: Boolean(item.likedByMe ?? item.isLiked),
  latestLike: item.latestLike || null,
  publicAccessible: item.publicAccessible !== false,
  mediaCategory: item.mediaCategory || 'IMAGE',
  thumbnailUrl: item.thumbnailUrl || '',
})

const mapItems = (items = []) => items.map(decorateItem)

onMounted(() => {
  loadGallery(1)
})

async function loadGallery(targetPage = 1) {
  if (loading.value) {
    queuedPage.value = targetPage
    return
  }
  loading.value = true
  queuedPage.value = null
  errorMessage.value = ''
  try {
    const zeroBasedPage = Math.max(targetPage - 1, 0)
    const response = await fetchPublicGallery({ page: zeroBasedPage, size: pageSize.value })
    const normalizedItems = mapItems(response.items || [])
    galleryItems.value = normalizedItems
    guestLikeEnabled.value = response.guestLikeEnabled ?? true
    totalElements.value = response.totalElements ?? normalizedItems.length
    totalPages.value = Math.max(response.totalPages || Math.ceil(totalElements.value / pageSize.value) || 1, 1)
    currentPage.value = (response.page ?? zeroBasedPage) + 1
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '加载公开图库失败'
  } finally {
    loading.value = false
    if (queuedPage.value !== null) {
      const pendingPage = queuedPage.value
      queuedPage.value = null
      loadGallery(pendingPage)
    }
  }
}

const changePageSize = (value) => {
  pageSize.value = value
}

const goToPreviousPage = () => {
  if (currentPage.value > 1) {
    loadGallery(currentPage.value - 1)
  }
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) {
    loadGallery(currentPage.value + 1)
  }
}

const applyJump = () => {
  const parsed = Number(jumpPageInput.value)
  if (!parsed || Number.isNaN(parsed)) {
    jumpPageInput.value = ''
    return
  }
  const target = Math.min(Math.max(Math.floor(parsed), 1), totalPages.value)
  loadGallery(target)
  jumpPageInput.value = ''
}

watch(pageSize, () => {
  loadGallery(1)
})

function openModal(item) {
  activeItem.value = item
}

function closeModal() {
  activeItem.value = null
}

function formatDate(value) {
  if (!value) return '时间未知'
  const date = new Date(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatBytes(bytes) {
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

function resolvePublicUrl(item) {
  if (!item) {
    return ''
  }
  const link = item.publicUrl
  if (link && /^https?:\/\//i.test(link)) {
    return link
  }
  if (assetBase && item.objectKey) {
    const normalizedKey = String(item.objectKey).replace(/^\/+/, '')
    return `${assetBase}/${normalizedKey}`
  }
  return link || ''
}

const videoExtensionPattern = /\.(mp4|mov|mpe?g|avi|wmv|webm|flv|mkv|m4v)$/i

function looksLikeVideoUrl(url) {
  if (typeof url !== 'string') {
    return false
  }
  const normalized = url.split('?')[0]
  return videoExtensionPattern.test(normalized)
}

function resolvePosterUrl(item) {
  if (!item) {
    return ''
  }
  if (item.thumbnailUrl) {
    if (/^https?:\/\//i.test(item.thumbnailUrl)) {
      return item.thumbnailUrl
    }
    if (assetBase) {
      const normalizedKey = String(item.thumbnailUrl).replace(/^\/+/, '')
      return `${assetBase}/${normalizedKey}`
    }
    return item.thumbnailUrl
  }
  return ''
}

async function copyLink(link) {
  if (!link) {
    ElMessage.warning('暂无可复制的链接')
    return
  }
  try {
    if (navigator?.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(link)
    } else {
      legacyCopyLink(link)
    }
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败', error)
    ElMessage.error('复制失败')
  }
}

function legacyCopyLink(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'absolute'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)
  const successful = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!successful) {
    throw new Error('Legacy clipboard copy failed')
  }
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

const hasLatestLikerProfile = (item) => Boolean(item?.latestLike && !item.latestLike.guest && item.latestLike.userId)

const ensureLikeAllowed = () => {
  if (!auth.isAuthenticated && !guestLikeEnabled.value) {
    ElMessage.warning('管理员已关闭 访客点赞，请登录后操作')
    return false
  }
  return true
}

const shouldDisableLikeButton = (item) => {
  if (!item) return true
  if (auth.isAuthenticated) {
    return false
  }
  return !guestLikeEnabled.value && !item.isLiked
}

async function handleLike(item, event) {
  if (!item || !ensureLikeAllowed()) {
    return
  }
  const isLiked = item.isLiked
  try {
    if (event?.target) {
      const button = event.target.closest('.chenxi-like-btn')
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

function getLikeDisplayName(item) {
  if (!item) return '暂无记录'
  if (item.latestLike) {
    if (item.latestLike.guest) {
      return '访客'
    }
    return item.latestLike.displayName || '站内用户'
  }
  if (auth.isAuthenticated && item.isLiked) {
    return auth.profile?.username || '您'
  }
  return '暂无记录'
}

function openLatestLikerProfile(item) {
  if (!hasLatestLikerProfile(item)) {
    return
  }
  goToUserProfile(item.latestLike.userId)
}

function goToUserProfile(userId) {
  if (!userId) {
    ElMessage.info('该用户暂无公开主页')
    return
  }
  closeModal()
  router.push({ name: 'public-user-profile', params: { userId } })
}
</script>

<style scoped>
.gallery-pagination-panel {
  margin-top: 1rem;
  padding: 1rem 1.25rem;
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(5, 5, 5, 0.85);
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
}

.pagination-info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.7);
}

.pagination-info strong {
  color: #fff;
}

.pagination-size {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.page-size-select {
  width: 110px;
}

.pagination-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.jump-control {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: rgba(255, 255, 255, 0.7);
}

.jump-input {
  width: 90px;
}

.jump-input :deep(.el-input__wrapper),
.page-size-select :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
}

.ghost-btn {
  border-color: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.04);
}

.ghost-btn:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
}

.ghost-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.video-poster-fallback {
  display: flex;
  height: 12rem;
  width: 100%;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.35rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.4), rgba(236, 72, 153, 0.35));
  border-radius: 1.25rem;
}

@media (max-width: 768px) {
  .gallery-pagination-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .pagination-actions {
    width: 100%;
  }
}
</style>
