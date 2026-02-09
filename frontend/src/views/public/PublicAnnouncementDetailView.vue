<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { 
  Megaphone, 
  Calendar, 
  ArrowLeft, 
  AlertTriangle, 
  Info,
  Pin,
  User,
  ChevronRight,
  Clock,
  Eye,
  Share2,
  FileText
} from 'lucide-vue-next'
import ChenxiGlobalFooter from '../../components/common/ChenxiGlobalFooter.vue'
import UserNavbar from '../../components/common/UserNavbar.vue'
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

const authorInfo = computed(() => {
  if (!announcement.value) {
    return {
      name: '系统公告',
      role: '系统',
      initials: '系',
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
    initials: name.slice(0, 1),
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

const getLevelIcon = (level) => {
  return level === 'EMERGENCY' ? AlertTriangle : Info
}

const getLevelClass = (level) => {
  return level === 'EMERGENCY' ? 'emergency' : 'notice'
}

const getLevelLabel = (level) => {
  return level === 'EMERGENCY' ? '紧急通知' : '一般公告'
}

const shareAnnouncement = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    // Could add toast notification here
  } catch (err) {
    console.error('复制链接失败', err)
  }
}
</script>

<template>
  <div class="announcement-detail-page">
    <UserNavbar />
    
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-badge">
            <Megaphone class="badge-icon" />
            <span>公告详情</span>
          </div>
          <h1 class="hero-title">{{ announcement?.title || '公告详情' }}</h1>
          <p class="hero-subtitle">查看完整的公告内容和相关信息</p>
        </div>
        <button class="btn-secondary" @click="router.push('/announcements')">
          <ArrowLeft class="btn-icon" />
          返回列表
        </button>
      </div>
      <div class="hero-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </section>

    <main class="main-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>正在加载公告内容...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="!announcement" class="error-container">
        <div class="error-illustration">
          <FileText class="error-icon" />
        </div>
        <h3 class="error-title">未找到公告</h3>
        <p class="error-desc">该公告可能已被删除或不存在</p>
        <button class="btn-primary" @click="router.push('/announcements')">
          返回公告列表
        </button>
      </div>

      <!-- Content -->
      <div v-else class="content-layout">
        <!-- Main Content -->
        <article class="main-content">
          <!-- Header Card -->
          <div class="header-card">
            <div class="header-top">
              <div class="level-badge" :class="getLevelClass(announcement.level)">
                <component :is="getLevelIcon(announcement.level)" class="badge-icon" />
                <span>{{ getLevelLabel(announcement.level) }}</span>
              </div>
              <div class="header-actions">
                <button class="action-btn" @click="shareAnnouncement" title="分享">
                  <Share2 class="action-icon" />
                </button>
              </div>
            </div>
            
            <h1 class="content-title">{{ announcement.title }}</h1>
            
            <div class="content-meta">
              <span class="meta-item">
                <Calendar class="meta-icon" />
                {{ dayjs(announcement.publishedAt || announcement.updatedAt).format('YYYY年MM月DD日 HH:mm') }}
              </span>
              <span v-if="announcement.pinned" class="meta-item pin">
                <Pin class="meta-icon" />
                置顶公告
              </span>
              <span class="meta-item">
                <Clock class="meta-icon" />
                {{ dayjs(announcement.publishedAt || announcement.updatedAt).fromNow() }}
              </span>
            </div>
          </div>

          <!-- Author Card -->
          <RouterLink
            v-if="authorInfo.link"
            :to="authorInfo.link"
            class="author-card"
          >
            <div class="author-avatar">
              <img v-if="authorInfo.avatar" :src="authorInfo.avatar" :alt="authorInfo.name" />
              <span v-else>{{ authorInfo.initials }}</span>
            </div>
            <div class="author-details">
              <span class="author-name">{{ authorInfo.name }}</span>
              <span class="author-role">{{ authorInfo.role }}</span>
            </div>
            <ChevronRight class="arrow-icon" />
          </RouterLink>
          
          <div v-else class="author-card">
            <div class="author-avatar">
              <span>{{ authorInfo.initials }}</span>
            </div>
            <div class="author-details">
              <span class="author-name">{{ authorInfo.name }}</span>
              <span class="author-role">{{ authorInfo.role }}</span>
            </div>
          </div>

          <!-- Summary Box -->
          <div v-if="announcement.summary" class="summary-box">
            <Info class="summary-icon" />
            <div class="summary-content">
              <span class="summary-label">摘要</span>
              <p>{{ announcement.summary }}</p>
            </div>
          </div>

          <!-- Content Box -->
          <div class="content-box">
            <div class="markdown-content" v-html="sanitizedContent"></div>
          </div>
        </article>

        <!-- Sidebar -->
        <aside class="sidebar">
          <!-- Quick Actions -->
          <div class="sidebar-card">
            <h3 class="sidebar-title">快速操作</h3>
            <div class="action-list">
              <button class="action-item" @click="router.push('/announcements')">
                <ArrowLeft class="item-icon" />
                <span>返回公告列表</span>
              </button>
              <button class="action-item" @click="shareAnnouncement">
                <Share2 class="item-icon" />
                <span>复制链接分享</span>
              </button>
            </div>
          </div>

          <!-- Info Card -->
          <div class="sidebar-card">
            <h3 class="sidebar-title">公告信息</h3>
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">状态</span>
                <span class="info-value" :class="announcement.status === 'PUBLISHED' ? 'published' : 'draft'">
                  {{ announcement.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">类型</span>
                <span class="info-value">{{ getLevelLabel(announcement.level) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">发布时间</span>
                <span class="info-value">{{ dayjs(announcement.publishedAt).format('YYYY-MM-DD') }}</span>
              </div>
              <div v-if="announcement.updatedAt !== announcement.publishedAt" class="info-item">
                <span class="info-label">更新时间</span>
                <span class="info-value">{{ dayjs(announcement.updatedAt).format('YYYY-MM-DD') }}</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <ChenxiGlobalFooter />
  </div>
</template>

<style scoped>
.announcement-detail-page {
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
  font-size: 2rem;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
  line-height: 1.3;
  max-width: 600px;
}

:global(.dark) .hero-title {
  color: #ffffff;
}

.hero-subtitle {
  font-size: 1rem;
  color: #4a4a5c;
  margin: 0;
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

.btn-icon {
  width: 18px;
  height: 18px;
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

/* Loading State */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 24px;
  text-align: center;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-container p {
  color: var(--text-muted, #6b7280);
  margin: 0;
}

/* Error State */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 24px;
  text-align: center;
}

.error-illustration {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(239, 68, 68, 0.05) 100%);
  border-radius: 32px;
  margin-bottom: 24px;
}

.error-icon {
  width: 56px;
  height: 56px;
  color: #ef4444;
}

.error-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 8px 0;
}

:global(.dark) .error-title {
  color: #ffffff;
}

.error-desc {
  font-size: 1rem;
  color: var(--text-muted, #6b7280);
  margin: 0 0 24px 0;
}

:global(.dark) .error-desc {
  color: rgba(255, 255, 255, 0.6);
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
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

/* Content Layout */
.content-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
}

@media (max-width: 1024px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
}

/* Main Content */
.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Header Card */
.header-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

:global(.dark) .header-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 0.85rem;
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
  width: 16px;
  height: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  border-radius: 12px;
  background: var(--bg-secondary, #f9fafb);
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .action-btn {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.action-btn:hover {
  border-color: #ff6b9d;
  color: #ff6b9d;
}

.action-icon {
  width: 18px;
  height: 18px;
}

.content-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 20px 0;
  line-height: 1.4;
}

:global(.dark) .content-title {
  color: #ffffff;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 20px;
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

/* Author Card */
.author-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  text-decoration: none;
  transition: all 0.2s ease;
}

:global(.dark) .author-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.author-card[href]:hover {
  border-color: rgba(255, 107, 157, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

:global(.dark) .author-card[href]:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .author-name {
  color: #ffffff;
}

.author-role {
  font-size: 0.9rem;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .author-role {
  color: rgba(255, 255, 255, 0.6);
}

.arrow-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted, #6b7280);
}

/* Summary Box */
.summary-box {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 16px;
}

.summary-icon {
  width: 24px;
  height: 24px;
  color: #3b82f6;
  flex-shrink: 0;
  margin-top: 2px;
}

.summary-content {
  flex: 1;
}

.summary-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-content p {
  margin: 0;
  font-size: 1rem;
  color: var(--text-secondary, #4a4a5c);
  line-height: 1.7;
}

:global(.dark) .summary-content p {
  color: rgba(255, 255, 255, 0.8);
}

/* Content Box */
.content-box {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

:global(.dark) .content-box {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.markdown-content {
  font-size: 1.05rem;
  line-height: 1.9;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .markdown-content {
  color: rgba(255, 255, 255, 0.9);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin-top: 1.8em;
  margin-bottom: 0.6em;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  line-height: 1.3;
}

:global(.dark) .markdown-content :deep(h1),
:global(.dark) .markdown-content :deep(h2),
:global(.dark) .markdown-content :deep(h3),
:global(.dark) .markdown-content :deep(h4) {
  color: #ffffff;
}

.markdown-content :deep(h1) { font-size: 1.8rem; }
.markdown-content :deep(h2) { font-size: 1.5rem; }
.markdown-content :deep(h3) { font-size: 1.25rem; }
.markdown-content :deep(h4) { font-size: 1.1rem; }

.markdown-content :deep(p) {
  margin-bottom: 1.2em;
}

.markdown-content :deep(a) {
  color: #ff6b9d;
  text-decoration: none;
  font-weight: 500;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-bottom: 1.2em;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin-bottom: 0.6em;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #ff6b9d;
  padding: 12px 20px;
  margin: 1.5em 0;
  background: rgba(255, 107, 157, 0.05);
  border-radius: 0 12px 12px 0;
  color: var(--text-secondary, #4a4a5c);
}

:global(.dark) .markdown-content :deep(blockquote) {
  background: rgba(255, 107, 157, 0.08);
  color: rgba(255, 255, 255, 0.7);
}

.markdown-content :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 3px 8px;
  border-radius: 6px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 0.9em;
  color: #ff6b9d;
}

:global(.dark) .markdown-content :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

.markdown-content :deep(pre) {
  background: #1a1a2e;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 1.5em 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #e2e8f0;
  font-size: 0.9em;
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: 12px;
  margin: 1.5em 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  margin: 2em 0;
}

:global(.dark) .markdown-content :deep(hr) {
  border-color: rgba(255, 255, 255, 0.1);
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5em 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 12px 16px;
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.1));
  text-align: left;
}

:global(.dark) .markdown-content :deep(th),
:global(.dark) .markdown-content :deep(td) {
  border-color: rgba(255, 255, 255, 0.1);
}

.markdown-content :deep(th) {
  background: var(--bg-secondary, #f9fafb);
  font-weight: 600;
}

:global(.dark) .markdown-content :deep(th) {
  background: rgba(255, 255, 255, 0.05);
}

/* Sidebar */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

:global(.dark) .sidebar-card {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.sidebar-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
}

:global(.dark) .sidebar-title {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.1);
}

/* Action List */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  background: var(--bg-secondary, #f9fafb);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.95rem;
  color: var(--text-primary, #1a1a2e);
  text-align: left;
}

:global(.dark) .action-item {
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
}

.action-item:hover {
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
}

.item-icon {
  width: 18px;
  height: 18px;
}

/* Info List */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.info-label {
  font-size: 0.9rem;
  color: var(--text-muted, #6b7280);
}

:global(.dark) .info-label {
  color: rgba(255, 255, 255, 0.6);
}

.info-value {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
}

:global(.dark) .info-value {
  color: #ffffff;
}

.info-value.published {
  color: #10b981;
}

.info-value.draft {
  color: #9ca3af;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-section {
    padding: 100px 20px 40px;
  }
  
  .hero-title {
    font-size: 1.5rem;
  }
  
  .hero-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .main-container {
    padding: 24px 16px;
  }
  
  .header-card,
  .content-box {
    padding: 24px;
  }
  
  .content-title {
    font-size: 1.4rem;
  }
  
  .content-meta {
    gap: 12px;
  }
  
  .markdown-content :deep(h1) { font-size: 1.5rem; }
  .markdown-content :deep(h2) { font-size: 1.3rem; }
  .markdown-content :deep(h3) { font-size: 1.15rem; }
}
</style>
