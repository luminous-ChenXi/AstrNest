<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { RouterLink, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { 
  Megaphone, 
  Search, 
  Filter, 
  ArrowRight, 
  Calendar, 
  User, 
  AlertTriangle, 
  Info,
  Pin,
  X,
  ChevronRight,
  RefreshCw,
  FileText
} from 'lucide-vue-next'
import ChenxiGlobalFooter from '../../components/common/ChenxiGlobalFooter.vue'
import UserNavbar from '../../components/common/UserNavbar.vue'
import { fetchPublicAnnouncementDetail, fetchPublicAnnouncements } from '../../services/announcements'

const router = useRouter()
const loading = ref(false)
const items = ref([])
const page = ref(1)
const size = ref(9)
const total = ref(0)
const level = ref('ALL')
const keyword = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const activeDetail = ref(null)

const levelOptions = [
  { value: 'ALL', label: '全部公告' },
  { value: 'EMERGENCY', label: '紧急通知' },
  { value: 'NOTICE', label: '一般公告' },
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
      initials: '系',
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
    initials: name.slice(0, 1),
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

const closeDetail = () => {
  detailVisible.value = false
}

onMounted(load)

const handlePageChange = (value) => {
  page.value = value
  load()
}

const getLevelIcon = (level) => {
  return level === 'EMERGENCY' ? AlertTriangle : Info
}

const getLevelClass = (level) => {
  return level === 'EMERGENCY' ? 'emergency' : 'notice'
}

const getLevelLabel = (level) => {
  return level === 'EMERGENCY' ? '紧急' : '公告'
}
</script>

<template>
  <div class="announcements-page">
    <UserNavbar />
    
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-badge">
            <Megaphone class="badge-icon" />
            <span>公告中心</span>
          </div>
          <h1 class="hero-title">站内公告</h1>
          <p class="hero-subtitle">服务变更、维护提醒、紧急通知，一站查看</p>
        </div>
        <button class="btn-secondary" @click="router.push('/')">
          返回首页
        </button>
      </div>
      <div class="hero-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </section>

    <main class="main-container">
      <!-- Search & Filter Section -->
      <section class="filter-section">
        <div class="filter-card">
          <div class="filter-row">
            <div class="search-box">
              <Search class="search-icon" />
              <input
                v-model="keyword"
                type="text"
                maxlength="120"
                placeholder="搜索公告标题或内容..."
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <div class="select-wrapper">
                <Filter class="select-icon" />
                <select v-model="level" @change="load">
                  <option v-for="item in levelOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </div>
              <button class="btn-primary" @click="load">
                <RefreshCw class="btn-icon" />
                刷新
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Announcements Grid -->
      <section class="announcements-section">
        <!-- Empty State -->
        <div v-if="!loading && items.length === 0" class="empty-state">
          <div class="empty-illustration">
            <FileText class="empty-icon" />
          </div>
          <h3 class="empty-title">暂无公告</h3>
          <p class="empty-desc">暂时没有相关公告，请稍后再来查看</p>
        </div>

        <!-- Loading State -->
        <div v-else-if="loading" class="loading-grid">
          <div v-for="i in 6" :key="i" class="skeleton-card">
            <div class="skeleton-header">
              <div class="skeleton-badge"></div>
              <div class="skeleton-date"></div>
            </div>
            <div class="skeleton-title"></div>
            <div class="skeleton-desc"></div>
            <div class="skeleton-footer"></div>
          </div>
        </div>

        <!-- Announcements Grid -->
        <div v-else class="announcements-grid">
          <article
            v-for="announcement in decoratedItems"
            :key="announcement.id"
            class="announcement-card"
            :class="{ 'is-pinned': announcement.pinned, 'is-emergency': announcement.level === 'EMERGENCY' }"
            @click="openDetail(announcement.id)"
          >
            <!-- Card Header -->
            <div class="card-header">
              <div class="level-badge" :class="getLevelClass(announcement.level)">
                <component :is="getLevelIcon(announcement.level)" class="badge-icon" />
                <span>{{ getLevelLabel(announcement.level) }}</span>
              </div>
              <div class="card-meta">
                <span v-if="announcement.pinned" class="pin-badge">
                  <Pin class="pin-icon" />
                  置顶
                </span>
                <span class="publish-date">
                  <Calendar class="date-icon" />
                  {{ dayjs(announcement.publishedAt || announcement.updatedAt).format('MM/DD') }}
                </span>
              </div>
            </div>

            <!-- Card Content -->
            <div class="card-content">
              <h3 class="card-title">{{ announcement.title }}</h3>
              <p class="card-summary">{{ announcement.summary || '暂无摘要' }}</p>
            </div>

            <!-- Card Footer -->
            <div class="card-footer">
              <div class="author-info">
                <div class="author-avatar">
                  <img v-if="announcement.authorInfo.avatar" :src="announcement.authorInfo.avatar" :alt="announcement.authorInfo.name" />
                  <span v-else>{{ announcement.authorInfo.initials }}</span>
                </div>
                <span class="author-name">{{ announcement.authorInfo.name }}</span>
              </div>
              <span class="read-more">
                查看详情
                <ChevronRight class="arrow-icon" />
              </span>
            </div>
          </article>
        </div>

        <!-- Pagination -->
        <div v-if="total > size" class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>

    <ChenxiGlobalFooter />

    <!-- Detail Modal -->
    <Transition name="modal">
      <div v-if="detailVisible" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-panel">
          <!-- Modal Header -->
          <div class="modal-header">
            <div class="header-content">
              <div class="header-badge" :class="activeDetail?.level === 'EMERGENCY' ? 'emergency' : 'notice'">
                <component :is="getLevelIcon(activeDetail?.level)" class="badge-icon" />
                <span>{{ getLevelLabel(activeDetail?.level) }}</span>
              </div>
              <h2 class="modal-title">{{ activeDetail?.title || '公告详情' }}</h2>
              <div class="modal-meta">
                <span class="meta-item">
                  <Calendar class="meta-icon" />
                  {{ activeDetail ? dayjs(activeDetail.publishedAt || activeDetail.updatedAt).format('YYYY年MM月DD日 HH:mm') : '' }}
                </span>
                <span v-if="activeDetail?.pinned" class="meta-item pin">
                  <Pin class="meta-icon" />
                  置顶公告
                </span>
              </div>
            </div>
            <button class="close-btn" @click="closeDetail">
              <X class="close-icon" />
            </button>
          </div>

          <!-- Modal Body -->
          <div class="modal-body">
            <div v-if="detailLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <p>正在加载公告内容...</p>
            </div>
            
            <template v-else-if="activeDetail">
              <!-- Author Info -->
              <RouterLink
                v-if="detailAuthorInfo.link"
                :to="detailAuthorInfo.link"
                class="author-card clickable"
                @click.stop
              >
                <div class="author-avatar">
                  <img v-if="detailAuthorInfo.avatar" :src="detailAuthorInfo.avatar" alt="avatar" />
                  <span v-else>{{ detailAuthorInfo.initials }}</span>
                </div>
                <div class="author-details">
                  <span class="author-name">{{ detailAuthorInfo.name }}</span>
                  <span class="author-role">{{ detailAuthorInfo.role }}</span>
                </div>
                <ChevronRight class="arrow-icon" />
              </RouterLink>
              
              <div v-else class="author-card">
                <div class="author-avatar">
                  <span>{{ detailAuthorInfo.initials }}</span>
                </div>
                <div class="author-details">
                  <span class="author-name">{{ detailAuthorInfo.name }}</span>
                  <span class="author-role">{{ detailAuthorInfo.role }}</span>
                </div>
              </div>

              <!-- Summary -->
              <div v-if="activeDetail.summary" class="summary-box">
                <Info class="summary-icon" />
                <p>{{ activeDetail.summary }}</p>
              </div>

              <!-- Content -->
              <div class="content-box">
                <div class="markdown-content" v-html="sanitizedContent"></div>
              </div>
            </template>

            <div v-else class="error-state">
              <AlertTriangle class="error-icon" />
              <p>未找到公告或已下线</p>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.announcements-page {
  min-height: 100vh;
  background: var(--bg-body, #fafafa);
}

/* Hero Section */
.hero-section {
  position: relative;
  padding: 120px 24px 60px;
  background: linear-gradient(135deg, #FADCE9 0%, #AED0ED 50%, #f0fbf4 100%);
  overflow: hidden;
}

:global(.dark) .hero-section {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f0f23 100%);
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 24px;
}

.hero-content {
  flex: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #ff6b9d;
  margin-bottom: 16px;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.2);
}

:global(.dark) .hero-badge {
  background: rgba(255, 255, 255, 0.1);
  color: #ff8fab;
}

.badge-icon {
  width: 18px;
  height: 18px;
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
}

:global(.dark) .hero-title {
  color: #ffffff;
}

.hero-subtitle {
  font-size: 1.1rem;
  color: #4a4a5c;
  margin: 0;
  max-width: 400px;
}

:global(.dark) .hero-subtitle {
  color: rgba(255, 255, 255, 0.7);
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #4a4a5c;
  cursor: pointer;
  transition: all 0.3s ease;
}

:global(.dark) .btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.btn-secondary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

/* Hero Decoration */
.hero-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
}

.circle-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 107, 157, 0.3) 0%, transparent 70%);
  top: -100px;
  right: 10%;
}

