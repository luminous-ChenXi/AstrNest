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
  Connection,
  Coin,
  Setting,
  User,
  Key,
  CopyDocument,
  SuccessFilled,
  Message,
  Lock,
  Link,
} from '@element-plus/icons-vue'
import {
  createInstallAdmin,
  finishInstall,
  getInstallError,
  getInstallToken,
  resetInstallState,
  runDatabaseInstall,
  saveSiteConfig,
  setInstallToken,
  testDatabaseConnection,
} from '../../services/install'
import { useInstallStore } from '../../stores/install'

const router = useRouter()
const installStore = useInstallStore()

// 统一规范流程：环境检测 → 数据库配置 → 初始化 → 站点配置 → 创建管理员 → 完成
const STEPS = ['环境检测', '数据库配置', '初始化', '站点配置', '创建管理员', '完成']
const STEP = { CHECKS: 0, DATABASE: 1, INIT: 2, SITE: 3, ADMIN: 4, FINISH: 5 }

const loading = ref(true)
const currentStep = ref(0)
const alreadyInstalled = ref(false)
const installError = ref(null)

// 安装令牌：服务端配置 CHENXI_INSTALL_TOKEN 后，向导写操作必须携带 X-Chenxi-Install-Token
const installTokenInput = ref(getInstallToken())
watch(installTokenInput, (value) => setInstallToken(value))

const installingDb = ref(false)
const databaseResult = ref(null)
const savingSite = ref(false)
const creatingAdmin = ref(false)
const adminResult = ref(null)
const finishing = ref(false)
const finishResult = ref(null)
const resetting = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

// 站点配置（全部可跳过：保存才写入，未提供的项保持系统默认）
const siteForm = reactive({
  registrationEnabled: false,
  guestUploadEnabled: false,
  maxUploadMb: 20,
  assetDomain: '',
  registrationEmailVerifyRequired: false,
  loginTotpRequired: false,
})

// 数据库配置：位置（本机/远程）+ 连接参数 + 即时测试
const dbForm = reactive({
  location: 'local',
  host: 'localhost',
  port: 3306,
  databaseName: '',
  username: '',
  password: '',
})
const dbTesting = ref(false)
const dbTestResult = ref(null)
const dbCreateIfMissing = ref(false)

// 强密码一次性展示：明文只在生成后/创建前可见，创建成功后系统任何页面都不再回显
const generatedPassword = ref('')

const DOC_URL = 'https://github.com/luminous-ChenXi/AstrNest/blob/main/CONFIG_GUIDE.md'

const checks = computed(() => installStore.checks)
const schemaState = computed(() => installStore.schemaState)

const fatalChecks = computed(() =>
  checks.value.filter((item) => !item.passed && !item.warning)
)
const hasFatalProblem = computed(() => fatalChecks.value.length > 0)

const databaseCheck = computed(() => checks.value.find((item) => item.id === 'database') || null)

// 完成汇总：站点地址取当前访问 origin；开关状态来自 finish 响应的 summary 快照
const siteUrl = `${window.location.origin}`
const finishSummary = computed(() => finishResult.value?.summary || null)

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
    // 已安装且（已写完成标记或防重装锁命中）→ 展示「系统已安装」卡片；
    // 已建管理员但未收尾（如最后一步前刷新页面）→ 直接进入「完成」步骤收尾
    alreadyInstalled.value = installStore.installed && (installStore.finished || installStore.locked)
    if (installStore.installed && !installStore.finished && !installStore.locked) {
      currentStep.value = STEP.FINISH
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshStatus(true)
})

// 流程跳转守卫：进入「初始化」时表结构已就绪则跳过；数据库不可用时无法前进
watch(currentStep, (step) => {
  if (step === STEP.INIT && schemaState.value && schemaState.value !== 'NOT_INSTALLED') {
    currentStep.value = STEP.SITE
  }
})

const goNextFromChecks = () => {
  if (hasFatalProblem.value) return
  if (!schemaState.value) {
    ElMessage.warning('请先完成环境检测')
    return
  }
  currentStep.value = STEP.DATABASE
}

const goNextFromDatabase = () => {
  if (!databaseCheck.value?.passed) return
  if (!dbTestResult.value?.success) {
    ElMessage.warning('请先完成「测试连接」并确认连接成功')
    return
  }
  currentStep.value = schemaState.value === 'NOT_INSTALLED' ? STEP.INIT : STEP.SITE
}

