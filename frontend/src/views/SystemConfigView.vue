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
  maxFilesPerUpload: 30,
  userStorageQuotaGb: 5,
  registrationEnabled: false,
  guestLikeEnabled: true,
  guestUploadEnabled: false,
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
    maxFilesPerUpload: form.maxFilesPerUpload,
    userStorageQuotaGb: form.userStorageQuotaGb,
    registrationEnabled: form.registrationEnabled,
    guestLikeEnabled: form.guestLikeEnabled,
    guestUploadEnabled: form.guestUploadEnabled,
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
  form.maxFilesPerUpload = config?.maxFilesPerUpload ?? 30
  form.userStorageQuotaGb = Math.round(config?.userStorageQuotaGigabytes ?? config?.userStorageQuotaBytes / (1024 * 1024 * 1024)) || 5
  form.registrationEnabled = Boolean(config?.registrationEnabled)
  form.guestLikeEnabled = config?.guestLikeEnabled ?? true
  form.guestUploadEnabled = config?.guestUploadEnabled ?? false
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
      maxFilesPerUpload: form.maxFilesPerUpload,
      userStorageQuotaGb: form.userStorageQuotaGb,
      registrationEnabled: form.registrationEnabled,
      guestLikeEnabled: form.guestLikeEnabled,
      guestUploadEnabled: form.guestUploadEnabled,
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
      color: 'pink',
    },
    {
      label: '绑定邮箱率',
      value: `${emailCompletionPercent.value}%`,
      hint: `${insights.value.usersWithEmail} 人填写邮箱`,
      color: 'mint',
    },
    {
      label: '累计上传',
      value: insights.value.totalUploads,
      hint: `今日 +${insights.value.todayUploads}`,
      color: 'sky',
    },
    {
      label: '占用存储',
      value: `${(insights.value.totalStorageGigabytes || 0).toFixed(2)} GB`,
      hint: `${insights.value.totalStorageBytes?.toLocaleString() || 0} Bytes`,
      color: 'coral',
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
    icon: '🔒',
  },
  {
    title: '413 请求体过大',
    subtitle: `当前单文件限制 ${configMeta.value?.maxUploadMegabytes ?? form.maxUploadMb} MB，可在此页上调`,
    tips: [
      '上传大图前可压缩或分批次上传',
      '管理员可即时调整上传上限并重新尝试',
      '若客户端频繁触发，可在日志中排查异常脚本',
    ],
    icon: '📦',
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
    <!-- 页面标题 -->
    <header class="mb-8">
      <p class="text-sm font-medium text-brand-primary/80 mb-1">System Configuration</p>
      <h1 class="text-3xl font-bold text-gradient">系统配置中心</h1>
      <p class="text-body-soft mt-2 max-w-3xl">
        设置上传大小、配额与开放注册策略，所有更改将实时持久化到 MySQL，便于后续审计与备份。
      </p>
    </header>

    <!-- 消息提示 -->
    <div v-if="errorMessage" class="rounded-2xl border border-rose-300 bg-rose-50 p-4 text-sm text-rose-700">
      <span class="mr-2">❌</span>{{ errorMessage }}
    </div>
    <div v-if="successMessage" class="rounded-2xl border border-emerald-300 bg-emerald-50 p-4 text-sm text-emerald-700">
      <span class="mr-2">✅</span>{{ successMessage }}
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="rounded-3xl bg-white/80 p-8 text-center text-body-soft shadow-sm">
      <div class="inline-block animate-spin rounded-full h-8 w-8 border-4 border-brand-primary/30 border-t-brand-primary mb-3"></div>
      <p>正在加载配置...</p>
    </div>

    <div v-else class="grid gap-6 lg:grid-cols-[2fr,1fr]">
      <!-- 左侧配置表单 -->
      <form class="space-y-6" @submit.prevent="handleSave" novalidate>
        <!-- 上传策略卡片 -->
        <div class="config-card">
          <div class="flex items-center justify-between mb-6">
            <div>
              <p class="text-xs font-medium text-brand-primary/70 uppercase tracking-wider">Upload Policy</p>
              <h2 class="text-xl font-bold text-body-primary">大小与配额</h2>
            </div>
            <span class="text-xs px-3 py-1.5 rounded-full bg-surface-secondary text-body-soft">
              最近更新：{{ configMeta?.updatedAt ? new Date(configMeta.updatedAt).toLocaleString() : 'N/A' }}
            </span>
          </div>

          <!-- 域名配置 -->
          <div class="grid gap-4 lg:grid-cols-2 mb-6">
            <div class="config-section">
              <label class="config-label">公开访问域名</label>
              <input
                v-model="form.assetDomain"
                type="text"
                placeholder="https://luminouschenxi.net"
                class="config-input"
              />
              <p class="config-hint">访问示例：{{ publicPreview }}</p>
              <p class="config-hint text-xs">磁盘落点固定为 <span class="font-medium text-brand-primary">/storage/upload/picture|video/{year}/{month}</span></p>
            </div>
            <div class="config-section bg-brand-mint/10">
              <div class="flex items-start gap-3">
                <span class="text-2xl">📁</span>
                <div>
                  <p class="font-semibold text-body-primary mb-1">路径说明</p>
                  <p class="text-sm text-body-soft leading-relaxed">后端会按照上传时间自动创建年份/月份文件夹，并通过 /upload/** 映射到静态资源。域名仅用于外部访问，可随时修改。</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 单图片上传限制 -->
          <div class="config-section mb-6">
            <label class="config-label">单图片文件大小上限</label>
            <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
              <div class="flex items-baseline gap-2">
                <span class="text-4xl font-bold text-gradient">{{ limitBadge }}</span>
              </div>
              <input
                v-model.number="form.maxUploadMb"
                type="number"
                min="1"
                max="512"
                class="w-24 config-input text-center"
              />
            </div>
            <input v-model.number="form.maxUploadMb" type="range" min="1" max="512" class="config-range" />
            <p class="config-hint mt-3">
              说明：当前后端默认 5 MB。如需支持原图，请适当增大但留意磁盘与带宽成本。
            </p>
          </div>

          <!-- 视频上传策略 -->
          <div class="config-section">
            <label class="config-label">单短视频文件上传策略</label>
            <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
              <div>
                <p class="text-3xl font-bold text-brand-accent">{{ form.maxVideoUploadMb }} MB</p>
                <p class="config-hint">用于 mp4 / webm 等短视频</p>
              </div>
              <input
                v-model.number="form.maxVideoUploadMb"
                type="number"
                min="10"
                max="2048"
                class="w-28 config-input text-center"
              />
            </div>
            <div class="flex flex-wrap items-center justify-between gap-4 p-4 rounded-xl bg-surface-secondary/50">
              <div>
                <p class="font-medium text-body-primary">分片上传</p>
                <p class="config-hint">适合大于 50 MB 的短视频，降低失败率</p>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.videoChunkUploadEnabled ? 'toggle-on' : 'toggle-off'"
                @click="form.videoChunkUploadEnabled = !form.videoChunkUploadEnabled"
              >
                <span class="toggle-dot" :class="form.videoChunkUploadEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>
            <div v-if="form.videoChunkUploadEnabled" class="flex items-center gap-3 mt-4 p-3 rounded-lg bg-brand-sky/10">
              <label class="text-sm text-body-soft">单片大小</label>
              <input
                v-model.number="form.videoChunkSizeMb"
                type="number"
                min="1"
                max="512"
                class="w-20 config-input text-center"
              />
              <span class="text-sm text-body-soft">MB</span>
            </div>
          </div>
        </div>

        <!-- 用户限制卡片 -->
        <div class="grid gap-4 md:grid-cols-3">
          <div class="config-card">
            <label class="config-label">每日上传次数 / 用户</label>
            <input
              v-model.number="form.dailyUploadCountLimit"
              type="number"
              min="1"
              max="100000"
              class="config-input"
            />
            <p class="config-hint">超出后可在用户中心提示或转人工审核。</p>
          </div>
          <div class="config-card">
            <label class="config-label">单次上传文件数</label>
            <input
              v-model.number="form.maxFilesPerUpload"
              type="number"
              min="1"
              max="100"
              class="config-input"
            />
            <p class="config-hint">单次请求最多允许上传的文件数量，默认 30 个。</p>
          </div>
          <div class="config-card">
            <label class="config-label">单用户空间配额</label>
            <div class="flex items-center gap-3">
              <input
                v-model.number="form.userStorageQuotaGb"
                type="number"
                min="1"
                max="2048"
                class="flex-1 config-input"
              />
              <span class="text-sm font-medium text-body-soft">GB</span>
            </div>
            <p class="config-hint">当前配额：{{ storageBadge }}，达到上限将暂停新上传。</p>
          </div>
        </div>

        <!-- 功能开关卡片 -->
        <div class="config-card">
          <p class="text-xs font-medium text-brand-primary/70 uppercase tracking-wider mb-4">Feature Toggles</p>
          
          <div class="space-y-4">
            <!-- 开放注册 -->
            <div class="flex items-center justify-between p-4 rounded-xl bg-surface-secondary/50">
              <div class="flex items-center gap-3">
                <span class="text-2xl">👤</span>
                <div>
                  <p class="font-medium text-body-primary">开放注册</p>
                  <p class="config-hint">关闭后仅管理员可创建账号，保障内网私有部署安全。</p>
                </div>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.registrationEnabled ? 'toggle-on-emerald' : 'toggle-off'"
                @click="form.registrationEnabled = !form.registrationEnabled"
              >
                <span class="toggle-dot" :class="form.registrationEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>

            <!-- 访客点赞 -->
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-primary/5 border border-brand-primary/20">
              <div class="flex items-center gap-3">
                <span class="text-2xl">❤️</span>
                <div>
                  <p class="font-medium text-brand-primary">访客点赞</p>
                  <p class="config-hint">允许未登录访客为图库内容点赞，昵称将以"访客"展示。</p>
                </div>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.guestLikeEnabled ? 'toggle-on' : 'toggle-off'"
                @click="form.guestLikeEnabled = !form.guestLikeEnabled"
              >
                <span class="toggle-dot" :class="form.guestLikeEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>

            <!-- 访客上传 -->
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-primary/5 border border-brand-primary/20">
              <div class="flex items-center gap-3">
                <span class="text-2xl">📤</span>
                <div>
                  <p class="font-medium text-body-primary">访客上传</p>
                  <p class="config-hint">允许未登录访客上传图片（无需注册登录）</p>
                </div>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.guestUploadEnabled ? 'toggle-on-emerald' : 'toggle-off'"
                @click="form.guestUploadEnabled = !form.guestUploadEnabled"
              >
                <span class="toggle-dot" :class="form.guestUploadEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>

            <!-- 自动清理 -->
            <div class="p-4 rounded-xl bg-surface-secondary/50">
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-3">
                  <span class="text-2xl">🧹</span>
                  <div>
                    <p class="font-medium text-body-primary">自动清理策略</p>
                    <p class="config-hint">单张图片连续 N 天无访问记录后自动下线，释放存储与 CDN 资源。</p>
                  </div>
                </div>
                <span class="text-xs px-2 py-1 rounded-full bg-brand-sky/20 text-brand-accent">UTC+8 深夜执行</span>
              </div>
              <div class="flex items-center gap-3">
                <input
                  v-model.number="form.autoCleanupDays"
                  type="number"
                  min="0"
                  max="365"
                  class="w-24 config-input text-center"
                />
                <span class="text-sm text-body-soft">天</span>
              </div>
              <p class="config-hint mt-2">填写 0 可关闭自动清理。建议 30-180 天，让长尾文件自动回收。</p>
            </div>
          </div>
        </div>

        <!-- AI 审核卡片 -->
        <div class="config-card border-brand-sky/30">
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center gap-3">
              <span class="text-3xl">·</span>
              <div>
                <p class="text-xs font-medium text-brand-accent uppercase tracking-wider">AI Moderation</p>
                <h2 class="text-xl font-bold text-body-primary">腾讯云违规识别 & 标签</h2>
                <p class="text-xs text-body-soft mt-1">
                  需保证公开访问域名可被腾讯云访问，命中阈值后会删除原文件并返回 2048x2048 违规占位图。
                </p>
              </div>
            </div>
            <span class="text-[10px] uppercase tracking-[0.4em] text-brand-accent/80 px-2 py-1 rounded-full bg-brand-sky/10">beta</span>
          </div>

          <!-- AI 功能开关 -->
          <div class="grid gap-4 md:grid-cols-2 mb-6">
            <div class="flex items-center justify-between p-4 rounded-xl bg-surface-secondary/50">
              <div class="flex items-center gap-3">
                <span class="text-xl">🛡️</span>
                <div>
                  <p class="font-medium text-body-primary">违规审核</p>
                  <p class="config-hint">拦截色情/涉政/涉暴，保留审核记录。</p>
                </div>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.aiModerationEnabled ? 'toggle-on-sky' : 'toggle-off'"
                @click="form.aiModerationEnabled = !form.aiModerationEnabled"
              >
                <span class="toggle-dot" :class="form.aiModerationEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-surface-secondary/50">
              <div class="flex items-center gap-3">
                <span class="text-xl">🏷️</span>
                <div>
                  <p class="font-medium text-body-primary">自动标签</p>
                  <p class="config-hint">与用户自定义标签一并入库，增强搜索。</p>
                </div>
              </div>
              <button
                type="button"
                class="toggle-btn"
                :class="form.aiLabelingEnabled ? 'toggle-on-sky' : 'toggle-off'"
                @click="form.aiLabelingEnabled = !form.aiLabelingEnabled"
              >
                <span class="toggle-dot" :class="form.aiLabelingEnabled ? 'translate-x-8' : 'translate-x-0'"></span>
              </button>
            </div>
          </div>

          <!-- 腾讯云配置 -->
          <div class="grid gap-4 md:grid-cols-2 mb-6">
            <div class="space-y-2">
              <label class="config-label">SecretId</label>
              <input
                v-model="form.aiTencentSecretId"
                type="text"
                autocomplete="off"
                class="config-input"
                placeholder="AKIDxxxxxxxx"
              />
            </div>
            <div class="space-y-2">
              <label class="config-label">SecretKey</label>
              <input
                v-model="form.aiTencentSecretKey"
                type="password"
                autocomplete="new-password"
                class="config-input"
                placeholder="1A2Z3Yxxxxx"
              />
            </div>
            <div class="space-y-2">
              <label class="config-label">Region (例如 ap-beijing)</label>
              <input
                v-model="form.aiTencentRegion"
                type="text"
                class="config-input"
                placeholder="ap-beijing"
              />
            </div>
            <div class="space-y-2">
              <label class="config-label">Bucket 名称 (bucket-appid)</label>
              <input
                v-model="form.aiTencentBucket"
                type="text"
                class="config-input"
                placeholder="demo-1328487995"
              />
            </div>
            <div class="space-y-2">
              <label class="config-label">识别场景 (逗号分隔)</label>
              <input
                v-model="form.aiTencentDetectScenes"
                type="text"
                class="config-input"
                placeholder="web,camera,album,news"
              />
              <p class="config-hint">官方建议至少启用 web + camera，以提高覆盖率。</p>
            </div>
            <div class="space-y-2">
              <label class="config-label">可用性提示</label>
              <div class="p-3 rounded-xl bg-brand-mint/10 text-sm text-body-soft">
                需确保该 Bucket 允许外网访问；若使用本地存储，请配置公网反向代理后在此填写其域名。
              </div>
            </div>
          </div>

          <!-- 阈值滑块 -->
          <div class="grid gap-4 md:grid-cols-3">
            <div class="p-4 rounded-xl bg-surface-secondary/50 space-y-3">
              <div class="flex items-center justify-between text-sm">
                <span class="text-body-soft">违规拦截阈值</span>
                <span class="font-bold text-rose-500">{{ form.aiModerationBlockConfidence }}%</span>
              </div>
              <input v-model.number="form.aiModerationBlockConfidence" type="range" min="0" max="100" class="config-range" />
            </div>
            <div class="p-4 rounded-xl bg-surface-secondary/50 space-y-3">
              <div class="flex items-center justify-between text-sm">
                <span class="text-body-soft">疑似复核阈值</span>
                <span class="font-bold text-amber-500">{{ form.aiModerationReviewConfidence }}%</span>
              </div>
              <input v-model.number="form.aiModerationReviewConfidence" type="range" min="0" max="100" class="config-range" />
            </div>
            <div class="p-4 rounded-xl bg-surface-secondary/50 space-y-3">
              <div class="flex items-center justify-between text-sm">
                <span class="text-body-soft">标签置信度</span>
                <span class="font-bold text-brand-accent">{{ form.aiLabelMinConfidence }}%</span>
              </div>
              <input v-model.number="form.aiLabelMinConfidence" type="range" min="0" max="100" class="config-range" />
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <button
          type="submit"
          class="btn-primary w-full py-4 text-lg"
          :disabled="saving"
        >
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </form>

      <!-- 右侧数据概览 -->
      <aside class="space-y-6">
        <!-- 数据盘点卡片 -->
        <div class="config-card sticky top-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <p class="text-xs font-medium text-brand-primary/70 uppercase tracking-wider">Overview</p>
              <h2 class="text-xl font-bold text-body-primary">数据盘点</h2>
            </div>
            <button class="text-sm text-brand-primary hover:text-brand-primary/80 transition" @click="loadAll">
              刷新
            </button>
          </div>
          <div class="grid gap-3">
            <div
              v-for="card in statCards"
              :key="card.label"
              class="stat-card"
              :class="`stat-${card.color}`"
            >
              <p class="text-xs text-body-soft">{{ card.label }}</p>
              <p class="text-2xl font-bold">{{ card.value }}</p>
              <p class="text-xs text-body-soft">{{ card.hint }}</p>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 自定义页脚 -->
    <div class="config-card">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <span class="text-2xl">·</span>
          <div>
            <p class="text-xs font-medium text-brand-primary/70 uppercase tracking-wider">Custom Footer</p>
            <h2 class="text-xl font-bold text-body-primary">公共页脚 HTML</h2>
          </div>
        </div>
        <span class="text-xs px-3 py-1.5 rounded-full bg-surface-secondary text-body-soft">Landing · 登录 · 注册 · 会员页</span>
      </div>
      <textarea
        v-model="form.customFooterHtml"
        rows="6"
        class="config-textarea"
        placeholder="<p>备案号 · 联系方式等 HTML 片段</p>"
      ></textarea>
      <el-alert
        v-if="footerDiagnostics.warning"
        type="warning"
        show-icon
        :closable="false"
        :title="footerDiagnostics.warning"
        class="mt-4"
      />
      <el-alert
        v-if="footerDiagnostics.error"
        type="error"
        show-icon
        :closable="false"
        :title="footerDiagnostics.error"
        class="mt-4"
      />
      <p class="config-hint mt-4">
        该内容会直接插入在页脚的默认信息上方，可用于备案号、隐私声明等自定义 HTML，请确保代码安全。
      </p>
      <div v-if="footerPreview" class="mt-4 p-4 rounded-xl bg-surface-secondary/50">
        <p class="text-xs uppercase tracking-[0.5em] text-body-soft mb-3">预览</p>
        <div class="text-sm text-body-secondary" v-html="footerPreview"></div>
      </div>
      <div class="flex justify-end mt-4">
        <button type="button" class="btn-secondary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存自定义页脚' }}
        </button>
      </div>
    </div>

    <!-- 诊断指引 -->
    <div class="config-card">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <span class="text-2xl">🔧</span>
          <div>
            <p class="text-xs font-medium text-brand-primary/70 uppercase tracking-wider">Diagnostics</p>
            <h2 class="text-xl font-bold text-body-primary">常见错误定位</h2>
          </div>
        </div>
        <span class="text-xs px-3 py-1.5 rounded-full bg-brand-sky/20 text-brand-accent">401 & 413</span>
      </div>
      <div class="grid gap-4 md:grid-cols-2">
        <div
          v-for="block in diagnosticBlocks"
          :key="block.title"
          class="diagnostic-card"
        >
          <div class="flex items-start gap-3 mb-3">
            <span class="text-2xl">{{ block.icon }}</span>
            <div>
              <h3 class="text-lg font-bold text-body-primary">{{ block.title }}</h3>
              <p class="text-xs text-body-soft mt-1">{{ block.subtitle }}</p>
            </div>
          </div>
          <ul class="space-y-2 text-sm text-body-secondary">
            <li v-for="tip in block.tips" :key="tip" class="flex items-start gap-2">
              <span class="text-brand-primary mt-0.5">•</span>
              <span>{{ tip }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 配置卡片 */
.config-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(250, 251, 252, 0.9));
  border: 1px solid rgba(232, 232, 240, 0.8);
  border-radius: 1.5rem;
  padding: 1.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;
}

