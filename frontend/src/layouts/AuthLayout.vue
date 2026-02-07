<script setup>
import { computed } from 'vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import AuthHero from '../components/chenxi/AuthHero.vue'
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

    <header class="relative z-10 border-b border-white/10 bg-gradient-to-r from-white/5 to-white/0 backdrop-blur-xl">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <RouterLink to="/" class="flex items-center gap-3">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-primary via-brand-accent to-brand-emerald text-lg font-semibold tracking-[0.2em]">
            CX
          </div>
          <div>
            <p class="text-sm uppercase tracking-[0.4em] text-white/60">Chenxi Secure Access</p>
            <p class="text-xl font-semibold">辰汐统一认证</p>
          </div>
        </RouterLink>
        <div class="flex items-center gap-3">
          <RouterLink to="/" class="hidden text-sm text-white/70 hover:text-white/100 md:block">返回首页</RouterLink>
          <a href="https://docs.luminouschenxi.com" target="_blank" class="rounded-full px-4 py-2 text-sm font-medium text-white/60 transition hover:text-white">
            帮助文档
          </a>
        </div>
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
