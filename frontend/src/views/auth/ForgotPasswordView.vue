<script setup>
import { ElMessage, ElSteps, ElStep, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus'
import { Message as MailIcon, Lock } from '@element-plus/icons-vue'
import { computed, reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import ChenxiCaptchaInput from '../../components/chenxi/ChenxiCaptchaInput.vue'
import { requestResetCode, resetChenxiPassword } from '../../services/chenxi'
import { useChenxiEmailCode } from '../../composables/useChenxiEmailCode'
import '../../assets/styles/auth-forms.css'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const form = reactive({
  email: '',
  captchaToken: '',
  code: '',
  password: '',
})

const activeStep = computed(() => {
  if (form.code.length === 6) return 2
  return form.captchaToken ? 1 : 0
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [{ required: true, message: '请设置新密码', trigger: 'blur' }],
}

const { buttonLabel, canSend, sendCode } = useChenxiEmailCode((payload) => requestResetCode(payload))

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  if (!form.captchaToken) {
    ElMessage.warning('请先完成人机验证')
    return
  }
  await sendCode({ email: form.email, captchaToken: form.captchaToken })
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await resetChenxiPassword({ email: form.email, code: form.code, newPassword: form.password })
      router.push({ name: 'reset-password-success' })
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '操作失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <div class="auth-acrylic">
    <div class="auth-panel">
      <header class="panel-head">
        <p class="eyebrow">Password Reset</p>
        <h1>找回密码</h1>
        <p class="helper">完成邮箱验证并立即设置新的登录密码。</p>
      </header>

      <ElSteps :active="activeStep" finish-status="success" align-center class="auth-steps">
        <ElStep title="邮箱验证" description="完成人机验证并接收验证码" />
        <ElStep title="设置新密码" description="重置后立即生效" />
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
        <ElFormItem label="邮箱" prop="email">
          <ElInput
            v-model="form.email"
            placeholder="请输入已注册的邮箱"
            :prefix-icon="MailIcon"
            autocomplete="email"
          />
        </ElFormItem>
        <ChenxiCaptchaInput v-model="form.captchaToken" />
        <ElFormItem label="邮箱验证码" prop="code">
          <div class="code-row">
            <ElInput v-model="form.code" maxlength="6" placeholder="6 位验证码" />
            <ElButton type="primary" plain class="send-btn" :disabled="!canSend" @click="handleSendCode">
              {{ buttonLabel }}
            </ElButton>
          </div>
        </ElFormItem>
        <ElFormItem label="新密码" prop="password">
          <ElInput v-model="form.password" :prefix-icon="Lock" show-password placeholder="请输入新密码" />
        </ElFormItem>
        <ElButton
          type="primary"
          class="btn-gradient mt-2"
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          重置密码
        </ElButton>
        <p class="helper-link">
          记起密码了？<RouterLink to="/login">立即登录</RouterLink>
        </p>
      </ElForm>
    </div>
  </div>
</template>

<style scoped>
.form-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.auth-steps {
  margin-bottom: 1.75rem;
}

.auth-steps :deep(.el-step__title) {
  color: rgba(255, 255, 255, 0.85);
}

.auth-steps :deep(.el-step__description) {
  color: rgba(255, 255, 255, 0.55);
}

.code-row {
  display: flex;
  gap: 0.75rem;
}

.send-btn {
  min-width: 140px;
}

.helper-link {
  margin-top: 0.75rem;
  text-align: center;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.65);
}

.helper-link a {
  color: #c084fc;
}

@media (max-width: 640px) {
  .code-row {
    flex-direction: column;
  }

  .send-btn {
    width: 100%;
  }
}
</style>
