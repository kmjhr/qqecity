import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

// ============================================================
// 管理前端路由配置
// 布局：侧边栏 + 顶栏 + 内容区
// ============================================================

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false, hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis', requiresAuth: true }
      },
      {
        path: 'system',
        name: 'System',
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          {
            path: 'user',
            name: 'UserManage',
            component: () => import('@/views/system/user/index.vue'),
            meta: { title: '用户管理', icon: 'User', requiresAuth: true }
          },
          {
            path: 'landlord/list',
            name: 'LandlordManage',
            component: () => import('@/views/landlord/index.vue'),
            meta: { title: '房东管理', icon: 'House', requiresAuth: true }
          }
          // 角色管理、菜单管理等在此扩展
        ]
      },
      // 步骤 8 新增：业务审核台 5 队列
      {
        path: 'business',
        name: 'Business',
        redirect: '/business/ai-review',
        meta: { title: '业务审核', icon: 'Checked' },
        children: [
          {
            path: 'ai-review',
            name: 'AIReview',
            component: () => import('@/views/business/ai-review/index.vue'),
            meta: { title: 'AI 复审队列', icon: 'View', requiresAuth: true }
          },
          {
            path: 'claim-review',
            name: 'ClaimReview',
            component: () => import('@/views/business/claim-review/index.vue'),
            meta: { title: '索赔复核队列', icon: 'Document', requiresAuth: true }
          },
          {
            path: 'loan-review',
            name: 'LoanReview',
            component: () => import('@/views/business/loan-review/index.vue'),
            meta: { title: '贷款审批', icon: 'Money', requiresAuth: true }
          },
          {
            path: 'merchant-audit',
            name: 'MerchantAudit',
            component: () => import('@/views/business/merchant-audit/index.vue'),
            meta: { title: '商户白名单', icon: 'Shop', requiresAuth: true }
          },
          {
            path: 'entrust-review',
            name: 'EntrustReview',
            component: () => import('@/views/business/entrust-review/index.vue'),
            meta: { title: '受托支付复核', icon: 'Money', requiresAuth: true }
          },
          {
            path: 'guarantee-manage',
            name: 'GuaranteeManage',
            component: () => import('@/views/business/guarantee-manage/index.vue'),
            meta: { title: '保函管理', icon: 'Document', requiresAuth: true }
          },
          {
            path: 'registration-review',
            name: 'RegistrationReview',
            component: () => import('@/views/business/registration-review/index.vue'),
            meta: { title: '注册审核', icon: 'UserFilled', requiresAuth: true }
          },
          {
            path: 'risk-overview',
            name: 'RiskOverview',
            component: () => import('@/views/business/risk-overview/index.vue'),
            meta: { title: '风险预警总览', icon: 'Warning', requiresAuth: true }
          }
        ]
      },
      // 内容管理（安全教育平台 + 政策专区）
      {
        path: 'content',
        name: 'Content',
        redirect: '/content/alert-manage',
        meta: { title: '内容管理', icon: 'Notebook' },
        children: [
          {
            path: 'alert-manage',
            name: 'AlertManage',
            component: () => import('@/views/content/alert-manage/index.vue'),
            meta: { title: '实时预警管理', icon: 'Bell', requiresAuth: true }
          },
          {
            path: 'anti-fraud-manage',
            name: 'AntiFraudManage',
            component: () => import('@/views/content/anti-fraud-manage/index.vue'),
            meta: { title: '反诈教学内容', icon: 'Reading', requiresAuth: true }
          },
          {
            path: 'portal-manage',
            name: 'PortalManage',
            component: () => import('@/views/content/portal-manage/index.vue'),
            meta: { title: '政策门户管理', icon: 'Link', requiresAuth: true }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  document.title = `${to.meta.title || ''} - 青启e城管理后台`

  if (to.meta.requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
