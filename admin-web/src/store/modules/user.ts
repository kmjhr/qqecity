import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'

// ============================================================
// 管理员状态管理
// ============================================================

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    refreshToken: localStorage.getItem('admin_refreshToken') || '',
    userInfo: JSON.parse(localStorage.getItem('admin_userInfo') || 'null') as UserInfo | null
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.userInfo?.nickname || state.userInfo?.username || ''
  },

  actions: {
    async login(form: { username: string; password: string }) {
      const data = await apiLogin(form)
      // 管理端要求 ADMIN 角色
      if (data.user.role !== 'ADMIN') {
        throw new Error('该账号无管理权限')
      }
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      this.userInfo = data.user
      localStorage.setItem('admin_token', data.accessToken)
      localStorage.setItem('admin_refreshToken', data.refreshToken)
      localStorage.setItem('admin_userInfo', JSON.stringify(data.user))
      return data
    },

    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        // 忽略
      }
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_refreshToken')
      localStorage.removeItem('admin_userInfo')
    }
  }
})
