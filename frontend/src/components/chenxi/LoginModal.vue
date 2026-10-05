<script setup>
import { ElMessage } from 'element-plus'
import { Lock, User, Close } from '@element-plus/icons-vue'
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { login, verifyTwoFactor, confirmTwoFactorSetup } from '../../services/auth'
import { buildAuthorizeUrl, createPkce, fetchSsoConfig, randomToken, SSO_SESSION_KEYS } from '../../services/sso'
import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'login-success', 'closed'])

const router = useRouter()
const auth = useAuthStore()

// SSO 按钮文案：默认中性表述；如需突出身份源品牌，可改为「使用辰汐通行证登录」等
const SSO_BUTTON_TEXT = '使用 SSO 登录'

const formRef = ref()
const submitting = ref(false)
const ssoLoading = ref(false)
// 仅在 astrnest.sso.enabled=true 时后端返回 enabled=true，前端才显示入口
const ssoEnabled = ref(false)
const ssoConfig = ref(null)
const form = reactive({
  username: '',
  password: '',
})

// 登录阶段机：credentials → 密码登录；totp-challenge → 已绑定用户输码；totp-setup → 强制绑定（扫码）
const stage = ref('credentials')
const twoFactor = reactive({
  tempToken: '',
  otpauthUri: '',
  secret: '',
  username: '',
})
const totpCode = ref('')
const totpSubmitting = ref(false)
const qrDataUrl = ref('')
// 一次性还原码（仅绑定成功后展示一次）
const recoveryCodes = ref([])
const recoveryCopied = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const toast = (type, message) =>
  ElMessage({
    type,
    message,
    offset: 16,
    showClose: true,
    grouping: true,
    zIndex: 11000,
  })

const handleClose = () => {
  emit('update:visible', false)
}

const goToRegister = () => {
  // 直接跳转，不关闭弹窗（页面切换后弹窗会自动消失）
  router.push('/register')
}

// SSO 登录：弹窗首次打开时懒加载配置（失败静默，不打扰本地登录）
const ensureSsoConfig = async () => {
  if (ssoConfig.value) return
  try {
    const { data } = await fetchSsoConfig()
    if (data?.enabled) {
      ssoConfig.value = data
      ssoEnabled.value = true
    }
  } catch {
    /* SSO 配置不可用时不影响本地登录 */
  }
}

// 发起 SSO 登录：生成 PKCE/state/nonce 存 sessionStorage 后整页跳转身份源授权页
const handleSsoLogin = async () => {
  if (!ssoConfig.value || ssoLoading.value) return
  ssoLoading.value = true
  try {
    const { codeVerifier, codeChallenge } = await createPkce()
    const state = randomToken()
    const nonce = randomToken()
    sessionStorage.setItem(SSO_SESSION_KEYS.state, state)
    sessionStorage.setItem(SSO_SESSION_KEYS.nonce, nonce)
    sessionStorage.setItem(SSO_SESSION_KEYS.verifier, codeVerifier)
    sessionStorage.setItem(SSO_SESSION_KEYS.returnTo, router.currentRoute.value.fullPath || '/')
    window.location.href = buildAuthorizeUrl(ssoConfig.value, { state, nonce, codeChallenge })
  } catch (error) {
    ssoLoading.value = false
    toast('error', error?.message || 'SSO 登录跳转失败，请稍后重试')
  }
}

