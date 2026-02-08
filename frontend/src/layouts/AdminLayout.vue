<script setup>
import { computed, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Home, LogOut, User } from 'lucide-vue-next'
import { useUiStore } from '../stores/ui'
import { useAuthStore } from '../stores/auth'
import { usePendingChangesStore } from '../stores/pendingChanges'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'

const auth = useAuthStore()
const navigation = [
  { name: 'admin-dashboard', label: '总览控制台', icon: '🏠' },
  { name: 'admin-images', label: '媒体管理', icon: '🖼️' },
  { name: 'admin-users', label: '用户管理', icon: '👥' },
  { name: 'admin-system', label: '系统配置', icon: '⚙️' },
  { name: 'admin-storage', label: '存储策略', icon: '🗄️' },
  { name: 'admin-announcements', label: '公告管理', icon: '📢' },
  { name: 'admin-mail-settings', label: '邮件设置', icon: '✉️' },
  { name: 'admin-security', label: '权限与安全', icon: '🛡️' },
  { name: 'admin-integration', label: '集成与 API', icon: '🔗' },
]

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const pendingChanges = usePendingChangesStore()

watch(
  () => route.name,
  (name) => {
    if (name) {
      uiStore.rememberRoute(name)
    }
  },
  { immediate: true }
)

const handleLogout = () => {
  if (pendingChanges.hasAdminSystemConfigChanges) {
    const confirmed = window.confirm('您所做的更改可能尚未保存，确认退出登录吗？')
    if (!confirmed) {
      return
    }
  }
  auth.logout()
  pendingChanges.resetAll()
  ElMessage.success('登出成功')
  router.replace({ path: '/', query: { login: '1' } })
}

const isCollapsed = computed(() => uiStore.isSidebarCollapsed)
</script>

<template>
  <div class="admin-shell">
    <div class="admin-layout">
      <aside
        :class="[
          'admin-sidebar transition-all duration-300',
          isCollapsed ? 'admin-sidebar--collapsed' : '',
        ]"
      >
        <div class="admin-sidebar__brand">
          <div class="admin-brand-avatar">
            CX
          </div>
          <div v-if="!isCollapsed" class="admin-brand-text">
            <p class="admin-brand-meta">ASTRNEST</p>
            <p class="admin-brand-name">辰汐</p>
          </div>
          <button class="admin-toggle-btn" @click="uiStore.toggleSidebar()">
            ⇔
          </button>
        </div>

        <nav class="admin-nav">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="{ name: item.name }"
            class="admin-nav-link"
            :class="route.name === item.name ? 'admin-nav-link--active' : ''"
          >
            <span class="text-lg">{{ item.icon }}</span>
            <span v-if="!isCollapsed" class="text-sm font-medium">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="admin-status-card" :class="{ 'items-center text-center': isCollapsed }">
          <p class="admin-status-label">状态</p>
          <p class="admin-status-title">服务运行正常</p>
          <p v-if="!isCollapsed" class="admin-status-meta">Ubuntu 24 · Java 21 · Vue 3</p>
        </div>
      </aside>

      <div class="admin-main">
        <header class="admin-header">
          <div>
            <p class="admin-header__meta">{{ route.meta.label }}</p>
            <h1 class="admin-header__title">辰汐运营面板</h1>
          </div>
          <div class="admin-header__actions">
            <RouterLink to="/" class="chenxi-home-btn">
              <Home class="h-4 w-4" />
              <span>首页</span>
            </RouterLink>
            <el-dropdown class="chenxi-user-dropdown" trigger="hover">
              <div class="chenxi-user-avatar">
                <User class="h-5 w-5" />
              </div>
              <template #dropdown>
                <el-dropdown-menu class="admin-dropdown-menu">
                  <RouterLink to="/user" class="block w-full">
                    <el-dropdown-item class="admin-dropdown-item">
                      用户中心
                    </el-dropdown-item>
                  </RouterLink>
                  <el-divider class="admin-dropdown-divider" />
                  <el-dropdown-item class="admin-dropdown-item admin-dropdown-item--danger" @click="handleLogout">
                    <LogOut class="mr-2 h-4 w-4" />
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <main class="admin-content">
          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: var(--admin-surface-bg);
  color: var(--color-text-primary);
  transition: background-color 0.4s ease, color 0.4s ease;
}

