<script setup>
import { RouterLink } from 'vue-router'
import { CheckCircle } from 'lucide-vue-next'
import ChenxiAuthCard from '../../components/chenxi/ChenxiAuthCard.vue'
</script>

<template>
  <ChenxiAuthCard>
    <div class="text-center">
      <CheckCircle class="mx-auto h-16 w-16 text-brand-emerald" />
      <h2 class="mt-4 text-2xl font-semibold text-white">密码重置成功</h2>
      <p class="mt-2 text-sm text-white/70">您的密码已成功重置，现在可以使用新密码登录了。</p>
      <div class="mt-6 flex justify-center gap-4">
        <RouterLink to="/login">
          <ElButton type="primary" size="large" class="bg-gradient-to-r from-brand-primary to-brand-accent border-none">
            立即登录
          </ElButton>
        </RouterLink>
        <RouterLink to="/">
          <ElButton size="large" plain class="text-white">
            返回首页
          </ElButton>
        </RouterLink>
      </div>
    </div>
  </ChenxiAuthCard>
</template>
