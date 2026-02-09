<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import DOMPurify from 'dompurify'
import { useSystemStore } from '@/stores/system'

const systemStore = useSystemStore()
const currentYear = new Date().getFullYear()
const hasClientDom = typeof window !== 'undefined' && typeof document !== 'undefined'

// 运行时间
const runTime = ref('')

const updateRunTime = () => {
  const birthDay = new Date('2025-01-08')
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
  updateRunTime()
  setInterval(updateRunTime, 1000)
})
</script>

<template>
  <footer class="site-footer">
    <div class="footer-container">
      <!-- ICP 备案信息 -->
      <div class="icp-section">
        <div class="icp-item">
          <img 
            src="https://assets.luminouschenxi.com/wp-content/themes/lolimeow-master/assets/images/icp25.png" 
            alt="ICP"
            class="icp-icon"
          />
          <a 
            href="https://beian.miit.gov.cn/" 
            target="_blank" 
            rel="noopener noreferrer"
            class="icp-link"
          >
            冀ICP备2024086279号-2
          </a>
        </div>
        
        <span class="divider">|</span>
        
        <div class="icp-item">
          <img 
            src="https://assets.luminouschenxi.com/wp-content/themes/lolimeow-master/assets/images/%E5%A4%87%E6%A1%88%E5%9B%BE%E6%A0%87.png" 
            alt="公安备案"
            class="icp-icon"
          />
          <a 
            href="https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=13010402003106" 
            target="_blank" 
            rel="noopener noreferrer"
            class="icp-link"
          >
            冀公网安备13010402003106号
          </a>
        </div>
        
        <span class="divider">|</span>
        
        <div class="icp-item">
          <img 
            src="https://assets.luminouschenxi.com/wp-content/themes/lolimeow-master/assets/images/icon120.png" 
            alt="萌ICP"
            class="icp-icon"
          />
          <a 
            href="https://icp.gov.moe/?keyword=luminouschenxi.net" 
            target="_blank" 
            rel="noopener noreferrer"
            class="icp-link"
          >
            萌ICP备20250917号
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
      <div class="footer-bottom">
        <div class="footer-left">
          <p class="copyright">© {{ currentYear }} 辰汐内容安全团队 · All Rights Reserved</p>
          <span class="divider">|</span>
          <p class="runtime">
            此站已运行: <span class="runtime-highlight">{{ runTime }}</span>
          </p>
        </div>
        
        <nav class="footer-nav" aria-label="网站导航">
          <RouterLink to="/gallery" class="nav-link">公开图库</RouterLink>
          <RouterLink to="/user" class="nav-link">用户中心</RouterLink>
          <RouterLink to="/announcements" class="nav-link">公告中心</RouterLink>
          <a href="mailto:chenxi@luminouschenxi.net" class="nav-link">联系我们</a>
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
  color: #ff6b9d;
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
  color: #ff6b9d;
  font-weight: 600;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

:global(.dark) .runtime-highlight {
  color: #ff8fab;
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
  background: linear-gradient(90deg, #ff6b9d, #ff8fab);
  transition: width 0.3s ease;
  border-radius: 1px;
}

.nav-link:hover {
  color: #ff6b9d;
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
