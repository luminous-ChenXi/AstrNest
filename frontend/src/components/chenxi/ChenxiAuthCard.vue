<script setup>
import { ElCard } from 'element-plus'
</script>

<template>
  <ElCard shadow="never" class="chenxi-auth-card">
    <div class="mb-6 text-center">
      <slot name="title">
        <p class="text-xs uppercase tracking-[0.6em] text-body-soft">Chenxi Auth</p>
        <h2 class="text-2xl font-semibold text-body-primary">辰汐认证中心</h2>
      </slot>
      <slot name="subtitle">
        <p class="text-sm text-body-muted">使用邮箱验证码 + 人机验证，保障访问安全</p>
      </slot>
    </div>
    <slot />
  </ElCard>
</template>

<style scoped>
.chenxi-auth-card :deep(.el-card__body) {
  @apply rounded-[32px] border border-body bg-surface-overlay p-8;
  box-shadow: var(--shadow-card);
}
</style>
