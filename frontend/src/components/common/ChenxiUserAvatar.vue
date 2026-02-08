<script setup>
import { computed, useAttrs } from 'vue'
import { User } from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  size: {
    type: [Number, String],
    default: 40,
  },
  initials: {
    type: String,
    default: '',
  },
  avatarUrl: {
    type: String,
    default: '',
  },
  gradientClass: {
    type: String,
    default: 'from-pink-400 to-pink-500',
  },
  uppercase: {
    type: Boolean,
    default: true,
  },
  useDropdown: {
    type: Boolean,
    default: false,
  },
})

const attrs = useAttrs()
const auth = useAuthStore()

const displayInitial = computed(() => {
  const fallback =
    props.initials ||
    auth.profile?.nickname ||
    auth.profile?.username ||
    auth.displayName ||
    auth.profile?.email ||
    ''
  if (!fallback.trim()) return ''
  const char = fallback.trim().charAt(0)
  return props.uppercase ? char.toUpperCase() : char
})

const hasInitial = computed(() => Boolean(displayInitial.value))
const resolvedAvatar = computed(() => props.avatarUrl || auth.profile?.avatarUrl || '')

const sizeStyle = computed(() => {
  const value = typeof props.size === 'number' ? `${props.size}px` : props.size
  return {
    width: value,
    height: value,
    minWidth: value,
    minHeight: value,
  }
})

const dropdownAttrs = computed(() => (props.useDropdown ? attrs : {}))
const avatarAttrs = computed(() => (props.useDropdown ? {} : attrs))
</script>

<template>
  <el-dropdown v-if="useDropdown" class="chenxi-user-dropdown" trigger="hover" v-bind="dropdownAttrs">
    <div
      class="chenxi-user-avatar chenxi-avatar-pink"
      :class="gradientClass"
      :style="sizeStyle"
    >
      <template v-if="resolvedAvatar">
        <img :src="resolvedAvatar" alt="用户头像" class="h-full w-full rounded-full object-cover" />
      </template>
      <span v-else-if="hasInitial" class="text-sm leading-none">
        {{ displayInitial }}
      </span>
      <User v-else class="h-5 w-5" />
    </div>
    <template v-if="$slots.dropdown" #dropdown>
      <slot name="dropdown" />
    </template>
  </el-dropdown>
  <div
    v-else
    class="chenxi-user-avatar chenxi-avatar-pink"
    :class="gradientClass"
    :style="sizeStyle"
    v-bind="avatarAttrs"
  >
    <template v-if="resolvedAvatar">
      <img :src="resolvedAvatar" alt="用户头像" class="h-full w-full rounded-full object-cover" />
    </template>
    <span v-else-if="hasInitial" class="text-sm leading-none">
      {{ displayInitial }}
    </span>
    <User v-else class="h-5 w-5" />
  </div>
</template>

<style scoped>
.chenxi-avatar-pink {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: linear-gradient(135deg, #ff8fab 0%, #ff6b9d 100%);
  color: white;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.35);
  transition: all 0.2s ease;
}

.chenxi-avatar-pink:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.45);
}

.dark .chenxi-avatar-pink {
  background: linear-gradient(135deg, #ffb3c1 0%, #ff8fab 100%);
  box-shadow: 0 4px 15px rgba(255, 179, 193, 0.3);
}

.dark .chenxi-avatar-pink:hover {
  box-shadow: 0 6px 20px rgba(255, 179, 193, 0.4);
}
</style>
