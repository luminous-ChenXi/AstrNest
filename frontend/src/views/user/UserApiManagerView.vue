<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createUserApiKey,
  deleteUserApiKey,
  fetchUserApiKeys,
  resetUserApiKey,
  updateUserApiKeyStatus,
} from '../../services/apiKeys'

const apiKeys = ref([])
const loading = ref(false)
const creating = ref(false)
const notifications = reactive({ success: '', error: '' })
const newKeyForm = reactive({ name: '', description: '' })
const singleUseKey = ref('')
const activeSnippet = ref('curl')
const apiHeaderName = 'X-API-Key'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

const cards = computed(() => {
  const totalKeys = apiKeys.value.length
  const activeKeys = apiKeys.value.filter((key) => key.active).length
  const todayRequests = apiKeys.value.reduce((sum, key) => sum + key.requestsToday, 0)
  const uploadCount = apiKeys.value.reduce((sum, key) => sum + key.uploadCount, 0)
  return [
    { label: '密钥总数', value: totalKeys },
    { label: '活跃密钥', value: activeKeys },
    { label: '今日调用', value: todayRequests },
    { label: 'API 上传', value: uploadCount },
  ]
})

const codeSamples = {
  curl: `curl -X POST \\\n  -H "${apiHeaderName}: ik_your_key_here" \\\n  -F "files=@/path/to/image.jpg" \\\n  ${apiBaseUrl}/api/uploads`,
  js: `const fileInput = document.querySelector('input[type="file"]')
const formData = new FormData()
formData.append('files', fileInput.files[0])
formData.append('cdn_domain', 'img.scdn.io')
await fetch('${apiBaseUrl}/api/uploads', {
  method: 'POST',
  headers: { '${apiHeaderName}': 'ik_your_key_here' },
  body: formData,
})`,
  python: `import requests

url = '${apiBaseUrl}/api/uploads'
headers = { '${apiHeaderName}': 'ik_your_key_here' }
with open('/path/to/image.jpg', 'rb') as f:
    files = { 'files': f }
    data = { 'cdn_domain': 'img.scdn.io' }
    response = requests.post(url, headers=headers, files=files, data=data)
print(response.json())`,
  php: `<?php
$headers = ['${apiHeaderName}: ik_your_key_here'];
$ch = curl_init('${apiBaseUrl}/api/uploads');
$data = ['files' => new CURLFile('/path/to/image.jpg')];
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$resp = curl_exec($ch);
curl_close($ch);
echo $resp;
?>`,
}

const snippetTabs = [
  { key: 'curl', label: 'cURL' },
  { key: 'js', label: 'JavaScript' },
  { key: 'python', label: 'Python' },
  { key: 'php', label: 'PHP' },
]

const resetNotifications = () => {
  notifications.success = ''
  notifications.error = ''
}

const notifyError = (message) => {
  notifications.error = message
  notifications.success = ''
}

const notifySuccess = (message) => {
  notifications.success = message
  notifications.error = ''
}

const loadKeys = async () => {
  loading.value = true
  try {
    const { data } = await fetchUserApiKeys()
    apiKeys.value = data || []
  } catch (error) {
    notifyError(error?.response?.data?.message || '加载 API 密钥失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadKeys)

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
    }
    const { data } = await createUserApiKey(payload)
    singleUseKey.value = data?.plainValue || ''
    notifySuccess('密钥创建成功，请保存明文值')
    newKeyForm.name = ''
    newKeyForm.description = ''
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '创建密钥失败')
  } finally {
    creating.value = false
  }
}

const toggleKey = async (key) => {
  resetNotifications()
  try {
    await updateUserApiKeyStatus(key.id, !key.active)
    notifySuccess(!key.active ? '密钥已启用' : '密钥已禁用')
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '更新状态失败')
  }
}

const resetKey = async (key) => {
  resetNotifications()
  try {
    const { data } = await resetUserApiKey(key.id)
    singleUseKey.value = data?.plainValue || ''
    notifySuccess('密钥已重置，请保存新明文')
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '重置失败')
  }
}

