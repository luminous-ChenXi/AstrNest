<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useStorageStrategyStore } from '@/stores/storageStrategies'

const storageStrategyStore = useStorageStrategyStore()

const isFormVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formErrors = reactive({})
const form = reactive({
  name: '',
  displayName: '',
  description: '',
  strategy: 'LOCAL',
  config: {},
  activateNow: true,
})

const strategyLabels = {
  LOCAL: '本地存储',
  ALIYUN_OSS: '阿里云 OSS',
  TENCENT_COS: '腾讯云 COS',
  QINIU_KODO: '七牛云 KODO',
  UPYUN_USS: '又拍云 USS',
  HUAWEI_OBS: '华为云 OBS',
  KS3: '金山云 KS3',
  S3_COMPATIBLE: 'S3 兼容',
  ONEDRIVE: 'OneDrive / SharePoint',
}

const strategyOptions = Object.entries(strategyLabels).map(([value, label]) => ({ value, label }))

const aclOptions = [
  { label: '私有读写 (private)', value: 'private' },
  { label: '公共读 (public-read)', value: 'public-read' },
  { label: '公共读写 (public-read-write)', value: 'public-read-write' },
]

const buildS3Defaults = (region) => ({
  bucket: '',
  endpoint: '',
  region,
  accessKey: '',
  secretKey: '',
  acl: 'private',
  cdnHost: '',
  pathStyle: true,
  accelerate: false,
  multipartThresholdMb: 5120,
  partSizeMb: 25,
})

const strategyDefaults = {
  LOCAL: { root: '../storage/upload', publicBaseUrl: '/upload' },
  ALIYUN_OSS: {
    bucket: '',
    endpoint: '',
    region: 'cn-hangzhou',
    accessKey: '',
    secretKey: '',
    acl: 'private',
    cdnHost: '',
    internalEndpoint: false,
    enableCname: false,
    corsOrigin: '*',
    corsMethods: 'GET,POST,PUT,DELETE,HEAD',
    corsAllowedHeaders: '*',
    corsExposeHeaders: '',
    corsMaxAgeSeconds: '3600',
  },
  TENCENT_COS: buildS3Defaults('ap-shanghai'),
  QINIU_KODO: buildS3Defaults('z0'),
  HUAWEI_OBS: buildS3Defaults('cn-north-4'),
  KS3: buildS3Defaults('cn-beijing-1'),
  S3_COMPATIBLE: buildS3Defaults('us-east-1'),
  UPYUN_USS: {
    bucket: '',
    operator: '',
    password: '',
    endpoint: 'https://v0.api.upyun.com',
    cdnHost: '',
  },
  ONEDRIVE: {
    driveType: 'business',
    tenantId: '',
    clientId: '',
    clientSecret: '',
    driveId: '',
    siteId: '',
    refreshToken: '',
    redirectUri: 'https://login.microsoftonline.com/common/oauth2/nativeclient',
    baseUrl: 'https://graph.microsoft.com/v1.0',
  },
}

const createS3Fields = (regionHint) => [
  { group: '基础配置', key: 'bucket', label: 'Bucket 名称', required: true, placeholder: 'astrnest-media' },
  { group: '基础配置', key: 'region', label: '地区代码', required: true, placeholder: regionHint },
  { group: '基础配置', key: 'endpoint', label: 'EndPoint', required: true, placeholder: 'https://cos.ap-shanghai.myqcloud.com' },
  { group: '认证信息', key: 'accessKey', label: 'AccessKey ID', required: true },
  { group: '认证信息', key: 'secretKey', label: 'AccessKey Secret', required: true, type: 'password' },
  { group: '高级设置', key: 'acl', label: 'Bucket 读写权限', required: true, type: 'select', options: aclOptions },
  { group: '高级设置', key: 'cdnHost', label: 'CDN 域名', placeholder: 'https://cdn.luminouschenxi.net' },
  { group: '高级设置', key: 'pathStyle', label: 'PathStyle 访问', type: 'checkbox' },
  { group: '高级设置', key: 'accelerate', label: '启用加速域', type: 'checkbox' },
  { group: '高级设置', key: 'multipartThresholdMb', label: '分片阈值 (MB)', type: 'number' },
  { group: '高级设置', key: 'partSizeMb', label: '分片大小 (MB)', type: 'number' },
]