.admin-layout {
  display: flex;
  min-height: 100vh;
}

.admin-sidebar {
  width: 16rem;
  background: var(--admin-panel-bg);
  border-right: 1px solid var(--admin-panel-border);
  box-shadow: var(--admin-panel-shadow);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
}

.admin-sidebar--collapsed {
  width: 5.5rem;
}

.admin-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.admin-brand-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 3rem;
  width: 3rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: var(--color-on-accent);
  font-weight: 700;
  box-shadow: var(--admin-panel-shadow);
}

.admin-brand-text {
  flex: 1;
}

.admin-brand-meta {
  margin: 0;
  font-size: 0.65rem;
  letter-spacing: 0.2em;
  color: var(--color-text-secondary);
  text-transform: uppercase;
}

.admin-brand-name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.admin-toggle-btn {
  margin-left: auto;
  border: 1px solid var(--admin-panel-border);
  background: transparent;
  color: var(--color-text-secondary);
  border-radius: 999px;
  width: 2.5rem;
  height: 2.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.admin-toggle-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-brand-primary);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.admin-nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 1.2rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  border: 1px solid transparent;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.admin-nav-link:hover {
  color: var(--color-text-primary);
  border-color: var(--admin-panel-border);
}

.admin-nav-link--active {
  border-color: var(--admin-panel-border);
  background: var(--admin-accent-soft);
  color: var(--color-text-primary);
}

.admin-status-card {
  padding: 1.25rem;
  border-radius: 1.5rem;
  border: 1px solid var(--admin-panel-border);
  background: linear-gradient(135deg, rgba(127, 123, 255, 0.14), rgba(255, 95, 143, 0.08));
  box-shadow: var(--admin-panel-shadow);
}

:global(.dark) .admin-status-card {
  background: linear-gradient(135deg, rgba(93, 105, 255, 0.32), rgba(255, 111, 177, 0.18));
}

.admin-status-label {
  margin: 0;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.35em;
  color: var(--color-text-secondary);
}

.admin-status-title {
  margin: 0.4rem 0 0;
  font-size: 1.15rem;
  font-weight: 600;
}

.admin-status-meta {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
  color: var(--text-soft);
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--admin-surface-bg);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem 2rem;
  background: var(--admin-header-bg);
  border-bottom: 1px solid var(--admin-header-border);
}

.admin-header__meta {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.admin-header__title {
  margin: 0.25rem 0 0;
  font-size: 1.65rem;
  font-weight: 600;
}

.admin-header__actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chenxi-home-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 999px;
  padding: 0.65rem 1.4rem;
  font-weight: 600;
  border: 1px solid var(--admin-panel-border);
  color: var(--color-text-secondary);
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.chenxi-home-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-brand-primary);
  background: var(--admin-accent-soft);
}

.chenxi-user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 2.5rem;
  width: 2.5rem;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: var(--color-on-accent);
}

.admin-dropdown-menu {
  background: var(--admin-panel-bg);
  border: 1px solid var(--admin-panel-border);
  box-shadow: var(--admin-panel-shadow);
  padding: 0.25rem 0;
}

.admin-dropdown-item {
  color: var(--color-text-secondary);
}

.admin-dropdown-item:hover {
  background: var(--admin-accent-soft);
  color: var(--color-text-primary);
}

.admin-dropdown-item--danger {
  color: #f87171;
}

.admin-dropdown-divider {
  border-color: var(--admin-panel-border);
}

.admin-content {
  flex: 1;
  padding: 2rem;
}

:deep(.admin-content) {
  color: var(--color-text-primary);
}

:deep(.admin-content [class~="text-white"]),
:deep(.admin-content [class~="text-white/90"]) {
  color: var(--color-text-primary) !important;
}

:deep(.admin-content [class~="text-white/80"]),
:deep(.admin-content [class~="text-white/70"]) {
  color: var(--color-text-secondary) !important;
}

:deep(.admin-content [class~="text-white/60"]) {
  color: var(--text-soft) !important;
}

:deep(.admin-content [class~="text-white/50"]),
:deep(.admin-content [class~="text-white/40"]) {
  color: var(--text-muted) !important;
}

:deep(.admin-content [class~="text-white/30"]),
:deep(.admin-content [class~="text-white/20"]) {
  color: var(--text-faint) !important;
}
</style>
