<script setup>
import { onMounted, ref, computed } from 'vue'
import { fetchOverviewMetrics, fetchOperationLogs } from '../services/monitoring'

const isLoading = ref(true)
const errorMessage = ref('')
const metrics = ref([])
const serverStatus = ref(null)
const logs = ref([])

const fetchData = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [overview, logEntries] = await Promise.all([
      fetchOverviewMetrics(),
      fetchOperationLogs(),
    ])
    metrics.value = overview?.cards ?? []
    serverStatus.value = overview?.serverStatus ?? null
    logs.value = logEntries ?? []
  } catch (error) {
    errorMessage.value = '获取监控数据失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchData)

const uptimeText = computed(() => {
  if (!serverStatus.value) return '--'
  const seconds = serverStatus.value.uptimeSeconds ?? 0
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${hours}h ${minutes}m`
})

const serverHealthBadge = computed(() => {
  const status = serverStatus.value?.health ?? 'UNKNOWN'
  if (status === 'HEALTHY') return 'text-emerald-300 bg-emerald-300/10'
  if (status === 'WARNING') return 'text-amber-300 bg-amber-300/10'
  if (status === 'CRITICAL') return 'text-rose-300 bg-rose-300/10'
  return 'text-white/70 bg-white/10'
})

const formatPercent = (value) => {
  if (value === undefined || value === null || value < 0) return '--'
  return `${value.toFixed(1)}%`
}

const formatLogTime = (timestamp) => {
  if (!timestamp) return '--'
  const diffMs = Date.now() - new Date(timestamp).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  return `${days} 天前`
}
</script>

<template>
  <section class="space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">overview</p>
      <h1 class="text-4xl font-semibold text-gradient">控制台总览</h1>
      <p class="text-white/70 mt-2 max-w-2xl">
        监控上传行为、带宽与告警状态，实时洞察资源运行情况。数据来自后端监控接口。
      </p>
    </header>

    <div v-if="errorMessage" class="glass-panel border border-rose-500/20 p-4 text-rose-200">
      {{ errorMessage }}
    </div>

    <div v-if="isLoading" class="glass-panel border border-white/10 p-6 animate-pulse">
      正在加载实时监控数据...
    </div>

    <div v-else class="grid gap-5 md:grid-cols-3">
      <article
        v-for="item in metrics"
        :key="item.label"
        class="glass-panel p-6 border border-white/10"
      >
        <p class="text-white/60 text-sm">{{ item.label }}</p>
        <p class="text-3xl font-semibold mt-3">{{ item.value }}</p>
        <p class="text-xs text-white/60 mt-1">{{ item.delta }}</p>
        <div :class="['mt-4 h-1 rounded-full bg-gradient-to-r', item.accent]"></div>
      </article>
    </div>

    <div class="grid gap-5 lg:grid-cols-2">
      <article class="glass-panel p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-white/60 text-sm">服务器状态</p>
            <h2 class="text-2xl font-semibold">资源监控</h2>
          </div>
          <span :class="['text-xs px-3 py-1 rounded-full', serverHealthBadge]">
            {{ serverStatus?.health ?? 'UNKNOWN' }}
          </span>
        </div>
        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div class="p-4 rounded-2xl bg-white/5 border border-white/5">
            <p class="text-xs text-white/60">CPU</p>
            <p class="text-3xl font-semibold">{{ formatPercent(serverStatus?.cpuUsage) }}</p>
            <p class="text-xs text-white/60 mt-1">运行时长 {{ uptimeText }}</p>
          </div>
          <div class="p-4 rounded-2xl bg-white/5 border border-white/5">
            <p class="text-xs text-white/60">内存</p>
            <p class="text-3xl font-semibold">{{ formatPercent(serverStatus?.memoryUsage) }}</p>
            <p class="text-xs text-white/60 mt-1">应用健康指数</p>
          </div>
        </div>
      </article>

      <article class="glass-panel p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-white/60 text-sm">操作日志</p>
            <h2 class="text-2xl font-semibold">最新事件</h2>
          </div>
          <button class="text-sm text-brand-primary" @click="fetchData">刷新</button>
        </div>
        <ul class="mt-4 space-y-3">
          <li
            v-for="log in logs"
            :key="log.id"
            class="flex items-center justify-between rounded-2xl bg-white/5 border border-white/5 px-4 py-3"
          >
            <div>
              <p class="font-medium">{{ log.action }}</p>
              <p class="text-xs text-white/60">{{ log.user }}</p>
            </div>
            <span class="text-xs text-white/60">{{ formatLogTime(log.timestamp) }}</span>
          </li>
          <li v-if="!logs.length" class="text-sm text-white/60">暂无日志</li>
        </ul>
      </article>
    </div>
  </section>
</template>