const completeLogin = (data, successText = '登录成功！') => {
  auth.setSession(data.token, data.profile, data.tokenType, data.expiresIn)
  toast('success', successText)
  emit('login-success')
  handleClose()
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const { data } = await login({ username: form.username.trim(), password: form.password })
      if (data?.mode === 'TOTP_CHALLENGE' && data.twoFactor?.tempToken) {
        // 已绑定用户：进入动态码挑战
        Object.assign(twoFactor, {
          tempToken: data.twoFactor.tempToken,
          otpauthUri: '',
          secret: '',
          username: data.twoFactor.username || form.username,
        })
        totpCode.value = ''
        stage.value = 'totp-challenge'
        return
      }
      if (data?.mode === 'TOTP_SETUP' && data.twoFactor?.tempToken) {
        // 强制绑定：渲染二维码
        Object.assign(twoFactor, {
          tempToken: data.twoFactor.tempToken,
          otpauthUri: data.twoFactor.otpauthUri || '',
          secret: data.twoFactor.secret || '',
          username: data.twoFactor.username || form.username,
        })
        totpCode.value = ''
        qrDataUrl.value = twoFactor.otpauthUri
          ? await QRCode.toDataURL(twoFactor.otpauthUri, { width: 200, margin: 1 })
          : ''
        stage.value = 'totp-setup'
        return
      }
      completeLogin(data)
    } catch (error) {
      toast('error', error.response?.data?.message || '登录失败，请检查凭证')
    } finally {
      submitting.value = false
    }
  })
}

/** 二步验证挑战：6 位动态码（或 8 位还原码）换正式 JWT */
const handleTotpVerify = async () => {
  if (!totpCode.value.trim()) {
    toast('warning', '请输入 6 位动态验证码或 8 位还原码')
    return
  }
  totpSubmitting.value = true
  try {
    const { data } = await verifyTwoFactor({
      tempToken: twoFactor.tempToken,
      code: totpCode.value.trim(),
    })
    completeLogin(data)
  } catch (error) {
    toast('error', error.response?.data?.message || '验证失败，请重试')
  } finally {
    totpSubmitting.value = false
  }
}

/** 强制绑定确认：绑定落库 + 一次性还原码展示 */
const handleTotpSetupConfirm = async () => {
  if (!/^\d{6}$/.test(totpCode.value.trim())) {
    toast('warning', '请输入验证器上的 6 位动态码')
    return
  }
  totpSubmitting.value = true
  try {
    const { data } = await confirmTwoFactorSetup({
      tempToken: twoFactor.tempToken,
      code: totpCode.value.trim(),
    })
    recoveryCodes.value = Array.isArray(data?.recoveryCodes) ? data.recoveryCodes : []
    recoveryCopied.value = false
    pendingSession.value = data
    stage.value = 'recovery-codes'
  } catch (error) {
    toast('error', error.response?.data?.message || '绑定失败，请重试')
  } finally {
    totpSubmitting.value = false
  }
}

// 绑定成功后暂存的会话：用户确认已保存还原码后再真正登录
const pendingSession = ref(null)

const copyRecoveryCodes = async () => {
  try {
    await navigator.clipboard.writeText(recoveryCodes.value.join(String.fromCharCode(10)))
    recoveryCopied.value = true
    toast('success', '还原码已复制')
  } catch (error) {
    toast('warning', '复制失败，请手动抄录')
  }
}

const finishRecoveryStep = () => {
  if (pendingSession.value) {
    completeLogin(pendingSession.value, '二步验证绑定完成，已登录！')
    pendingSession.value = null
  }
}

const backToCredentials = () => {
  stage.value = 'credentials'
  totpCode.value = ''
  qrDataUrl.value = ''
}

// ESC 键关闭
const handleKeydown = (e) => {
  if (e.key === 'Escape' && props.visible) {
    handleClose()
  }
}

