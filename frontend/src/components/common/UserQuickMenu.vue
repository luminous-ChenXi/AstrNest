<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChenxiUserAvatar from './ChenxiUserAvatar.vue'
import { useAuthStore } from '../../stores/auth'

const DEFAULT_ITEMS = Object.freeze([
  { name: 'user-home', label: '仪表盘' },
  { name: 'user-images', label: '媒体管理' },
  { name: 'user-albums', label: '我的图集' },
  { name: 'user-profile', label: '资料信息' },
  { name: 'user-security', label: '安全设置' },
  { name: 'user-api', label: 'API 接口管理' },
])

const props = defineProps({
  items: {
    type: Array,
  },
  size: {
    type: [Number, String],
    default: 40,
  },
  gradientClass: {
    type: String,
    default: 'from-brand-primary to-brand-accent',
  },
  showLogout: {
    type: Boolean,
    default: true,
  },
})

const auth = useAuthStore()
const router = useRouter()

const menuItems = computed(() => (props.items?.length ? props.items : DEFAULT_ITEMS))

const normalizeRoute = (item) => {
  if (item.route) return item.route
  if (item.name) return { name: item.name }
  return '/'
}

const handleBeforeNavigate = () => {
  if (!auth.isAuthenticated) {
    ElMessage.warning('未登录，请登录后再操作')
  }
}

const handleLogout = () => {
  auth.logout()
  ElMessage.success('登出成功')
  router.replace({ path: '/', query: { login: '1' } })
}
</script>

<template>
  <ChenxiUserAvatar :size="size" :gradient-class="gradientClass" use-dropdown class="cursor-pointer">
    <template #dropdown>
      <el-dropdown-menu class="user-dropdown-menu">
        <RouterLink
          v-for="item in menuItems"
          :key="item.name || item.label"
          :to="normalizeRoute(item)"
          @click="handleBeforeNavigate"
        >
          <el-dropdown-item>{{ item.label }}</el-dropdown-item>
        </RouterLink>
        <template v-if="showLogout">
          <el-divider />
          <el-dropdown-item class="logout-item" @click="handleLogout">
            退出登录
          </el-dropdown-item>
        </template>
      </el-dropdown-menu>
    </template>
  </ChenxiUserAvatar>
</template>

<style scoped>
/* 下拉菜单样式 - 与用户界面一致 */
:deep(.user-dropdown-menu) {
  min-width: 160px;
}

:deep(.logout-item) {
  color: #f87171;
}

:deep(.logout-item:hover) {
  color: #dc2626;
  background: rgba(248, 113, 113, 0.1);
}
</style>
