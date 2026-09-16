<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { fetchPublicAnnouncements } from '../../services/announcements'

const router = useRouter()
const items = ref([])
const loading = ref(false)
const activeIndex = ref(0)
let timer = null

const load = async () => {
  loading.value = true
  try {
    const { data } = await fetchPublicAnnouncements({ page: 0, size: 5 })
    items.value = data.items || []
    activeIndex.value = 0
  } catch (error) {
    console.error('加载公告聚合失败', error)
  } finally {
    loading.value = false
  }
}

const startTicker = () => {
  clearInterval(timer)
  timer = window.setInterval(() => {
    if (!items.value.length) return
    activeIndex.value = (activeIndex.value + 1) % items.value.length
  }, 5200)
}

const buildAuthorInfo = (announcement) => {
  if (!announcement) {
    return { name: '系统公告', role: '系统', initials: 'SYS', avatar: null, link: null }
  }
  const name = announcement.author || '系统公告'
  const role = announcement.authorRole || (announcement.author ? '公告发布者' : '系统')
  const avatar = announcement.authorAvatar || null
  const link = announcement.authorUserId
    ? { name: 'public-user-profile', params: { userId: announcement.authorUserId } }
    : null
  return { name, role, initials: name.slice(0, 2).toUpperCase(), avatar, link }
}

const activeAnnouncement = computed(() => items.value[activeIndex.value] || null)
const activeAuthor = computed(() => buildAuthorInfo(activeAnnouncement.value))

onMounted(() => {
  load()
  startTicker()
})

onBeforeUnmount(() => {
  clearInterval(timer)
})

watch(items, () => {
  startTicker()
})

const badgeClass = (level) => {
  if (level === 'EMERGENCY') {
    return 'border border-rose-200/70 bg-rose-100/80 text-rose-800 dark:border-rose-400/40 dark:bg-rose-500/20 dark:text-rose-50'
  }
  return 'border border-amber-200/70 bg-amber-100/80 text-amber-700 dark:border-amber-300/40 dark:bg-amber-500/20 dark:text-amber-50'
}

const goDetail = (id) => {
  router.push({ name: 'public-announcement-detail', params: { id } })
}
</script>

<template>
  <section class="glass-panel rounded-3xl p-5 text-body-primary shadow-card">
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div class="flex items-center gap-3">
        <span class="rounded-full border border-body bg-surface-overlay px-3 py-1 text-xs font-semibold text-body-primary">最新公告</span>
        <p class="text-sm text-body-soft">服务变更、维护提醒、紧急通知</p>
      </div>
      <div class="flex gap-2">
        <button
          class="rounded-full border border-body px-3 py-1 text-xs font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
          @click="router.push({ name: 'public-announcements' })"
        >
          查看全部
        </button>
      </div>
    </div>

    <div v-if="loading" class="mt-4 rounded-2xl border border-body bg-surface-overlay p-4 text-sm text-body-soft">
      公告加载中...
    </div>

    <div v-else-if="!items.length" class="mt-4 rounded-2xl border border-body bg-surface-overlay p-4 text-sm text-body-soft">
      暂无公告。
    </div>

    <transition name="fade" mode="out-in">
      <div
        v-if="items.length"
        :key="items[activeIndex]?.id"
        class="mt-4 rounded-[28px] border border-body bg-surface-overlay p-6"
      >
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex items-center gap-2 text-xs text-body-soft">
            <span class="h-2 w-2 rounded-full bg-brand-primary"></span>
            <span>
              {{ dayjs(items[activeIndex]?.publishedAt || items[activeIndex]?.updatedAt).format('YYYY/MM/DD HH:mm') }}
            </span>
            <span class="rounded-full border px-3 py-1 text-xs font-semibold" :class="badgeClass(items[activeIndex]?.level)">
              {{ items[activeIndex]?.level === 'EMERGENCY' ? '紧急' : '注意' }}
            </span>
            <span
              v-if="items[activeIndex]?.pinned"
              class="rounded-full border border-amber-200/70 bg-amber-100/80 px-2 py-1 text-xs font-medium text-amber-700 dark:border-amber-400/30 dark:bg-amber-500/25 dark:text-amber-50"
            >
              置顶
            </span>
          </div>
          <!-- Git分割行---------------到时候看看哪一个按钮好看，在上面呢还是在下面呢 -->
           
          <!-- <button class="text-sm text-brand-primary hover:underline" @click="goDetail(items[activeIndex]?.id)">
            查看详情 →
          </button> -->
        </div>
        <h3 class="mt-3 text-lg font-semibold text-body-primary">{{ items[activeIndex]?.title }}</h3>
        <p class="mt-2 line-clamp-2 text-sm leading-6 text-body-soft">{{ items[activeIndex]?.summary || '暂无摘要' }}</p>
        <div class="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-body-soft">
          <p class="author-inline">
            <span class="author-name">{{ activeAuthor.name }}</span>
            <span class="author-dot" v-if="activeAuthor.role">·</span>
            <span class="author-role" v-if="activeAuthor.role">{{ activeAuthor.role }}</span>
          </p>
          <button class="text-sm text-brand-primary hover:underline" @click="goDetail(items[activeIndex]?.id)">
            查看详情 →
          </button>
          <!-- 废话，当然是下面的 -->
        </div>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.author-inline {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 0.9rem;
  color: var(--text-soft);
}

.author-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.author-role {
  color: var(--text-muted);
}

.author-dot {
  color: var(--text-muted);
}
</style>
