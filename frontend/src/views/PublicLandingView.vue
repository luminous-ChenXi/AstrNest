<template>
  <div class="landing-wrapper">
    <div class="landing-page relative min-h-screen overflow-hidden">
      <!-- 动态背景层 - 三色混合过渡 (60%覆盖，40%留白) -->
      <div class="landing-bg">
      <!-- 柔粉光球 - 左上 (最大) -->
      <div class="gradient-orb orb-pink"></div>
      <!-- 淡蓝光球 - 右上 (中等) -->
      <div class="gradient-orb orb-blue"></div>
      <!-- 薄荷绿光球 - 中下 (较小) -->
      <div class="gradient-orb orb-mint"></div>
      <!-- 过渡混合层 -->
      <div class="gradient-blend blend-pink-blue"></div>
      <div class="gradient-blend blend-blue-mint"></div>
      <div class="gradient-blend blend-mint-pink"></div>
      <!-- 微光效果 -->
      <div class="shimmer-overlay"></div>
      <!-- 噪点纹理 -->
      <div class="noise-overlay"></div>
    </div>

    <!-- 导航栏 -->
    <header class="landing-header fixed inset-x-0 top-0 z-50">
      <div class="header-inner mx-auto flex max-w-7xl items-center justify-between px-4 py-3 md:px-6 md:py-4">
        <div class="flex items-center gap-3 md:gap-4">
          <div class="logo-wrapper">
            <SiteLogo :width="40" :height="40" />
          </div>
          <div class="logo-text">
            <p class="logo-title">AstrNest</p>
            <p class="logo-subtitle hidden sm:block">图床系统</p>
          </div>
        </div>

        <!-- 桌面导航 -->
        <nav class="hidden items-center gap-8 md:flex">
          <RouterLink to="/gallery" class="nav-link">公开图库</RouterLink>
          <RouterLink to="/gallery#gallery-search" class="nav-link">智能检索</RouterLink>
          <RouterLink to="/announcements" class="nav-link">公告</RouterLink>
          <a href="#contact" class="nav-link">文档</a>
        </nav>

        <!-- 桌面登录/用户入口 -->
        <div v-if="!auth.isAuthenticated" class="hidden items-center gap-3 md:flex">
          <ThemeSwitcher />
          <button type="button" class="btn-ghost" @click="openLoginModal">登录</button>
          <RouterLink to="/register" class="btn-primary">注册</RouterLink>
        </div>
        <div v-else class="hidden items-center gap-3 md:flex">
          <ThemeSwitcher />
          <RouterLink to="/user" class="btn-ghost">会员中心</RouterLink>
          <UserQuickMenu :items="userMenuItems" />
        </div>

        <!-- 移动端操作区 -->
        <div class="flex items-center gap-2 md:hidden">
          <!-- 未登录状态 -->
          <template v-if="!auth.isAuthenticated">
            <ThemeSwitcher />
            <button type="button" class="btn-login-mobile" @click="openLoginModal">登录</button>
            <button class="menu-toggle" @click="isNavOpen = !isNavOpen" aria-label="切换导航">
              <span v-if="!isNavOpen" class="menu-icon">☰</span>
              <span v-else class="menu-icon">✕</span>
            </button>
          </template>
          <!-- 已登录状态 -->
          <template v-else>
            <ThemeSwitcher />
            <button class="menu-toggle" @click="isNavOpen = !isNavOpen" aria-label="切换导航">
              <span v-if="!isNavOpen" class="menu-icon">☰</span>
              <span v-else class="menu-icon">✕</span>
            </button>
          </template>
        </div>
      </div>

      <!-- 移动端折叠导航 -->
      <transition name="slide-down">
        <div v-if="isNavOpen" class="mobile-nav md:hidden">
          <div class="mobile-nav-inner">
            <RouterLink to="/gallery" class="mobile-nav-link" @click="isNavOpen = false">
              <span class="mobile-nav-icon">🖼️</span>
              <span>公开图库</span>
            </RouterLink>
            <RouterLink to="/gallery#gallery-search" class="mobile-nav-link" @click="isNavOpen = false">
              <span class="mobile-nav-icon">🔍</span>
              <span>智能检索</span>
            </RouterLink>
            <RouterLink to="/announcements" class="mobile-nav-link" @click="isNavOpen = false">
              <span class="mobile-nav-icon">📢</span>
              <span>公告</span>
            </RouterLink>
            <a href="#contact" class="mobile-nav-link" @click="isNavOpen = false">
              <span class="mobile-nav-icon">📖</span>
              <span>文档</span>
            </a>
            <div class="mobile-nav-divider"></div>
            <div class="mobile-nav-actions">
              <template v-if="!auth.isAuthenticated">
                <button type="button" class="btn-primary w-full text-center" @click="openLoginModal(); isNavOpen = false">登录</button>
                <RouterLink to="/register" class="btn-ghost w-full text-center mt-2" @click="isNavOpen = false">注册账号</RouterLink>
              </template>
              <template v-else>
                <RouterLink to="/user" class="btn-primary w-full text-center" @click="isNavOpen = false">会员中心</RouterLink>
                <button type="button" class="btn-ghost w-full text-center mt-2 text-red-500" @click="logout(); isNavOpen = false">退出登录</button>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </header>

    <main class="landing-main relative z-10">
      <!-- Hero Section -->
      <section class="hero-section" v-lazy-animate="{ fromY: 40, duration: 0.8 }">
        <div class="hero-container">
          <div class="hero-grid">
            <div class="hero-content">
              <p class="text-xs uppercase tracking-[0.45em] text-black/50">Luminouscx · 辰汐</p>
              <!-- <div class="hero-tag">
                <span class="tag-pulse"></span>
                <span class="tag-text">Luminouscx · 辰汐</span>
              </div> -->
              <h1 class="hero-title">
                <span class="title-line">AstrNest</span>
                <!-- <span class="title-line title-accent">辰汐</span> -->
              </h1>
              <p class="hero-description">
                Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3.
              </p>
              <div class="hero-actions">
                <RouterLink to="/user" class="btn-primary btn-lg">
                  <Sparkles class="btn-icon" />
                  立即开始
                </RouterLink>
                <RouterLink to="/gallery" class="btn-ghost btn-lg">
                  <Search class="btn-icon" />
                  浏览图库
                </RouterLink>
              </div>
              
              <!-- 痛点解决卡片 - 对称网格布局 -->
              <div class="pain-points-grid">
                <div class="pain-card" v-lazy-animate="{ fromY: 25, delay: 0.1 }">
                  <div class="pain-icon">
                    <Zap class="icon" />
                  </div>
                  <div class="pain-content">
                    <p class="pain-title">快速</p>
                    <p class="pain-desc">CDN+COS加速</p>
                  </div>
                </div>
                <div class="pain-card" v-lazy-animate="{ fromY: 25, delay: 0.2 }">
                  <div class="pain-icon">
                    <UploadCloud class="icon" />
                  </div>
                  <div class="pain-content">
                    <p class="pain-title">Ctrl+V</p>
                    <p class="pain-desc">截图直接粘贴，上传拿到链接</p>
                  </div>
                </div>
                <div class="pain-card" v-lazy-animate="{ fromY: 25, delay: 0.3 }">
                  <div class="pain-icon">
                    <ShieldCheck class="icon" />
                  </div>
                  <div class="pain-content">
                    <p class="pain-title">违规图片自动拦截</p>
                    <p class="pain-desc">AI 预审 + 人工复核，省心</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="hero-visual" v-lazy-animate="{ fromY: 30, delay: 0.2, duration: 0.9 }">
              <div class="upload-panel-wrapper">
                <HeroUploadModule />
              </div>
              <div class="quick-tips">
                <div class="tip-card" v-lazy-animate="{ fromY: 15, delay: 0.4 }">
                  <Sparkles class="tip-icon tip-icon-blue" />
                  <div>
                    <p class="tip-title">直接粘贴</p>
                    <p class="tip-desc">Ctrl+V 快速上传</p>
                  </div>
                </div>
                <div class="tip-card" v-lazy-animate="{ fromY: 15, delay: 0.5 }">
                  <ShieldCheck class="tip-icon" />
                  <div>
                    <p class="tip-title">静态路由</p>
                    <p class="tip-desc">/upload/{yyyy}/{mm}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 公告区域 -->
      <section class="section-announcement" v-lazy-animate="{ fromY: 30, duration: 0.7 }">
        <div class="section-container">
          <PublicAnnouncementSpotlight />
        </div>
      </section>

      <!-- 公开图库预览 -->
      <section id="gallery" class="section-gallery" v-lazy-animate="{ fromY: 30, duration: 0.7 }">
        <div class="section-container">
          <div class="section-header-centered">
            <p class="section-eyebrow">gallery</p>
            <h2 class="section-title">公开图库</h2>
            <p class="section-desc">灵感枯竭？来这里逛逛，说不定就有新想法</p>
            <div class="stats-row">
              <div class="stat-pill" v-lazy-animate="{ fromY: 20, delay: 0.1 }">
                <span class="stat-num">{{ formattedPublicImages }}</span>
                <span class="stat-text">张公开图片</span>
              </div>
              <div class="stat-pill" v-lazy-animate="{ fromY: 20, delay: 0.2 }">
                <span class="stat-num">{{ formattedTagCount }}</span>
                <span class="stat-text">个分类标签</span>
              </div>
            </div>
          </div>
          <div class="gallery-preview" v-lazy-animate="{ fromY: 25, delay: 0.2 }">
            <PublicGalleryGrid simplified :limit="12" />
          </div>
          <div class="section-footer">
            <RouterLink to="/gallery" class="btn-ghost btn-lg">
              前往公开图库
              <ArrowRight class="btn-icon" />
            </RouterLink>
          </div>
        </div>
      </section>

      <!-- 安全与信任 -->
      <section id="security" class="section-features" v-lazy-animate="{ fromY: 30, duration: 0.7 }">
        <div class="section-container">
          <div class="trust-grid">
            <article class="trust-card trust-main" v-lazy-animate="{ fromY: 25, delay: 0.1 }">
              <div class="trust-badge">
                <ShieldCheck class="badge-icon" />
                <span>安全承诺</span>
              </div>
              <h3 class="trust-title">你的图，<br>只有你能删</h3>
              <ul class="trust-list">
                <li class="trust-item">
                  <span class="trust-dot"></span>
                  <p>AI 预审拦截违规内容，人工复核双重保障</p>
                </li>
                <li class="trust-item">
                  <span class="trust-dot"></span>
                  <p>操作日志保留 180 天，谁动了你的图一清二楚</p>
                </li>
                <li class="trust-item">
                  <span class="trust-dot"></span>
                  <p>HTTPS 全链路加密，链接即开即用</p>
                </li>
              </ul>
            </article>
            <div class="trust-side">
              <article class="trust-card trust-coming" v-lazy-animate="{ fromY: 25, delay: 0.2 }">
                <span class="coming-label">即将上线</span>
                <h4 class="coming-title">不用登录也能传</h4>
                <p class="coming-desc">访客模式 + 一次性令牌，临时传图更方便</p>
              </article>
              <article class="trust-card trust-stats" v-lazy-animate="{ fromY: 25, delay: 0.3 }">
                <div class="stats-row">
                  <div class="stat-mini">
                    <span class="stat-num">99.9%</span>
                    <span class="stat-text">可用性</span>
                  </div>
                  <div class="stat-mini">
                    <span class="stat-num">&lt;50ms</span>
                    <span class="stat-text">响应</span>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </div>
      </section>

      <!-- 联系我们 -->
      <section id="contact" class="section-contact" v-lazy-animate="{ fromY: 30, duration: 0.7 }">
        <div class="section-container">
          <div class="contact-asymmetric">
            <div class="contact-left">
              <span class="contact-label">有问题？</span>
              <h3 class="contact-title">随时找我们<br>聊聊</h3>
            </div>
            <div class="contact-right">
              <p class="contact-desc">
                不管是技术问题还是合作意向，<br>直接发邮件，24 小时内回复
              </p>
              <div class="contact-actions">
                <a href="mailto:chenxi@luminouschenxi.net" class="btn-ghost">
                  <Mail class="btn-icon" />
                  发邮件
                </a>
                <a href="https://luminouschenxi.com" target="_blank" rel="noopener noreferrer" class="btn-blue">
                  <ExternalLink class="btn-icon" />
                  了解更多
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <ChenxiGlobalFooter />
    </div>
    
    <!-- 登录弹窗 -->
    <LoginModal v-model:visible="showLoginModal" @login-success="handleLoginSuccess" @closed="handleLoginModalClosed" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { Sparkles, Search, ShieldCheck, UploadCloud, Zap, ArrowRight, Mail, ExternalLink } from 'lucide-vue-next'
