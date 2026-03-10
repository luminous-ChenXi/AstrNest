<script setup>
import { RouterLink } from 'vue-router'
import { CheckCircle2, Sparkles, ArrowRight, Home, Shield, Zap, Lock, Sun, Moon, PartyPopper, Star, Trophy } from 'lucide-vue-next'
import { ref, onMounted, onUnmounted } from 'vue'

const isDark = ref(true)
const STORAGE_KEY = 'chenxi-theme'
const fireworks = ref([])
const particles = ref([])
let animationId = null

// 从 localStorage 获取主题设置
const getStoredTheme = () => {
  if (typeof window === 'undefined') return true
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored !== null) {
    return stored === 'dark'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 保存主题设置
const storeTheme = (dark) => {
  if (typeof window === 'undefined') return
  localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light')
}

// 初始化主题
const initTheme = () => {
  isDark.value = getStoredTheme()
}

// 手动切换主题
const toggleTheme = () => {
  isDark.value = !isDark.value
  storeTheme(isDark.value)
}

// 烟花粒子类
class Particle {
  constructor(x, y, color, velocity) {
    this.x = x
    this.y = y
    this.color = color
    this.velocity = velocity
    this.alpha = 1
    this.decay = Math.random() * 0.015 + 0.01
    this.gravity = 0.1
    this.size = Math.random() * 3 + 2
  }

  update() {
    this.velocity.x *= 0.98
    this.velocity.y *= 0.98
    this.velocity.y += this.gravity
    this.x += this.velocity.x
    this.y += this.velocity.y
    this.alpha -= this.decay
  }

  draw(ctx) {
    ctx.save()
    ctx.globalAlpha = this.alpha
    ctx.fillStyle = this.color
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
    
    // 发光效果
    ctx.shadowBlur = 10
    ctx.shadowColor = this.color
    ctx.fill()
    ctx.restore()
  }
}

// 创建烟花
const createFirework = (x, y) => {
  const colors = [
    '#f472b6', '#a78bfa', '#60a5fa', '#34d399', '#fbbf24', 
    '#f87171', '#818cf8', '#2dd4bf', '#fb923c', '#e879f9'
  ]
  const particleCount = 30 + Math.random() * 20
  
  for (let i = 0; i < particleCount; i++) {
    const angle = (Math.PI * 2 / particleCount) * i + Math.random() * 0.5
    const velocity = Math.random() * 6 + 3
    const color = colors[Math.floor(Math.random() * colors.length)]
    
    particles.value.push(new Particle(
      x,
      y,
      color,
      {
        x: Math.cos(angle) * velocity,
        y: Math.sin(angle) * velocity
      }
    ))
  }
}

// 自动发射烟花
const autoFireworks = () => {
  const canvas = document.getElementById('fireworks-canvas')
  if (!canvas) return
  
  const x = Math.random() * canvas.width
  const y = Math.random() * (canvas.height * 0.6) + canvas.height * 0.1
  createFirework(x, y)
}

// 动画循环
const animate = () => {
  const canvas = document.getElementById('fireworks-canvas')
  if (!canvas) return
  
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = isDark.value 
    ? 'rgba(5, 6, 12, 0.1)' 
    : 'rgba(250, 250, 250, 0.1)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  particles.value = particles.value.filter(particle => {
    particle.update()
    particle.draw(ctx)
    return particle.alpha > 0
  })
  
  animationId = requestAnimationFrame(animate)
}

// 初始化画布
const initCanvas = () => {
  const canvas = document.getElementById('fireworks-canvas')
  if (!canvas) return
  
  const resizeCanvas = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  
  // 启动动画
  animate()
  
  // 自动发射烟花
  const fireworkInterval = setInterval(() => {
    if (Math.random() > 0.3) {
      autoFireworks()
    }
  }, 800)
  
  // 初始发射几个烟花
  setTimeout(() => autoFireworks(), 100)
  setTimeout(() => autoFireworks(), 400)
  setTimeout(() => autoFireworks(), 700)
  
  return () => {
    clearInterval(fireworkInterval)
    window.removeEventListener('resize', resizeCanvas)
  }
}

