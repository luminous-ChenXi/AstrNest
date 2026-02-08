<script setup>
import StorageStrategyPanel from '../components/admin/StorageStrategyPanel.vue'
</script>

<template>
  <section class="space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">storage</p>
      <h1 class="text-4xl font-semibold text-gradient">存储策略中心</h1>
      <p class="text-white/70 mt-2 max-w-3xl">
        采取多云策略管理，集中创建或切换 OSS/COS/OneDrive 等驱动，所有配置会持久化到数据库并即时生效。
      </p>
    </header>

    <StorageStrategyPanel />
  </section>
</template>