.circle-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(174, 208, 237, 0.4) 0%, transparent 70%);
  bottom: -50px;
  left: 5%;
}

.circle-3 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(240, 251, 244, 0.5) 0%, transparent 70%);
  top: 50%;
  right: 20%;
}

/* Main Container */
.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* Filter Section */
.filter-section {
  margin-bottom: 32px;
}

.filter-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 20px;
  padding: 20px 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

:global(.dark) .filter-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.search-box {
  flex: 1;
  min-width: 280px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  color: #9ca3af;
}

.search-box input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 12px;
  font-size: 0.95rem;
  background: var(--bg-input, #f9fafb);
  color: var(--text-primary, #1a1a2e);
  transition: all 0.2s ease;
}

:global(.dark) .search-box input {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.search-box input:focus {
  outline: none;
  border-color: #ff6b9d;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.search-box input::placeholder {
  color: #9ca3af;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.select-wrapper {
  position: relative;
}

.select-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #9ca3af;
  pointer-events: none;
}

.select-wrapper select {
  padding: 14px 40px 14px 42px;
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 12px;
  font-size: 0.95rem;
  background: var(--bg-input, #f9fafb);
  color: var(--text-primary, #1a1a2e);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  min-width: 140px;
}

:global(.dark) .select-wrapper select {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.select-wrapper select:focus {
  outline: none;
  border-color: #ff6b9d;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.35);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 107, 157, 0.45);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

/* Announcements Section */
.announcements-section {
  min-height: 400px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.empty-illustration {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(255, 143, 171, 0.1) 100%);
  border-radius: 24px;
  margin-bottom: 24px;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: #ff6b9d;
}

.empty-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 8px 0;
}

:global(.dark) .empty-title {
  color: #ffffff;
}

.empty-desc {
  font-size: 1rem;
  color: var(--text-muted, #6b7280);
  margin: 0;
}

:global(.dark) .empty-desc {
  color: rgba(255, 255, 255, 0.6);
}

/* Loading Grid */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.skeleton-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 20px;
  padding: 24px;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

:global(.dark) .skeleton-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.skeleton-badge {
  width: 60px;
  height: 24px;
  background: var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 999px;
}

.skeleton-date {
  width: 80px;
  height: 20px;
  background: var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 6px;
}

.skeleton-title {
  height: 28px;
  background: var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 6px;
  margin-bottom: 12px;
}

.skeleton-desc {
  height: 60px;
  background: var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 6px;
  margin-bottom: 16px;
}

.skeleton-footer {
  height: 40px;
  background: var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 6px;
}

/* Announcements Grid */
.announcements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.announcement-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 20px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
}

:global(.dark) .announcement-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.announcement-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border-color: rgba(255, 107, 157, 0.3);
}

:global(.dark) .announcement-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.announcement-card.is-pinned {
  border-color: rgba(251, 191, 36, 0.4);
  background: linear-gradient(135deg, var(--bg-card, #ffffff) 0%, rgba(251, 191, 36, 0.05) 100%);
}

:global(.dark) .announcement-card.is-pinned {
  background: linear-gradient(135deg, #1a1a2e 0%, rgba(251, 191, 36, 0.08) 100%);
}

.announcement-card.is-emergency {
  border-color: rgba(239, 68, 68, 0.4);
  background: linear-gradient(135deg, var(--bg-card, #ffffff) 0%, rgba(239, 68, 68, 0.05) 100%);
}

:global(.dark) .announcement-card.is-emergency {
  background: linear-gradient(135deg, #1a1a2e 0%, rgba(239, 68, 68, 0.08) 100%);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
}

.level-badge.emergency {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.level-badge.notice {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.level-badge .badge-icon {
  width: 14px;
  height: 14px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(251, 191, 36, 0.15);
  color: #d97706;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.pin-icon {
  width: 12px;
  height: 12px;
}

.publish-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.85rem;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .publish-date {
  color: rgba(255, 255, 255, 0.6);
}

.date-icon {
  width: 14px;
  height: 14px;
}

/* Card Content */
.card-content {
  flex: 1;
  margin-bottom: 20px;
}

.card-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 10px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:global(.dark) .card-title {
  color: #ffffff;
}

.announcement-card:hover .card-title {
  color: #ff6b9d;
}

.card-summary {
  font-size: 0.9rem;
  color: var(--text-muted, #6b7280);
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:global(.dark) .card-summary {
  color: rgba(255, 255, 255, 0.6);
}

/* Card Footer */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
}

:global(.dark) .card-footer {
  border-color: rgba(255, 255, 255, 0.1);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  color: white;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-secondary, #4a4a5c);
}

:global(.dark) .author-name {
  color: rgba(255, 255, 255, 0.8);
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #ff6b9d;
  transition: all 0.2s ease;
}

.arrow-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
}

.announcement-card:hover .arrow-icon {
  transform: translateX(4px);
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
}

.modal-panel {
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  background: var(--bg-card, #ffffff);
  border-radius: 24px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:global(.dark) .modal-panel {
  background: #1a1a2e;
}

/* Modal Header */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 28px 32px;
  border-bottom: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
}

:global(.dark) .modal-header {
  border-color: rgba(255, 255, 255, 0.1);
}

.header-content {
  flex: 1;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 12px;
}

.header-badge.emergency {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.header-badge.notice {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.header-badge .badge-icon {
  width: 16px;
  height: 16px;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 12px 0;
  line-height: 1.3;
}

:global(.dark) .modal-title {
  color: #ffffff;
}

.modal-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9rem;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .meta-item {
  color: rgba(255, 255, 255, 0.6);
}

.meta-item.pin {
  color: #d97706;
}

.meta-icon {
  width: 16px;
  height: 16px;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-muted, #6b7280);
  flex-shrink: 0;
  margin-left: 16px;
}

:global(.dark) .close-btn {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.close-icon {
  width: 20px;
  height: 20px;
}

/* Modal Body */
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 28px 32px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: var(--text-muted, #6b7280);
  margin: 0;
}

/* Author Card */
.author-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: var(--bg-secondary, #f9fafb);
  border-radius: 16px;
  margin-bottom: 24px;
}

:global(.dark) .author-card {
  background: rgba(255, 255, 255, 0.05);
}

.author-card.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.author-card.clickable:hover {
  background: rgba(255, 107, 157, 0.1);
}

.author-card .author-avatar {
  width: 48px;
  height: 48px;
  font-size: 1.2rem;
}

.author-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-details .author-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .author-details .author-name {
  color: #ffffff;
}

.author-role {
  font-size: 0.85rem;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .author-role {
  color: rgba(255, 255, 255, 0.6);
}

.author-card .arrow-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted, #6b7280);
}

/* Summary Box */
.summary-box {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  margin-bottom: 24px;
}

.summary-icon {
  width: 20px;
  height: 20px;
  color: #3b82f6;
  flex-shrink: 0;
  margin-top: 2px;
}

.summary-box p {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-secondary, #4a4a5c);
  line-height: 1.6;
}

:global(.dark) .summary-box p {
  color: rgba(255, 255, 255, 0.8);
}

/* Content Box */
.content-box {
  background: var(--bg-secondary, #f9fafb);
  border-radius: 16px;
  padding: 28px;
}

:global(.dark) .content-box {
  background: rgba(255, 255, 255, 0.05);
}

.markdown-content {
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .markdown-content {
  color: rgba(255, 255, 255, 0.9);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .markdown-content :deep(h1),
:global(.dark) .markdown-content :deep(h2),
:global(.dark) .markdown-content :deep(h3),
:global(.dark) .markdown-content :deep(h4) {
  color: #ffffff;
}

.markdown-content :deep(p) {
  margin-bottom: 1em;
}

.markdown-content :deep(a) {
  color: #ff6b9d;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-bottom: 1em;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin-bottom: 0.5em;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #ff6b9d;
  padding-left: 1em;
  margin: 1em 0;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .markdown-content :deep(blockquote) {
  color: rgba(255, 255, 255, 0.6);
}

.markdown-content :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', monospace;
  font-size: 0.9em;
}

:global(.dark) .markdown-content :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-content :deep(pre) {
  background: #1a1a2e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #e2e8f0;
}

/* Error State */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
}

.error-icon {
  width: 48px;
  height: 48px;
  color: #ef4444;
  margin-bottom: 16px;
}

.error-state p {
  color: var(--text-muted, #6b7280);
  margin: 0;
}

/* Modal Transition */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-panel,
.modal-leave-to .modal-panel {
  transform: scale(0.95) translateY(20px);
}

/* Responsive */
@media (max-width: 768px) {
  .hero-section {
    padding: 100px 20px 40px;
  }
  
  .hero-title {
    font-size: 1.8rem;
  }
  
  .hero-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .main-container {
    padding: 24px 16px;
  }
  
  .filter-row {
    flex-direction: column;
  }
  
  .search-box {
    min-width: 100%;
  }
  
  .filter-group {
    width: 100%;
  }
  
  .select-wrapper {
    flex: 1;
  }
  
  .select-wrapper select {
    width: 100%;
  }
  
  .btn-primary {
    flex-shrink: 0;
  }
  
  .announcements-grid {
    grid-template-columns: 1fr;
  }
  
  .modal-overlay {
    padding: 16px;
  }
  
  .modal-header,
  .modal-body {
    padding: 20px 24px;
  }
  
  .modal-title {
    font-size: 1.2rem;
  }
}
</style>
