<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  deleteUpload,
  deleteUploadsBatch,
  fetchUploadDetail,
  fetchUploads,
  toggleUploadLike,
  updateUploadVisibility,
} from '../../services/user'
import { useUploadStore } from '../../stores/upload'
import UploadResultModal from '../../components/user/UploadResultModal.vue'

const upload = useUploadStore()

const items = ref([])
const page = ref(0)
const size = ref(12)
const total = ref(0)
const loading = ref(false)
const deleting = ref(0)
const batchDeleting = ref(false)
const message = ref('')
const selected = ref(new Set())
const modalOpen = ref(false)
const modalItems = ref([])
const detailMode = ref(false)
const detailMessage = ref('')
const detailLoading = ref(false)
const actionLoading = ref(false)

const syncSelection = () => {
  const availableIds = new Set(items.value.map((item) => item.id))
  selected.value = new Set([...selected.value].filter((id) => availableIds.has(id)))
}

const loadData = async () => {
  loading.value = true
  message.value = ''
  try {
    const { data } = await fetchUploads({ page: page.value, size: size.value })
    items.value = data.items || []
    total.value = data.total || 0
    syncSelection()
  } catch (error) {
    message.value = error.response?.data?.message || '获取图片列表失败'
  } finally {
    loading.value = false
  }
}

const totalPages = () => Math.ceil(total.value / size.value)

const goPage = (newPage) => {
  if (newPage < 0 || newPage >= totalPages()) return
  page.value = newPage
  loadData()
}

const isSelected = (id) => selected.value.has(id)

