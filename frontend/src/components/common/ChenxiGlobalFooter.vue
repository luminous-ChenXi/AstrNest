<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import DOMPurify from 'dompurify'
import { useSystemStore } from '@/stores/system'

const systemStore = useSystemStore()
const currentYear = new Date().getFullYear()
const hasClientDom = typeof window !== 'undefined' && typeof document !== 'undefined'

const normalizeMarkup = (markup) => {
  if (!hasClientDom) {
    return markup
  }
  const template = document.createElement('template')
  template.innerHTML = markup
  return template.innerHTML
}

const footerRenderState = computed(() => {
  const raw = systemStore.config?.customFooterHtml?.trim() || ''
  if (!raw) {
    return { html: '', warning: '' }
  }
  try {
    const normalized = normalizeMarkup(raw)
    const sanitized = DOMPurify.sanitize(normalized, {
      USE_PROFILES: { html: true },
      ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[^a-z]|[a-z+.-]+(?:[^a-z]|$))/i,
    })
    return {
      html: sanitized,
      warning: '',
    }
  } catch (error) {
    const safeText = DOMPurify.sanitize(raw, { ALLOWED_TAGS: [] })
    return {
      html: safeText,
      warning: '自定义页脚存在无法解析的标签，已暂时降级为纯文本。',
    }
  }
})

onMounted(() => {
  systemStore.fetchSystemConfig()
})
</script>

<template>
  <footer class="relative z-10 border-t border-body bg-surface-overlay text-body-secondary backdrop-blur-2xl">
    <div class="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-8">
      <div
        v-if="footerRenderState.html"
        class="rounded-2xl border border-body bg-surface-body px-4 py-3 text-sm text-body-primary"
        v-html="footerRenderState.html"
      ></div>
      <p
        v-if="footerRenderState.warning"
        class="rounded-2xl border border-amber-400/40 bg-amber-400/10 px-4 py-2 text-xs text-amber-700 dark:text-amber-100"
      >
        {{ footerRenderState.warning }}
      </p>
      <div class="flex flex-wrap items-center justify-between gap-4 text-sm text-body-soft">
        <p class="text-body-secondary">© {{ currentYear }} 辰汐内容安全团队 · All Rights Reserved</p>
        <nav class="flex flex-wrap gap-4" aria-label="网站导航">
          <RouterLink to="/gallery" class="transition hover:text-body-primary">公开图库</RouterLink>
          <RouterLink to="/user" class="transition hover:text-body-primary">用户中心</RouterLink>
          <RouterLink to="/announcements" class="transition hover:text-body-primary">公告中心</RouterLink>
          <a href="mailto:chenxi@luminouschenxi.net" class="transition hover:text-body-primary">联系我们</a>
        </nav>
      </div>
    </div>
  </footer>
</template>
