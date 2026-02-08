<script setup>
import { ElMessage } from 'element-plus'
import { Lock, User, Close, Star } from '@element-plus/icons-vue'
import { reactive, ref, watch } from 'vue'
import { login } from '../../services/auth'
import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'login-success', 'closed'])

const auth = useAuthStore()

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

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const { data } = await login({ username: form.username.trim(), password: form.password })
      auth.setSession(data.token, data.profile)
      toast('success', '登录成功！')
      emit('login-success')
      handleClose()
    } catch (error) {
      toast('error', error.response?.data?.message || '登录失败，请检查凭证')
    } finally {
      submitting.value = false
    }
  })
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
    // 重置表单
    form.username = ''
    form.password = ''
    submitting.value = false
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
              <div class="brand-logo-large">
                <span class="logo-text">CX</span>
              </div>
              <h2 class="brand-title">辰汐图床</h2>
              <p class="brand-slogan">灵感存储，随时分享</p>
              
              <!-- 占位图片区域 -->
              <div class="brand-visual">
                <div class="visual-placeholder">
                  <Star class="placeholder-icon" />
                  <span>创作无界限</span>
                </div>
              </div>
            </div>

            <!-- 右侧：登录表单 -->
            <div class="modal-form">
              <header class="form-header">
                <p class="form-eyebrow">欢迎回来</p>
                <h1 class="form-title">登录账号</h1>
              </header>

              <ElForm
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

                <div class="form-footer">
                  <span class="footer-text">还没有账号？</span>
                  <RouterLink to="/register" class="register-link" @click="handleClose">
                    立即注册
                  </RouterLink>
                </div>
              </ElForm>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
  background: linear-gradient(135deg, #FADCE9 0%, #F9A8C8 50%, #E87A9F 100%);
  color: white;
  text-align: center;
}

.brand-logo-large {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  margin-bottom: 1.5rem;
}

.logo-text {
  font-size: 2rem;
  font-weight: 800;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
  color: white;
}

.brand-slogan {
  font-size: 1rem;
  opacity: 0.9;
  margin: 0 0 2rem;
}

/* 占位图片区域 */
.brand-visual {
  width: 100%;
  max-width: 200px;
}

.visual-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.2);
  border: 2px dashed rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  backdrop-filter: blur(4px);
}

.placeholder-icon {
  width: 40px;
  height: 40px;
  opacity: 0.8;
}

.visual-placeholder span {
  font-size: 0.9rem;
  opacity: 0.9;
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
