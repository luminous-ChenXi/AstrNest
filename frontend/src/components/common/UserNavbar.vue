<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from 'lucide-vue-next'

import ThemeSwitcher from './ThemeSwitcher.vue'
import LoginModal from '../chenxi/LoginModal.vue'
import { useAuthStore } from '../../stores/auth'
import siteLogo from '../../assets/img/favicon.png'

const showLoginModal = ref(false)

const openLoginModal = () => {
  showLoginModal.value = true
}

const closeLoginModal = () => {
  showLoginModal.value = false
}

const FALLBACK_TABS = Object.freeze([
  { name: 'user-home', label: '仪表盘' },
  { name: 'user-images', label: '媒体管理' },
  { name: 'user-profile', label: '资料信息' },
  { name: 'user-security', label: '安全设置' },
  { name: 'user-api', label: 'API 接口管理' },
])

const props = defineProps({
  tabs: {
    type: Array,
  },
  homeTo: {
    type: [String, Object],
    default: () => '/',
  },
  homeLabel: {
    type: String,
    default: '首页',
  },
  showHomeButton: {
    type: Boolean,
    default: true,
  },
  showAdminButton: {
    type: Boolean,
    default: true,
  },
  showThemeToggle: {
    type: Boolean,
    default: true,
  },
})

const auth = useAuthStore()
const router = useRouter()

const resolvedTabs = computed(() => (props.tabs?.length ? props.tabs : FALLBACK_TABS))

const userInitial = computed(() => {
  const source = auth.profile?.username || auth.profile?.nickname || auth.displayName || 'U'
  return String(source).charAt(0).toUpperCase()
})

const handleBeforeNavigate = () => {
  if (!auth.isAuthenticated) {
    ElMessage.warning('未登录，请登录后再操作')
  }
}

const goAdmin = () => {
  if (!props.showAdminButton) return
  router.push({ name: 'admin-dashboard' })
}

const logout = () => {
  auth.logout()
  ElMessage.success('登出成功')
  router.replace({ path: '/', query: { login: '1' } })
}
</script>

<template>
  <header class="user-navbar fixed inset-x-0 top-0 z-50">
    <div class="user-navbar__inner mx-auto flex max-w-6xl items-center justify-between px-3 py-3 lg:px-6 lg:py-4">
      <!-- Logo 区域 -->
      <RouterLink to="/" class="flex items-center gap-2 lg:gap-3">
        <div class="user-navbar__logo-badge">
          <img :src="siteLogo" alt="AstrNest 徽标" class="h-full w-full rounded-xl object-contain drop-shadow-lg" />
        </div>
        <div class="hidden sm:block">
          <p class="user-navbar__eyebrow">member space</p>
          <p class="text-sm font-semibold lg:text-base">AstrNest 控制台</p>
        </div>
      </RouterLink>

      <!-- 桌面端操作区 -->
      <div class="user-navbar__actions hidden md:flex">
        <ThemeSwitcher v-if="showThemeToggle" />
        <RouterLink
          v-if="showHomeButton"
          :to="homeTo"
          class="chenxi-home-btn flex items-center gap-2 rounded-full border border-body px-5 py-2 font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary"
        >
          <span>{{ homeLabel }}</span>
        </RouterLink>
        <button
          v-if="showAdminButton && auth.isAdmin"
          type="button"
          class="chenxi-admin-btn rounded-full border border-body px-5 py-2 font-semibold text-body-secondary transition hover:border-brand-primary hover:text-body-primary inline-flex"
          @click="goAdmin"
        >
          进入后台
        </button>
        
        <!-- 未登录显示登录按钮 -->
        <button
          v-if="!auth.isAuthenticated"
          type="button"
          class="btn-login-nav"
          @click="openLoginModal"
        >
          登录
        </button>
        
        <!-- 已登录显示用户头像 -->
        <el-dropdown v-else class="chenxi-user-dropdown" trigger="hover">
          <div
            class="chenxi-user-avatar flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-primary to-brand-accent text-white font-semibold transition-transform hover:scale-110"
          >
            <span v-if="auth.profile?.username || auth.profile?.nickname || auth.displayName" class="text-sm">
              {{ userInitial }}
            </span>
            <User v-else class="h-5 w-5" />
          </div>
          <template #dropdown>
            <el-dropdown-menu class="chenxi-dropdown-menu panel">
              <RouterLink
                v-for="tab in resolvedTabs"
                :key="tab.name"
                :to="{ name: tab.name }"
                class="block w-full"
                @click="handleBeforeNavigate"
              >
                <el-dropdown-item class="dropdown-link">
                  {{ tab.label }}
                </el-dropdown-item>
              </RouterLink>
              <el-divider class="dropdown-divider" />
              <el-dropdown-item class="dropdown-link dropdown-link--danger" @click="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 移动端操作区 -->
      <div class="flex items-center gap-2 md:hidden">
        <ThemeSwitcher v-if="showThemeToggle" />
        
        <!-- 未登录显示登录按钮 -->
        <button
          v-if="!auth.isAuthenticated"
          type="button"
          class="btn-login-nav-mobile"
          @click="openLoginModal"
        >
          登录
        </button>
        
        <!-- 已登录显示用户头像 -->
        <el-dropdown v-else class="chenxi-user-dropdown" trigger="click">
          <div
            class="chenxi-user-avatar-mobile flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-primary to-brand-accent text-white font-semibold"
          >
            <span v-if="auth.profile?.username || auth.profile?.nickname || auth.displayName" class="text-sm">
              {{ userInitial }}
            </span>
            <User v-else class="h-4 w-4" />
          </div>
          <template #dropdown>
            <el-dropdown-menu class="chenxi-dropdown-menu panel">
              <RouterLink
                v-for="tab in resolvedTabs"
                :key="tab.name"
                :to="{ name: tab.name }"
                class="block w-full"
                @click="handleBeforeNavigate"
              >
                <el-dropdown-item class="dropdown-link">
                  {{ tab.label }}
                </el-dropdown-item>
              </RouterLink>
              <el-divider class="dropdown-divider" />
              <el-dropdown-item class="dropdown-link dropdown-link--danger" @click="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
  
  <!-- 登录弹窗 -->
  <LoginModal v-model:visible="showLoginModal" @login-success="closeLoginModal" />
