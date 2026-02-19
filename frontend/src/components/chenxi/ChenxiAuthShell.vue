<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '辰汐安全云' },
  subtitle: { type: String, default: 'Chenxi Secure Access Layer' },
  badge: { type: String, default: 'CHENXI' },
})

const route = useRoute()

const navLinks = [
  { to: { path: '/', query: { login: '1' } }, label: '登录' },
  { to: '/register', label: '注册' },
  { to: '/forgot-password', label: '找回密码' },
]

const activePath = computed(() => route.path)
</script>

<template>
  <div class="relative min-h-screen bg-[#05060c] text-white">
    <div class="pointer-events-none absolute inset-0 opacity-60">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_18%_18%,rgba(123,132,255,0.35),transparent_45%)]" />
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_82%_0%,rgba(255,115,161,0.35),transparent_38%)]" />
    </div>

    <header class="relative z-10 border-b border-white/10 bg-gradient-to-r from-white/5 to-white/0 backdrop-blur-xl">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-3">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-primary via-brand-accent to-brand-emerald text-lg font-semibold tracking-[0.2em]">
            {{ props.badge }}
          </div>
          <div>
            <p class="text-sm uppercase tracking-[0.4em] text-white/60">{{ props.subtitle }}</p>
            <p class="text-xl font-semibold">{{ props.title }}</p>
          </div>
        </div>
        <nav class="hidden gap-3 md:flex">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="rounded-full px-4 py-2 text-sm font-medium transition"
            :class="activePath === link.to ? 'bg-white/20 text-white' : 'text-white/60 hover:text-white'"
          >
            {{ link.label }}
          </RouterLink>
        </nav>
        <div class="flex items-center gap-3">
          <RouterLink to="/" class="hidden text-sm text-white/70 hover:text-white/100 md:block">返回首页</RouterLink>
          <RouterLink :to="{ path: '/', query: { login: '1' } }">
            <ElButton type="primary" class="bg-gradient-to-r from-brand-primary to-brand-accent border-none shadow-lg">
              控制台登录
            </ElButton>
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="relative z-10 mx-auto flex max-w-6xl flex-col gap-10 px-6 py-12 lg:flex-row lg:py-16">
      <section class="flex-1 space-y-6">
        <slot name="hero">
          <p class="text-sm uppercase tracking-[0.6em] text-white/50">Chenxi Access</p>
          <h1 class="text-4xl font-semibold leading-tight text-gradient">辰汐统一认证与内容治理门户</h1>
          <p class="text-base text-white/70">
            使用邮箱 + 人机验证的零信任流程，自动接入会员内容存储、审计与 API 生态，体验极速上传与风控联动。
          </p>
        </slot>
      </section>
      <section class="flex-1">
        <slot />
      </section>
    </main>

    <footer class="relative z-10 border-t border-white/10 bg-black/20">
      <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-6 text-sm text-white/60">
        <p>© {{ new Date().getFullYear() }} 辰汐内容安全团队 · All Rights Reserved</p>
        <div class="flex gap-4">
          <RouterLink to="/security" class="hover:text-white">安全中心</RouterLink>
          <RouterLink to="/integration" class="hover:text-white">开放平台</RouterLink>
          <a href="mailto:chenxi@luminouschenxi.net" class="hover:text-white">chenxi@luminouschenxi.net</a>
        </div>
      </div>
    </footer>
  </div>
</template>