// ==================== 数据库连接测试 ====================

const switchDbLocation = (location) => {
  dbForm.location = location
  dbTestResult.value = null
  dbCreateIfMissing.value = false
  if (location === 'local') {
    dbForm.host = 'localhost'
    dbForm.port = 3306
  } else {
    dbForm.host = ''
    dbForm.port = 3306
  }
}

const handleTestConnection = async (withCreate = false) => {
  const host = (dbForm.host || '').trim()
  const name = (dbForm.databaseName || '').trim()
  const username = (dbForm.username || '').trim()
  if (!host) {
    ElMessage.warning('请填写数据库主机地址')
    return
  }
  if (!name) {
    ElMessage.warning('请填写数据库名称')
    return
  }
  if (!username) {
    ElMessage.warning('请填写数据库用户名')
    return
  }
  dbTesting.value = true
  try {
    const data = await testDatabaseConnection({
      location: dbForm.location,
      host,
      port: Number(dbForm.port) || 3306,
      databaseName: name,
      username,
      password: dbForm.password,
      createIfMissing: Boolean(withCreate),
    })
    dbTestResult.value = data
    if (data.success) {
      ElMessage.success(withCreate ? '数据库已创建并连接成功' : '连接成功')
    } else {
      ElMessage.error(data.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error(getInstallError(error, '测试连接失败'))
  } finally {
    dbTesting.value = false
  }
}

// ==================== 初始化 / 重置 ====================

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

/**
 * 重置安装状态：装库/收尾失败后的恢复入口。
 * 清理 install.lock 与 DB 完成标记（仅未完成站点可调），重置后回到第一步重走流程。
 */
const handleResetInstall = async () => {
  resetting.value = true
  try {
    const data = await resetInstallState()
    ElMessage.success(data.message || '安装状态已重置')
    databaseResult.value = null
    adminResult.value = null
    finishResult.value = null
    dbTestResult.value = null
    currentStep.value = STEP.CHECKS
    await refreshStatus(true)
  } catch (error) {
    ElMessage.error(getInstallError(error, '重置安装状态失败'))
  } finally {
    resetting.value = false
  }
}

// ==================== 站点配置 ====================

const handleSaveSiteConfig = async () => {
  savingSite.value = true
  try {
    await saveSiteConfig({
      registrationEnabled: siteForm.registrationEnabled,
      guestUploadEnabled: siteForm.guestUploadEnabled,
      maxUploadMb: siteForm.maxUploadMb,
      assetDomain: siteForm.assetDomain.trim(),
      registrationEmailVerifyRequired: siteForm.registrationEmailVerifyRequired,
      loginTotpRequired: siteForm.loginTotpRequired,
    })
    ElMessage.success('站点配置已保存')
    currentStep.value = STEP.ADMIN
  } catch (error) {
    ElMessage.error(getInstallError(error, '站点配置保存失败'))
  } finally {
    savingSite.value = false
  }
}

const skipSiteConfig = () => {
  currentStep.value = STEP.ADMIN
}

/**
 * 生成 16 位强密码：保证至少含大写、小写、数字、符号各一，其余随机填充并整体洗牌。
 * 明文只在生成面板展示一次（可复制），要求在"确认密码"里二次输入；
 * 创建成功后日志、接口响应、完成页一律不回显。
 */
const generateStrongPassword = () => {
  const length = 16
  const lowerSet = 'abcdefghijklmnopqrstuvwxyz'
  const upperSet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  const digitSet = '0123456789'
  const symbolSet = '!@#$%^&*()-_=+[]{}'
  const allSets = [lowerSet, upperSet, digitSet, symbolSet]

  const randomInt = (max) => {
    const bytes = new Uint32Array(1)
    crypto.getRandomValues(bytes)
    return bytes[0] % max
  }

  const chars = allSets.map((set) => set[randomInt(set.length)])
  const allChars = allSets.join('')
  while (chars.length < length) {
    chars.push(allChars[randomInt(allChars.length)])
  }
  // Fisher-Yates 洗牌，打乱"每类一位"的固定位置
  for (let i = chars.length - 1; i > 0; i -= 1) {
    const j = randomInt(i + 1)
    ;[chars[i], chars[j]] = [chars[j], chars[i]]
  }
  const password = chars.join('')
  form.password = password
  form.confirmPassword = ''
  generatedPassword.value = password
}

const copyGeneratedPassword = async () => {
  if (!generatedPassword.value) return
  try {
    await navigator.clipboard.writeText(generatedPassword.value)
    ElMessage.success('密码已复制，请妥善保存')
  } catch (error) {
    ElMessage.warning('复制失败，请手动选择复制')
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
    generatedPassword.value = ''
    form.password = ''
    form.confirmPassword = ''
    currentStep.value = STEP.FINISH
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
      // 403 = 完成标记/锁已存在（向导已结束），视为成功
      finishResult.value = { success: true, installedAt: '', message: '安装已完成' }
    } else {
      ElMessage.error(getInstallError(error, '完成安装失败'))
    }
  } finally {
    finishing.value = false
  }
}

watch(currentStep, (step) => {
  if (step === STEP.FINISH && !finishResult.value) {
    goFinish()
  }
})

// 安装完成强制进入登录页：AstrNest 登录为首页登录弹层（/?login=1）
const goLogin = async () => {
  // 强制刷新安装状态，避免路由守卫使用过期缓存再次跳回 /install
  await installStore.fetchStatus(true)
  router.push({ path: '/', query: { login: '1' } })
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
        <p class="install-subtitle">首次部署引导：环境检测 → 数据库配置 → 初始化 → 站点配置 → 创建管理员 → 完成</p>
      </header>

      <el-steps class="install-steps" :active="currentStep" align-center finish-status="success">
        <el-step v-for="(title, index) in STEPS" :key="title" :title="title" :description="`第 ${index + 1} 步`" />
      </el-steps>

      <!-- 已安装：拒绝重新安装 -->
      <section v-if="alreadyInstalled" class="install-card">
        <div class="installed-box">
          <el-icon class="installed-icon" :size="46"><SuccessFilled /></el-icon>
          <h2>系统已安装</h2>
          <p class="muted">检测到 AstrNest 已完成安装（防重装锁已生效）。出于安全考虑，安装向导已关闭，不可重复初始化。</p>
          <div class="actions">
            <el-button type="primary" size="large" @click="goLogin">
              <el-icon class="btn-icon"><HomeFilled /></el-icon>
              前往登录
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
        <!-- 步骤 1：环境检测（自检报告） -->
        <section v-show="currentStep === 0" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Monitor /></el-icon>
            <div>
              <h2>环境自检报告</h2>
              <p class="muted">逐项检查数据库、存储目录、Java 运行时与邮件服务；带「警告」的项不影响安装，可稍后在管理后台处理。</p>
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

          <div class="install-token-box">
            <el-form label-position="top">
              <el-form-item label="安装令牌（可选）">
                <el-input
                  v-model="installTokenInput"
                  placeholder="部署时配置了 CHENXI_INSTALL_TOKEN 则必填，否则留空"
                  autocomplete="off"
                  show-password
                />
                <div class="muted" style="margin-top: 4px">
                  公网部署时建议在服务端配置安装令牌（.env 的 CHENXI_INSTALL_TOKEN），
                  防止安装完成前被陌生人抢注管理员；此处填写后会自动附加到后续每一步安装请求。
                </div>
              </el-form-item>
            </el-form>
          </div>

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

        <!-- 步骤 2：数据库配置（位置 + 连接参数 + 测试连接） -->
        <section v-show="currentStep === 1" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Connection /></el-icon>
            <div>
              <h2>数据库配置</h2>
              <p class="muted">
                选择数据库位置并填写连接参数，点击「测试连接」即时验证。
                AstrNest 运行时连接由部署配置（ASTRNEST_DB_URL 等）承载，请保持两者一致。
              </p>
            </div>
          </div>

          <div class="step-body">
            <el-radio-group :model-value="dbForm.location" class="db-location" @update:model-value="switchDbLocation">
              <el-radio-button value="local">本机数据库</el-radio-button>
              <el-radio-button value="remote">远程数据库</el-radio-button>
            </el-radio-group>

            <el-form label-position="top" class="db-form">
              <div class="db-grid">
                <el-form-item label="主机地址">
                  <el-input
                    v-model="dbForm.host"
                    :placeholder="dbForm.location === 'local' ? 'localhost' : '如 192.168.1.10 或 db.example.com'"
                  />
                </el-form-item>
                <el-form-item label="端口">
                  <el-input-number v-model="dbForm.port" :min="1" :max="65535" :controls="false" class="port-input" />
                </el-form-item>
                <el-form-item label="数据库名称">
                  <el-input v-model="dbForm.databaseName" placeholder="如 astrnest" maxlength="64" />
                </el-form-item>
                <el-form-item label="用户名">
                  <el-input v-model="dbForm.username" placeholder="数据库账号" autocomplete="off" />
                </el-form-item>
                <el-form-item label="密码" class="db-password">
                  <el-input
                    v-model="dbForm.password"
                    type="password"
                    show-password
                    placeholder="数据库密码（可空）"
                    autocomplete="off"
                  />
                </el-form-item>
              </div>
            </el-form>

            <div class="actions actions--start">
              <el-button type="primary" :loading="dbTesting" @click="handleTestConnection(false)">
                <el-icon class="btn-icon"><Connection /></el-icon>
                {{ dbTesting ? '正在测试连接…' : '测试连接' }}
              </el-button>
              <el-button :icon="Refresh" :loading="loading" @click="refreshStatus(true)">重新检测运行时</el-button>
            </div>

            <!-- 测试成功 -->
            <el-alert
              v-if="dbTestResult?.success"
              type="success"
              :closable="false"
              show-icon
              title="数据库连接成功"
            >
              <div class="muted">
                MySQL 版本：<strong>{{ dbTestResult.mysqlVersion }}</strong>
                <template v-if="dbTestResult.charsetServer">
                  ，服务器字符集 <strong>{{ dbTestResult.charsetServer }}</strong>
                  ，目标库字符集 <strong>{{ dbTestResult.charsetDatabase }}</strong>
                </template>
                <template v-if="dbTestResult.databaseCreated">（本次已自动创建数据库）</template>
              </div>
              <div v-if="!dbTestResult.matchesRuntime" class="muted db-runtime-warn">
                <el-icon><WarningFilled /></el-icon>
                注意：当前后端运行时连接为
                <code>{{ dbTestResult.runtimeJdbcUrl || '（未知）' }}</code>
                ，与上方表单不一致。「初始化」将作用于运行时连接；如需安装到其他数据库，请修改
                ASTRNEST_DB_URL / ASTRNEST_DB_USERNAME / ASTRNEST_DB_PASSWORD 后重启后端再继续。
              </div>
            </el-alert>

            <!-- 测试失败：库不存在 → 提供尝试建库 -->
            <el-alert
              v-else-if="dbTestResult"
              type="error"
              :closable="false"
              show-icon
              title="数据库连接失败"
            >
              <div class="muted">{{ dbTestResult.message }}</div>
              <div v-if="dbTestResult.errorCode === 'DB_MISSING'" class="db-create-row">
                <el-checkbox v-model="dbCreateIfMissing">尝试创建数据库（需要该账号具备建库权限）</el-checkbox>
                <el-button size="small" type="warning" plain :loading="dbTesting" @click="handleTestConnection(true)">
                  按此重试并建库
                </el-button>
              </div>
            </el-alert>
          </div>

          <div class="actions">
            <el-button @click="currentStep = 0">上一步</el-button>
            <el-button
              type="primary"
              :disabled="!databaseCheck?.passed || !dbTestResult?.success"
              @click="goNextFromDatabase"
            >
              下一步
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </section>

        <!-- 步骤 3：初始化 -->
        <section v-show="currentStep === 2" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Coin /></el-icon>
            <div>
              <h2>初始化</h2>
              <p class="muted">将创建 AstrNest 所需的数据表、索引与默认配置（幂等执行，重复点击不会破坏已有数据）。</p>
            </div>
          </div>

          <div v-if="installingDb" class="step-body center">
            <el-icon class="rotating" :size="30"><Loading /></el-icon>
            <p class="muted">正在执行建表脚本，请稍候（通常数秒内完成）…</p>
          </div>

          <div v-else-if="!databaseResult" class="step-body">
            <p class="muted">即将执行 <code>backend/db/install-schema.sql</code>：包含用户、媒体、图集、公告等核心表与默认角色。</p>
            <div class="actions">
              <el-button type="primary" size="large" @click="handleInstallDatabase">开始初始化数据库</el-button>
            </div>
          </div>

          <div v-else class="step-body">
            <el-alert
              :type="databaseResult.success ? 'success' : 'error'"
              :closable="false"
              show-icon
              :title="databaseResult.success ? '数据库初始化完成' : '数据库初始化未完全成功'"
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
              <el-button @click="handleInstallDatabase">重新执行</el-button>
              <!-- 失败处理：清掉残留的完成标记/锁，从头重走安装 -->
              <el-button type="warning" plain :loading="resetting" @click="handleResetInstall">
                重置安装状态
              </el-button>
              <el-button
                v-if="databaseResult.success"
                type="primary"
                @click="currentStep = 3"
              >
                下一步
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </section>

        <!-- 步骤 4：站点配置 -->
        <section v-show="currentStep === 3" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><Setting /></el-icon>
            <div>
              <h2>站点配置</h2>
              <p class="muted">设置初始站点开关（安装后可在管理后台随时调整）。不确定时可直接跳过，保持系统默认。</p>
            </div>
          </div>

          <el-form label-position="top" class="site-form">
            <el-form-item label="开放邮箱注册">
              <el-switch v-model="siteForm.registrationEnabled" />
              <span class="form-hint">关闭时仅管理员可创建账号（默认关闭）</span>
            </el-form-item>
            <el-form-item label="注册邮箱验证">
              <el-switch v-model="siteForm.registrationEmailVerifyRequired" />
              <span class="form-hint">开启后注册需输入邮箱验证码激活；需先在管理后台配置 SMTP（默认关闭）</span>
            </el-form-item>
            <el-form-item label="登录二步验证（TOTP）">
              <el-switch v-model="siteForm.loginTotpRequired" />
              <span class="form-hint">开启后所有用户登录需绑定并输入动态口令（默认关闭）</span>
            </el-form-item>
            <el-form-item label="允许访客上传">
              <el-switch v-model="siteForm.guestUploadEnabled" />
              <span class="form-hint">未登录访客可在配额内上传（默认关闭）</span>
            </el-form-item>
            <el-form-item label="单文件上传上限（MB）">
              <el-input-number v-model="siteForm.maxUploadMb" :min="1" :max="512" />
              <span class="form-hint">默认 20 MB</span>
            </el-form-item>
            <el-form-item label="资源加速域名（可选）">
              <el-input v-model="siteForm.assetDomain" placeholder="如 https://cdn.example.com，留空使用本站域名" />
            </el-form-item>
          </el-form>

          <div class="actions">
            <el-button @click="skipSiteConfig">跳过此步</el-button>
            <el-button type="primary" :loading="savingSite" @click="handleSaveSiteConfig">
              保存并下一步
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </section>

        <!-- 步骤 5：创建管理员 -->
        <section v-show="currentStep === 4" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><User /></el-icon>
            <div>
              <h2>创建管理员</h2>
              <p class="muted">这是系统的初始管理员账号（ADMIN 角色，上传配额不限）。</p>
            </div>
          </div>

          <div v-if="!adminResult" class="step-body">
            <el-alert type="warning" :closable="false" show-icon title="这是最高权限管理员账号">
              <div class="muted">
                该账号拥有站点全部权限。凭据仅此一次展示，<strong>请务必妥善保存</strong>：
                密码使用「生成强密码」后仅明文显示一次，创建成功后系统任何页面都不再回显。
              </div>
            </el-alert>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="admin-form">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="3-32 位字母、数字、下划线、点、短横线" maxlength="32" />
              </el-form-item>
              <el-form-item label="邮箱（必填）" prop="email">
                <el-input v-model="form.email" placeholder="用于找回密码与站内通知" maxlength="180">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <div class="password-field">
                  <el-input
                    v-model="form.password"
                    type="password"
                    show-password
                    placeholder="至少 8 位且含字母和数字，或点击右侧生成"
                  />
                  <el-button :icon="Key" @click="generateStrongPassword">生成强密码</el-button>
                </div>
              </el-form-item>
              <div v-if="generatedPassword" class="pw-panel">
                <div class="pw-title">
                  <el-icon><Key /></el-icon>
                  已生成 16 位强密码（含大小写、数字、符号）——明文仅此一次展示，请立即保存
                </div>
                <div class="pw-row">
                  <code class="pw-value">{{ generatedPassword }}</code>
                  <el-button size="small" :icon="CopyDocument" @click="copyGeneratedPassword">复制</el-button>
                </div>
                <div class="muted">请在下方「确认密码」中再次输入相同密码以确认你已保存；创建成功后系统不再回显。</div>
              </div>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="再次输入相同密码"
                  :prefix-icon="Lock"
                />
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
                绑定邮箱 {{ adminResult.email }}。密码不回显：请使用刚才保存的密码登录，稍后可在个人中心修改。
              </div>
            </el-alert>
            <div class="actions">
              <el-button type="primary" @click="currentStep = 5">
                下一步
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </section>

        <!-- 步骤 6：完成（汇总页） -->
        <section v-show="currentStep === 5" class="install-card">
          <div class="card-head">
            <el-icon class="card-icon"><SuccessFilled /></el-icon>
            <div>
              <h2>完成安装</h2>
              <p class="muted">写入完成标记与防重装锁并收尾，之后即可正常访问站点。</p>
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
                安装后再次访问 <code>/install</code> 将被引导离开，写接口也会被拒绝（防重装锁：
                DB 完成标记 + <code>storage/install.lock</code> 文件 + API 403 + 前端跳转），
                如需重建请清空数据库并删除锁文件后重新部署。
              </div>
            </el-alert>

            <!-- 完成汇总 -->
            <div class="summary-grid">
              <div class="summary-item">
                <div class="summary-label"><el-icon><Link /></el-icon> 站点地址</div>
                <div class="summary-value"><code>{{ siteUrl }}</code></div>
              </div>
              <div class="summary-item">
                <div class="summary-label"><el-icon><User /></el-icon> 管理员账号</div>
                <div class="summary-value">
                  {{ finishSummary?.adminUsername || adminResult?.username || '（见上方记录）' }}
                  <span v-if="finishSummary?.adminEmail" class="muted">（{{ finishSummary.adminEmail }}）</span>
                  <div class="muted">密码不显示：请使用安装时保存的密码登录</div>
                </div>
              </div>
              <div class="summary-item">
                <div class="summary-label"><el-icon><Message /></el-icon> 注册邮箱验证</div>
                <div class="summary-value">
                  <el-tag :type="finishSummary?.emailVerifyRequired ? 'success' : 'info'" size="small">
                    {{ finishSummary?.emailVerifyRequired ? '已开启' : '未开启' }}
                  </el-tag>
                  <span v-if="finishSummary?.emailVerifyRequired && !finishSummary?.smtpConfigured" class="muted warn-text">
                    （SMTP 未配置，请在管理后台配置后验证码才可发送）
                  </span>
                </div>
              </div>
              <div class="summary-item">
                <div class="summary-label"><el-icon><Lock /></el-icon> 登录二步验证（TOTP）</div>
                <div class="summary-value">
                  <el-tag :type="finishSummary?.totpRequired ? 'success' : 'info'" size="small">
                    {{ finishSummary?.totpRequired ? '已开启' : '未开启' }}
                  </el-tag>
                </div>
              </div>
            </div>

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
              <el-button type="primary" size="large" @click="goLogin">
                <el-icon class="btn-icon"><HomeFilled /></el-icon>
                前往登录
              </el-button>
            </div>
          </div>

          <div v-else class="step-body center">
            <el-alert type="error" :closable="false" show-icon title="完成安装失败">
              <div class="muted">请检查数据库连接后重试；若状态残留导致无法继续，可重置安装状态后重走向导。</div>
            </el-alert>
            <div class="actions">
              <el-button type="primary" :loading="finishing" @click="goFinish">重试完成安装</el-button>
              <el-button type="warning" plain :loading="resetting" @click="handleResetInstall">重置安装状态</el-button>
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

.db-location {
  margin-bottom: 4px;
}

.db-form {
  max-width: 720px;
}

.db-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.db-grid .db-password {
  grid-column: span 2;
}

.port-input {
  width: 100%;
}

.db-runtime-warn {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  align-items: flex-start;
  color: #b45309;
}

.db-runtime-warn .el-icon {
  margin-top: 3px;
}

.db-create-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-form {
  max-width: 460px;
}

.password-field {
  display: flex;
  gap: 10px;
  width: 100%;
}

.password-field .el-input {
  flex: 1;
}

.pw-panel {
  border: 1px dashed var(--color-brand-primary, #4ecdc4);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(78, 205, 196, 0.06);
}

.pw-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.pw-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pw-value {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  word-break: break-all;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  padding: 12px 14px;
  background: var(--color-bg-secondary);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.summary-value {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.warn-text {
  color: #b45309;
}

.site-form {
  max-width: 460px;
}

.form-hint {
  margin-left: 10px;
  color: var(--text-muted);
  font-size: 12px;
}

.db-status {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;
  border-radius: 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.actions--start {
  justify-content: flex-start;
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

  .db-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .db-grid .db-password {
    grid-column: span 1;
  }

  .actions {
    flex-direction: column-reverse;
  }

  .actions .el-button {
    width: 100%;
  }
}
</style>
