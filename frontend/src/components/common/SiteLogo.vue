<template>
  <svg
    class="logo-svg"
    viewBox="0 0 1024 1024"
    version="1.1"
    xmlns="http://www.w3.org/2000/svg"
    :width="width"
    :height="height"
  >
    <defs>
      <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" class="gradient-start" />
        <stop offset="100%" class="gradient-end" />
      </linearGradient>
    </defs>
    <path
      class="logo-path"
      fill="url(#logoGradient)"
      d="M810.667 490.667L787.2 552.533h-61.867l51.2 38.4-23.466 70.4 57.6-42.666 57.6 42.666-23.467-70.4 51.2-38.4h-61.867l-23.466-61.866zM539.733 448l57.6-42.667 57.6 42.667-23.466-70.4 51.2-38.4H620.8l-23.467-61.867-23.466 61.867H512l51.2 38.4-23.467 70.4z m441.6-320H896l-32-85.333L832 128h-85.333l70.4 53.333-32 96 81.066-59.733 81.067 59.733-32-96L981.333 128zM362.667 339.2c0-113.067 40.533-215.467 106.666-296.533C230.4 64 42.667 266.667 42.667 512c0 260.267 209.066 469.333 469.333 469.333 147.2 0 298.667-68.266 384-172.8-14.933 2.134-49.067 2.134-64 2.134-260.267 0-469.333-211.2-469.333-471.467zM512 938.667c-234.667 0-426.667-192-426.667-426.667 0-187.733 119.467-349.867 292.267-405.333-36.267 72.533-57.6 153.6-57.6 234.666 0 268.8 204.8 488.534 467.2 512-76.8 53.334-177.067 85.334-275.2 85.334z"
    />
  </svg>
</template>

<script setup>
defineProps({
  width: {
    type: [Number, String],
    default: 40
  },
  height: {
    type: [Number, String],
    default: 40
  }
})
</script>

<style scoped>
.logo-svg {
  display: block;
}

/* 亮主题：黑色 */
.gradient-start {
  stop-color: #1a1a1a;
}

.gradient-end {
  stop-color: #333333;
}

/* 暗色主题：白色 */
:global(.dark) .gradient-start {
  stop-color: #ffffff;
}

:global(.dark) .gradient-end {
  stop-color: #e0e0e0;
}

/* 使用 CSS 变量的替代方案 */
@supports (stop-color: var(--color-text-primary)) {
  .gradient-start {
    stop-color: var(--color-text-primary, #1a1a1a);
  }
  
  .gradient-end {
    stop-color: var(--color-text-secondary, #333333);
  }
}
</style>
