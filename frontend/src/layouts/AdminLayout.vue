<script setup>
import { computed, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Home, LogOut, User } from 'lucide-vue-next'
import { useUiStore } from '../stores/ui'
import { useAuthStore } from '../stores/auth'
import { usePendingChangesStore } from '../stores/pendingChanges'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'

const auth = useAuthStore()
const navigation = [
  { name: 'admin-dashboard', label: '总览控制台', icon: '🏠' },
  { name: 'admin-images', label: '媒体管理', icon: '🖼️' },
  { name: 'admin-users', label: '用户管理', icon: '👥' },
  { name: 'admin-system', label: '系统配置', icon: '⚙️' },
  { name: 'admin-storage', label: '存储策略', icon: '🗄️' },
  { name: 'admin-mail-settings', label: '邮件设置', icon: '✉️' },
  { name: 'admin-security', label: '权限与安全', icon: '🛡️' },
  { name: 'admin-integration', label: '集成与 API', icon: '🔗' },
]

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const pendingChanges = usePendingChangesStore()

watch(
  () => route.name,
  (name) => {
    if (name) {
      uiStore.rememberRoute(name)
    }
  },
  { immediate: true }
)

const handleLogout = () => {
  if (pendingChanges.hasAdminSystemConfigChanges) {
    const confirmed = window.confirm('您所做的更改可能尚未保存，确认退出登录吗？')
    if (!confirmed) {
      return
    }
  }
  auth.logout()
  pendingChanges.resetAll()
  ElMessage.success('登出成功')
  router.replace({ name: 'login' })
}

const isCollapsed = computed(() => uiStore.isSidebarCollapsed)
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-surface-body">
    <div class="pointer-events-none absolute -top-40 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-brand-primary/30 blur-[120px]"></div>
    <div class="relative z-10 flex min-h-screen">
      <aside
        :class="[
          'transition-all duration-300 glass-panel border-r border-white/5 p-6 flex flex-col gap-8',
          isCollapsed ? 'w-24' : 'w-64',
        ]"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 text-lg font-semibold">
              CX
            </div>
            <div v-if="!isCollapsed">
              <p class="text-sm text-white/60">ASTRNEST</p>
              <p class="text-lg font-semibold">辰汐</p>
            </div>
          </div>
          <button class="rounded-full border border-white/10 bg-white/5 p-2" @click="uiStore.toggleSidebar()">
            <span class="text-xs">⇔</span>
          </button>
        </div>

        <nav class="space-y-2">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="{ name: item.name }"
            class="group flex items-center gap-3 rounded-2xl px-4 py-3 transition"
            :class="
              route.name === item.name
                ? 'bg-white/15 border border-white/20 shadow-card'
                : 'border border-transparent hover:border-white/10'
            "
          >
            <span class="text-lg">{{ item.icon }}</span>
            <span v-if="!isCollapsed" class="text-sm font-medium">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="mt-auto rounded-3xl bg-gradient-to-br from-brand-accent/30 to-brand-primary/20 p-4 text-sm">
          <p class="text-xs uppercase tracking-[0.3em] text-white/70">状态</p>
          <p class="text-lg font-semibold mt-2">服务运行正常</p>
          <p class="text-white/60 mt-1">Ubuntu 24 · Java 21 · Vue 3</p>
        </div>
      </aside>

      <div class="flex-1 flex flex-col">
        <header class="flex items-center justify-between px-8 py-6 border-b border-white/5 bg-surface-panel/40 backdrop-blur-3xl">
          <div>
            <p class="text-sm text-white/60">{{ route.meta.label }}</p>
            <h1 class="text-2xl font-semibold">辰汐运营面板</h1>
          </div>
          <div class="flex items-center gap-4">
            <RouterLink
              to="/"
              class="chenxi-home-btn rounded-full border border-white/20 px-5 py-2 font-semibold text-white/80 transition hover:border-brand-primary hover:text-white flex items-center gap-2"
            >
              <Home class="h-4 w-4" />
              <span>首页</span>
            </RouterLink>
            <el-dropdown class="chenxi-user-dropdown" trigger="hover">
              <div class="chenxi-user-avatar flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-primary to-brand-accent text-white font-semibold transition-transform hover:scale-110">
                <User class="h-5 w-5" />
              </div>
              <template #dropdown>
                <el-dropdown-menu class="chenxi-dropdown-menu bg-surface-panel border border-white/10">
                  <RouterLink
                    to="/user"
                    class="block w-full"
                  >
                    <el-dropdown-item class="text-white/80 hover:text-white hover:bg-white/10">
                      用户中心
                    </el-dropdown-item>
                  </RouterLink>
                  <el-divider class="border-white/10" />
                  <el-dropdown-item class="text-red-400 hover:text-red-300 hover:bg-white/10" @click="handleLogout">
                    <LogOut class="mr-2 h-4 w-4" />
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>
        <main class="flex-1 overflow-y-auto p-8">
          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>
