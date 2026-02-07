<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchPublicUserProfile } from '../../services/publicUsers'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const profile = ref(null)
const errorMessage = ref('')

const formatBytes = (value) => {
  if (!value || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let sizeValue = value
  let unitIndex = 0
  while (sizeValue >= 1024 && unitIndex < units.length - 1) {
    sizeValue /= 1024
    unitIndex += 1
  }
  return `${sizeValue.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

const stats = computed(() => {
  if (!profile.value) {
    return []
  }
  return [
    { label: '累计上传', value: profile.value.uploadCount },
    { label: '占用空间', value: formatBytes(profile.value.storageBytes) },
    { label: '点赞总数', value: profile.value.likeCount },
  ]
})

const loadProfile = async () => {
  const userId = route.params.userId
  if (!userId) {
    errorMessage.value = '未指定用户 ID'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchPublicUserProfile(userId)
    profile.value = data
  } catch (error) {
    const message = error?.response?.data?.message || '获取用户信息失败'
    errorMessage.value = message
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.userId,
  () => {
    loadProfile()
  }
)

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <section class="relative min-h-[80vh] overflow-hidden bg-surface-body px-4 py-16">
    <div class="pointer-events-none absolute inset-0 bg-gradient-to-b from-brand-primary/10 via-transparent to-surface-body"></div>
    <div class="relative mx-auto max-w-4xl space-y-8">
      <button
        class="inline-flex items-center gap-2 text-sm text-white/70 transition hover:text-white"
        @click="router.push('/')"
      >
        ← 返回首页
      </button>

      <div class="glass-panel rounded-3xl border border-white/10 bg-white/5 p-8 text-white">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-center">
          <div class="relative flex h-28 w-28 items-center justify-center rounded-3xl border border-white/20 bg-white/10 text-3xl font-semibold text-white">
            <img
              v-if="profile && profile.avatarUrl"
              :src="profile.avatarUrl"
              alt="avatar"
              class="h-full w-full rounded-3xl object-cover"
            />
            <span v-else>{{ profile && profile.displayName ? profile.displayName.slice(0, 1) : '?' }}</span>
          </div>
          <div class="flex-1">
            <p class="text-xs uppercase tracking-[0.5em] text-white/60">uploader</p>
            <h1 class="mt-2 text-4xl font-semibold">
              {{ profile && profile.displayName ? profile.displayName : '匿名用户' }}
            </h1>
            <p class="mt-2 text-white/70">
              {{ profile && profile.signature ? profile.signature : '这位用户还没有留下任何介绍。' }}
            </p>
            <div class="mt-4 flex flex-wrap gap-3 text-sm text-white/80">
              <a v-if="profile && profile.email" :href="`mailto:${profile.email}`" class="rounded-full border border-white/20 px-4 py-1.5">
                {{ profile.email }}
              </a>
              <span class="rounded-full border border-white/20 px-4 py-1.5">ID：{{ route.params.userId }}</span>
            </div>
          </div>
        </div>

        <div class="mt-8 grid gap-4 sm:grid-cols-3">
          <div v-for="stat in stats" :key="stat.label" class="rounded-2xl border border-white/10 bg-white/5 px-5 py-4">
            <p class="text-xs uppercase tracking-[0.3em] text-white/60">{{ stat.label }}</p>
            <p class="mt-3 text-3xl font-semibold">{{ stat.value }}</p>
          </div>
        </div>
      </div>

      <div v-if="errorMessage && !loading" class="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-rose-100">
        {{ errorMessage }}
      </div>

      <div v-else-if="loading" class="rounded-2xl border border-white/5 bg-white/5 px-4 py-6 text-center text-white/60">
        正在加载用户信息...
      </div>

      <div class="rounded-3xl border border-white/10 bg-white/5 p-6 text-sm text-white/70">
        <p>
          所有数据均来自用户公开图库与上传记录，禁止用于广告骚扰或非法用途。若需举报违规，请联系管理员。
        </p>
      </div>
    </div>
  </section>
</template>
