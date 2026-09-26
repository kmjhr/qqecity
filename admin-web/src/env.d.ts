/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/** 通用 API 响应 */
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页参数 */
interface PageParams {
  pageNum: number
  pageSize: number
  keyword?: string
}

/** 分页结果 */
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 用户信息 */
interface UserInfo {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  role: 'USER' | 'ADMIN'
  status: number
  createTime: string
}

/** 登录返回 */
interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}
