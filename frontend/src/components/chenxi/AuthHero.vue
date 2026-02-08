<script setup>
import { CheckCircle } from 'lucide-vue-next'

defineProps({
  badge: String,
  title: String,
  description: String,
  features: Array,
})
</script>

<template>
  <div class="space-y-6">
    <p v-if="badge" class="text-sm uppercase tracking-[0.6em] text-white/60">{{ badge }}</p>
    <h1 v-if="title" class="text-4xl font-semibold leading-tight text-gradient">{{ title }}</h1>
    <p v-if="description" class="text-base text-white/70">{{ description }}</p>
    
    <div v-if="features && features.length" class="grid gap-3 text-sm text-white/70 sm:grid-cols-2">
      <div 
        v-for="(feature, index) in features" 
        :key="index"
        class="glass-panel flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3"
      >
        <CheckCircle class="h-5 w-5 text-brand-primary" />
        <p>{{ feature }}</p>
      </div>
    </div>
  </div>
</template>
