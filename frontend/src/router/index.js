import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
      { path: 'guarantee', name: 'Guarantee', component: () => import('../views/Guarantee.vue'), meta: { title: '安居保函' } },
      { path: 'loan', name: 'Loan', component: () => import('../views/Loan.vue'), meta: { title: '青创e贷' } },
      { path: 'budget', name: 'Budget', component: () => import('../views/Budget.vue'), meta: { title: '消费预算' } },
      { path: 'bookkeeping', name: 'Bookkeeping', component: () => import('../views/Bookkeeping.vue'), meta: { title: '经营赋能' } },
      { path: 'safety', name: 'Safety', component: () => import('../views/Safety.vue'), meta: { title: '金融安全' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const store = useUserStore()
  if (to.path !== '/login' && !store.isLogin) {
    next('/login')
  } else {
    next()
  }
})

export default router
