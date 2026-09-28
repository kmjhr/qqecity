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
    unreadCount: 0,
    // 智能对话侧栏是否打开
    chatOpen: false
  }),
  actions: {
    togglePanel() {
      // 打开消息侧栏时关闭对话侧栏（互斥，避免重叠遮挡）
      if (this.chatOpen) {
        this.chatOpen = false
      }
      this.panelOpen = !this.panelOpen
    },
    openPanel(tab = 'msg') {
      this.panelTab = tab
      this.chatOpen = false
      this.panelOpen = true
    },
    closePanel() {
      this.panelOpen = false
    },
    openChat() {
      // 打开对话侧栏时关闭消息侧栏（互斥，避免同时占 400px）
      this.panelOpen = false
      this.chatOpen = true
    },
    closeChat() {
      this.chatOpen = false
    },
    setUnread(n) {
      this.unreadCount = Number(n) || 0
    },
    decreaseUnread() {
      this.unreadCount = Math.max(0, this.unreadCount - 1)
    }
  }
})
