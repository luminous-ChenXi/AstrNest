<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Message, Lock, Connection, RefreshLeft } from '@element-plus/icons-vue'
import {
  fetchMailConfig,
  updateMailConfig,
  testMailConfig,
} from '../../services/chenxi'
import { fetchSecuritySettings, updateSecuritySettings } from '../../services/system'
import { fetchAdminUsers, resetUserTotp } from '../../services/adminUsers'

// ==================== 站长开关 ====================

const settingsLoading = ref(false)
const savingSettings = ref(false)
const settings = reactive({
  emailVerifyRequired: false,
  totpRequired: false,
  smtpConfigured: false,
})

const loadSettings = async () => {
  settingsLoading.value = true
  try {
    const { data } = await fetchSecuritySettings()
    settings.emailVerifyRequired = Boolean(data?.emailVerifyRequired)
    settings.totpRequired = Boolean(data?.totpRequired)
    settings.smtpConfigured = Boolean(data?.smtpConfigured)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载安全设置失败')
  } finally {
    settingsLoading.value = false
  }
}

const saveSettings = async () => {
  if (settings.emailVerifyRequired && !settings.smtpConfigured) {
    ElMessage.warning('开启注册邮箱验证前，请先在下方配置并启用 SMTP，否则验证码无法发送')
    return
  }
  savingSettings.value = true
  try {
    const { data } = await updateSecuritySettings({
      emailVerifyRequired: settings.emailVerifyRequired,
      totpRequired: settings.totpRequired,
    })
    settings.emailVerifyRequired = Boolean(data?.emailVerifyRequired)
    settings.totpRequired = Boolean(data?.totpRequired)
    settings.smtpConfigured = Boolean(data?.smtpConfigured)
    ElMessage.success('安全开关已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    savingSettings.value = false
  }
}

// ==================== SMTP 配置 ====================

const smtpFormRef = ref()
const smtpLoading = ref(false)
const smtpSaving = ref(false)
const smtpTesting = ref(false)
const testEmail = ref('')
const smtpForm = reactive({
  smtpHost: '',
  smtpPort: 465,
  smtpUsername: '',
  smtpPassword: '',
  secureType: 'ssl',
  fromEmail: '',
  fromName: 'AstrNest',
  enabled: false,
})

const smtpRules = {
  smtpHost: [{ required: true, message: '请输入 SMTP 服务器', trigger: 'blur' }],
  smtpPort: [{ required: true, message: '请输入端口号', trigger: 'change' }],
  smtpUsername: [{ required: true, message: '请输入邮箱账号', trigger: 'blur' }],
  fromEmail: [{ required: true, message: '请输入发件人邮箱', trigger: 'blur' }],
  fromName: [{ required: true, message: '请输入发件人名称', trigger: 'blur' }],
}

const loadSmtp = async () => {
  smtpLoading.value = true
  try {
    const { data } = await fetchMailConfig()
    if (data) {
      // password 脱敏：后端不回传明文，留空表示「不修改」
      Object.assign(smtpForm, { ...data, smtpPassword: '' })
    }
  } catch (error) {
    ElMessage.error('加载 SMTP 配置失败')
  } finally {
    smtpLoading.value = false
  }
}

const saveSmtp = async () => {
  try {
    if (smtpFormRef.value) {
      await smtpFormRef.value.validate()
    }
    smtpSaving.value = true
    await updateMailConfig({ ...smtpForm })
    ElMessage.success('SMTP 配置已保存')
    await Promise.all([loadSmtp(), loadSettings()])
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    smtpSaving.value = false
  }
}

const handleTestSmtp = async () => {
  if (!testEmail.value) {
    ElMessage.warning('请输入测试收件邮箱地址')
    return
  }
  smtpTesting.value = true
  try {
    await testMailConfig(testEmail.value)
    ElMessage.success('测试邮件已发送，请查收')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '测试发送失败')
  } finally {
    smtpTesting.value = false
  }
}