import UserQuickMenu from '../components/common/UserQuickMenu.vue'
import LoginModal from '../components/chenxi/LoginModal.vue'
import { useAuthStore } from '../stores/auth'
import { fetchPublicGalleryMetrics } from '../services/gallery'
import HeroUploadModule from '../components/public/HeroUploadModule.vue'
import PublicGalleryGrid from '../components/public/PublicGalleryGrid.vue'
import PublicAnnouncementSpotlight from '../components/public/PublicAnnouncementSpotlight.vue'
import ChenxiGlobalFooter from '../components/common/ChenxiGlobalFooter.vue'
import ThemeSwitcher from '../components/common/ThemeSwitcher.vue'
import SiteLogo from '../components/common/SiteLogo.vue'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'

const router = useRouter()
const route = useRoute()
const isNavOpen = ref(false)
const showLoginModal = ref(false)
const auth = useAuthStore()

const redirectTarget = computed(() => route.query.redirect)

const openLoginModal = () => {
  if (auth.isAuthenticated) {
    router.push({ name: 'user-home' })
    return
  }
  // 强制打开弹窗，无论当前路由状态如何
  showLoginModal.value = true
  // 如果当前没有 login 查询参数，添加它
  if (route.query.login === undefined) {
    router.replace({ path: '/', query: { ...route.query, login: '1' } })
  }
}

