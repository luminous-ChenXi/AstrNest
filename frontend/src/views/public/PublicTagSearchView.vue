<template>
  <div class="min-h-screen bg-surface-body text-white">
    <div class="relative border-b border-white/10 bg-[radial-gradient(circle_at_20%_20%,rgba(127,123,255,0.25),transparent_55%)]">
      <div class="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-16 md:flex-row md:items-center md:justify-between">
        <div class="space-y-3">
          <p class="text-xs uppercase tracking-[0.45em] text-white/50">标签检索</p>
          <h1 class="text-3xl font-semibold leading-tight md:text-4xl">图片标签搜索</h1>
          <p class="text-sm text-white/70">
            通过标签快速筛选公开图库，登录用户还可搜索自己的私有图片；访客仅可检索公开内容。
          </p>
        </div>
        <RouterLink
          to="/"
          class="inline-flex items-center gap-2 rounded-full border border-white/20 px-5 py-2 text-sm font-medium text-white/80 transition hover:border-white hover:text-white"
        >
          返回首页
        </RouterLink>
      </div>
    </div>

    <main class="mx-auto max-w-6xl space-y-10 px-6 py-12">
      <section class="glass-panel rounded-3xl border border-white/10 bg-white/5 p-6 shadow-card">
        <form class="flex flex-col gap-4 md:flex-row" @submit.prevent="submitSearch">
          <div class="relative flex-1">
            <Search class="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-white/40" />
            <input
              v-model="keyword"
              type="text"
              maxlength="120"
              placeholder="输入要搜索的标签，例如：风景 / 插画 / 星空"
              class="w-full rounded-2xl border border-white/15 bg-black/30 px-12 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
            />
          </div>
          <button
            type="submit"
            class="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent px-6 py-3 text-sm font-semibold text-white shadow-[0_10px_30px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loading"
          >
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            <Search v-else class="h-4 w-4" />
            搜索
          </button>
        </form>
        <p class="mt-3 text-xs text-white/60">
          搜索结果仅展示符合条件的公开图片；如果您已登录，还会包含您个人上传且设置为私有的内容。
        </p>
      </section>

      <section class="space-y-6">
        <div v-if="errorMessage" class="rounded-2xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
          {{ errorMessage }}
        </div>

        <div v-if="loading && !resultItems.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div v-for="item in 6" :key="item" class="animate-pulse space-y-3 rounded-3xl border border-white/5 bg-white/5 p-4">
            <div class="h-40 rounded-2xl bg-white/10"></div>
            <div class="h-4 w-3/4 rounded-full bg-white/10"></div>
            <div class="h-3 w-1/2 rounded-full bg-white/10"></div>
            <div class="flex flex-wrap gap-2">
              <span class="h-6 w-16 rounded-full bg-white/10"></span>
              <span class="h-6 w-20 rounded-full bg-white/10"></span>
              <span class="h-6 w-14 rounded-full bg-white/10"></span>
            </div>
          </div>
        </div>

        <div v-else-if="hasSearched && !resultItems.length" class="rounded-3xl border border-white/10 bg-white/5 px-6 py-10 text-center text-white/70">
          {{ emptyMessage }}
        </div>

        <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <article
            v-for="item in resultItems"
            :key="item.id"
            class="flex flex-col gap-4 rounded-3xl border border-white/10 bg-white/5 p-5"
          >
            <div class="relative overflow-hidden rounded-2xl">
              <template v-if="item.mediaCategory === 'VIDEO'">
                <video
                  class="h-48 w-full object-cover"
                  :poster="resolvePosterUrl(item) || undefined"
                  preload="metadata"
                  muted
                  playsinline
                >
                  <source :src="resolvePublicUrl(item)" />
                </video>
                <span class="absolute left-3 top-3 inline-flex items-center gap-2 rounded-full bg-black/60 px-3 py-1 text-xs font-semibold text-white/80">
                  短视频
                </span>
              </template>
              <template v-else>
                <img
                  :src="resolvePublicUrl(item)"
                  :alt="item.fileName"
                  class="h-48 w-full object-cover"
                  loading="lazy"
                />
              </template>
              <div class="pointer-events-none absolute inset-0 bg-gradient-to-t from-surface-body/80 via-transparent to-transparent"></div>
              <div class="absolute left-3 top-3 inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold"
                :class="item.publicAccessible ? 'bg-emerald-400/20 text-emerald-200' : 'bg-rose-400/20 text-rose-100'"
              >
                {{ item.publicAccessible ? '公开' : '仅自己可见' }}
              </div>
              <button
                type="button"
                class="absolute right-3 top-3 inline-flex items-center gap-1 rounded-full border border-white/20 bg-black/40 px-3 py-1 text-xs text-white/80 transition hover:border-white hover:text-white"
                @click="copyLink(resolvePublicUrl(item))"
              >
                <Copy class="h-3.5 w-3.5" />
                复制链接
              </button>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">{{ item.fileName }}</p>
              <p class="text-xs text-white/60">
                {{ formatDate(item.uploadedAt) }} · {{ formatBytes(item.size) }}
              </p>
            </div>
            <div class="flex flex-wrap gap-2 text-xs text-white/80">
              <span
                v-for="tag in item.tags"
                :key="tag.id || `${item.id}-${tag.name}`"
                class="tag-pill"
              >
                #{{ tag.name }}
              </span>
              <span v-if="!item.tags.length" class="tag-pill muted">未添加标签</span>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-xs text-white/70">
              <span>上传者：{{ item.ownerDisplayName }}</span>
              <RouterLink
                v-if="item.ownerId"
                :to="{ name: 'public-user-profile', params: { userId: item.ownerId } }"
                class="inline-flex items-center gap-1 rounded-full border border-white/20 px-3 py-1 text-white/80 transition hover:border-white hover:text-white"
              >
                <LinkIcon class="h-3.5 w-3.5" />
                查看主页
              </RouterLink>
              <span v-else class="inline-flex items-center gap-1 rounded-full border border-white/10 px-3 py-1 text-white/50">
                匿名上传
              </span>
            </div>
          </article>
        </div>

        <div
          v-if="paginationVisible"
          class="flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-white/10 bg-white/5 px-5 py-4 text-sm text-white/70"
        >
          <div>
            共 {{ totalElements }} 张 · 第 {{ currentPage }} / {{ totalPages || 1 }} 页
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <button
              type="button"
              class="rounded-full border border-white/20 px-4 py-1 text-white/80 transition hover:border-white hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="currentPage === 1"
              @click="goToPreviousPage"
            >
              上一页
            </button>
            <button
              type="button"
              class="rounded-full border border-white/20 px-4 py-1 text-white/80 transition hover:border-white hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="currentPage >= totalPages"
              @click="goToNextPage"
            >
              下一页
            </button>
            <div class="inline-flex items-center gap-2">
              <span>每页</span>
              <el-select v-model="pageSize" size="small" class="w-28">
                <el-option v-for="size in pageSizeOptions" :key="size" :label="`${size} 张`" :value="size" />
              </el-select>
            </div>
          </div>
        </div>
      </section>
    </main>

    <ChenxiGlobalFooter />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Copy, LinkIcon, Loader2, Search } from 'lucide-vue-next'
