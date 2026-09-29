import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// ============================================================
// Axios 请求封装
// 功能：
// 1. 统一请求头（Token 注入）
// 2. 统一响应处理（错误码提示、登录过期跳转）
// 3. 统一错误处理
// ============================================================

const request = axios.create({
  baseURL: '/api',
  // 60s：agent 模式下 LLM 在本地 CPU 推理可能超过 15s（7B 模型首次可达 30-60s）
  timeout: 60000
})

// 登录过期处理防抖标志：并发 1002/1004/1005 只弹一次、只跳一次
let authRedirecting = false

// 请求拦截器：注入 Token
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理返回结果
request.interceptors.response.use(
  response => {
    const res = response.data
    // 后端统一返回格式：{ code, message, data }
    if (res.code === 0) {
      return res.data
    }
    // 登录过期 / Token 无效
    if (res.code === 1002 || res.code === 1004 || res.code === 1005) {
      // 本地直接清理会话（不调后端 logout 接口：无 token 时登出接口同样返回 1002，
      // 会触发本分支 → 递归弹窗风暴）
      const userStore = useUserStore()
      userStore.token = ''
      userStore.refreshToken = ''
      userStore.userInfo = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      // 并发多个请求同时 1002 时只提示一次、只跳转一次（防抖窗口 2s）
      if (!authRedirecting) {
        authRedirecting = true
        if (router.currentRoute.value.path !== '/login') {
          ElMessage.warning('登录已过期，请重新登录')
        }
        router.push('/login')
        setTimeout(() => { authRedirecting = false }, 2000)
      }
      return Promise.reject(new Error(res.message))
    }
    // 其他业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    console.error('请求错误：', error)
    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