const handleLoginSuccess = () => {
  showLoginModal.value = false
  const redirect = redirectTarget.value
  if (redirect && typeof redirect === 'string') {
    router.push(redirect)
  } else {
    router.push({ name: 'user-home' })
  }
}

const handleLoginModalClosed = () => {
  // 清除 login 查询参数
  if (route.query.login !== undefined) {
    const { login, ...restQuery } = route.query
    router.replace({ path: '/', query: restQuery })
  }
}

const logout = () => {
  auth.logout()
  isNavOpen.value = false
}

const userMenuItems = [
  { label: '仪表盘', route: { name: 'user-home' } },
  { label: '媒体管理', route: { name: 'user-images' } },
  { label: '资料信息', route: { name: 'user-profile' } },
  { label: '安全设置', route: { name: 'user-security' } },
]

const metrics = ref({
  totalPublicImages: null,
  totalTags: null,
})

const formatNumber = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }
  return value.toLocaleString('zh-CN')
}

const formattedPublicImages = computed(() => formatNumber(metrics.value.totalPublicImages))
const formattedTagCount = computed(() => formatNumber(metrics.value.totalTags))

const loadMetrics = async () => {
  try {
    const response = await fetchPublicGalleryMetrics()
    if (response) {
      metrics.value = {
        totalPublicImages: response.totalPublicImages ?? null,
        totalTags: response.totalTags ?? null,
      }
    }
  } catch (error) {
    console.error('Failed to fetch public gallery metrics', error)
  }
}

