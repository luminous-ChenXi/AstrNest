<template>
  <div class="glass-panel h-full w-full p-6 text-body-primary lg:p-8">
    <div class="flex flex-col gap-2 border-b border-body pb-6 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <p class="text-xs uppercase tracking-[0.35em] text-body-muted">即时上传</p>
        <h3 class="mt-2 text-2xl font-semibold text-body-primary">AstrNest 媒体管理</h3>
        <p class="mt-2 text-sm text-body-soft">
          拖拽、选择或按 Ctrl+V 粘贴即可完成上传，支持高清图片与短视频。
        </p>
      </div>
      <div class="rounded-2xl border border-body bg-surface-overlay px-4 py-3 text-right">
        <p class="text-xs text-body-muted">Upload</p>
        <p class="text-2xl font-semibold text-gradient">{{ selectedFiles.length }}</p>
      </div>
    </div>

    <div class="mt-6 space-y-6">
      <div
        ref="dropZoneRef"
        role="button"
        tabindex="0"
        aria-label="拖拽或粘贴媒体文件"
        @focus="moduleFocused = true"
        @blur="moduleFocused = false"
        @dragenter.prevent="onDragEnter"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
        @paste.prevent="handlePaste"
        @keydown.enter.prevent="triggerPicker"
        @keydown.space.prevent="triggerPicker"
        class="flex flex-col items-center justify-center gap-4 rounded-3xl border-2 border-dashed px-6 py-10 text-center transition"
        :class="dropActive
          ? 'border-brand-primary bg-surface-overlay shadow-card'
          : 'border-body bg-surface-overlay hover:border-brand-primary/70'"
      >
        <UploadCloud class="h-12 w-12 text-brand-primary" />
        <div class="space-y-2">
          <p class="text-lg font-semibold text-body-primary">拖入文件或点击选择</p>
          <p class="text-sm text-body-soft">
            支持 JPG / PNG / GIF / WEBP 以及 MP4 / WEBM，单次最多 {{ systemStore.config?.maxFilesPerUpload || 30 }} 个文件，粘贴前请先聚焦此区域，聚焦后直接 Ctrl+V。
          </p>
        </div>
        <div class="flex flex-wrap justify-center gap-3">
          <button
            type="button"
            @click="triggerPicker"
            class="rounded-full border border-body px-5 py-2 text-sm font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
          >
            浏览文件
          </button>
          <button
            type="button"
            @click="focusPasteTarget"
            class="inline-flex items-center gap-2 rounded-full border border-body bg-surface-overlay px-5 py-2 text-sm font-semibold text-body-primary transition hover:border-brand-primary"
          >
            <ClipboardPaste class="h-4 w-4" />
            监听剪贴板
          </button>
        </div>
        <input
          ref="pickerRef"
          type="file"
          accept="image/*,video/*"
          multiple
          class="hidden"
          @change="handleFilePick"
        />
      </div>

      <div v-if="!auth.isAuthenticated" class="rounded-2xl border border-body bg-surface-overlay p-4 text-sm text-body-soft">
        <div class="flex items-start gap-3">
          <AlertCircle class="mt-0.5 h-4 w-4 text-brand-accent" />
          <p>上传接口需要登录授权，请先登录后再提交，系统会自动携带令牌并统计日志。</p>
        </div>
      </div>

      <div v-if="selectedFiles.length" class="space-y-4 rounded-3xl border border-body bg-surface-overlay p-5">
        <div class="flex items-center justify-between text-sm text-body-soft">
          <p>已选择 {{ selectedFiles.length }} 个 · {{ totalSizeLabel }}</p>
          <button type="button" class="text-body-muted transition hover:text-body-primary" @click="clearSelection">
            清空所选
          </button>
        </div>
        <ul class="space-y-3">
          <li
            v-for="(file, index) in selectedFiles"
            :key="`${file.name}-${index}`"
            class="flex items-center justify-between rounded-2xl border border-body bg-surface-body/60 px-4 py-3"
          >
            <div class="flex items-center gap-3 text-left">
              <component :is="file.type?.startsWith('video/') ? Film : FileImage" class="h-4 w-4 text-brand-primary" />
              <div>
                <p class="text-sm font-medium text-body-primary">{{ file.name || '剪贴板文件' }}</p>
                <p class="text-xs text-body-muted">{{ file.type?.startsWith('video/') ? '短视频' : '图片' }} · {{ formatBytes(file.size) }}</p>
              </div>
            </div>
            <button
              type="button"
              class="text-body-muted transition hover:text-brand-accent"
              @click="removeFile(index)"
            >
              <Trash2 class="h-4 w-4" />
            </button>
          </li>
        </ul>
        <div class="flex flex-wrap items-center gap-2 text-xs text-body-soft">
          <span>标签：</span>
          <template v-if="selectedTags.length">
            <span
              v-for="tag in selectedTags"
              :key="tag"
              class="tag-chip"
            >
              {{ tag }}
            </span>
            <button type="button" class="text-body-muted hover:text-body-primary" @click="clearSelectedTags">清空</button>
          </template>
          <span v-else>暂无标签，点击下方“添加标签”设置</span>
        </div>
        <div class="flex flex-wrap gap-3">
          <button
            type="button"
            class="inline-flex flex-1 items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent px-5 py-3 text-sm font-semibold text-white transition"
            :class="canSubmit ? 'cursor-pointer hover:translate-y-0.5' : 'cursor-not-allowed opacity-60'"
            :disabled="!canSubmit"
            @click="startUpload"
          >
            <component :is="uploading ? Loader2 : UploadCloud" class="h-4 w-4" :class="{ 'animate-spin': uploading }" />
            <span>{{ uploadButtonLabel }}</span>
          </button>
          <button
            type="button"
            class="flex items-center justify-center gap-2 rounded-2xl border border-body px-5 py-3 text-sm font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
            @click="openTagDialog"
          >
            <Tag class="h-4 w-4" />
            添加标签
          </button>
        </div>
      </div>

      <div v-if="uploading && uploadProgress.stage" class="rounded-2xl border border-body bg-surface-overlay p-4">
        <div class="flex items-center justify-between text-sm text-body-secondary mb-2">
          <span class="flex items-center gap-2">
            <Loader2 class="h-4 w-4 animate-spin" />
            {{ uploadProgressText }}
          </span>
        </div>
        <div class="h-2 rounded-full bg-surface-body overflow-hidden">
          <div
            class="h-full bg-gradient-to-r from-brand-primary to-brand-accent transition-all duration-300 ease-in-out"
            :style="{ width: uploadProgress.stage === 'extracting' ? `${(uploadProgress.current / uploadProgress.total) * 100}%` : `${uploadProgress.current}%` }"
          />
        </div>
      </div>

      <div v-if="errorMessage" class="flex items-start gap-3 rounded-2xl border border-brand-accent/30 bg-brand-accent/10 p-4 text-sm text-brand-accent">
        <AlertCircle class="mt-0.5 h-4 w-4" />
        <p>{{ errorMessage }}</p>
      </div>

      <div v-if="uploadResult.length" class="space-y-4 rounded-3xl border border-brand-primary/20 bg-brand-primary/5 p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex items-center gap-2 text-sm text-body-secondary">
            <CheckCircle2 class="h-4 w-4 text-brand-emerald" />
            <span>成功上传 {{ uploadResult.length }} 个媒体文件</span>
          </div>
          <button type="button" class="text-sm text-body-soft transition hover:text-body-primary" @click="clearResults">
            隐藏结果
          </button>
        </div>
        <ul class="space-y-3">
          <li
            v-for="item in uploadResult"
            :key="item.objectKey"
            class="rounded-2xl border border-body bg-surface-body/50 px-4 py-3"
          >
            <div class="flex flex-col gap-1 text-left">
              <p class="text-sm font-semibold text-body-primary">
                {{ item.originalFileName || item.fileName }}
                <span v-if="item.mediaCategory === 'VIDEO'" class="ml-2 rounded-full border border-body px-2 py-0.5 text-xs text-body-soft">短视频</span>
              </p>
              <p class="text-xs text-body-muted">{{ item.fileName }} · {{ formatBytes(item.size) }}</p>
            </div>
            <div v-if="item.tags?.length" class="mt-2 flex flex-wrap gap-2 text-xs text-body-soft">
              <span
                v-for="tag in item.tags"
                :key="tag.id || tag.name"
                class="tag-chip"
              >
                {{ tag.name }}
              </span>
            </div>
            <div class="mt-3 flex flex-wrap gap-3 text-sm">
              <a
                :href="resolvePublicUrl(item)"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center gap-2 rounded-full border border-body px-4 py-2 text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
              >
                查看链接
              </a>
              <button
                type="button"
                class="inline-flex items-center gap-2 rounded-full bg-surface-overlay px-4 py-2 text-body-primary transition hover:bg-surface-body"
                @click="copyLink(resolvePublicUrl(item), '访问链接已复制到剪贴板')"
              >
                复制 URL
              </button>
              <button
                v-if="item.mediaCategory === 'VIDEO' && resolveEmbedUrl(item)"
                type="button"
                class="inline-flex items-center gap-2 rounded-full bg-surface-overlay px-4 py-2 text-body-primary transition hover:bg-surface-body"
                @click="copyLink(buildEmbedSnippet(item), '嵌入代码已复制到剪贴板')"
              >
                复制嵌入代码
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
  <ChenxiTagDialog v-model:visible="tagDialogOpen" v-model:tags="selectedTags" />
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { AlertCircle, CheckCircle2, ClipboardPaste, FileImage, Film, Loader2, Tag, Trash2, UploadCloud } from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'
import { useSystemStore } from '../../stores/system'
import { uploadFiles } from '../../services/upload'
import ChenxiTagDialog from '../common/ChenxiTagDialog.vue'

