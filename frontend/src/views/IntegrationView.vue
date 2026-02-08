<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  createAdminApiKey,
  deleteAdminApiKey,
  fetchAdminApiKeyDashboard,
  fetchAdminApiKeyOwners,
  fetchAdminApiKeys,
  resetAdminApiKey,
  updateAdminApiKeyLimits,
  updateAdminApiKeyStatus,
} from '../services/apiKeys'

const dashboard = ref(null)
const owners = ref([])
const apiKeys = ref([])
const loading = reactive({ dashboard: false, owners: false, keys: false })
const notifications = reactive({ success: '', error: '' })
const singleUseKey = ref('')
const creating = ref(false)
const filters = reactive({ search: '', ownerId: 'all', activeOnly: false })
const newKeyForm = reactive({ name: '', description: '', dailyQuota: 1000, perMinuteQuota: 120, ownerId: '' })
const limitDialog = reactive({ visible: false, id: null, name: '', dailyQuota: 1000, perMinuteQuota: 120, saving: false })
const apiHeaderName = 'X-API-Key'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

const ownerOptions = computed(() => [
  { label: '全部所有者', value: 'all' },
  ...owners.value
    .filter((o) => o.ownerId != null)
    .map((o) => ({
      label: `${o.displayName || o.username} (#${o.ownerId})`,
      value: String(o.ownerId),
    })),
])

const curlSnippet = computed(
  () => `curl -X POST \\\n  -H "${apiHeaderName}: ik_your_key_here" \\\n  -F "files=@/path/to/image.png" \\\n  ${apiBaseUrl}/api/uploads`
)

const cards = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '密钥总数', value: dashboard.value.totalKeys },
    { label: '活跃密钥', value: dashboard.value.activeKeys },
    { label: '今日调用', value: dashboard.value.todaysRequests },
    { label: 'API 上传(今日)', value: dashboard.value.todayUploadsViaApi },
  ]
})

const ownerHighlight = computed(() => owners.value.slice(0, 5))

const resetNotifications = () => {
  notifications.error = ''
  notifications.success = ''
}

const notifyError = (message) => {
  notifications.error = message
  notifications.success = ''
}

const notifySuccess = (message) => {
  notifications.success = message
  notifications.error = ''
}

const loadDashboard = async () => {
  loading.dashboard = true
  try {
    const { data } = await fetchAdminApiKeyDashboard()
    dashboard.value = data
  } catch (error) {
    notifyError(error?.response?.data?.message || '加载指标失败')
  } finally {
    loading.dashboard = false
  }
}

const loadOwners = async () => {
  loading.owners = true
  try {
    const { data } = await fetchAdminApiKeyOwners()
    owners.value = data || []
  } catch (error) {
    notifyError(error?.response?.data?.message || '加载所有者统计失败')
  } finally {
    loading.owners = false
  }
}

const buildFilterParams = () => {
  const params = {}
  if (filters.search.trim()) {
    params.search = filters.search.trim()
  }
  if (filters.ownerId !== 'all') {
    params.ownerId = Number(filters.ownerId)
  }
  if (filters.activeOnly) {
    params.activeOnly = true
  }
  return params
}

const loadKeys = async () => {
  loading.keys = true
  try {
    const { data } = await fetchAdminApiKeys(buildFilterParams())
    apiKeys.value = data || []
  } catch (error) {
    notifyError(error?.response?.data?.message || '加载密钥列表失败')
  } finally {
    loading.keys = false
  }
}

const loadAll = async () => {
  await Promise.all([loadDashboard(), loadOwners(), loadKeys()])
}

onMounted(loadAll)

let searchTimer

watch(
  () => filters.search,
  () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
      loadKeys()
    }, 400)
  }
)

watch(
  () => [filters.ownerId, filters.activeOnly],
  () => {
    loadKeys()
  }
)

onBeforeUnmount(() => {
  clearTimeout(searchTimer)
})

const handleCreate = async () => {
  if (!newKeyForm.name.trim()) {
    notifyError('请填写密钥名称')
    return
  }
  resetNotifications()
  creating.value = true
  try {
    const payload = {
      name: newKeyForm.name.trim(),
      description: newKeyForm.description?.trim() || undefined,
      dailyQuota: newKeyForm.dailyQuota,
      perMinuteQuota: newKeyForm.perMinuteQuota,
      ownerId: newKeyForm.ownerId ? Number(newKeyForm.ownerId) : undefined,
    }
    const { data } = await createAdminApiKey(payload)
    singleUseKey.value = data?.plainValue || ''
    notifySuccess('密钥创建成功，请立即保存明文')
    newKeyForm.name = ''
    newKeyForm.description = ''
    newKeyForm.ownerId = ''
    await Promise.all([loadDashboard(), loadOwners(), loadKeys()])
  } catch (error) {
    notifyError(error?.response?.data?.message || '创建 API 密钥失败')
  } finally {
    creating.value = false
  }
}

