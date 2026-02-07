<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LogOut, ShieldCheck, User } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useUploadStore } from '../stores/upload'
import { fetchProfile } from '../services/user'
import { fetchUploadLimits } from '../services/upload'
import siteLogo from '../assets/img/favicon.png'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'
import ChenxiGlobalFooter from '../components/common/ChenxiGlobalFooter.vue'

const router = useRouter()
const auth = useAuthStore()
const upload = useUploadStore()
const loadingProfile = ref(false)

const tabs = [
  { name: 'user-home', label: '仪表盘' },
  { name: 'user-images', label: '媒体管理' },
  { name: 'user-profile', label: '资料信息' },
  { name: 'user-security', label: '安全设置' },
  { name: 'user-api', label: 'API 接口管理' },
]

const currentRouteName = computed(() => router.currentRoute.value?.name)
const isActiveTab = (name) => currentRouteName.value === name

const goAdmin = () => {
  router.push({ name: 'admin-dashboard' })
}

const logout = () => {
  auth.logout()
  ElMessage.success('登出成功')
  router.replace({ name: 'login' })
}

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
  <div class="relative min-h-screen bg-surface-body text-white">
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_15%_20%,rgba(127,123,255,0.3),transparent_50%)]" />
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_85%_0%,rgba(255,95,143,0.25),transparent_45%)]" />

    <header class="fixed inset-x-0 top-0 z-40 border-b border-white/10 bg-surface-panel/70 backdrop-blur-2xl">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 lg:px-6">
        <RouterLink to="/" class="flex items-center gap-3">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 p-2">
            <img :src="siteLogo" alt="AstrNest 徽标" class="h-full w-full rounded-xl object-contain drop-shadow-lg" />
          </div>
          <div>
            <p class="text-sm uppercase tracking-[0.4em] text-white/60">member space</p>
            <p class="text-base font-semibold">AstrNest 控制台</p>
          </div>
        </RouterLink>
        <div class="flex items-center gap-3 text-sm">
          <RouterLink
            to="/"
            class="chenxi-home-btn rounded-full border border-white/20 px-5 py-2 font-semibold text-white/80 transition hover:border-brand-primary hover:text-white flex items-center gap-2"
          >
            <span>首页</span>
          </RouterLink>
          <button
            v-if="auth.isAdmin"
            type="button"
            class="chenxi-admin-btn hidden rounded-full border border-white/20 px-5 py-2 font-semibold text-white/80 transition hover:border-brand-primary hover:text-white md:inline-flex"
            @click="goAdmin"
          >
            进入后台
          </button>
          <el-dropdown class="chenxi-user-dropdown" trigger="hover">
            <div class="chenxi-user-avatar flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-primary to-brand-accent text-white font-semibold transition-transform hover:scale-110">
              <span v-if="auth.profile?.username" class="text-sm">
                {{ auth.profile.username.charAt(0).toUpperCase() }}
              </span>
              <User v-else class="h-5 w-5" />
            </div>
            <template #dropdown>
              <el-dropdown-menu class="chenxi-dropdown-menu bg-surface-panel border border-white/10">
                <RouterLink
                  v-for="tab in tabs"
                  :key="tab.name"
                  :to="{ name: tab.name }"
                  class="block w-full"
                >
                  <el-dropdown-item class="text-white/80 hover:text-white hover:bg-white/10">
                    {{ tab.label }}
                  </el-dropdown-item>
                </RouterLink>
                <el-divider class="border-white/10" />
                <el-dropdown-item class="text-red-400 hover:text-red-300 hover:bg-white/10" @click="logout">
                  <LogOut class="mr-2 h-4 w-4" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="relative z-10 mx-auto max-w-6xl space-y-8 px-4 pb-16 pt-28 lg:px-6">
      <section class="glass-panel overflow-hidden border border-white/10 bg-white/5 p-6">
        <div class="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div class="space-y-3">
            <p class="text-xs uppercase tracking-[0.5em] text-white/60">welcome back</p>
            <h1 class="text-3xl font-semibold text-gradient">{{ auth.displayName }}</h1>
            <p class="max-w-3xl text-sm text-white/70">
              AstrNest 用户前端管理系统
            </p>
            <p v-if="loadingProfile" class="text-xs text-white/50">正在刷新资料...</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-black/20 px-5 py-4 text-sm text-white/70">
            <p class="font-semibold text-white">账号摘要</p>
            <p class="mt-1">角色：{{ auth.isAdmin ? '管理员' : '会员' }}</p>
            <p class="mt-1">登录令牌：{{ auth.isAuthenticated ? '已激活' : '未登录' }}</p>
          </div>
        </div>
      </section>

      <section class="glass-panel space-y-6 border border-white/10 bg-white/5 p-6">
        <nav class="flex flex-wrap gap-3">
          <RouterLink
            v-for="tab in tabs"
            :key="tab.name"
            :to="{ name: tab.name }"
            class="inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="isActiveTab(tab.name)
              ? 'bg-white/20 text-white shadow-[0_10px_30px_rgba(15,23,42,0.45)]'
              : 'bg-white/5 text-white/60 hover:text-white'"
          >
            <ShieldCheck v-if="tab.name === 'user-security'" class="h-4 w-4" />
            <span>{{ tab.label }}</span>
          </RouterLink>
        </nav>
        <div class="rounded-3xl border border-white/5 bg-black/30 p-4">
          <RouterView />
        </div>
      </section>
    </main>
    <ChenxiGlobalFooter />
  </div>
</template>