const auth = useAuthStore()
const systemStore = useSystemStore()
const assetBase = (import.meta.env.VITE_PUBLIC_ASSET_BASE || '').replace(/\/$/, '')
const dropZoneRef = ref(null)
const pickerRef = ref(null)
const selectedFiles = ref([])
const selectedTags = ref([])
const tagDialogOpen = ref(false)
const uploadResult = ref([])
const uploading = ref(false)
const dropActive = ref(false)
const moduleFocused = ref(false)
const errorMessage = ref('')
const forbiddenExtensions = ['.sh', '.bat', '.cmd', '.exe', '.msi', '.js', '.mjs', '.ts', '.php', '.py', '.rb', '.yml', '.yaml', '.json', '.conf', '.ini', '.log', '.txt']
const origin = typeof window !== 'undefined' ? window.location.origin.replace(/\/$/, '') : ''

const totalSizeLabel = computed(() => formatBytes(selectedFiles.value.reduce((sum, file) => sum + (file?.size || 0), 0)))
const canSubmit = computed(() => selectedFiles.value.length > 0 && auth.isAuthenticated && !uploading.value)
const uploadButtonLabel = computed(() => {
  if (!auth.isAuthenticated) return '请先登录'
  if (uploading.value) return '正在上传'
  if (!selectedFiles.value.length) return '选择文件以开始'
  return '开始上传'
})

