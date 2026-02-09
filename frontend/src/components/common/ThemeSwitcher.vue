<template>
  <div class="theme-switcher">
    <button
      @click="mode = 'light'"
      :class="[
        'theme-btn',
        mode === 'light' && 'theme-btn-active-light',
      ]"
      title="亮色模式"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    </button>
    <button
      @click="mode = 'dark'"
      :class="[
        'theme-btn',
        mode === 'dark' && 'theme-btn-active-dark',
      ]"
      title="暗色模式"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
      </svg>
    </button>
    <button
      @click="mode = 'auto'"
      :class="[
        'theme-btn',
        mode === 'auto' && 'theme-btn-active-auto',
      ]"
      title="跟随系统"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'

const { mode } = useTheme()
</script>

<style scoped>
.theme-switcher {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  border-radius: 9999px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

/* 暗色主题下的容器样式 */
:global(.dark) .theme-switcher {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
}

.theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9999px;
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.5);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: rgba(0, 0, 0, 0.8);
}

/* 暗色主题下的按钮样式 */
:global(.dark) .theme-btn {
  color: rgba(255, 255, 255, 0.5);
}

:global(.dark) .theme-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

/* 亮色模式选中状态 */
.theme-btn-active-light {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
}

/* 暗色模式选中状态 */
.theme-btn-active-dark {
  background: linear-gradient(135deg, #6366f1, #4f46e5) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}

/* 自动模式选中状态 */
.theme-btn-active-auto {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.4);
}

/* 选中按钮在暗色主题下的样式调整 */
:global(.dark) .theme-btn-active-light,
:global(.dark) .theme-btn-active-dark,
:global(.dark) .theme-btn-active-auto {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}
</style>
