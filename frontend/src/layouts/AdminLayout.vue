<script setup>
import { computed, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Home, LogOut, User, ChevronLeft, ChevronRight, Activity, LayoutDashboard, Image, Users, Settings, Database, Megaphone, Mail, Shield, Link2 } from 'lucide-vue-next'
import { useUiStore } from '../stores/ui'
import { useAuthStore } from '../stores/auth'
import { usePendingChangesStore } from '../stores/pendingChanges'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'

const auth = useAuthStore()
const navigation = [
  { name: 'admin-dashboard', label: '总览控制台', icon: LayoutDashboard },
  { name: 'admin-images', label: '媒体管理', icon: Image },
  { name: 'admin-users', label: '用户管理', icon: Users },
  { name: 'admin-system', label: '系统配置', icon: Settings },
  { name: 'admin-storage', label: '存储策略', icon: Database },
  { name: 'admin-announcements', label: '公告管理', icon: Megaphone },
  { name: 'admin-mail-settings', label: '邮件设置', icon: Mail },
  { name: 'admin-security', label: '权限与安全', icon: Shield },
  { name: 'admin-integration', label: '集成与 API', icon: Link2 },
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
          'admin-sidebar',
          isCollapsed ? 'admin-sidebar--collapsed' : '',
        ]"
      >
        <!-- Brand Section -->
        <div class="admin-sidebar__brand">
          <div class="admin-brand-avatar">
            CX
          </div>
          <div v-if="!isCollapsed" class="admin-brand-text">
            <p class="admin-brand-meta">ASTRNEST</p>
            <p class="admin-brand-name">辰汐</p>
          </div>
          <button 
            class="admin-toggle-btn" 
            :class="{ 'admin-toggle-btn--collapsed': isCollapsed }"
            @click="uiStore.toggleSidebar()"
            :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'"
          >
            <ChevronLeft v-if="!isCollapsed" class="h-4 w-4" />
            <ChevronRight v-else class="h-4 w-4" />
          </button>
        </div>

        <!-- Navigation -->
        <nav class="admin-nav">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="{ name: item.name }"
            class="admin-nav-link"
            :class="[
              route.name === item.name ? 'admin-nav-link--active' : '',
              isCollapsed ? 'admin-nav-link--collapsed' : ''
            ]"
            :title="isCollapsed ? item.label : ''"
          >
            <component :is="item.icon" class="admin-nav-icon" />
            <span v-if="!isCollapsed" class="admin-nav-label">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <!-- Status Card -->
        <div 
          class="admin-status-card" 
          :class="{ 'admin-status-card--collapsed': isCollapsed }"
        >
          <div class="admin-status-header">
            <Activity class="admin-status-icon" :class="{ 'admin-status-icon--collapsed': isCollapsed }" />
            <span v-if="!isCollapsed" class="admin-status-label">系统状态</span>
          </div>
          <template v-if="!isCollapsed">
            <p class="admin-status-title">服务运行正常</p>
            <p class="admin-status-meta">Ubuntu 24 · Java 21 · Vue 3</p>
          </template>
          <div v-else class="admin-status-dot" title="服务运行正常"></div>
        </div>
      </aside>

      <!-- Main Content -->
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

/* Sidebar Base Styles */
.admin-sidebar {
  width: 16rem;
  background: var(--admin-panel-bg);
  border-right: 1px solid var(--admin-panel-border);
  box-shadow: var(--admin-panel-shadow);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), padding 0.3s ease;
  flex-shrink: 0;
}

/* Collapsed Sidebar */
.admin-sidebar--collapsed {
  width: 5rem;
  padding: 1.5rem 0.75rem;
}

/* Brand Section */
.admin-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  position: relative;
}

.admin-sidebar--collapsed .admin-sidebar__brand {
  justify-content: center;
  gap: 0;
}

.admin-brand-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 2.75rem;
  width: 2.75rem;
  border-radius: 0.875rem;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: var(--color-on-accent);
  font-weight: 700;
  font-size: 0.875rem;
  box-shadow: 0 4px 14px rgba(var(--color-brand-primary-rgb, 127, 123, 255), 0.35);
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.admin-brand-avatar:hover {
  transform: scale(1.05);
}

.admin-brand-text {
  flex: 1;
  overflow: hidden;
  transition: opacity 0.2s ease, width 0.2s ease;
}

.admin-sidebar--collapsed .admin-brand-text {
  opacity: 0;
  width: 0;
  flex: 0;
}

.admin-brand-meta {
  margin: 0;
  font-size: 0.625rem;
  letter-spacing: 0.15em;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  white-space: nowrap;
}

.admin-brand-name {
  margin: 0.125rem 0 0;
  font-size: 1rem;
  font-weight: 600;
  white-space: nowrap;
}