const fieldDefinitions = {
  LOCAL: [
    { group: '基础配置', key: 'root', label: '根目录', required: true, placeholder: '../storage/upload' },
    { group: '基础配置', key: 'publicBaseUrl', label: '公开访问路径', required: true, placeholder: '/upload' },
  ],
  ALIYUN_OSS: [
    { group: '基础配置', key: 'bucket', label: 'Bucket 名称', required: true, placeholder: 'astrnest-prod' },
    { group: '基础配置', key: 'endpoint', label: 'EndPoint', required: true, placeholder: 'https://oss-cn-hangzhou.aliyuncs.com' },
    { group: '基础配置', key: 'region', label: '地区代码', required: true, placeholder: 'cn-hangzhou' },
    { group: '认证信息', key: 'accessKey', label: 'AccessKey ID', required: true },
    { group: '认证信息', key: 'secretKey', label: 'AccessKey Secret', required: true, type: 'password' },
    { group: '高级设置', key: 'acl', label: 'Bucket 读写权限', required: true, type: 'select', options: aclOptions },
    { group: '高级设置', key: 'cdnHost', label: 'CDN 域名', placeholder: 'https://cdn.luminouschenxi.net' },
    { group: '高级设置', key: 'internalEndpoint', label: '使用内网 Endpoint', type: 'checkbox' },
    { group: '高级设置', key: 'enableCname', label: '启用自定义域名 (CNAME)', type: 'checkbox' },
    { group: '跨域策略', key: 'corsOrigin', label: '来源 (Origin)', required: true, placeholder: '*' },
    { group: '跨域策略', key: 'corsMethods', label: '允许 Methods', required: true, placeholder: 'GET,POST,PUT,DELETE,HEAD' },
    { group: '跨域策略', key: 'corsAllowedHeaders', label: '允许 Headers', required: true, placeholder: '*' },
    { group: '跨域策略', key: 'corsExposeHeaders', label: '暴露 Headers', placeholder: 'x-astrnest-meta' },
    { group: '跨域策略', key: 'corsMaxAgeSeconds', label: '缓存时间 (秒)', required: true, type: 'number', placeholder: '3600' },
  ],
  TENCENT_COS: createS3Fields('ap-shanghai'),
  QINIU_KODO: createS3Fields('z0'),
  HUAWEI_OBS: createS3Fields('cn-north-4'),
  KS3: createS3Fields('cn-beijing-1'),
  S3_COMPATIBLE: createS3Fields('us-east-1'),
  UPYUN_USS: [
    { group: '基础配置', key: 'bucket', label: 'Bucket 名称', required: true },
    { group: '基础配置', key: 'endpoint', label: 'EndPoint', required: true, placeholder: 'https://v0.api.upyun.com' },
    { group: '认证信息', key: 'operator', label: '操作员', required: true },
    { group: '认证信息', key: 'password', label: '操作员密码', required: true, type: 'password' },
    { group: '高级设置', key: 'cdnHost', label: 'CDN 域名', placeholder: 'https://static.luminouschenxi.net' },
  ],
  ONEDRIVE: [
    { group: '基础配置', key: 'driveType', label: 'Drive 类型', required: true, type: 'select', options: [
      { label: '企业 (business)', value: 'business' },
      { label: '个人 (personal)', value: 'personal' },
    ] },
    { group: '基础配置', key: 'driveId', label: 'Drive ID', placeholder: 'b!x1Z...' },
    { group: '基础配置', key: 'siteId', label: 'SharePoint Site ID', placeholder: 'astrnest.sharepoint.com,1234' },
    { group: '认证信息', key: 'tenantId', label: 'Tenant ID', required: true },
    { group: '认证信息', key: 'clientId', label: 'Client ID', required: true },
    { group: '认证信息', key: 'clientSecret', label: 'Client Secret', required: true, type: 'password' },
    { group: '认证信息', key: 'refreshToken', label: 'Refresh Token', placeholder: '个人盘需填写' },
    { group: '高级设置', key: 'redirectUri', label: 'Redirect URI', required: true },
    { group: '高级设置', key: 'baseUrl', label: 'Graph API Base URL', required: true },
  ],
}

const sortedStrategies = computed(() => {
  const list = [...storageStrategyStore.strategies]
  return list.sort((a, b) => {
    if (a.active === b.active) {
      return new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0)
    }
    return a.active ? -1 : 1
  })
})