onMounted(() => {
  initTheme()
  
  // 监听 storage 事件
  window.addEventListener('storage', (e) => {
    if (e.key === STORAGE_KEY) {
      isDark.value = e.newValue === 'dark'
    }
  })
  
  // 初始化烟花
  const cleanup = initCanvas()
  
  onUnmounted(() => {
    if (animationId) {
      cancelAnimationFrame(animationId)
    }
    cleanup && cleanup()
  })
})
</script>

<template>
  <div class="register-success-page" :class="{ 'light': !isDark }">
    <!-- Fireworks Canvas -->
    <canvas id="fireworks-canvas" class="fireworks-canvas"></canvas>
    
    <!-- Background Effects -->
    <div class="bg-effects">
      <div class="bg-gradient-1" />
      <div class="bg-gradient-2" />
      <div class="bg-grid" />
      <div class="bg-glow" />
    </div>

    <!-- Theme Toggle -->
    <button class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
      <Sun v-if="isDark" class="theme-icon" />
      <Moon v-else class="theme-icon" />
    </button>

    <!-- Content Container -->
    <div class="content-container">
      <!-- Success Card -->
      <div class="success-card">
        <!-- Celebration Badge -->
        <div class="celebration-badge">
          <PartyPopper class="celebration-icon" />
        </div>
        
        <!-- Icon Animation -->
        <div class="icon-wrapper">
          <div class="icon-bg">
            <CheckCircle2 class="success-icon" />
          </div>
          <div class="sparkles">
            <Sparkles class="sparkle sparkle-1" />
            <Sparkles class="sparkle sparkle-2" />
            <Sparkles class="sparkle sparkle-3" />
          </div>
          <div class="floating-stars">
            <Star class="star star-1" />
            <Star class="star star-2" />
            <Star class="star star-3" />
            <Trophy class="star star-4" />
          </div>
        </div>

        <!-- Text Content -->
        <div class="text-content">
          <span class="badge">
            <span class="badge-line"></span>
            CHENXI SIGNUP
            <span class="badge-line"></span>
          </span>
          <h1 class="title">
            <span class="title-highlight">恭喜！</span>
            注册成功
          </h1>
          <p class="subtitle">您的账号已创建成功，现在可以登录并开始使用辰汐图床了。</p>
        </div>

        <!-- Feature Highlights -->
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <Zap class="icon" />
            </div>
            <span>极速上传</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Shield class="icon" />
            </div>
            <span>安全存储</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Lock class="icon" />
            </div>
            <span>隐私保护</span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="actions">
          <RouterLink :to="{ path: '/', query: { login: '1' } }" class="btn-primary">
            <span>立即登录</span>
            <ArrowRight class="btn-icon" />
          </RouterLink>
          <RouterLink to="/" class="btn-secondary">
            <Home class="btn-icon" />
            <span>返回首页</span>
          </RouterLink>
        </div>
      </div>

      <!-- Decorative Elements -->
      <div class="decoration">
        <div class="circle circle-1" />
        <div class="circle circle-2" />
        <div class="circle circle-3" />
        <div class="confetti confetti-1" />
        <div class="confetti confetti-2" />
        <div class="confetti confetti-3" />
        <div class="confetti confetti-4" />
        <div class="confetti confetti-5" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== CSS Variables for Theming ===== */
