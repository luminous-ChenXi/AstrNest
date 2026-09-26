import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchInstallStatus } from '@/services/install'

/**
 * 安装状态全局缓存：路由守卫据此把未安装的访问强制导向 /install，
 * 已安装时拒绝再次进入向导。请求失败（网络异常等）标记为 unknown，
 * 守卫放行以避免死循环，由向导页自行展示错误与重试。
 */
export const useInstallStore = defineStore('install', () => {
  /** 'unknown' | 'installed' | 'not-installed' */
  const status = ref('unknown')
  const installed = ref(false)
  const finished = ref(false)
  const locked = ref(false)
  const schemaState = ref(null)
  const checks = ref([])
  const error = ref(null)

  const CACHE_MILLIS = 3000
  let fetchedAt = 0
  let inflight = null

  const snapshot = () => ({
    status: status.value,
    installed: installed.value,
    finished: finished.value,
    locked: locked.value,
    schemaState: schemaState.value,
    checks: checks.value,
  })

  const applyPayload = (data) => {
    installed.value = Boolean(data?.installed)
    finished.value = Boolean(data?.finished)
    // 防重装锁：install.lock 文件或 DB 完成标记任一命中即锁定（缺省视为随 finished）
    locked.value = Boolean(data?.locked ?? data?.finished)
    schemaState.value = data?.schemaState || null
    checks.value = Array.isArray(data?.checks) ? data.checks : []
    status.value = installed.value ? 'installed' : 'not-installed'
    error.value = null
    fetchedAt = Date.now()
  }

  async function fetchStatus(force = false) {
    // 已知或未知的结果都进入 3s 缓存窗口，避免每次路由跳转都发探测请求；force 用于「重新检测」
    const withinCache = Date.now() - fetchedAt < CACHE_MILLIS
    if (inflight) return inflight
    if (withinCache && !force) {
      return snapshot()
    }
    inflight = (async () => {
      try {
        const data = await fetchInstallStatus()
        applyPayload(data)
      } catch (err) {
        if (err?.response?.status === 503) {
          // 后端守卫的 503 也视为未安装（理论 /api/install/status 不会被拦，容错兜底）
          applyPayload({ installed: false, finished: false, locked: false, schemaState: null, checks: [] })
        } else {
          status.value = 'unknown'
          error.value = err?.message || '无法连接服务器'
          // 失败结果同样进入 3s 缓存窗口，避免后端宕机时每次路由跳转都卡在超时请求上
          fetchedAt = Date.now()
        }
      } finally {
        inflight = null
      }
      return snapshot()
    })()
    return inflight
  }

  return { status, installed, finished, locked, schemaState, checks, error, fetchStatus }
})
