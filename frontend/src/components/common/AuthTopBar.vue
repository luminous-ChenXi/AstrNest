<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import ThemeSwitcher from './ThemeSwitcher.vue'
import SiteLogo from './SiteLogo.vue'

const isNavOpen = ref(false)

const navLinks = [
  { to: '/gallery', label: '公开图库' },
  { to: '/gallery#gallery-search', label: '智能检索' },
  { to: '/announcements', label: '公告' },
  { to: '#contact', label: '文档' },
]
</script>

<template>
  <header class="fixed inset-x-0 top-0 z-40 border-b border-border bg-surface-panel/85 backdrop-blur-2xl">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 md:px-6 md:py-4">
      <div class="flex items-center gap-3 md:gap-4">
        <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-black/10 p-2 dark:bg-white/10 md:h-12 md:w-12">
          <SiteLogo :width="32" :height="32" />
        </div>
        <div class="leading-tight">
          <p class="text-base font-semibold text-text-primary md:text-lg">AstrNest</p>
          <p class="text-[11px] text-text-secondary md:text-xs">图床系统</p>
        </div>
      </div>

      <!-- 桌面导航 -->
      <nav class="hidden items-center gap-8 text-sm text-text-secondary md:flex">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="transition hover:text-text-primary"
        >
          {{ link.label }}
        </RouterLink>
      </nav>

      <!-- 移动端右侧操作区：主题 + 折叠菜单 -->
      <div class="flex items-center gap-2 md:hidden">
        <ThemeSwitcher />
        <button
          class="chenxi-landing-btn flex h-10 w-10 items-center justify-center rounded-full border border-border text-text-secondary transition hover:border-brand-primary hover:text-text-primary"
          @click="isNavOpen = !isNavOpen"
          aria-label="切换导航"
        >
          <span v-if="!isNavOpen">☰</span>
          <span v-else>✕</span>
        </button>
      </div>

      <!-- 桌面右侧操作区（不含登录注册） -->
      <div class="hidden items-center gap-3 md:flex">
        <ThemeSwitcher />
      </div>
    </div>

    <!-- 移动端折叠导航 -->
    <transition name="chenxi-fade">
      <div
        v-if="isNavOpen"
        class="md:hidden border-t border-border bg-surface-panel/95 px-4 pb-4 pt-3 backdrop-blur-xl"
      >
        <div class="flex flex-col gap-3 text-sm text-text-secondary">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="rounded-xl px-3 py-2 hover:bg-black/5 dark:hover:bg-white/5"
            @click="isNavOpen = false"
          >
            {{ link.label }}
          </RouterLink>
        </div>
      </div>
    </transition>
  </header>
</template>