// 监听 visible 变化，添加/移除键盘事件
watch(() => props.visible, (newVal, oldVal) => {
  if (newVal) {
    document.addEventListener('keydown', handleKeydown)
    // 重置表单与二步验证阶段
    form.username = ''
    form.password = ''
    stage.value = 'credentials'
    totpCode.value = ''
    qrDataUrl.value = ''
    recoveryCodes.value = []
    pendingSession.value = null
    submitting.value = false
    // 懒加载 SSO 配置，决定是否展示 SSO 登录入口
    ensureSsoConfig()
  } else {
    document.removeEventListener('keydown', handleKeydown)
    // 弹窗关闭时触发 closed 事件
    if (oldVal === true) {
      emit('closed')
    }
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-show="visible" class="login-modal-overlay" @click.self="handleClose">
        <div class="login-modal-container">
          <!-- 关闭按钮 -->
          <button class="modal-close" @click="handleClose">
            <Close class="close-icon" />
          </button>

          <div class="modal-content">
            <!-- 左侧：品牌展示 -->
            <div class="modal-brand">
              <!-- 品牌渐变背景（纯 CSS，随主题切换） -->
              <div class="brand-image-container">
                <div class="brand-aurora">
                  <div class="aurora-blob aurora-primary"></div>
                  <div class="aurora-blob aurora-accent"></div>
                  <div class="aurora-blob aurora-sky"></div>
                </div>
                <div class="brand-image-placeholder">
                  <span class="placeholder-text">AstrNest</span>
                </div>
              </div>
            </div>

            <!-- 右侧：登录表单 -->
            <div class="modal-form">
              <header class="form-header">
                <p class="form-eyebrow">{{ stage === 'credentials' ? '欢迎回来' : '二步验证' }}</p>
                <h1 class="form-title">
                  {{ stage === 'credentials' ? '登录账号' : stage === 'recovery-codes' ? '保存还原码' : '动态口令' }}
                </h1>
              </header>

              <!-- 阶段一：用户名密码（含密码通过后的 2FA 分流触发） -->
              <ElForm
                v-if="stage === 'credentials'"
                ref="formRef"
                :model="form"
                :rules="rules"
                label-position="top"
                size="large"
                class="login-form"
                @submit.prevent
              >
                <ElFormItem label="用户名 / 邮箱" prop="username">
                  <ElInput
                    v-model="form.username"
                    placeholder="请输入用户名或邮箱"
                    :prefix-icon="User"
                    autocomplete="username"
                    clearable
                  />
                </ElFormItem>
                
                <ElFormItem label="密码" prop="password">
                  <ElInput
                    v-model="form.password"
                    type="password"
                    show-password
                    autocomplete="current-password"
                    :prefix-icon="Lock"
                    placeholder="请输入密码"
                    @keyup.enter="handleSubmit"
                  />
                </ElFormItem>

                <div class="form-options">
                  <RouterLink to="/forgot-password" class="forgot-link" @click="handleClose">
                    忘记密码？
                  </RouterLink>
                </div>

                <ElButton
                  type="primary"
                  class="btn-login"
                  size="large"
                  :loading="submitting"
                  @click="handleSubmit"
                >
                  登录
                </ElButton>

                <!-- SSO 登录入口：仅当后端 astrnest.sso.enabled=true 时显示（默认关闭） -->
                <template v-if="ssoEnabled">
                  <div class="sso-divider"><span>或</span></div>
                  <ElButton
                    class="btn-sso"
                    size="large"
                    :loading="ssoLoading"
                    @click="handleSsoLogin"
                  >
                    {{ SSO_BUTTON_TEXT }}
                  </ElButton>
                </template>

                <div class="form-footer">
                  <span class="footer-text">还没有账号？</span>
                  <a href="/register" class="register-link" @click.prevent="goToRegister">
                    立即注册
                  </a>
                </div>
              </ElForm>

              <!-- 阶段二：二步验证挑战（已绑定用户：输动态码或还原码） -->
              <ElForm
                v-else-if="stage === 'totp-challenge'"
                label-position="top"
                size="large"
                class="login-form"
                @submit.prevent
              >
                <p class="twofa-hint">
                  账号 <strong>{{ twoFactor.username }}</strong> 已开启二步验证，请输入验证器上的 6 位动态码；
                  丢失验证器时可改用 8 位还原码。
                </p>
                <ElFormItem label="动态验证码">
                  <ElInput
                    v-model="totpCode"
                    placeholder="6 位动态码或 8 位还原码"
                    maxlength="8"
                    :prefix-icon="Lock"
                    @keyup.enter="handleTotpVerify"
                  />
                </ElFormItem>
                <ElButton
                  type="primary"
                  class="btn-login"
                  size="large"
                  :loading="totpSubmitting"
                  @click="handleTotpVerify"
                >
                  验证并登录
                </ElButton>
                <ElButton text size="large" class="btn-back" @click="backToCredentials">返回重新登录</ElButton>
              </ElForm>

              <!-- 阶段三：强制绑定（扫码 + 确认） -->
              <ElForm
                v-else-if="stage === 'totp-setup'"
                label-position="top"
                size="large"
                class="login-form"
                @submit.prevent
              >
                <p class="twofa-hint">
                  本站已开启强制二步验证。请使用 Google Authenticator 等验证器扫描二维码，
                  然后输入 6 位动态码完成绑定。无法扫码时可手工录入密钥：
                </p>
                <div class="qr-box">
                  <img v-if="qrDataUrl" :src="qrDataUrl" alt="TOTP 二维码" class="qr-img" />
                  <div v-else class="qr-fallback">二维码生成失败，请手工录入密钥</div>
                </div>
                <code class="secret-value">{{ twoFactor.secret }}</code>
                <ElFormItem label="动态验证码" class="confirm-code">
                  <ElInput
                    v-model="totpCode"
                    placeholder="输入 6 位动态码确认绑定"
                    maxlength="6"
                    :prefix-icon="Lock"
                    @keyup.enter="handleTotpSetupConfirm"
                  />
                </ElFormItem>
                <ElButton
                  type="primary"
                  class="btn-login"
                  size="large"
                  :loading="totpSubmitting"
                  @click="handleTotpSetupConfirm"
                >
                  确认绑定
                </ElButton>
                <ElButton text size="large" class="btn-back" @click="backToCredentials">返回重新登录</ElButton>
              </ElForm>

              <!-- 阶段四：一次性还原码展示 -->
              <div v-else class="login-form recovery-box">
                <p class="twofa-hint">
                  绑定成功！以下是 <strong>10 个一次性还原码</strong>，用于丢失验证器时登录。
                  <strong>仅此一次展示</strong>，请立即复制或抄录并妥善保存。
                </p>
                <div class="recovery-grid">
                  <code v-for="code in recoveryCodes" :key="code" class="recovery-code">{{ code }}</code>
                </div>
                <div class="recovery-actions">
                  <el-button size="large" @click="copyRecoveryCodes">
                    {{ recoveryCopied ? '已复制' : '复制全部' }}
                  </el-button>
                  <el-button type="primary" size="large" @click="finishRecoveryStep">我已保存，进入站点</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 二步验证相关 */
.twofa-hint {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-secondary, #64748b);
}

.qr-box {
  display: flex;
  justify-content: center;
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--border-soft, #e8ecf2);
  border-radius: 12px;
  background: #fff;
}

.qr-img {
  width: 200px;
  height: 200px;
}

.qr-fallback {
  display: flex;
  align-items: center;
  height: 200px;
  color: var(--color-text-secondary, #64748b);
  font-size: 13px;
}

.secret-value {
  display: block;
  margin: 0 0 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--color-bg-secondary, #f5f7fa);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 14px;
  letter-spacing: 1px;
  word-break: break-all;
  user-select: all;
}

.confirm-code {
  margin-top: 4px;
}

.btn-back {
  margin-top: 8px;
  width: 100%;
}

.recovery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.recovery-code {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--color-bg-secondary, #f5f7fa);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  text-align: center;
}

.recovery-actions {
  display: flex;
  gap: 10px;
}

.recovery-actions .el-button {
  flex: 1;
}

/* 遮罩层 - 亚克力质感 */
.login-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 弹窗容器 */
.login-modal-container {
  position: relative;
  width: 100%;
  max-width: 800px;
  background: white;
  border-radius: 24px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

/* 关闭按钮 */
.modal-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #333;
}

.close-icon {
  width: 18px;
  height: 18px;
}

/* 内容布局 */
.modal-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 500px;
}

/* 左侧品牌区 */
.modal-brand {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #FADCE9 0%, #F9A8C8 50%, #E87A9F 100%);
}

/* 背景图片容器 */
.brand-image-container {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.brand-background-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.brand-aurora {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #FADCE9 0%, #F9A8C8 50%, #E87A9F 100%);
}

.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(48px);
  opacity: 0.55;
}

.aurora-primary {
  width: 70%;
  aspect-ratio: 1;
  top: -20%;
  left: -15%;
  background: var(--halo-primary);
}

.aurora-accent {
  width: 60%;
  aspect-ratio: 1;
  bottom: -18%;
  right: -12%;
  background: var(--halo-secondary);
}

.aurora-sky {
  width: 50%;
  aspect-ratio: 1;
  top: 34%;
  left: 36%;
  background: radial-gradient(circle at 50% 50%, rgba(135, 206, 235, 0.3), transparent 65%);
}

.brand-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #FADCE9 0%, #F9A8C8 50%, #E87A9F 100%);
}

.placeholder-text {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  opacity: 0.8;
}

/* 右侧表单区 */
.modal-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 3rem 2.5rem;
  background: white;
}

.form-header {
  text-align: center;
  margin-bottom: 2rem;
}

.form-eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #F9A8C8;
  margin: 0 0 0.5rem;
}

