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
      path: '/search',
      name: 'public-search',
      component: () => import('../views/public/PublicTagSearchView.vue'),
      meta: { public: true, pageTransitionOrder: 5 },
    },
    {
      path: '/users/:userId',
      name: 'public-user-profile',
      component: () => import('../views/public/PublicUserProfileView.vue'),
      meta: { public: true, pageTransitionOrder: 0 },
    },
    {
      path: '/auth',
      component: AuthLayout,
      meta: { public: true, pageTransitionOrder: 0 },
      children: [
        {
          path: '/login',
          name: 'login',
          component: () => import('../views/auth/LoginView.vue'),
          meta: {
            authLayout: 'centered',
            heroBadge: 'Chenxi Console',
            heroTitle: '零信任认证 · 极速图库治理',
            heroDescription: '统一访问控制台，联通上传中心、审计大屏、API 密钥与对象存储治理。账号全程启用行为验证与风控评分。',
            heroFeatures: [
              '粘贴板极速上传 · 自动命名',
              '落盘映射 /upload/** · 全链路审计',
            ],
            pageTransitionOrder: 10,
          },
        },
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
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  const ui = useUiStore()

  ui.setPageLoading(true)

  if (!to.meta.public && to.meta.requiresAuth && !auth.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/user')
    return
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    next('/user')
    return
  }
  next()
})

router.afterEach(() => {
  const ui = useUiStore()
  window.setTimeout(() => ui.setPageLoading(false), 180)
})

export default router
