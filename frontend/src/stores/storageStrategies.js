import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  activateStorageStrategy,
  createStorageStrategy,
  deleteStorageStrategy,
  listStorageStrategies,
  updateStorageStrategy,
} from '@/services/storageStrategies'

export const useStorageStrategyStore = defineStore('storageStrategies', () => {
  const strategies = ref([])
  const loading = ref(false)
  const errorMessage = ref('')

  const activeStrategyId = computed(() => {
    const activeProfile = strategies.value.find((profile) => profile.active)
    return activeProfile ? activeProfile.id : null
  })

  const findStrategyById = (id) => strategies.value.find((profile) => profile.id === id)

  const upsertStrategy = (profile) => {
    if (!profile) {
      return
    }
    const index = strategies.value.findIndex((item) => item.id === profile.id)
    if (index >= 0) {
      const next = [...strategies.value]
      next.splice(index, 1, profile)
      strategies.value = next
      return
    }
    strategies.value = [profile, ...strategies.value]
  }

  const removeStrategyLocal = (id) => {
    strategies.value = strategies.value.filter((profile) => profile.id !== id)
  }

  const syncActiveProfile = (profile) => {
    if (!profile || !profile.active) {
      return
    }
    strategies.value = strategies.value.map((item) => ({ ...item, active: item.id === profile.id }))
  }

  const fetchStrategies = () => {
    loading.value = true
    errorMessage.value = ''
    return listStorageStrategies()
      .then((data) => {
        strategies.value = Array.isArray(data) ? data : []
      })
      .catch((error) => {
        console.error('加载存储策略失败', error)
        errorMessage.value = error?.response?.data?.message || '加载存储策略失败'
      })
      .finally(() => {
        loading.value = false
      })
  }

  const createStrategy = (payload) =>
    createStorageStrategy(payload)
      .then((profile) => {
        upsertStrategy(profile)
        syncActiveProfile(profile)
        return profile
      })
      .catch((error) => {
        console.error('创建存储策略失败', error)
        throw error
      })

  const updateStrategy = (id, payload) =>
    updateStorageStrategy(id, payload)
      .then((profile) => {
        upsertStrategy(profile)
        syncActiveProfile(profile)
        return profile
      })
      .catch((error) => {
        console.error('更新存储策略失败', error)
        throw error
      })

  const activateStrategy = (id) =>
    activateStorageStrategy(id)
      .then((profile) => {
        upsertStrategy(profile)
        syncActiveProfile(profile)
        return profile
      })
      .catch((error) => {
        console.error('激活存储策略失败', error)
        throw error
      })

  const removeStrategy = (id) =>
    deleteStorageStrategy(id)
      .then(() => {
        removeStrategyLocal(id)
      })
      .catch((error) => {
        console.error('删除存储策略失败', error)
        throw error
      })

  return {
    strategies,
    loading,
    errorMessage,
    activeStrategyId,
    findStrategyById,
    fetchStrategies,
    createStrategy,
    updateStrategy,
    activateStrategy,
    removeStrategy,
  }
})
