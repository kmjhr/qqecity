import { defineStore } from 'pinia'

// ============================================================
// 全局 UI 状态：消息侧栏（消息中心 + 智能对话 侧边栏展示）
// ============================================================
export const useUiStore = defineStore('ui', {
  state: () => ({
    // 消息侧栏是否打开
    panelOpen: false,
    // 当前侧栏 Tab：msg=消息 / chat=对话
    panelTab: 'msg',
    // 未读消息数
    unreadCount: 0
  }),
  actions: {
    togglePanel() {
      this.panelOpen = !this.panelOpen
    },
    openPanel(tab = 'msg') {
      this.panelTab = tab
      this.panelOpen = true
    },
    closePanel() {
      this.panelOpen = false
    },
    setUnread(n) {
      this.unreadCount = Number(n) || 0
    },
    decreaseUnread() {
      this.unreadCount = Math.max(0, this.unreadCount - 1)
    }
  }
})
