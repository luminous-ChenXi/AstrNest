<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import DOMPurify from 'dompurify'
import { useSystemStore } from '@/stores/system'

const systemStore = useSystemStore()
const currentYear = new Date().getFullYear()
const hasClientDom = typeof window !== 'undefined' && typeof document !== 'undefined'

// 页脚个性化信息均通过环境变量注入，未配置时对应板块整体不渲染
const env = import.meta.env
const icpNumber = String(env.VITE_ICP_NUMBER || '').trim()
const beianRecordCode = String(env.VITE_BEIAN_RECORD_CODE || '').trim()
const siteBirthday = String(env.VITE_SITE_BIRTHDAY || '').trim()
const siteCopyright = String(env.VITE_SITE_COPYRIGHT || '').trim()
const contactEmail = String(env.VITE_CONTACT_EMAIL || '').trim()

const hasIcp = Boolean(icpNumber)
const hasBeian = Boolean(beianRecordCode)
const hasIcpSection = hasIcp || hasBeian
const hasRuntime = Boolean(siteBirthday)
const hasCopyright = Boolean(siteCopyright)
const hasContact = Boolean(contactEmail)
const hasFooterNav = true
const hasFooterBottom = hasCopyright || hasRuntime || hasFooterNav

// 运行时间
const runTime = ref('')

const updateRunTime = () => {
  if (!hasRuntime) {
    return
  }
  const birthDay = new Date(siteBirthday)
  if (Number.isNaN(birthDay.getTime())) {
    return
  }
  const today = new Date()
  const timeold = today.getTime() - birthDay.getTime()
  const msPerDay = 24 * 60 * 60 * 1000
  const daysold = Math.floor(timeold / msPerDay)
  const hours = today.getHours()
  const minutes = today.getMinutes()
  const seconds = today.getSeconds()
  runTime.value = `${daysold}天${hours}时${minutes}分${seconds}秒`
}

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
  if (hasRuntime) {
    updateRunTime()
    setInterval(updateRunTime, 1000)
  }
})
</script>

<template>
  <footer class="site-footer">
    <div class="footer-container">
      <!-- ICP 备案信息（仅当对应环境变量已配置时渲染） -->
      <div v-if="hasIcpSection" class="icp-section">
        <div v-if="hasIcp" class="icp-item">
          <img 
            src="/images/footer/icp25.png" 
            alt="ICP"
            class="icp-icon"
          />
          <a 
            href="https://beian.miit.gov.cn/" 
            target="_blank" 
            rel="noopener noreferrer"
            class="icp-link"
          >
            {{ icpNumber }}
          </a>
        </div>
        
        <span v-if="hasIcp && hasBeian" class="divider">|</span>
        
        <div v-if="hasBeian" class="icp-item">
          <img 
            src="/images/footer/beian-icon.png" 
            alt="公安备案"
            class="icp-icon"
          />
          <a 
            :href="`https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=${beianRecordCode}`" 
            target="_blank" 
            rel="noopener noreferrer"
            class="icp-link"
          >
            {{ beianRecordCode }}
          </a>
        </div>
      </div>

      <!-- 自定义页脚内容 -->
      <div
        v-if="footerRenderState.html"
        class="custom-footer"
        v-html="footerRenderState.html"
      ></div>
      <p
        v-if="footerRenderState.warning"
        class="footer-warning"
      >
        {{ footerRenderState.warning }}
      </p>

      <!-- 底部信息栏 -->
      <div v-if="hasFooterBottom" class="footer-bottom">
        <div v-if="hasCopyright || hasRuntime" class="footer-left">
          <p v-if="hasCopyright" class="copyright">© {{ currentYear }} {{ siteCopyright }} · All Rights Reserved</p>
          <span v-if="hasCopyright && hasRuntime" class="divider">|</span>
          <p v-if="hasRuntime" class="runtime">
            此站已运行: <span class="runtime-highlight">{{ runTime }}</span>
          </p>
        </div>
        
        <nav class="footer-nav" aria-label="网站导航">
          <RouterLink to="/gallery" class="nav-link">公开图库</RouterLink>
          <RouterLink to="/user" class="nav-link">用户中心</RouterLink>
          <RouterLink to="/announcements" class="nav-link">公告中心</RouterLink>
          <a v-if="hasContact" :href="`mailto:${contactEmail}`" class="nav-link">联系我们</a>
        </nav>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  position: relative;
  z-index: 10;
  background: var(--bg-body, #fafafa);
  border-top: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  padding: 32px 0;
}

:global(.dark) .site-footer {
  background: #0a0a0f;
  border-color: rgba(255, 255, 255, 0.1);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ICP Section */
.icp-section {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 16px;
  padding: 16px 24px;
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

:global(.dark) .icp-section {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
}

.icp-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.icp-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
  vertical-align: middle;
}

.icp-link {
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
  text-decoration: none;
  transition: all 0.2s ease;
}

:global(.dark) .icp-link {
  color: rgba(255, 255, 255, 0.7);
}

.icp-link:hover {
  color: var(--color-brand-primary);
}

.divider {
  color: var(--border-soft, rgba(0, 0, 0, 0.2));
  font-size: 0.85rem;
}

:global(.dark) .divider {
  color: rgba(255, 255, 255, 0.3);
}

/* Custom Footer */
.custom-footer {
  padding: 16px 24px;
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
  border-radius: 16px;
  font-size: 0.9rem;
  color: var(--text-secondary, #4a4a5c);
}

:global(.dark) .custom-footer {
  background: #1a1a2e;
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.footer-warning {
  padding: 12px 16px;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  border-radius: 12px;
  font-size: 0.85rem;
  color: #d97706;
}

/* Footer Bottom */
.footer-bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
}

:global(.dark) .footer-bottom {
  border-color: rgba(255, 255, 255, 0.1);
}

.footer-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.copyright {
  font-size: 0.9rem;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

:global(.dark) .copyright {
  color: rgba(255, 255, 255, 0.6);
}

.runtime {
  font-size: 0.9rem;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

:global(.dark) .runtime {
  color: rgba(255, 255, 255, 0.6);
}

.runtime-highlight {
  color: var(--color-brand-primary);
  font-weight: 600;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

/* Footer Nav */
.footer-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 20px;
}

.nav-link {
  font-size: 0.9rem;
  color: var(--text-secondary, #6b7280);
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
}

:global(.dark) .nav-link {
  color: rgba(255, 255, 255, 0.6);
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--color-brand-primary), var(--color-brand-accent));
  transition: width 0.3s ease;
  border-radius: 1px;
}

.nav-link:hover {
  color: var(--color-brand-primary);
}

.nav-link:hover::after {
  width: 100%;
}

/* Responsive */
@media (max-width: 768px) {
  .site-footer {
    padding: 24px 0;
  }
  
  .footer-container {
    padding: 0 16px;
    gap: 16px;
  }
  
  .icp-section {
    padding: 12px 16px;
    gap: 6px 12px;
  }
  
  .icp-link {
    font-size: 0.8rem;
  }
  
  .footer-bottom {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 20px;
  }
  
  .footer-left {
    flex-direction: column;
    gap: 8px;
  }
  
  .footer-left .divider {
    display: none;
  }
  
  .footer-nav {
    justify-content: center;
  }
}
</style>