const currentFieldGroups = computed(() => {
  const fields = fieldDefinitions[form.strategy] || []
  const grouped = {}
  fields.forEach((field) => {
    if (!field.label && field.key === 'displayName') {
      return
    }
    const key = field.group || '基础配置'
    if (!grouped[key]) {
      grouped[key] = []
    }
    grouped[key].push(field)
  })
  return Object.entries(grouped).map(([title, items]) => ({ title, fields: items }))
})

const isEditing = computed(() => Boolean(editingId.value))
const formTitle = computed(() => (isEditing.value ? '编辑存储策略' : '添加存储策略'))

const clearErrors = () => {
  Object.keys(formErrors).forEach((key) => delete formErrors[key])
}

const assignConfig = (strategy, overrides = {}) => {
  const defaults = strategyDefaults[strategy] || {}
  form.config = { ...defaults, ...overrides }
}

const resetForm = (strategy = 'LOCAL') => {
  editingId.value = null
  form.name = ''
  form.displayName = ''
  form.description = ''
  form.strategy = strategy
  assignConfig(strategy)
  form.activateNow = true
  clearErrors()
}

const openCreateDrawer = () => {
  resetForm('LOCAL')
  isFormVisible.value = true
}

const openEditDrawer = (profile) => {
  if (!profile) {
    return
  }
  editingId.value = profile.id
  form.name = profile.name
  form.displayName = profile.displayName
  form.description = profile.description || ''
  form.strategy = profile.strategy
  assignConfig(profile.strategy, profile.config || {})
  form.activateNow = profile.active
  clearErrors()
  isFormVisible.value = true
}

const validateForm = () => {
  clearErrors()
  if (!isEditing.value && !form.name.trim()) {
    formErrors.name = '请填写策略名称'
  }
  if (!form.displayName.trim()) {
    formErrors.displayName = '请填写展示名'
  }
  const fields = fieldDefinitions[form.strategy] || []
  fields.forEach((field) => {
    if (!field.required) {
      return
    }
    const value = form.config[field.key]
    const emptyString = typeof value === 'string' && !value.trim()
    if (value === undefined || value === null || emptyString) {
      formErrors[field.key] = `${field.label}为必填项`
    }
  })
  return Object.keys(formErrors).length === 0
}

const normalizeConfig = () => {
  const normalized = {}
  Object.entries(form.config || {}).forEach(([key, value]) => {
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (!trimmed) {
        return
      }
      normalized[key] = trimmed
      return
    }
    if (value === undefined || value === null) {
      return
    }
    normalized[key] = value
  })
  return normalized
}

const submitForm = () => {
  if (!validateForm()) {
    return
  }
  submitting.value = true
  const payload = {
    displayName: form.displayName.trim(),
    description: form.description.trim(),
    strategy: form.strategy,
    config: normalizeConfig(),
    active: form.activateNow,
  }
  const action = isEditing.value
    ? storageStrategyStore.updateStrategy(editingId.value, payload)
    : storageStrategyStore.createStrategy({ ...payload, name: form.name.trim() })
  action
    .then(() => storageStrategyStore.fetchStrategies())
    .then(() => {
      ElMessage.success(isEditing.value ? '策略已更新' : '策略已创建')
      isFormVisible.value = false
    })
    .catch((error) => {
      const message = error?.response?.data?.message || '保存失败，请稍后再试'
      ElMessage.error(message)
    })
    .finally(() => {
      submitting.value = false
    })
}

const handleActivate = (profile) => {
  if (!profile || profile.active) {
    return
  }
  if (!window.confirm(`确定将 “${profile.displayName}” 设为默认存储策略吗？`)) {
    return
  }
  storageStrategyStore.activateStrategy(profile.id)
    .then(() => {
      ElMessage.success('已切换至该存储策略')
    })
    .catch((error) => {
      const message = error?.response?.data?.message || '切换失败，请稍后重试'
      ElMessage.error(message)
    })
}

const handleDelete = (profile) => {
  if (!profile || profile.active) {
    ElMessage.warning('请先切换到其他策略后再删除')
    return
  }
  if (!window.confirm(`删除后将无法恢复 “${profile.displayName}”，确定继续？`)) {
    return
  }
  storageStrategyStore.removeStrategy(profile.id)
    .then(() => {
      ElMessage.success('策略已删除')
    })
    .catch((error) => {
      const message = error?.response?.data?.message || '删除失败，请稍后重试'
      ElMessage.error(message)
    })
}

const handleRefresh = () => {
  storageStrategyStore.fetchStrategies()
}

const typeLabel = (value) => strategyLabels[value] || value

