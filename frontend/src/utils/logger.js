/**
 * 统一日志工具
 * 生产环境自动禁用调试日志
 */

const isDev = import.meta.env.DEV
const isProd = import.meta.env.PROD

const LogLevel = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
}

// 根据环境设置日志级别
const currentLevel = isProd ? LogLevel.WARN : LogLevel.DEBUG

const logger = {
  debug(...args) {
    if (currentLevel <= LogLevel.DEBUG) {
      console.debug('[DEBUG]', ...args)
    }
  },

  info(...args) {
    if (currentLevel <= LogLevel.INFO) {
      console.info('[INFO]', ...args)
    }
  },

  warn(...args) {
    if (currentLevel <= LogLevel.WARN) {
      console.warn('[WARN]', ...args)
    }
  },

  error(...args) {
    if (currentLevel <= LogLevel.ERROR) {
      console.error('[ERROR]', ...args)
    }
  },

  // 分组日志
  group(label) {
    if (isDev) {
      console.group(label)
    }
  },

  groupEnd() {
    if (isDev) {
      console.groupEnd()
    }
  },

  // 性能计时
  time(label) {
    if (isDev) {
      console.time(label)
    }
  },

  timeEnd(label) {
    if (isDev) {
      console.timeEnd(label)
    }
  },
}

export default logger
export { LogLevel }