:root.dark .config-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95), rgba(15, 15, 30, 0.9));
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

/* 配置区块 */
.config-section {
  background: linear-gradient(135deg, rgba(255, 245, 247, 0.6), rgba(240, 253, 250, 0.4));
  border: 1px solid rgba(255, 182, 193, 0.2);
  border-radius: 1rem;
  padding: 1.25rem;
}

:root.dark .config-section {
  background: linear-gradient(135deg, rgba(30, 30, 50, 0.6), rgba(20, 25, 40, 0.4));
  border-color: rgba(255, 255, 255, 0.06);
}

/* 标签 */
.config-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
}

/* 输入框 */
.config-input {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(232, 232, 240, 0.8);
  border-radius: 0.75rem;
  transition: all 0.2s ease;
}

.config-input:focus {
  outline: none;
  border-color: var(--color-brand-primary);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15);
}

:root.dark .config-input {
  background: rgba(15, 15, 30, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

/* 文本域 */
.config-textarea {
  width: 100%;
  padding: 1rem;
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--color-text-primary);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(232, 232, 240, 0.8);
  border-radius: 1rem;
  resize: vertical;
  transition: all 0.2s ease;
}

.config-textarea:focus {
  outline: none;
  border-color: var(--color-brand-primary);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15);
}

:root.dark .config-textarea {
  background: rgba(15, 15, 30, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

/* 提示文字 */
.config-hint {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
  line-height: 1.5;
}

/* 滑块 */
.config-range {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: linear-gradient(90deg, rgba(255, 182, 193, 0.3), rgba(168, 230, 207, 0.3));
  border-radius: 3px;
  outline: none;
}

.config-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #ff6b9d, #ff8fab);
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.4);
  transition: transform 0.2s ease;
}

