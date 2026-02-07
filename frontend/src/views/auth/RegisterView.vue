<script setup>
import { ElMessage, ElSteps, ElStep, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus'
import { User, Message as MailIcon, Lock } from '@element-plus/icons-vue'
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { debounce } from 'lodash-es'
import ChenxiCaptchaInput from '../../components/chenxi/ChenxiCaptchaInput.vue'
import { requestRegisterCode, registerChenxiAccount, checkEmailAvailability } from '../../services/chenxi'
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

const rules = {
  email: [
    { validator: validateEmail, trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
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
  currentStep.value = 1
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
    <div class="panel-lane">
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
                class="btn-gradient"
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
                  class="btn-gradient"
                  size="large"
                  :loading="submitting"
                  @click="handleSubmit"
                >
                  创建辰汐账号
                </ElButton>
              </div>
            </template>
            <p class="auth-links text-center">
              已有账号？<RouterLink to="/login">立即登录</RouterLink>
            </p>
          </div>
        </ElForm>
      </div>
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
  color: #fff;
  overflow: hidden;
  background: #05060c;
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
  background: radial-gradient(circle at 20% 20%, rgba(127, 123, 255, 0.18), transparent 55%),
    radial-gradient(circle at 80% 0%, rgba(255, 95, 143, 0.15), transparent 45%),
    linear-gradient(120deg, rgba(5, 6, 12, 0.65), rgba(5, 6, 12, 0.35));
  pointer-events: none;
  z-index: 0;
}

.panel-lane {
  position: relative;
  z-index: 1;
  width: min(560px, 100%);
  display: flex;
  justify-content: flex-end;
  padding: clamp(1rem, 3vw, 1.75rem);
  border-radius: 36px;
  background: linear-gradient(110deg, rgba(2, 4, 15, 0.9) 20%, rgba(2, 4, 15, 0.45) 55%, rgba(2, 4, 15, 0.2) 100%);
  backdrop-filter: blur(12px);
  box-shadow: 0 35px 80px rgba(0, 0, 0, 0.45);
}

.auth-panel {
  width: 100%;
}

.register-steps {
  margin-top: 1.25rem;
  margin-bottom: 1.5rem;
}

.register-steps :deep(.el-step__title) {
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
}

.register-steps :deep(.el-step__head.is-process),
.register-steps :deep(.el-step__head.is-success) {
  border-color: rgba(190, 144, 255, 0.8);
}

.register-steps :deep(.el-step__icon-inner) {
  color: #0f172a;
}

.form-stage {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 28px;
  padding: clamp(1.25rem, 4vw, 2rem);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
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
  color: rgba(255, 255, 255, 0.55);
}

.stage-header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
}

.stage-header p {
  margin: 0;
  color: rgba(255, 255, 255, 0.65);
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
  flex-direction: column;
  gap: 0.75rem;
}

@media (min-width: 640px) {
  .inline-field {
    flex-direction: row;
    align-items: center;
  }
}

.captcha-field {
  width: 100%;
}

.send-code {
  min-width: 150px;
  background: linear-gradient(135deg, #a78bfa, #7dd3fc);
  border: none;
  color: #0f172a;
  font-weight: 600;
  border-radius: 14px;
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
  color: rgba(255, 255, 255, 0.6);
}

.ghost-btn {
  color: rgba(255, 255, 255, 0.65);
}

.ghost-btn:hover {
  color: #fff;
}

.auth-links a {
  color: #a78bfa;
}
.auth-links a:hover {
  color: #c4b5fd;
}

@media (max-width: 1024px) {
  .register-layout {
    justify-content: center;
  }

  .panel-lane {
    width: 100%;
  }
}
</style>