/* Toggle Button */
.admin-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-panel-border);
  background: var(--admin-surface-bg);
  color: var(--color-text-secondary);
  border-radius: 0.5rem;
  width: 2rem;
  height: 2rem;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.admin-toggle-btn:hover {
  color: var(--color-brand-primary);
  border-color: var(--color-brand-primary);
  background: var(--admin-accent-soft);
}

.admin-toggle-btn--collapsed {
  position: absolute;
  right: -0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--color-brand-primary);
  color: white;
  border: none;
  box-shadow: 0 2px 8px rgba(var(--color-brand-primary-rgb, 127, 123, 255), 0.4);
}

.admin-toggle-btn--collapsed:hover {
  background: var(--color-brand-accent);
  color: white;
  transform: translateY(-50%) scale(1.1);
}

/* Navigation */
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  overflow-y: auto;
}

.admin-nav-link {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.75rem 1rem;
  border-radius: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: 1px solid transparent;
  transition: all 0.2s ease;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
}

.admin-nav-link:hover {
  color: var(--color-text-primary);
  background: var(--admin-accent-soft);
  border-color: var(--admin-panel-border);
}

.admin-nav-link--active {
  background: linear-gradient(135deg, rgba(var(--color-brand-primary-rgb, 127, 123, 255), 0.12), rgba(var(--color-brand-accent-rgb, 255, 95, 143), 0.08));
  color: var(--color-brand-primary);
  border-color: rgba(var(--color-brand-primary-rgb, 127, 123, 255), 0.3);
}

.admin-nav-link--collapsed {
  justify-content: center;
  padding: 0.75rem;
}

.admin-nav-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

.admin-nav-label {
  font-size: 0.875rem;
  transition: opacity 0.2s ease;
}

/* Status Card */
.admin-status-card {
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid var(--admin-panel-border);
  background: linear-gradient(135deg, rgba(var(--color-brand-primary-rgb, 127, 123, 255), 0.08), rgba(var(--color-brand-accent-rgb, 255, 95, 143), 0.05));
  transition: all 0.3s ease;
}

.admin-status-card--collapsed {
  padding: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-status-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.admin-status-card--collapsed .admin-status-header {
  margin-bottom: 0;
}

.admin-status-icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-brand-primary);
  flex-shrink: 0;
}

.admin-status-icon--collapsed {
  width: 1.25rem;
  height: 1.25rem;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.admin-status-label {
  margin: 0;
  font-size: 0.625rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-text-secondary);
}

.admin-status-title {
  margin: 0.25rem 0 0;
  font-size: 0.9375rem;
  font-weight: 600;
}

.admin-status-meta {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  color: var(--text-soft);
}

.admin-status-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  animation: pulse 2s ease-in-out infinite;
}

/* Main Content */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--admin-surface-bg);
  min-width: 0;
  overflow: hidden;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  background: var(--admin-header-bg);
  border-bottom: 1px solid var(--admin-header-border);
  flex-shrink: 0;
}

.admin-header__meta {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.admin-header__title {
  margin: 0.125rem 0 0;
  font-size: 1.375rem;
  font-weight: 600;
}

.admin-header__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.chenxi-home-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 999px;
  padding: 0.5rem 1rem;
  font-weight: 500;
  font-size: 0.875rem;
  border: 1px solid var(--admin-panel-border);
  color: var(--color-text-secondary);
  transition: all 0.2s ease;
  text-decoration: none;
}

.chenxi-home-btn:hover {
  color: var(--color-brand-primary);
  border-color: var(--color-brand-primary);
  background: var(--admin-accent-soft);
}

.chenxi-user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 2.25rem;
  width: 2.25rem;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: var(--color-on-accent);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.chenxi-user-avatar:hover {
  transform: scale(1.05);
}

.admin-dropdown-menu {
  background: var(--admin-panel-bg);
  border: 1px solid var(--admin-panel-border);
  box-shadow: var(--admin-panel-shadow);
  padding: 0.25rem 0;
  border-radius: 0.75rem;
}

.admin-dropdown-item {
  color: var(--color-text-secondary);
  padding: 0.5rem 1rem;
}

.admin-dropdown-item:hover {
  background: var(--admin-accent-soft);
  color: var(--color-text-primary);
}

.admin-dropdown-item--danger {
  color: #f87171;
}

.admin-dropdown-item--danger:hover {
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
}

.admin-dropdown-divider {
  border-color: var(--admin-panel-border);
  margin: 0.25rem 0;
}

.admin-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
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

/* Responsive */
@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 100;
    transform: translateX(-100%);
  }
  
  .admin-sidebar--collapsed {
    transform: translateX(0);
    width: 16rem;
  }
  
  .admin-sidebar:not(.admin-sidebar--collapsed) {
    transform: translateX(0);
  }
}
</style>