function triggerPicker() {
  pickerRef.value?.click()
}

function focusPasteTarget() {
  dropZoneRef.value?.focus()
  moduleFocused.value = true
}

function handleFilePick(event) {
  const files = Array.from(event.target?.files || [])
  addFiles(files)
  if (event.target) {
    event.target.value = ''
  }
}

function onDragEnter() {
  dropActive.value = true
}

function onDragOver() {
  dropActive.value = true
}

function onDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    dropActive.value = false
  }
}

function onDrop(event) {
  dropActive.value = false
  const files = Array.from(event.dataTransfer?.files || [])
  addFiles(files)
}

function handlePaste(event) {
  if (!moduleFocused.value) {
    return
  }
  const files = Array.from(event.clipboardData?.files || [])
  addFiles(files)
}

const isDangerousExtension = (file) => {
  if (!file?.name) return false
  const lower = file.name.toLowerCase()
  return forbiddenExtensions.some((ext) => lower.endsWith(ext))
}

const isAllowedType = (file) => {
  if (!file) return false
  if (file.type?.startsWith('image/') || file.type?.startsWith('video/')) {
    return true
  }
  const name = file.name?.toLowerCase() || ''
  return ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.mp4', '.mov', '.webm', '.ogv'].some((ext) => name.endsWith(ext))
}