const toggleSelect = (id) => {
  const next = new Set(selected.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selected.value = next
}

const selectedCount = computed(() => selected.value.size)

const patchListRecord = (id, payload) => {
  const index = items.value.findIndex((entry) => entry.id === id)
  if (index !== -1) {
    items.value[index] = { ...items.value[index], ...payload }
  }
}

const openDetailModal = async (item) => {
  detailMessage.value = ''
  detailLoading.value = true
  try {
    const { data } = await fetchUploadDetail(item.id)
    modalItems.value = [data]
    detailMode.value = true
    modalOpen.value = true
  } catch (error) {
    message.value = error.response?.data?.message || '获取图片详情失败'
  } finally {
    detailLoading.value = false
  }
}

const closeCopyModal = () => {
  modalOpen.value = false
  detailMode.value = false
  detailMessage.value = ''
  actionLoading.value = false
}

const removeItem = async (item) => {
  if (!confirm(`确认删除 ${item.fileName} 吗？`)) return
  deleting.value = item.id
  message.value = ''
  try {
    await deleteUpload(item.id)
    selected.value.delete(item.id)
    message.value = '删除成功'
    await loadData()
  } catch (error) {
    message.value = error.response?.data?.message || '删除失败'
  } finally {
    deleting.value = 0
  }
}

const handleBatchDelete = async () => {
  if (!selectedCount.value) return
  if (!confirm(`确认删除选中的 ${selectedCount.value} 张图片？`)) return
  batchDeleting.value = true
  message.value = ''
  try {
    await deleteUploadsBatch(Array.from(selected.value))
    message.value = '批量删除成功'
    selected.value = new Set()
    await loadData()
  } catch (error) {
    message.value = error.response?.data?.message || '批量删除失败'
  } finally {
    batchDeleting.value = false
  }
}

const handleToggleLike = async (item) => {
  if (actionLoading.value) return
  actionLoading.value = true
  detailMessage.value = ''
  try {
    const { data } = await toggleUploadLike(item.id)
    const updated = { ...item, likeCount: data.likeCount, likedByMe: data.liked }
    modalItems.value = [updated]
    patchListRecord(item.id, { likeCount: data.likeCount })
  } catch (error) {
    detailMessage.value = error.response?.data?.message || '更新喜欢状态失败'
  } finally {
    actionLoading.value = false
  }
}

const handleVisibilityChange = async (item, nextValue) => {
  if (actionLoading.value) return
  actionLoading.value = true
  detailMessage.value = ''
  try {
    const { data } = await updateUploadVisibility(item.id, { publicAccessible: nextValue })
    modalItems.value = [data]
    patchListRecord(item.id, { publicAccessible: data.publicAccessible })
  } catch (error) {
    detailMessage.value = error.response?.data?.message || '更新公开状态失败'
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="user-images space-y-8">
    <section class="glass-panel rounded-[32px] border border-body bg-surface-overlay p-6">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h3 class="text-xl font-semibold">媒体管理</h3>
          <p class="text-sm text-body-soft">批量查看、复制或删除你的上传记录</p>
          <div v-if="upload.limits && upload.limits.maxFilesPerDay" class="mt-2 text-xs text-body-faint">
            单次最多上传 {{ upload.limits.maxFilesPerDay }} 个文件，单个文件不超过 {{ upload.limits.maxFileSizeMb }}MB。
            允许的扩展名：{{ upload.limits.allowedTypes ? upload.limits.allowedTypes.join(', ') : 'jpg, png, gif, webp' }}
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-3 text-xs text-body-muted">
          <span class="rounded-full border border-body px-3 py-1">共 {{ total }} 张</span>
          <span v-if="selectedCount" class="rounded-full border border-brand-primary/40 px-3 py-1 text-brand-primary">
            已选择 {{ selectedCount }} 张
          </span>
          <button
            class="rounded-full border border-rose-400/40 px-4 py-2 text-sm font-semibold text-rose-300 transition hover:border-rose-300 hover:text-rose-100 disabled:opacity-50"
            :disabled="!selectedCount || batchDeleting"
            @click="handleBatchDelete"
          >
            {{ batchDeleting ? '删除中…' : `批量删除${selectedCount ? `（${selectedCount}）` : ''}` }}
          </button>
        </div>
      </div>
      <p v-if="message" class="mt-4 text-sm" :class="message.includes('失败') ? 'text-brand-accent' : 'text-emerald-500'">{{ message }}</p>
    </section>

    <section>
      <div v-if="loading" class="rounded-3xl border border-body bg-surface-overlay p-6 text-sm text-body-soft">加载中…</div>
      <div v-else-if="!items.length" class="rounded-3xl border border-body bg-surface-overlay p-6 text-sm text-body-soft">暂无图片，快去上传吧</div>
      <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="item in items"
          :key="item.id"
          class="glass-panel flex flex-col gap-4 rounded-3xl border border-body bg-surface-strong p-4 shadow-card"
        >
          <div class="relative overflow-hidden rounded-2xl">
            <template v-if="item.mediaCategory === 'VIDEO'">
              <video
                class="h-56 w-full object-cover"
                :poster="item.thumbnailUrl || undefined"
                preload="metadata"
                muted
                playsinline
              >
                <source :src="item.publicUrl" />
              </video>
              <span class="media-chip absolute right-3 top-3 rounded-full px-3 py-1 text-xs">短视频</span>
            </template>
            <template v-else>
              <img :src="item.publicUrl" :alt="item.fileName" class="h-56 w-full object-cover" />
            </template>
            <label class="absolute left-3 top-3 inline-flex items-center gap-2 rounded-full bg-surface-overlay/80 px-3 py-1 text-xs">
              <input
                type="checkbox"
                class="h-4 w-4 rounded border-body bg-transparent text-brand-primary focus:ring-brand-primary/40"
                :checked="isSelected(item.id)"
                @change.stop="toggleSelect(item.id)"
              />

            </label>
            <!-- 选择框 -->
          </div>
          <div class="space-y-2 text-sm">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <p class="font-semibold text-body-primary truncate" :title="item.fileName">{{ item.fileName }}</p>
                <p class="text-xs text-body-soft">{{ (item.size / 1024).toFixed(1) }} KB · {{ item.reviewStatus || '已发布' }}</p>
              </div>
              <div class="min-w-[7rem] shrink-0 text-right text-xs text-body-soft">
                <p>状态：{{ item.publicAccessible ? '公开' : '仅自己' }}</p>
                <p>喜欢：{{ item.likeCount || 0 }}</p>
              </div>
            </div>
            <div class="flex flex-wrap gap-2 text-xs text-body-soft">
              <span class="rounded-full border border-body px-2 py-1">调用 {{ item.invokeCount || 0 }}</span>
              <span v-if="item.violation" class="rounded-full border border-brand-accent/40 px-2 py-1 text-brand-accent">需复检</span>
            </div>
          </div>
          <div class="flex gap-3 text-sm font-semibold">
            <button
              class="flex-1 rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent px-3 py-2 text-sm text-white shadow-[0_10px_30px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5 disabled:opacity-60"
              type="button"
              :disabled="detailLoading"
              @click="openDetailModal(item)"
            >
              {{ detailLoading ? '加载中…' : '详情 / 复制' }}
            </button>
            <button
              class="flex-1 rounded-2xl border border-rose-400/40 px-3 py-2 text-sm text-rose-300 transition hover:border-rose-300 hover:text-rose-100 disabled:opacity-50"
              type="button"
              :disabled="deleting === item.id"
              @click="removeItem(item)"
            >
              {{ deleting === item.id ? '删除中…' : '删除' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="totalPages() > 1" class="flex items-center justify-center gap-4 text-sm">
      <button class="rounded-full border border-body px-4 py-2 text-body-muted disabled:opacity-40" :disabled="page === 0" @click="goPage(page - 1)">
        上一页
      </button>
      <span class="text-body-soft">{{ page + 1 }} / {{ totalPages() }}</span>
      <button class="rounded-full border border-body px-4 py-2 text-body-muted disabled:opacity-40" :disabled="page >= totalPages() - 1" @click="goPage(page + 1)">
        下一页
      </button>
    </div>
  </div>

  <UploadResultModal :open="modalOpen" :items="modalItems" title="图片详情" @close="closeCopyModal">
    <template #extra="{ item }">
      <div v-if="detailMode" class="space-y-4 rounded-2xl border border-body bg-surface-overlay p-4 text-sm text-body-primary">
        <div class="flex items-center justify-between">
          <span>喜欢人数</span>
          <span class="font-semibold text-emerald-500">{{ item.likeCount || 0 }}</span>
        </div>
        <button
          class="w-full rounded-full border border-emerald-500/60 px-3 py-2 font-semibold text-emerald-500 transition hover:bg-emerald-500 hover:text-white disabled:opacity-50"
          type="button"
          :disabled="actionLoading"
          @click="handleToggleLike(item)"
        >
          {{ item.likedByMe ? '取消喜欢' : '喜欢这张图' }}
        </button>
        <div class="flex items-center justify-between">
          <span>公开状态</span>
          <span :class="item.publicAccessible ? 'text-emerald-500' : 'text-amber-500'">
            {{ item.publicAccessible ? '公开访问' : '仅自己可见' }}
          </span>
        </div>
        <button
          class="w-full rounded-full border border-body px-3 py-2 font-semibold text-body-primary transition hover:bg-surface-strong disabled:opacity-50"
          type="button"
          :disabled="actionLoading"
          @click="handleVisibilityChange(item, !item.publicAccessible)"
        >
          {{ item.publicAccessible ? '改为私密' : '设为公开' }}
        </button>
        <p v-if="detailMessage" class="text-xs text-brand-accent">{{ detailMessage }}</p>
      </div>
    </template>
  </UploadResultModal>
</template>

<style scoped>
.media-chip {
  background: rgba(0, 0, 0, 0.55);
  color: var(--color-on-accent);
}
</style>
