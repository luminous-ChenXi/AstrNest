import { ref, createApp, h } from 'vue'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'

/**
 * 使用确认弹窗
 * @returns {Object} { confirm, ConfirmDialogComponent }
 * 
 * 使用示例：
 * const { confirm } = useConfirm()
 * 
 * // 方式1：使用 Promise
 * const result = await confirm({
 *   title: '删除确认',
 *   message: '确定要删除这张图片吗？',
 *   type: 'danger'
 * })
 * if (result) {
 *   // 用户点击了确定
 * }
 * 
 * // 方式2：使用回调
 * confirm({
 *   title: '删除确认',
 *   message: '确定要删除这张图片吗？',
 *   type: 'danger',
 *   onConfirm: () => {
 *     // 用户点击了确定
 *   },
 *   onCancel: () => {
 *     // 用户点击了取消
 *   }
 * })
 */
export function useConfirm() {
  const confirm = (options = {}) => {
    return new Promise((resolve) => {
      const container = document.createElement('div')
      document.body.appendChild(container)

      const app = createApp({
        setup() {
          const visible = ref(true)

          const handleConfirm = () => {
            visible.value = false
            if (options.onConfirm) {
              options.onConfirm()
            }
            resolve(true)
            setTimeout(() => {
              app.unmount()
              document.body.removeChild(container)
            }, 300)
          }

          const handleCancel = () => {
            visible.value = false
            if (options.onCancel) {
              options.onCancel()
            }
            resolve(false)
            setTimeout(() => {
              app.unmount()
              document.body.removeChild(container)
            }, 300)
          }

          return () => h(ConfirmDialog, {
            modelValue: visible.value,
            'onUpdate:modelValue': (val) => { visible.value = val },
            title: options.title || '确认操作',
            message: options.message || '确定要执行此操作吗？',
            type: options.type || 'warning',
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            closeOnOverlay: options.closeOnOverlay !== false,
            onConfirm: handleConfirm,
            onCancel: handleCancel
          })
        }
      })

      app.mount(container)
    })
  }

  return { confirm }
}

export default useConfirm