const maxFilesPerUpload = computed(() => systemStore.config?.maxFilesPerUpload || 30)

function addFiles(files) {
  if (!files.length) {
    return
  }
  const safeFiles = files.filter((file) => isAllowedType(file) && !isDangerousExtension(file))
  if (!safeFiles.length) {
    errorMessage.value = '仅支持图片或短视频，禁止上传脚本/配置文件'
    return
  }
  const combined = [...selectedFiles.value, ...safeFiles]
  const limit = maxFilesPerUpload.value
  if (combined.length > limit) {
    errorMessage.value = `单次最多上传 ${limit} 个文件，已自动截取前 ${limit} 个`
    selectedFiles.value = combined.slice(0, limit)
  } else {
    selectedFiles.value = combined
    errorMessage.value = ''
  }
}

function removeFile(index) {
  selectedFiles.value.splice(index, 1)
}

function clearSelection() {
  selectedFiles.value = []
}

function clearSelectedTags() {
  selectedTags.value = []
}

function clearResults() {
  uploadResult.value = []
}

function openTagDialog() {
  tagDialogOpen.value = true
}

function formatBytes(bytes) {
  if (!bytes || Number.isNaN(bytes)) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }
  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
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

const resolveEmbedUrl = (item) => {
  if (!item) return null
  if (item.embedUrl) return item.embedUrl
  if (item.mediaUuid) return `${origin}/embed/video/${item.mediaUuid}`
  return null
}

const buildEmbedSnippet = (item) => {
  const embedUrl = resolveEmbedUrl(item)
  if (!embedUrl) return ''
  return `<iframe src="${embedUrl}" scrolling="no" frameborder="0" allowfullscreen style="width:100%;height:420px;border-radius:16px;"></iframe>`
}

const uploadProgress = ref({ stage: '', current: 0, total: 0 })
const uploadProgressText = computed(() => {
  if (uploadProgress.value.stage === 'extracting') {
    return `正在提取封面 ${uploadProgress.value.current}/${uploadProgress.value.total}`
  }
  if (uploadProgress.value.stage === 'uploading') {
    return `正在上传 ${uploadProgress.value.current}%`
  }
  return '正在处理'
})

async function startUpload() {
  if (!auth.isAuthenticated) {
    errorMessage.value = '请先登录后再上传'
    return
  }
  if (!selectedFiles.value.length) {
    errorMessage.value = '请选择至少一个文件'
    return
  }
  uploading.value = true
  uploadProgress.value = { stage: '', current: 0, total: 0 }
  errorMessage.value = ''
  uploadResult.value = []
  try {
    const result = await uploadFiles(
      selectedFiles.value,
      selectedTags.value,
      (progress) => {
        uploadProgress.value = progress
      }
    )
    uploadResult.value = result
    selectedFiles.value = []
    ElMessage.success('上传成功')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error.message || '上传失败'
  } finally {
    uploading.value = false
    uploadProgress.value = { stage: '', current: 0, total: 0 }
  }
}

async function copyLink(link, successMessage = '链接已复制到剪贴板') {
  if (!link) {
    return
  }
  try {
    if (navigator?.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(link)
    } else {
      legacyCopyLink(link)
    }
    ElMessage.success(successMessage)
  } catch (error) {
    console.error('复制失败', error)
    ElMessage.error('复制失败，请手动复制')
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
    throw new Error('复制失败')
  }
}

onMounted(async () => {
  if (!systemStore.config) {
    await systemStore.fetchSystemConfig()
  }
})
</script>
