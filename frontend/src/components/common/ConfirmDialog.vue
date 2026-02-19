<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="confirm-dialog-overlay" @click="handleOverlayClick">
        <Transition name="dialog-scale">
          <div v-if="visible" class="confirm-dialog" @click.stop>
            <div class="dialog-header">
              <div class="dialog-icon" :class="type">
                <svg v-if="type === 'warning'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/>
                  <path d="M12 9v4"/>
                  <path d="M12 17h.01"/>
                </svg>
                <svg v-else-if="type === 'danger'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="m15 9-6 6"/>
                  <path d="m9 9 6 6"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 16v-4"/>
                  <path d="M12 8h.01"/>
                </svg>
              </div>
              <h3 class="dialog-title">{{ title }}</h3>
            </div>
            <div class="dialog-content">
              <p class="dialog-message">{{ message }}</p>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-cancel" @click="handleCancel">
                {{ cancelText }}
              </button>
              <button type="button" class="btn btn-confirm" :class="type" @click="handleConfirm">
                {{ confirmText }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认操作'
  },
  message: {
    type: String,
    default: '确定要执行此操作吗？'
  },
  type: {
    type: String,
    default: 'warning',
    validator: (value) => ['info', 'warning', 'danger'].includes(value)
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(props.modelValue)

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
})

watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
})

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    handleCancel()
  }
}

const handleConfirm = () => {
  visible.value = false
  emit('confirm')
}

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}
</script>

<style scoped>
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.confirm-dialog {
  background: var(--color-bg-primary, #ffffff);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 400px;
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--border-soft, rgba(0, 0, 0, 0.08));
}

.dialog-header {
  padding: 1.5rem 1.5rem 0.75rem;
  text-align: center;
}

.dialog-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  transition: all 0.3s ease;
}

.dialog-icon.info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.dialog-icon.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.dialog-icon.danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.dialog-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary, #1f2937);
  margin: 0;
}

.dialog-content {
  padding: 0.75rem 1.5rem;
  text-align: center;
}

.dialog-message {
  font-size: 0.95rem;
  color: var(--color-text-secondary, #6b7280);
  line-height: 1.6;
  margin: 0;
}

.dialog-footer {
  padding: 1.25rem 1.5rem 1.5rem;
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.btn {
  padding: 0.625rem 1.5rem;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  outline: none;
  min-width: 80px;
}

.btn-cancel {
  background: var(--color-bg-secondary, #f3f4f6);
  color: var(--color-text-secondary, #6b7280);
}

.btn-cancel:hover {
  background: var(--color-bg-tertiary, #e5e7eb);
  transform: translateY(-1px);
}

.btn-confirm {
  color: white;
}

.btn-confirm.info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.btn-confirm.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.btn-confirm.danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.btn:active {
  transform: translateY(0);
}

/* 过渡动画 */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.dialog-scale-enter-active,
.dialog-scale-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dialog-scale-enter-from,
.dialog-scale-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .confirm-dialog {
    background: var(--color-bg-primary, #1f2937);
    border-color: var(--border-soft, rgba(255, 255, 255, 0.1));
  }

  .dialog-title {
    color: var(--color-text-primary, #f9fafb);
  }

  .dialog-message {
    color: var(--color-text-secondary, #9ca3af);
  }

  .btn-cancel {
    background: var(--color-bg-secondary, #374151);
    color: var(--color-text-secondary, #9ca3af);
  }

  .btn-cancel:hover {
    background: var(--color-bg-tertiary, #4b5563);
  }
}
</style>
