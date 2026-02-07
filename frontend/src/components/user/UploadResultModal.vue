<script setup>
import { computed, reactive, watch } from 'vue'

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
    return { text: 'AI 未检测', type: 'info' }
  }
  if (decision === 'BLOCK') {
    return { text: 'AI 拦截', type: 'danger' }
  }
  if (decision === 'REVIEW') {
    return { text: 'AI 待复核', type: 'warning' }
  }
  if (decision === 'PASS') {
    return { text: 'AI 放行', type: 'success' }
  }
  return { text: 'AI 未检测', type: 'info' }
}

const aiLabelsForItem = (item) => item?.aiReview?.labels || []

const aiErrorMessage = (item) => item?.aiReview?.errorMessage

const aiRequestId = (item) => item?.aiReview?.errorRequestId

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
</script>

<template>
  <transition name="fade">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
      <div class="w-full max-w-3xl rounded-3xl bg-white shadow-2xl">
        <header class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <div>
            <p class="text-xs uppercase tracking-[0.4em] text-gray-400">share</p>
            <h3 class="text-xl font-semibold text-gray-900">{{ title }}</h3>
          </div>
          <button class="text-gray-400 hover:text-gray-900" @click="close">✕</button>
        </header>
        <div class="max-h-[70vh] overflow-y-auto divide-y divide-gray-100">
          <article v-for="item in items" :key="item.objectKey || item.id" class="flex flex-col gap-4 px-6 py-5 md:flex-row">
            <div class="w-full md:w-1/3">
              <div class="aspect-video overflow-hidden rounded-2xl bg-gray-100">
                <template v-if="isVideo(item)">
                  <video :poster="item.thumbnailUrl || undefined" controls class="h-full w-full object-cover" :src="item.publicUrl"></video>
                </template>
                <template v-else>
                  <img v-if="item.publicUrl" :src="item.publicUrl" :alt="item.fileName" class="h-full w-full object-cover" />
                </template>
              </div>
              <p class="mt-2 text-sm text-gray-500 truncate">{{ item.fileName || item.objectKey }}</p>
              <p class="text-xs text-gray-400">{{ isVideo(item) ? '短视频' : '图片' }}</p>
            </div>
            <div class="flex-1 space-y-3">
              <div class="space-y-2 rounded-2xl border border-gray-200 bg-white/80 p-4 text-xs text-slate-700">
                <div class="flex items-center justify-between text-sm font-semibold">
                  <span>AI 审核</span>
                  <el-tag v-if="item.aiReview" size="small" :type="aiMetaForItem(item).type">
                    {{ aiMetaForItem(item).text }}
                  </el-tag>
                  <el-tag v-else size="small" type="info">AI 未检测</el-tag>
                </div>
                <p v-if="aiErrorMessage(item)" class="text-xs text-rose-500">{{ aiErrorMessage(item) }}</p>
                <p v-if="aiRequestId(item)" class="text-[11px] text-gray-500">RequestId: {{ aiRequestId(item) }}</p>
                <div v-if="aiLabelsForItem(item).length" class="flex flex-wrap gap-2">
                  <el-tag
                    v-for="label in aiLabelsForItem(item)"
                    :key="`${item.objectKey || item.id}-${label.name}`"
                    size="small"
                    effect="plain"
                  >
                    {{ label.name }} · {{ label.confidence }}%
                  </el-tag>
                </div>
                <el-alert
                  v-if="item.aiReview?.decision === 'BLOCK'"
                  type="error"
                  :closable="false"
                  show-icon
                  title="该文件已被 AI 拦截，访问链接已替换为占位图"
                />
                <el-alert
                  v-else-if="item.aiReview?.decision === 'REVIEW'"
                  type="warning"
                  :closable="false"
                  show-icon
                  title="AI 建议人工复核，管理员处理前暂不公开"
                />
              </div>
              <div v-for="entry in formatterEntries(item)" :key="`${item.objectKey || item.id}-${entry.key}`" class="space-y-1">
                <div class="flex items-center justify-between text-xs text-gray-500 uppercase tracking-widest">
                  <span>{{ entry.label }}</span>
                  <span class="text-emerald-500">{{ state[`${item.objectKey || item.id}-${entry.key}`] }}</span>
                </div>
                <div class="flex gap-2">
                  <textarea
                    :value="entry.value"
                    class="flex-1 rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700"
                    readonly
                    :rows="entry.value?.length > 120 ? 3 : 1"
                  ></textarea>
                  <button
                    class="rounded-2xl bg-gray-900 px-4 py-2 text-sm font-medium text-white"
                    @click="copyText(entry.value, `${item.objectKey || item.id}-${entry.key}`)"
                  >
                    复制
                  </button>
                </div>
              </div>
              <slot name="extra" :item="item"></slot>
            </div>
          </article>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
