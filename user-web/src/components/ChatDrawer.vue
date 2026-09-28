<template>
  <transition name="msg-panel">
    <div v-if="uiStore.chatOpen" class="chat-drawer">
      <div class="panel-head">
        <div class="panel-title">
          <el-icon :size="16"><ChatLineSquare /></el-icon>
          <span>智能对话</span>
        </div>
        <div class="panel-head-actions">
          <el-tag size="small" effect="plain" type="warning" style="margin-right:8px">本地 / AI 增强</el-tag>
          <el-button size="small" text @click="uiStore.closeChat()">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="panel-body">
        <ChatPanel />
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ChatLineSquare, Close } from '@element-plus/icons-vue'
import { useUiStore } from '@/store/ui'
import ChatPanel from './ChatPanel.vue'

const uiStore = useUiStore()
</script>

<style scoped>
.chat-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 400px;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 0;
  flex-shrink: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-head-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.panel-body {
  flex: 1;
  min-height: 0;
  padding: 12px 16px 16px;
}

/* 滑出动画（与消息侧栏一致） */
.msg-panel-enter-active,
.msg-panel-leave-active {
  transition: transform 0.3s ease;
}

.msg-panel-enter-from,
.msg-panel-leave-to {
  transform: translateX(100%);
}
</style>
