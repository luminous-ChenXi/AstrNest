<template>
  <Teleport to="body">
    <Transition name="chenxi-loader-fade">
      <div v-if="active" class="chenxi-loader-backdrop">
        <div class="chenxi-loader-content">
          <!-- Logo 动画 -->
          <div class="loader-logo">
            <div class="logo-ring ring-1"></div>
            <div class="logo-ring ring-2"></div>
            <div class="logo-ring ring-3"></div>
            <span class="logo-text">CX</span>
          </div>
          
          <!-- 加载文字 -->
          <div class="loader-text-group">
            <p class="loader-title">辰汐图床</p>
            <p class="loader-subtitle">{{ loadingText }}</p>
          </div>
          
          <!-- 进度条 -->
          <div class="loader-progress">
            <div class="progress-track">
              <div class="progress-bar"></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  active: {
    type: Boolean,
    default: false,
  },
})

const loadingTexts = [
  '正在连接服务器...',
  '加载资源中...',
  '准备就绪...',
]

const loadingText = ref(loadingTexts[0])
let textInterval = null

watch(() => props.active, (newVal) => {
  if (newVal) {
    let index = 0
    loadingText.value = loadingTexts[0]
    textInterval = setInterval(() => {
      index = (index + 1) % loadingTexts.length
      loadingText.value = loadingTexts[index]
    }, 800)
  } else {
    if (textInterval) {
      clearInterval(textInterval)
      textInterval = null
    }
  }
})
</script>

<style scoped>
/* 遮罩层 - 适配明暗主题 */
.chenxi-loader-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 深色主题 - 纯黑背景 */
:global(.dark) .chenxi-loader-backdrop {
  background: rgba(0, 0, 0, 0.9);
}

/* 内容容器 */
.chenxi-loader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

/* Logo 动画容器 */
.loader-logo {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 旋转圆环 */
.logo-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.ring-1 {
  width: 100%;
  height: 100%;
  border-top-color: #F9A8C8;
  border-right-color: #F9A8C8;
  animation: spin 1.5s linear infinite;
}

.ring-2 {
  width: 75%;
  height: 75%;
  border-bottom-color: #AED0ED;
  border-left-color: #AED0ED;
  animation: spin 1.2s linear infinite reverse;
}

.ring-3 {
  width: 50%;
  height: 50%;
  border-top-color: #F9A8C8;
  animation: spin 0.8s linear infinite;
}

/* Logo 文字 */
.logo-text {
  font-size: 1.5rem;
  font-weight: 800;
  color: #F9A8C8;
  z-index: 1;
}

/* 深色主题 Logo 文字 */
:global(.dark) .logo-text {
  color: #E87A9F;
}

/* 文字组 */
.loader-text-group {
  text-align: center;
}

.loader-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 0.5rem;
}

.loader-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
  min-height: 1.25rem;
  transition: opacity 0.3s ease;
}

/* 深色主题文字 */
:global(.dark) .loader-title {
  color: #ffffff;
}

:global(.dark) .loader-subtitle {
  color: rgba(255, 255, 255, 0.6);
}

/* 进度条 */
.loader-progress {
  width: 200px;
}

.progress-track {
  height: 4px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

:global(.dark) .progress-track {
  background: rgba(255, 255, 255, 0.1);
}

.progress-bar {
  height: 100%;
  width: 30%;
  background: linear-gradient(90deg, #F9A8C8, #E87A9F);
  border-radius: 2px;
  animation: progress 1.5s ease-in-out infinite;
}

/* 动画定义 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes progress {
  0% {
    transform: translateX(-100%);
  }
  50% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(300%);
  }
}

/* 淡入淡出动画 */
.chenxi-loader-fade-enter-active,
.chenxi-loader-fade-leave-active {
  transition: opacity 0.3s ease;
}

.chenxi-loader-fade-enter-from,
.chenxi-loader-fade-leave-to {
  opacity: 0;
}

.chenxi-loader-fade-enter-from .chenxi-loader-content,
.chenxi-loader-fade-leave-to .chenxi-loader-content {
  transform: scale(0.95);
  opacity: 0;
}

.chenxi-loader-fade-enter-active .chenxi-loader-content,
.chenxi-loader-fade-leave-active .chenxi-loader-content {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
</style>
