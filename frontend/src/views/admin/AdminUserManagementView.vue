<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElOption } from 'element-plus'
import {
  fetchAdminUsers,
  updateAdminUserLimits,
  updateAdminUserRole,
  deleteAdminUser,
} from '../../services/adminUsers'

const loading = ref(false)
const keyword = ref('')
const users = ref([])
const pageSizeOptions = [10, 15, 50]
const pageSize = ref(15)
const currentPage = ref(1)
const jumpPageInput = ref('')

const roleOptions = [
  { value: 'ADMIN', label: '管理员', description: '可进入运营后台、上传、点赞' },
  { value: 'USER', label: '用户', description: '可上传、点赞，无法访问管理端' },
  { value: 'GUEST', label: '游客', description: '仅可点赞，禁止上传' },
]

const derivePrimaryRole = (roles = []) => {
  if (!Array.isArray(roles)) return 'USER'
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('USER')) return 'USER'
  return 'GUEST'
}

const decorateUser = (user) => ({
  ...user,
  editDailyLimit: user.dailyUploadLimit ?? null,
  editStorageQuotaMb: user.storageQuotaMb ?? null,
  selectedRole: derivePrimaryRole(user.roles),
})

const syncUser = (target, payload) => {
  Object.assign(target, decorateUser(payload))
}

const filteredUsers = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return users.value
  return users.value.filter((user) => {
    return [user.username, user.displayName, user.email]
      .filter(Boolean)
      .some((field) => field.toLowerCase().includes(term))
  })
})

const totalItems = computed(() => filteredUsers.value.length)
const totalPages = computed(() => Math.max(Math.ceil(totalItems.value / pageSize.value) || 1, 1))
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

watch([filteredUsers, pageSize], () => {
  const maxPage = Math.max(Math.ceil(totalItems.value / pageSize.value) || 1, 1)
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
  if (currentPage.value < 1) {
    currentPage.value = 1
  }
})

watch(keyword, () => {
  currentPage.value = 1
})

watch(pageSize, () => {
  currentPage.value = 1
})

const toNullableNumber = (value) => {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return Number(value)
}

const loadUsers = async () => {
  loading.value = true
  try {
    const { data } = await fetchAdminUsers()
    users.value = (data || []).map((item) => decorateUser(item))
    currentPage.value = 1
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const changePageSize = (value) => {
  pageSize.value = value
}

const goToPreviousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value -= 1
  }
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1
  }
}

const applyJump = () => {
  const parsed = Number(jumpPageInput.value)
  if (!parsed || Number.isNaN(parsed)) {
    jumpPageInput.value = ''
    return
  }
  const target = Math.min(Math.max(Math.floor(parsed), 1), totalPages.value)
  currentPage.value = target
  jumpPageInput.value = ''
}

const handleSaveLimits = async (user) => {
  try {
    const { data } = await updateAdminUserLimits(user.id, {
      dailyUploadLimit: toNullableNumber(user.editDailyLimit),
      storageQuotaMb: toNullableNumber(user.editStorageQuotaMb),
    })
    syncUser(user, data)
    ElMessage.success('配额已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存配额失败')
  }
}

const handleRoleChange = async (user, role) => {
  try {
    const { data } = await updateAdminUserRole(user.id, { role })
    syncUser(user, data)
    ElMessage.success('角色已更新')
  } catch (error) {
    user.selectedRole = derivePrimaryRole(user.roles)
    ElMessage.error(error?.response?.data?.message || '更新角色失败')
  }
}