.form-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

/* 表单样式 */
.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #4a4a5a;
  padding-bottom: 0.5rem;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e5e7eb;
  transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #F9A8C8;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #F9A8C8;
}

.form-options {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1.5rem;
}

.forgot-link {
  font-size: 0.875rem;
  color: #F9A8C8;
  text-decoration: none;
  transition: color 0.2s ease;
}

.forgot-link:hover {
  color: #E87A9F;
}

/* 登录按钮 */
.btn-login {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  background: #F9A8C8;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(249, 168, 200, 0.35);
}

.btn-login:hover {
  background: #EC8DAD;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(249, 168, 200, 0.45);
}

/* SSO 登录分隔线与按钮 */
.sso-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1.25rem 0;
  color: #9ca3af;
  font-size: 0.8rem;
}

.sso-divider::before,
.sso-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eceef3;
}

.sso-divider span {
  white-space: nowrap;
}

.btn-sso {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #4a4a5a;
  background: #fff;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.btn-sso:hover {
  color: #E87A9F;
  border-color: #F9A8C8;
  background: #fff7fa;
}

.dark .sso-divider::before,
.dark .sso-divider::after {
  background: #2a2a3a;
}

.dark .btn-sso {
  color: #c0c0d0;
  background: #252538;
  border-color: #3a3a4a;
}

