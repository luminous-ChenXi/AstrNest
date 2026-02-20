<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { fetchOverview } from '../../services/user'
import { uploadFiles } from '../../services/upload'
import UploadResultModal from '../../components/user/UploadResultModal.vue'

const overview = ref({
  totalUploads: 0,
  todayUploads: 0,
  storageBytes: 0,
  totalUploadLimit: null,
  totalRemaining: -1,
  storageQuotaMb: null,
  storageRemainingBytes: -1,
  latestUploads: [],
})
const loadingOverview = ref(true)
const uploadBusy = ref(false)
const uploadError = ref('')
const successMessage = ref('')
const isDragging = ref(false)
const fileInput = ref(null)
const resultModalOpen = ref(false)
const resultItems = ref([])
const pasteHint = ref('支持 Ctrl + V 直接粘贴图片或短视频')
const MB = 1024 * 1024

const formatDate = (value) => (value ? dayjs(value).format('YYYY/MM/DD HH:mm') : '-')

const aiBadgeMeta = (decision) => {
  switch (decision) {
    case 'BLOCK':
      return { text: 'AI 拦截', type: 'danger' }
    case 'REVIEW':
      return { text: 'AI 待复核', type: 'warning' }
    case 'PASS':
      return { text: 'AI 放行', type: 'success' }
    default:
      return { text: 'AI 未检测', type: 'info' }
  }
}

const aiBadgeForItem = (item) => {
  const decision = item?.aiReview?.decision
  if (!decision) return null
  const meta = aiBadgeMeta(decision)
  return { ...meta, decision }
}

const quotaCards = computed(() => {
  const cards = []
  const summary = overview.value
  const totalLimit = summary.totalUploadLimit
  if (totalLimit && totalLimit > 0) {
    const remaining = typeof summary.totalRemaining === 'number' ? summary.totalRemaining : -1
    const used = remaining >= 0 ? Math.max(totalLimit - remaining, 0) : summary.totalUploads
    const progress = Math.min(Math.round((used / totalLimit) * 100), 100)
    cards.push({
      key: 'total',
      label: '总数量上限',
      badge: remaining === 0 ? '数量已达上限' : remaining < 0 ? '不限' : `${remaining} 张剩余`,
      value: `${used}/${totalLimit} 张`,
      progress,
      danger: remaining === 0,
      hint: remaining === 0 ? '上传数量已达上限，请联系管理员提升配额' : '如需更高额度，可向管理员申请',
    })
  }
  const quotaMb = summary.storageQuotaMb
  if (quotaMb && quotaMb > 0) {
    const totalBytes = quotaMb * MB
    const remainingBytes = typeof summary.storageRemainingBytes === 'number' ? summary.storageRemainingBytes : totalBytes - summary.storageBytes
    const usedBytes = Math.min(totalBytes, Math.max(totalBytes - Math.max(remainingBytes, 0), summary.storageBytes))
    const progress = Math.min(Math.round((usedBytes / totalBytes) * 100), 100)
    cards.push({
      key: 'storage',
      label: '存储空间上限',
      badge: remainingBytes <= 0 ? '空间已满' : `${prettySize(Math.max(remainingBytes, 0))} 可用`,
      value: `${prettySize(usedBytes)} / ${quotaMb} MB`,
      progress,
      danger: remainingBytes <= 0,
      hint: remainingBytes <= 0 ? '请删除部分历史图片或升级空间配额' : '空间使用实时同步，可随时调整',
    })
  }
  return cards
})

const quotaAlerts = computed(() => quotaCards.value.filter((card) => card.danger).map((card) => card.hint))

const remindPaste = () => {
  pasteHint.value = '聚焦完成，请直接使用 Ctrl + V 上传'
}

const loadOverview = async () => {
  loadingOverview.value = true
  try {
    const { data } = await fetchOverview()
    overview.value = data
  } catch (error) {
    console.error('加载概览失败', error)
  } finally {
    loadingOverview.value = false
  }
}

