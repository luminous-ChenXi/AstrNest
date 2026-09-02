<template>
  <transition name="gallery-lightbox-fade">
    <div v-if="visible && item" class="gallery-lightbox" @click.self="handleClose">
      <transition name="gallery-lightbox-scale">
        <div v-if="visible" class="gallery-lightbox__panel">
          <button type="button" class="close-btn" @click="handleClose">
            <X class="h-4 w-4" />
          </button>
          <div class="gallery-lightbox__grid">
            <div class="space-y-4">
              <div class="media-wrapper">
                <template v-if="isVideo">
                  <video
                    class="media-frame"
                    :poster="resolvedPosterUrl"
                    controls
                    playsinline
                    preload="metadata"
                  >
                    <source :src="resolvedMediaUrl" />
                  </video>
                </template>
                <template v-else>
                  <img :src="resolvedMediaUrl" :alt="item?.fileName" class="media-frame" loading="lazy" />
                </template>
              </div>
              <div class="stat-chips">
                <span class="stat-chip">
                  <CalendarClock class="h-3.5 w-3.5" />
                  {{ formattedDate }}
                </span>
                <span class="stat-chip">
                  <ThumbsUp class="h-3.5 w-3.5" :class="{ 'text-brand-accent': likeActive }" />
                  {{ likeCountLabel }}
                </span>
                <span class="stat-chip">
                  <Eye class="h-3.5 w-3.5 text-brand-primary" />
                  {{ viewCountLabel }}
                </span>
              </div>
            </div>

            <div class="space-y-4">
              <div>
                <p class="section-label">文件信息</p>
                <p class="heading">{{ item?.fileName || '未知文件' }}</p>
                <p class="subheading">由 {{ item?.ownerDisplayName || '匿名用户' }} 上传</p>
              </div>
              <ul class="meta-list">
                <li class="meta-item">
                  <FileImage class="h-4 w-4 text-brand-primary" />
                  <span>{{ formattedSize }}</span>
                </li>
                <li class="meta-item">
                  <LinkIcon class="h-4 w-4 text-brand-accent" />
                  <span class="truncate">{{ resolvedMediaUrl }}</span>
                </li>
                <li v-if="objectKeyLabel" class="meta-item">
                  <Layers class="h-4 w-4 text-brand-emerald" />
                  <span>{{ objectKeyLabel }}</span>
                </li>
              </ul>
              <!-- 图集信息 -->
              <div v-if="item?.album" class="info-card">
                <p class="section-label">所属图集</p>
                <button
                  type="button"
                  class="uploader-btn"
                  @click="handleAlbumClick"
                >
                  <div class="uploader-fallback album-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
                  </div>
                  <div class="flex-1 text-left">
                    <p class="heading-sm">{{ item.album.name }}</p>
                    <p class="subheading-sm">查看图集详情</p>
                  </div>
                  <ChevronRight class="h-4 w-4 muted-icon" />
                </button>
              </div>
              <div class="info-card">
                <p class="section-label">上传者</p>
                <button
                  type="button"
                  class="uploader-btn"
                  :disabled="!item?.ownerId"
                  @click="handleUploader"
                >
                  <template v-if="item?.ownerAvatarUrl">
                    <img :src="item.ownerAvatarUrl" alt="uploader avatar" class="uploader-avatar" />
                  </template>
                  <template v-else>
                    <div class="uploader-fallback">{{ ownerInitial }}</div>
                  </template>
                  <div class="flex-1 text-left">
                    <p class="heading-sm">{{ item?.ownerDisplayName || '匿名用户' }}</p>
                    <p class="subheading-sm">{{ item?.ownerId ? '查看用户详情' : '暂无公开主页' }}</p>
                  </div>
                  <ChevronRight class="h-4 w-4 muted-icon" />
                </button>
              </div>
              <div class="info-card">
                <p class="info-card-title">点赞统计</p>
                <div class="flex items-center gap-3 info-card-sub">
                  <User class="h-3.5 w-3.5 muted-icon" />
                  <div class="flex items-center gap-1">
                    <span>最近点赞：</span>
                    <button
                      v-if="showLatestLikerButton"
                      type="button"
                      class="latest-link"
                      @click="handleLatestLiker"
                    >
                      {{ latestLikerLabel || '站内用户' }}
                    </button>
                    <span v-else>{{ latestLikerLabel || '暂无记录' }}</span>
                  </div>
                </div>
                <button
                  type="button"
                  class="like-btn"
                  :class="{ active: likeActive }"
                  :disabled="disableLike"
                  @click="handleLike"
                >
                  <ThumbsUp class="h-4 w-4" />
                  {{ likeLabel }}
                </button>
                <p v-if="guestLikeNotice" class="notice">{{ guestLikeNotice }}</p>
              </div>
              <div class="action-row">
                <a
                  :href="resolvedMediaUrl"
                  target="_blank"
                  rel="noopener"
                  class="primary-action"
                >
                  <ExternalLink class="h-4 w-4" />
                  新标签打开
                </a>
                <button type="button" class="ghost-action" @click="handleCopy">
                  <Copy class="h-4 w-4" />
                  复制链接
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import {
  CalendarClock,
  ChevronRight,
  Copy,
  ExternalLink,
  Eye,
  FileImage,
  Layers,
  Link as LinkIcon,
  ThumbsUp,
  User,
  X,
} from 'lucide-vue-next'