</template>

<style scoped>
.user-navbar {
  border-bottom: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--color-bg-primary) 82%, transparent);
  backdrop-filter: blur(24px);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.user-navbar__inner {
  color: var(--color-text-primary);
}

.user-navbar__logo-badge {
  display: flex;
  height: 3rem;
  width: 3rem;
  align-items: center;
  justify-content: center;
  border-radius: 1.25rem;
  background: color-mix(in srgb, var(--color-text-primary) 6%, transparent);
  padding: 0.5rem;
}

.user-navbar__eyebrow {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.4em;
  color: var(--text-soft);
}

.user-navbar__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
}

/* 导航栏登录按钮 */
.btn-login-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1.25rem;
  border-radius: 9999px;
  background: #F9A8C8;
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(249, 168, 200, 0.35);
}

.btn-login-nav:hover {
  background: #EC8DAD;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(249, 168, 200, 0.45);
}

.dark .btn-login-nav {
  background: #E87A9F;
  box-shadow: 0 4px 15px rgba(232, 122, 159, 0.35);
}

.dark .btn-login-nav:hover {
  background: #EC8DAD;
  box-shadow: 0 6px 20px rgba(232, 122, 159, 0.45);
}

/* 移动端登录按钮 */
.btn-login-nav-mobile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.375rem 1rem;
  border-radius: 9999px;
  background: #F9A8C8;
  color: white;
  font-size: 0.8125rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 3px 12px rgba(249, 168, 200, 0.35);
}

.btn-login-nav-mobile:hover {
  background: #EC8DAD;
  box-shadow: 0 4px 16px rgba(249, 168, 200, 0.45);
}

.dark .btn-login-nav-mobile {
  background: #E87A9F;
  box-shadow: 0 3px 12px rgba(232, 122, 159, 0.35);
}

.dark .btn-login-nav-mobile:hover {
  background: #EC8DAD;
  box-shadow: 0 4px 16px rgba(232, 122, 159, 0.45);
}

/* 移动端用户头像 */
.chenxi-user-avatar-mobile {
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(249, 168, 200, 0.4);
}

.dark .chenxi-user-avatar-mobile {
  box-shadow: 0 2px 8px rgba(232, 122, 159, 0.4);
}

:deep(.dropdown-link) {
  color: var(--text-muted);
  transition: color 0.2s ease, background-color 0.2s ease;
}

:deep(.dropdown-link:hover) {
  color: var(--color-text-primary);
  background: var(--panel-overlay);
}

:deep(.dropdown-link--danger) {
  color: #f87171;
}

:deep(.dropdown-link--danger:hover) {
  color: #dc2626;
  background: color-mix(in srgb, #f87171 12%, transparent);
}

:deep(.dropdown-divider) {
  border-color: var(--border-soft);
}
</style>
