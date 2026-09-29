<template>
  <transition name="msg-panel">
    <div v-if="uiStore.panelOpen" class="msg-panel">
      <div class="panel-head">
        <div class="panel-title">
          <el-icon :size="16"><Bell /></el-icon>
          <span>消息中心</span>
        </div>
        <div class="panel-head-actions">
          <el-button size="small" type="primary" link class="grad-white-btn" @click="handleMarkAll" :disabled="uiStore.unreadCount === 0">
            全部已读
          </el-button>
          <el-button size="small" text @click="uiStore.closePanel()">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <el-tabs v-model="uiStore.panelTab" class="panel-tabs">
        <!-- 消息 Tab -->
        <el-tab-pane label="消息" name="msg">
          <div class="msg-body" v-loading="loading">
            <el-empty v-if="list.length === 0 && !loading" description="暂无消息" :image-size="60" />
            <div
              v-for="item in list"
              :key="item.id"
              class="message-item"
              :class="{ unread: item.isRead === 0 }"
              @click="handleRead(item)"
            >
              <div class="msg-icon" :class="typeClass(item.type)">
                <el-icon :size="18">
                  <Bell v-if="item.type === 'SYSTEM'" />
                  <TrendCharts v-else-if="item.type === 'BUDGET'" />
                  <OfficeBuilding v-else />
                </el-icon>
              </div>
              <div class="msg-content">
                <div class="msg-title-row">
                  <span class="msg-title">{{ item.title }}</span>
                  <span class="msg-time">{{ formatTime(item.createTime) }}</span>
                </div>
                <p class="msg-desc">{{ item.content }}</p>
              </div>
              <div v-if="item.isRead === 0" class="unread-dot"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 对话 Tab：内置智能对话（本地 / AI 增强） -->
        <el-tab-pane label="对话" name="chat">
          <div class="chat-wrap">
            <ChatPanel />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, TrendCharts, OfficeBuilding, Close } from '@element-plus/icons-vue'
import { useUiStore } from '@/store/ui'
import { getMessagePage, markRead, markAllRead, getUnreadCount } from '@/api/message'
import ChatPanel from './ChatPanel.vue'

const uiStore = useUiStore()
const loading = ref(false)
const list = ref([])

onMounted(() => {
  loadMessages()
  loadUnreadCount()
})

// 侧栏每次打开时自动刷新（消息/未读数保持最新）
watch(
  () => uiStore.panelOpen,
  (open) => {
    if (open) {
      loadMessages()
      loadUnreadCount()
    }
  }
)

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessagePage({ pageNum: 1, pageSize: 20 })
    list.value = res.records || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const count = await getUnreadCount()
    uiStore.setUnread(count || 0)
  } catch (e) {
    uiStore.setUnread(0)
  }
}

function handleRead(item) {
  if (item.isRead === 1) return
  markRead(item.id).then(() => {
    item.isRead = 1
    uiStore.decreaseUnread()
  })
}

function handleMarkAll() {
  markAllRead().then(() => {
    ElMessage.success('已全部标记为已读')
    list.value.forEach(item => item.isRead = 1)
    uiStore.setUnread(0)
  })
}

function typeClass(type) {
  const map = {
    SYSTEM: 'type-system',
    BUDGET: 'type-budget',
    BUSINESS: 'type-business'
  }
  return map[type] || 'type-system'
}

function formatTime(time) {
  if (!time) return ''
  return String(time).replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.msg-panel {
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

.panel-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 16px;
}

.panel-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
  flex-shrink: 0;
}

.panel-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.panel-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.msg-body {
  height: 100%;
  overflow-y: auto;
  padding-bottom: 12px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #f2f2f2;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
  border-radius: 8px;
}

.message-item:hover {
  background: #fafafa;
}

.message-item.unread {
  background: #f0f9ff;
}

.msg-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.type-system {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.type-budget {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.type-business {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.msg-content {
  flex: 1;
  min-width: 0;
}

.msg-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.msg-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  flex-shrink: 0;
}

.msg-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
  margin-top: 6px;
}

.chat-wrap {
  height: 100%;
  min-height: 0;
}

/* 滑出动画 */
.msg-panel-enter-active,
.msg-panel-leave-active {
  transition: transform 0.3s ease;
}

.msg-panel-enter-from,
.msg-panel-leave-to {
  transform: translateX(100%);
}
</style>