const toggleKey = async (key) => {
  resetNotifications()
  try {
    await updateAdminApiKeyStatus(key.id, !key.active)
    notifySuccess(!key.active ? '密钥已启用' : '密钥已禁用')
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '更新密钥状态失败')
  }
}

const openLimitDialog = (key) => {
  limitDialog.visible = true
  limitDialog.id = key.id
  limitDialog.name = key.name
  limitDialog.dailyQuota = key.dailyQuota
  limitDialog.perMinuteQuota = key.perMinuteQuota
}

const saveLimits = async () => {
  if (!limitDialog.id) return
  limitDialog.saving = true
  resetNotifications()
  try {
    await updateAdminApiKeyLimits(limitDialog.id, {
      dailyQuota: limitDialog.dailyQuota,
      perMinuteQuota: limitDialog.perMinuteQuota,
    })
    notifySuccess('限流策略已更新')
    limitDialog.visible = false
    await Promise.all([loadDashboard(), loadKeys()])
  } catch (error) {
    notifyError(error?.response?.data?.message || '更新限流策略失败')
  } finally {
    limitDialog.saving = false
  }
}

const resetKey = async (key) => {
  resetNotifications()
  try {
    const { data } = await resetAdminApiKey(key.id)
    singleUseKey.value = data?.plainValue || ''
    notifySuccess('密钥已重新生成')
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '重置密钥失败')
  }
}

const removeKey = async (key) => {
  if (!confirm(`确定删除「${key.name}」吗？`)) return
  resetNotifications()
  try {
    await deleteAdminApiKey(key.id)
    notifySuccess('密钥已删除')
    await Promise.all([loadDashboard(), loadKeys()])
  } catch (error) {
    notifyError(error?.response?.data?.message || '删除密钥失败')
  }
}

const copyValue = async (value) => {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    notifySuccess('已复制到剪贴板')
  } catch (error) {
    notifyError('复制失败，请手动选择')
    console.error('copy error', error)
  }
}

const formatNumber = (value) => {
  if (value == null) return '0'
  return Number(value).toLocaleString('zh-CN')
}

const ownerLabel = (key) => {
  if (key.owner?.displayName) {
    return key.owner.displayName
  }
  if (key.owner?.username) {
    return key.owner.username
  }
  return '系统接口'
}

const openEmbedDoc = () => {
  window.open('https://luminouschenxi.net/docs/api-uploads', '_blank')
}
</script>

