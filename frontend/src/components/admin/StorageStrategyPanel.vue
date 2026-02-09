<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useStorageStrategyStore } from '@/stores/storageStrategies'
import { Plus, RefreshCw, Edit3, Trash2, CheckCircle2, Database, Server, Cloud, HardDrive, ExternalLink, X, AlertCircle } from 'lucide-vue-next'

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

const strategyIcons = {
  LOCAL: HardDrive,
  ALIYUN_OSS: Cloud,
  TENCENT_COS: Cloud,
  QINIU_KODO: Cloud,
  UPYUN_USS: Cloud,
  HUAWEI_OBS: Cloud,
  KS3: Cloud,
  S3_COMPATIBLE: Database,
  ONEDRIVE: Server,
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
  if (!window.confirm(`确定将 "${profile.displayName}" 设为默认存储策略吗？`)) {
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
  if (!window.confirm(`删除后将无法恢复 "${profile.displayName}"，确定继续？`)) {
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
  <div class="storage-strategy-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <p class="subtitle">存储策略</p>
        <h1 class="title">存储策略中心</h1>
        <p class="description">采取多云策略管理，集中创建或切换 OSS/COS/OneDrive 等驱动，所有配置会持久化到数据库并即时生效。</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="handleRefresh">
          <RefreshCw class="icon" />
          刷新
        </button>
        <button class="btn-create" @click="openCreateDrawer">
          <Plus class="icon" />
          添加存储策略
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="storageStrategyStore.errorMessage" class="error-alert">
      <AlertCircle class="error-icon" />
      {{ storageStrategyStore.errorMessage }}
    </div>

    <!-- 加载状态 -->
    <div v-if="storageStrategyStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载存储策略...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="sortedStrategies.length === 0" class="empty-state">
      <div class="empty-illustration">
        <Database class="empty-icon" />
      </div>
      <h3 class="empty-title">暂无存储策略</h3>
      <p class="empty-desc">点击右上角"添加存储策略"开始配置本地或第三方对象存储。</p>
      <button class="btn-create mt-4" @click="openCreateDrawer">
        <Plus class="icon" />
        添加存储策略
      </button>
    </div>

    <!-- 策略列表 -->
    <div v-else class="strategy-list">
      <div
        v-for="profile in sortedStrategies"
        :key="profile.id"
        class="strategy-card"
        :class="{ 'is-active': profile.active }"
      >
        <div class="card-header">
          <div class="card-type">
            <div class="type-icon" :class="profile.active ? 'active' : 'inactive'">
              <component :is="strategyIcons[profile.strategy] || Database" class="icon" />
            </div>
            <div class="type-info">
              <p class="type-label">{{ typeLabel(profile.strategy) }}</p>
              <h3 class="strategy-name">{{ profile.displayName }}</h3>
            </div>
          </div>
          <span class="status-badge" :class="profile.active ? 'active' : 'standby'">
            <CheckCircle2 v-if="profile.active" class="badge-icon" />
            {{ profile.active ? '已启用' : '备用' }}
          </span>
        </div>

        <div class="card-body">
          <p v-if="profile.description" class="strategy-desc">{{ profile.description }}</p>
          <div class="config-grid">
            <div class="config-item">
              <span class="config-label">Bucket / Drive</span>
              <span class="config-value">{{ profile.config?.bucket || profile.config?.driveId || '—' }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">EndPoint / Base URL</span>
              <span class="config-value truncate">{{ profile.config?.endpoint || profile.config?.baseUrl || '—' }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">地区代码</span>
              <span class="config-value">{{ profile.config?.region || profile.config?.siteId || '—' }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">最近更新</span>
              <span class="config-value">{{ formatUpdatedAt(profile.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="update-info">
            <span class="update-by">by {{ profile.updatedBy || 'system' }}</span>
          </div>
          <div class="card-actions">
            <button class="action-btn" @click="openEditDrawer(profile)" title="编辑配置">
              <Edit3 class="action-icon" />
              <span>编辑</span>
            </button>
            <button
              v-if="!profile.active"
              class="action-btn activate"
              @click="handleActivate(profile)"
              title="设为默认"
            >
              <CheckCircle2 class="action-icon" />
              <span>启用</span>
            </button>
            <button
              v-if="!profile.active"
              class="action-btn delete"
              @click="handleDelete(profile)"
              title="删除"
            >
              <Trash2 class="action-icon" />
              <span>删除</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 抽屉表单 -->
    <Transition name="drawer">
      <div v-if="isFormVisible" class="drawer-overlay" @click.self="closeDrawer">
        <div class="drawer-panel">
          <!-- 抽屉头部 -->
          <div class="drawer-header">
            <div class="header-content">
              <p class="drawer-subtitle">{{ isEditing ? '更新已有策略' : '新增第三方策略' }}</p>
              <h3 class="drawer-title">{{ formTitle }}</h3>
            </div>
            <button type="button" class="close-btn" @click="closeDrawer">
              <X class="close-icon" />
            </button>
          </div>

          <!-- 抽屉内容 -->
          <div class="drawer-content">
            <!-- 基本信息 -->
            <div class="form-section">
              <h4 class="section-title">基本信息</h4>
              <div class="form-grid">
                <div class="form-field">
                  <label class="field-label">
                    策略名称 (唯一标识)
                    <span v-if="isEditing" class="field-hint">不可修改</span>
                  </label>
                  <input
                    v-model="form.name"
                    :disabled="isEditing"
                    type="text"
                    class="field-input"
                    :class="{ 'has-error': formErrors.name }"
                    placeholder="oss-primary"
                  />
                  <p v-if="!isEditing" class="field-help">创建后不可修改，仅用于系统区分。</p>
                  <p v-if="formErrors.name" class="field-error">{{ formErrors.name }}</p>
                </div>

                <div class="form-field">
                  <label class="field-label">展示名</label>
                  <input
                    v-model="form.displayName"
                    type="text"
                    class="field-input"
                    :class="{ 'has-error': formErrors.displayName }"
                    placeholder="上海 OSS 主集群"
                  />
                  <p class="field-help">会展示给管理员与成员，建议带上地域信息。</p>
                  <p v-if="formErrors.displayName" class="field-error">{{ formErrors.displayName }}</p>
                </div>

                <div class="form-field full-width">
                  <label class="field-label">策略描述</label>
                  <textarea
                    v-model="form.description"
                    rows="2"
                    class="field-input textarea"
                    placeholder="描述用途、接入团队或路由策略，方便日后回溯"
                  ></textarea>
                </div>

                <div class="form-field">
                  <label class="field-label">存储类型</label>
                  <select
                    v-model="form.strategy"
                    :disabled="isEditing"
                    class="field-input select"
                  >
                    <option v-for="option in strategyOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                  <p class="field-help">若需其它驱动，可在后端扩展 StorageStrategy 枚举。</p>
                </div>

                <div class="form-field checkbox-field">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="form.activateNow" class="checkbox-input" />
                    <span class="checkbox-text">保存后立即启用此策略</span>
                  </label>
                </div>
              </div>
            </div>

            <!-- 配置分组 -->
            <div
              v-for="group in currentFieldGroups"
              :key="group.title"
              class="form-section"
            >
              <h4 class="section-title">{{ group.title }}</h4>
              <div class="form-grid">
                <div v-for="field in group.fields" :key="field.key" class="form-field">
                  <label class="field-label">{{ field.label }}</label>
                  <template v-if="field.type === 'select'">
                    <select
                      v-model="form.config[field.key]"
                      class="field-input select"
                    >
                      <option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </template>
                  <template v-else-if="field.type === 'checkbox'">
                    <label class="checkbox-label">
                      <input type="checkbox" v-model="form.config[field.key]" class="checkbox-input" />
                      <span class="checkbox-text">启用</span>
                    </label>
                  </template>
                  <template v-else-if="field.type === 'textarea'">
                    <textarea
                      v-model="form.config[field.key]"
                      rows="2"
                      class="field-input textarea"
                      :placeholder="field.placeholder"
                    ></textarea>
                  </template>
                  <template v-else>
                    <input
                      v-model="form.config[field.key]"
                      :type="field.type === 'password' ? 'password' : field.type === 'number' ? 'number' : 'text'"
                      class="field-input"
                      :class="{ 'has-error': formErrors[field.key] }"
                      :placeholder="field.placeholder"
                    />
                  </template>
                  <p v-if="field.description" class="field-help">{{ field.description }}</p>
                  <p v-if="formErrors[field.key]" class="field-error">{{ formErrors[field.key] }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 抽屉底部 -->
          <div class="drawer-footer">
            <button type="button" class="btn-cancel" @click="closeDrawer">
              取消
            </button>
            <button
              type="button"
              class="btn-save"
              :disabled="submitting"
              @click="submitForm"
            >
              {{ submitting ? '保存中...' : '保存策略' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.storage-strategy-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-title .subtitle {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin: 0 0 0.25rem 0;
}

.header-title .title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem 0;
}

.header-title .description {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0;
  max-width: 600px;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 999px;
  box-shadow: 0 8px 25px rgba(255, 107, 157, 0.35);
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-create:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(255, 107, 157, 0.45);
}

.btn-create .icon {
  width: 1.1rem;
  height: 1.1rem;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover {
  background: var(--color-bg-tertiary);
  border-color: var(--border-medium);
}

.btn-refresh .icon {
  width: 1rem;
  height: 1rem;
}

/* 错误提示 */
.error-alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 1rem;
  color: #ef4444;
  margin-bottom: 1.5rem;
}

.error-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 1.5rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-soft);
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: var(--text-muted);
  margin: 0;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 1.5rem;
}

.empty-illustration {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(255, 143, 171, 0.1) 100%);
  border-radius: 1.5rem;
  margin-bottom: 1.5rem;
}

.empty-icon {
  width: 40px;
  height: 40px;
  color: #ff6b9d;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem 0;
}

.empty-desc {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0 0 1.5rem 0;
}

/* 策略列表 */
.strategy-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.strategy-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 1.25rem;
  padding: 1.5rem;
  transition: all 0.3s ease;
}

.strategy-card:hover {
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.08);
}

.strategy-card.is-active {
  border-color: rgba(34, 197, 94, 0.4);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, rgba(34, 197, 94, 0.03) 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.card-type {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.type-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  transition: all 0.3s ease;
}

.type-icon.active {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15) 0%, rgba(34, 197, 94, 0.05) 100%);
  color: #22c55e;
}

.type-icon.inactive {
  background: linear-gradient(135deg, rgba(156, 163, 175, 0.15) 0%, rgba(156, 163, 175, 0.05) 100%);
  color: #9ca3af;
}

.type-icon .icon {
  width: 24px;
  height: 24px;
}

.type-info .type-label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin: 0 0 0.25rem 0;
}

.type-info .strategy-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: 999px;
}

.status-badge.active {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.status-badge.standby {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}

.badge-icon {
  width: 1rem;
  height: 1rem;
}

.card-body {
  margin-bottom: 1.25rem;
}

.strategy-desc {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0 0 1rem 0;
  line-height: 1.5;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.config-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-soft);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.config-value {
  font-size: 0.9rem;
  color: var(--color-text-primary);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.config-value.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1.25rem;
  border-top: 1px solid var(--border-soft);
}

.update-info {
  font-size: 0.8rem;
  color: var(--text-faint);
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
  border: 1px solid var(--border-soft);
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: var(--color-bg-tertiary);
  border-color: var(--border-medium);
}

.action-btn.activate {
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.3);
}

.action-btn.activate:hover {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.5);
}

.action-btn.delete {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}

.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.5);
}