const removeKey = async (key) => {
  if (!confirm(`确定删除「${key.name}」吗？`)) return
  resetNotifications()
  try {
    await deleteUserApiKey(key.id)
    notifySuccess('密钥已删除')
    await loadKeys()
  } catch (error) {
    notifyError(error?.response?.data?.message || '删除失败')
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
</script>

<template>
  <div class="user-api space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-body-soft">developer api</p>
      <h1 class="text-3xl font-semibold text-gradient">API 接口管理</h1>
      <p class="mt-2 text-body-muted">
        使用专属 API 密钥将图片 / 短视频直接上传到 AstrNest，支持 CDN 域名绑定、密码保护与多种输出格式。请妥善保管明文密钥，避免泄露。
      </p>
    </header>

    <div v-if="notifications.error" class="glass-panel border border-rose-500/40 p-4 text-sm text-rose-100">
      {{ notifications.error }}
    </div>
    <div v-if="notifications.success" class="glass-panel border border-emerald-500/40 p-4 text-sm text-emerald-200">
      {{ notifications.success }}
    </div>

    <div class="grid gap-4 md:grid-cols-2">
      <article class="glass-panel border border-body p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-body-soft">创建密钥</p>
            <h2 class="text-xl font-semibold">新的访问凭证</h2>
          </div>
        </div>
        <div class="space-y-3">
          <input v-model="newKeyForm.name" class="w-full rounded-2xl border border-body bg-surface-strong px-4 py-2 text-sm text-body-primary placeholder:text-body-faint focus:border-brand-primary focus:outline-none" placeholder="密钥名称 (例如：桌面 Typora)" />
          <input v-model="newKeyForm.description" class="w-full rounded-2xl border border-body bg-surface-strong px-4 py-2 text-sm text-body-primary placeholder:text-body-faint focus:border-brand-primary focus:outline-none" placeholder="备注信息 (可选)" />
          <button class="w-full rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent py-3 text-sm font-semibold text-white shadow-[0_15px_45px_rgba(127,123,255,0.35)] disabled:opacity-60" :disabled="creating" @click="handleCreate">
            {{ creating ? '创建中…' : '生成密钥' }}
          </button>
        </div>
        <div class="rounded-2xl border border-body bg-surface-strong p-4 text-xs text-body-soft">
          <p>上传端点：<code class="font-mono text-body-primary">POST {{ apiBaseUrl }}/api/uploads</code></p>
          <p class="mt-2">Header：<code class="font-mono text-body-primary">{{ apiHeaderName }}</code></p>
        </div>
      </article>

      <article class="glass-panel border border-body p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-body-soft">调用指引</p>
            <h2 class="text-xl font-semibold">支持的请求参数</h2>
          </div>
        </div>
        <ul class="space-y-2 text-sm text-body-muted">
          <li><span class="font-semibold text-body-primary">image / files</span>：必填，上传的图片或短视频，需使用 <code class="font-mono text-body-primary">multipart/form-data</code>。</li>
          <li><span class="font-semibold text-body-primary">outputFormat</span>：可选，支持 auto / jpeg / png / webp / gif / webp_animated。</li>
          <li><span class="font-semibold text-body-primary">password_enabled + image_password</span>：开启访问密码保护。</li>
          <li><span class="font-semibold text-body-primary">cdn_domain</span>：可选，指定返回的外链 CDN 域名（需提前在后台授权）。</li>
          <li>所有请求需携带 <code class="font-mono text-body-primary">{{ apiHeaderName }}</code>，值为明文密钥，例如 <code class="font-mono text-body-primary">ik_xxx_xxx</code>。</li>
        </ul>
      </article>
    </div>

    <div class="grid gap-4 md:grid-cols-4">
      <article v-for="card in cards" :key="card.label" class="glass-panel border border-body p-4">
        <p class="text-xs uppercase tracking-[0.4em] text-body-faint">{{ card.label }}</p>
        <p class="mt-2 text-3xl font-semibold">{{ formatNumber(card.value) }}</p>
      </article>
    </div>

    <article v-if="singleUseKey" class="glass-panel border border-amber-400/40 p-6 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold">一次性明文密钥</h2>
        <button class="text-sm text-brand-primary" @click="copyValue(singleUseKey)">复制</button>
      </div>
      <p class="text-sm text-body-muted">该明文仅显示一次，如需重置可在列表中重新生成。</p>
      <div class="rounded-2xl bg-surface-strong border border-body px-4 py-3 font-mono text-sm break-all">{{ singleUseKey }}</div>
    </article>

    <article class="glass-panel border border-body p-6 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-body-soft">我的密钥</p>
          <h2 class="text-xl font-semibold">当前可用 {{ apiKeys.length }} 个</h2>
        </div>
        <button class="text-sm text-brand-primary" @click="loadKeys">刷新</button>
      </div>
      <div v-if="loading" class="py-10 text-center text-body-soft">加载中…</div>
      <div v-else-if="!apiKeys.length" class="py-10 text-center text-body-soft">暂未创建任何密钥</div>
      <div v-else class="grid gap-4 lg:grid-cols-2">
        <div v-for="key in apiKeys" :key="key.id" class="rounded-2xl border border-body bg-surface-strong p-4 space-y-3">
          <div class="flex items-start justify-between">
            <div>
              <p class="text-lg font-semibold text-body-primary">{{ key.name }}</p>
              <p class="text-xs text-body-faint">{{ key.description || '无备注' }}</p>
            </div>
            <span class="rounded-full px-3 py-1 text-xs" :class="key.active ? 'bg-emerald-400/20 text-emerald-200' : 'bg-surface-overlay text-body-faint'">
              {{ key.active ? 'ACTIVE' : 'DISABLED' }}
            </span>
          </div>
          <p class="font-mono text-sm text-body-muted">{{ key.maskedKey }}</p>
          <div class="grid grid-cols-2 gap-3 text-xs text-body-soft">
            <div>
              <p class="text-body-faint">今日调用</p>
              <p class="font-semibold text-body-primary">{{ key.requestsToday }} / {{ key.dailyQuota }}</p>
            </div>
            <div>
              <p class="text-body-faint">分钟限流</p>
              <p class="font-semibold text-body-primary">{{ key.requestsCurrentMinute }} / {{ key.perMinuteQuota }}</p>
            </div>
            <div>
              <p class="text-body-faint">累计调用</p>
              <p class="font-semibold text-body-primary">{{ formatNumber(key.requestCount) }}</p>
            </div>
            <div>
              <p class="text-body-faint">API 上传</p>
              <p class="font-semibold text-body-primary">{{ formatNumber(key.uploadCount) }}</p>
            </div>
          </div>
          <div class="text-xs text-body-faint">
            <p>上次使用：{{ key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleString() : '尚未调用' }}</p>
          </div>
          <div class="flex flex-wrap gap-2 text-xs">
            <button class="rounded-full border border-body px-3 py-1 text-body-muted hover:border-brand-primary" @click="() => toggleKey(key)">
              {{ key.active ? '禁用' : '启用' }}
            </button>
            <button class="rounded-full border border-body px-3 py-1 text-body-muted hover:border-brand-primary" @click="() => resetKey(key)">重置密钥</button>
            <button class="rounded-full border border-rose-400/40 px-3 py-1 text-rose-200 hover:border-rose-300" @click="() => removeKey(key)">删除</button>
          </div>
        </div>
      </div>
    </article>

    <article class="glass-panel border border-body p-6 space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-sm text-body-soft">代码示例</p>
          <h2 class="text-xl font-semibold">快速集成</h2>
        </div>
        <div class="flex flex-wrap gap-2">
          <button v-for="tab in snippetTabs" :key="tab.key" class="rounded-full px-4 py-1 text-xs font-semibold" :class="activeSnippet === tab.key ? 'bg-surface-strong text-body-primary' : 'bg-surface-overlay text-body-soft'" @click="activeSnippet = tab.key">
            {{ tab.label }}
          </button>
        </div>
      </div>
      <pre class="rounded-2xl border border-body bg-surface-strong p-4 text-sm text-body-secondary overflow-x-auto">{{ codeSamples[activeSnippet] }}</pre>
      <button class="text-sm text-brand-primary" @click="copyValue(codeSamples[activeSnippet])">复制代码片段</button>
    </article>
  </div>
</template>

<style scoped>
/* 所有样式已迁移到 Tailwind CSS 类 */
</style>
