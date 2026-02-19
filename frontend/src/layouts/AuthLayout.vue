<script setup>
import { computed } from 'vue'
import { useRoute, RouterView } from 'vue-router'
import AuthHero from '../components/chenxi/AuthHero.vue'
import AuthTopBar from '../components/common/AuthTopBar.vue'
import ChenxiGlobalFooter from '../components/common/ChenxiGlobalFooter.vue'

const route = useRoute()

const meta = computed(() => route.meta || {})
const layoutMode = computed(() => meta.value.authLayout || 'default')
const isDefaultLayout = computed(() => layoutMode.value === 'default')
</script>

<template>
  <div class="relative min-h-screen bg-[#05060c] text-white">
    <div class="pointer-events-none absolute inset-0 opacity-60">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_18%_18%,rgba(123,132,255,0.35),transparent_45%)]" />
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_82%_0%,rgba(255,115,161,0.35),transparent_38%)]" />
    </div>

    <header class="auth-header">
      <div class="header-container">
        <RouterLink to="/" class="brand">
          <div class="brand-logo">
            <span class="logo-text">CX</span>
          </div>
          <div class="brand-info">
            <p class="brand-subtitle">Chenxi Secure Access</p>
            <p class="brand-title">辰汐统一认证</p>
          </div>
        </RouterLink>
        <nav class="header-nav">
          <RouterLink to="/" class="nav-link nav-link-home">
            <span class="nav-icon">←</span>
            <span>返回首页</span>
          </RouterLink>
          <a href="https://docs.luminouschenxi.com" target="_blank" class="nav-link nav-link-docs">
            <span>帮助文档</span>
            <span class="nav-icon">↗</span>
          </a>
        </nav>
      </div>
    </header>

    <main
      class="relative z-10 w-full"
      :class="
        isDefaultLayout
          ? 'mx-auto flex max-w-6xl flex-col gap-10 px-6 py-12 lg:flex-row lg:py-16'
          : 'px-0 py-0'
      "
    >
      <template v-if="isDefaultLayout">
        <section class="flex-1 space-y-6">
          <AuthHero
            :badge="meta.heroBadge"
            :title="meta.heroTitle"
            :description="meta.heroDescription"
            :features="meta.heroFeatures"
          />
        </section>
        <section class="flex-1">
          <RouterView />
        </section>
      </template>
      <template v-else>
        <RouterView />
      </template>
    </main>

    <ChenxiGlobalFooter />
  </div>
</template>

<style scoped>
/* 头部容器 */
.auth-header {
  position: relative;
  z-index: 10;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(135deg, rgba(249, 168, 200, 0.08) 0%, rgba(174, 208, 237, 0.05) 50%, rgba(255, 255, 255, 0.02) 100%);
  backdrop-filter: blur(20px);
}

.header-container {
  max-width: 72rem;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
}

/* 品牌区域 */
.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  transition: opacity 0.2s ease;
}

.brand:hover {
  opacity: 0.85;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 50%, #D06EF6 100%);
  box-shadow: 0 4px 20px rgba(249, 168, 200, 0.3);
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: white;
  letter-spacing: 0.1em;
}

.brand-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.brand-subtitle {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.5);
}

.brand-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
  letter-spacing: 0.02em;
}

/* 导航链接 */
.header-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  border-radius: 9999px;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: white;
  background: rgba(255, 255, 255, 0.08);
}

.nav-link-home {
  display: none;
}

@media (min-width: 768px) {
  .nav-link-home {
    display: inline-flex;
  }
}

.nav-link-docs {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
}

.nav-link-docs:hover {
  border-color: rgba(249, 168, 200, 0.4);
  background: rgba(249, 168, 200, 0.1);
}

.nav-icon {
  font-size: 0.875rem;
  opacity: 0.7;
}
</style>
