<script setup>
// SSO 登录回调页：/auth/sso/callback?code=...&state=...
// 校验 sessionStorage 中的 state → 授权码换 access_token → 换本地 JWT → 写入会话 → 跳回来源页。
import { ElButton, ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { clearSsoSession, exchangeCode, fetchSsoConfig, SSO_SESSION_KEYS } from '../../services/sso'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// loading | error
const status = ref('loading')
const errorMessage = ref('')

const fail = (message) => {
  errorMessage.value = message
  status.value = 'error'
}

onMounted(async () => {
  const code = typeof route.query.code === 'string' ? route.query.code : ''
  const state = typeof route.query.state === 'string' ? route.query.state : ''
  const idpError = typeof route.query.error === 'string' ? route.query.error : ''
  const savedState = sessionStorage.getItem(SSO_SESSION_KEYS.state)
  const codeVerifier = sessionStorage.getItem(SSO_SESSION_KEYS.verifier)
  const returnTo = sessionStorage.getItem(SSO_SESSION_KEYS.returnTo) || '/'
  // 读取后立即清理，state/verifier 均一次性使用
  clearSsoSession()

  if (idpError) {
    fail('身份源拒绝了本次登录：' + idpError)
    return
  }
  if (!code) {
    fail('缺少授权码，请重新发起 SSO 登录')
    return
  }
  // 防 CSRF：回调 state 必须与跳转前存入 sessionStorage 的值一致
  if (!savedState || savedState !== state) {
    fail('state 校验失败，请重新发起 SSO 登录')
    return
  }
  if (!codeVerifier) {
    fail('登录会话已过期，请重新发起 SSO 登录')
    return
  }

  try {
    const { data: cfg } = await fetchSsoConfig()
    if (!cfg?.enabled) {
      fail('SSO 登录未启用，请使用账号密码登录')
      return
    }
    const session = await exchangeCode(cfg, { code, codeVerifier })
    auth.setSession(session.token, session.profile, session.tokenType)
    ElMessage.success('登录成功！')
    router.replace(returnTo)
  } catch (error) {
    fail(error.response?.data?.message || error.message || 'SSO 登录失败，请稍后重试')
  }
})

const backToLogin = () => {
  router.replace({ path: '/', query: { login: '1' } })
}
</script>

<template>
  <div class="sso-callback-page">
    <div class="sso-callback-card">
      <template v-if="status === 'loading'">
        <div class="spinner" aria-hidden="true"></div>
        <h1 class="card-title">正在完成 SSO 登录</h1>
        <p class="card-text">正在校验授权信息并签发本地会话，请稍候…</p>
      </template>
      <template v-else>
        <div class="error-badge">!</div>
        <h1 class="card-title">SSO 登录失败</h1>
        <p class="card-text">{{ errorMessage }}</p>
        <ElButton type="primary" class="back-btn" @click="backToLogin">返回登录</ElButton>
      </template>
    </div>
  </div>
</template>

<style scoped>
.sso-callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 1.5rem;
  background: linear-gradient(160deg, #f7f8fc 0%, #eef1f8 100%);
}

.sso-callback-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  max-width: 420px;
  padding: 2.5rem 2rem;
  text-align: center;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 45px -20px rgba(26, 26, 46, 0.25);
}

.spinner {
  width: 38px;
  height: 38px;
  margin-bottom: 0.5rem;
  border: 3px solid #e5e7eb;
  border-top-color: #f9a8c8;
  border-radius: 50%;
  animation: sso-spin 0.8s linear infinite;
}

@keyframes sso-spin {
  to {
    transform: rotate(360deg);
  }
}

.error-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-bottom: 0.25rem;
  font-size: 1.25rem;
  font-weight: 700;
  color: #d92d20;
  background: #fee4e2;
  border-radius: 50%;
}

.card-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a2e;
}

.card-text {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.6;
  color: #6b7280;
  word-break: break-all;
}

.back-btn {
  margin-top: 0.75rem;
  border-radius: 10px;
  background: #f9a8c8;
  border: none;
}

.back-btn:hover {
  background: #ec8dad;
}

.dark .sso-callback-page {
  background: linear-gradient(160deg, #14141f 0%, #1a1a2e 100%);
}

.dark .sso-callback-card {
  background: #1a1a2e;
  box-shadow: 0 20px 45px -20px rgba(0, 0, 0, 0.6);
}

.dark .card-title {
  color: #fff;
}

.dark .card-text {
  color: #a0a0b0;
}
</style>