const formatUpdatedAt = (value) => {
  if (!value) {
    return '尚未更新'
  }
  return new Date(value).toLocaleString()
}

const closeDrawer = () => {
  isFormVisible.value = false
}

onMounted(() => {
  storageStrategyStore.fetchStrategies()
})
</script>

<template>
  <section class="space-y-5">
    <header class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <p class="text-sm uppercase tracking-[0.4em] text-white/60">storage</p>
        <h2 class="text-3xl font-semibold text-white">存储策略</h2>
        <p class="text-white/70 mt-1 max-w-2xl">
          参考 Cloudreve 的多云策略管理，集中创建或切换 OSS/COS/OneDrive 等驱动，所有配置会持久化到数据库并即时生效。
        </p>
      </div>
      <div class="flex gap-3">
        <button
          type="button"
          class="rounded-2xl border border-white/20 px-4 py-2 text-sm text-white/80 hover:border-white/40"
          @click="handleRefresh"
        >
          刷新
        </button>
        <button
          type="button"
          class="rounded-2xl bg-brand-primary px-4 py-2 text-sm font-semibold text-black"
          @click="openCreateDrawer"
        >
          添加存储策略
        </button>
      </div>
    </header>

    <div v-if="storageStrategyStore.errorMessage" class="glass-panel border border-rose-500/40 p-4 text-sm text-rose-100">
      {{ storageStrategyStore.errorMessage }}
    </div>

    <div v-if="storageStrategyStore.loading" class="glass-panel border border-white/10 p-6 text-white/70 animate-pulse">
      正在加载存储策略...
    </div>

    <div
      v-else-if="sortedStrategies.length === 0"
      class="glass-panel border border-white/10 p-6 text-white/70"
    >
      暂无存储策略，点击右上角“添加存储策略”开始配置本地或第三方对象存储。
    </div>

    <div v-else class="grid gap-5 lg:grid-cols-2">
      <article
        v-for="profile in sortedStrategies"
        :key="profile.id"
        class="glass-panel border border-white/10 p-5 flex flex-col gap-4"
      >
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.4em] text-white/60">{{ typeLabel(profile.strategy) }}</p>
            <h3 class="text-2xl font-semibold text-white">{{ profile.displayName }}</h3>
          </div>
          <span
            class="rounded-full px-3 py-1 text-xs font-semibold"
            :class="profile.active ? 'bg-emerald-400/20 text-emerald-200' : 'bg-white/10 text-white/60'"
          >
            {{ profile.active ? '已启用' : '备用' }}
          </span>
        </div>
        <dl class="grid gap-2 text-sm text-white/70">
          <div class="flex justify-between gap-3">
            <dt class="text-white/50">Bucket / Drive</dt>
            <dd class="text-right">{{ profile.config?.bucket || profile.config?.driveId || '—' }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-white/50">EndPoint / Base URL</dt>
            <dd class="text-right">{{ profile.config?.endpoint || profile.config?.baseUrl || '—' }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-white/50">地区代码</dt>
            <dd class="text-right">{{ profile.config?.region || profile.config?.siteId || '—' }}</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-white/50">最近更新</dt>
            <dd class="text-right">{{ formatUpdatedAt(profile.updatedAt) }} · {{ profile.updatedBy || 'system' }}</dd>
          </div>
        </dl>
        <div class="flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-xl border border-white/20 px-4 py-2 text-sm text-white/80 hover:border-white/40"
            @click="openEditDrawer(profile)"
          >
            编辑配置
          </button>
          <button
            v-if="!profile.active"
            type="button"
            class="rounded-xl border border-emerald-400/40 px-4 py-2 text-sm text-emerald-200 hover:border-emerald-300/60"
            @click="handleActivate(profile)"
          >
            设为默认
          </button>
          <button
            v-if="!profile.active"
            type="button"
            class="rounded-xl border border-rose-400/40 px-4 py-2 text-sm text-rose-200 hover:border-rose-300/60"
            @click="handleDelete(profile)"
          >
            删除
          </button>
        </div>
      </article>
    </div>

    <div
      v-if="isFormVisible"
      class="fixed inset-0 z-50 flex justify-end"
    >
      <div class="absolute inset-0 bg-black/60" @click="closeDrawer"></div>
      <div class="relative z-10 h-full w-full max-w-3xl bg-[#0d101c] p-6 overflow-y-auto">
        <div class="flex items-center justify-between border-b border-white/10 pb-4">
          <div>
            <p class="text-sm text-white/60">{{ isEditing ? '更新已有策略' : '新增第三方策略' }}</p>
            <h3 class="text-2xl font-semibold text-white">{{ formTitle }}</h3>
          </div>
          <button type="button" class="text-white/60 hover:text-white" @click="closeDrawer">✕</button>
        </div>

        <div class="grid gap-4 py-6 md:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm text-white/70">策略名称 (唯一标识)</span>
            <input
              v-model="form.name"
              :disabled="isEditing"
              type="text"
              class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white disabled:opacity-60"
              placeholder="oss-primary"
            />
            <p v-if="!isEditing" class="text-xs text-white/50">创建后不可修改，仅用于系统区分。</p>
            <p v-else class="text-xs text-white/50">策略名称不可变更。</p>
            <p v-if="formErrors.name" class="text-xs text-rose-400">{{ formErrors.name }}</p>
          </label>
          <label class="space-y-2">
            <span class="text-sm text-white/70">展示名</span>
            <input
              v-model="form.displayName"
              type="text"
              class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
              placeholder="上海 OSS 主集群"
            />
            <p class="text-xs text-white/50">会展示给管理员与成员，建议带上地域信息。</p>
            <p v-if="formErrors.displayName" class="text-xs text-rose-400">{{ formErrors.displayName }}</p>
          </label>
          <label class="space-y-2 md:col-span-2">
            <span class="text-sm text-white/70">策略描述</span>
            <textarea
              v-model="form.description"
              rows="2"
              class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
              placeholder="描述用途、接入团队或路由策略，方便日后回溯"
            ></textarea>
          </label>
          <label class="space-y-2">
            <span class="text-sm text-white/70">存储类型</span>
            <select
              v-model="form.strategy"
              :disabled="isEditing"
              class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
            >
              <option v-for="option in strategyOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <p class="text-xs text-white/50">若需其它驱动，可在后端扩展 StorageStrategy 枚举。</p>
          </label>
          <label class="flex items-center gap-3 text-white/80">
            <input type="checkbox" v-model="form.activateNow" class="h-5 w-5 rounded border-white/40 bg-black/30" />
            <span class="text-sm">保存后立即启用此策略</span>
          </label>
        </div>

        <div class="space-y-5">
          <div
            v-for="group in currentFieldGroups"
            :key="group.title"
            class="rounded-2xl border border-white/10 bg-black/20 p-4"
          >
            <h4 class="text-sm font-semibold text-white">{{ group.title }}</h4>
            <div class="mt-4 grid gap-4 md:grid-cols-2">
              <div v-for="field in group.fields" :key="field.key" class="space-y-2">
                <label class="text-sm text-white/70">{{ field.label }}</label>
                <template v-if="field.type === 'select'">
                  <select
                    v-model="form.config[field.key]"
                    class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
                  >
                    <option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </template>
                <template v-else-if="field.type === 'checkbox'">
                  <label class="flex items-center gap-3 text-white/80">
                    <input type="checkbox" v-model="form.config[field.key]" class="h-5 w-5 rounded border-white/40 bg-black/30" />
                    <span class="text-sm">启用</span>
                  </label>
                </template>
                <template v-else-if="field.type === 'textarea'">
                  <textarea
                    v-model="form.config[field.key]"
                    rows="2"
                    class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
                    :placeholder="field.placeholder"
                  ></textarea>
                </template>
                <template v-else>
                  <input
                    v-model="form.config[field.key]"
                    :type="field.type === 'password' ? 'password' : field.type === 'number' ? 'number' : 'text'"
                    class="w-full rounded-xl border border-white/15 bg-black/30 px-3 py-2 text-white"
                    :placeholder="field.placeholder"
                  />
                </template>
                <p v-if="field.description" class="text-xs text-white/50">{{ field.description }}</p>
                <p v-if="formErrors[field.key]" class="text-xs text-rose-400">{{ formErrors[field.key] }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3 border-t border-white/10 pt-4">
          <button
            type="button"
            class="rounded-xl border border-white/20 px-5 py-2 text-sm text-white/70 hover:border-white/40"
            @click="closeDrawer"
          >
            取消
          </button>
          <button
            type="button"
            class="rounded-xl bg-brand-primary px-6 py-2 text-sm font-semibold text-black disabled:opacity-60"
            :disabled="submitting"
            @click="submitForm"
          >
            {{ submitting ? '保存中...' : '保存策略' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
