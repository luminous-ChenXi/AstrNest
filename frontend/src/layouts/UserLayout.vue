<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useUploadStore } from '../stores/upload'
import { fetchProfile } from '../services/user'
import { fetchUploadLimits } from '../services/upload'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'
import ChenxiGlobalFooter from '../components/common/ChenxiGlobalFooter.vue'
import ThemeSwitcher from '../components/common/ThemeSwitcher.vue'
import UserNavbar from '../components/common/UserNavbar.vue'

const router = useRouter()
const auth = useAuthStore()
const upload = useUploadStore()
const loadingProfile = ref(false)

const tabs = [
  { name: 'user-home', label: '仪表盘' },
  { name: 'user-images', label: '媒体管理' },
  { name: 'user-albums', label: '我的图集' },
  { name: 'user-profile', label: '资料信息' },
  { name: 'user-security', label: '安全设置' },
  { name: 'user-api', label: 'API 接口管理' },
]

const currentRouteName = computed(() => router.currentRoute.value?.name)
const isActiveTab = (name) => currentRouteName.value === name

onMounted(async () => {
  if (!auth.profile && auth.isAuthenticated) {
    try {
      loadingProfile.value = true
      const { data } = await fetchProfile()
      const nextProfile = { ...(auth.profile || {}), ...data }
      auth.updateProfile(nextProfile)
    } catch (error) {
      console.error('加载用户信息失败', error)
    } finally {
      loadingProfile.value = false
    }
  }

  if (!upload.limits.maxFiles) {
    try {
      upload.setLoading(true)
      const { data } = await fetchUploadLimits()
      upload.setLimits(data)
    } catch (error) {
      console.error('加载上传限制失败', error)
      upload.setError(error)
    } finally {
      upload.setLoading(false)
    }
  }
})
</script>

<template>
  <div class="user-layout relative min-h-screen bg-surface-body text-body-primary">
    <div class="user-layout__halo user-layout__halo--primary" aria-hidden="true" />
    <div class="user-layout__halo user-layout__halo--secondary" aria-hidden="true" />

    <UserNavbar :tabs="tabs" />

    <main class="relative z-10 mx-auto max-w-6xl space-y-8 px-4 pb-16 pt-28 lg:px-6">
      <section class="glass-panel overflow-hidden p-6">
        <div class="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div class="space-y-3">
            <p class="text-xs uppercase tracking-[0.5em] text-body-soft">welcome back</p>
            <h1 class="text-3xl font-semibold text-gradient">{{ auth.displayName }}</h1>
            <p class="max-w-3xl text-sm text-body-muted">
              AstrNest 用户前端管理系统
            </p>
            <p v-if="loadingProfile" class="text-xs text-body-faint">正在刷新资料...</p>
          </div>
          <div class="panel-strong rounded-3xl px-5 py-4 text-sm text-body-muted">
            <p class="font-semibold text-body-primary">账号摘要</p>
            <p class="mt-1">角色：{{ auth.isAdmin ? '管理员' : '会员' }}</p>
            <p class="mt-1">登录令牌：{{ auth.isAuthenticated ? '已激活' : '未登录' }}</p>
          </div>
        </div>
      </section>

      <section class="glass-panel space-y-6 p-6">
        <nav class="flex flex-wrap gap-3">
          <ThemeSwitcher />
          <RouterLink
            v-for="tab in tabs"
            :key="tab.name"
            :to="{ name: tab.name }"
            class="user-tabs__link inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="isActiveTab(tab.name) ? 'user-tabs__link--active' : ''"
          >
            <ShieldCheck v-if="tab.name === 'user-security'" class="h-4 w-4" />
            <span>{{ tab.label }}</span>
          </RouterLink>
        </nav>
        <div class="panel-strong rounded-3xl p-4">
          <RouterView />
        </div>
      </section>
    </main>
    <ChenxiGlobalFooter />
  </div>
</template>

<style scoped>
.user-layout {
  background: var(--bg-gradient-mixed);
  color: var(--color-text-primary);
}

.user-layout__halo {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.user-layout__halo--primary {
  background: var(--halo-primary);
}

.user-layout__halo--secondary {
  background: var(--halo-secondary);
}

.user-tabs__link {
  border: 1px solid var(--border-soft);
  background: var(--panel-overlay);
  color: var(--text-muted);
}

.user-tabs__link--active {
  background: var(--color-bg-strong);
  border-color: var(--border-strong);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-card);
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
