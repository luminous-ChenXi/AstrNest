<template>
  <div class="app-shell">
    <ChenxiPageLoader :active="isPageLoading" />
    <RouterView v-slot="{ Component }">
      <Transition :name="activeTransition" mode="out-in">
        <component :is="Component" :key="routeKey" />
      </Transition>
    </RouterView>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ChenxiPageLoader from './components/common/ChenxiPageLoader.vue'
import { useUiStore } from './stores/ui'

const route = useRoute()
const ui = useUiStore()

const activeTransition = ref('chenxi-page-fade')
const routeKey = computed(() => route.fullPath)
const lastOrder = ref(route.meta.pageTransitionOrder ?? 0)
const isPageLoading = computed(() => ui.pageLoading)

watch(
  () => route.fullPath,
  () => {
    const nextOrder = route.meta.pageTransitionOrder ?? 0
    const previousOrder = lastOrder.value ?? 0
    if (nextOrder > previousOrder) {
      activeTransition.value = 'chenxi-page-forward'
    } else if (nextOrder < previousOrder) {
      activeTransition.value = 'chenxi-page-back'
    } else {
      activeTransition.value = 'chenxi-page-fade'
    }
    lastOrder.value = nextOrder
  },
  { immediate: true }
)
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
