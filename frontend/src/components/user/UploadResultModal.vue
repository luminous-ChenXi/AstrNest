<script setup>
import { computed, reactive, watch } from 'vue'
import { Link2, Image, Code, FileCode, Video, X, Copy, Check, Shield, AlertTriangle, AlertCircle, Heart, Lock, Unlock } from 'lucide-vue-next'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '复制链接' },
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['close'])

const state = reactive({})

watch(
  () => props.items,
  () => {
    Object.keys(state).forEach((key) => delete state[key])
  }
)

const buildResponsivePlayerSnippet = (item) => {
  const videoUrl = item?.publicUrl
  if (!videoUrl) return ''
  const posterSource = item?.thumbnailUrl
  const posterAttr = posterSource ? ` poster="${posterSource}"` : ''
  const mime = item?.contentType || 'video/mp4'
  const fallbackId = `astrnest-player-${Date.now()}`
  const rawId = (item?.objectKey || item?.id || fallbackId).toString()
  const safeId = rawId.replace(/[^a-zA-Z0-9_-]/g, '')
  const playerId = safeId ? `astrnest-player-${safeId}` : fallbackId
  return `<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/plyr@3.7.8/dist/plyr.css" />
  <section class="astrnest-player-card">
    <h2>${playerId}</h2>
    <div class="astrnest-responsive-player">
      <video
        id="${playerId}"
        class="plyr-player"
        controls
        playsinline
        preload="metadata"${posterAttr}
      >
        <source src="${videoUrl}" type="${mime}" />
        您的浏览器暂不支持 HTML5 视频播放。
      </video>
    </div>
  </section>
<style>
  .astrnest-player-shell {
    font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
    padding: 32px 16px;
  }
  .astrnest-player-header {
    max-width: 960px;
    margin: 0 auto 24px;
  }
  .astrnest-player-header h1 {
    margin: 0 0 12px;
    font-size: clamp(2rem, 5vw, 2.75rem);
    color: #0b1a39;
  }
  .astrnest-player-lead {
    margin: 0;
    color: #475569;
    line-height: 1.65;
  }
  .astrnest-player-card {
    max-width: 960px;
    margin: 0 auto;
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    padding: 28px;
    box-shadow: 0 25px 65px rgba(15, 23, 42, 0.12);
  }
  .astrnest-player-card h2 {
    margin: 0 0 14px;
    font-size: 1rem;
    letter-spacing: 0.42em;
    text-transform: uppercase;
    color: #1e2a4a;
  }
  .astrnest-responsive-player {
    border-radius: 18px;
    border: 1px solid #cbd5f5;
    background: #000;
    overflow: hidden;
  }
  .astrnest-responsive-player .plyr {
    border-radius: inherit;
    background: #000;
  }
  .astrnest-responsive-player video {
    width: 100%;
    height: auto;
    display: block;
    background: #000;
  }
  @media (max-width: 640px) {
    .astrnest-player-card {
      padding: 22px;
      border-radius: 16px;
    }
  }
</style>
<script src="https://cdn.jsdelivr.net/npm/plyr@3.7.8/dist/plyr.polyfilled.min.js"><\/script>
<script>
  ;(function () {
    const initPlayer = () => {
      if (!window.Plyr) return
      window.astrnestPlayers = window.astrnestPlayers || {}
      if (window.astrnestPlayers['${playerId}']) return
      window.astrnestPlayers['${playerId}'] = new window.Plyr('#${playerId}', {
        clickToPlay: true,
        tooltips: { controls: true, seek: true },
        controls: [
          'play-large',
          'play',
          'progress',
          'current-time',
          'mute',
          'volume',
          'settings',
          'pip',
          'airplay',
          'fullscreen'
        ]
      })
    }
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initPlayer, { once: true })
    } else {
      initPlayer()
    }
  })()
<\/script>`
}

const aiMetaForItem = (item) => {
  const decision = item?.aiReview?.decision
  if (!decision) {
    return { text: 'AI 未检测', type: 'info', icon: Shield }
  }
  if (decision === 'BLOCK') {
    return { text: 'AI 拦截', type: 'danger', icon: AlertCircle }
  }
  if (decision === 'REVIEW') {
    return { text: 'AI 待复核', type: 'warning', icon: AlertTriangle }
  }
  if (decision === 'PASS') {
    return { text: 'AI 放行', type: 'success', icon: Check }
  }
  return { text: 'AI 未检测', type: 'info', icon: Shield }
}

const aiLabelsForItem = (item) => item?.aiReview?.labels || []

const aiErrorMessage = (item) => item?.aiReview?.errorMessage

const aiRequestId = (item) => item?.aiReview?.errorRequestId

const getFormatIcon = (key) => {
  const icons = {
    url: Link2,
    html: Code,
    markdown: FileCode,
    bbcode: FileCode,
    embed: Video,
  }
  return icons[key] || Link2
}

