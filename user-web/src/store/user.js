import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, register as apiRegister } from '@/api/auth'
import { getProfile } from '@/api/user'

// ============================================================
// 用户状态管理
// 存储：Token、用户信息、刷新令牌
// 持久化：localStorage
// ============================================================

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.userInfo?.nickname || state.userInfo?.username || '',
    userRole: state => state.userInfo?.role || ''
  },

  actions: {
    /** 登录 */
    async login(loginForm) {
      const data = await apiLogin(loginForm)
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      this.userInfo = data.user
      // 持久化
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('userInfo', JSON.stringify(data.user))
      return data
    },

    /** 注册 */
    async register(form) {
      return await apiRegister(form)
    },

    /** 获取用户信息 */
    async fetchProfile() {
      const data = await getProfile()
      this.userInfo = data
      localStorage.setItem('userInfo', JSON.stringify(data))
      return data
    },

    /** 登出 */
    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        // 忽略登出接口错误
      }
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    }
  }
})
