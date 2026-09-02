import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref } from 'vue'

export function useChenxiEmailCode(sendHandler) {
  const countdown = ref(0)
  const sending = ref(false)
  let timer = null

  const canSend = computed(() => countdown.value === 0 && !sending.value)
  const buttonLabel = computed(() => {
    if (countdown.value > 0) {
      return `${countdown.value}s 后可重发`
    }
    return sending.value ? '发送中...' : '发送验证码'
  })

  const sendCode = async (payload) => {
    if (!canSend.value) {
      return
    }
    sending.value = true
    try {
      await sendHandler(payload)
      ElMessage.success('验证码已发送到邮箱，请在 5 分钟内完成验证')
      startCountdown()
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '验证码发送失败')
    } finally {
      sending.value = false
    }
  }

  const startCountdown = () => {
    countdown.value = 60
    clearInterval(timer)
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
        countdown.value = 0
      }
    }, 1000)
  }

  onBeforeUnmount(() => {
    clearInterval(timer)
  })

  return {
    countdown,
    buttonLabel,
    canSend,
    sendCode,
  }
}