const formatterEntries = (item) => {
  if (!item) return []
  const url = item.publicUrl
  const fileName = item.fileName || item.objectKey
  const entries = []
  if (url) {
    entries.push({ key: 'url', label: '直接链接', value: url })
  }
  if (item.mediaCategory === 'VIDEO') {
    const playerSnippet = buildResponsivePlayerSnippet(item)
    if (playerSnippet) {
      entries.push({
        key: 'embed',
        label: '响应式播放器',
        value: playerSnippet,
      })
    }
    if (url) {
      entries.push({
        key: 'markdown',
        label: 'Markdown',
        value: `[视频 ${fileName}](${url})`,
      })
    }
  } else if (url) {
    entries.push(
      { key: 'html', label: 'HTML', value: `<img src="${url}" alt="${fileName}">` },
      { key: 'markdown', label: 'Markdown', value: `![${fileName}](${url})` },
      { key: 'bbcode', label: 'BBCode', value: `[img]${url}[/img]` }
    )
  }
  return entries
}

const isVideo = (item) => item?.mediaCategory === 'VIDEO'

const copyText = async (text, key) => {
  try {
    await navigator.clipboard.writeText(text)
    state[key] = '已复制'
    setTimeout(() => {
      if (state[key] === '已复制') {
        delete state[key]
      }
    }, 2000)
  } catch (error) {
    state[key] = '复制失败'
    console.error('copy failed', error)
  }
}

const close = () => emit('close')

const getAiTagClass = (type) => {
  const classes = {
    info: 'ai-tag-info',
    success: 'ai-tag-success',
    warning: 'ai-tag-warning',
    danger: 'ai-tag-danger',
  }
  return classes[type] || classes.info
}
</script>

<template>
  <teleport to="body">
    <transition name="modal">
      <div v-if="open" class="modal-overlay" @click.self="close">
        <div class="modal-container">
          <!-- Header -->
          <header class="modal-header">
            <div class="header-content">
              <div class="header-badge">
                <Link2 class="badge-icon" />
                <span>分享</span>
              </div>
              <h3 class="modal-title">{{ title }}</h3>
            </div>
            <button class="close-btn" @click="close">
              <X class="close-icon" />
            </button>
          </header>

          <!-- Content -->
          <div class="modal-body">
            <article v-for="item in items" :key="item.objectKey || item.id" class="content-layout">
              <!-- Left: Media Preview -->
              <div class="media-section">
                <div class="media-preview">
                  <template v-if="isVideo(item)">
                    <video :poster="item.thumbnailUrl || undefined" controls class="media-video" :src="item.publicUrl"></video>
                  </template>
                  <template v-else>
                    <img v-if="item.publicUrl" :src="item.publicUrl" :alt="item.fileName" class="media-image" />
                  </template>
                </div>
                <div class="media-info">
                  <p class="media-name">{{ item.fileName || item.objectKey }}</p>
                  <span class="media-type">{{ isVideo(item) ? '短视频' : '图片' }}</span>
                </div>
              </div>

              <!-- Right: Details -->
              <div class="details-section">
                <!-- AI Review Card -->
                <div class="ai-review-card">
                  <div class="ai-review-header">
                    <div class="ai-review-title">
                      <component :is="aiMetaForItem(item).icon" class="ai-icon" />
                      <span>AI 审核</span>
                    </div>
                    <span class="ai-tag" :class="getAiTagClass(aiMetaForItem(item).type)">
                      {{ aiMetaForItem(item).text }}
                    </span>
                  </div>
                  <p v-if="aiErrorMessage(item)" class="ai-error">{{ aiErrorMessage(item) }}</p>
                  <p v-if="aiRequestId(item)" class="ai-request-id">RequestId: {{ aiRequestId(item) }}</p>
                  <div v-if="aiLabelsForItem(item).length" class="ai-labels">
                    <span
                      v-for="label in aiLabelsForItem(item)"
                      :key="`${item.objectKey || item.id}-${label.name}`"
                      class="ai-label"
                    >
                      {{ label.name }} · {{ label.confidence }}%
                    </span>
                  </div>
                  <div v-if="item.aiReview?.decision === 'BLOCK'" class="ai-alert ai-alert-danger">
                    <AlertCircle class="alert-icon" />
                    <span>该文件已被 AI 拦截，访问链接已替换为占位图</span>
                  </div>
                  <div v-else-if="item.aiReview?.decision === 'REVIEW'" class="ai-alert ai-alert-warning">
                    <AlertTriangle class="alert-icon" />
                    <span>AI 建议人工复核，管理员处理前暂不公开</span>
                  </div>
                </div>

                <!-- Format Entries -->
                <div v-for="entry in formatterEntries(item)" :key="`${item.objectKey || item.id}-${entry.key}`" class="format-item">
                  <div class="format-header">
                    <div class="format-label">
                      <component :is="getFormatIcon(entry.key)" class="format-icon" />
                      <span>{{ entry.label }}</span>
                    </div>
                    <span v-if="state[`${item.objectKey || item.id}-${entry.key}`]" class="copy-status">
                      <Check class="status-icon" />
                      {{ state[`${item.objectKey || item.id}-${entry.key}`] }}
                    </span>
                  </div>
                  <div class="format-input-group">
                    <textarea
                      :value="entry.value"
                      class="format-textarea"
                      readonly
                      :rows="entry.value?.length > 120 ? 3 : 1"
                    ></textarea>
                    <button
                      class="copy-btn"
                      :class="{ 'copied': state[`${item.objectKey || item.id}-${entry.key}`] === '已复制' }"
                      @click="copyText(entry.value, `${item.objectKey || item.id}-${entry.key}`)"
                    >
                      <Copy class="copy-icon" />
                      <span>复制</span>
                    </button>
                  </div>
                </div>

                <!-- Extra slot for action buttons -->
                <slot name="extra" :item="item"></slot>
              </div>
            </article>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
