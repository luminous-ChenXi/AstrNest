import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import AuthLayout from '../layouts/AuthLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('../views/PublicLandingView.vue'),
      meta: { public: true, pageTransitionOrder: 0 },
    },
    {
      path: '/gallery',
      name: 'public-gallery',
      component: () => import('../views/public/PublicGalleryView.vue'),
      meta: { public: true, pageTransitionOrder: 6 },
    },
    {
      path: '/announcements',
      name: 'public-announcements',
      component: () => import('../views/public/PublicAnnouncementsView.vue'),
      meta: { public: true, pageTransitionOrder: 7 },
    },
    {
      path: '/announcements/:id',
      name: 'public-announcement-detail',
      component: () => import('../views/public/PublicAnnouncementDetailView.vue'),
      meta: { public: true, pageTransitionOrder: 7 },
    },
    {
      path: '/users/:userId',
      name: 'public-user-profile',
      component: () => import('../views/public/PublicUserProfileView.vue'),
      meta: { public: true, pageTransitionOrder: 0 },
    },
    {
      path: '/login',
      name: 'login',
      redirect: { path: '/', query: { login: '1' } },
      meta: { public: true },
    },
    {
      path: '/auth',
      component: AuthLayout,
      meta: { public: true, pageTransitionOrder: 0 },
      children: [
        {
          path: '/register',
          name: 'register',
          component: () => import('../views/auth/RegisterView.vue'),
          meta: {
            authLayout: 'split',
            heroBadge: 'Chenxi Signup',
            heroTitle: '辰汐会员邮箱注册 · 自助开通',
            heroDescription: '完成图形验证码校验与邮箱验证码，即可自动创建普通用户并访问图床上传中心、API 配额与内容治理面板。',
            heroFeatures: [
              '5 分钟验证码有效期 · 60 秒重发冷却',
              '自动授予 USER 角色，可随时升级权限',
            ],
            pageTransitionOrder: 20,
          },
        },
        {
          path: '/forgot-password',
          name: 'forgot-password',
          component: () => import('../views/auth/ForgotPasswordView.vue'),
          meta: {
            authLayout: 'centered',
          },
        },
        {
          path: '/register-success',
          name: 'register-success',
          component: () => import('../views/auth/RegisterSuccessView.vue'),
          meta: {
            heroBadge: 'Chenxi Signup',
            heroTitle: '注册成功',
            heroDescription: '您的账号已创建成功，现在可以登录并开始使用辰汐图床了。',
          },
        },
        {
          path: '/reset-password-success',
          name: 'reset-password-success',
          component: () => import('../views/auth/ResetPasswordSuccessView.vue'),
          meta: {
            heroBadge: 'Chenxi Recovery',
            heroTitle: '密码重置成功',
            heroDescription: '您的密码已成功重置，现在可以使用新密码登录了。',
          },
        },
      ],
    },
    {
      path: '/user',
      component: () => import('../layouts/UserLayout.vue'),
      meta: { requiresAuth: true, pageTransitionOrder: 30 },
      children: [
        {
          path: '',
          name: 'user-home',
          component: () => import('../views/user/UserHomeView.vue'),
          meta: { label: '会员中心' },
        },
        {
          path: 'images',
          name: 'user-images',
          component: () => import('../views/user/UserImagesView.vue'),
          meta: { label: '媒体管理' },
        },
        {
          path: 'profile',
          name: 'user-profile',
          component: () => import('../views/user/UserProfileView.vue'),
          meta: { label: '用户信息' },
        },
        {
          path: 'security',
          name: 'user-security',
          component: () => import('../views/user/UserSecurityView.vue'),
          meta: { label: '安全设置' },
        },
        {
          path: 'api',
          name: 'user-api',
          component: () => import('../views/user/UserApiManagerView.vue'),
          meta: { label: 'API 接口管理' },
        },
        {
          path: 'albums',
          name: 'user-albums',
          component: () => import('../views/user/AlbumView.vue'),
          meta: { label: '我的图集' },
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', redirect: { name: 'admin-dashboard' } },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { label: '总览控制台' },
        },
        {
          path: 'images',
          name: 'admin-images',
          component: () => import('../views/AdminImageManagerView.vue'),
          meta: { label: '媒体管理' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/AdminUserManagementView.vue'),
          meta: { label: '用户管理' },
        },
        {
          path: 'system',
          name: 'admin-system',
          component: () => import('../views/SystemConfigView.vue'),
          meta: { label: '系统配置' },
        },
        {
          path: 'storage',
          name: 'admin-storage',
          component: () => import('../views/StorageStrategyView.vue'),
          meta: { label: '存储策略' },
        },
        {
          path: 'security',
          name: 'admin-security',
          component: () => import('../views/SecurityCenterView.vue'),
          meta: { label: '权限与安全' },
        },
        {
          path: 'mail-settings',
          name: 'admin-mail-settings',
          component: () => import('../views/admin/AdminMailSettingsView.vue'),
          meta: { label: '邮件设置' },
        },
        {
          path: 'integration',
          name: 'admin-integration',
          component: () => import('../views/IntegrationView.vue'),
          meta: { label: '集成与 API' },
        },
        {
          path: 'announcements',
          name: 'admin-announcements',
          component: () => import('../views/admin/AdminAnnouncementListView.vue'),
          meta: { label: '公告管理' },
        },
        {
          path: 'announcements/create',
          name: 'admin-announcement-create',
          component: () => import('../views/admin/AdminAnnouncementEditView.vue'),
          meta: { label: '新建公告' },
        },
        {
          path: 'announcements/:id',
          name: 'admin-announcement-edit',
          component: () => import('../views/admin/AdminAnnouncementEditView.vue'),
          meta: { label: '编辑公告' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

// 使用函数获取 store 实例，避免在模块初始化时导入
let authStore = null
let uiStore = null

const getAuthStore = () => {
  if (!authStore) {
    authStore = useAuthStore()
  }
  return authStore
}

const getUiStore = () => {
  if (!uiStore) {
    uiStore = useUiStore()
  }
  return uiStore
}

router.beforeEach((to, _from, next) => {
  const auth = getAuthStore()
  const ui = getUiStore()

  ui.setPageLoading(true)
  auth.pruneIfExpired()

  if (!to.meta.public && to.meta.requiresAuth && !auth.isAuthenticated) {
    next({ path: '/', query: { login: '1', redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/user')
    return
  }
  next()
})

router.afterEach(() => {
  const ui = getUiStore()
  window.setTimeout(() => ui.setPageLoading(false), 180)
})

export default router