import ChenxiGlobalFooter from '../../components/common/ChenxiGlobalFooter.vue'
import { searchGalleryByTag } from '../../services/gallery'

const keyword = ref('')
const lastSubmittedKeyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const resultItems = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const pageSizeOptions = [21, 42, 63]
const pageSize = ref(21)
const hasSearched = ref(false)
const assetBase = (import.meta.env.VITE_PUBLIC_ASSET_BASE || '').replace(/\/$/, '')

const emptyMessage = computed(() => {
  if (!lastSubmittedKeyword.value) {
    return '请输入要搜索的标签内容'
  }
  return `没有找到包含 “${lastSubmittedKeyword.value}” 的图片`
})

const paginationVisible = computed(() => totalPages.value > 1)

const decorateItem = (item = {}) => ({
  ...item,
  tags: Array.isArray(item.tags) ? item.tags : [],
  publicAccessible: item.publicAccessible !== false,
  mediaCategory: item.mediaCategory || 'IMAGE',
  thumbnailUrl: item.thumbnailUrl || '',
})

const videoExtensionPattern = /\.(mp4|mov|mpe?g|avi|wmv|webm|flv|mkv|m4v)$/i

const resolvePublicUrl = (item) => {
  if (!item) {
    return ''
  }
  if (item.publicUrl && /^https?:\/\//i.test(item.publicUrl)) {
    return item.publicUrl
  }
  if (assetBase && item.objectKey) {
    const normalizedKey = String(item.objectKey).replace(/^\/+/, '')
    return `${assetBase}/${normalizedKey}`
  }
  return item.publicUrl || ''
}

const formatDate = (value) => {
  if (!value) {
    return '时间未知'
  }
  const date = new Date(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatBytes = (bytes) => {
  if (!bytes) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let sizeValue = bytes
  let unitIndex = 0
  while (sizeValue >= 1024 && unitIndex < units.length - 1) {
    sizeValue /= 1024
    unitIndex += 1
  }
  return `${sizeValue.toFixed(sizeValue >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

const looksLikeVideoUrl = (url) => {
  if (typeof url !== 'string') {
    return false
  }
  const normalized = url.split('?')[0]
  return videoExtensionPattern.test(normalized)
}

const resolvePosterUrl = (item) => {
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
  if (item.mediaCategory !== 'VIDEO') {
    return resolvePublicUrl(item)
  }
  return ''
}

const submitSearch = () => {
  performSearch(1)
}

async function performSearch(targetPage = 1) {
  const sanitized = keyword.value.trim()
  if (!sanitized) {
    ElMessage.warning('请输入要搜索的标签')
    return
  }
  loading.value = true
  errorMessage.value = ''
  hasSearched.value = true
  try {
    const response = await searchGalleryByTag({
      keyword: sanitized,
      page: Math.max(targetPage - 1, 0),
      size: pageSize.value,
    })
    lastSubmittedKeyword.value = sanitized
    const items = (response?.items || []).map(decorateItem)
    resultItems.value = items
    totalElements.value = response?.totalElements ?? items.length
    const backendPages = response?.totalPages
    if (typeof backendPages === 'number' && backendPages > 0) {
      totalPages.value = backendPages
    } else if (totalElements.value > 0) {
      totalPages.value = Math.max(Math.ceil(totalElements.value / pageSize.value), 1)
    } else {
      totalPages.value = 0
    }
    currentPage.value = (response?.page ?? 0) + 1
  } catch (error) {
    console.error('标签搜索失败', error)
    errorMessage.value = error?.response?.data?.message || '搜索失败，请稍后重试'
    resultItems.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const goToPreviousPage = () => {
  if (currentPage.value > 1) {
    performSearch(currentPage.value - 1)
  }
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) {
    performSearch(currentPage.value + 1)
  }
}

watch(pageSize, () => {
  if (hasSearched.value) {
    performSearch(1)
  }
})

const copyLink = async (link) => {
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
    ElMessage.success('链接已复制')
  } catch (error) {
    console.error('复制失败', error)
    ElMessage.error('复制失败，请重试')
  }
}

const legacyCopyLink = (text) => {
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
</script>

<style scoped>
.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.05);
  font-size: 0.75rem;
}

.tag-pill.muted {
  color: rgba(255, 255, 255, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
