<script setup>
import { onMounted, reactive, ref } from 'vue'
import { changePassword, fetchSecuritySettings } from '../../services/user'

const settings = ref({ apiHeaderName: 'X-API-Key', defaultDailyQuota: 0, recentLogins: [] })
const loading = ref(true)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const message = ref('')
const saving = ref(false)

const loadSettings = async () => {
  loading.value = true
  try {
    const { data } = await fetchSecuritySettings()
    settings.value = data
  } catch (error) {
    console.error('加载安全配置失败', error)
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.value = '两次输入的密码不一致'
    return
  }
  saving.value = true
  message.value = ''
  try {
    await changePassword({ ...passwordForm })
    message.value = '密码已更新'
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    message.value = error.response?.data?.message || '修改失败'
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <div class="user-security space-y-8">
    <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-xl font-semibold">API 安全</h3>
          <p class="text-sm text-white/60">保持密钥安全，必要时及时重置</p>
        </div>
        <span class="rounded-full border border-white/20 px-4 py-1 text-xs text-white/70">Header: {{ settings.apiHeaderName }}</span>
      </div>
      <p class="mt-4 text-sm text-white/60">默认日调用配额：{{ settings.defaultDailyQuota }}</p>
    </section>

    <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6">
      <h4 class="text-xl font-semibold">修改密码</h4>
      <p class="text-sm text-white/60">建议定期更新密码，避免多人共用。</p>
      <form class="mt-6 space-y-4" @submit.prevent="submit">
        <label class="block text-sm text-white/70">
          当前密码
          <input
            v-model="passwordForm.currentPassword"
            type="password"
            autocomplete="current-password"
            required
            placeholder="请输入当前密码"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          />
        </label>
        <label class="block text-sm text-white/70">
          新密码
          <input
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            required
            placeholder="请输入新密码"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          />
        </label>
        <label class="block text-sm text-white/70">
          确认新密码
          <input
            v-model="passwordForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            required
            placeholder="再次输入新密码"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          />
        </label>
        <p v-if="message" :class="message.includes('失败') ? 'text-sm text-brand-accent' : 'text-sm text-brand-emerald'">{{ message }}</p>
        <button
          class="w-full rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent py-3 text-sm font-semibold shadow-[0_20px_60px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5 disabled:opacity-60"
          type="submit"
          :disabled="saving"
        >
          {{ saving ? '提交中…' : '保存修改' }}
        </button>
      </form>
    </section>

    <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6">
      <div class="flex items-center justify-between gap-3">
        <div>
          <h4 class="text-xl font-semibold">最近登录</h4>
          <p class="text-sm text-white/60">帮助你发现异常登录</p>
        </div>
      </div>
      <div v-if="loading" class="mt-4 text-sm text-white/60">加载中…</div>
      <div v-else-if="!settings.recentLogins?.length" class="mt-4 text-sm text-white/60">暂未记录登录历史</div>
      <div v-else class="mt-4 overflow-x-auto rounded-2xl border border-white/10 bg-black/20">
        <table class="w-full text-left text-sm">
          <thead>
            <tr class="text-white/60">
              <th class="px-4 py-3">时间</th>
              <th class="px-4 py-3">IP</th>
              <th class="px-4 py-3">位置</th>
              <th class="px-4 py-3">设备</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in settings.recentLogins" :key="row.id || row.occurredAt" class="border-t border-white/5 text-white/80">
              <td class="px-4 py-3">{{ new Date(row.occurredAt).toLocaleString() }}</td>
              <td class="px-4 py-3">{{ row.ip }}</td>
              <td class="px-4 py-3">{{ row.location || '未知区域' }}</td>
              <td class="px-4 py-3">{{ row.device || '未知设备' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h4 class="text-xl font-semibold">API 接口管理</h4>
          <p class="text-sm text-white/60">使用密钥在任何客户端直接上传媒体</p>
        </div>
        <RouterLink
          class="rounded-full border border-white/20 px-5 py-2 text-sm font-semibold text-white/80 transition hover:border-brand-primary hover:text-white"
          :to="{ name: 'user-api' }"
        >
          去管理
        </RouterLink>
      </div>
      <ul class="mt-4 space-y-2 text-sm text-white/70">
        <li>✔ 复制 Demo/上传API实例.txt 中的 cURL / JS 示例即可接入</li>
        <li>✔ 支持密码保护、CDN 域名切换与多格式输出</li>
        <li>✔ 分别查看调用次数与分钟级限流，发现异常可立即禁用</li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.user-security :deep(.text-white\/80) {
  color: var(--text-body-secondary);
}

.user-security :deep(.text-white\/70) {
  color: var(--text-body-muted);
}

.user-security :deep(.text-white\/60) {
  color: var(--text-body-soft);
}

.user-security :deep(.text-white\/50),
.user-security :deep(.text-white\/40) {
  color: var(--text-body-faint);
}

.user-security :deep(.text-white) {
  color: var(--color-text-primary);
}

.user-security :deep(.bg-white\/5) {
  background-color: var(--panel-overlay) !important;
}

.user-security :deep(.bg-black\/20) {
  background-color: var(--color-bg-strong) !important;
}

.user-security :deep(.border-white\/10),
.user-security :deep(.border-white\/15),
.user-security :deep(.border-white\/20) {
  border-color: var(--border-soft) !important;
}

.user-security :deep(.placeholder\:text-white\/40::placeholder) {
  color: var(--text-body-faint);
}
</style>
