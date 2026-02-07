<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { fetchSystemConfig, fetchSystemInsights, updateSystemConfig } from '../services/system'
import { usePendingChangesStore } from '../stores/pendingChanges'

const form = reactive({
  assetDomain: '',
  maxUploadMb: 5,
  maxVideoUploadMb: 100,
  dailyUploadCountLimit: 5000,
  userStorageQuotaGb: 5,
  registrationEnabled: false,
  guestLikeEnabled: true,
  autoCleanupDays: 30,
  videoChunkUploadEnabled: true,
  videoChunkSizeMb: 5,
  customFooterHtml: '',
  aiModerationEnabled: false,
  aiLabelingEnabled: false,
  aiTencentSecretId: '',
  aiTencentSecretKey: '',
  aiTencentRegion: '',
  aiTencentBucket: '',
  aiTencentDetectScenes: 'web,camera,album,news',
  aiModerationBlockConfidence: 90,
  aiModerationReviewConfidence: 60,
  aiLabelMinConfidence: 60,
})

const pendingChanges = usePendingChangesStore()
const isSyncingConfig = ref(false)
const initialSnapshot = ref('')
const serializeFormState = () =>
  JSON.stringify({
    assetDomain: form.assetDomain,
    maxUploadMb: form.maxUploadMb,
    maxVideoUploadMb: form.maxVideoUploadMb,
    dailyUploadCountLimit: form.dailyUploadCountLimit,
    userStorageQuotaGb: form.userStorageQuotaGb,
    registrationEnabled: form.registrationEnabled,
    guestLikeEnabled: form.guestLikeEnabled,
    autoCleanupDays: form.autoCleanupDays,
    videoChunkUploadEnabled: form.videoChunkUploadEnabled,
    videoChunkSizeMb: form.videoChunkSizeMb,
    customFooterHtml: form.customFooterHtml,
    aiModerationEnabled: form.aiModerationEnabled,
    aiLabelingEnabled: form.aiLabelingEnabled,
    aiTencentSecretId: form.aiTencentSecretId,
    aiTencentSecretKey: form.aiTencentSecretKey,
    aiTencentRegion: form.aiTencentRegion,
    aiTencentBucket: form.aiTencentBucket,
    aiTencentDetectScenes: form.aiTencentDetectScenes,
    aiModerationBlockConfidence: form.aiModerationBlockConfidence,
    aiModerationReviewConfidence: form.aiModerationReviewConfidence,
    aiLabelMinConfidence: form.aiLabelMinConfidence,
  })

const captureSnapshot = () => {
  initialSnapshot.value = serializeFormState()
  pendingChanges.setAdminSystemConfigDirty(false)
}

const configMeta = ref(null)
const insights = ref(null)
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const hasClientDom = typeof window !== 'undefined' && typeof document !== 'undefined'

const sanitizePlain = (value) => (value ?? '').trim()

const normalizeMarkup = (markup) => {
  if (!hasClientDom) {
    return markup
  }
  const template = document.createElement('template')
  template.innerHTML = markup
  return template.innerHTML
}

const sanitizeDomain = (value) => {
  const raw = (value || '').trim()
  if (!raw) return ''
  let normalized = raw
  if (!normalized.startsWith('http://') && !normalized.startsWith('https://')) {
    normalized = `https://${normalized}`
  }
  while (normalized.length > 1 && normalized.endsWith('/')) {
    normalized = normalized.slice(0, -1)
  }
  return normalized
}