.dark .btn-sso:hover {
  color: #F9A8C8;
  border-color: #F9A8C8;
  background: #2c2c42;
}

/* 底部链接 */
.form-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #f0f0f5;
}

.footer-text {
  font-size: 0.875rem;
  color: #6b7280;
}

.register-link {
  font-size: 0.875rem;
  font-weight: 600;
  color: #F9A8C8;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s ease;
}

.register-link:hover {
  color: #E87A9F;
}

/* 动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .login-modal-container,
.modal-fade-leave-to .login-modal-container {
  transform: scale(0.95) translateY(10px);
}

/* 深色主题 */
.dark .login-modal-overlay {
  background: rgba(0, 0, 0, 0.6);
}

.dark .login-modal-container {
  background: #1a1a2e;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.05);
}

.dark .modal-close {
  background: rgba(255, 255, 255, 0.1);
  color: #999;
}

.dark .modal-close:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.dark .modal-form {
  background: #1a1a2e;
}

.dark .form-title {
  color: #fff;
}

.dark .login-form :deep(.el-form-item__label) {
  color: #a0a0b0;
}

.dark .login-form :deep(.el-input__wrapper) {
  background: #252538;
  box-shadow: 0 0 0 1px #3a3a4a;
}

.dark .login-form :deep(.el-input__inner) {
  color: #fff;
}

.dark .form-footer {
  border-top-color: #2a2a3a;
}

.dark .footer-text {
  color: #808090;
}

/* 响应式 */
@media (max-width: 640px) {
  .modal-content {
    grid-template-columns: 1fr;
  }
  
  .modal-brand {
    display: none;
  }
  
  .modal-form {
    padding: 2rem 1.5rem;
  }
}
</style>
