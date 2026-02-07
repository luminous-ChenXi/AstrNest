import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSystemConfig } from '@/services/system'

export const useSystemStore = defineStore('system', () => {
  const config = ref<Record<string, any> | null>(null)
  const loading = ref(false)
  let inflightPromise: Promise<any> | null = null

  async function fetchSystemConfig(force = false) {
    if (config.value && !force) {
      return config.value
    }
    if (loading.value && inflightPromise) {
      return inflightPromise
    }
    loading.value = true
    inflightPromise = getSystemConfig()
      .then((response) => {
        config.value = response.data
        return config.value
      })
      .catch((error) => {
        console.error('获取系统配置失败', error)
        return null
      })
      .finally(() => {
        loading.value = false
        inflightPromise = null
      })
    return inflightPromise
  }

  return {
    config,
    loading,
    fetchSystemConfig,
  }
})
