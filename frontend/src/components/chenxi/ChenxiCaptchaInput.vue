<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElButton, ElIcon, ElInput, ElMessage, ElSkeleton } from 'element-plus'
import { Refresh, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { createChenxiCaptcha, verifyChenxiCaptcha } from '../../services/chenxi'

defineProps({
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const verifying = ref(false)
const challenge = ref(null)
const inputValue = ref('')
const status = ref('idle')
const expiresAt = ref(null)

const remainingSeconds = computed(() => {
  if (!expiresAt.value) return 0
  const diff = Math.floor((expiresAt.value - Date.now()) / 1000)
  return diff > 0 ? diff : 0
})

const helperText = computed(() => {
  if (status.value === 'success') return '验证通过，可继续下一步操作'
  if (status.value === 'error') return '验证码错误，请重新输入'
  if (challenge.value && remainingSeconds.value === 0) return '验证码已过期，请刷新'
  return '请输入图片中的字符完成图形校验'
})

const helperToneClass = computed(() => {
  if (status.value === 'success') return 'helper-success'
  if (status.value === 'error') return 'helper-error'
  return 'helper-default'
})

const inputToneClass = computed(() => {
  if (status.value === 'error') return 'tone-error'
  if (status.value === 'success') return 'tone-success'
  return 'tone-idle'
})

const loadCaptcha = async () => {
  try {
    loading.value = true
    emit('update:modelValue', '')
    status.value = 'idle'
    inputValue.value = ''
    const { data } = await createChenxiCaptcha()
    challenge.value = {
      id: data.captchaId,
      image: data.imageBase64,
      width: data.width,
      height: data.height,
    }
    expiresAt.value = Date.now() + data.expiresIn * 1000
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验证码获取失败')
  } finally {
    loading.value = false
    verifying.value = false
  }
}

const handleVerify = async () => {
  if (!challenge.value) {
    ElMessage.warning('请先刷新图形验证码')
    return
  }
  if (!inputValue.value.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  if (remainingSeconds.value === 0) {
    ElMessage.warning('验证码已过期，请点击刷新')
    return
  }
  verifying.value = true
  try {
    const { data } = await verifyChenxiCaptcha({
      captchaId: challenge.value.id,
      captchaCode: inputValue.value.trim(),
    })
    if (data.passed) {
      status.value = 'success'
      emit('update:modelValue', data.certificationToken)
      ElMessage.success('图形验证码已验证通过')
    } else {
      status.value = 'error'
      emit('update:modelValue', '')
      inputValue.value = ''
      ElMessage.error('图形验证码不正确，请重新输入')
      loadCaptcha()
    }
  } catch (error) {
    status.value = 'error'
    emit('update:modelValue', '')
    ElMessage.error(error.response?.data?.message || '校验失败，请稍后重试')
    loadCaptcha()
  } finally {
    verifying.value = false
  }
}

watch(inputValue, () => {
  if (status.value === 'success') {
    status.value = 'idle'
    emit('update:modelValue', '')
  }
})

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <div class="space-y-4">
    <div class="captcha-helper">
      <div class="helper-left" :class="helperToneClass">
        <ElIcon :size="16">
          <CircleCheckFilled v-if="status === 'success'" />
          <WarningFilled v-else />
        </ElIcon>
        <span :class="helperToneClass">{{ helperText }}</span>
      </div>
      <div class="helper-right" v-if="challenge">
        <span v-if="remainingSeconds > 0">{{ remainingSeconds }}s</span>
        <ElButton link type="primary" :loading="loading" @click="loadCaptcha">
          <ElIcon><Refresh /></ElIcon>
          刷新
        </ElButton>
      </div>
    </div>

    <ElSkeleton v-if="loading" animated :count="1" class="captcha-skeleton">
      <template #template>
        <div class="captcha-skeleton-block" />
      </template>

    </ElSkeleton>

    <div v-else class="captcha-card">
      <div class="captcha-row">
        <div :class="['captcha-input-wrap', inputToneClass]">
          <ElInput
            v-model="inputValue"
            maxlength="8"
            placeholder="输入图形中的字符"
            autocomplete="off"
            class="captcha-input"
            @keyup.enter="handleVerify"
          />
        </div>
        <div class="captcha-image-wrap">
          <img
            v-if="challenge"
            :src="challenge.image"
            alt="captcha"
            class="captcha-image"
          />
          <span v-else class="captcha-image-hint">验证码加载中...</span>
        </div>
      </div>
      <div class="captcha-actions">
        <ElButton
          type="primary"
          class="captcha-submit"
          :loading="verifying"
          size="small"
          @click="handleVerify"
        >
          提交验证
        </ElButton>
        <p class="captcha-error" v-if="status === 'error'">
          如多次失败请刷新图形验证码后再试
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.captcha-helper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.95rem;
}

.helper-left {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.helper-right {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.helper-success {
  color: #34d399;
}

.helper-error {
  color: #f87171;
}

.helper-default {
  color: var(--color-text-secondary);
}

.captcha-skeleton {
  border-radius: 24px;
}

.captcha-skeleton-block {
  height: 150px;
  border-radius: 24px;
  background: var(--color-bg-secondary);
}

.captcha-card {
  border-radius: 22px;
  border: 1px solid var(--glass-border);
  background: var(--glass-bg);
  padding: 1rem;
  box-shadow: var(--shadow-card);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 150px;
  gap: 0.75rem;
  align-items: center;
}

@media (max-width: 640px) {
  .captcha-row {
    grid-template-columns: 1fr;
  }
}

.captcha-input-wrap {
  border-radius: 16px;
  padding: 0.4rem 0.6rem;
  background: var(--color-bg-secondary);
  border: 1px solid var(--border-soft);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.captcha-input {
  border: none;
  background: transparent;
  color: var(--color-text-primary);
}

.captcha-input :deep(.el-input__inner) {
  background: transparent;
  border: none;
  color: var(--color-text-primary);
}

.captcha-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding: 0;
}

.captcha-input :deep(.el-input__inner::placeholder) {
  color: var(--color-text-secondary);
}

.captcha-image-wrap {
  position: relative;
  height: 56px;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: var(--color-bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-image-hint {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.captcha-actions {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  width: 100%;
}

.captcha-submit {
  width: 100%;
  height: 44px;
  padding: 0 1.5rem;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
}

.captcha-error {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.tone-error {
  border-color: rgba(248, 113, 113, 0.5);
  box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.4);
}

.tone-success {
  border-color: rgba(74, 222, 128, 0.6);
  box-shadow: 0 0 0 2px rgba(74, 222, 128, 0.35);
}

.tone-idle {
  border-color: var(--border-soft);
}
</style>
