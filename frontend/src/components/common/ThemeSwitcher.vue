
<template>
  <div class="hidden md:flex items-center space-x-1 p-1 rounded-full bg-surface-strong text-sm">
    <button
      @click="mode = 'light'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'light'
          ? 'bg-surface-panel text-brand-primary shadow-sm'
          : 'text-body-muted hover:text-body-primary',
      ]"
    >
      亮
    </button>
    <button
      @click="mode = 'dark'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'dark'
          ? 'bg-surface-strong text-body-primary shadow-sm'
          : 'text-body-muted hover:text-body-primary',
      ]"
    >
      暗
    </button>
    <button
      @click="mode = 'auto'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'auto'
          ? 'bg-surface-strong text-body-primary shadow-sm'
          : 'text-body-muted hover:text-body-primary',
      ]"
    >
      自动
    </button>
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'

const { mode } = useTheme()
</script>
