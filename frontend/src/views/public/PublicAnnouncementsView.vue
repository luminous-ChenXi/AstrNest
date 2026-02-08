<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { RouterLink, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import ChenxiGlobalFooter from '../../components/common/ChenxiGlobalFooter.vue'
import UserNavbar from '../../components/common/UserNavbar.vue'
import { fetchPublicAnnouncementDetail, fetchPublicAnnouncements } from '../../services/announcements'

const router = useRouter()
const loading = ref(false)
const items = ref([])
const page = ref(1)
const size = ref(8)
const total = ref(0)
const level = ref('ALL')
const keyword = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const activeDetail = ref(null)

const levelOptions = [
  { value: 'ALL', label: '全部等级' },
  { value: 'EMERGENCY', label: '紧急' },
  { value: 'NOTICE', label: '注意' },
]

const sanitizedContent = computed(() => {
  if (!activeDetail.value?.contentMarkdown) return ''
  return DOMPurify.sanitize(marked.parse(activeDetail.value.contentMarkdown))
})

const buildAuthorInfo = (announcement) => {
  if (!announcement) {
    return {
      name: '系统公告',
      role: '系统',
      initials: 'SYS',
      avatar: null,
      link: null,
    }
  }
  const name = announcement.author || '系统公告'
  const role = announcement.authorRole || (announcement.author ? '公告发布者' : '系统')
  const avatar = announcement.authorAvatar || null
  const link = announcement.authorUserId
    ? { name: 'public-user-profile', params: { userId: announcement.authorUserId } }
    : null
  return {
    name,
    role,
    initials: name.slice(0, 2).toUpperCase(),
    avatar,
    link,
  }
}

const decoratedItems = computed(() => items.value.map((item) => ({ ...item, authorInfo: buildAuthorInfo(item) })))
const detailAuthorInfo = computed(() => buildAuthorInfo(activeDetail.value))

const load = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: size.value,
    }
    if (level.value !== 'ALL') params.level = level.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const { data } = await fetchPublicAnnouncements(params)
    items.value = data.items || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载公告失败', error)
  } finally {
    loading.value = false
  }
}

