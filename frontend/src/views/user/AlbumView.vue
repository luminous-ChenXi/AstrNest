<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Folder, Image, Link, Trash2, Edit3, Eye, EyeOff, Copy, ExternalLink, AlertTriangle, Sparkles, Layers, TrendingUp } from 'lucide-vue-next'
import { albumApi } from '../../api/album'
import { useSystemStore } from '../../stores/system'

const router = useRouter()
const systemStore = useSystemStore()
const albums = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentAlbum = ref(null)
const currentAlbumMedias = ref([])

const createForm = ref({
  pathSlug: '',
  name: '',
  description: '',
  isPublic: false
})

const createFormRef = ref(null)
const createRules = {
  pathSlug: [
    { required: true, message: '请输入路径标识', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '路径仅支持英文、数字、下划线、连字符', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入图集名称', trigger: 'blur' },
    { max: 100, message: '最多 100 个字符', trigger: 'blur' }
  ]
}

const normalizedDomain = computed(() => {
  const domain = systemStore.config?.assetDomain
  if (!domain) return window.location.origin
  return domain.replace(/\/$/, '')
})

const slugSuggestion = ref('')

const slugify = (value = '') => {
  const ascii = value
    .trim()
    .replace(/\s+/g, '-')
    .replace(/[^a-zA-Z0-9_-]/g, '')
    .toLowerCase()
  return ascii.slice(0, 50)
}

// 统计信息
const totalAlbums = computed(() => albums.value.length)
const totalImages = computed(() => albums.value.reduce((sum, album) => sum + (album.mediaCount || 0), 0))
const totalAccess = computed(() => albums.value.reduce((sum, album) => sum + (album.accessCount || 0), 0))
const publicAlbums = computed(() => albums.value.filter(a => a.isPublic).length)

watch(
  () => createForm.value.name,
  (val) => {
    if (!createForm.value.pathSlug) {
      const suggested = slugify(val)
      slugSuggestion.value = suggested
      createForm.value.pathSlug = suggested
    }
  }
)

const fetchAlbums = async () => {
  loading.value = true
  try {
    await systemStore.fetchSystemConfig()
    const res = await albumApi.getMyAlbums()
    albums.value = res.data.content || []
  } catch (error) {
    ElMessage.error('获取图集列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createFormRef.value) return

  createForm.value.pathSlug = slugify(createForm.value.pathSlug)

  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await albumApi.createAlbum(createForm.value)
        ElMessage.success('图集创建成功')
        showCreateDialog.value = false
        createForm.value = { pathSlug: '', name: '', description: '', isPublic: false }
        slugSuggestion.value = ''
        fetchAlbums()
      } catch (error) {
        if (error.response?.status === 409) {
          ElMessage.error('路径标识已被使用')
        } else {
          ElMessage.error(error.response?.data?.message || '创建失败')
        }
      }
    }
  })
}