.register-success-page {
  /* Dark theme (default) */
  --bg-primary: #05060c;
  --bg-gradient-1: rgba(123, 132, 255, 0.25);
  --bg-gradient-2: rgba(255, 115, 161, 0.25);
  --grid-color: rgba(255, 255, 255, 0.02);
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  --card-border: rgba(255, 255, 255, 0.15);
  --card-shadow: 0 25px 80px rgba(2, 6, 23, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  --text-primary: #ffffff;
  --text-secondary: rgba(255, 255, 255, 0.7);
  --text-muted: rgba(255, 255, 255, 0.8);
  --feature-bg: linear-gradient(135deg, rgba(244, 114, 182, 0.25) 0%, rgba(167, 139, 250, 0.25) 100%);
  --feature-border: rgba(244, 114, 182, 0.4);
  --feature-text: rgba(255, 255, 255, 0.8);
  --divider-color: rgba(255, 255, 255, 0.1);
  --btn-secondary-bg: rgba(255, 255, 255, 0.1);
  --btn-secondary-border: rgba(255, 255, 255, 0.2);
  --btn-secondary-text: rgba(255, 255, 255, 0.95);
  --btn-secondary-hover-bg: rgba(255, 255, 255, 0.2);
  --btn-secondary-hover-border: rgba(255, 255, 255, 0.3);
  --circle-border: rgba(244, 114, 182, 0.15);
  --glow-opacity: 0.4;
}

.register-success-page.light {
  /* Light theme */
  --bg-primary: linear-gradient(135deg, #fafafa 0%, #f0f0f5 100%);
  --bg-gradient-1: rgba(244, 114, 182, 0.12);
  --bg-gradient-2: rgba(167, 139, 250, 0.12);
  --grid-color: rgba(0, 0, 0, 0.02);
  --card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  --card-border: rgba(0, 0, 0, 0.08);
  --card-shadow: 0 25px 80px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.9) inset;
  --text-primary: #1a1a2e;
  --text-secondary: rgba(26, 26, 46, 0.7);
  --text-muted: rgba(26, 26, 46, 0.8);
  --feature-bg: linear-gradient(135deg, rgba(244, 114, 182, 0.15) 0%, rgba(167, 139, 250, 0.15) 100%);
  --feature-border: rgba(244, 114, 182, 0.3);
  --feature-text: rgba(26, 26, 46, 0.85);
  --divider-color: rgba(0, 0, 0, 0.06);
  --btn-secondary-bg: rgba(0, 0, 0, 0.05);
  --btn-secondary-border: rgba(0, 0, 0, 0.12);
  --btn-secondary-text: rgba(26, 26, 46, 0.95);
  --btn-secondary-hover-bg: rgba(0, 0, 0, 0.1);
  --btn-secondary-hover-border: rgba(0, 0, 0, 0.18);
  --circle-border: rgba(244, 114, 182, 0.2);
  --glow-opacity: 0.25;
}

/* ===== Base Styles ===== */
.register-success-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
  transition: background 0.5s ease;
}

/* Fireworks Canvas */
.fireworks-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

/* Theme Toggle Button */
.theme-toggle {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 100;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--card-shadow);
}

.theme-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 25px rgba(244, 114, 182, 0.4);
}

.theme-icon {
  width: 22px;
  height: 22px;
  color: var(--text-primary);
}

/* Background Effects */
.bg-effects {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.bg-gradient-1 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 20%, var(--bg-gradient-1), transparent 50%);
  transition: opacity 0.5s ease;
}

.bg-gradient-2 {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 10%, var(--bg-gradient-2), transparent 45%);
  transition: opacity 0.5s ease;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--grid-color) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-color) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  transition: all 0.5s ease;
}

.bg-glow {
  position: absolute;
  width: 700px;
  height: 700px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(244, 114, 182, var(--glow-opacity)) 0%, transparent 70%);
  pointer-events: none;
  transition: opacity 0.5s ease;
}

/* Content Container */
.content-container {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 520px;
}

/* Success Card */
.success-card {
  width: 100%;
  padding: 3rem 2.5rem;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 32px;
  backdrop-filter: blur(20px);
  box-shadow: var(--card-shadow);
  text-align: center;
  animation: card-in 0.8s cubic-bezier(0.16, 1, 0.3, 1);
  transition: all 0.5s ease;
  position: relative;
  overflow: hidden;
}

.success-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #f472b6, #a78bfa, #60a5fa, #34d399, #fbbf24, #f472b6);
  background-size: 200% 100%;
  animation: rainbow 3s linear infinite;
}