onMounted(() => {
  loadMetrics()
  if (route.query.login !== undefined) {
    openLoginModal()
  }
})

watch(
  () => route.query.login,
  (val) => {
    if (val !== undefined) {
      openLoginModal()
    }
  }
)
</script>

<style scoped>
/* 背景系统 */
.landing-page {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

.landing-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

/* 三色光球系统 - 新配色 #FADCE9 #AED0ED #f0fbf4 (60%覆盖，40%留白) */
.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.6;
  animation: float 25s ease-in-out infinite;
}

/* 柔粉色 #FADCE9 - 面积最大 (~30%)，左上区域 */
.orb-pink {
  width: 700px;
  height: 700px;
  background: radial-gradient(circle at 30% 30%, 
    rgba(250, 220, 233, 0.85) 0%, 
    rgba(250, 220, 233, 0.55) 30%, 
    rgba(250, 220, 233, 0.25) 60%,
    transparent 85%);
  top: -200px;
  left: -120px;
  animation-delay: 0s;
}

/* 淡蓝色 #AED0ED - 面积中等 (~20%)，右上区域 */
.orb-blue {
  width: 550px;
  height: 550px;
  background: radial-gradient(circle at 70% 30%, 
    rgba(174, 208, 237, 0.8) 0%, 
    rgba(174, 208, 237, 0.5) 30%, 
    rgba(174, 208, 237, 0.2) 60%,
    transparent 85%);
  top: -80px;
  right: -80px;
  animation-delay: -8s;
}

