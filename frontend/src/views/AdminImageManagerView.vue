<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Eye, EyeOff, RefreshCw, Search, ShieldAlert, Trash2 } from 'lucide-vue-next'
import {
  deleteAdminUpload,
  fetchAdminUploads,
  updateAdminViolation,
  updateAdminVisibility,
} from '../services/admin'

const state = ref({
  records: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 12,
})

const filters = reactive({
  search: '',
  violation: 'ALL',
  visibility: 'ALL',
})

const loading = ref(false)
const feedback = ref('')
let feedbackTimer

const violationButtons = [
  { value: 'ALL', label: '全部状态' },
  { value: 'VIOLATION', label: '仅违规' },
  { value: 'CLEAN', label: '仅正常' },
]

const visibilityButtons = [
  { value: 'ALL', label: '全部可见性' },
  { value: 'PUBLIC', label: '公开' },
  { value: 'PRIVATE', label: '私有' },
]

const summaryCards = computed(() => {
  const visibleRecords = state.value.records
  const flagged = visibleRecords.filter((item) => item.violation).length
  const privateCount = visibleRecords.filter((item) => !item.publicAccessible).length
  const invokeCount = visibleRecords.reduce((sum, item) => sum + item.invokeCount, 0)
  return [
    {
      label: '库内图片',
      value: state.value.totalElements.toLocaleString(),
      description: '包含所有用户上传记录',
    },
    {
      label: '本页违规',
      value: flagged.toString(),
      description: '需要人工复审的记录',
    },
    {
      label: '本页私有 / 调用',
      value: `${privateCount} · ${invokeCount}`,
      description: '私有图片数量 / 总调用次数',
    },
  ]
})

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  }
  if (bytes >= 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${bytes} B`
}

const formatDate = (isoString) => {
  if (!isoString) return '未知时间'
  const date = new Date(isoString)
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    hour12: false,
  }).format(date)
}

const setFeedback = (message) => {
  clearTimeout(feedbackTimer)
  feedback.value = message
  feedbackTimer = setTimeout(() => {
    feedback.value = ''
  }, 3200)
}

const loadUploads = async () => {
  loading.value = true
  try {
    const params = {
      page: state.value.page,
      size: state.value.size,
    }
    if (filters.search.trim()) {
      params.search = filters.search.trim()
    }
    if (filters.violation !== 'ALL') {
      params.violation = filters.violation === 'VIOLATION'
    }
    if (filters.visibility !== 'ALL') {
      params.publicAccessible = filters.visibility === 'PUBLIC'
    }
    const { data } = await fetchAdminUploads(params)
    state.value = {
      records: data.records || [],
      totalElements: data.totalElements || 0,
      totalPages: data.totalPages || 0,
      page: data.page || 0,
      size: data.size || state.value.size,
    }
  } catch (error) {
    setFeedback(error?.response?.data?.message || '加载图片列表失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  state.value.page = 0
  loadUploads()
}

const resetFilters = () => {
  filters.search = ''
  filters.violation = 'ALL'
  filters.visibility = 'ALL'
  applyFilters()
}

const changePage = (direction) => {
  const target = state.value.page + direction
  if (target < 0 || (state.value.totalPages && target >= state.value.totalPages)) return
  state.value.page = target
  loadUploads()
}

const updateRecordInState = (updatedRecord) => {
  state.value = {
    ...state.value,
    records: state.value.records.map((item) => (item.id === updatedRecord.id ? updatedRecord : item)),
  }
}

const toggleVisibility = async (record) => {
  try {
    const { data } = await updateAdminVisibility(record.id, {
      publicAccessible: !record.publicAccessible,
    })
    updateRecordInState(data)
    setFeedback(data.publicAccessible ? '已设为公开' : '已设为私有')
  } catch (error) {
    setFeedback(error?.response?.data?.message || '更新可见性失败')
  }
}

const toggleViolation = async (record) => {
  try {
    const { data } = await updateAdminViolation(record.id, {
      violation: !record.violation,
    })
    updateRecordInState(data)
    setFeedback(data.violation ? '标记为违规' : '已恢复正常')
  } catch (error) {
    setFeedback(error?.response?.data?.message || '更新违规状态失败')
  }
}

const removeRecord = async (record) => {
  const confirmed = window.confirm(`确认删除图片「${record.fileName}」吗？该操作不可撤销。`)
  if (!confirmed) return
  try {
    await deleteAdminUpload(record.id)
    setFeedback('图片已删除')
    const remaining = state.value.records.filter((item) => item.id !== record.id)
    state.value = {
      ...state.value,
      records: remaining,
      totalElements: Math.max(0, state.value.totalElements - 1),
    }
    if (!remaining.length && state.value.page > 0) {
      state.value.page -= 1
    }
    loadUploads()
  } catch (error) {
    setFeedback(error?.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  loadUploads()
})
</script>

<template>
  <section class="space-y-8">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">moderation</p>
      <div class="mt-3 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="text-4xl font-semibold text-gradient">媒体管理控制台</h1>
          <p class="text-white/60 mt-2">
            统筹所有用户的上传记录，支持检索、批次巡检与敏感内容标记，保障图床质量。
          </p>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-5 py-2 text-sm text-white/80"
          :disabled="loading"
          @click="loadUploads"
        >
          <RefreshCw class="h-4 w-4" />
          刷新数据
        </button>
      </div>
    </header>

    <div class="grid gap-4 md:grid-cols-3">
      <article
        v-for="card in summaryCards"
        :key="card.label"
        class="glass-panel rounded-3xl border border-white/10 p-5"
      >
        <p class="text-xs uppercase tracking-[0.4em] text-white/50">{{ card.label }}</p>
        <p class="mt-3 text-3xl font-semibold">{{ card.value }}</p>
        <p class="mt-2 text-sm text-white/60">{{ card.description }}</p>
      </article>
    </div>

    <div class="glass-panel rounded-3xl border border-white/10 p-6 space-y-4">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center">
        <label class="relative flex-1">
          <Search class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-white/40" />
          <input
            v-model="filters.search"
            type="text"
            placeholder="搜索文件名、链接或上传人"
            class="w-full rounded-2xl border border-white/10 bg-white/5 py-3 pl-12 pr-4 text-sm text-white/90 placeholder:text-white/40 focus:border-brand-primary focus:outline-none"
            @keyup.enter="applyFilters"
          />
        </label>
        <div class="flex gap-3">
          <button
            type="button"
            class="rounded-2xl border border-white/10 px-4 py-2 text-sm text-white/80"
            @click="applyFilters"
          >
            应用筛选
          </button>
          <button
            type="button"
            class="rounded-2xl border border-transparent bg-white/10 px-4 py-2 text-sm text-white/70 hover:bg-white/20"
            @click="resetFilters"
          >
            重置
          </button>
        </div>
      </div>
      <div class="flex flex-col gap-3 md:flex-row">
        <div class="flex flex-wrap gap-2">
          <button
            v-for="button in violationButtons"
            :key="button.value"
            type="button"
            class="rounded-full px-4 py-1.5 text-xs font-medium transition"
            :class="
              filters.violation === button.value
                ? 'bg-brand-primary text-surface-panel'
                : 'bg-white/5 text-white/60 hover:bg-white/10'
            "
            @click="() => {
              filters.violation = button.value
              applyFilters()
            }"
          >
            {{ button.label }}
          </button>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="button in visibilityButtons"
            :key="button.value"
            type="button"
            class="rounded-full px-4 py-1.5 text-xs font-medium transition"
            :class="
              filters.visibility === button.value
                ? 'bg-brand-accent text-surface-panel'
                : 'bg-white/5 text-white/60 hover:bg-white/10'
            "
            @click="() => {
              filters.visibility = button.value
              applyFilters()
            }"
          >
            {{ button.label }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="feedback" class="glass-panel rounded-2xl border border-brand-primary/30 bg-brand-primary/10 px-4 py-3 text-sm text-brand-primary">
      {{ feedback }}
    </div>

    <div class="relative">
      <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center rounded-3xl bg-surface-panel/70 backdrop-blur">
        <div class="flex items-center gap-3 text-white/70">
          <RefreshCw class="h-5 w-5 animate-spin" />
          数据加载中...
        </div>
      </div>
      <div
        v-if="state.records.length"
        class="grid gap-5" :class="state.records.length > 1 ? 'lg:grid-cols-2' : 'lg:grid-cols-1'"
      >
        <article
          v-for="record in state.records"
          :key="record.id"
          class="glass-panel relative rounded-3xl border border-white/10 p-5"
        >
          <div class="flex flex-col gap-4 md:flex-row">
            <div class="md:w-40">
              <div class="aspect-video overflow-hidden rounded-2xl bg-black/30">
                <img
                  v-if="record.publicUrl"
                  :src="record.publicUrl"
                  alt="preview"
                  class="h-full w-full object-cover"
                />
              </div>
            </div>
            <div class="flex-1 space-y-3">
              <div class="flex flex-wrap items-center gap-3">
                <p class="text-lg font-semibold text-white/90">{{ record.fileName }}</p>
                <span class="rounded-full border border-white/10 px-3 py-0.5 text-xs text-white/60">#{{ record.id }}</span>
                <span
                  class="rounded-full px-3 py-0.5 text-xs"
                  :class="record.publicAccessible ? 'bg-emerald-300/10 text-emerald-200' : 'bg-white/10 text-white/60'"
                >
                  {{ record.publicAccessible ? '公开' : '私有' }}
                </span>
                <span
                  class="rounded-full px-3 py-0.5 text-xs"
                  :class="record.violation ? 'bg-rose-400/10 text-rose-200' : 'bg-emerald-300/10 text-emerald-200'"
                >
                  {{ record.violation ? '违规待处置' : '内容安全' }}
                </span>
              </div>
              <p class="text-sm text-white/60">
                {{ record.uploaderDisplayName || '未命名用户' }} · {{ record.uploaderEmail || '无邮箱' }} · IP {{ record.uploaderIp || '未知' }}
              </p>
              <div class="text-xs text-white/50 break-all">
                {{ record.publicUrl }}
              </div>
              <div class="grid gap-2 text-sm text-white/70 sm:grid-cols-2">
                <p>体积：{{ formatSize(record.size) }}</p>
                <p>类型：{{ record.contentType || 'unknown' }}</p>
                <p>调用：{{ record.invokeCount }} 次</p>
                <p>点赞：{{ record.likeCount }}</p>
                <p>上传：{{ formatDate(record.uploadedAt) }}</p>
                <p>存储：{{ record.storageProvider }} · {{ record.storageMode }}</p>
              </div>
              <div class="flex flex-wrap gap-3 pt-2">
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-sm text-white/80"
                  @click="toggleVisibility(record)"
                >
                  <component :is="record.publicAccessible ? EyeOff : Eye" class="h-4 w-4" />
                  {{ record.publicAccessible ? '设为私有' : '设为公开' }}
                </button>
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-sm"
                  :class="record.violation ? 'text-rose-200' : 'text-white/80'"
                  @click="toggleViolation(record)"
                >
                  <ShieldAlert class="h-4 w-4" />
                  {{ record.violation ? '撤销违规' : '标记违规' }}
                </button>
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-sm text-rose-200"
                  @click="removeRecord(record)"
                >
                  <Trash2 class="h-4 w-4" />
                  删除
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="glass-panel rounded-3xl border border-dashed border-white/15 p-10 text-center text-white/60">
        暂无符合筛选条件的图片，尝试调整筛选条件或稍后再试。
      </div>
    </div>

    <div class="flex items-center justify-between rounded-full border border-white/10 bg-white/5 px-5 py-2 text-sm text-white/70">
      <span>第 {{ state.page + 1 }} / {{ Math.max(1, state.totalPages || 1) }} 页 · 共 {{ state.totalElements }} 条</span>
      <div class="flex gap-2">
        <button
          type="button"
          class="rounded-full border border-white/10 px-4 py-1 text-white/70 disabled:opacity-40"
          :disabled="state.page === 0"
          @click="changePage(-1)"
        >
          上一页
        </button>
        <button
          type="button"
          class="rounded-full border border-white/10 px-4 py-1 text-white/70 disabled:opacity-40"
          :disabled="state.totalPages ? state.page >= state.totalPages - 1 : false"
          @click="changePage(1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
