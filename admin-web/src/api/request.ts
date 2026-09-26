import axios, { AxiosError } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

// ============================================================
// Axios 请求封装（管理端）
// 管理端接口前缀：/api/admin/v1
// ============================================================

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res: ApiResponse = response.data
    if (res.code === 0) {
      return res.data
    }
    // 登录过期
    if (res.code === 1002 || res.code === 1004 || res.code === 1005) {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        showCancelButton: false,
        type: 'warning'
      }).then(() => {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      })
      return Promise.reject(new Error(res.message))
    }
    // 权限不足
    if (res.code === 1003) {
      ElMessage.error('权限不足，无法访问')
      return Promise.reject(new Error(res.message))
    }
    // 其他错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error: AxiosError) => {
    console.error('请求错误：', error)
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
