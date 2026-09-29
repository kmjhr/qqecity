<template>
  <div class="message-page">
    <el-card shadow="never" class="message-header">
      <div class="header-left">
        <h2>消息中心</h2>
        <span class="unread-tip">
          <el-badge :value="unreadCount" :max="99" class="unread-badge">
            未读消息
          </el-badge>
        </span>
      </div>
      <el-button type="primary" link class="grad-white-btn" @click="handleMarkAll" :disabled="unreadCount === 0">
        全部已读
      </el-button>
    </el-card>

    <el-card shadow="never" class="message-list" v-loading="loading">
      <el-empty v-if="list.length === 0" description="暂无消息" />

      <div
        v-for="item in list"
        :key="item.id"
        class="message-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleRead(item)"
      >
        <div class="msg-icon" :class="typeClass(item.type)">
          <el-icon :size="20">
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

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="loadMessages"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, TrendCharts, OfficeBuilding } from '@element-plus/icons-vue'
import { getMessagePage, markRead, markAllRead, getUnreadCount } from '@/api/message'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const unreadCount = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

onMounted(() => {
  loadMessages()
  loadUnreadCount()
})

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessagePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    // 占位：后端接口未就绪时显示空状态
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const count = await getUnreadCount()
    unreadCount.value = count || 0
  } catch (e) {
    unreadCount.value = 0
  }
}

function handleRead(item) {
  if (item.isRead === 1) return
  markRead(item.id).then(() => {
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  })
}

function handleMarkAll() {
  markAllRead().then(() => {
    ElMessage.success('已全部标记为已读')
    list.value.forEach(item => item.isRead = 1)
    unreadCount.value = 0
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
  return time.split('T')[0]
}
</script>

<style scoped>
.message-page {
  max-width: 800px;
  margin: 0 auto;
}

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.message-header :deep(.el-card__body) {
  padding: 16px 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  font-size: 18px;
  margin: 0;
}

.unread-badge {
  font-size: 14px;
  color: #909399;
}

.message-list :deep(.el-card__body) {
  padding: 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 24px;
  border-bottom: 1px solid #f2f2f2;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.message-item:last-child {
  border-bottom: none;
}

.message-item:hover {
  background: #fafafa;
}

.message-item.unread {
  background: #f0f9ff;
}

.msg-icon {
  width: 40px;
  height: 40px;
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
  margin-bottom: 6px;
}

.msg-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.msg-time {
  font-size: 12px;
  color: #c0c4cc;
}

.msg-desc {
  font-size: 13px;
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

.pagination {
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}
</style>
