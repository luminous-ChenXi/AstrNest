<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useStorageStrategyStore } from '@/stores/storageStrategies'
import { Plus, RefreshCw, Database, Settings, Trash2, CheckCircle2, AlertCircle } from 'lucide-vue-next'

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
  <div class="storage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <p class="subtitle">存储管理</p>
        <h1 class="title">存储策略中心</h1>
        <p class="description">多云策略管理，集中创建或切换 OSS/COS/OneDrive 等驱动</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="handleRefresh">
          <RefreshCw class="icon" />
          刷新
        </button>
        <button class="btn-create" @click="openCreateDrawer">
          <Plus class="icon" />
          添加策略
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="storageStrategyStore.errorMessage" class="error-alert">
      <AlertCircle class="alert-icon" />
      {{ storageStrategyStore.errorMessage }}
    </div>

    <!-- 加载状态 -->
    <div v-if="storageStrategyStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载存储策略...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="sortedStrategies.length === 0" class="empty-state">
      <Database class="empty-icon" />
      <p class="empty-title">暂无存储策略</p>
      <p class="empty-desc">点击右上角"添加策略"开始配置本地或第三方对象存储</p>
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
            <Database class="type-icon" />
            <span class="type-label">{{ typeLabel(profile.strategy) }}</span>
          </div>
          <span
            class="status-badge"
            :class="profile.active ? 'status-active' : 'status-standby'"
          >
            <CheckCircle2 v-if="profile.active" class="status-icon" />
            {{ profile.active ? '已启用' : '备用' }}
          </span>
        </div>

        <div class="card-body">
          <h3 class="card-title">{{ profile.displayName }}</h3>
          <p v-if="profile.description" class="card-desc">{{ profile.description }}</p>
          
          <div class="card-info">
            <div class="info-row">
              <span class="info-label">Bucket / Drive</span>
              <span class="info-value">{{ profile.config?.bucket || profile.config?.driveId || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">EndPoint / Base URL</span>
              <span class="info-value">{{ profile.config?.endpoint || profile.config?.baseUrl || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">地区代码</span>
              <span class="info-value">{{ profile.config?.region || profile.config?.siteId || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">最近更新</span>
              <span class="info-value">{{ formatUpdatedAt(profile.updatedAt) }} · {{ profile.updatedBy || 'system' }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <button class="btn-edit" @click="openEditDrawer(profile)">
            <Settings class="btn-icon" />
            编辑配置
          </button>
          <button
            v-if="!profile.active"
            class="btn-activate"
            @click="handleActivate(profile)"
          >
            <CheckCircle2 class="btn-icon" />
            设为默认
          </button>
          <button
            v-if="!profile.active"
            class="btn-delete"
            @click="handleDelete(profile)"
          >
            <Trash2 class="btn-icon" />
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 表单抽屉 -->
    <div v-if="isFormVisible" class="drawer-overlay">
      <div class="drawer-backdrop" @click="closeDrawer"></div>
      <div class="drawer-panel">
        <div class="drawer-header">
          <div>
            <p class="drawer-subtitle">{{ isEditing ? '更新已有策略' : '新增存储策略' }}</p>
            <h3 class="drawer-title">{{ formTitle }}</h3>
          </div>
          <button type="button" class="btn-close" @click="closeDrawer">✕</button>
        </div>

        <div class="drawer-body">
          <div class="form-grid">
            <label class="form-field">
              <span class="field-label">策略名称 (唯一标识)</span>
              <input
                v-model="form.name"
                :disabled="isEditing"
                type="text"
                class="field-input"
                :class="{ 'has-error': formErrors.name }"
                placeholder="oss-primary"
              />
              <p v-if="!isEditing" class="field-hint">创建后不可修改，仅用于系统区分。</p>
              <p v-else class="field-hint">策略名称不可变更。</p>
              <p v-if="formErrors.name" class="field-error">{{ formErrors.name }}</p>
            </label>

            <label class="form-field">
              <span class="field-label">展示名</span>
              <input
                v-model="form.displayName"
                type="text"
                class="field-input"
                :class="{ 'has-error': formErrors.displayName }"
                placeholder="上海 OSS 主集群"
              />
              <p class="field-hint">会展示给管理员与成员，建议带上地域信息。</p>
              <p v-if="formErrors.displayName" class="field-error">{{ formErrors.displayName }}</p>
            </label>

            <label class="form-field full-width">
              <span class="field-label">策略描述</span>
              <textarea
                v-model="form.description"
                rows="2"
                class="field-textarea"
                placeholder="描述用途、接入团队或路由策略，方便日后回溯"
              ></textarea>
            </label>

            <label class="form-field">
              <span class="field-label">存储类型</span>
              <select
                v-model="form.strategy"
                :disabled="isEditing"
                class="field-select"
              >
                <option v-for="option in strategyOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
              <p class="field-hint">若需其它驱动，可在后端扩展 StorageStrategy 枚举。</p>
            </label>

            <label class="form-field checkbox-field">
              <input type="checkbox" v-model="form.activateNow" class="field-checkbox" />
              <span class="checkbox-label">保存后立即启用此策略</span>
            </label>
          </div>

          <div class="config-sections">
            <div
              v-for="group in currentFieldGroups"
              :key="group.title"
              class="config-section"
            >
              <h4 class="section-title">{{ group.title }}</h4>
              <div class="section-grid">
                <div v-for="field in group.fields" :key="field.key" class="form-field">
                  <label class="field-label">{{ field.label }}</label>
                  <template v-if="field.type === 'select'">
                    <select
                      v-model="form.config[field.key]"
                      class="field-select"
                    >
                      <option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </template>
                  <template v-else-if="field.type === 'checkbox'">
                    <label class="checkbox-inline">
                      <input type="checkbox" v-model="form.config[field.key]" class="field-checkbox" />
                      <span class="checkbox-label">启用</span>
                    </label>
                  </template>
                  <template v-else-if="field.type === 'textarea'">
                    <textarea
                      v-model="form.config[field.key]"
                      rows="2"
                      class="field-textarea"
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
                  <p v-if="field.description" class="field-hint">{{ field.description }}</p>
                  <p v-if="formErrors[field.key]" class="field-error">{{ formErrors[field.key] }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

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
  </div>
</template>

<style scoped>
.storage-page {
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
  color: #ff8fab;
  margin: 0 0 0.25rem 0;
}

.header-title .title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 0.5rem 0;
}

.header-title .description {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
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

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #666;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover {
  border-color: #ff8fab;
  color: #ff6b9d;
}

.icon {
  width: 1.1rem;
  height: 1.1rem;
}

/* 错误提示 */
.error-alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 1rem;
  color: #dc2626;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.alert-icon {
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
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 1.5rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f0f0f0;
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: #666;
  font-size: 0.95rem;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 1.5rem;
}

.empty-icon {
  width: 56px;
  height: 56px;
  color: #ff8fab;
  margin-bottom: 1rem;
}

.empty-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 0.5rem 0;
}

.empty-desc {
  font-size: 0.9rem;
  color: #888;
  margin: 0;
}

/* 策略列表 */
.strategy-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 1.25rem;
}

.strategy-card {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 1.25rem;
  padding: 1.5rem;
  transition: all 0.3s ease;
}

.strategy-card:hover {
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 8px 30px rgba(255, 107, 157, 0.1);
}

.strategy-card.is-active {
  border-color: rgba(255, 107, 157, 0.4);
  background: linear-gradient(135deg, white 0%, rgba(255, 107, 157, 0.03) 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.card-type {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.type-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: #ff6b9d;
}

.type-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #ff6b9d;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.875rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: 999px;
}

.status-active {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.status-standby {
  background: #f3f4f6;
  color: #6b7280;
}

.status-icon {
  width: 0.875rem;
  height: 0.875rem;
}

.card-body {
  margin-bottom: 1.25rem;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 0.5rem 0;
}

.card-desc {
  font-size: 0.85rem;
  color: #666;
  margin: 0 0 1rem 0;
  line-height: 1.5;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  font-size: 0.85rem;
}

.info-label {
  color: #999;
  flex-shrink: 0;
}

.info-value {
  color: #444;
  text-align: right;
  word-break: break-all;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid #f5f5f5;
}

.btn-edit,
.btn-activate,
.btn-delete {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.5rem 0.875rem;
  font-size: 0.8rem;
  font-weight: 500;
  border-radius: 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-icon {
  width: 0.875rem;
  height: 0.875rem;
}

.btn-edit {
  background: #f3f4f6;
  color: #4b5563;
}

.btn-edit:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.btn-activate {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.btn-activate:hover {
  background: rgba(34, 197, 94, 0.2);
}

.btn-delete {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
}

.btn-delete:hover {
  background: rgba(239, 68, 68, 0.15);
}

/* 抽屉 */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
}

.drawer-panel {
  position: relative;
  z-index: 10;
  height: 100%;
  width: 100%;
  max-width: 720px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.5rem;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.drawer-subtitle {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #ff8fab;
  margin: 0 0 0.25rem 0;
}

.drawer-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.btn-close {
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: #6b7280;
  background: #f3f4f6;
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-close:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  background: white;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

/* 表单样式 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.field-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.field-input,
.field-select,
.field-textarea {
  padding: 0.625rem 0.875rem;
  font-size: 0.9rem;
  color: #1f2937;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 0.625rem;
  transition: all 0.2s ease;
}

.field-input:focus,
.field-select:focus,
.field-textarea:focus {
  outline: none;
  border-color: #ff6b9d;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.field-input.has-error,
.field-select.has-error {
  border-color: #ef4444;
}

.field-input:disabled {
  background: #f3f4f6;
  color: #6b7280;
  cursor: not-allowed;
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.field-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2.25rem;
}

.field-hint {
  font-size: 0.8rem;
  color: #9ca3af;
  margin: 0;
}

.field-error {
  font-size: 0.8rem;
  color: #ef4444;
  margin: 0;
}

.checkbox-field,
.checkbox-inline {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-direction: row;
}

.field-checkbox {
  width: 1.125rem;
  height: 1.125rem;
  border: 2px solid #d1d5db;
  border-radius: 0.25rem;
  cursor: pointer;
  accent-color: #ff6b9d;
}

.checkbox-label {
  font-size: 0.9rem;
  color: #374151;
  cursor: pointer;
}

/* 配置区块 */
.config-sections {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.config-section {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  padding: 1.25rem;
}

.section-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f3f4f6;
}

.section-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

/* 按钮 */
.btn-cancel {
  padding: 0.625rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #6b7280;
  background: #f3f4f6;
  border: none;
  border-radius: 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel:hover {
  background: #e5e7eb;
  color: #374151;
}

.btn-save {
  padding: 0.625rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border: none;
  border-radius: 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.3);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 640px) {
  .strategy-list {
    grid-template-columns: 1fr;
  }
  
  .form-grid,
  .section-grid {
    grid-template-columns: 1fr;
  }
  
  .form-field.full-width {
    grid-column: span 1;
  }
  
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: flex-end;
  }
}
</style>