@keyframes rainbow {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Celebration Badge */
.celebration-badge {
  position: absolute;
  top: -12px;
  right: 30px;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #f472b6 0%, #ec4899 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 25px rgba(244, 114, 182, 0.5);
  animation: celebration-bounce 2s ease-in-out infinite;
  z-index: 10;
}

@keyframes celebration-bounce {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-8px) rotate(5deg); }
  50% { transform: translateY(0) rotate(0deg); }
  75% { transform: translateY(-4px) rotate(-5deg); }
}

.celebration-icon {
  width: 26px;
  height: 26px;
  color: white;
}

/* Icon Wrapper */
.icon-wrapper {
  position: relative;
  display: inline-flex;
  margin-bottom: 1.5rem;
}

.icon-bg {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 0 4px rgba(16, 185, 129, 0.3),
    0 0 0 8px rgba(16, 185, 129, 0.15),
    0 0 0 12px rgba(16, 185, 129, 0.08),
    0 25px 50px rgba(16, 185, 129, 0.4);
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% {
    box-shadow:
      0 0 0 4px rgba(16, 185, 129, 0.3),
      0 0 0 8px rgba(16, 185, 129, 0.15),
      0 0 0 12px rgba(16, 185, 129, 0.08),
      0 25px 50px rgba(16, 185, 129, 0.4);
    transform: scale(1);
  }
  50% {
    box-shadow:
      0 0 0 8px rgba(16, 185, 129, 0.2),
      0 0 0 16px rgba(16, 185, 129, 0.1),
      0 0 0 24px rgba(16, 185, 129, 0.05),
      0 30px 60px rgba(16, 185, 129, 0.5);
    transform: scale(1.05);
  }
}

.success-icon {
  width: 52px;
  height: 52px;
  color: white;
  stroke-width: 2.5;
}

/* Sparkles */
.sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  width: 24px;
  height: 24px;
  color: #fbbf24;
  opacity: 0;
}

.sparkle-1 {
  top: -15px;
  right: -15px;
  animation: sparkle 2s ease-in-out infinite;
}

.sparkle-2 {
  bottom: 15px;
  left: -20px;
  animation: sparkle 2s ease-in-out infinite 0.5s;
}

.sparkle-3 {
  top: 25px;
  right: -25px;
  animation: sparkle 2s ease-in-out infinite 1s;
}

@keyframes sparkle {
  0%, 100% {
    opacity: 0;
    transform: scale(0) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1.2) rotate(180deg);
  }
}

/* Floating Stars */
.floating-stars {
  position: absolute;
  inset: -30px;
  pointer-events: none;
}

.star {
  position: absolute;
  color: #fbbf24;
  opacity: 0;
}

.star-1 {
  top: 0;
  left: 10%;
  width: 16px;
  height: 16px;
  animation: float-star 3s ease-in-out infinite;
}

.star-2 {
  top: 20%;
  right: 5%;
  width: 12px;
  height: 12px;
  animation: float-star 3s ease-in-out infinite 0.7s;
}

.star-3 {
  bottom: 30%;
  left: 0;
  width: 14px;
  height: 14px;
  animation: float-star 3s ease-in-out infinite 1.4s;
}

.star-4 {
  bottom: 10%;
  right: 10%;
  width: 18px;
  height: 18px;
  color: #f472b6;
  animation: float-star 3s ease-in-out infinite 2.1s;
}

@keyframes float-star {
  0%, 100% {
    opacity: 0;
    transform: translateY(0) scale(0.5);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-20px) scale(1);
  }
}

/* Text Content */
.text-content {
  margin-bottom: 2rem;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: #f472b6;
  margin-bottom: 1rem;
}

.badge-line {
  display: block;
  width: 30px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #f472b6);
}