const normalizedDomain = computed(() => sanitizeDomain(form.assetDomain))
const publicPreview = computed(() =>
  normalizedDomain.value ? `${normalizedDomain.value}/upload/picture/2025/12/preview.png` : '尚未配置'
)
const footerDiagnostics = computed(() => {
  const raw = (form.customFooterHtml || '').trim()
  if (!raw) {
    return { sanitized: '', warning: '', error: '' }
  }
  try {
    const normalized = normalizeMarkup(raw)
    const sanitized = DOMPurify.sanitize(normalized, {
      USE_PROFILES: { html: true },
      ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[^a-z]|[a-z+.-]+(?:[^a-z]|$))/i,
    })
    return {
      sanitized,
      warning:
        normalized !== raw ? '检测到未闭合或需自动补全的标签，系统会在展示时自动修复，请尽快校正。' : '',
      error: '',
    }
  } catch (error) {
    const safeText = DOMPurify.sanitize(raw, { ALLOWED_TAGS: [] })
    return {
      sanitized: safeText,
      warning: '',
      error: '自定义页脚存在无法解析的标签，请修复后再保存。',
    }
  }
})
const footerPreview = computed(() => footerDiagnostics.value.sanitized)

const loadAll = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [config, overview] = await Promise.all([
      fetchSystemConfig(),
      fetchSystemInsights(),
    ])
    applyConfig(config)
    insights.value = overview
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '加载系统配置失败'
  } finally {
    loading.value = false
  }
}

const applyConfig = (config) => {
  isSyncingConfig.value = true
  configMeta.value = config
  form.assetDomain = config?.assetDomain || ''
  form.maxUploadMb = Math.round(config?.maxUploadMegabytes ?? config?.maxUploadBytes / (1024 * 1024)) || 5
  form.maxVideoUploadMb = Math.round(config?.maxVideoUploadMegabytes ?? config?.maxVideoUploadBytes / (1024 * 1024)) || 100
  form.dailyUploadCountLimit = config?.dailyUploadCountLimit ?? 5000
  form.userStorageQuotaGb = Math.round(config?.userStorageQuotaGigabytes ?? config?.userStorageQuotaBytes / (1024 * 1024 * 1024)) || 5
  form.registrationEnabled = Boolean(config?.registrationEnabled)
  form.guestLikeEnabled = config?.guestLikeEnabled ?? true
  form.autoCleanupDays = Number.isFinite(config?.autoCleanupDays) ? config.autoCleanupDays : 30
  form.videoChunkUploadEnabled = config?.videoChunkUploadEnabled ?? true
  form.videoChunkSizeMb = config?.videoChunkSizeMb ?? 5
  form.customFooterHtml = config?.customFooterHtml || ''
  form.aiModerationEnabled = Boolean(config?.aiModerationEnabled)
  form.aiLabelingEnabled = Boolean(config?.aiLabelingEnabled)
  form.aiTencentSecretId = config?.aiTencentSecretId || ''
  form.aiTencentSecretKey = config?.aiTencentSecretKey || ''
  form.aiTencentRegion = config?.aiTencentRegion || ''
  form.aiTencentBucket = config?.aiTencentBucket || ''
  form.aiTencentDetectScenes = config?.aiTencentDetectScenes || 'web,camera,album,news'
  form.aiModerationBlockConfidence = Number.isFinite(config?.aiModerationBlockConfidence)
    ? config.aiModerationBlockConfidence
    : 90
  form.aiModerationReviewConfidence = Number.isFinite(config?.aiModerationReviewConfidence)
    ? config.aiModerationReviewConfidence
    : 60
  form.aiLabelMinConfidence = Number.isFinite(config?.aiLabelMinConfidence)
    ? config.aiLabelMinConfidence
    : 60
  captureSnapshot()
  isSyncingConfig.value = false
}

