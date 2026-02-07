<script setup>
import { ElMessage } from 'element-plus'
import { Lock, Message as MailIcon } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { login } from '../../services/auth'
import { useAuthStore } from '../../stores/auth'
import '../../assets/styles/auth-forms.css'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const { data } = await login({ username: form.username, password: form.password })
      auth.setSession(data.token, data.profile)
      ElMessage.success('登录成功，正在跳转控制台')
      router.replace(route.query.redirect || '/user')
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '登录失败，请检查凭证')
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
        <p class="eyebrow">AstrNest Access</p>
        <h1>登录</h1>
        <p class="helper">--LOGIN--</p>
      </header>

      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="auth-form"
        @submit.prevent
      >
        <ElFormItem label="用户名 / 邮箱" prop="username">
          <ElInput
            v-model="form.username"
            v-chenxi-focus
            placeholder="例如 luminouscx"
            :prefix-icon="MailIcon"
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
            placeholder="请输入后台下发的密码"
          />
        </ElFormItem>
        <div class="auth-links">
          <RouterLink to="/register">首次使用？去注册</RouterLink>
          <RouterLink to="/forgot-password">忘记密码</RouterLink>
        </div>
        <ElButton
          type="primary"
          class="btn-gradient"
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          登录  LOGIN
        </ElButton>
      </ElForm>
    </div>
  </div>
</template>

