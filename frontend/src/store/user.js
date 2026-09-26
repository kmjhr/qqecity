import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('qingqi_token') || '',
    userInfo: null
  }),
  getters: {
    isLogin: state => !!state.token
  },
  actions: {
    async login(phone, password) {
      const data = await authApi.login({ phone, password })
      this.token = data.token
      localStorage.setItem('qingqi_token', data.token)
      this.userInfo = data
    },
    async fetchMe() {
      this.userInfo = await authApi.me()
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('qingqi_token')
    }
  }
})