.config-range::-webkit-slider-thumb:hover {
  transform: scale(1.1);
}

/* 切换按钮 */
.toggle-btn {
  position: relative;
  width: 3.5rem;
  height: 2rem;
  border-radius: 9999px;
  transition: all 0.3s ease;
}

.toggle-on {
  background: linear-gradient(135deg, #ff6b9d, #ff8fab);
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.4);
}

.toggle-on-emerald {
  background: linear-gradient(135deg, #2ecc71, #5eead4);
  box-shadow: 0 4px 15px rgba(46, 204, 113, 0.4);
}

.toggle-on-sky {
  background: linear-gradient(135deg, #87ceeb, #7dd3fc);
  box-shadow: 0 4px 15px rgba(135, 206, 235, 0.4);
}

.toggle-off {
  background: rgba(200, 200, 220, 0.5);
}

:root.dark .toggle-off {
  background: rgba(255, 255, 255, 0.15);
}

.toggle-dot {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
  width: 1.5rem;
  height: 1.5rem;
  background: white;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 统计卡片 */
.stat-card {
  padding: 1rem 1.25rem;
  border-radius: 1rem;
  border: 1px solid rgba(232, 232, 240, 0.6);
  background: white;
  transition: all 0.2s ease;
}

:root.dark .stat-card {
  background: rgba(30, 30, 50, 0.6);
  border-color: rgba(255, 255, 255, 0.06);
}

.stat-pink {
  border-left: 4px solid #ff6b9d;
}

.stat-mint {
  border-left: 4px solid #4ecdc4;
}

.stat-sky {
  border-left: 4px solid #87ceeb;
}

.stat-coral {
  border-left: 4px solid #ff8a80;
}

/* 诊断卡片 */
.diagnostic-card {
  background: linear-gradient(135deg, rgba(255, 245, 247, 0.5), rgba(240, 253, 250, 0.3));
  border: 1px solid rgba(255, 182, 193, 0.2);
  border-radius: 1rem;
  padding: 1.25rem;
}

:root.dark .diagnostic-card {
  background: linear-gradient(135deg, rgba(30, 30, 50, 0.5), rgba(20, 25, 40, 0.3));
  border-color: rgba(255, 255, 255, 0.06);
}

/* 按钮 */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1.75rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 9999px;
  box-shadow: 0 10px 35px rgba(255, 107, 157, 0.35), 
              0 4px 15px rgba(255, 107, 157, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 45px rgba(255, 107, 157, 0.45), 
              0 6px 20px rgba(255, 107, 157, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, #ff7aa8 0%, #ff9eb8 100%);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #4ecdc4 0%, #6ee7d8 100%);
  border: none;
  border-radius: 9999px;
  box-shadow: 0 8px 25px rgba(78, 205, 196, 0.3), 
              0 3px 10px rgba(78, 205, 196, 0.15);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.btn-secondary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(78, 205, 196, 0.4), 
              0 4px 15px rgba(78, 205, 196, 0.25);
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
