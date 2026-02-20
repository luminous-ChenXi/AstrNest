<script setup>
import { RouterLink } from 'vue-router'
import { CheckCircle2, Sparkles, ArrowRight, Home, Shield, Zap, Lock, Sun, Moon } from 'lucide-vue-next'
import { ref, onMounted } from 'vue'

const isDark = ref(true)
const STORAGE_KEY = 'chenxi-theme'

// 从 localStorage 获取主题设置
const getStoredTheme = () => {
  if (typeof window === 'undefined') return true
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored !== null) {
    return stored === 'dark'
  }
  // 如果没有存储，检测系统偏好
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 保存主题设置到 localStorage
const storeTheme = (dark) => {
  if (typeof window === 'undefined') return
  localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light')
}

// 初始化主题
const initTheme = () => {
  isDark.value = getStoredTheme()
}

// 监听系统主题变化
onMounted(() => {
  initTheme()

  // 监听 storage 事件，当其他页面切换主题时同步
  window.addEventListener('storage', (e) => {
    if (e.key === STORAGE_KEY) {
      isDark.value = e.newValue === 'dark'
    }
  })

  // 监听系统主题变化（仅在用户没有手动设置时）
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', (e) => {
    // 只有当用户没有手动设置过主题时才自动切换
    if (localStorage.getItem(STORAGE_KEY) === null) {
      isDark.value = e.matches
    }
  })
})

// 手动切换主题
const toggleTheme = () => {
  isDark.value = !isDark.value
  storeTheme(isDark.value)
}
</script>

<template>
  <div class="register-success-page" :class="{ 'light': !isDark }">
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

    <!-- Content Container -->
    <div class="content-container">
      <!-- Success Card -->
      <div class="success-card">
        <!-- Icon Animation -->
        <div class="icon-wrapper">
          <div class="icon-bg">
            <CheckCircle2 class="success-icon" />
          </div>
          <div class="sparkles">
            <Sparkles class="sparkle sparkle-1" />
            <Sparkles class="sparkle sparkle-2" />
            <Sparkles class="sparkle sparkle-3" />
          </div>
        </div>

        <!-- Text Content -->
        <div class="text-content">
          <span class="badge">CHENXI SIGNUP</span>
          <h1 class="title">注册成功</h1>
          <p class="subtitle">您的账号已创建成功，现在可以登录并开始使用辰汐图床了。</p>
        </div>

        <!-- Feature Highlights -->
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <Zap class="icon" />
            </div>
            <span>极速上传</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Shield class="icon" />
            </div>
            <span>安全存储</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Lock class="icon" />
            </div>
            <span>隐私保护</span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="actions">
          <RouterLink :to="{ path: '/', query: { login: '1' } }" class="btn-primary">
            <span>立即登录</span>
            <ArrowRight class="btn-icon" />
          </RouterLink>
          <RouterLink to="/" class="btn-secondary">
            <Home class="btn-icon" />
            <span>返回首页</span>
          </RouterLink>
        </div>
      </div>

      <!-- Decorative Elements -->
      <div class="decoration">
        <div class="circle circle-1" />
        <div class="circle circle-2" />
        <div class="circle circle-3" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== CSS Variables for Theming ===== */
.register-success-page {
  /* Dark theme (default) */
  --bg-primary: #05060c;
  --bg-gradient-1: rgba(123, 132, 255, 0.35);
  --bg-gradient-2: rgba(255, 115, 161, 0.35);
  --grid-color: rgba(255, 255, 255, 0.03);
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
  --card-border: rgba(255, 255, 255, 0.1);
  --card-shadow: 0 25px 80px rgba(2, 6, 23, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.05) inset;
  --text-primary: #ffffff;
  --text-secondary: rgba(255, 255, 255, 0.6);
  --text-muted: rgba(255, 255, 255, 0.7);
  --feature-bg: linear-gradient(135deg, rgba(244, 114, 182, 0.2) 0%, rgba(167, 139, 250, 0.2) 100%);
  --feature-border: rgba(244, 114, 182, 0.3);
  --feature-text: rgba(255, 255, 255, 0.7);
  --divider-color: rgba(255, 255, 255, 0.08);
  --btn-secondary-bg: rgba(255, 255, 255, 0.08);
  --btn-secondary-border: rgba(255, 255, 255, 0.15);
  --btn-secondary-text: rgba(255, 255, 255, 0.9);
  --btn-secondary-hover-bg: rgba(255, 255, 255, 0.15);
  --btn-secondary-hover-border: rgba(255, 255, 255, 0.25);
  --circle-border: rgba(244, 114, 182, 0.1);
  --glow-opacity: 0.5;
}