/* Modal Overlay - Full screen dark background */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* Modal Container - Auto height based on content */
.modal-container {
  width: 100%;
  max-width: 900px;
  max-height: calc(100vh - 48px);
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.35), 0 10px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modal-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #ff6b9d;
}

.badge-icon {
  width: 14px;
  height: 14px;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  transform: rotate(90deg);
}

.close-icon {
  width: 18px;
  height: 18px;
  color: #6b7280;
}

/* Modal Body - No scrollbar, auto height */
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.modal-body::-webkit-scrollbar {
  display: none;
}

/* Content Layout */
.content-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

/* Media Section */
.media-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.media-preview {
  aspect-ratio: 4/3;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.media-image,
.media-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.media-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.media-type {
  font-size: 0.75rem;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Details Section */
.details-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* AI Review Card */
.ai-review-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  padding: 16px;
}

.ai-review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ai-review-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
}

.ai-icon {
  width: 16px;
  height: 16px;
  color: #64748b;
}

.ai-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}

.ai-tag-info {
  background: #e0e7ff;
  color: #4f46e5;
}

.ai-tag-success {
  background: #d1fae5;
  color: #059669;
}

.ai-tag-warning {
  background: #fef3c7;
  color: #d97706;
}

.ai-tag-danger {
  background: #fee2e2;
  color: #dc2626;
}

.ai-error {
  font-size: 0.8125rem;
  color: #dc2626;
  margin: 0 0 8px 0;
}

.ai-request-id {
  font-size: 0.6875rem;
  color: #94a3b8;
  font-family: 'SF Mono', monospace;
  margin: 0 0 8px 0;
}

.ai-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ai-label {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  font-size: 0.6875rem;
  color: #64748b;
}

.ai-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  margin-top: 12px;
  font-size: 0.8125rem;
}

.ai-alert-danger {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.ai-alert-warning {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.alert-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* Format Items */
.format-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.format-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.format-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #6b7280;
}

.format-icon {
  width: 14px;
  height: 14px;
  color: #9ca3af;
}

.copy-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: #10b981;
  font-weight: 500;
}

.status-icon {
  width: 14px;
  height: 14px;
}

.format-input-group {
  display: flex;
  gap: 8px;
}

.format-textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
  font-size: 0.8125rem;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: #374151;
  resize: none;
  outline: none;
  transition: all 0.2s ease;
  line-height: 1.5;
}

.format-textarea:focus {
  border-color: #ff6b9d;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  border: none;
  border-radius: 12px;
  color: #ffffff;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.copy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.copy-btn.copied {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.copy-icon {
  width: 14px;
  height: 14px;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}

/* Responsive */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 16px;
    align-items: flex-end;
  }

  .modal-container {
    max-height: calc(100vh - 32px);
    border-radius: 20px 20px 0 0;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .media-section {
    order: -1;
  }

  .media-preview {
    aspect-ratio: 16/9;
  }

  .modal-header {
    padding: 16px 20px;
  }

  .modal-body {
    padding: 16px;
  }

  .format-input-group {
    flex-direction: column;
  }

  .copy-btn {
    justify-content: center;
  }
}

/* Dark Mode Support */
@media (prefers-color-scheme: dark) {
  .modal-container {
    background: #1a1a2e;
  }

  .modal-header {
    background: linear-gradient(135deg, #252542 0%, #1a1a2e 100%);
    border-color: rgba(255, 255, 255, 0.06);
  }

  .modal-title {
    color: #f1f5f9;
  }

  .close-btn {
    background: rgba(255, 255, 255, 0.1);
  }

  .close-btn:hover {
    background: rgba(255, 255, 255, 0.15);
  }

  .close-icon {
    color: #94a3b8;
  }

  .media-preview {
    background: linear-gradient(135deg, #252542 0%, #1a1a2e 100%);
    border-color: rgba(255, 255, 255, 0.06);
  }

  .media-name {
    color: #e2e8f0;
  }

  .ai-review-card {
    background: linear-gradient(135deg, #252542 0%, #1a1a2e 100%);
    border-color: rgba(255, 255, 255, 0.06);
  }

  .ai-review-title {
    color: #f1f5f9;
  }

  .ai-icon {
    color: #94a3b8;
  }

  .format-textarea {
    background: #252542;
    border-color: rgba(255, 255, 255, 0.1);
    color: #e2e8f0;
  }

  .format-textarea:focus {
    background: #1a1a2e;
  }
}
</style>
