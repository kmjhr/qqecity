import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// 请求拦截：附加 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('qingqi_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：解包统一返回结构 {code, message, data}
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 2001) {
        localStorage.removeItem('qingqi_token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  error => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
