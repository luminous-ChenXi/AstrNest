<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus'
import { usePreferredDark } from '@vueuse/core'
import { Sun, Moon } from 'lucide-vue-next'
import { useTheme } from '../../composables/useTheme'

const props = defineProps({
  title: { type: String, default: '辰汐安全云' },
  subtitle: { type: String, default: 'Chenxi Secure Access Layer' },
  badge: { type: String, default: 'CHENXI' },
})

const route = useRoute()

// 主题统一走全局 useTheme（<html> 上的 .dark 类），此处只做展示层联动
const { mode } = useTheme()
const preferredDark = usePreferredDark()
const isDark = computed(() => (mode.value === 'auto' ? preferredDark.value : mode.value === 'dark'))

// 手动切换主题（写入全局模式，全站生效）
const toggleTheme = () => {
  mode.value = isDark.value ? 'light' : 'dark'
}

onMounted(() => {
  // 兼容旧版本写入的私有主题键，迁移后移除
  if (localStorage.getItem('chenxi-theme') !== null) {
    mode.value = localStorage.getItem('chenxi-theme') === 'dark' ? 'dark' : 'light'
    localStorage.removeItem('chenxi-theme')
  }
})

const navLinks = [
  { to: { path: '/', query: { login: '1' } }, label: '登录' },
  { to: '/register', label: '注册' },
  { to: '/forgot-password', label: '找回密码' },
]

const activePath = computed(() => route.path)
</script>

<template>
  <div class="chenxi-auth-shell" :class="{ 'light': !isDark }">
    <!-- Background Effects -->
    <div class="bg-effects">
      <div class="bg-gradient-1" />
      <div class="bg-gradient-2" />
      <div class="bg-grid" />
      <div class="bg-glow" />
    </div>

    <!-- Theme Toggle -->
    <button class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
      <Sun v-if="isDark" class="theme-icon" />
      <Moon v-else class="theme-icon" />
    </button>

    <header class="shell-header">
      <div class="header-content">
        <div class="brand">
          <div class="brand-badge">
            {{ props.badge }}
          </div>
          <div class="brand-text">
            <p class="brand-subtitle">{{ props.subtitle }}</p>
            <p class="brand-title">{{ props.title }}</p>
          </div>
        </div>
        <nav class="nav-links">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link"
            :class="{ 'active': activePath === link.to }"
          >
            {{ link.label }}
          </RouterLink>
        </nav>
        <div class="header-actions">
          <RouterLink to="/" class="back-link">返回首页</RouterLink>
          <RouterLink :to="{ path: '/', query: { login: '1' } }">
            <ElButton type="primary" class="login-btn">
              控制台登录
            </ElButton>
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="shell-main">
      <section class="hero-section">
        <slot name="hero">
          <p class="hero-badge">Chenxi Access</p>
          <h1 class="hero-title">辰汐统一认证与内容治理门户</h1>
          <p class="hero-desc">
            使用邮箱 + 人机验证的零信任流程，自动接入会员内容存储、审计与 API 生态，体验极速上传与风控联动。
          </p>
        </slot>
      </section>
      <section class="content-section">
        <slot />
      </section>
    </main>

    <footer class="shell-footer">
      <div class="footer-content">
        <p class="footer-copyright">© {{ new Date().getFullYear() }} 辰汐内容安全团队 · All Rights Reserved</p>
        <div class="footer-links">
          <RouterLink to="/security">安全中心</RouterLink>
          <RouterLink to="/integration">开放平台</RouterLink>
          <a href="mailto:chenxi@luminouschenxi.net">chenxi@luminouschenxi.net</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ===== CSS Variables for Theming ===== */
.chenxi-auth-shell {
  /* Dark theme (default) */
  --bg-primary: #05060c;
  --bg-gradient-1: rgba(123, 132, 255, 0.35);
  --bg-gradient-2: rgba(255, 115, 161, 0.35);
  --grid-color: rgba(255, 255, 255, 0.03);
  --glow-opacity: 0.5;
  --text-primary: #ffffff;
  --text-secondary: rgba(255, 255, 255, 0.6);
  --text-muted: rgba(255, 255, 255, 0.5);
  --header-bg: linear-gradient(to right, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0));
  --header-border: rgba(255, 255, 255, 0.1);
  --nav-link-color: rgba(255, 255, 255, 0.6);
  --nav-link-hover: #ffffff;
  --nav-link-active-bg: rgba(255, 255, 255, 0.2);
  --footer-bg: rgba(0, 0, 0, 0.2);
  --footer-border: rgba(255, 255, 255, 0.1);
  --link-hover: #ffffff;
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
  --card-border: rgba(255, 255, 255, 0.1);
  --card-shadow: 0 25px 80px rgba(2, 6, 23, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.05) inset;
}

