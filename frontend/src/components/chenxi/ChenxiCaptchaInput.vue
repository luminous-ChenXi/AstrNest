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
  if (status.value === 'success') return 'text-emerald-300'
  if (status.value === 'error') return 'text-rose-300'
  return 'text-white/70'
})

const inputToneClass = computed(() => {
  if (status.value === 'error') return 'ring-2 ring-rose-400/60 focus-within:ring-rose-400/80'
  if (status.value === 'success') return 'ring-2 ring-emerald-300/50 focus-within:ring-emerald-300/70'
  return 'ring-1 ring-white/10 focus-within:ring-white/30'
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
    }
  } catch (error) {
    status.value = 'error'
    emit('update:modelValue', '')
    ElMessage.error(error.response?.data?.message || '校验失败，请稍后重试')
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
    <div class="flex items-center justify-between text-sm text-white/70">
      <div class="flex items-center gap-2" :class="helperToneClass">
        <ElIcon :size="16">
          <CircleCheckFilled v-if="status === 'success'" />
          <WarningFilled v-else />
        </ElIcon>
        <span :class="helperToneClass">{{ helperText }}</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-white/60" v-if="challenge">
        <span v-if="remainingSeconds > 0">{{ remainingSeconds }}s</span>
        <ElButton link type="primary" :loading="loading" @click="loadCaptcha">
          <ElIcon><Refresh /></ElIcon>
          刷新
        </ElButton>
      </div>
    </div>

    <ElSkeleton v-if="loading" animated :count="1" class="rounded-2xl">
      <template #template>
        <div class="h-[150px] rounded-3xl bg-white/5" />
      </template>

    </ElSkeleton>

    <div v-else class="rounded-3xl border border-white/10 bg-white/5 p-4">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div
          class="relative flex h-[48px] sm:h-[52px] flex-none items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-white/10 px-2 sm:px-3"
        >
          <img
            v-if="challenge"
            :src="challenge.image"
            alt="captcha"
            class="h-full w-full object-contain"
          />
          <span v-else class="text-xs text-white/60">验证码加载中...</span>
        </div>
        <div class="flex w-full flex-col gap-2">
          <div :class="['rounded-2xl bg-white/5 px-3 py-2 transition-all duration-150', inputToneClass]">
            <ElInput
              v-model="inputValue"
              maxlength="8"
              placeholder="输入图形中的字符"
              autocomplete="off"
              class="!border-none !bg-transparent text-white placeholder:text-white/40"
              @keyup.enter="handleVerify"
            />
          </div>
          <ElButton
            type="primary"
            class="captcha-submit"
            :loading="verifying"
            size="small"
            @click="handleVerify"
          >
            提交验证
          </ElButton>
        </div>
      </div>
      <p class="mt-3 text-right text-xs text-white/50" v-if="status === 'error'">
        如多次失败请刷新图形验证码后再试
      </p>
    </div>
  </div>
</template>

<style scoped>
.captcha-submit {
  height: 42px;
  padding: 0 1.5rem;
  border-radius: 14px;
  font-size: 0.9rem;
}
</style>
