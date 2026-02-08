<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { fetchPublicAnnouncementDetail } from '../../services/announcements'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const announcement = ref(null)

const sanitizedContent = computed(() => {
  if (!announcement.value?.contentMarkdown) return ''
  const html = marked.parse(announcement.value.contentMarkdown)
  return DOMPurify.sanitize(html)
})

const badgeClass = computed(() => {
  if (announcement.value?.level === 'EMERGENCY') return 'border border-rose-400/30 bg-rose-500/15 text-rose-100'
  return 'border border-amber-300/30 bg-amber-400/15 text-amber-50'
})

const authorInfo = computed(() => {
  if (!announcement.value) {
    return {
      name: '系统公告',
      role: '系统',
      initials: 'SYS',
      avatar: null,
      link: null,
    }
  }
  const name = announcement.value.author || '系统公告'
  const role = announcement.value.authorRole || (announcement.value.author ? '公告发布者' : '系统')
  const avatar = announcement.value.authorAvatar || null
  const link = announcement.value.authorUserId
    ? { name: 'public-user-profile', params: { userId: announcement.value.authorUserId } }
    : null
  return {
    name,
    role,
    initials: name.slice(0, 2).toUpperCase(),
    avatar,
    link,
  }
})

const load = async () => {
  loading.value = true
  try {
    const { id } = route.params
    const { data } = await fetchPublicAnnouncementDetail(id)
    announcement.value = data
  } catch (error) {
    console.error('加载公告详情失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-surface-body text-white">
    <div class="relative border-b border-white/10 bg-[radial-gradient(circle_at_20%_20%,rgba(127,123,255,0.25),transparent_55%)]">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-12">
        <div class="space-y-2">
          <p class="hero-eyebrow text-xs uppercase tracking-[0.35em]">公告详情</p>
          <h1 class="hero-title text-3xl font-semibold leading-tight">{{ announcement?.title || '公告详情' }}</h1>
          <p class="hero-desc text-sm">服务变更、维护提醒、紧急通知。</p>
        </div>
        <button
          class="hero-cta inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-medium transition"
          @click="router.push('/announcements')"
        >
          返回列表
        </button>
      </div>
    </div>

    <div class="mx-auto max-w-5xl px-6 py-10">
      <div v-if="announcement" class="detail-card">
        <div class="detail-card-head">
          <div class="space-y-3">
            <p class="meta-eyebrow text-xs uppercase tracking-[0.25em]">{{ dayjs(announcement.publishedAt || announcement.updatedAt).format('YYYY/MM/DD HH:mm') }}</p>
            <p class="meta-sub">{{ announcement.summary || '暂无摘要' }}</p>
            <div class="flex flex-wrap items-center gap-2 text-xs detail-badges">
              <span class="badge-soft" :class="badgeClass">{{ announcement.level === 'EMERGENCY' ? '紧急' : '注意' }}</span>
              <span v-if="announcement.pinned" class="badge-soft badge-accent">置顶</span>
              <span class="status-chip">{{ announcement.status === 'PUBLISHED' ? '已发布' : '草稿' }}</span>
            </div>
          </div>
        </div>
        <RouterLink
          v-if="authorInfo.link"
          :to="authorInfo.link"
          class="author-tile clickable mt-5"
        >
          <div class="author-avatar">
            <img v-if="authorInfo.avatar" :src="authorInfo.avatar" alt="avatar" />
            <span v-else>{{ authorInfo.initials }}</span>
          </div>
          <div class="tile-text">
            <p class="tile-name">{{ authorInfo.name }}</p>
            <p class="tile-role">{{ authorInfo.role || '查看发布者详情' }}</p>
          </div>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m9 18 6-6-6-6" />
          </svg>
        </RouterLink>
        <div v-else class="author-tile disabled mt-5">
          <div class="author-avatar">
            <span>{{ authorInfo.initials }}</span>
          </div>
          <div class="tile-text">
            <p class="tile-name">{{ authorInfo.name }}</p>
            <p class="tile-role">{{ authorInfo.role || '系统' }}</p>
          </div>
        </div>
        <div class="detail-content prose prose-invert max-w-none prose-headings:font-semibold prose-a:text-brand-primary">
          <div v-html="sanitizedContent"></div>
        </div>
      </div>

      <div v-else-if="loading" class="rounded-2xl border border-white/10 bg-white/5 p-6 text-sm text-white/70">正在加载公告...</div>
      <div v-else class="rounded-2xl border border-white/10 bg-white/5 p-6 text-sm text-white/70">未找到公告或已下线。</div>
    </div>
  </div>
</template>

<style scoped>
.hero-eyebrow {
  color: var(--text-soft, rgba(255, 255, 255, 0.6));
}

.hero-title {
  color: var(--color-text-primary, #0f172a);
}

.hero-desc {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.hero-cta {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.8));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.06));
}

.hero-cta:hover {
  border-color: var(--brand-primary, #7f7bff);
  color: var(--color-text-primary, #fff);
}

.detail-card {
  border-radius: 32px;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.08));
  background: var(--glass-bg, color-mix(in srgb, var(--color-bg-primary, #0b1021) 88%, rgba(255, 255, 255, 0.05)));
  padding: 32px;
  box-shadow: var(--shadow-card, 0 25px 60px rgba(0, 0, 0, 0.55));
}

.detail-card-head {
  color: var(--color-text-primary, #0f172a);
}

.meta-eyebrow {
  color: var(--text-soft, rgba(255, 255, 255, 0.6));
}

.meta-sub {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.detail-badges .badge-soft {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
  color: var(--color-text-primary, #0f172a);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-weight: 600;
}

.detail-badges .badge-accent {
  border-color: rgba(251, 191, 36, 0.4);
  background: rgba(251, 191, 36, 0.12);
  color: var(--color-text-primary, #0f172a);
}

.status-chip {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  color: var(--color-text-primary, #0f172a);
}

.detail-content {
  margin-top: 28px;
}

.detail-content :deep(p) {
  color: var(--color-text-secondary, rgba(238, 242, 255, 0.9));
}

.detail-content :deep(blockquote) {
  border-left: 3px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  padding-left: 0.75rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.author-tile {
  display: flex;
  align-items: center;
  gap: 14px;
  border-radius: 22px;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.12));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.03));
  padding: 14px 18px;
  transition: all 0.2s ease;
  color: var(--color-text-primary, #0f172a);
}

.author-tile.clickable:hover {
  border-color: var(--brand-primary, rgba(255, 255, 255, 0.4));
  background: color-mix(in srgb, var(--brand-primary, #7f7bff) 12%, transparent);
}

.author-tile svg {
  width: 20px;
  height: 20px;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--color-text-primary, #fff);
  background: linear-gradient(135deg, #8b5cf6, #ec4899);
  box-shadow: 0 12px 28px rgba(139, 92, 246, 0.4);
}

.author-avatar img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.tile-text {
  flex: 1;
}

.tile-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary, #0f172a);
}

.tile-role {
  font-size: 0.85rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.65));
}

.detail-content :deep(a) {
  color: var(--brand-primary, #7f7bff);
}
</style>