// ==================== 2FA 重置 ====================

const users = ref([])
const usersLoading = ref(false)
const resettingId = ref(null)

const boundUsers = computed(() => users.value.filter((user) => user.totpBound))

const loadUsers = async () => {
  usersLoading.value = true
  try {
    const { data } = await fetchAdminUsers()
    users.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    usersLoading.value = false
  }
}

const handleResetTotp = (user) => {
  ElMessageBox.confirm(
    `确定重置用户「${user.displayName || user.username}」的二步验证（TOTP）绑定吗？其还原码将全部作废，下次登录需重新绑定。`,
    '重置二步验证',
    { confirmButtonText: '重置', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    resettingId.value = user.id
    try {
      await resetUserTotp(user.id)
      ElMessage.success('已重置该用户的二步验证绑定')
      await loadUsers()
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '重置失败')
    } finally {
      resettingId.value = null
    }
  }).catch(() => {})
}

onMounted(() => {
  loadSettings()
  loadSmtp()
  loadUsers()
})
</script>

<template>
  <div class="security-settings">
    <header class="page-head">
      <h2>注册与登录安全</h2>
      <p class="muted">站长级安全开关：控制注册是否需要邮箱验证、登录是否强制二步验证（TOTP），并在此维护 SMTP 发件配置。</p>
    </header>

    <!-- 站长开关 -->
    <section class="settings-card">
      <div class="card-title">
        <el-icon><Lock /></el-icon>
        <h3>站点安全开关</h3>
      </div>
      <div v-loading="settingsLoading" class="switch-list">
        <div class="switch-item">
          <div class="switch-info">
            <div class="switch-name">
              注册邮箱验证
              <el-tag :type="settings.emailVerifyRequired ? 'success' : 'info'" size="small">
                {{ settings.emailVerifyRequired ? '已开启' : '未开启' }}
              </el-tag>
            </div>
            <p class="muted">
              开启后，新用户注册必须输入邮箱验证码（或点击邮件中的验证链接）完成验证后才激活账号；关闭时注册即激活。
            </p>
            <el-alert
              v-if="!settings.smtpConfigured"
              class="inline-alert"
              type="warning"
              :closable="false"
              show-icon
              title="开启前需先配置 SMTP"
            >
              <div class="muted">当前尚未配置可用的 SMTP 发件服务，开启后验证码将无法发送。请在下方「SMTP 邮件服务」完成配置并启用。</div>
            </el-alert>
          </div>
          <el-switch v-model="settings.emailVerifyRequired" />
        </div>

        <el-divider />

        <div class="switch-item">
          <div class="switch-info">
            <div class="switch-name">
              登录二步验证（TOTP）
              <el-tag :type="settings.totpRequired ? 'success' : 'info'" size="small">
                {{ settings.totpRequired ? '已开启' : '未开启' }}
              </el-tag>
            </div>
            <p class="muted">
              开启后，所有用户登录在密码校验通过后需输入动态口令：未绑定用户进入强制绑定（扫码 + 一次性还原码），已绑定用户输入 6 位动态码。
            </p>
          </div>
          <el-switch v-model="settings.totpRequired" />
        </div>

        <div class="card-actions">
          <el-button type="primary" :loading="savingSettings" @click="saveSettings">保存开关</el-button>
        </div>
      </div>
    </section>

    <!-- SMTP 配置 -->
    <section class="settings-card">
      <div class="card-title">
        <el-icon><Connection /></el-icon>
        <h3>SMTP 邮件服务</h3>
        <el-tag :type="settings.smtpConfigured ? 'success' : 'warning'" size="small" class="title-tag">
          {{ settings.smtpConfigured ? '已就绪' : '未配置' }}
        </el-tag>
      </div>

      <el-form
        ref="smtpFormRef"
        v-loading="smtpLoading"
        :model="smtpForm"
        :rules="smtpRules"
        label-position="top"
        class="smtp-form"
      >
        <div class="smtp-grid">
          <el-form-item label="SMTP 服务器" prop="smtpHost">
            <el-input v-model="smtpForm.smtpHost" placeholder="如 smtp.qq.com" />
          </el-form-item>
          <el-form-item label="端口" prop="smtpPort">
            <el-input-number v-model="smtpForm.smtpPort" :min="1" :max="65535" :controls="false" class="port-input" />
          </el-form-item>
          <el-form-item label="加密方式" prop="secureType">
            <el-select v-model="smtpForm.secureType">
              <el-option label="SSL" value="ssl" />
              <el-option label="TLS (STARTTLS)" value="tls" />
              <el-option label="不加密" value="none" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮箱账号" prop="smtpUsername">
            <el-input v-model="smtpForm.smtpUsername" placeholder="发件邮箱账号" autocomplete="off" />
          </el-form-item>
          <el-form-item label="授权码 / 密码" prop="smtpPassword">
            <el-input
              v-model="smtpForm.smtpPassword"
              type="password"
              show-password
                placeholder="留空表示保持现有授权码不变"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="发件人邮箱" prop="fromEmail">
            <el-input v-model="smtpForm.fromEmail" placeholder="与邮箱账号一致" />
          </el-form-item>
          <el-form-item label="发件人名称" prop="fromName">
            <el-input v-model="smtpForm.fromName" placeholder="如 AstrNest" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="smtpForm.enabled" />
            <span class="form-hint">关闭时邮件功能整体停用</span>
          </el-form-item>
        </div>
      </el-form>

      <div class="smtp-test">
        <el-input v-model="testEmail" placeholder="测试收件邮箱地址" class="test-input">
          <template #prefix><el-icon><Message /></el-icon></template>
        </el-input>
        <el-button :loading="smtpTesting" @click="handleTestSmtp">测试发送</el-button>
      </div>

      <div class="card-actions">
        <el-button type="primary" :loading="smtpSaving" @click="saveSmtp">保存 SMTP 配置</el-button>
      </div>
    </section>

    <!-- 2FA 重置 -->
    <section class="settings-card">
      <div class="card-title">
        <el-icon><RefreshLeft /></el-icon>
        <h3>二步验证重置</h3>
      </div>
      <p class="muted">
        用户丢失验证器或还原码时，管理员可重置其 TOTP 绑定：还原码全部作废，
        该用户下次登录（登录二步验证开启时）将重新进入扫码绑定流程。
      </p>
      <el-table v-loading="usersLoading" :data="boundUsers" class="totp-table" empty-text="当前没有用户绑定二步验证">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="displayName" label="昵称" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="warning"
              plain
              :loading="resettingId === row.id"
              @click="handleResetTotp(row)"
            >
              重置 2FA
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.security-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 960px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.muted {
  margin: 0;
  color: var(--text-muted, #8a94a6);
  font-size: 13px;
  line-height: 1.7;
}

.settings-card {
  background: var(--color-bg-primary, #fff);
  border: 1px solid var(--border-soft, #e8ecf2);
  border-radius: 14px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title h3 {
  margin: 0;
  font-size: 16px;
}

.title-tag {
  margin-left: 4px;
}

.switch-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.switch-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.switch-info {
  flex: 1;
}

.switch-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.inline-alert {
  margin-top: 10px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}

.smtp-form {
  max-width: 100%;
}

.smtp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
}

.port-input {
  width: 100%;
}

.form-hint {
  margin-left: 10px;
  color: var(--text-muted, #8a94a6);
  font-size: 12px;
}

.smtp-test {
  display: flex;
  gap: 12px;
  max-width: 460px;
}

.smtp-test .test-input {
  flex: 1;
}

.totp-table {
  width: 100%;
}

@media (max-width: 768px) {
  .smtp-grid {
    grid-template-columns: 1fr;
  }

  .switch-item {
    flex-direction: column;
  }
}
</style>