.badge-line:last-child {
  background: linear-gradient(90deg, #f472b6, transparent);
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
  color: var(--text-primary);
  transition: color 0.5s ease;
}

.title-highlight {
  background: linear-gradient(135deg, #f472b6 0%, #a78bfa 50%, #60a5fa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: block;
  font-size: 1.5rem;
  margin-bottom: 0.25rem;
}

.subtitle {
  font-size: 1.0625rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.7;
  transition: color 0.5s ease;
}

/* Features */
.features {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 2rem;
  padding: 1.75rem 0;
  border-top: 1px solid var(--divider-color);
  border-bottom: 1px solid var(--divider-color);
  transition: all 0.5s ease;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  transition: transform 0.3s ease;
}

.feature-item:hover {
  transform: translateY(-5px);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--feature-bg);
  border: 1px solid var(--feature-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.feature-item:hover .feature-icon {
  box-shadow: 0 10px 25px rgba(244, 114, 182, 0.35);
  transform: scale(1.1);
}

.feature-icon .icon {
  width: 24px;
  height: 24px;
  color: #f472b6;
}

.feature-item span {
  font-size: 0.875rem;
  color: var(--feature-text);
  font-weight: 500;
  transition: color 0.5s ease;
}

/* Actions */
.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-primary,
.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem 2rem;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #f472b6 0%, #ec4899 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(244, 114, 182, 0.4);
  position: relative;
  overflow: hidden;
}

.btn-primary::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(244, 114, 182, 0.55);
}

.btn-primary:hover::before {
  left: 100%;
}

.btn-secondary {
  background: var(--btn-secondary-bg);
  color: var(--btn-secondary-text);
  border: 1px solid var(--btn-secondary-border);
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: var(--btn-secondary-hover-bg);
  border-color: var(--btn-secondary-hover-border);
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.btn-icon {
  width: 20px;
  height: 20px;
}

/* Decoration */
.decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid var(--circle-border);
  transition: border-color 0.5s ease;
}

.circle-1 {
  width: 350px;
  height: 350px;
  top: -120px;
  right: -120px;
  animation: float 10s ease-in-out infinite;
}

.circle-2 {
  width: 250px;
  height: 250px;
  bottom: -80px;
  left: -80px;
  animation: float 10s ease-in-out infinite 2.5s;
}

.circle-3 {
  width: 180px;
  height: 180px;
  top: 40%;
  left: -90px;
  animation: float 10s ease-in-out infinite 5s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-25px) rotate(8deg);
  }
}

/* Confetti */
.confetti {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  opacity: 0.6;
}

.confetti-1 {
  background: #f472b6;
  top: 10%;
  left: 5%;
  animation: confetti-fall 4s ease-in-out infinite;
}

.confetti-2 {
  background: #a78bfa;
  top: 20%;
  right: 8%;
  animation: confetti-fall 4s ease-in-out infinite 0.8s;
}

.confetti-3 {
  background: #60a5fa;
  bottom: 30%;
  left: 8%;
  animation: confetti-fall 4s ease-in-out infinite 1.6s;
}

.confetti-4 {
  background: #34d399;
  bottom: 15%;
  right: 5%;
  animation: confetti-fall 4s ease-in-out infinite 2.4s;
}

.confetti-5 {
  background: #fbbf24;
  top: 50%;
  right: 3%;
  animation: confetti-fall 4s ease-in-out infinite 3.2s;
}

@keyframes confetti-fall {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.6;
  }
  50% {
    transform: translateY(30px) rotate(180deg);
    opacity: 0.3;
  }
}

/* Responsive */
@media (max-width: 640px) {
  .register-success-page {
    padding: 1rem;
  }

  .theme-toggle {
    top: 1rem;
    right: 1rem;
    width: 40px;
    height: 40px;
  }

  .success-card {
    padding: 2.5rem 1.5rem;
  }

  .celebration-badge {
    width: 40px;
    height: 40px;
    top: -10px;
    right: 20px;
  }

  .celebration-icon {
    width: 20px;
    height: 20px;
  }

  .icon-bg {
    width: 90px;
    height: 90px;
  }

  .success-icon {
    width: 42px;
    height: 42px;
  }

  .title {
    font-size: 2rem;
  }

  .title-highlight {
    font-size: 1.25rem;
  }

  .features {
    gap: 1.25rem;
  }

  .feature-icon {
    width: 42px;
    height: 42px;
  }

  .actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
    padding: 0.875rem 1.5rem;
  }
}
</style>
