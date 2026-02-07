import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUploadStore = defineStore('upload', () => {
  const limits = ref({
    allowedExtensions: [],
    maxSize: '0B',
    maxSizePerFile: '0B',
    maxFiles: 0,
  })
  const loading = ref(false)
  const error = ref(null)

  const setLimits = (newLimits) => {
    limits.value = { ...limits.value, ...newLimits }
  }

  const setLoading = (isLoading) => {
    loading.value = isLoading
  }

  const setError = (newError) => {
    error.value = newError
  }

  return { limits, loading, error, setLimits, setLoading, setError }
})