const handleDelete = async (album) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除图集 "${album.name}" 吗？图集中的图片不会被删除。`,
      '确认删除',
      { type: 'warning' }
    )
    await albumApi.deleteAlbum(album.albumUuid)
    ElMessage.success('删除成功')
    fetchAlbums()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewAlbumDetail = async (album) => {
  try {
    const res = await albumApi.getAlbumDetail(album.albumUuid)
    currentAlbum.value = res.data.album
    currentAlbumMedias.value = res.data.medias || []
    showDetailDialog.value = true
  } catch (error) {
    ElMessage.error('获取图集详情失败')
  }
}

const copyAlbumLink = (album) => {
  // 复制图集详情页链接（图片瀑布流瀑布流页面）
  const link = `${window.location.origin}/album/${album.pathSlug}`
  navigator.clipboard.writeText(link).then(() => {
    ElMessage.success('图集链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const openAlbumLink = (album) => {
  // 跳转到图集详情页面（图片瀑布流瀑布流）
  window.open(`/album/${album.pathSlug}`, '_blank')
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchAlbums()
})
</script>

<template>
  <div class="album-view">
    <div class="album-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <div class="header-badge">
            <Layers class="badge-icon" />
            <span>图集管理</span>
          </div>
          <h1 class="page-title">我的图集</h1>
          <p class="page-subtitle">创建精美图集，通过短链分享你的作品</p>
        </div>
        <button class="btn-primary-gradient" @click="showCreateDialog = true">
          <Plus class="btn-icon" />
          创建图集
        </button>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon-wrap pink">
            <Folder class="stat-icon" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ totalAlbums }}</span>
            <span class="stat-label">图集总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap blue">
            <Image class="stat-icon" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ totalImages }}</span>
            <span class="stat-label">图片总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap mint">
            <TrendingUp class="stat-icon" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ totalAccess }}</span>
            <span class="stat-label">总访问量</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap purple">
            <Eye class="stat-icon" />
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ publicAlbums }}</span>
            <span class="stat-label">公开图集</span>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="album-loading">
        <div class="loading-animation">
          <div class="loading-ring"></div>
          <div class="loading-ring"></div>
          <div class="loading-ring"></div>
        </div>
        <p class="loading-text">正在加载图集...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="albums.length === 0" class="album-empty">
        <div class="empty-illustration">
          <div class="empty-bg-circle"></div>
          <Folder class="empty-icon" />
        </div>
        <h3 class="empty-title">还没有图集</h3>
        <p class="empty-desc">创建你的第一个图集，开始整理和分享图片吧</p>
        <button class="btn-primary-gradient mt-4" @click="showCreateDialog = true">
          <Plus class="btn-icon" />
          创建图集
        </button>
      </div>

      <!-- 图集网格 -->
      <div v-else class="album-grid">
        <div
          v-for="album in albums"
          :key="album.albumUuid"
          class="album-card"
        >
          <!-- 封面区域 -->
          <div class="album-card-cover">
            <img
              v-if="album.coverImageUuid"
              :src="`/upload/${album.coverImageUuid.slice(0, 2)}/${album.coverImageUuid.slice(2, 4)}/${album.coverImageUuid}`.replace('//', '/')"
              :alt="album.name"
              class="cover-image"
            />
            <div v-else class="album-card-placeholder">
              <Image class="placeholder-icon" />
            </div>
            
            <!-- 隐私徽章 -->
            <div class="album-card-badge" :class="{ 'is-private': !album.isPublic }">
              <Eye v-if="album.isPublic" class="badge-icon" />
              <EyeOff v-else class="badge-icon" />
              <span>{{ album.isPublic ? '公开' : '私密' }}</span>
            </div>

            <!-- 悬停遮罩 -->
            <div class="cover-overlay">
              <button class="overlay-btn" @click="openAlbumLink(album)">
                <ExternalLink class="overlay-icon" />
                访问
              </button>
            </div>
          </div>
          
          <!-- 内容区域 -->
          <div class="album-card-content">
            <h3 class="album-card-title">{{ album.name }}</h3>
            <p class="album-card-desc">{{ album.description || '暂无描述' }}</p>
            
            <!-- 元信息 -->
            <div class="album-card-meta">
              <div class="meta-item">
                <div class="meta-icon-wrap">
                  <Image class="meta-icon" />
                </div>
                <span>{{ album.mediaCount || 0 }} 张图片</span>
              </div>
              <div class="meta-item">
                <div class="meta-icon-wrap">
                  <ExternalLink class="meta-icon" />
                </div>
                <span>{{ album.accessCount || 0 }} 次访问</span>
              </div>
            </div>

            <!-- 路径 -->
            <div class="album-card-path" @click="copyAlbumLink(album)">
              <Link class="path-icon" />
              <code class="path-code">/album/{{ album.pathSlug }}</code>
              <Copy class="copy-icon" />
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="album-card-actions">
            <button class="action-btn primary" @click="viewAlbumDetail(album)" title="管理">
              <Edit3 class="action-icon" />
              <span>管理</span>
            </button>
            <button class="action-btn" @click="copyAlbumLink(album)" title="复制链接">
              <Copy class="action-icon" />
            </button>
            <button class="action-btn danger" @click="handleDelete(album)" title="删除">
              <Trash2 class="action-icon" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建图集对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建图集"
      width="520px"
      destroy-on-close
      class="album-dialog"
    >
      <div class="dialog-header">
        <div class="dialog-icon">
          <Sparkles class="icon" />
        </div>
        <div class="dialog-info">
          <h3>创建新图集</h3>
          <p>创建一个精美的图片集合</p>
        </div>
      </div>

      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-position="top"
        class="album-form"
      >
        <el-form-item label="路径标识" prop="pathSlug">
          <el-input
            v-model="createForm.pathSlug"
            placeholder="仅英文、数字、下划线、连字符"
            @blur="createForm.pathSlug = slugify(createForm.pathSlug)"
            class="slug-input"
          >
            <template #prepend>/picture/</template>
          </el-input>
          <p class="form-hint">图集页面：{{ `${normalizedDomain}/album/${createForm.pathSlug || slugSuggestion || 'slug'}` }}</p>
          <p class="form-hint">随机图片：{{ `${normalizedDomain}/picture/random/${createForm.pathSlug || slugSuggestion || 'slug'}` }}</p>
          <p class="form-hint text-warning" v-if="slugSuggestion && createForm.pathSlug !== slugSuggestion">
            <AlertTriangle class="inline-icon" /> 建议使用：{{ slugSuggestion }}（避免中文或空格）
          </p>
        </el-form-item>

        <el-form-item label="图集名称" prop="name">
          <el-input
            v-model="createForm.name"
            placeholder="给图集起个名字"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="描述一下这个图集（可选）"
          />
        </el-form-item>

        <el-form-item>
          <div class="privacy-toggle">
            <el-checkbox v-model="createForm.isPublic" size="large">
              <span class="privacy-label">公开图集</span>
            </el-checkbox>
            <p class="privacy-hint">公开后可被他人搜索/访问；私密仅自己可见</p>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" class="btn-create">
          <Plus class="btn-icon-small" />
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 图集详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="currentAlbum?.name"
      width="800px"
      destroy-on-close
      class="album-dialog"
    >
      <div v-if="currentAlbum" class="album-detail">
        <div class="detail-header">
          <div class="detail-stats">
            <div class="detail-stat">
              <span class="stat-num">{{ currentAlbumMedias.length }}</span>
              <span class="stat-label">张图片</span>
            </div>
            <div class="detail-stat">
              <span class="stat-num">{{ currentAlbum.accessCount || 0 }}</span>
              <span class="stat-label">次访问</span>
            </div>
          </div>
          <div class="detail-actions">
            <el-button type="primary" @click="copyAlbumLink(currentAlbum)">
              <Copy class="btn-icon-small" />
              复制链接
            </el-button>
            <el-button @click="openAlbumLink(currentAlbum)">
              <ExternalLink class="btn-icon-small" />
              访问图集
            </el-button>
          </div>
        </div>

        <el-divider />

        <h4 class="section-title">
          <Image class="section-icon" />
          图集图片
        </h4>
        
        <div v-if="currentAlbumMedias.length === 0" class="detail-empty">
          <div class="empty-state">
            <Image class="empty-icon" />
            <p>暂无图片</p>
            <span>请在媒体管理中添加图片到图集</span>
          </div>
        </div>
        
        <div v-else class="detail-media-grid">
          <div
            v-for="media in currentAlbumMedias"
            :key="media.mediaUuid"
            class="detail-media-item"
          >
            <img :src="media.publicUrl" :alt="media.fileName" loading="lazy" />
            <div class="media-overlay">
              <p class="media-name">{{ media.fileName }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.album-view {
  min-height: 100vh;
  background: var(--color-bg-primary);
}

.album-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.header-content {
  flex: 1;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.15), rgba(174, 208, 237, 0.15));
  border: 1px solid rgba(249, 168, 200, 0.3);
  border-radius: 999px;
  color: #E87A9F;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.75rem;
}

.header-badge .badge-icon {
  width: 1rem;
  height: 1rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
  color: var(--color-text-primary);
  background: linear-gradient(135deg, #1a1a2e, #E87A9F);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 1rem;
}

/* 渐变按钮 */
.btn-primary-gradient {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.75rem;
  background: linear-gradient(135deg, #F9A8C8, #E87A9F);
  color: white;
  border: none;
  border-radius: 1rem;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(232, 122, 159, 0.35);
}

.btn-primary-gradient:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(232, 122, 159, 0.45);
}

.btn-icon {
  width: 1.25rem;
  height: 1.25rem;
}

.btn-icon-small {
  width: 1rem;
  height: 1rem;
  margin-right: 0.25rem;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 1rem;
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
}

.stat-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
}

.stat-icon-wrap.pink {
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.2), rgba(249, 168, 200, 0.1));
  color: #E87A9F;
}

.stat-icon-wrap.blue {
  background: linear-gradient(135deg, rgba(174, 208, 237, 0.2), rgba(174, 208, 237, 0.1));
  color: #5B8DB8;
}

.stat-icon-wrap.mint {
  background: linear-gradient(135deg, rgba(167, 243, 208, 0.2), rgba(167, 243, 208, 0.1));
  color: #10B981;
}

.stat-icon-wrap.purple {
  background: linear-gradient(135deg, rgba(196, 181, 253, 0.2), rgba(196, 181, 253, 0.1));
  color: #8B5CF6;
}

.stat-icon {
  width: 24px;
  height: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

/* 加载状态 */
.album-loading {
  text-align: center;
  padding: 5rem 2rem;
}

.loading-animation {
  position: relative;
  width: 60px;
  height: 60px;
  margin: 0 auto 1.5rem;
}

.loading-ring {
  position: absolute;
  inset: 0;
  border: 3px solid transparent;
  border-top-color: #F9A8C8;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

.loading-ring:nth-child(2) {
  inset: 8px;
  border-top-color: #AED0ED;
  animation-duration: 0.9s;
  animation-direction: reverse;
}

.loading-ring:nth-child(3) {
  inset: 16px;
  border-top-color: #E87A9F;
  animation-duration: 0.6s;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

/* 空状态 */
.album-empty {
  text-align: center;
  padding: 5rem 2rem;
}

.empty-illustration {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.empty-bg-circle {
  position: absolute;
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.15), rgba(174, 208, 237, 0.15));
  border-radius: 50%;
}

.empty-icon {
  width: 56px;
  height: 56px;
  color: #E87A9F;
  position: relative;
  z-index: 1;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem;
}

.empty-desc {
  color: var(--color-text-secondary);
  margin: 0 0 1.5rem;
}

.mt-4 {
  margin-top: 1rem;
}

/* 图集网格 */
.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.album-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 1.25rem;
  overflow: hidden;
  transition: all 0.3s ease;
}

.album-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.12);
  border-color: rgba(249, 168, 200, 0.3);
}

/* 封面区域 */
.album-card-cover {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, #f8f9fc, #eef0f5);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.album-card:hover .cover-image {
  transform: scale(1.05);
}

.album-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-icon {
  width: 48px;
  height: 48px;
  color: #cbd5e1;
}

/* 隐私徽章 */
.album-card-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  background: rgba(16, 185, 129, 0.9);
  color: white;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.album-card-badge.is-private {
  background: rgba(107, 114, 128, 0.9);
}

.badge-icon {
  width: 0.875rem;
  height: 0.875rem;
}

/* 悬停遮罩 */
.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.album-card:hover .cover-overlay {
  opacity: 1;
}

.overlay-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  background: white;
  color: #1a1a2e;
  border: none;
  border-radius: 0.75rem;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.overlay-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.overlay-icon {
  width: 1rem;
  height: 1rem;
}

/* 内容区域 */
.album-card-content {
  padding: 1.25rem;
}

.album-card-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.375rem;
  color: var(--color-text-primary);
}

.album-card-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0 0 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5rem;
}

/* 元信息 */
.album-card-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.meta-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--color-bg-tertiary);
  border-radius: 6px;
}

.meta-icon {
  width: 14px;
  height: 14px;
  color: var(--color-text-muted);
}

/* 路径 */
.album-card-path {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg-tertiary);
  border-radius: 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.album-card-path:hover {
  background: rgba(249, 168, 200, 0.1);
}

.path-icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-text-muted);
}

.path-code {
  flex: 1;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8125rem;
  color: #E87A9F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-text-muted);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.album-card-path:hover .copy-icon {
  opacity: 1;
}

/* 操作按钮 */
.album-card-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.875rem 1.25rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: 0.625rem;
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
  background: rgba(249, 168, 200, 0.1);
}

.action-btn.primary {
  flex: 1;
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.15), rgba(249, 168, 200, 0.05));
  border-color: rgba(249, 168, 200, 0.3);
  color: #E87A9F;
}

.action-btn.primary:hover {
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.25), rgba(249, 168, 200, 0.1));
}

.action-btn.danger:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.action-icon {
  width: 1rem;
  height: 1rem;
}

/* 表单样式 */
.form-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 0.5rem;
}

.form-hint.text-warning {
  color: #f59e0b;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.inline-icon {
  width: 0.875rem;
  height: 0.875rem;
}

/* 对话框样式 */
:deep(.album-dialog) {
  border-radius: 1.5rem;
}

:deep(.album-dialog .el-dialog__header) {
  display: none;
}

:deep(.album-dialog .el-dialog__body) {
  padding: 1.5rem;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.dialog-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #F9A8C8, #E87A9F);
  border-radius: 12px;
}

.dialog-icon .icon {
  width: 24px;
  height: 24px;
  color: white;
}

.dialog-info h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.25rem;
  color: var(--color-text-primary);
}

.dialog-info p {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.album-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--color-text-primary);
}

.slug-input :deep(.el-input-group__prepend) {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
  font-family: monospace;
}

.privacy-toggle {
  padding: 1rem;
  background: var(--color-bg-tertiary);
  border-radius: 0.75rem;
}

.privacy-label {
  font-weight: 500;
  color: var(--color-text-primary);
}

.privacy-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0.25rem 0 0;
  padding-left: 1.5rem;
}

.btn-create {
  background: linear-gradient(135deg, #F9A8C8, #E87A9F);
  border: none;
}

/* 详情对话框 */
.album-detail {
  padding: 0.5rem 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.detail-stats {
  display: flex;
  gap: 1.5rem;
}

.detail-stat {
  display: flex;
  align-items: baseline;
  gap: 0.375rem;
}

.stat-num {
  font-size: 1.5rem;
  font-weight: 700;
  color: #E87A9F;
}

.detail-actions {
  display: flex;
  gap: 0.75rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 1rem 0;
}

.section-icon {
  width: 1.125rem;
  height: 1.125rem;
  color: #E87A9F;
}

.detail-empty {
  padding: 3rem;
}

.empty-state {
  text-align: center;
}

.empty-state .empty-icon {
  width: 48px;
  height: 48px;
  color: var(--color-text-muted);
  margin-bottom: 1rem;
}

.empty-state p {
  font-size: 1rem;
  color: var(--color-text-secondary);
  margin: 0 0 0.25rem;
}

.empty-state span {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.detail-media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
}

.detail-media-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 0.625rem;
  overflow: hidden;
  cursor: pointer;
  background: var(--color-bg-tertiary);
}

.detail-media-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.detail-media-item:hover img {
  transform: scale(1.1);
}

.media-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0.5rem;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  opacity: 0;
  transition: opacity 0.2s ease;
}

.detail-media-item:hover .media-overlay {
  opacity: 1;
}

.media-name {
  margin: 0;
  font-size: 0.6875rem;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .album-grid {
    grid-template-columns: 1fr;
  }
  
  .detail-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .album-card-actions {
    flex-wrap: wrap;
  }
  
  .action-btn.primary {
    width: 100%;
  }
}

/* 深色主题适配 */
:global(.dark) .album-card-cover {
  background: linear-gradient(135deg, #1a1a2e, #252538);
}

:global(.dark) .placeholder-icon {
  color: #4a4a5c;
}

:global(.dark) .page-title {
  background: linear-gradient(135deg, #ffffff, #F9A8C8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>
