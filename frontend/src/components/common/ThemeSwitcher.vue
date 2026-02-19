
<template>
  <div class="flex items-center space-x-1 p-1 rounded-full bg-gray-200/50 dark:bg-gray-800/50 text-sm">
    <button
      @click="mode = 'light'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'light'
          ? 'bg-white text-blue-500 shadow-sm'
          : 'text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-white',
      ]"
    >
      亮
    </button>
    <button
      @click="mode = 'dark'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'dark'
          ? 'bg-gray-700 text-white shadow-sm'
          : 'text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-white',
      ]"
    >
      暗
    </button>
    <button
      @click="mode = 'auto'"
      :class="[
        'px-3 py-1 rounded-full transition-colors duration-300',
        mode === 'auto'
          ? 'bg-gray-500/50 text-white shadow-sm'
          : 'text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-white',
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
