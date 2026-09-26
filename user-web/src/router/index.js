import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// ============================================================
// 用户前端路由配置
// 对齐文档《需求清单》五大模块 + 公共支撑
// 路由守卫：需要登录的页面检查 token
// 布局：MainLayout（顶部导航 + 内容区）
// ============================================================

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      // 场景一：安居金融风控
      {
        path: 'guarantee',
        name: 'Guarantee',
        component: () => import('@/views/Guarantee.vue'),
        meta: { title: '安居保函', requiresAuth: true }
      },
      // 场景二：轻创业智能授信
      {
        path: 'loan',
        name: 'Loan',
        component: () => import('@/views/Loan.vue'),
        meta: { title: '青创e贷', requiresAuth: true }
      },
      // 场景二配套：创业经营赋能
      {
        path: 'bookkeeping',
        name: 'Bookkeeping',
        component: () => import('@/views/Bookkeeping.vue'),
        meta: { title: '经营赋能', requiresAuth: true }
      },
      // 场景三：碎片消费治理
      {
        path: 'budget',
        name: 'Budget',
        component: () => import('@/views/Budget.vue'),
        meta: { title: '预算消费', requiresAuth: true }
      },
      // 场景三配套：青年金融安全
      {
        path: 'safety',
        name: 'Safety',
        component: () => import('@/views/Safety.vue'),
        meta: { title: '金融安全', requiresAuth: true }
      },
      // 公共支撑：消息中心
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/Message.vue'),
        meta: { title: '消息中心', requiresAuth: true }
      },
      // 公共支撑：个人中心
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录检查
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = `${to.meta.title || ''} - 青启e城`

  if (to.meta.requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