/* 薄荷绿色 #f0fbf4 - 面积较小 (~10%)，右下区域 */
.orb-mint {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle at 50% 50%, 
    rgba(240, 251, 244, 0.75) 0%, 
    rgba(220, 245, 230, 0.45) 30%, 
    rgba(200, 235, 215, 0.18) 60%,
    transparent 85%);
  bottom: 5%;
  right: 10%;
  animation-delay: -16s;
}

/* 过渡混合层 - 柔和交融效果 */
.gradient-blend {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.4;
  animation: pulse 15s ease-in-out infinite;
}

/* 柔粉与淡蓝过渡 */
.blend-pink-blue {
  width: 450px;
  height: 450px;
  background: linear-gradient(135deg, 
    rgba(250, 220, 233, 0.5) 0%, 
    rgba(174, 208, 237, 0.5) 100%);
  top: 5%;
  left: 30%;
  animation-delay: -5s;
}

/* 淡蓝与薄荷绿过渡 */
.blend-blue-mint {
  width: 380px;
  height: 380px;
  background: linear-gradient(225deg, 
    rgba(174, 208, 237, 0.45) 0%, 
    rgba(240, 251, 244, 0.45) 100%);
  top: 25%;
  right: 15%;
  animation-delay: -10s;
}

/* 薄荷绿与柔粉过渡 */
.blend-mint-pink {
  width: 400px;
  height: 400px;
  background: linear-gradient(45deg, 
    rgba(240, 251, 244, 0.45) 0%, 
    rgba(250, 220, 233, 0.45) 100%);
  bottom: 15%;
  left: 15%;
  animation-delay: -15s;
}

/* 微光叠加层 */
.shimmer-overlay {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 20% 25%, rgba(250, 220, 233, 0.12) 0%, transparent 45%),
    radial-gradient(ellipse at 80% 20%, rgba(174, 208, 237, 0.1) 0%, transparent 40%),
    radial-gradient(ellipse at 70% 80%, rgba(240, 251, 244, 0.08) 0%, transparent 35%);
  animation: shimmer 20s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { 
    opacity: 0.4; 
    transform: scale(1); 
  }
  50% { 
    opacity: 0.6; 
    transform: scale(1.1); 
  }
}

@keyframes shimmer {
  0%, 100% { 
    opacity: 0.8; 
  }
  50% { 
    opacity: 1; 
  }
}

.noise-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 10px) scale(1.02); }
}

/* 导航栏 */
.landing-header {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  transition: all 0.3s ease;
}

.dark .landing-header {
  background: var(--glass-bg);
  border-bottom-color: var(--border-soft);
}

.header-inner {
  max-width: 1400px;
}

.logo-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(127, 123, 255, 0.15), rgba(255, 95, 143, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
}

@media (min-width: 768px) {
  .logo-wrapper {
    width: 44px;
    height: 44px;
    border-radius: 14px;
    padding: 8px;
  }
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-text {
  line-height: 1.2;
}

.logo-title {
  font-size: 1.1rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-subtitle {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.nav-link {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: color 0.2s ease;
  position: relative;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #ff6b9d, #4ecdc4);
  transition: width 0.3s ease;
}

.nav-link:hover {
  color: var(--color-text-primary);
}

.nav-link:hover::after {
  width: 100%;
}

/* 按钮系统 - 使用全局样式，这里只添加特定调整 */
.btn-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

.btn-lg .btn-icon {
  width: 1.5rem;
  height: 1.5rem;
}

/* 移动端菜单 */
.menu-toggle {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-toggle:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

@media (min-width: 768px) {
  .menu-toggle {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    font-size: 1.2rem;
  }
}

/* 移动端登录按钮 */
.btn-login-mobile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.375rem 0.875rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, #ff6b9d, #feca57);
  color: white;
  font-size: 0.8125rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 3px 12px rgba(255, 107, 157, 0.35);
}

.btn-login-mobile:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 16px rgba(255, 107, 157, 0.45);
}

.dark .btn-login-mobile {
  background: linear-gradient(135deg, #e55a8a, #e5b54d);
  box-shadow: 0 3px 12px rgba(229, 90, 138, 0.35);
}

.mobile-nav {
  background: var(--color-bg-primary);
  backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-soft);
  max-height: calc(100vh - 70px);
  overflow-y: auto;
}

.mobile-nav-inner {
  padding: 1rem 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all 0.2s ease;
}

.mobile-nav-link:hover {
  background: rgba(255, 107, 157, 0.08);
  color: var(--color-brand-primary);
}

.mobile-nav-icon {
  font-size: 1.25rem;
  width: 1.5rem;
  text-align: center;
}

.mobile-nav-divider {
  height: 1px;
  background: var(--border-soft);
  margin: 0.5rem 0;
}

.mobile-nav-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-top: 0.5rem;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Hero Section */
.landing-main {
  padding-top: 100px;
}

.hero-section {
  padding: 4rem 0 6rem;
}

.hero-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: start;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 新标签样式 */
.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: color-mix(in srgb, var(--color-brand-primary) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 25%, transparent);
  border-radius: 999px;
  width: fit-content;
}

.tag-pulse {
  width: 8px;
  height: 8px;
  background: var(--color-brand-primary);
  border-radius: 50%;
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
}

.tag-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-brand-accent);
}