const handleDelete = async (user) => {
  try {
    await ElMessageBox.confirm(`确定删除「${user.displayName}」吗？该操作无法撤销。`, '删除用户', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAdminUser(user.id)
    users.value = users.value.filter((item) => item.id !== user.id)
    ElMessage.success('用户已删除')
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

const formatBytes = (value) => {
  if (!value || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

const formatDate = (value) => {
  if (!value) return '—'
  return new Date(value).toLocaleDateString()
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="space-y-6">
    <header>
      <p class="admin-section-subtitle">members</p>
      <h1 class="admin-section-title">用户管理</h1>
      <p class="admin-section-description">
        列表视图展示全部成员，支持搜索 / 调整角色 / 配额与删除操作，默认新注册用户享有总数量 100 张、空间 200MB 配额。
      </p>
    </header>

    <div class="admin-table-shell">
      <div class="table-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          clearable
          class="max-w-md"
          @keyup.enter.native="loadUsers"
        />
        <el-button class="refresh-btn" :loading="loading" @click="loadUsers">刷新</el-button>
      </div>

      <div class="user-table-surface">
        <div class="user-table-scroll">
          <div class="user-table-minwidth">
            <el-table
              :data="paginatedUsers"
              v-loading="loading"
              row-key="id"
              border
              stripe
              class="user-table"
              empty-text="暂无用户，等待注册"
            >
            <el-table-column label="用户" min-width="260">
              <template #default="{ row }">
                <div class="user-cell">
                  <img
                    v-if="row.avatarUrl"
                    :src="row.avatarUrl"
                    alt="avatar"
                    class="avatar"
                  />
                  <div v-else class="avatar placeholder">{{ row.displayName?.slice(0, 1) || 'U' }}</div>
                  <div>
                    <p class="font-semibold text-white">{{ row.displayName }}</p>
                    <p class="text-sm text-white/60">{{ row.username }} · {{ row.email || '未绑定邮箱' }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="角色" width="200">
              <template #default="{ row }">
                <el-select v-model="row.selectedRole" size="small" @change="(value) => handleRoleChange(row, value)">
                  <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <p class="text-xs text-white/50">{{ roleOptions.find((item) => item.value === row.selectedRole)?.description }}</p>
              </template>
            </el-table-column>

            <el-table-column label="配额 (总数量 / 空间MB)" width="260">
              <template #default="{ row }">
                <div class="quota-editor">
                  <el-input-number
                    v-model="row.editDailyLimit"
                    :min="0"
                    :max="10000"
                    size="small"
                    placeholder="不限"
                  />
                  <el-input-number
                    v-model="row.editStorageQuotaMb"
                    :min="0"
                    :max="102400"
                    size="small"
                    placeholder="不限"
                  />
                </div>
                <p class="text-xs text-white/50">0 或留空表示不限</p>
                <p class="text-xs text-white/60">
                  已上传 {{ row.uploadCount }} 张 · 已占用 {{ formatBytes(row.storageBytes) }}
                </p>
              </template>
            </el-table-column>

            <el-table-column label="使用情况" min-width="240">
              <template #default="{ row }">
                <p class="text-white">累计上传 {{ row.uploadCount }} 个媒体 · 点赞 {{ row.likeCount }}</p>
                <p class="text-sm text-white/60">存储 {{ formatBytes(row.storageBytes) }}</p>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="180">
              <template #default="{ row }">
                <el-tag size="small" :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag>
                <p class="text-xs text-white/50">创建于 {{ formatDate(row.createdAt) }}</p>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <div class="flex gap-2">
                  <el-button size="small" type="primary" plain @click="() => handleSaveLimits(row)">保存配额</el-button>
                  <el-button size="small" type="danger" plain @click="() => handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
      <div class="user-pagination-panel">
        <div class="pagination-info">
          <span>当前第 <strong>{{ currentPage }}</strong> / {{ totalPages }} 页</span>
          <span>共 {{ totalItems }} 条</span>
          <span class="pagination-size">
            每页
            <el-select v-model="pageSize" size="small" class="page-size-select" @change="changePageSize">
              <el-option v-for="option in pageSizeOptions" :key="option" :label="`${option} 条`" :value="option" />
            </el-select>
            条
          </span>
        </div>
        <div class="pagination-actions">
          <el-button size="small" class="ghost-btn" :disabled="currentPage === 1" @click="goToPreviousPage">上一页</el-button>
          <el-button size="small" class="ghost-btn" :disabled="currentPage === totalPages" @click="goToNextPage">下一页</el-button>
          <div class="jump-control">
            <span>跳转到</span>
            <el-input
              v-model="jumpPageInput"
              size="small"
              class="jump-input"
              type="number"
              placeholder="页码"
              @keyup.enter.native="applyJump"
            />
            <el-button size="small" type="primary" plain @click="applyJump">确认</el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-section-subtitle {
  font-size: 0.9rem;
  color: var(--text-soft);
  letter-spacing: 0.35em;
  text-transform: uppercase;
}

.admin-section-title {
  margin: 0.25rem 0 0;
  font-size: clamp(2rem, 4vw, 2.75rem);
  font-weight: 600;
  color: var(--color-text-primary);
}

.admin-section-description {
  margin-top: 0.75rem;
  color: var(--text-muted);
  font-size: 0.95rem;
  max-width: 720px;
}

.admin-table-shell {
  border-radius: 1.75rem;
  border: 1px solid var(--admin-panel-border);
  background: var(--admin-panel-bg);
  box-shadow: var(--admin-panel-shadow);
  padding: 1.5rem;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.refresh-btn {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1.25rem;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: var(--color-on-accent);
  font-weight: 600;
}

.admin-table-shell :deep(.el-input__wrapper),
.admin-table-shell :deep(.el-select .el-input__wrapper),
.admin-table-shell :deep(.el-input-number .el-input__wrapper) {
  background: color-mix(in srgb, var(--admin-panel-bg) 85%, transparent);
  border: 1px solid var(--admin-panel-border);
  box-shadow: none;
  color: var(--color-text-primary);
}

:global(.dark) .admin-table-shell :deep(.el-input__wrapper),
:global(.dark) .admin-table-shell :deep(.el-select .el-input__wrapper),
:global(.dark) .admin-table-shell :deep(.el-input-number .el-input__wrapper) {
  background: color-mix(in srgb, var(--color-bg-primary) 85%, transparent);
}

.admin-table-shell :deep(.el-input__inner),
.admin-table-shell :deep(.el-input-number__inner) {
  color: var(--color-text-primary);
}

.admin-table-shell :deep(.el-select-dropdown) {
  background: var(--admin-panel-bg);
  border: 1px solid var(--admin-panel-border);
  color: var(--color-text-primary);
}

.user-table-surface {
  --table-header: color-mix(in srgb, var(--admin-panel-bg) 92%, transparent);
  --table-border: color-mix(in srgb, var(--admin-panel-border) 85%, transparent);
  --table-row: color-mix(in srgb, var(--admin-panel-bg) 98%, transparent);
  --table-row-alt: color-mix(in srgb, var(--admin-panel-bg) 92%, transparent);
  --table-hover: color-mix(in srgb, var(--color-brand-primary) 6%, transparent);
  border-radius: 1.5rem;
  border: 1px solid var(--admin-panel-border);
  background: var(--admin-panel-bg);
  overflow: hidden;
}

:global(.dark) .user-table-surface {
  --table-header: color-mix(in srgb, var(--color-bg-primary) 90%, transparent);
  --table-border: color-mix(in srgb, var(--admin-panel-border) 80%, transparent);
  --table-row: color-mix(in srgb, var(--color-bg-primary) 85%, transparent);
  --table-row-alt: color-mix(in srgb, var(--color-bg-primary) 78%, transparent);
  --table-hover: color-mix(in srgb, var(--color-brand-primary) 10%, transparent);
}

.user-table {
  width: 100%;
  background: transparent;
}

.user-table-surface :deep(.el-table__header-wrapper th),
.user-table-surface :deep(.el-table__fixed-header-wrapper th) {
  background: var(--table-header) !important;
  border-color: var(--table-border) !important;
  color: var(--color-text-secondary);
  font-weight: 600;
}

.user-table-surface :deep(.el-table__cell) {
  background: transparent !important;
  border-color: var(--table-border);
  color: var(--color-text-primary);
}

.user-table-surface :deep(.el-table__row) {
  background: var(--table-row);
}

.user-table-surface :deep(.el-table__row.el-table__row--striped) {
  background: var(--table-row-alt);
}

.user-table-surface :deep(.el-table__body tr:hover > td) {
  background: var(--table-hover) !important;
}

.user-table-scroll {
  max-height: clamp(320px, 60vh, 720px);
  overflow: auto;
}

.user-table-scroll::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.user-table-scroll::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--color-brand-primary) 50%, transparent);
  border-radius: 999px;
}

.user-table-minwidth {
  min-width: 1200px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid var(--admin-panel-border);
}

.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--color-text-primary);
  background: var(--admin-accent-soft);
}

.quota-editor {
  display: flex;
  gap: 0.5rem;
}

.user-pagination-panel {
  margin-top: 1.5rem;
  padding: 1rem 1.5rem;
  border-radius: 1.5rem;
  border: 1px solid var(--admin-panel-border);
  background: var(--admin-panel-bg);
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
}

.pagination-info {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  color: var(--text-soft);
  font-size: 0.95rem;
}

.pagination-info strong {
  color: var(--color-text-primary);
}

.pagination-size {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.page-size-select {
  width: 110px;
}

.pagination-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.jump-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-soft);
}

.jump-input {
  width: 96px;
}

.ghost-btn {
  border-color: var(--admin-panel-border);
  color: var(--color-text-secondary);
  background: color-mix(in srgb, var(--admin-panel-bg) 80%, transparent);
}

.ghost-btn:hover:not(:disabled) {
  border-color: var(--color-brand-primary);
  color: var(--color-text-primary);
}

.ghost-btn:disabled {
  opacity: 0.45;
}

:deep(.user-table [class~="text-white"]) {
  color: var(--color-text-primary) !important;
}

:deep(.user-table [class~="text-white/60"]) {
  color: var(--text-soft) !important;
}

:deep(.user-table [class~="text-white/50"]) {
  color: var(--text-muted) !important;
}

:deep(.user-table [class~="text-white/40"]) {
  color: var(--text-faint) !important;
}

@media (max-width: 1024px) {
  .user-table-minwidth {
    min-width: 960px;
  }

  .user-pagination-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .pagination-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