const prettySize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / 1024 ** index).toFixed(1)} ${units[index]}`
}

const handleFiles = async (files) => {
  if (!files?.length) return
  uploadBusy.value = true
  uploadError.value = ''
  successMessage.value = ''
  try {
    const data = await uploadFiles(Array.from(files))
    resultItems.value = data || []
    resultModalOpen.value = true
    successMessage.value = `上传完成（共 ${data?.length || files.length} 个文件）`
    ElMessage.success(successMessage.value)
    await loadOverview()
  } catch (error) {
    uploadError.value = error.response?.data?.message || '上传失败，请重试'
    ElMessage.error(uploadError.value)
  } finally {
    uploadBusy.value = false
  }
}

const onDrop = (event) => {
  event.preventDefault()
  isDragging.value = false
  handleFiles(event.dataTransfer.files)
}

const onDragOver = (event) => {
  event.preventDefault()
  isDragging.value = true
}

const onDragLeave = (event) => {
  if (event.target === event.currentTarget) {
    isDragging.value = false
  }
}

const openPicker = () => fileInput.value?.click()

const onFileChange = (event) => {
  handleFiles(event.target.files)
  event.target.value = ''
}

const onPaste = (event) => {
  const files = event.clipboardData?.files
  if (files?.length) {
    handleFiles(files)
  }
}

const closeResultModal = () => {
  resultModalOpen.value = false
}

onMounted(() => {
  loadOverview()
  window.addEventListener('paste', onPaste)
})

onBeforeUnmount(() => {
  window.removeEventListener('paste', onPaste)
})
</script>

<template>
  <div class="space-y-8">
    <section class="grid gap-4 md:grid-cols-3">
      <div
        v-for="card in [
          { label: '累计上传', value: overview.totalUploads },
          { label: '今日上传', value: overview.todayUploads },
          { label: '存储占用', value: prettySize(overview.storageBytes) },
        ]"
        :key="card.label"
        class="glass-panel rounded-3xl border border-body bg-surface-overlay p-5 shadow-card"
      >
        <p class="text-xs uppercase tracking-[0.35em] text-body-soft">{{ card.label }}</p>
        <p class="mt-2 text-3xl font-semibold text-gradient">{{ card.value }}</p>
      </div>
    </section>

    <section v-if="quotaCards.length" class="glass-panel rounded-[32px] border border-body bg-surface-strong p-6 shadow-card">
      <div class="grid gap-4 md:grid-cols-2">
        <article
          v-for="card in quotaCards"
          :key="card.key"
          class="quota-card"
          :class="card.danger ? 'quota-card--danger' : ''"
        >
          <div class="flex items-center justify-between text-sm">
            <p class="font-semibold">{{ card.label }}</p>
            <span class="badge">{{ card.badge }}</span>
          </div>
          <p class="mt-3 text-2xl font-semibold">{{ card.value }}</p>
          <div class="quota-track">
            <div class="quota-progress" :style="{ width: card.progress + '%' }" />
          </div>
          <p class="text-sm text-body-soft">{{ card.hint }}</p>
        </article>
      </div>
      <div v-for="alert in quotaAlerts" :key="alert" class="quota-alert">
        {{ alert }}
      </div>
    </section>

    <section class="glass-panel space-y-6 rounded-[32px] border border-body bg-surface-overlay p-6">
      <div class="flex flex-wrap items-center gap-3">
        <div>
          <h3 class="text-xl font-semibold">上传媒体</h3>
          <p class="text-sm text-body-soft">拖拽 / 点击 / Ctrl+V </p>
        </div>
        <span class="chip-soft px-3 py-1 text-xs">审查：自动 + 人工信号</span>
      </div>
      <div
        class="dashboard-dropzone flex flex-col items-center justify-center gap-4 rounded-[32px] border-2 border-dashed px-6 py-12 text-center transition"
        :class="{ 'dashboard-dropzone--active': isDragging }"
        @drop="onDrop"
        @dragover="onDragOver"
        @dragleave="onDragLeave"
      >
        <input ref="fileInput" type="file" class="hidden" multiple accept="image/*,video/*" @change="onFileChange" />
        <p class="text-lg font-semibold">将图片拖入此处或点击选择</p>
        <p class="text-sm text-body-soft">支持 JPG / PNG / GIF / WEBP 以及 MP4 / WEBM 短视频，单次最多 30 个文件</p>
        <div class="flex flex-wrap justify-center gap-3">
          <button
            type="button"
            class="rounded-full bg-gradient-to-r from-brand-primary to-brand-accent px-6 py-2 text-sm font-semibold shadow-[0_12px_40px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5 disabled:opacity-60"
            :disabled="uploadBusy"
            @click="openPicker"
          >
            {{ uploadBusy ? '上传中...' : '选择文件' }}
          </button>
          <button
            type="button"
            class="rounded-full border border-body px-6 py-2 text-sm font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
            @click="remindPaste"
          >
            监听剪贴板
          </button>
        </div>
        <p class="text-xs text-body-soft">{{ pasteHint }}</p>
        <el-alert
          v-if="uploadError"
          :title="uploadError"
          type="error"
          show-icon
          :closable="false"
          class="w-full max-w-2xl"
        />
        <el-alert
          v-if="successMessage"
          :title="successMessage"
          type="success"
          show-icon
          :closable="false"
          class="w-full max-w-2xl"
        />
      </div>
    </section>

    <section class="glass-panel rounded-[32px] border border-body bg-surface-overlay p-6">
      <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-xl font-semibold">最近上传</h3>
          <p class="text-sm text-body-soft">系统自动保留最近 5 条记录</p>
        </div>
        <RouterLink
          class="rounded-full border border-body px-4 py-2 text-xs font-semibold text-body-muted transition hover:border-brand-primary hover:text-body-primary"
          :to="{ name: 'user-images' }"
        >
          查看全部
        </RouterLink>
      </div>
      <div v-if="loadingOverview" class="text-sm text-body-soft">正在加载...</div>
      <div v-else-if="!overview.latestUploads?.length" class="text-sm text-body-soft">暂时没有上传记录</div>
      <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <article
          v-for="item in overview.latestUploads"
          :key="item.id"
          class="group overflow-hidden rounded-2xl border border-body bg-surface-strong shadow-card transition hover:-translate-y-1"
        >
          <div class="relative">
            <el-tag
              v-if="aiBadgeForItem(item)"
              :type="aiBadgeForItem(item).type"
              effect="dark"
              size="small"
              class="absolute left-3 top-3 z-10"
            >
              {{ aiBadgeForItem(item).text }}
            </el-tag>
            <template v-if="item.mediaCategory === 'VIDEO'">
              <video
                class="h-48 w-full object-cover"
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
              <img :src="item.publicUrl" :alt="item.fileName" class="h-48 w-full object-cover" />
            </template>
          </div>
          <div class="space-y-1 border-t border-body p-4 text-sm">
            <p class="font-semibold">{{ item.fileName }}</p>
            <p class="text-xs text-body-soft">{{ formatDate(item.uploadedAt) }}</p>
            <p v-if="item.aiReview?.errorMessage" class="text-xs text-rose-300">{{ item.aiReview.errorMessage }}</p>
          </div>
        </article>
      </div>
    </section>
  </div>

  <UploadResultModal
    :open="resultModalOpen"
    :items="resultItems"
    title="上传成功，选择复制格式"
    @close="closeResultModal"
  />
</template>

<style scoped>
.quota-card {
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  padding: 1.25rem;
  background: var(--panel-overlay);
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--color-text-primary) 8%, transparent);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.quota-card--danger {
  border-color: color-mix(in srgb, #ef4444 50%, var(--border-strong));
  background: color-mix(in srgb, #ef4444 10%, transparent);
}

.badge {
  border-radius: 9999px;
  border: 1px solid var(--chip-border);
  padding: 0.1rem 0.75rem;
  font-size: 0.75rem;
  color: var(--chip-text);
  background: var(--chip-bg);
}

.quota-track {
  margin: 0.85rem 0;
  height: 6px;
  border-radius: 9999px;
  background: color-mix(in srgb, var(--color-text-secondary) 12%, transparent);
  overflow: hidden;
}

.quota-progress {
  height: 100%;
  border-radius: 9999px;
  background: linear-gradient(135deg, #7c3aed, #f472b6);
  transition: width 0.3s ease;
}

.quota-card--danger .quota-progress {
  background: linear-gradient(135deg, #f97316, #ef4444);
}

.quota-alert {
  margin-top: 1rem;
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, #f97316 45%, transparent);
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
  color: color-mix(in srgb, #f97316 65%, var(--color-text-primary));
  background: color-mix(in srgb, #f97316 12%, transparent);
}

.dashboard-dropzone {
  border-color: var(--border-soft);
  background: var(--color-bg-strong);
}

.dashboard-dropzone--active {
  border-color: color-mix(in srgb, var(--color-brand-primary) 70%, transparent);
  background: color-mix(in srgb, var(--color-bg-secondary) 85%, transparent);
}

.media-chip {
  background: rgba(0, 0, 0, 0.55);
  color: var(--color-on-accent);
}
</style>