<template>
  <section class="space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">api orchestration</p>
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-4xl font-semibold text-gradient">API 接口管理</h1>
          <p class="mt-2 max-w-3xl text-white/70">
            统一监管用户的上传接口，随时掌握调用频率、分钟级限流和系统判定。管理员可直接签发密钥，并以秒级刷新查看调用与上传统计。
          </p>
        </div>
        <button class="rounded-full border border-white/20 px-5 py-2 text-sm text-white/80 transition hover:border-brand-primary hover:text-white" @click="loadAll">刷新数据</button>
      </div>
    </header>

    <div v-if="notifications.error" class="glass-panel border border-rose-500/40 p-4 text-sm text-rose-100">
      {{ notifications.error }}
    </div>
    <div v-if="notifications.success" class="glass-panel border border-emerald-500/40 p-4 text-sm text-emerald-200">
      {{ notifications.success }}
    </div>

    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <article v-for="card in cards" :key="card.label" class="glass-panel border border-white/10 p-4">
        <p class="text-xs uppercase tracking-[0.4em] text-white/50">{{ card.label }}</p>
        <p class="mt-2 text-3xl font-semibold">{{ formatNumber(card.value) }}</p>
      </article>
    </div>

    <div class="grid gap-6 lg:grid-cols-3">
      <article class="glass-panel border border-white/10 p-6 lg:col-span-2">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-white/60">流量洞察</p>
            <h2 class="text-2xl font-semibold">所有者调用排行</h2>
          </div>
        </div>
        <div class="mt-4 overflow-x-auto rounded-2xl border border-white/5 bg-black/20">
          <table class="w-full text-left text-sm">
            <thead class="text-white/60">
              <tr>
                <th class="px-4 py-3">所有者</th>
                <th class="px-4 py-3">密钥数</th>
                <th class="px-4 py-3">活跃</th>
                <th class="px-4 py-3">累计调用</th>
                <th class="px-4 py-3">今日调用</th>
                <th class="px-4 py-3">API 上传</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="owner in ownerHighlight" :key="owner.ownerId || 'system'" class="border-t border-white/5 text-white/80">
                <td class="px-4 py-3">
                  <div class="font-semibold">{{ owner.displayName || owner.username || '系统接口' }}</div>
                  <div class="text-xs text-white/50">{{ owner.username || 'internal' }}</div>
                </td>
                <td class="px-4 py-3">{{ formatNumber(owner.keyCount) }}</td>
                <td class="px-4 py-3">{{ formatNumber(owner.activeKeyCount) }}</td>
                <td class="px-4 py-3">{{ formatNumber(owner.totalRequests) }}</td>
                <td class="px-4 py-3">{{ formatNumber(owner.todaysRequests) }}</td>
                <td class="px-4 py-3">{{ formatNumber(owner.uploadCount) }}</td>
              </tr>
              <tr v-if="!ownerHighlight.length">
                <td colspan="6" class="px-4 py-4 text-center text-white/60">暂无所有者统计</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="glass-panel border border-white/10 p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-white/60">快速签发</p>
            <h2 class="text-xl font-semibold">创建新的 API 密钥</h2>
          </div>
        </div>
        <div class="space-y-3">
          <input v-model="newKeyForm.name" class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="密钥名称 (例如：Typora 上传)" />
          <input v-model="newKeyForm.description" class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="备注信息 (可选)" />
          <input v-model.number="newKeyForm.dailyQuota" type="number" min="100" max="100000" class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="每日调用上限" />
          <input v-model.number="newKeyForm.perMinuteQuota" type="number" min="10" max="10000" class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="每分钟限流阈值" />
          <input v-model="newKeyForm.ownerId" type="number" min="1" class="w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="用户 ID (可选，为指定用户预生成)" />
          <button class="w-full rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent py-3 text-sm font-semibold shadow-[0_15px_45px_rgba(127,123,255,0.35)] disabled:opacity-60" :disabled="creating" @click="handleCreate">
            {{ creating ? '创建中...' : '生成密钥' }}
          </button>
        </div>
        <div class="rounded-2xl border border-white/10 bg-black/30 p-4 text-xs text-white/60">
          <p>上传端点：</p>
          <p class="font-mono text-sm text-white">POST {{ apiBaseUrl }}/api/uploads</p>
          <p class="mt-2">Header：<code class="font-mono text-white">{{ apiHeaderName }}</code></p>
        </div>
        <div class="rounded-2xl border border-white/10 bg-black/30 p-4">
          <div class="flex items-center justify-between text-white/80">
            <p class="text-sm font-semibold">cURL 示例</p>
            <button class="text-xs text-brand-primary" @click="copyValue(curlSnippet)">复制</button>
          </div>
          <pre class="mt-2 whitespace-pre-wrap break-words text-xs text-white/70">{{ curlSnippet }}</pre>
        </div>
      </article>
    </div>

    <article v-if="singleUseKey" class="glass-panel border border-amber-400/40 p-6 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold">一次性明文密钥</h2>
        <button class="text-sm text-brand-primary" @click="copyValue(singleUseKey)">复制</button>
      </div>
      <p class="text-sm text-white/70">以下密钥仅显示一次，请务必保存。</p>
      <div class="rounded-2xl bg-black/40 border border-white/10 px-4 py-3 font-mono text-sm break-all">{{ singleUseKey }}</div>
    </article>

    <article class="glass-panel border border-white/10 p-6 space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p class="text-sm text-white/60">密钥列表</p>
          <h2 class="text-2xl font-semibold">当前可用 {{ apiKeys.length }} 个</h2>
        </div>
        <div class="flex flex-wrap gap-3">
          <input v-model="filters.search" class="w-48 rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none" placeholder="搜索名称 / 所有者" />
          <select v-model="filters.ownerId" class="rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-sm text-white focus:border-brand-primary focus:outline-none">
            <option v-for="option in ownerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <label class="flex items-center gap-2 text-sm text-white/70">
            <input v-model="filters.activeOnly" type="checkbox" class="rounded border-white/30 bg-transparent" /> 仅显示启用
          </label>
        </div>
      </div>
      <div v-if="loading.keys" class="py-10 text-center text-white/60">加载中…</div>
      <div v-else-if="!apiKeys.length" class="py-10 text-center text-white/60">暂无密钥，先创建一个吧。</div>
      <div v-else class="grid gap-4 lg:grid-cols-2">
        <div v-for="key in apiKeys" :key="key.id" class="rounded-2xl border border-white/10 bg-black/30 p-4 space-y-3">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-lg font-semibold text-white">{{ key.name }}</p>
              <p class="text-xs text-white/50">{{ ownerLabel(key) }}</p>
            </div>
            <span class="rounded-full px-3 py-1 text-xs" :class="key.active ? 'bg-emerald-400/20 text-emerald-200' : 'bg-white/10 text-white/50'">
              {{ key.active ? 'ACTIVE' : 'DISABLED' }}
            </span>
          </div>
          <p class="font-mono text-sm text-white/70">{{ key.maskedKey }}</p>
          <div class="grid grid-cols-2 gap-3 text-xs text-white/60">
            <div>
              <p class="text-white/40">今日调用</p>
              <p class="font-semibold text-white">{{ key.requestsToday }} / {{ key.dailyQuota }}</p>
            </div>
            <div>
              <p class="text-white/40">分钟限流</p>
              <p class="font-semibold text-white">{{ key.requestsCurrentMinute }} / {{ key.perMinuteQuota }}</p>
            </div>
            <div>
              <p class="text-white/40">累计调用</p>
              <p class="font-semibold text-white">{{ formatNumber(key.requestCount) }}</p>
            </div>
            <div>
              <p class="text-white/40">API 上传</p>
              <p class="font-semibold text-white">{{ formatNumber(key.uploadCount) }}</p>
            </div>
          </div>
          <div class="text-xs text-white/50">
            <p>上次使用：{{ key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleString() : '尚未调用' }}</p>
            <p>最近上传：{{ key.lastUploadAt ? new Date(key.lastUploadAt).toLocaleString() : '暂无记录' }}</p>
          </div>
          <div class="flex flex-wrap gap-2 text-xs">
            <button class="rounded-full border border-white/15 px-3 py-1 text-white/70 hover:border-brand-primary" @click="() => toggleKey(key)">
              {{ key.active ? '禁用' : '启用' }}
            </button>
            <button class="rounded-full border border-white/15 px-3 py-1 text-white/70 hover:border-brand-primary" @click="() => openLimitDialog(key)">限流策略</button>
            <button class="rounded-full border border-white/15 px-3 py-1 text-white/70 hover:border-brand-primary" @click="() => resetKey(key)">重置密钥</button>
            <button class="rounded-full border border-rose-400/40 px-3 py-1 text-rose-200 hover:border-rose-300" @click="() => removeKey(key)">删除</button>
          </div>
        </div>
      </div>
    </article>

    <article class="glass-panel border border-white/10 p-6 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold">嵌入与调用文档</h2>
        <button class="text-sm text-brand-primary" @click="openEmbedDoc">查看完整文档</button>
      </div>
      <p class="text-sm text-white/70">
        API 上传采用 <code class="font-mono text-white">multipart/form-data</code>，参数名称 <code class="font-mono text-white">files</code>，默认支持图片与短视频。更多场景可参考
        Demo/上传API实例.txt。
      </p>
    </article>
  </section>

  <div v-if="limitDialog.visible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
    <div class="w-full max-w-md rounded-3xl border border-white/10 bg-surface-panel p-6 shadow-2xl">
      <h3 class="text-xl font-semibold text-white">调整限流 · {{ limitDialog.name }}</h3>
      <div class="mt-4 space-y-4 text-sm text-white/80">
        <label class="block">
          每日调用上限
          <input v-model.number="limitDialog.dailyQuota" type="number" min="100" max="100000" class="mt-2 w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-white focus:border-brand-primary focus:outline-none" />
        </label>
        <label class="block">
          每分钟限流阈值
          <input v-model.number="limitDialog.perMinuteQuota" type="number" min="10" max="10000" class="mt-2 w-full rounded-2xl border border-white/15 bg-black/30 px-4 py-2 text-white focus:border-brand-primary focus:outline-none" />
        </label>
      </div>
      <div class="mt-6 flex items-center justify-end gap-3 text-sm">
        <button class="rounded-full border border-white/20 px-5 py-2 text-white/70" @click="limitDialog.visible = false">取消</button>
        <button class="rounded-full bg-gradient-to-r from-brand-primary to-brand-accent px-5 py-2 font-semibold text-white disabled:opacity-60" :disabled="limitDialog.saving" @click="saveLimits">
          {{ limitDialog.saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>
