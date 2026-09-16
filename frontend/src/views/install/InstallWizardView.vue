<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  WarningFilled,
  Refresh,
  ArrowRight,
  HomeFilled,
  Loading,
  Monitor,
  Coin,
  User,
  SuccessFilled,
} from '@element-plus/icons-vue'
import {
  createInstallAdmin,
  finishInstall,
  getInstallError,
  runDatabaseInstall,
} from '../../services/install'
import { useInstallStore } from '../../stores/install'

const router = useRouter()
const installStore = useInstallStore()

const STEPS = ['环境检测', '安装数据库', '创建管理员', '完成']

const loading = ref(true)
const currentStep = ref(0)
const alreadyInstalled = ref(false)
const installError = ref(null)

const installingDb = ref(false)
const databaseResult = ref(null)
const creatingAdmin = ref(false)
const adminResult = ref(null)
const finishing = ref(false)
const finishResult = ref(null)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const DOC_URL = 'https://github.com/luminous-ChenXi/AstrNest/blob/main/CONFIG_GUIDE.md'

const checks = computed(() => installStore.checks)
const schemaState = computed(() => installStore.schemaState)

const fatalChecks = computed(() =>
  checks.value.filter((item) => !item.passed && !item.warning)
)
const hasFatalProblem = computed(() => fatalChecks.value.length > 0)

const usernamePattern = /^[A-Za-z0-9_.-]{3,32}$/

const rules = {
  username: [
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) return callback(new Error('请输入用户名'))
        if (!usernamePattern.test(value.trim())) {
          return callback(new Error('用户名需 3-32 位，仅允许字母、数字、下划线、点、短横线'))
        }
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请设置登录密码'))
        if (value.length < 8 || value.length > 64) {
          return callback(new Error('密码长度需在 8-64 位之间'))
        }
        if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) {
          return callback(new Error('密码需同时包含字母和数字'))
        }
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请再次输入密码'))
        if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
}

const formRef = ref()

const refreshStatus = async (force = true) => {
  loading.value = true
  installError.value = null
  try {
    await installStore.fetchStatus(force)
    // 已安装且已写完成标记 → 展示「系统已安装」卡片；
    // 已建管理员但未写标记（如第 4 步前刷新页面）→ 直接进入「完成」步骤收尾
    alreadyInstalled.value = installStore.installed && installStore.finished
    if (installStore.installed && !installStore.finished) {
      currentStep.value = 3
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshStatus(true)
})

// 进入「安装数据库」步骤时：表结构已就绪（EMPTY/INSTALLED）则自动跳过
watch(currentStep, (step) => {
  if (step === 1 && schemaState.value && schemaState.value !== 'NOT_INSTALLED') {
    currentStep.value = 2
  }
})

const goNextFromChecks = () => {
  if (hasFatalProblem.value) return
  if (!schemaState.value) {
    ElMessage.warning('请先完成环境检测')
    return
  }
  currentStep.value = schemaState.value === 'NOT_INSTALLED' ? 1 : 2
}

const handleInstallDatabase = async () => {
  installingDb.value = true
  try {
    databaseResult.value = await runDatabaseInstall()
    await refreshStatus(true)
    if (databaseResult.value?.success) {
      ElMessage.success('数据库安装完成')
    }
  } catch (error) {
    ElMessage.error(getInstallError(error, '数据库安装失败'))
  } finally {
    installingDb.value = false
  }
}

const handleCreateAdmin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }
  creatingAdmin.value = true
  try {
    adminResult.value = await createInstallAdmin({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      displayName: form.username.trim(),
    })
    ElMessage.success('管理员创建成功')
    currentStep.value = 3
  } catch (error) {
    ElMessage.error(getInstallError(error, '创建管理员失败'))
  } finally {
    creatingAdmin.value = false
  }
}