.hero-title {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.title-line {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.1;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
}

.title-accent {
  color: var(--color-brand-primary);
  font-weight: 800;
}

.hero-description {
  font-size: 1.1rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
  max-width: 480px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

/* 痛点卡片 - 对称网格布局 */
.pain-points-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-top: 1.5rem;
}

.pain-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--glass-bg);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.pain-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--color-brand-primary) 40%, transparent);
  box-shadow: 0 8px 25px color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
}

.pain-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pain-icon .icon {
  width: 20px;
  height: 20px;
  color: var(--color-brand-primary);
}

/* 蓝色图标样式 */
.pain-icon-blue {
  background: color-mix(in srgb, var(--color-info) 15%, transparent);
}

.pain-icon-blue .icon {
  color: var(--color-info);
}

.tip-icon-blue {
  color: var(--color-info) !important;
}

.pain-content {
  flex: 1;
}

.pain-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 0.25rem;
}

.pain-desc {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

/* Hero Visual */
.hero-visual {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.upload-panel-wrapper {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.quick-tips {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.tip-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.875rem;
  transition: all 0.3s ease;
}

.tip-card:hover {
  border-color: var(--color-brand-primary);
  transform: translateY(-2px);
}

.tip-icon {
  width: 20px;
  height: 20px;
  color: var(--color-brand-primary);
  flex-shrink: 0;
}

.tip-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.tip-desc {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

/* Section 通用样式 */
.section-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.section-announcement {
  padding: 2rem 0;
}

/* Gallery Section */
.section-gallery {
  padding: 5rem 0;
}

/* 居中对齐的 section header */
.section-header-centered {
  text-align: center;
  margin-bottom: 2.5rem;
}

.section-header-centered .section-eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--text-soft);
  margin-bottom: 0.75rem;
}

.section-header-centered .section-title {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 0.75rem;
}

.section-header-centered .section-desc {
  font-size: 1rem;
  color: var(--color-text-secondary);
  margin-bottom: 1.5rem;
}

.stats-row {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  background: color-mix(in srgb, var(--color-brand-primary) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 25%, transparent);
  border-radius: 999px;
}

.stat-num {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-brand-primary);
}

.stat-text {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}



.stat-value {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-brand-primary);
}

.stat-label {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
}

.gallery-preview {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 28px;
  padding: 2rem;
  box-shadow: var(--shadow-card);
}

.section-footer {
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
}

/* Features Section - 信任区域 */
.section-features {
  padding: 5rem 0;
}

/* 不对称网格布局 */
.trust-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 1.5rem;
}

.trust-main {
  background: var(--glass-bg);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
  border-radius: 28px;
  padding: 2.5rem;
  transition: all 0.3s ease;
}

.trust-main:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px color-mix(in srgb, var(--color-brand-primary) 10%, transparent);
}

.trust-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: color-mix(in srgb, var(--color-brand-primary) 12%, transparent);
  border-radius: 999px;
  margin-bottom: 1.5rem;
}

.badge-icon {
  width: 16px;
  height: 16px;
  color: var(--color-brand-primary);
}

.trust-badge span {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-brand-accent);
}

.trust-title {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.2;
  margin-bottom: 1.5rem;
  letter-spacing: -0.02em;
}

.trust-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.trust-item {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  font-size: 0.95rem;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.trust-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 0.5rem;
  flex-shrink: 0;
  background: var(--color-brand-primary);
}

/* 侧边卡片 */
.trust-side {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.trust-coming {
  background: var(--glass-bg);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
  border-radius: 24px;
  padding: 1.75rem;
  flex: 1;
}

.coming-label {
  display: inline-block;
  padding: 0.35rem 0.75rem;
  background: color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-brand-accent);
  margin-bottom: 1rem;
}