const handleSave = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  saving.value = true
  try {
    if (!normalizedDomain.value) {
      errorMessage.value = '请先填写公开访问域名'
      return
    }
    if (footerDiagnostics.value.error) {
      ElMessage.error(footerDiagnostics.value.error)
      return
    }
    if (footerDiagnostics.value.warning) {
      ElMessage.warning(footerDiagnostics.value.warning)
    }
    const payload = {
      maxUploadMb: form.maxUploadMb,
      maxVideoUploadMb: form.maxVideoUploadMb,
      dailyUploadCountLimit: form.dailyUploadCountLimit,
      userStorageQuotaGb: form.userStorageQuotaGb,
      registrationEnabled: form.registrationEnabled,
      guestLikeEnabled: form.guestLikeEnabled,
      autoCleanupDays: form.autoCleanupDays,
      videoChunkUploadEnabled: form.videoChunkUploadEnabled,
      videoChunkSizeMb: form.videoChunkSizeMb,
      assetDomain: normalizedDomain.value,
      customFooterHtml: form.customFooterHtml,
      aiModerationEnabled: form.aiModerationEnabled,
      aiLabelingEnabled: form.aiLabelingEnabled,
      aiTencentSecretId: sanitizePlain(form.aiTencentSecretId),
      aiTencentSecretKey: sanitizePlain(form.aiTencentSecretKey),
      aiTencentRegion: sanitizePlain(form.aiTencentRegion),
      aiTencentBucket: sanitizePlain(form.aiTencentBucket),
      aiTencentDetectScenes: sanitizePlain(form.aiTencentDetectScenes),
      aiModerationBlockConfidence: form.aiModerationBlockConfidence,
      aiModerationReviewConfidence: form.aiModerationReviewConfidence,
      aiLabelMinConfidence: form.aiLabelMinConfidence,
    }
    const updated = await updateSystemConfig(payload)
    applyConfig(updated)
    successMessage.value = '设置成功'
    ElMessage.success('设置成功')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

const limitBadge = computed(() => `${form.maxUploadMb} MB`)
const storageBadge = computed(() => `${form.userStorageQuotaGb} GB`)

const emailCompletionPercent = computed(() => {
  if (!insights.value?.totalUsers) return 0
  return Math.round((insights.value.emailCompletionRate || 0) * 1000) / 10
})

const statCards = computed(() => {
  if (!insights.value) {
    return []
  }
  return [
    {
      label: '注册用户',
      value: insights.value.totalUsers,
      hint: `含管理员 ${insights.value.adminUsers}`,
    },
    {
      label: '绑定邮箱率',
      value: `${emailCompletionPercent.value}%`,
      hint: `${insights.value.usersWithEmail} 人填写邮箱`,
    },
    {
      label: '累计上传',
      value: insights.value.totalUploads,
      hint: `今日 +${insights.value.todayUploads}`,
    },
    {
      label: '占用存储',
      value: `${(insights.value.totalStorageGigabytes || 0).toFixed(2)} GB`,
      hint: `${insights.value.totalStorageBytes?.toLocaleString() || 0} Bytes`,
    },
  ]
})

const diagnosticBlocks = computed(() => [
  {
    title: '401 未授权',
    subtitle: '多因登录态过期或未携带 API Key',
    tips: [
      '请确认前端 Pinia 中保留的 Basic Token 未过期，或重新登录刷新凭证',
      '调用 API 时务必附带 Authorization 头或配置 X-API-Key',
      '若通过内网访问，请确认配置的域名/端口在 CORS 白名单中',
    ],
  },
  {
    title: '413 请求体过大',
    subtitle: `当前单文件限制 ${configMeta.value?.maxUploadMegabytes ?? form.maxUploadMb} MB，可在此页上调`,
    tips: [
      '上传大图前可压缩或分批次上传',
      '管理员可即时调整上传上限并重新尝试',
      '若客户端频繁触发，可在日志中排查异常脚本',
    ],
  },
])

captureSnapshot()

watch(
  () => serializeFormState(),
  (current) => {
    if (isSyncingConfig.value) {
      return
    }
    pendingChanges.setAdminSystemConfigDirty(current !== initialSnapshot.value)
  }
)

onMounted(() => {
  loadAll()
})

onBeforeUnmount(() => {
  pendingChanges.setAdminSystemConfigDirty(false)
})
</script>

<template>
  <section class="space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">system</p>
      <h1 class="text-4xl font-semibold text-gradient">系统配置中心</h1>
      <p class="text-white/70 mt-2 max-w-3xl">
        设置上传大小、配额与开放注册策略，所有更改将实时持久化到 MySQL，便于后续审计与备份。
      </p>
    </header>

    <div v-if="errorMessage" class="glass-panel border border-rose-500/40 p-4 text-sm text-rose-100">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="glass-panel border border-emerald-400/30 p-4 text-sm text-emerald-100">
      {{ successMessage }}
    </div>

    <div v-if="loading" class="glass-panel border border-white/10 p-6 animate-pulse text-white/70">
      正在加载配置...
    </div>

    <div v-else class="grid gap-6 lg:grid-cols-[2fr,1fr]">
      <form class="glass-panel p-6 space-y-5" @submit.prevent="handleSave" novalidate>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-white/60">上传策略</p>
            <h2 class="text-2xl font-semibold">大小与配额</h2>
          </div>
          <span class="text-xs px-3 py-1 rounded-full bg-white/10 text-white/60">
            最近更新：{{ configMeta?.updatedAt ? new Date(configMeta.updatedAt).toLocaleString() : 'N/A' }}
          </span>
        </div>

        <div class="grid gap-4 lg:grid-cols-2">
          <div class="rounded-2xl border border-white/10 bg-black/20 p-4 space-y-3">
            <label class="text-sm text-white/70">公开访问域名</label>
            <input
              v-model="form.assetDomain"
              type="text"
              placeholder="https://luminouschenxi.net"
              class="w-full rounded-xl border border-white/15 bg-black/40 px-4 py-2 text-white placeholder:text-white/40"
            />
            <p class="text-xs text-white/60">访问示例：{{ publicPreview }}</p>
            <p class="text-xs text-white/40">磁盘落点固定为 <span class="font-semibold text-white/70">/storage/upload/picture|video/{year}/{month}</span></p>
          </div>
          <div class="rounded-2xl border border-white/10 bg-black/20 p-4 text-sm text-white/70">
            <p class="font-semibold text-white">路径说明</p>
            <p class="mt-2">后端会按照上传时间自动创建年份/月份文件夹，并通过 /upload/** 映射到静态资源。域名仅用于外部访问，可随时修改。</p>
          </div>
        </div>

        <label class="block text-sm text-white/70">单图片文件大小上限</label>
        <div class="rounded-2xl border border-white/10 bg-black/20 p-4">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <p class="text-3xl font-semibold">{{ limitBadge }}</p>
            <input
              v-model.number="form.maxUploadMb"
              type="number"
              min="1"
              max="512"
              class="w-28 rounded-xl bg-black/40 border border-white/10 px-3 py-2 text-center"
            />
          </div>
          <input v-model.number="form.maxUploadMb" type="range" min="1" max="512" class="mt-4 w-full" />
          <p class="text-xs text-white/60 mt-2">
            说明：当前后端默认 5 MB。如需支持原图，请适当增大但留意磁盘与带宽成本。
          </p>
        </div>

        <label class="mt-6 block text-sm text-white/70">单短视频文件上传策略</label>
        <div class="rounded-2xl border border-white/10 bg-black/20 p-4 space-y-4">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p class="text-2xl font-semibold">{{ form.maxVideoUploadMb }} MB</p>
              <p class="text-xs text-white/60">用于 mp4 / webm 等短视频</p>
            </div>
            <input
              v-model.number="form.maxVideoUploadMb"
              type="number"
              min="10"
              max="2048"
              class="w-32 rounded-xl bg-black/40 border border-white/10 px-3 py-2 text-center"
            />
          </div>
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p class="text-sm font-medium text-white">分片上传</p>
              <p class="text-xs text-white/60">适合大于 50 MB 的短视频，降低失败率</p>
            </div>
            <button
              class="relative h-8 w-16 rounded-full transition"
              :class="form.videoChunkUploadEnabled ? 'bg-brand-primary/80' : 'bg-white/20'"
              @click="form.videoChunkUploadEnabled = !form.videoChunkUploadEnabled"
            >
              <span
                class="absolute top-1 h-6 w-6 rounded-full bg-white transition"
                :class="form.videoChunkUploadEnabled ? 'right-1' : 'left-1'"
              ></span>
            </button>
          </div>
          <div class="flex items-center gap-3" :class="form.videoChunkUploadEnabled ? '' : 'opacity-50'">
            <label class="text-xs text-white/60">单片大小 (MB)</label>
            <input
              v-model.number="form.videoChunkSizeMb"
              type="number"
              min="1"
              max="512"
              class="w-24 rounded-xl bg-black/40 border border-white/10 px-3 py-2 text-center"
              :disabled="!form.videoChunkUploadEnabled"
            />
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="rounded-2xl border border-white/10 bg-black/20 p-4 space-y-3">
            <label class="text-sm text-white/70">每日上传次数 / 用户</label>
            <input
              v-model.number="form.dailyUploadCountLimit"
              type="number"
              min="1"
              max="100000"
              class="w-full rounded-xl bg-black/40 border border-white/10 px-4 py-2"
            />
            <p class="text-xs text-white/60">超出后可在用户中心提示或转人工审核。</p>
          </div>
          <div class="rounded-2xl border border-white/10 bg-black/20 p-4 space-y-3">
            <label class="text-sm text-white/70">单用户空间配额</label>
            <div class="flex items-center gap-3">
              <input
                v-model.number="form.userStorageQuotaGb"
                type="number"
                min="1"
                max="2048"
                class="flex-1 rounded-xl bg-black/40 border border-white/10 px-4 py-2"
              />
              <span class="text-sm text-white/70">GB</span>
            </div>
            <p class="text-xs text-white/60">当前配额：{{ storageBadge }}，达到上限将暂停新上传。</p>
          </div>
        </div>

        <div class="rounded-2xl border border-white/10 bg-black/20 p-4 flex items-center justify-between">
          <div>
            <p class="text-sm font-medium">开放注册</p>
            <p class="text-xs text-white/60">关闭后仅管理员可创建账号，保障内网私有部署安全。</p>
          </div>
          <button
            class="relative h-8 w-16 rounded-full transition"
            :class="form.registrationEnabled ? 'bg-emerald-400/80' : 'bg-white/20'"
            @click="form.registrationEnabled = !form.registrationEnabled"
          >
            <span
              class="absolute top-1 h-6 w-6 rounded-full bg-white transition"
              :class="form.registrationEnabled ? 'right-1' : 'left-1'"
            ></span>
          </button>
        </div>

        <div class="rounded-2xl border border-brand-primary/30 bg-black/15 p-4 flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-brand-primary">访客点赞</p>
            <p class="text-xs text-white/60">允许未登录访客为图库内容点赞，昵称将以“访客”展示。</p>
          </div>
          <button
            class="relative h-8 w-16 rounded-full transition"
            :class="form.guestLikeEnabled ? 'bg-brand-primary/80' : 'bg-white/20'"
            @click="form.guestLikeEnabled = !form.guestLikeEnabled"
          >
            <span
              class="absolute top-1 h-6 w-6 rounded-full bg-white transition"
              :class="form.guestLikeEnabled ? 'right-1' : 'left-1'"
            ></span>
          </button>
        </div>

        <div class="rounded-2xl border border-white/10 bg-black/20 p-4 space-y-3">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-white">自动清理策略</p>
              <p class="text-xs text-white/60">单张图片连续 N 天无访问记录后自动下线，释放存储与 CDN 资源。</p>
            </div>
            <span class="rounded-full border border-white/20 px-3 py-1 text-xs text-white/60">UTC+8 深夜执行</span>
          </div>
          <div class="flex items-center gap-3">
            <input
              v-model.number="form.autoCleanupDays"
              type="number"
              min="0"
              max="365"
              class="w-28 rounded-xl bg-black/40 border border-white/10 px-3 py-2 text-center"
            />
            <span class="text-sm text-white/70">天</span>
          </div>
          <p class="text-xs text-white/60">填写 0 可关闭自动清理。建议 30-180 天，让长尾文件自动回收。</p>
        </div>

        <div class="rounded-2xl border border-cyan-400/30 bg-black/20 p-4 space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/60">AI 智能审核</p>
              <h2 class="text-xl font-semibold text-white">腾讯云违规识别 &amp; 标签</h2>
              <p class="text-xs text-white/50 mt-1">
                需保证公开访问域名可被腾讯云访问，命中阈值后会删除原文件并返回 2048x2048 违规占位图。
              </p>
            </div>
            <span class="text-[10px] uppercase tracking-[0.4em] text-cyan-200/80">beta</span>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="flex items-center justify-between rounded-xl border border-white/10 bg-black/30 p-4">
              <div>
                <p class="text-sm text-white/80">违规审核</p>
                <p class="text-xs text-white/50">拦截色情/涉政/涉暴，保留审核记录。</p>
              </div>
              <button
                class="relative h-8 w-16 rounded-full transition"
                :class="form.aiModerationEnabled ? 'bg-cyan-400/80' : 'bg-white/20'"
                @click="form.aiModerationEnabled = !form.aiModerationEnabled"
              >
                <span
                  class="absolute top-1 h-6 w-6 rounded-full bg-white transition"
                  :class="form.aiModerationEnabled ? 'right-1' : 'left-1'"
                ></span>
              </button>
            </div>
            <div class="flex items-center justify-between rounded-xl border border-white/10 bg-black/30 p-4">
              <div>
                <p class="text-sm text-white/80">自动标签</p>
                <p class="text-xs text-white/50">与用户自定义标签一并入库，增强搜索。</p>
              </div>
              <button
                class="relative h-8 w-16 rounded-full transition"
                :class="form.aiLabelingEnabled ? 'bg-cyan-400/80' : 'bg-white/20'"
                @click="form.aiLabelingEnabled = !form.aiLabelingEnabled"
              >
                <span
                  class="absolute top-1 h-6 w-6 rounded-full bg-white transition"
                  :class="form.aiLabelingEnabled ? 'right-1' : 'left-1'"
                ></span>
              </button>
            </div>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <label class="text-xs text-white/60">SecretId</label>
              <input
                v-model="form.aiTencentSecretId"
                type="text"
                autocomplete="off"
                class="w-full rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-white placeholder:text-white/30"
                placeholder="AKIDxxxxxxxx"
              />
            </div>
            <div class="space-y-2">
              <label class="text-xs text-white/60">SecretKey</label>
              <input
                v-model="form.aiTencentSecretKey"
                type="password"
                autocomplete="new-password"
                class="w-full rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-white placeholder:text-white/30"
                placeholder="1A2Z3Yxxxxx"
              />
            </div>
            <div class="space-y-2">
              <label class="text-xs text-white/60">Region (例如 ap-beijing)</label>
              <input
                v-model="form.aiTencentRegion"
                type="text"
                class="w-full rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-white placeholder:text-white/30"
                placeholder="ap-beijing"
              />
            </div>
            <div class="space-y-2">
              <label class="text-xs text-white/60">Bucket 名称 (bucket-appid)</label>
              <input
                v-model="form.aiTencentBucket"
                type="text"
                class="w-full rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-white placeholder:text-white/30"
                placeholder="demo-1328487995"
              />
            </div>
            <div class="space-y-2">
              <label class="text-xs text-white/60">识别场景 (逗号分隔)</label>
              <input
                v-model="form.aiTencentDetectScenes"
                type="text"
                class="w-full rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-white placeholder:text-white/30"
                placeholder="web,camera,album,news"
              />
              <p class="text-[11px] text-white/50">官方建议至少启用 web + camera，以提高覆盖率。</p>
            </div>
            <div class="space-y-2">
              <label class="text-xs text-white/60">可用性提示</label>
              <div class="rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-[11px] text-white/60">
                需确保该 Bucket 允许外网访问；若使用本地存储，请配置公网反向代理后在此填写其域名。
              </div>
            </div>
          </div>
          <div class="grid gap-4 md:grid-cols-3">
            <div class="rounded-xl border border-white/10 bg-black/30 p-4 space-y-2">
              <div class="flex items-center justify-between text-xs text-white/60">
                <span>违规拦截阈值</span>
                <span class="text-white font-semibold">{{ form.aiModerationBlockConfidence }}%</span>
              </div>
              <input v-model.number="form.aiModerationBlockConfidence" type="range" min="0" max="100" class="w-full" />
            </div>
            <div class="rounded-xl border border-white/10 bg-black/30 p-4 space-y-2">
              <div class="flex items-center justify-between text-xs text-white/60">
                <span>疑似复核阈值</span>
                <span class="text-white font-semibold">{{ form.aiModerationReviewConfidence }}%</span>
              </div>
              <input v-model.number="form.aiModerationReviewConfidence" type="range" min="0" max="100" class="w-full" />
            </div>
            <div class="rounded-xl border border-white/10 bg-black/30 p-4 space-y-2">
              <div class="flex items-center justify-between text-xs text-white/60">
                <span>标签置信度</span>
                <span class="text-white font-semibold">{{ form.aiLabelMinConfidence }}%</span>
              </div>
              <input v-model.number="form.aiLabelMinConfidence" type="range" min="0" max="100" class="w-full" />
            </div>
          </div>
        </div>

        <button
          type="submit"
          class="w-full rounded-2xl bg-brand-primary text-surface-panel py-3 font-semibold disabled:opacity-60"
          :disabled="saving"
        >
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </form>

      <article class="glass-panel p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-white/60">概要</p>
            <h2 class="text-xl font-semibold">数据盘点</h2>
          </div>
          <button class="text-sm text-brand-primary" @click="loadAll">刷新</button>
        </div>
        <div class="grid gap-3">
          <div
            v-for="card in statCards"
            :key="card.label"
            class="rounded-2xl bg-white/5 border border-white/5 px-4 py-3"
          >
            <p class="text-xs text-white/60">{{ card.label }}</p>
            <p class="text-2xl font-semibold">{{ card.value }}</p>
            <p class="text-xs text-white/60">{{ card.hint }}</p>
          </div>
        </div>
      </article>
    </div>

    <article class="glass-panel p-6 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-white/60">自定义页脚</p>
          <h2 class="text-2xl font-semibold">公共页脚 HTML</h2>
        </div>
        <span class="text-xs text-white/60">Landing · 登录 · 注册 · 会员页</span>
      </div>
      <textarea
        v-model="form.customFooterHtml"
        rows="6"
        class="w-full rounded-2xl border border-white/10 bg-black/30 px-4 py-3 text-sm text-white placeholder:text-white/40"
        placeholder="<p>备案号 · 联系方式等 HTML 片段</p>"
      ></textarea>
      <el-alert
        v-if="footerDiagnostics.warning"
        type="warning"
        show-icon
        :closable="false"
        :title="footerDiagnostics.warning"
      />
      <el-alert
        v-if="footerDiagnostics.error"
        type="error"
        show-icon
        :closable="false"
        :title="footerDiagnostics.error"
      />
      <p class="text-xs text-white/60">
        该内容会直接插入在页脚的默认信息上方，可用于备案号、隐私声明等自定义 HTML，请确保代码安全。
      </p>
      <div v-if="footerPreview" class="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-sm text-white/80">
        <p class="text-xs uppercase tracking-[0.5em] text-white/50">预览</p>
        <div class="mt-3 space-y-2" v-html="footerPreview"></div>
      </div>
      <div class="flex justify-end">
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存自定义页脚' }}
        </el-button>
      </div>
    </article>

    <article class="glass-panel p-6 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-white/60">诊断与指引</p>
          <h2 class="text-2xl font-semibold">常见错误定位</h2>
        </div>
        <span class="text-xs text-white/60">401 &amp; 413</span>
      </div>
      <div class="grid gap-4 md:grid-cols-2">
        <div
          v-for="block in diagnosticBlocks"
          :key="block.title"
          class="rounded-2xl border border-white/10 bg-black/30 p-4"
        >
          <h3 class="text-lg font-semibold">{{ block.title }}</h3>
          <p class="text-xs text-white/60 mt-1">{{ block.subtitle }}</p>
          <ul class="mt-3 space-y-2 text-sm text-white/70 list-disc list-inside">
            <li v-for="tip in block.tips" :key="tip">{{ tip }}</li>
          </ul>
        </div>
      </div>
    </article>
  </section>
</template>
