<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Menu, X } from 'lucide-vue-next'

import ThemeSwitcher from './ThemeSwitcher.vue'
import LoginModal from '../chenxi/LoginModal.vue'
import LogoSvg from './LogoSvg.vue'
import { useAuthStore } from '../../stores/auth'

const showLoginModal = ref(false)
const isMobileMenuOpen = ref(false)

const openLoginModal = () => {
  showLoginModal.value = true
}

const closeLoginModal = () => {
  showLoginModal.value = false
}

const FALLBACK_TABS = Object.freeze([
  { name: 'user-home', label: '仪表盘' },
  { name: 'user-images', label: '媒体管理' },
  { name: 'user-albums', label: '我的图集' },
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
  isMobileMenuOpen.value = false
  router.replace({ path: '/', query: { login: '1' } })
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}
</script>

<template>
  <header class="user-navbar">
    <div class="navbar-container">
      <!-- Logo 区域 -->
      <RouterLink to="/" class="logo-link">
        <div class="logo-badge">
          <LogoSvg :width="32" :height="32" />
        </div>
        <div class="logo-text">
          <span class="logo-eyebrow">member space</span>
          <span class="logo-title">AstrNest 控制台</span>
        </div>
      </RouterLink>

      <!-- 桌面端导航 -->
      <nav class="desktop-nav">
        <ThemeSwitcher v-if="showThemeToggle" />
        
        <RouterLink
          v-if="showHomeButton"
          :to="homeTo"
          class="nav-btn nav-btn-ghost"
        >
          {{ homeLabel }}
        </RouterLink>
        
        <button
          v-if="showAdminButton && auth.isAdmin"
          type="button"
          class="nav-btn nav-btn-ghost"
          @click="goAdmin"
        >
          进入后台
        </button>
        
        <!-- 未登录 -->
        <button
          v-if="!auth.isAuthenticated"
          type="button"
          class="nav-btn nav-btn-primary"
          @click="openLoginModal"
        >
          登录
        </button>
        
        <!-- 已登录 -->
        <el-dropdown v-else trigger="hover">
          <div class="user-avatar">
            <span v-if="auth.profile?.username || auth.profile?.nickname || auth.displayName">
              {{ userInitial }}
            </span>
            <User v-else class="user-icon" />
          </div>
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown-menu">
              <RouterLink
                v-for="tab in resolvedTabs"
                :key="tab.name"
                :to="{ name: tab.name }"
                @click="handleBeforeNavigate"
              >
                <el-dropdown-item>{{ tab.label }}</el-dropdown-item>
              </RouterLink>
              <el-divider />
              <el-dropdown-item class="logout-item" @click="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <!-- 移动端操作区 -->
      <div class="mobile-actions">
        <!-- 未登录：显示登录按钮 + 菜单按钮 -->
        <template v-if="!auth.isAuthenticated">
          <button type="button" class="mobile-login-btn" @click="openLoginModal">
            登录
          </button>
          <button type="button" class="mobile-menu-btn" @click="toggleMobileMenu">
            <Menu v-if="!isMobileMenuOpen" class="menu-icon" />
            <X v-else class="menu-icon" />
          </button>
        </template>
        
        <!-- 已登录：显示用户头像 + 菜单按钮 -->
        <template v-else>
          <el-dropdown trigger="click">
            <div class="user-avatar mobile-avatar">
              <span v-if="auth.profile?.username || auth.profile?.nickname || auth.displayName">
                {{ userInitial }}
              </span>
              <User v-else class="user-icon" />
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <RouterLink
                  v-for="tab in resolvedTabs"
                  :key="tab.name"
                  :to="{ name: tab.name }"
                  @click="handleBeforeNavigate"
                >
                  <el-dropdown-item>{{ tab.label }}</el-dropdown-item>
                </RouterLink>
                <el-divider />
                <el-dropdown-item class="logout-item" @click="logout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <button type="button" class="mobile-menu-btn" @click="toggleMobileMenu">
            <Menu v-if="!isMobileMenuOpen" class="menu-icon" />
            <X v-else class="menu-icon" />
          </button>
        </template>
      </div>
    </div>

    <!-- 移动端折叠菜单 -->
    <transition name="slide-down">
      <div v-if="isMobileMenuOpen" class="mobile-menu">
        <div class="mobile-menu-inner">
          <RouterLink 
            v-if="showHomeButton" 
            :to="homeTo" 
            class="mobile-menu-item"
            @click="isMobileMenuOpen = false"
          >
            <span class="menu-icon-emoji">🏠</span>
            <span>{{ homeLabel }}</span>
          </RouterLink>
          
          <button 
            v-if="showAdminButton && auth.isAdmin" 
            type="button"
            class="mobile-menu-item"
            @click="goAdmin(); isMobileMenuOpen = false"
          >
            <span class="menu-icon-emoji">⚙️</span>
            <span>进入后台</span>
          </button>
          
          <div class="mobile-menu-divider"></div>
          
          <template v-if="!auth.isAuthenticated">
            <button 
              type="button" 
              class="mobile-menu-btn-primary"
              @click="openLoginModal(); isMobileMenuOpen = false"
            >
              登录
            </button>
          </template>
          
          <template v-else>
            <RouterLink 
              :to="{ name: 'user-home' }" 
              class="mobile-menu-btn-primary"
              @click="isMobileMenuOpen = false"
            >
              会员中心
            </RouterLink>
            <button 
              type="button" 
              class="mobile-menu-btn-ghost"
              @click="logout(); isMobileMenuOpen = false"
            >
              退出登录
            </button>
          </template>
        </div>
      </div>
    </transition>
  </header>
  
  <!-- 登录弹窗 -->
  <LoginModal v-model:visible="showLoginModal" @login-success="closeLoginModal" />
</template>

<style scoped>
/* 基础样式 */
.user-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  border-bottom: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--color-bg-primary) 85%, transparent);
  backdrop-filter: blur(20px);
}