const props = defineProps({
  visible: { type: Boolean, default: false },
  item: { type: Object, default: null },
  disableLike: { type: Boolean, default: false },
  likeActive: { type: Boolean, default: false },
  likeLabel: { type: String, default: '点赞' },
  guestLikeNotice: { type: String, default: '' },
  latestLikerLabel: { type: String, default: '暂无记录' },
  showLatestLikerButton: { type: Boolean, default: false },
  resolvePublicUrl: {
    type: Function,
    default: (item) => item?.publicUrl || '',
  },
  resolvePosterUrl: {
    type: Function,
    default: (item) => item?.publicUrl || '',
  },
  formatDate: {
    type: Function,
    default: (value) => {
      if (!value) return '时间未知'
      const date = new Date(value)
      return date.toLocaleString('zh-CN', { hour12: false })
    },
  },
  formatBytes: {
    type: Function,
    default: (bytes) => {
      if (!bytes) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let sizeValue = bytes
      let unitIndex = 0
      while (sizeValue >= 1024 && unitIndex < units.length - 1) {
        sizeValue /= 1024
        unitIndex += 1
      }
      return `${sizeValue.toFixed(sizeValue >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
    },
  },
})

const emit = defineEmits(['close', 'like', 'copy', 'open-uploader', 'open-latest-liker', 'open-album'])

const resolvedMediaUrl = computed(() => (props.item ? props.resolvePublicUrl?.(props.item) || '' : ''))
const resolvedPosterUrl = computed(() => (props.item ? props.resolvePosterUrl?.(props.item) || '' : ''))
const formattedDate = computed(() => (props.item ? props.formatDate?.(props.item.uploadedAt) : '时间未知'))
const formattedSize = computed(() => (props.item ? props.formatBytes?.(props.item.size) : '0 B'))
const viewCountLabel = computed(() => `${props.item?.invokeCount ?? 0} 次引用`)
const likeCountLabel = computed(() => `${props.item?.likeCount ?? 0} 喜欢`)
const ownerInitial = computed(() => (props.item?.ownerDisplayName ? props.item.ownerDisplayName.slice(0, 1) : 'U'))
const objectKeyLabel = computed(() => (props.item?.objectKey ? `对象键：${props.item.objectKey}` : ''))
const isVideo = computed(() => (props.item?.mediaCategory || '').toUpperCase() === 'VIDEO')

const handleClose = () => emit('close')
const handleCopy = () => emit('copy', resolvedMediaUrl.value)
const handleLike = (event) => emit('like', { event, item: props.item })
const handleUploader = () => emit('open-uploader', props.item)
const handleLatestLiker = () => emit('open-latest-liker', props.item)
const handleAlbumClick = () => {
  if (props.item?.album?.pathSlug) {
    emit('open-album', props.item.album)
  }
}

</script>

<style scoped>
.gallery-lightbox {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
}

.gallery-lightbox__panel {
  position: relative;
  width: 100%;
  max-width: 960px;
  border-radius: 1.75rem;
  padding: 1.5rem;
  border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.12));
  background: var(--glass-bg, color-mix(in srgb, var(--color-bg-primary, #0f172a) 85%, rgba(15, 23, 42, 0.95)));
  color: var(--color-text-primary, #fff);
  box-shadow: var(--shadow-card, 0 35px 80px rgba(15, 23, 42, 0.45));
}

.gallery-lightbox__grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.media-wrapper {
  border-radius: 1.5rem;
  overflow: hidden;
}

.media-frame {
  width: 100%;
  height: 18rem;
  object-fit: cover;
  border-radius: 1.5rem;
}

.stat-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  background: var(--panel-overlay, rgba(255, 255, 255, 0.08));
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.12));
  font-size: 0.8rem;
  color: var(--color-text-primary);
}

.section-label {
  font-size: 0.75rem;
  letter-spacing: 0.35em;
  text-transform: uppercase;
  color: var(--text-soft, rgba(255, 255, 255, 0.6));
}

.heading {
  margin-top: 0.35rem;
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.subheading {
  font-size: 0.9rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.6));
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  font-size: 0.9rem;
  color: var(--color-text-primary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.info-card {
  border-radius: 1.5rem;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.08));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.06));
  padding: 1rem;
}

.info-card-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.info-card-sub {
  font-size: 0.9rem;
  color: var(--color-text-secondary, #6b7280);
}

.muted-icon {
  color: var(--color-text-secondary, #6b7280);
}

.uploader-btn {
  margin-top: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  border-radius: 1rem;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.15));
  background: var(--panel-overlay, rgba(0, 0, 0, 0.2));
  padding: 0.6rem 0.9rem;
  color: var(--color-text-primary);
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.uploader-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.uploader-btn:not(:disabled):hover {
  border-color: var(--brand-primary, #7f7bff);
}

.uploader-avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  object-fit: cover;
}

.uploader-fallback {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel-overlay, rgba(255, 255, 255, 0.08));
  color: var(--color-text-primary);
  font-weight: 600;
}

.heading-sm {
  font-size: 0.95rem;
  font-weight: 600;
}

.subheading-sm {
  font-size: 0.75rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.65));
}

.like-btn {
  margin-top: 0.9rem;
  width: 100%;
  border-radius: 1rem;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.25));
  padding: 0.65rem 0.9rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--color-text-primary, rgba(255, 255, 255, 0.8));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.like-btn:not(:disabled):hover {
  border-color: var(--brand-primary, #7f7bff);
  background: color-mix(in srgb, var(--brand-primary, #7f7bff) 10%, transparent);
  color: var(--brand-primary, #7f7bff);
}

.like-btn.active {
  border-color: var(--brand-accent, #ff5f8f);
  color: var(--brand-accent, #ff5f8f);
  background: color-mix(in srgb, var(--brand-accent, #ff5f8f) 12%, transparent);
}

.like-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notice {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: rgba(255, 200, 200, 0.85);
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.primary-action,
.ghost-action {
  flex: 1;
  min-width: 180px;
  border-radius: 1.25rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-weight: 600;
  padding: 0.75rem 1rem;
}

.primary-action {
  background: linear-gradient(135deg, var(--brand-primary, #7f7bff), var(--brand-accent, #ff5f8f));
  color: #fff;
}

.ghost-action {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  color: var(--color-text-primary, rgba(255, 255, 255, 0.85));
  background: var(--panel-overlay, transparent);
}

.close-btn {
  position: absolute;
  right: 1rem;
  top: 1rem;
  border-radius: 999px;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.15));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.08));
  padding: 0.35rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.6));
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.close-btn:hover {
  color: var(--color-text-primary, #fff);
  border-color: var(--brand-primary, rgba(255, 255, 255, 0.35));
  background: color-mix(in srgb, var(--brand-primary, #7f7bff) 12%, transparent);
}

.latest-link {
  color: var(--brand-primary, #7f7bff);
  text-decoration: underline dotted;
  text-underline-offset: 2px;
}

.gallery-lightbox-fade-enter-active,
.gallery-lightbox-fade-leave-active {
  transition: opacity 0.25s ease;
}

.gallery-lightbox-fade-enter-from,
.gallery-lightbox-fade-leave-to {
  opacity: 0;
}

.gallery-lightbox-scale-enter-active,
.gallery-lightbox-scale-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.gallery-lightbox-scale-enter-from,
.gallery-lightbox-scale-leave-to {
  transform: scale(0.96);
  opacity: 0;
}

@media (max-width: 640px) {
  .gallery-lightbox__panel {
    padding: 1.1rem;
  }
  .media-frame {
    height: 14rem;
  }
}
</style>
