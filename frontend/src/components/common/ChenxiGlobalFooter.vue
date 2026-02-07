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
  <footer class="relative z-10 border-t border-white/10 bg-black/20">
    <div class="mx-auto flex max-w-6xl flex-col gap-3 px-6 py-8">
      <div
        v-if="footerRenderState.html"
        class="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white/80"
        v-html="footerRenderState.html"
      ></div>
      <p
        v-if="footerRenderState.warning"
        class="rounded-2xl border border-amber-400/30 bg-amber-400/10 px-4 py-2 text-xs text-amber-100"
      >
        {{ footerRenderState.warning }}
      </p>
      <div class="flex flex-wrap items-center justify-between gap-4 text-sm text-white/60">
        <p>© {{ currentYear }} 辰汐内容安全团队 · All Rights Reserved</p>
        <div class="flex gap-4">
          <RouterLink to="/user" class="hover:text-white">用户中心</RouterLink>
          <a href="mailto:chenxi@luminouschenxi.net" class="hover:text-white">联系我们</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer-badge {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.footer-badge img {
  height: 16px;
}

.footer-badge .divider {
  color: rgba(255, 255, 255, 0.6);
}
</style>