.navbar-container {
  max-width: 72rem;
  margin: 0 auto;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (min-width: 1024px) {
  .navbar-container {
    padding: 1rem 1.5rem;
  }
}

/* Logo 样式 */
.logo-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}

@media (min-width: 1024px) {
  .logo-link {
    gap: 1rem;
  }
}

.logo-badge {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  background: color-mix(in srgb, var(--color-text-primary) 6%, transparent);
  padding: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  transition: all 0.3s ease;
}

/* 暗色主题下 logo 容器更亮 */
:global(.dark) .logo-badge {
  background: color-mix(in srgb, rgba(255, 255, 255, 0.95) 12%, transparent);
}

@media (min-width: 768px) {
  .logo-badge {
    width: 3rem;
    height: 3rem;
    border-radius: 1rem;
    padding: 0.5rem;
  }
}

.logo-badge svg {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-text {
  display: none;
  flex-direction: column;
  line-height: 1.2;
}

@media (min-width: 640px) {
  .logo-text {
    display: flex;
  }
}

.logo-eyebrow {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.3em;
  color: var(--text-soft);
}

@media (min-width: 1024px) {
  .logo-eyebrow {
    font-size: 0.75rem;
    letter-spacing: 0.4em;
  }
}

.logo-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

@media (min-width: 1024px) {
  .logo-title {
    font-size: 1rem;
  }
}

/* 桌面端导航 - 默认隐藏，md 以上显示 */
.desktop-nav {
  display: none;
  align-items: center;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .desktop-nav {
    display: flex;
  }
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1.25rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.nav-btn-ghost {
  background: transparent;
  border: 1px solid var(--border-soft);
  color: var(--color-text-secondary);
}

.nav-btn-ghost:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

.nav-btn-primary {
  background: linear-gradient(135deg, #ff6b9d, #feca57);
  color: white;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.35);
}

.nav-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.45);
}

.dark .nav-btn-primary {
  background: linear-gradient(135deg, #e55a8a, #e5b54d);
  box-shadow: 0 4px 15px rgba(229, 90, 138, 0.35);
}

/* 用户头像 */
.user-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  color: white;
  font-weight: 600;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease;
  box-shadow: 0 2px 8px rgba(249, 168, 200, 0.4);
}

.user-avatar:hover {
  transform: scale(1.1);
}

.user-icon {
  width: 1.25rem;
  height: 1.25rem;
}

.mobile-avatar {
  width: 2.25rem;
  height: 2.25rem;
  font-size: 0.8125rem;
}

/* 移动端操作区 - 默认显示，md 以上隐藏 */
.mobile-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

@media (min-width: 768px) {
  .mobile-actions {
    display: none;
  }
}

.mobile-login-btn {
  padding: 0.375rem 1rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, #ff6b9d, #feca57);
  color: white;
  font-size: 0.8125rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 3px 12px rgba(255, 107, 157, 0.35);
}

.mobile-login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.45);
}

.mobile-menu-btn {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid var(--border-soft);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--color-text-secondary);
}

.mobile-menu-btn:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

.menu-icon {
  width: 1.25rem;
  height: 1.25rem;
}

/* 移动端菜单 */
.mobile-menu {
  background: color-mix(in srgb, var(--color-bg-primary) 98%, transparent);
  border-top: 1px solid var(--border-soft);
  backdrop-filter: blur(20px);
}

.mobile-menu-inner {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  text-decoration: none;
  text-align: left;
  transition: all 0.2s ease;
}

.mobile-menu-item:hover {
  background: rgba(255, 107, 157, 0.08);
  color: var(--color-brand-primary);
}

.menu-icon-emoji {
  font-size: 1.25rem;
  width: 1.5rem;
  text-align: center;
}

.mobile-menu-divider {
  height: 1px;
  background: var(--border-soft);
  margin: 0.25rem 0;
}

.mobile-menu-btn-primary {
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #ff6b9d, #feca57);
  color: white;
  font-size: 0.9375rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  text-align: center;
  text-decoration: none;
  transition: all 0.2s ease;
  box-shadow: 0 3px 12px rgba(255, 107, 157, 0.35);
}

.mobile-menu-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 16px rgba(255, 107, 157, 0.45);
}

.mobile-menu-btn-ghost {
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  background: transparent;
  border: 1px solid var(--border-soft);
  color: #f87171;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
}

.mobile-menu-btn-ghost:hover {
  background: rgba(248, 113, 113, 0.1);
  border-color: #f87171;
}

/* 动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 下拉菜单样式 */
:deep(.user-dropdown-menu) {
  min-width: 160px;
}

:deep(.logout-item) {
  color: #f87171;
}

:deep(.logout-item:hover) {
  color: #dc2626;
  background: rgba(248, 113, 113, 0.1);
}
</style>
