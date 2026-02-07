<script setup>
import { ElCard } from 'element-plus'
</script>

<template>
  <ElCard shadow="never" class="chenxi-auth-card">
    <div class="mb-6 text-center">
      <slot name="title">
        <p class="text-xs uppercase tracking-[0.6em] text-white/50">Chenxi Auth</p>
        <h2 class="text-2xl font-semibold text-white">辰汐认证中心</h2>
      </slot>
      <slot name="subtitle">
        <p class="text-sm text-white/60">使用邮箱验证码 + 人机验证，保障访问安全</p>
      </slot>
    </div>
    <slot />
  </ElCard>
</template>

<style scoped>
.chenxi-auth-card :deep(.el-card__body) {
  @apply rounded-[32px] border border-white/10 bg-white/5 shadow-[0_25px_80px_rgba(2,6,23,0.55)] p-8;
}
</style>