.action-icon {
  width: 1rem;
  height: 1rem;
}

/* 抽屉遮罩 */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  justify-content: flex-end;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.drawer-panel {
  width: 100%;
  max-width: 640px;
  height: 100%;
  background: var(--color-bg-primary);
  display: flex;
  flex-direction: column;
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.2);
}

/* 抽屉头部 */
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-soft);
  flex-shrink: 0;
}

.header-content .drawer-subtitle {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin: 0 0 0.25rem 0;
}

.header-content .drawer-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-muted);
}

.close-btn:hover {
  background: var(--color-bg-tertiary);
  border-color: var(--border-medium);
  color: var(--color-text-primary);
}

.close-icon {
  width: 1.25rem;
  height: 1.25rem;
}

/* 抽屉内容 */
.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.form-section {
  margin-bottom: 2rem;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-soft);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

.field-hint {
  font-size: 0.75rem;
  color: var(--text-faint);
  font-weight: 400;
}

.field-input {
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  border-radius: 0.75rem;
  transition: all 0.2s ease;
  font-family: inherit;
}

.field-input:focus {
  outline: none;
  border-color: #ff6b9d;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.field-input.has-error {
  border-color: #ef4444;
}

.field-input.has-error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.field-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: var(--color-bg-tertiary);
}

