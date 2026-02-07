<template>
  <div class="relative min-h-screen overflow-hidden bg-surface-body text-white">
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(127,123,255,0.35),transparent_55%)]"></div>
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_80%_0%,rgba(255,95,143,0.35),transparent_45%)]"></div>

    <header class="fixed inset-x-0 top-0 z-40 border-b border-white/10 bg-surface-panel/80 backdrop-blur-2xl">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 p-2">
            <img :src="siteLogo" alt="AstrNest 徽标" class="h-full w-full rounded-xl object-contain drop-shadow-lg" />
          </div>

          <div>
            <p class="text-lg font-semibold">AstrNest</p>
            <p class="text-xs text-white/60">图床系统</p>
          </div>
        </div>
        <nav class="hidden items-center gap-8 text-sm text-white/70 md:flex">
          <a href="#upload" class="transition hover:text-white">即时上传</a>
          <a href="#gallery" class="transition hover:text-white">公开图库</a>
          <a href="#security" class="transition hover:text-white">安全合规</a>
          <a href="#contact" class="transition hover:text-white">联系我们</a>
          <RouterLink to="/search" class="transition hover:text-white">标签搜索</RouterLink>
        </nav>
        <div v-if="!auth.isAuthenticated" class="flex items-center gap-3">
          <RouterLink
            to="/login"
            class="chenxi-landing-btn rounded-full border border-white/20 px-5 py-2 text-sm font-medium text-white/80 transition hover:border-brand-primary hover:text-white"
          >
            登录
          </RouterLink>
          <RouterLink
            to="/register"
            class="chenxi-landing-btn inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-brand-primary to-brand-accent px-5 py-2 text-sm font-semibold text-white shadow-[0_10px_30px_rgba(127,123,255,0.35)] transition hover:translate-y-0.5"
          >
            注册
          </RouterLink>
        </div>
        <div v-else class="flex items-center gap-3">
          <RouterLink
            to="/user"
            class="chenxi-landing-btn rounded-full border border-white/20 px-5 py-2 text-sm font-medium text-white/80 transition hover:border-brand-primary hover:text-white"
          >
            会员中心
          </RouterLink>
          <el-dropdown class="chenxi-user-dropdown" trigger="hover">
            <div class="chenxi-user-avatar flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-primary to-brand-accent text-white font-semibold transition-transform hover:scale-110">
              <User class="h-5 w-5" />
            </div>
            <template #dropdown>
              <el-dropdown-menu class="chenxi-dropdown-menu bg-surface-panel border border-white/10">
                <RouterLink
                  v-for="item in userMenuItems"
                  :key="item.label"
                  :to="item.route"
                  class="block w-full"
                >
                  <el-dropdown-item class="text-white/80 hover:text-white hover:bg-white/10">
                    {{ item.label }}
                  </el-dropdown-item>
                </RouterLink>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="relative z-10 pt-28 pb-16">
      <section
        id="upload"
        class="mx-auto grid max-w-6xl grid-cols-1 gap-12 px-6 py-12 md:grid-cols-2"
        v-lazy-animate="{ fromY: 32, duration: 0.7 }"
      >
        <div class="space-y-8">
          <p class="text-xs uppercase tracking-[0.45em] text-white/50">Luminouscx · 辰汐</p>
          <div class="space-y-4">
            <h1 class="text-4xl font-semibold leading-tight text-gradient md:text-6xl">
              AstrNest
              <br/>
              
            </h1>
            <p class="text-base leading-relaxed text-white/75">
              Modern full-stack image hosting platform built with Spring Boot 3.4.1 and Vue 3.
            </p>
          </div>
          <div class="flex flex-wrap gap-4">
            <RouterLink
              to="/user"
              class="inline-flex items-center gap-2 rounded-full bg-white/10 px-6 py-3 text-sm font-semibold text-white transition hover:bg-white/20"
            >
              <Sparkles class="h-4 w-4" />
              立即登录
            </RouterLink>
            <RouterLink
              to="/search"
              class="inline-flex items-center gap-2 rounded-full border border-white/20 px-6 py-3 text-sm font-semibold text-white/80 transition hover:border-white hover:text-white"
            >
              <Search class="h-4 w-4" />
              标签搜索
            </RouterLink>
            <a
              href="#gallery"
              class="inline-flex items-center gap-2 rounded-full border border-white/20 px-6 py-3 text-sm font-semibold text-white/80 transition hover:border-white hover:text-white"
            >
              <ShieldCheck class="h-4 w-4" />
              查看公开图片
            </a>
          </div>
          <div class="glass-panel mt-4 flex items-start gap-4 rounded-3xl border border-white/10 bg-white/5 p-4 text-left">
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-primary to-brand-accent text-white shadow-lg">
              <Zap class="h-5 w-5" />
            </div>
            <div class="space-y-1 text-sm">
              <p class="text-xs uppercase tracking-[0.35em] text-white/50">Auto Cleanup</p>
              <p class="text-base font-semibold text-white">自动清理提示</p>
              <p class="text-xs text-white/70">
                AstrNest 会每日巡检公开图库，自动清理违规、超期或未使用的临时文件，保障首页展示始终纯净、安全。
              </p>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="glass-panel space-y-3 p-5">
              <p class="text-xs uppercase tracking-[0.35em] text-white/50">多云存储+CDN</p>
              <p class="text-3xl font-semibold text-gradient">COS+CDN</p>
              <p class="flex items-center gap-2 text-xs text-white/60">
                <UploadCloud class="h-4 w-4" />
                同时支持本地、阿里云、腾讯云、七牛、OneDrive 等驱动
              </p>
            </div>
            <div class="glass-panel space-y-3 p-5">
              <p class="text-xs uppercase tracking-[0.35em] text-white/50">图片治理</p>
              <p class="text-3xl font-semibold text-gradient">AI + 审核</p>
              <p class="flex items-center gap-2 text-xs text-white/60">
                <ShieldCheck class="h-4 w-4" />
                请勿乱传违规/色情/低俗/政治/未知二码等图片！谢谢配合！
              </p>
            </div>
          </div>
        </div>
        <div class="relative">
          <div class="absolute inset-0 blur-3xl">
            <div class="h-full w-full rounded-[40px] bg-gradient-to-br from-brand-primary/25 via-white/10 to-brand-accent/25"></div>
          </div>
          <div class="relative">
            <HeroUploadModule />
            <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div class="glass-panel flex items-center justify-between rounded-3xl border-white/10 bg-white/5 px-4 py-4 text-left">
                <div>
                  <p class="text-xs uppercase tracking-[0.35em] text-white/50">Clipboard</p>
                  <p class="text-base font-semibold text-white">直接粘贴</p>
                  <p class="text-xs text-white/60">快捷键curl+V上传剪贴板图片</p>
                </div>
                <Sparkles class="h-6 w-6 text-brand-primary" />
              </div>
              <div class="glass-panel flex items-center justify-between rounded-3xl border-white/10 bg-white/5 px-4 py-4 text-left">
                <div>
                  <p class="text-xs uppercase tracking-[0.35em] text-white/50">Static route</p>
                  <p class="text-base font-semibold text-white">地址参考</p>
                  <p class="text-xs text-white/60">网址+/upload/{yyyy}/{mm}</p>
                </div>
                <ShieldCheck class="h-6 w-6 text-brand-emerald" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="gallery" class="mx-auto max-w-6xl px-6 py-12" v-lazy-animate="{ fromY: 28, duration: 0.6 }">
        <div class="glass-panel border-white/10 bg-white/5 p-8">
          <div class="flex flex-col gap-8 md:flex-row md:items-center md:justify-between">
            <div class="space-y-4">
              <p class="text-xs uppercase tracking-[0.35em] text-white/50">Public showcase</p>
              <h2 class="text-3xl font-semibold text-white">公开图片</h2>
              <p class="text-sm leading-relaxed text-white/70">
                以下列表是网友上传的部分公开图片
              </p>
            </div>
            <div class="grid grid-cols-2 gap-4 text-center text-sm font-semibold">
              <div class="rounded-3xl border border-white/10 bg-white/10 px-6 py-5">
                <p class="text-xs uppercase tracking-wide text-white/60">公开的图片</p>
                <p class="text-3xl text-gradient">{{ formattedPublicImages }}</p>
              </div>
              <div class="rounded-3xl border border-white/10 bg-white/10 px-6 py-5">
                <p class="text-xs uppercase tracking-wide text-white/60">图片已知标签</p>
                <p class="text-3xl text-gradient">{{ formattedTagCount }}</p>
              </div>
            </div>
          </div>
        </div>
        <div class="mt-8">
          <PublicGalleryGrid />
        </div>
      </section>

      <section id="security" class="mx-auto max-w-6xl px-6 py-12" v-lazy-animate="{ fromY: 28, duration: 0.6 }">
        <div class="grid gap-6 md:grid-cols-2">
          <article class="glass-panel border-white/10 bg-white/5 p-6">
            <p class="text-xs uppercase tracking-[0.35em] text-white/50">Trust first</p>
            <h3 class="mt-3 text-2xl font-semibold text-white">多层安全与审计</h3>
            <ul class="mt-4 space-y-3 text-sm text-white/70">
              <li class="flex items-start gap-2">
                <span class="mt-1 h-2 w-2 rounded-full bg-brand-primary"></span>
                <p>违规检测流水线实时调用 AI+规则引擎，保障公共图库不出现违禁素材。</p>
              </li>
              <li class="flex items-start gap-2">
                <span class="mt-1 h-2 w-2 rounded-full bg-brand-accent"></span>
                <p>审计日志与 API Key 日志可回溯 180 天，配合运营中心差异化提醒。</p>
              </li>
              <li class="flex items-start gap-2">
                <span class="mt-1 h-2 w-2 rounded-full bg-brand-emerald"></span>
                <p> /upload/{year}/{month}：绕过多级跳转并保留 HTTPS 证据链，与后端静态映射同步，落地即可访问。</p>
              </li>
            </ul>
          </article>
          <article class="glass-panel border-white/10 bg-white/5 p-6">
            <p class="text-xs uppercase tracking-[0.35em] text-white/50">Roadmap</p>
            <h3 class="mt-3 text-2xl font-semibold text-white">下一阶段能力</h3>
            <p class="mt-4 text-sm leading-relaxed text-white/70">
              公共首页会陆续上线粘贴板极速上传、批量授权、以及访客级访问控制，便于用户无需登录即可完成轻量投递，但仍通过一次性令牌追踪行为。
            </p>
          </article>
        </div>
      </section>

      <section id="contact" class="mx-auto max-w-6xl px-6 pb-8">
        <div class="glass-panel flex flex-col gap-6 border-white/10 bg-white/5 p-6 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.35em] text-white/50">Talk to us</p>
            <h3 class="mt-2 text-2xl font-semibold text-white">加入我们？我们可以为提供：</h3>
            <p class="mt-2 text-sm text-white/70">
              <br>
              • 存储与加速服务：专业搭建 CDN、OSS、OBS、COS 等云存储与内容分发网络。
              <br>
              • 智能升级：为您的项目集成并升级 AI-Agent 能力。
              <br></br>
              • 个性化域名：申请个性化的网站子域名，展现您的独特风格。
            </p>
          </div>
          <div class="flex flex-wrap gap-4">
            <a
              href="mailto:chenxi@luminouschenxi.net"
              class="inline-flex items-center rounded-full border border-white/20 px-6 py-3 text-sm font-semibold text-white/80 transition hover:border-white hover:text-white"
            >
              邮箱
            </a>
            <a
              href="https://luminouschenxi.com"
              target="_blank"
              rel="noopener noreferrer"
              class="rounded-full bg-gradient-to-r from-brand-primary to-brand-accent px-6 py-3 text-sm font-semibold text-white shadow-[0_10px_30px_rgba(255,95,143,0.35)] transition hover:translate-y-0.5"
            >
              预约演示
            </a>
          </div>
        </div>
      </section>
    </main>
    <ChenxiGlobalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Search, ShieldCheck, Sparkles, UploadCloud, Zap, User } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { fetchPublicGalleryMetrics } from '../services/gallery'
import HeroUploadModule from '../components/public/HeroUploadModule.vue'
import PublicGalleryGrid from '../components/public/PublicGalleryGrid.vue'
import ChenxiGlobalFooter from '../components/common/ChenxiGlobalFooter.vue'
import siteLogo from '../assets/img/favicon.png'
import '../assets/styles/chenxi-transitions.css'
import '../assets/styles/chenxi-interactions.css'

const auth = useAuthStore()
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

onMounted(loadMetrics)
</script>