.coming-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 0.5rem;
}

.coming-desc {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.trust-stats {
  background: color-mix(in srgb, var(--color-brand-primary) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
  border-radius: 24px;
  padding: 1.5rem;
}

.stats-row {
  display: flex;
  gap: 2rem;
}

.stat-mini {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-mini .stat-num {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-brand-primary);
}

.stat-mini .stat-text {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.block-desc {
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.roadmap-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.roadmap-tag {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-brand-primary);
  background: rgba(127, 123, 255, 0.1);
  border: 1px solid rgba(127, 123, 255, 0.2);
}

/* Contact Section */
.section-contact {
  padding: 3rem 0 6rem;
}

/* 联系区域 - 不对称布局 */
.contact-asymmetric {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 3rem;
  align-items: center;
  background: var(--glass-bg);
  border: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
  border-radius: 28px;
  padding: 3rem;
}

.contact-left {
  padding-right: 2rem;
  border-right: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
}

.contact-label {
  display: inline-block;
  padding: 0.4rem 0.875rem;
  background: color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-brand-accent);
  margin-bottom: 1rem;
}

.contact-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.contact-right {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.contact-desc {
  font-size: 1.1rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.contact-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 3rem;
  }
  
  .hero-visual {
    order: -1;
  }
  
  .title-line {
    font-size: 3rem;
  }
  
  .trust-grid {
    grid-template-columns: 1fr;
  }
  
  .contact-asymmetric {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
  
  .contact-left {
    padding-right: 0;
    border-right: none;
    border-bottom: 1px solid color-mix(in srgb, var(--color-brand-primary) 20%, transparent);
    padding-bottom: 2rem;
  }
}

@media (max-width: 768px) {
  .landing-main {
    padding-top: 80px;
  }
  
  .hero-section {
    padding: 2rem 0 4rem;
  }
  
  .title-line {
    font-size: 2.5rem;
  }
  
  /* 痛点卡片移动端单列 */
  .pain-points-grid {
    grid-template-columns: 1fr;
  }
  
  .quick-tips {
    grid-template-columns: 1fr;
  }
  
  .section-header-centered .section-title {
    font-size: 1.75rem;
  }
  
  .contact-asymmetric {
    padding: 2rem;
  }
  
  .contact-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 480px) {
  .hero-actions {
    flex-direction: column;
  }
  
  .btn-lg {
    width: 100%;
  }
  
  .gallery-preview {
    padding: 1rem;
  }
  
  .stats-right {
    flex-direction: column;
  }
  
  .stats-row {
    flex-direction: column;
    gap: 0.75rem;
  }
}

/* ==================== 深色主题配色 ====================

/* 深色主题 - 标题强调色 */
.dark .title-accent {
  color: var(--color-brand-accent);
  font-weight: 800;
}

/* 深色主题 - 统计数字 */
.dark .stat-value {
  color: var(--color-brand-accent);
}

/* 深色主题 - 主要按钮 */
.dark .btn-primary {
  background: var(--color-brand-accent);
  box-shadow: 0 4px 20px color-mix(in srgb, var(--color-brand-accent) 40%, transparent);
}

.dark .btn-primary:hover {
  background: var(--color-brand-primary);
  box-shadow: 0 8px 30px color-mix(in srgb, var(--color-brand-accent) 50%, transparent);
}

/* 深色主题 - 发送验证码按钮 (Element Plus 样式覆盖) */
.dark .el-button--primary {
  background: var(--color-brand-accent) !important;
  border-color: var(--color-brand-accent) !important;
  color: var(--color-on-accent) !important;
}

.dark .el-button--primary:hover {
  background: var(--color-brand-primary) !important;
  border-color: var(--color-brand-primary) !important;
}

.dark .el-button--primary.is-plain {
  background: transparent !important;
  border-color: var(--color-brand-accent) !important;
  color: var(--color-brand-accent) !important;
}

.dark .el-button--primary.is-plain:hover {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent) !important;
  border-color: var(--color-brand-primary) !important;
  color: var(--color-brand-primary) !important;
}

/* 深色主题 - 使用CSS变量 */
.dark .landing-page {
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
}

/* 深色主题 - 隐藏渐变光球 */
.dark .landing-bg {
  display: none;
}

.dark .hero-description {
  color: var(--color-text-secondary);
}

.dark .section-desc {
  color: var(--color-text-secondary);
}

.dark .feature-desc {
  color: var(--color-text-muted);
}

.dark .contact-desc {
  color: var(--color-text-secondary);
}

/* 深色主题 - 次要文字 */
.dark .stat-label,
.dark .feature-label,
.dark .section-eyebrow,
.dark .contact-eyebrow {
  color: var(--color-text-muted);
}

/* 深色主题 - 卡片背景调整 */
.dark .stat-card,
.dark .tip-card {
  background: var(--panel-overlay);
  border-color: var(--border-soft);
}

/* 深色主题 - 新组件样式 */
.dark .pain-card {
  background: var(--panel-overlay);
  border-color: color-mix(in srgb, var(--color-brand-accent) 20%, transparent);
}

.dark .pain-card:hover {
  border-color: color-mix(in srgb, var(--color-brand-accent) 40%, transparent);
  box-shadow: 0 4px 20px color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .pain-icon {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .pain-icon .icon {
  color: var(--color-brand-accent);
}

.dark .trust-main,
.dark .trust-coming {
  background: var(--panel-overlay);
  border-color: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .trust-main:hover {
  box-shadow: 0 20px 40px color-mix(in srgb, var(--color-brand-accent) 10%, transparent);
}

.dark .trust-badge {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .trust-badge span {
  color: var(--color-brand-primary);
}

.dark .trust-dot {
  background: var(--color-brand-accent);
}

.dark .trust-stats {
  background: color-mix(in srgb, var(--color-brand-accent) 8%, transparent);
  border-color: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .stat-mini .stat-num {
  color: var(--color-brand-accent);
}

.dark .coming-label {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
  color: var(--color-brand-primary);
}

.dark .contact-asymmetric {
  background: var(--panel-overlay);
  border-color: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
}

.dark .contact-left {
  border-color: color-mix(in srgb, var(--color-brand-accent) 20%, transparent);
}

.dark .contact-label {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
  color: var(--color-brand-primary);
}

.dark .hero-tag {
  background: color-mix(in srgb, var(--color-brand-accent) 12%, transparent);
  border-color: color-mix(in srgb, var(--color-brand-accent) 25%, transparent);
}

.dark .tag-text {
  color: var(--color-brand-primary);
}

.dark .gallery-preview {
  background: var(--panel-overlay);
  border-color: var(--border-soft);
}

/* 深色主题 - 导航链接 */
.dark .nav-link {
  color: var(--color-text-secondary);
}

.dark .nav-link:hover {
  color: var(--color-brand-accent);
}

/* 深色主题 - Ghost 按钮 */
.dark .btn-ghost {
  color: var(--color-text-secondary);
  border-color: var(--border-soft);
}

.dark .btn-ghost:hover {
  color: var(--color-brand-accent);
  border-color: var(--color-brand-accent);
  background: color-mix(in srgb, var(--color-brand-accent) 10%, transparent);
}

/* 深色主题 - Logo 文字 */
.dark .logo-title {
  background: linear-gradient(135deg, var(--color-brand-accent), var(--color-brand-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dark .logo-subtitle {
  color: var(--color-text-muted);
}

/* 深色主题 - 特性图标背景 */
.dark .feature-icon-wrapper.icon-purple {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
  color: var(--color-brand-accent);
}

.dark .feature-icon-wrapper.icon-pink {
  background: color-mix(in srgb, var(--color-brand-primary) 15%, transparent);
  color: var(--color-brand-primary);
}

.dark .feature-icon-wrapper.icon-green {
  background: color-mix(in srgb, var(--color-success) 15%, transparent);
  color: var(--color-success);
}

/* 深色主题 - 列表项圆点 */
.dark .item-dot.dot-purple {
  background: var(--color-brand-accent);
}

.dark .item-dot.dot-pink {
  background: var(--color-brand-primary);
}

.dark .item-dot.dot-green {
  background: var(--color-success);
}

/* 深色主题 - Roadmap 标签 */
.dark .roadmap-tag {
  background: color-mix(in srgb, var(--color-brand-accent) 15%, transparent);
  color: var(--color-brand-accent);
  border-color: color-mix(in srgb, var(--color-brand-accent) 30%, transparent);
}

.dark .roadmap-tag:hover {
  background: color-mix(in srgb, var(--color-brand-accent) 25%, transparent);
  border-color: var(--color-brand-accent);
}
</style>