const openDetail = async (id) => {
  detailVisible.value = true
  detailLoading.value = true
  activeDetail.value = null
  try {
    const { data } = await fetchPublicAnnouncementDetail(id)
    activeDetail.value = data
  } catch (error) {
    console.error('加载公告详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)

const handlePageChange = (value) => {
  page.value = value
  load()
}

const levelBadgeClass = (value) => {
  if (value === 'EMERGENCY') return 'border border-rose-400/30 bg-rose-500/15 text-rose-100'
  return 'border border-amber-300/30 bg-amber-400/15 text-amber-50'
}
</script>

<template>
  <div class="min-h-screen bg-surface-body pt-32 text-white">
    <UserNavbar />
    <div class="relative border-b border-white/10 bg-[radial-gradient(circle_at_20%_20%,rgba(127,123,255,0.25),transparent_55%)]">
      <div class="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-16 md:flex-row md:items-center md:justify-between">
        <div class="space-y-3">
          <p class="hero-eyebrow text-xs uppercase tracking-[0.45em]">公告中心</p>
          <h1 class="hero-title text-3xl font-semibold leading-tight md:text-4xl">站内公告</h1>
          <p class="hero-desc text-sm">服务变更、维护提醒、紧急通知，一站查看。</p>
        </div>
        <button
          class="hero-cta inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-medium transition"
          @click="router.push('/')"
        >
          返回首页
        </button>
      </div>
    </div>

    <main class="mx-auto max-w-6xl space-y-8 px-6 py-10">
      <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6 shadow-card md:p-7">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:gap-4">
          <div class="relative flex-1">
            <input
              v-model="keyword"
              type="text"
              maxlength="120"
              placeholder="搜索标题或摘要"
              class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
              @keyup.enter="load"
            />
          </div>
          <select
            v-model="level"
            class="w-full rounded-2xl border border-white/15 bg-black/30 px-3 py-3 text-sm text-white/80 outline-none transition focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/30 md:w-40"
            @change="load"
          >
            <option v-for="item in levelOptions" :key="item.value" :value="item.value" class="text-slate-900">{{ item.label }}</option>
          </select>
          <button
            class="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent px-6 py-3 text-sm font-semibold text-white shadow-[0_10px_30px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5"
            @click="load"
          >
            刷新
          </button>
        </div>
        <p class="mt-2 text-xs login-hint">登录后可看到更多与您相关的更新。</p>
      </section>

      <section class="space-y-4">
        <div
          v-if="!loading && items.length === 0"
          class="rounded-3xl border border-white/10 bg-white/5 px-6 py-10 text-center text-white/70"
        >
          暂无公告，敬请期待。
        </div>

        <div v-else class="grid gap-5 md:grid-cols-2">
          <article
            v-for="announcement in decoratedItems"
            :key="announcement.id"
            class="announcement-card group cursor-pointer rounded-[32px] p-7 transition hover:-translate-y-1"
            @click="openDetail(announcement.id)"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex items-center gap-2 text-xs card-muted">
                <span class="h-2 w-2 rounded-full bg-brand-primary"></span>
                <p class="font-semibold uppercase tracking-wide">{{ dayjs(announcement.publishedAt || announcement.updatedAt).format('YYYY/MM/DD HH:mm') }}</p>
              </div>
              <span class="rounded-full px-3 py-1 text-xs font-semibold badge-soft" :class="levelBadgeClass(announcement.level)">
                {{ announcement.level === 'EMERGENCY' ? '紧急' : '注意' }}
              </span>
            </div>
            <h3 class="mt-3 text-xl font-semibold card-title group-hover:text-brand-primary">{{ announcement.title }}</h3>
            <p class="mt-2 line-clamp-3 text-sm leading-6 card-desc">{{ announcement.summary || '暂未提供摘要。' }}</p>
            <div class="mt-5 flex flex-wrap items-center justify-between gap-4 text-xs card-muted">
              <div class="flex flex-wrap items-center gap-3 author-block">
                <RouterLink
                  v-if="announcement.authorInfo.link"
                  :to="announcement.authorInfo.link"
                  class="author-inline clickable"
                  @click.stop
                >
                  <span class="author-name">{{ announcement.authorInfo.name }}</span>
                  <span class="author-divider" v-if="announcement.authorInfo.role">·</span>
                  <span class="author-role" v-if="announcement.authorInfo.role">{{ announcement.authorInfo.role }}</span>
                </RouterLink>
                <div v-else class="author-inline">
                  <span class="author-name">{{ announcement.authorInfo.name }}</span>
                  <span class="author-divider" v-if="announcement.authorInfo.role">·</span>
                  <span class="author-role" v-if="announcement.authorInfo.role">{{ announcement.authorInfo.role }}</span>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="status-chip">{{ announcement.status === 'PUBLISHED' ? '已发布' : '草稿' }}</span>
                  <span v-if="announcement.pinned" class="rounded-full border border-amber-300/30 bg-amber-400/15 px-2 py-1 text-amber-100">置顶</span>
                </div>
              </div>
              <span class="text-brand-primary">查看详情 →</span>
            </div>
          </article>
        </div>

        <div v-if="total > size" class="flex justify-center pt-2">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="handlePageChange"
          />
        </div>

        <div v-if="loading" class="rounded-3xl border border-white/10 bg-white/5 p-6 text-sm text-white/70">
          正在加载公告...
        </div>
      </section>
    </main>

    <ChenxiGlobalFooter />

    <el-dialog
      v-model="detailVisible"
      :title="activeDetail?.title || '公告详情'"
      width="720px"
      align-center
      class="chenxi-dialog dark"
    >
      <template #header>
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-4">
            <div class="space-y-2">
              <p class="text-xs uppercase tracking-[0.25em] text-white/50">公告详情</p>
              <h3 class="detail-title">{{ activeDetail?.title || '公告详情' }}</h3>
              <div class="detail-meta" v-if="activeDetail">
                <span>{{ dayjs(activeDetail.publishedAt || activeDetail.updatedAt).format('YYYY/MM/DD HH:mm') }}</span>
                <span class="level-pill">{{ activeDetail.level === 'EMERGENCY' ? '紧急' : '注意' }}</span>
              </div>
            </div>
            <span v-if="activeDetail?.pinned" class="rounded-full border border-amber-300/30 bg-amber-400/15 px-3 py-1 text-xs text-amber-100">置顶</span>
          </div>
          <RouterLink
            v-if="detailAuthorInfo.link"
            :to="detailAuthorInfo.link"
            class="author-tile clickable"
          >
            <div class="author-avatar">
              <img v-if="detailAuthorInfo.avatar" :src="detailAuthorInfo.avatar" alt="avatar" />
              <span v-else>{{ detailAuthorInfo.initials }}</span>
            </div>
            <div class="tile-text">
              <p class="tile-name">{{ detailAuthorInfo.name }}</p>
              <p class="tile-role">{{ detailAuthorInfo.role || '查看发布者详情' }}</p>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6" />
            </svg>
          </RouterLink>
          <div v-else class="author-tile disabled">
            <div class="author-avatar">
              <span>{{ detailAuthorInfo.initials }}</span>
            </div>
            <div class="tile-text">
              <p class="tile-name">{{ detailAuthorInfo.name }}</p>
              <p class="tile-role">{{ detailAuthorInfo.role || '系统' }}</p>
            </div>
          </div>
        </div>
      </template>

      <div v-if="detailLoading" class="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">正在加载...</div>
      <div v-else-if="activeDetail" class="dialog-body space-y-5 text-sm text-white/85">
        <p class="text-white/80">{{ activeDetail.summary || '暂无摘要' }}</p>
        <div class="prose prose-invert max-w-none prose-headings:font-semibold prose-a:text-brand-primary prose-strong:text-white">
          <div v-html="sanitizedContent"></div>
        </div>
      </div>
      <div v-else class="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">未找到公告或已下线。</div>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.chenxi-dialog) {
  background: radial-gradient(circle at 20% 20%, rgba(127, 123, 255, 0.25), transparent 55%), rgba(10, 12, 24, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 70px rgba(0, 0, 0, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(28px);
  border-radius: 28px;
  color: #f8fafc;
}

:deep(.chenxi-dialog .el-dialog__body) {
  padding: 28px 32px 32px;
}

.dialog-body {
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(9, 11, 24, 0.7);
  padding: 24px 26px 28px;
}

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

.announcement-card {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.1));
  background: var(--glass-bg, color-mix(in srgb, var(--color-bg-primary, #0b1021) 88%, rgba(255, 255, 255, 0.04)));
  box-shadow: var(--shadow-card, 0 20px 50px rgba(0, 0, 0, 0.35));
}

.announcement-card:hover {
  border-color: color-mix(in srgb, var(--brand-primary, #7f7bff) 50%, var(--border-soft, rgba(255, 255, 255, 0.1)) 50%);
  background: color-mix(in srgb, var(--glass-bg, #0b1021) 85%, rgba(255, 255, 255, 0.07));
}

.card-title {
  color: var(--color-text-primary, #fff);
}

.card-desc {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.72));
}

.card-muted {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.65));
}

.badge-soft {
  border-color: var(--border-soft, rgba(255, 255, 255, 0.2));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
  color: var(--color-text-primary, #fff);
}

.status-chip {
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.2));
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  color: var(--color-text-primary, #0f172a);
}

.author-block {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.author-inline {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.7));
}

.author-inline.clickable {
  cursor: pointer;
  transition: color 0.2s ease;
}

.author-inline.clickable:hover {
  color: var(--color-text-primary, #fff);
}

.author-name {
  font-weight: 600;
  color: var(--color-text-primary, #0f172a);
}

.author-role {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.55));
}

.author-divider {
  color: var(--text-soft, rgba(255, 255, 255, 0.35));
}

.author-tile {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.02);
  padding: 12px 16px;
  text-align: left;
  transition: all 0.2s ease;
}

.author-tile.clickable:hover {
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.08);
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #7c3aed, #f472b6);
  box-shadow: 0 10px 25px rgba(124, 58, 237, 0.35);
}

.author-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.author-tile .tile-text {
  flex: 1;
}

.author-tile .tile-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #fff;
}

.author-tile .tile-role {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.65);
}

.author-tile svg {
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.6);
}

.author-tile.disabled {
  cursor: default;
}

.detail-title {
  font-size: 1.65rem;
  font-weight: 600;
  color: var(--color-text-primary, #0f172a);
}

.detail-meta {
  display: flex;
  gap: 12px;
  font-size: 0.82rem;
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.6));
}

.detail-meta .level-pill {
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.18));
  font-size: 0.75rem;
  background: var(--panel-overlay, rgba(255, 255, 255, 0.04));
}

.login-hint {
  color: var(--color-text-secondary, rgba(255, 255, 255, 0.6));
}

:deep(.prose) {
  color: var(--color-text-secondary, #e5e7eb);
}

:deep(.prose strong) {
  color: #fff;
}

:deep(.prose p) {
  color: #dce1f0;
}
</style>
