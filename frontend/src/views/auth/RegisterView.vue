
<script setup>
import { ElMessage, ElSteps, ElStep, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus'
import { User, Message as MailIcon, Lock } from '@element-plus/icons-vue'
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { debounce } from 'lodash-es'
import ChenxiCaptchaInput from '../../components/chenxi/ChenxiCaptchaInput.vue'
import { requestRegisterCode, verifyRegisterCode, registerChenxiAccount, checkEmailAvailability } from '../../services/chenxi'
import { useChenxiEmailCode } from '../../composables/useChenxiEmailCode'

const backgroundUrl = ref('')
const backgroundModules = import.meta.glob('../../assets/img/backgroud/*', {
  eager: true,
  import: 'default',
})
const backgroundPool = Object.values(backgroundModules).filter(Boolean)
const fallbackBackground =
  'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1600&q=80'

const resolvedBackground = computed(() => backgroundUrl.value || fallbackBackground)
const MAX_PARALLAX_OFFSET = 36
const parallaxOffset = ref({ x: 0, y: 0 })
const backgroundTransformStyle = computed(() => ({
  transform: `translate3d(${parallaxOffset.value.x}px, ${parallaxOffset.value.y}px, 0) scale(1.02)`,
}))

const pickBackground = () => {
  if (!backgroundPool.length) return fallbackBackground
  const randomIndex = Math.floor(Math.random() * backgroundPool.length)
  return backgroundPool[randomIndex]
}

onMounted(() => {
  backgroundUrl.value = pickBackground()
})

const handleMouseMove = (event) => {
  if (typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(pointer: coarse)').matches) {
    parallaxOffset.value = { x: 0, y: 0 }
    return
  }
  const rect = event.currentTarget?.getBoundingClientRect?.()
  if (!rect) return
  const relativeX = (event.clientX - rect.left) / rect.width - 0.5
  const relativeY = (event.clientY - rect.top) / rect.height - 0.5
  parallaxOffset.value = {
    x: -relativeX * MAX_PARALLAX_OFFSET,
    y: -relativeY * MAX_PARALLAX_OFFSET,
  }
}

const resetParallax = () => {
  parallaxOffset.value = { x: 0, y: 0 }
}

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const emailAvailable = ref(true)

const form = reactive({
  email: '',
  captchaToken: '',
  code: '',
  username: '',
  password: '',
  displayName: '',
})

const currentStep = ref(0)
const canProceedToAccountStep = computed(
  () => Boolean(form.email && form.captchaToken && form.code.length === 6 && emailAvailable.value)
)

const checkEmail = debounce(async (email) => {
  if (!email || !/.+@.+\..+/.test(email)) {
    emailAvailable.value = true
    return
  }
  try {
    const { data } = await checkEmailAvailability(email)
    emailAvailable.value = data.available
    if (!data.available) {
      formRef.value?.validateField('email')
    }
  } catch (error) {
    emailAvailable.value = true
  }
}, 500)

watch(() => form.email, (newEmail) => {
  checkEmail(newEmail)
})

const validateFields = async (fields) => {
  if (!formRef.value) return true
  try {
    await Promise.all(fields.map((field) => formRef.value.validateField(field)))
    return true
  } catch (error) {
    return false
  }
}

const validateEmail = (_rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入邮箱地址'))
  }
  if (!emailAvailable.value) {
    return callback(new Error('该邮箱已被注册'))
  }
  callback()
}

const usernamePattern = /^[A-Za-z0-9_.-]{1,20}$/
const validateUsername = (_rule, value, callback) => {
  if (!value) return callback(new Error('请输入用户名'))
  const trimmed = value.trim()
  if (trimmed.length === 0) return callback(new Error('请输入用户名'))
  if (trimmed.length > 20) return callback(new Error('用户名需在 20 个字符以内'))
  if (!usernamePattern.test(trimmed)) return callback(new Error('仅允许字母、数字、下划线、点、短横线'))
  callback()
}

