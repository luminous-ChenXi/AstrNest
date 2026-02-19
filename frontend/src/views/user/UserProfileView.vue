<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchProfile, updateProfile } from '../../services/user'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const form = reactive({
  username: '',
  email: '',
  displayName: '',
  website: '',
  signature: '',
  avatarUrl: '',
  location: '',
})
const loading = ref(true)
const saving = ref(false)
const message = ref('')

const avatarPreview = computed(() => {
  if (form.avatarUrl?.trim()) return form.avatarUrl.trim()
  const seed = form.username || 'astrnest'
  return `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(seed)}`
})

const loadProfile = async () => {
  loading.value = true
  message.value = ''
  try {
    const { data } = await fetchProfile()
    form.username = data.username
    form.email = data.email
    form.displayName = data.displayName || ''
    form.website = data.website || ''
    form.signature = data.signature || ''
    form.avatarUrl = data.avatarUrl || ''
    form.location = data.location || ''
    const nextProfile = { ...(auth.profile || {}), ...data }
    auth.updateProfile(nextProfile)
  } catch (error) {
    message.value = error.response?.data?.message || '加载用户信息失败'
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.displayName.trim()) {
    message.value = '昵称不能为空'
    return
  }
  saving.value = true
  message.value = ''
  try {
    const payload = {
      displayName: form.displayName.trim(),
      avatarUrl: form.avatarUrl?.trim() || null,
      website: form.website?.trim() || null,
      signature: form.signature?.trim() || null,
      location: form.location?.trim() || null,
    }
    const { data } = await updateProfile(payload)
    const merged = { ...(auth.profile || {}), ...data }
    auth.updateProfile(merged)
    message.value = '资料已更新'
  } catch (error) {
    message.value = error.response?.data?.message || '更新失败'
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="user-profile space-y-8">
    <section class="glass-panel flex flex-col gap-6 rounded-[32px] border border-white/10 bg-white/5 p-6 md:flex-row md:items-center">
      <img :src="avatarPreview" class="h-28 w-28 rounded-full border-4 border-white/30 object-cover shadow-card" alt="avatar" />
      <div>
        <p class="text-xs uppercase tracking-[0.45em] text-white/60">profile</p>
        <h3 class="text-2xl font-semibold text-gradient">{{ form.displayName || form.username || '未命名用户' }}</h3>
        <p class="text-sm text-white/60">补全资料让账号更可信，可展示在公共图库与分享链接中。</p>
      </div>
    </section>

    <section class="glass-panel rounded-[32px] border border-white/10 bg-white/5 p-6">
      <div class="mb-6">
        <h4 class="text-xl font-semibold">更新资料</h4>
        <p class="text-sm text-white/60">用户名与邮箱为唯一标识，不支持在线修改。</p>
      </div>
      <div v-if="loading" class="text-sm text-white/60">正在加载资料...</div>
      <form v-else class="space-y-5" @submit.prevent="submit">
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-xs uppercase tracking-[0.35em] text-white/50">用户名</label>
            <input v-model="form.username" type="text" disabled class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white/70" />
          </div>
          <div>
            <label class="text-xs uppercase tracking-[0.35em] text-white/50">邮箱</label>
            <input v-model="form.email" type="email" disabled class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white/70" />
          </div>
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-xs uppercase tracking-[0.35em] text-white/50">昵称</label>
            <input
              v-model="form.displayName"
              type="text"
              placeholder="请输入展示名称"
              class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
            />
          </div>
          <div>
            <label class="text-xs uppercase tracking-[0.35em] text-white/50">所在地</label>
            <input
              v-model="form.location"
              type="text"
              placeholder="例如：上海·浦东"
              class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
            />
          </div>
        </div>
        <div>
          <label class="text-xs uppercase tracking-[0.35em] text-white/50">头像地址</label>
          <input
            v-model="form.avatarUrl"
            type="url"
            placeholder="https://example.com/avatar.png"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          />
        </div>
        <div>
          <label class="text-xs uppercase tracking-[0.35em] text-white/50">网站 / 链接</label>
          <input
            v-model="form.website"
            type="url"
            placeholder="https://your.blog"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          />
        </div>
        <div>
          <label class="text-xs uppercase tracking-[0.35em] text-white/50">个性签名</label>
          <textarea
            v-model="form.signature"
            rows="3"
            placeholder="写点自我介绍吧"
            class="mt-2 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-white/40 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/40"
          ></textarea>
        </div>
        <p v-if="message" :class="message.includes('失败') ? 'text-sm text-brand-accent' : 'text-sm text-brand-emerald'">
          {{ message }}
        </p>
        <button
          type="submit"
          class="w-full rounded-2xl bg-gradient-to-r from-brand-primary to-brand-accent py-3 text-sm font-semibold shadow-[0_20px_60px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5 disabled:opacity-60"
          :disabled="saving"
        >
          {{ saving ? '保存中…' : '保存信息' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.user-profile :deep(.text-white\/60) {
  color: var(--text-body-soft);
}

.user-profile :deep(.text-white\/70) {
  color: var(--text-body-muted);
}

.user-profile :deep(.text-white\/50),
.user-profile :deep(.text-white\/40) {
  color: var(--text-body-faint);
}

.user-profile :deep(.text-white) {
  color: var(--color-text-primary);
}

.user-profile :deep(.bg-white\/5) {
  background-color: var(--panel-overlay) !important;
}

.user-profile :deep(.border-white\/10),
.user-profile :deep(.border-white\/15),
.user-profile :deep(.border-white\/30) {
  border-color: var(--border-soft) !important;
}

.user-profile :deep(.placeholder\:text-white\/40::placeholder) {
  color: var(--text-body-faint);
}
</style>