.register-success-page.light {
  /* Light theme */
  --bg-primary: linear-gradient(135deg, #fafafa 0%, #f0f0f5 100%);
  --bg-gradient-1: rgba(244, 114, 182, 0.15);
  --bg-gradient-2: rgba(167, 139, 250, 0.15);
  --grid-color: rgba(0, 0, 0, 0.03);
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  --card-border: rgba(0, 0, 0, 0.08);
  --card-shadow: 0 25px 80px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.8) inset;
  --text-primary: #1a1a2e;
  --text-secondary: rgba(26, 26, 46, 0.6);
  --text-muted: rgba(26, 26, 46, 0.7);
  --feature-bg: linear-gradient(135deg, rgba(244, 114, 182, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  --feature-border: rgba(244, 114, 182, 0.2);
  --feature-text: rgba(26, 26, 46, 0.8);
  --divider-color: rgba(0, 0, 0, 0.06);
  --btn-secondary-bg: rgba(0, 0, 0, 0.05);
  --btn-secondary-border: rgba(0, 0, 0, 0.1);
  --btn-secondary-text: rgba(26, 26, 46, 0.9);
  --btn-secondary-hover-bg: rgba(0, 0, 0, 0.1);
  --btn-secondary-hover-border: rgba(0, 0, 0, 0.15);
  --circle-border: rgba(244, 114, 182, 0.15);
  --glow-opacity: 0.3;
}

/* ===== Base Styles ===== */
.register-success-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
  transition: background 0.5s ease;
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
}

.bg-gradient-1 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 18% 18%, var(--bg-gradient-1), transparent 45%);
  transition: opacity 0.5s ease;
}

.bg-gradient-2 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 82% 0%, var(--bg-gradient-2), transparent 38%);
  transition: opacity 0.5s ease;
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
  width: 600px;
  height: 600px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(244, 114, 182, var(--glow-opacity)) 0%, transparent 70%);
  pointer-events: none;
  transition: opacity 0.5s ease;
}

/* Content Container */
.content-container {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 520px;
}

/* Success Card */
.success-card {
  width: 100%;
  padding: 3rem 2.5rem;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 32px;
  backdrop-filter: blur(20px);
  box-shadow: var(--card-shadow);
  text-align: center;
  animation: card-in 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  transition: all 0.5s ease;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Icon Wrapper */
.icon-wrapper {
  position: relative;
  display: inline-flex;
  margin-bottom: 1.5rem;
}

.icon-bg {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 0 4px rgba(16, 185, 129, 0.2),
    0 0 0 8px rgba(16, 185, 129, 0.1),
    0 20px 40px rgba(16, 185, 129, 0.3);
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% {
    box-shadow:
      0 0 0 4px rgba(16, 185, 129, 0.2),
      0 0 0 8px rgba(16, 185, 129, 0.1),
      0 20px 40px rgba(16, 185, 129, 0.3);
  }
  50% {
    box-shadow:
      0 0 0 8px rgba(16, 185, 129, 0.15),
      0 0 0 16px rgba(16, 185, 129, 0.08),
      0 25px 50px rgba(16, 185, 129, 0.4);
  }
}

.success-icon {
  width: 48px;
  height: 48px;
  color: white;
  stroke-width: 2.5;
}

/* Sparkles */
.sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  width: 20px;
  height: 20px;
  color: #fbbf24;
  opacity: 0;
}

.sparkle-1 {
  top: -10px;
  right: -10px;
  animation: sparkle 2s ease-in-out infinite;
}

.sparkle-2 {
  bottom: 10px;
  left: -15px;
  animation: sparkle 2s ease-in-out infinite 0.5s;
}

.sparkle-3 {
  top: 20px;
  right: -20px;
  animation: sparkle 2s ease-in-out infinite 1s;
}

@keyframes sparkle {
  0%, 100% {
    opacity: 0;
    transform: scale(0) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1) rotate(180deg);
  }
}

/* Text Content */
.text-content {
  margin-bottom: 2rem;
}

.badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: #f472b6;
  margin-bottom: 0.75rem;
}

.title {
  font-size: 2.25rem;
  font-weight: 700;
  margin: 0 0 0.75rem 0;
  background: linear-gradient(135deg, var(--text-primary) 0%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: all 0.5s ease;
}

.subtitle {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
  transition: color 0.5s ease;
}

/* Features */
.features {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 2rem;
  padding: 1.5rem 0;
  border-top: 1px solid var(--divider-color);
  border-bottom: 1px solid var(--divider-color);
  transition: all 0.5s ease;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--feature-bg);
  border: 1px solid var(--feature-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.feature-item:hover .feature-icon {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(244, 114, 182, 0.25);
}

.feature-icon .icon {
  width: 22px;
  height: 22px;
  color: #f472b6;
}

.feature-item span {
  font-size: 0.875rem;
  color: var(--feature-text);
  font-weight: 500;
  transition: color 0.5s ease;
}

/* Actions */
.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-primary,
.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1.75rem;
  border-radius: 12px;
  font-size: 0.9375rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #f472b6 0%, #ec4899 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(244, 114, 182, 0.35);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(244, 114, 182, 0.5);
}

.btn-secondary {
  background: var(--btn-secondary-bg);
  color: var(--btn-secondary-text);
  border: 1px solid var(--btn-secondary-border);
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: var(--btn-secondary-hover-bg);
  border-color: var(--btn-secondary-hover-border);
  transform: translateY(-2px);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

/* Decoration */
.decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid var(--circle-border);
  transition: border-color 0.5s ease;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -50px;
  animation: float 8s ease-in-out infinite 2s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: -75px;
  animation: float 8s ease-in-out infinite 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/* Responsive */
@media (max-width: 640px) {
  .register-success-page {
    padding: 1rem;
  }

  .theme-toggle {
    top: 1rem;
    right: 1rem;
    width: 40px;
    height: 40px;
  }

  .success-card {
    padding: 2rem 1.5rem;
  }

  .icon-bg {
    width: 80px;
    height: 80px;
  }

  .success-icon {
    width: 40px;
    height: 40px;
  }

  .title {
    font-size: 1.75rem;
  }

  .features {
    gap: 1rem;
  }

  .actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
  }
}
</style>
