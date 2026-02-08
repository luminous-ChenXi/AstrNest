<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deleteAnnouncement, fetchAdminAnnouncements } from '../../services/announcements'
import { Plus, Edit, Eye, Trash2, Pin, AlertCircle, FileText } from 'lucide-vue-next'

const router = useRouter()
const loading = ref(false)
const announcements = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const level = ref('ALL')
const status = ref('ALL')

const levelOptions = [
  { value: 'ALL', label: '全部等级' },
  { value: 'EMERGENCY', label: '紧急' },
  { value: 'NOTICE', label: '注意' },
]

const statusOptions = [
  { value: 'ALL', label: '全部状态' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'DRAFT', label: '草稿' },
]

const load = async () => {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (level.value !== 'ALL') params.level = level.value
    if (status.value !== 'ALL') params.status = status.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const { data } = await fetchAdminAnnouncements(params)
    announcements.value = data.items || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载公告列表失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const handleDelete = async (row) => {
  const confirmed = window.confirm(`确定删除公告「${row.title}」吗？`)
  if (!confirmed) return
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    load()
  } catch (error) {
    console.error('删除公告失败', error)
    ElMessage.error('删除失败')
  }
}

const handleEdit = (row) => {
  router.push({ name: 'admin-announcement-edit', params: { id: row.id } })
}

const handleCreate = () => {
  router.push({ name: 'admin-announcement-create' })
}

const handlePreview = (row) => {
  router.push({ name: 'public-announcement-detail', params: { id: row.id } })
}

const handlePageChange = (value) => {
  page.value = value
  load()
}

const getLevelClass = (level) => {
  return level === 'EMERGENCY' ? 'level-emergency' : 'level-notice'
}

const getLevelLabel = (level) => {
  return level === 'EMERGENCY' ? '紧急' : '注意'
}

const getStatusClass = (status) => {
  return status === 'PUBLISHED' ? 'status-published' : 'status-draft'
}

const getStatusLabel = (status) => {
  return status === 'PUBLISHED' ? '已发布' : '草稿'
}
</script>

<template>
  <div class="announcement-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <p class="subtitle">公告管理</p>
        <h1 class="title">配置站点公告</h1>
        <p class="description">支持置顶与 Markdown 格式</p>
      </div>
      <button class="btn-create" @click="handleCreate">
        <Plus class="icon" />
        新建公告
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索标题或摘要..."
          class="filter-input search-input"
          @keyup.enter="load"
        />
        <select v-model="level" class="filter-input" @change="load">
          <option v-for="item in levelOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <select v-model="status" class="filter-input" @change="load">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </div>
      <button class="btn-filter" @click="load">筛选</button>
    </div>

    <!-- 公告列表 -->
    <div class="announcement-list">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="announcements.length === 0" class="empty-state">
        <FileText class="empty-icon" />
        <p class="empty-title">暂无公告</p>
        <p class="empty-desc">点击右上角按钮创建第一条公告</p>
      </div>

      <div v-else class="list-container">
        <div
          v-for="item in announcements"
          :key="item.id"
          class="announcement-card"
          :class="{ 'is-pinned': item.pinned }"
        >
          <div class="card-header">
            <div class="card-badges">
              <span class="badge" :class="getLevelClass(item.level)">
                <AlertCircle v-if="item.level === 'EMERGENCY'" class="badge-icon" />
                {{ getLevelLabel(item.level) }}
              </span>
              <span class="badge" :class="getStatusClass(item.status)">
                {{ getStatusLabel(item.status) }}
              </span>
              <span v-if="item.pinned" class="badge badge-pinned">
                <Pin class="badge-icon" />
                置顶
              </span>
            </div>
            <div class="card-actions">
              <button class="action-btn" @click="handleEdit(item)" title="编辑">
                <Edit class="action-icon" />
              </button>
              <button class="action-btn" @click="handlePreview(item)" title="预览">
                <Eye class="action-icon" />
              </button>
              <button class="action-btn action-delete" @click="handleDelete(item)" title="删除">
                <Trash2 class="action-icon" />
              </button>
            </div>
          </div>

          <div class="card-body">
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-summary">{{ item.summary || '暂无摘要' }}</p>
          </div>

          <div class="card-footer">
            <div class="time-info">
              <span v-if="item.publishedAt" class="time-item">
                发布于 {{ dayjs(item.publishedAt).format('YYYY/MM/DD HH:mm') }}
              </span>
              <span v-else class="time-item time-draft">草稿</span>
              <span class="time-divider">·</span>
              <span class="time-item">
                更新于 {{ dayjs(item.updatedAt).format('YYYY/MM/DD HH:mm') }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
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
    </div>
  </div>
</template>

<style scoped>
.announcement-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-title .subtitle {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin: 0 0 0.25rem 0;
}

.header-title .title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem 0;
}