.chenxi-auth-shell.light {
  /* Light theme */
  --bg-primary: linear-gradient(135deg, #fafafa 0%, #f0f0f5 100%);
  --bg-gradient-1: rgba(244, 114, 182, 0.15);
  --bg-gradient-2: rgba(167, 139, 250, 0.15);
  --grid-color: rgba(0, 0, 0, 0.03);
  --glow-opacity: 0.3;
  --text-primary: #1a1a2e;
  --text-secondary: rgba(26, 26, 46, 0.6);
  --text-muted: rgba(26, 26, 46, 0.5);
  --header-bg: linear-gradient(to right, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.4));
  --header-border: rgba(0, 0, 0, 0.08);
  --nav-link-color: rgba(26, 26, 46, 0.6);
  --nav-link-hover: #1a1a2e;
  --nav-link-active-bg: rgba(244, 114, 182, 0.15);
  --footer-bg: rgba(255, 255, 255, 0.5);
  --footer-border: rgba(0, 0, 0, 0.08);
  --link-hover: #f472b6;
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  --card-border: rgba(0, 0, 0, 0.08);
  --card-shadow: 0 25px 80px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

/* ===== Base Styles ===== */
.chenxi-auth-shell {
  position: relative;
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.5s ease;
  overflow-x: hidden;
}

/* Theme Toggle Button */
.theme-toggle {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--card-shadow);
}

.theme-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 25px rgba(244, 114, 182, 0.3);
}

.theme-icon {
  width: 22px;
  height: 22px;
  color: var(--text-primary);
}

/* Background Effects */
.bg-effects {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.6;
}

.bg-gradient-1 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 18% 18%, var(--bg-gradient-1), transparent 45%);
  transition: all 0.5s ease;
}

.bg-gradient-2 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 82% 0%, var(--bg-gradient-2), transparent 38%);
  transition: all 0.5s ease;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--grid-color) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-color) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  transition: all 0.5s ease;
}

.bg-glow {
  position: absolute;
  width: 800px;
  height: 800px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(244, 114, 182, var(--glow-opacity)) 0%, transparent 70%);
  pointer-events: none;
  transition: opacity 0.5s ease;
}

/* Header */
.shell-header {
  position: relative;
  z-index: 10;
  border-bottom: 1px solid var(--header-border);
  background: var(--header-bg);
  backdrop-filter: blur(20px);
  transition: all 0.5s ease;
}

.header-content {
  max-width: 72rem;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-badge {
  display: flex;
  height: 3rem;
  width: 3rem;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: linear-gradient(to bottom right, #f472b6, #a78bfa, #10b981);
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: white;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-subtitle {
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.4em;
  color: var(--text-secondary);
  margin: 0;
  transition: color 0.5s ease;
}

.brand-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  transition: color 0.5s ease;
}

.nav-links {
  display: none;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .nav-links {
    display: flex;
  }
}

.nav-link {
  border-radius: 9999px;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--nav-link-color);
  text-decoration: none;
  transition: all 0.3s ease;
}

.nav-link:hover {
  color: var(--nav-link-hover);
}

.nav-link.active {
  background: var(--nav-link-active-bg);
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.back-link {
  display: none;
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.3s ease;
}

@media (min-width: 768px) {
  .back-link {
    display: block;
  }
}

.back-link:hover {
  color: var(--link-hover);
}

.login-btn {
  background: linear-gradient(to right, #f472b6, #a78bfa) !important;
  border: none !important;
  box-shadow: 0 10px 15px -3px rgba(244, 114, 182, 0.3) !important;
}

/* Main Content */
.shell-main {
  position: relative;
  z-index: 10;
  max-width: 72rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
  padding: 3rem 1.5rem;
}

@media (min-width: 1024px) {
  .shell-main {
    flex-direction: row;
    padding-top: 4rem;
    padding-bottom: 4rem;
  }
}

.hero-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.hero-badge {
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.6em;
  color: var(--text-muted);
  margin: 0;
  transition: color 0.5s ease;
}

.hero-title {
  font-size: 2.25rem;
  font-weight: 600;
  line-height: 1.25;
  background: linear-gradient(135deg, var(--text-primary) 0%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  transition: all 0.5s ease;
}

.hero-desc {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  transition: color 0.5s ease;
}

.content-section {
  flex: 1;
}

/* Footer */
.shell-footer {
  position: relative;
  z-index: 10;
  border-top: 1px solid var(--footer-border);
  background: var(--footer-bg);
  backdrop-filter: blur(10px);
  transition: all 0.5s ease;
}

.footer-content {
  max-width: 72rem;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem;
}

.footer-copyright {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
  transition: color 0.5s ease;
}

.footer-links {
  display: flex;
  gap: 1rem;
}

.footer-links a {
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-links a:hover {
  color: var(--link-hover);
}

/* Responsive */
@media (max-width: 640px) {
  .theme-toggle {
    top: 1rem;
    right: 1rem;
    width: 40px;
    height: 40px;
  }

  .header-content {
    padding: 0.75rem 1rem;
  }

  .brand-badge {
    height: 2.5rem;
    width: 2.5rem;
    font-size: 1rem;
  }

  .brand-title {
    font-size: 1rem;
  }

  .brand-subtitle {
    font-size: 0.75rem;
  }

  .hero-title {
    font-size: 1.75rem;
  }

  .shell-main {
    padding: 2rem 1rem;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
    padding: 1rem;
  }
}
</style>