const rules = {
  email: [
    { validator: validateEmail, trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  username: [{ validator: validateUsername, trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请设置登录密码', trigger: 'blur' }],
}

const { buttonLabel, canSend, sendCode } = useChenxiEmailCode((payload) => requestRegisterCode(payload))

const handleSendCode = async () => {
  const emailField = await formRef.value?.validateField('email').catch(() => null)
  if (!emailField) return

  if (!form.captchaToken) {
    ElMessage.warning('请先完成人机验证')
    return
  }
  await sendCode({ email: form.email, captchaToken: form.captchaToken })
}

const handleNextStep = async () => {
  const verified = await validateFields(['email', 'code'])
  if (!verified) {
    return
  }
  if (!form.captchaToken) {
    ElMessage.warning('请先完成人机验证')
    return
  }
  if (!canProceedToAccountStep.value) {
    ElMessage.warning('请填写完整的邮箱与验证码信息')
    return
  }
  // 验证验证码是否正确
  try {
    await verifyRegisterCode({
      email: form.email,
      code: form.code,
      scene: 'REGISTER'
    })
    currentStep.value = 1
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验证码验证失败')
  }
}

const handlePrevStep = () => {
  currentStep.value = 0
}

const handleSubmit = async () => {
  if (currentStep.value === 0) {
    await handleNextStep()
    return
  }
  const accountValid = await validateFields(['username', 'password'])
  if (!accountValid) {
    return
  }
  submitting.value = true
  try {
    await registerChenxiAccount({
      email: form.email,
      code: form.code,
      username: form.username,
      password: form.password,
      displayName: form.displayName,
    })
    router.push({ name: 'register-success' })
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div
    class="register-layout"
    @mousemove="handleMouseMove"
    @mouseleave="resetParallax"
  >
    <div class="background-layer" aria-hidden="true">
      <img
        :src="resolvedBackground"
        alt="AstrNest background"
        class="background-image"
        :style="backgroundTransformStyle"
      />
    </div>
    <div class="background-overlay"></div>
    <div class="auth-panel">
      <header class="panel-head">
        <p class="eyebrow">现在加入</p>
        <h1>创建您的账户</h1>
      </header>

      <ElSteps :active="currentStep" finish-status="success" align-center class="register-steps">
        <ElStep title="邮箱验证" />
        <ElStep title="设置账户" />
      </ElSteps>

      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="auth-form"
        @submit.prevent
      >
        <Transition name="chenxi-fade" mode="out-in">
          <section
            v-if="currentStep === 0"
            key="verification"
            class="form-stage"
          >
            <div class="stage-header">
              <p class="stage-label">STEP 1</p>
              <h2>完成邮箱验证</h2>
              <p>为保障账号安全，请填入准确的邮箱</p>
            </div>
            <div class="form-grid">
              <ElFormItem label="邮箱" prop="email" class="full-width">
                <ElInput
                  v-model="form.email"
                  placeholder="请输入常用邮箱"
                  :prefix-icon="MailIcon"
                  autocomplete="email"
                />
              </ElFormItem>
              <div class="full-width captcha-field">
                <ChenxiCaptchaInput v-model="form.captchaToken" />
              </div>
              <ElFormItem label="邮箱验证码" prop="code" class="full-width">
                <div class="inline-field">
                  <ElInput v-model="form.code" maxlength="6" placeholder="6 位验证码" class="flex-1" />
                  <ElButton
                    type="primary"
                    plain
                    class="send-code"
                    :disabled="!canSend"
                    @click="handleSendCode"
                  >
                    {{ buttonLabel }}
                  </ElButton>
                </div>
              </ElFormItem>
            </div>
          </section>

          <section
            v-else
            key="account"
            class="form-stage"
          >
            <div class="stage-header">
              <p class="stage-label">STEP 2</p>
              <h2>设置账户信息</h2>
              <p>配置用户名、昵称与密码，正式启用 AstrNest 账户。</p>
            </div>
            <div class="form-grid two-cols">
              <ElFormItem label="用户名" prop="username">
                <ElInput v-model="form.username" :prefix-icon="User" placeholder="用于登录与识别" />
              </ElFormItem>
              <ElFormItem label="显示昵称">
                <ElInput v-model="form.displayName" placeholder="可选，默认为用户名" />
              </ElFormItem>
              <ElFormItem label="登录密码" prop="password" class="full-width">
                <ElInput v-model="form.password" :prefix-icon="Lock" show-password placeholder="至少 8 位" />
              </ElFormItem>
            </div>
          </section>
        </Transition>

        <div class="panel-actions">   
          <template v-if="currentStep === 0">
            <ElButton
              type="primary"
              class="btn-primary-pink btn-full"
              size="large"
              :disabled="!canProceedToAccountStep"
              @click="handleNextStep"
            >
              下一步：设置账户
            </ElButton>
            <p class="stage-hint">完成邮箱验证后即可进入下一步</p>
          </template>
          <template v-else>
            <div class="action-row">
              <ElButton text class="ghost-btn" @click="handlePrevStep">返回邮箱验证</ElButton>
              <ElButton
                type="primary"
                class="btn-primary-pink"
                size="large"
                :loading="submitting"
                @click="handleSubmit"
              >
                创建辰汐账号
              </ElButton>
            </div>
          </template>
          <p class="auth-links text-center">
            已有账号？<RouterLink :to="{ path: '/', query: { login: '1' } }">立即登录</RouterLink>
          </p>
        </div>
      </ElForm>
    </div>
  </div>
</template>

<style scoped>
@import '../../assets/styles/auth-forms.css';

.register-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: clamp(1.5rem, 5vw, 3rem);
  color: var(--color-text-primary);
  overflow: hidden;
  background: var(--bg-gradient-mixed);
  transition: background 0.5s ease, color 0.3s ease;
}

.background-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.background-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform-origin: center;
  transition: transform 0.85s cubic-bezier(0.33, 1, 0.68, 1);
  will-change: transform;
}

.background-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(5, 6, 12, 0.45), rgba(5, 6, 12, 0.2)), var(--bg-register-overlay);
  pointer-events: none;
  z-index: 0;
  transition: background 0.3s ease, opacity 0.3s ease;
  mix-blend-mode: multiply;
}

.dark .background-overlay {
  background: linear-gradient(135deg, rgba(2, 4, 15, 0.75), rgba(2, 4, 15, 0.6)), var(--bg-register-overlay);
  mix-blend-mode: normal;
}

.auth-panel {
  position: relative;
  z-index: 1;
  width: min(560px, 100%);
  margin-left: auto;
  padding: clamp(1rem, 3vw, 1.75rem);
  border-radius: 36px;
  border: 1px solid var(--color-border);
  background: var(--bg-register-panel-lane);
  backdrop-filter: blur(12px);
  box-shadow: 0 35px 80px rgba(0, 0, 0, 0.45);
  transition: background 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

.dark .auth-panel {
  border-color: var(--border-soft);
  box-shadow: 0 35px 80px rgba(0, 0, 0, 0.6);
}

.register-steps {
  margin-top: 1.25rem;
  margin-bottom: 1.5rem;
}

.register-steps :deep(.el-step__title) {
  color: var(--color-step-title);
  font-size: 0.9rem;
}

/* 粉色步骤条样式 */
.register-steps :deep(.el-step__head.is-process),
.register-steps :deep(.el-step__head.is-success) {
  border-color: #F9A8C8;
}

.register-steps :deep(.el-step__head.is-process .el-step__icon-inner),
.register-steps :deep(.el-step__head.is-success .el-step__icon-inner) {
  color: #F9A8C8;
}

.register-steps :deep(.el-step__title.is-process),
.register-steps :deep(.el-step__title.is-success) {
  color: #F9A8C8;
  font-weight: 600;
}

.register-steps :deep(.el-step__line-inner) {
  background-color: #F9A8C8;
}

.dark .register-steps :deep(.el-step__head.is-process),
.dark .register-steps :deep(.el-step__head.is-success) {
  border-color: #E87A9F;
}

.dark .register-steps :deep(.el-step__head.is-process .el-step__icon-inner),
.dark .register-steps :deep(.el-step__head.is-success .el-step__icon-inner) {
  color: #E87A9F;
}

.dark .register-steps :deep(.el-step__title.is-process),
.dark .register-steps :deep(.el-step__title.is-success) {
  color: #E87A9F;
}

.dark .register-steps :deep(.el-step__line-inner) {
  background-color: #E87A9F;
}

.form-stage {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: var(--bg-form-stage);
  border: 1px solid var(--color-border-form-stage);
  border-radius: 28px;
  padding: clamp(1.25rem, 4vw, 2rem);
  box-shadow: var(--shadow-form-stage);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.stage-header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stage-label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-text-secondary);
}

.stage-header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.stage-header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-grid.two-cols {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.25rem;
}

.form-grid .full-width {
  width: 100%;
}

.form-grid.two-cols .full-width {
  grid-column: span 2;
}

.inline-field {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.inline-field .flex-1 {
  flex: 3;
}

.inline-field .send-code {
  flex: 1;
}

.captcha-field {
  width: 100%;
}

.send-code {
  min-width: auto;
  width: 100%;
  background: #F9A8C8;
  border: none;
  color: white;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.send-code:hover {
  background: #EC8DAD;
  box-shadow: 0 4px 15px rgba(249, 168, 200, 0.4);
}

.send-code:disabled {
  background: rgba(249, 168, 200, 0.4);
  cursor: not-allowed;
}

/* 粉色主按钮 */
.btn-primary-pink {
  background: #F9A8C8 !important;
  border-color: #F9A8C8 !important;
  color: white !important;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.btn-primary-pink:hover {
  background: #EC8DAD !important;
  border-color: #EC8DAD !important;
  box-shadow: 0 8px 25px rgba(249, 168, 200, 0.4);
  transform: translateY(-2px);
}

.btn-primary-pink:disabled {
  background: rgba(249, 168, 200, 0.5) !important;
  border-color: rgba(249, 168, 200, 0.5) !important;
  cursor: not-allowed;
  transform: none;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

/* 深色主题 */
.dark .send-code {
  background: #E87A9F;
}

.dark .send-code:hover {
  background: #EC8DAD;
  box-shadow: 0 4px 15px rgba(232, 122, 159, 0.4);
}

.dark .btn-primary-pink {
  background: #E87A9F !important;
  border-color: #E87A9F !important;
}

.dark .btn-primary-pink:hover {
  background: #EC8DAD !important;
  border-color: #EC8DAD !important;
  box-shadow: 0 8px 25px rgba(232, 122, 159, 0.4);
}

/* 头部样式 - 图片风格 */
.panel-head {
  text-align: center;
  margin-bottom: 1.5rem;
}

.panel-head .eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
}

.panel-head h1 {
  margin: 0;
  font-size: clamp(1.5rem, 4vw, 1.875rem);
  font-weight: 700;
  color: var(--color-text-primary);
}

.btn-full {
  width: 100%;
  justify-content: center;
}

.panel-actions {
  margin-top: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (min-width: 640px) {
  .action-row {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.stage-hint {
  text-align: center;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.ghost-btn {
  color: var(--color-text-secondary);
  transition: color 0.2s ease;
}

.ghost-btn:hover {
  color: var(--color-text-primary);
}

.auth-links a {
  color: var(--color-brand-primary);
  transition: color 0.2s ease;
}
.auth-links a:hover {
  color: var(--color-brand-accent);
}

@media (max-width: 1024px) {
  .register-layout {
    justify-content: center;
  }

  .auth-panel {
    width: 100%;
  }
}
</style>