const goFinish = async () => {
  if (finishing.value) return
  finishing.value = true
  try {
    finishResult.value = await finishInstall()
  } catch (error) {
    if (error?.response?.status === 403) {
      // 403 = 完成标记已存在（向导已结束），视为成功
      finishResult.value = { success: true, installedAt: '', message: '安装已完成' }
    } else {
      ElMessage.error(getInstallError(error, '完成安装失败'))
    }
  } finally {
    finishing.value = false
  }
}

watch(currentStep, (step) => {
  if (step === 3 && !finishResult.value) {
    goFinish()
  }
})

const goHome = async () => {
  // 强制刷新安装状态，避免路由守卫使用过期缓存再次跳回 /install
  await installStore.fetchStatus(true)
  router.push('/')
}
</script>

<template>
  <div class="install-page">
    <div class="install-shell">
      <header class="install-header">
        <div class="brand">
          <span class="brand-mark">A</span>
          <span class="brand-name">AstrNest</span>
        </div>
        <h1 class="install-title">安装向导</h1>
        <p class="install-subtitle">首次部署引导：检测环境 → 安装数据库 → 创建管理员 → 完成</p>
      </header>

      <el-steps class="install-steps" :active="currentStep" align-center finish-status="success">
        <el-step v-for="(title, index) in STEPS" :key="title" :title="title" :description="`第 ${index + 1} 步`" />
      </el-steps>

      <!-- 已安装：拒绝重新安装 -->
      <section v-if="alreadyInstalled" class="install-card">
        <div class="installed-box">
          <el-icon class="installed-icon" :size="46"><SuccessFilled /></el-icon>
          <h2>系统已安装</h2>
          <p class="muted">检测到 AstrNest 已完成安装。出于安全考虑，安装向导已关闭，不可重复初始化。</p>
          <div class="actions">
            <el-button type="primary" size="large" @click="goHome">
              <el-icon class="btn-icon"><HomeFilled /></el-icon>
              前往首页
            </el-button>
          </div>
        </div>
      </section>

      <!-- 状态未知 / 请求失败：给重试入口，避免死循环 -->
      <section v-else-if="installStore.status === 'unknown'" class="install-card">
        <el-alert type="error" :closable="false" show-icon>
          <template #title>无法获取安装状态</template>
          <div class="muted">
            {{ installStore.error || '无法连接服务器' }}。请确认后端服务与数据库连接配置
            （ASTRNEST_DB_URL / ASTRNEST_DB_USERNAME / ASTRNEST_DB_PASSWORD）后重试。
            更多排查方式见
            <a :href="DOC_URL" target="_blank" rel="noreferrer">CONFIG_GUIDE</a>。
          </div>
        </el-alert>
        <div class="actions">
          <el-button :icon="Refresh" :loading="loading" @click="refreshStatus(true)">重新检测</el-button>
        </div>
      </section>

      <template v-else>
        <!-- 步骤 1：环境检测 -->
        <section v-show="currentStep === 0" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Monitor /></el-icon>
            <div>
              <h2>环境检测</h2>
              <p class="muted">检查数据库、存储目录与运行环境，确认满足安装条件。</p>
            </div>
          </div>

          <ul class="check-list">
            <li v-for="item in checks" :key="item.id" class="check-item">
              <el-icon
                class="check-icon"
                :class="item.passed && !item.warning ? 'ok' : item.passed ? 'warn' : item.warning ? 'warn' : 'fail'"
                :size="22"
              >
                <CircleCheckFilled v-if="item.passed && !item.warning" />
                <WarningFilled v-else-if="item.warning" />
                <CircleCloseFilled v-else />
              </el-icon>
              <div class="check-body">
                <div class="check-name">
                  {{ item.name }}
                  <el-tag v-if="item.warning" size="small" type="warning" effect="plain">警告</el-tag>
                </div>
                <div class="check-detail">{{ item.detail }}</div>
              </div>
            </li>
          </ul>

          <el-alert
            v-if="hasFatalProblem"
            type="error"
            :closable="false"
            show-icon
            title="存在致命问题，暂时无法继续安装"
          >
            <div class="muted">
              请优先修复上方标红的检查项（通常是数据库连接）。排查思路：确认 MySQL 已启动、账号密码正确、
              数据库允许来自应用主机的连接；详见
              <a :href="DOC_URL" target="_blank" rel="noreferrer">CONFIG_GUIDE</a>。修复后点击「重新检测」。
            </div>
          </el-alert>

          <div class="actions">
            <el-button :icon="Refresh" :loading="loading" @click="refreshStatus(true)">重新检测</el-button>
            <el-button
              type="primary"
              :disabled="hasFatalProblem || loading || !schemaState"
              @click="goNextFromChecks"
            >
              下一步
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </section>

        <!-- 步骤 2：安装数据库 -->
        <section v-show="currentStep === 1" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Coin /></el-icon>
            <div>
              <h2>安装数据库</h2>
              <p class="muted">将创建 AstrNest 所需的数据表、索引与默认配置（幂等执行，重复点击不会破坏已有数据）。</p>
            </div>
          </div>

          <div v-if="!databaseResult" class="step-body">
            <p class="muted">即将执行 <code>backend/db/install-schema.sql</code>：包含用户、媒体、图集、公告等核心表与默认角色。</p>
            <div class="actions">
              <el-button type="primary" size="large" :loading="installingDb" @click="handleInstallDatabase">
                {{ installingDb ? '正在安装…' : '开始安装数据库' }}
              </el-button>
            </div>
          </div>

          <div v-else class="step-body">
            <el-alert
              :type="databaseResult.success ? 'success' : 'error'"
              :closable="false"
              show-icon
              :title="databaseResult.success ? '数据库安装完成' : '数据库安装未完全成功'"
            >
              <div class="muted">
                共执行 {{ databaseResult.executedStatements }} 条语句（其中建表语句 {{ databaseResult.tablesCreated }} 条，已存在的自动跳过），
                失败 {{ databaseResult.errors.length }} 条，跳过 {{ databaseResult.skippedStatements }} 条已存在项。
                <template v-if="databaseResult.errors && databaseResult.errors.length">
                  失败详情请查看后端日志。
                </template>
              </div>
            </el-alert>
            <div class="actions">
              <el-button :loading="installingDb" @click="handleInstallDatabase">重新执行</el-button>
              <el-button
                v-if="databaseResult.success"
                type="primary"
                @click="currentStep = 2"
              >
                下一步
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </section>

        <!-- 步骤 3：创建管理员 -->
        <section v-show="currentStep === 2" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><User /></el-icon>
            <div>
              <h2>创建管理员</h2>
              <p class="muted">这是系统的初始管理员账号（ADMIN 角色，上传配额不限），请妥善保管。</p>
            </div>
          </div>

          <div v-if="!adminResult" class="step-body">
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="admin-form">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="3-32 位字母、数字、下划线、点、短横线" maxlength="32" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="用于找回密码与站内通知" maxlength="180" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位，需同时包含字母和数字" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入相同密码" />
              </el-form-item>
            </el-form>
            <div class="actions">
              <el-button type="primary" size="large" :loading="creatingAdmin" @click="handleCreateAdmin">
                创建管理员账号
              </el-button>
            </div>
          </div>

          <div v-else class="step-body">
            <el-alert type="success" :closable="false" show-icon title="管理员创建成功">
              <div class="muted">
                账号 <strong>{{ adminResult.username }}</strong>（{{ adminResult.role }}）已创建，
                绑定邮箱 {{ adminResult.email }}。请牢记密码，稍后可在个人中心修改。
              </div>
            </el-alert>
            <div class="actions">
              <el-button type="primary" @click="currentStep = 3">
                下一步
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </section>

        <!-- 步骤 4：完成 -->
        <section v-show="currentStep === 3" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><SuccessFilled /></el-icon>
            <div>
              <h2>完成安装</h2>
              <p class="muted">写入完成标记并收尾，之后即可正常访问站点。</p>
            </div>
          </div>

          <div v-if="finishing" class="step-body center">
            <el-icon class="rotating" :size="30"><Loading /></el-icon>
            <p class="muted">正在完成安装…</p>
          </div>

          <div v-else-if="finishResult && finishResult.success" class="step-body">
            <el-alert type="success" :closable="false" show-icon title="AstrNest 安装完成">
              <div class="muted">
                <template v-if="finishResult.installedAt">完成时间：{{ finishResult.installedAt }}。</template>
                安装后再次访问 <code>/install</code> 将提示「系统已安装」，写接口也会被拒绝，
                如需重建请清空数据库后重新部署。
              </div>
            </el-alert>
            <ul class="check-list">
              <li v-for="item in checks" :key="item.id" class="check-item">
                <el-icon
                  class="check-icon"
                  :class="item.passed && !item.warning ? 'ok' : 'warn'"
                  :size="22"
                >
                  <WarningFilled v-if="item.warning" />
                  <CircleCheckFilled v-else />
                </el-icon>
                <div class="check-body">
                  <div class="check-name">{{ item.name }}</div>
                  <div class="check-detail">{{ item.detail }}</div>
                </div>
              </li>
            </ul>
            <div class="actions">
              <el-button type="primary" size="large" @click="goHome">
                <el-icon class="btn-icon"><HomeFilled /></el-icon>
                前往登录
              </el-button>
            </div>
          </div>

          <div v-else class="step-body center">
            <el-alert type="error" :closable="false" show-icon title="完成安装失败">
              <div class="muted">请检查数据库连接后重试。</div>
            </el-alert>
            <div class="actions">
              <el-button type="primary" :loading="finishing" @click="goFinish">重试完成安装</el-button>
            </div>
          </div>
        </section>
      </template>

      <footer class="install-footer">
        <span class="muted">AstrNest · 开箱即用的媒体管理系统</span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.install-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 15% 20%, rgba(255, 182, 193, 0.25), transparent 45%),
    radial-gradient(circle at 85% 10%, rgba(168, 230, 207, 0.22), transparent 40%),
    var(--bg-gradient, var(--color-bg-secondary, #fafbfc));
  display: flex;
  justify-content: center;
  padding: 48px 16px 32px;
  box-sizing: border-box;
  color: var(--color-text-primary);
}

.install-shell {
  width: 100%;
  max-width: 860px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.install-header {
  text-align: center;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--color-on-accent);
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.install-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
}

.install-subtitle {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}

.install-steps {
  padding: 6px 8px;
}

.install-card {
  background: var(--glass-bg, var(--color-bg-primary));
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  box-shadow: var(--shadow-card, 0 20px 50px rgba(0, 0, 0, 0.08));
  padding: 26px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.card-icon {
  margin-top: 3px;
  color: var(--color-brand-primary);
}

.card-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}

.muted {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.check-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.check-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
}

.check-icon.ok {
  color: var(--color-brand-success, #2ecc71);
}

.check-icon.warn {
  color: #e6a23c;
}

.check-icon.fail {
  color: #f56c6c;
}

.check-name {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-detail {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
  word-break: break-all;
}

.step-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-body.center {
  align-items: center;
  padding: 10px 0;
}

.admin-form {
  max-width: 460px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-icon {
  margin-left: 4px;
}

.installed-box {
  text-align: center;
  padding: 28px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.installed-icon {
  color: var(--color-brand-success, #2ecc71);
}

.installed-box h2 {
  margin: 0;
}

.installed-box .actions {
  justify-content: center;
  margin-top: 8px;
}

.install-footer {
  text-align: center;
  padding-bottom: 10px;
}

code {
  padding: 1px 6px;
  border-radius: 6px;
  background: var(--code-bg, rgba(78, 205, 196, 0.08));
  font-size: 12px;
}

.rotating {
  animation: install-spin 1s linear infinite;
  color: var(--color-brand-primary);
}

@keyframes install-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .install-card {
    padding: 20px 16px;
  }

  .actions {
    flex-direction: column-reverse;
  }

  .actions .el-button {
    width: 100%;
  }
}
</style>