.header-title .description {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0;
}

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 999px;
  box-shadow: 0 8px 25px rgba(255, 107, 157, 0.35);
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-create:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(255, 107, 157, 0.45);
}

.btn-create .icon {
  width: 1.1rem;
  height: 1.1rem;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  flex: 1;
}

.filter-input {
  padding: 0.6rem 1rem;
  font-size: 0.9rem;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 0.75rem;
  transition: all 0.2s ease;
}

.filter-input:focus {
  outline: none;
  border-color: #ff6b9d;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.search-input {
  min-width: 240px;
  flex: 1;
  max-width: 400px;
}

select.filter-input {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
}

.btn-filter {
  padding: 0.6rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: white;
  background: linear-gradient(135deg, #4ecdc4 0%, #6ee7d8 100%);
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-filter:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(78, 205, 196, 0.35);
}

/* 公告列表 */
.announcement-list {
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 1.5rem;
  padding: 1.5rem;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-soft);
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: var(--text-faint);
  margin-bottom: 1rem;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 0.25rem 0;
}

.empty-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0;
}

/* 卡片列表 */
.list-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.announcement-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--border-soft);
  border-radius: 1rem;
  padding: 1.25rem;
  transition: all 0.3s ease;
}

.announcement-card:hover {
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.1);
}

.announcement-card.is-pinned {
  border-color: rgba(255, 107, 157, 0.4);
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, rgba(255, 107, 157, 0.03) 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.875rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.card-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 999px;
}

.badge-icon {
  width: 0.875rem;
  height: 0.875rem;
}

.level-emergency {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.level-notice {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.status-published {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.status-draft {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}

.badge-pinned {
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 107, 157, 0.1);
  border-color: rgba(255, 107, 157, 0.3);
}

.action-btn.action-delete:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
}

.action-icon {
  width: 1rem;
  height: 1rem;
  color: var(--text-muted);
}

.action-btn:hover .action-icon {
  color: #ff6b9d;
}

.action-btn.action-delete:hover .action-icon {
  color: #ef4444;
}

.card-body {
  margin-bottom: 1rem;
}

.card-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.card-summary {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  padding-top: 0.875rem;
  border-top: 1px solid var(--border-soft);
}

.time-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.time-item {
  font-size: 0.8rem;
  color: var(--text-soft);
}

.time-draft {
  color: #9ca3af;
}

.time-divider {
  color: var(--text-faint);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-soft);
}

/* 深色模式适配 */
:root.dark .announcement-list {
  background: rgba(26, 26, 46, 0.5);
}

:root.dark .announcement-card {
  background: rgba(15, 15, 26, 0.8);
}

:root.dark .action-btn {
  background: rgba(26, 26, 46, 0.8);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    flex-direction: column;
  }

  .search-input {
    max-width: none;
  }

  .card-header {
    flex-direction: column;
  }

  .card-actions {
    align-self: flex-end;
  }
}
</style>