.field-input.textarea {
  resize: vertical;
  min-height: 80px;
}

.field-input.select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  padding-right: 2.5rem;
}

.field-help {
  font-size: 0.75rem;
  color: var(--text-faint);
  margin: 0;
}

.field-error {
  font-size: 0.75rem;
  color: #ef4444;
  margin: 0;
}

.checkbox-field {
  display: flex;
  align-items: flex-end;
  padding-bottom: 0.5rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  cursor: pointer;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid var(--border-soft);
  border-radius: 0.375rem;
  cursor: pointer;
  accent-color: #ff6b9d;
}

.checkbox-text {
  font-size: 0.9rem;
  color: var(--color-text-primary);
}

/* 抽屉底部 */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-soft);
  background: var(--color-bg-secondary);
  flex-shrink: 0;
}

.btn-cancel {
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-muted);
  background: transparent;
  border: 1px solid var(--border-soft);
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel:hover {
  background: var(--color-bg-tertiary);
  border-color: var(--border-medium);
  color: var(--color-text-primary);
}

.btn-save {
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.35);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.45);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 抽屉动画 */
.drawer-enter-active,
.drawer-leave-active {
  transition: all 0.3s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .drawer-panel,
.drawer-leave-to .drawer-panel {
  transform: translateX(100%);
}

.drawer-enter-to .drawer-panel,
.drawer-leave-from .drawer-panel {
  transform: translateX(0);
}

/* 响应式 */
@media (max-width: 768px) {
  .storage-strategy-page {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .header-actions {
    width: 100%;
    justify-content: stretch;
  }

  .btn-create,
  .btn-refresh {
    flex: 1;
    justify-content: center;
  }

  .config-grid {
    grid-template-columns: 1fr;
  }

  .card-footer {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .card-actions {
    justify-content: stretch;
  }

  .action-btn {
    flex: 1;
    justify-content: center;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-field.full-width {
    grid-column: span 1;
  }

  .drawer-panel {
    max-width: 100%;
  }
}
</style>
