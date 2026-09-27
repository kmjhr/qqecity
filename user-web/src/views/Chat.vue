<template>
  <div class="chat-page">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>智能对话引擎</span>
            <el-tag v-if="engineStatus" :type="engineStatus.agentAvailable ? 'success' : 'info'" size="small">
              {{ engineStatus.activeEngine === 'agent' ? 'AI 增强（agent）' : '本地模式（local）' }}
            </el-tag>
            <el-tag type="warning" size="small">模拟对话引擎</el-tag>
          </div>
          <el-popconfirm title="确定清空全部历史对话？" @confirm="handleClear">
            <template #reference>
              <el-button size="small" type="danger" plain>清空历史</el-button>
            </template>
          </el-popconfirm>
        </div>
      </template>

      <!-- 引擎提示 -->
      <el-alert
        v-if="engineStatus?.notice"
        :title="engineStatus.notice"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />

      <!-- 消息列表 -->
      <div ref="msgBoxRef" class="msg-box" v-loading="historyLoading">
        <div v-if="!messages.length && !historyLoading" class="empty-tip">
          <el-empty description="暂无对话，发送一条消息开始吧" />
        </div>
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg-item"
          :class="m.role === 'user' ? 'msg-user' : 'msg-assistant'"
        >
          <el-avatar :size="32" class="msg-avatar">
            {{ m.role === 'user' ? '我' : 'AI' }}
          </el-avatar>
          <div class="msg-content">
            <div class="msg-text">{{ m.content }}</div>
            <div v-if="m.engineMode" class="msg-meta">
              <el-tag size="small" type="info">{{ engineModeName(m.engineMode) }}</el-tag>
            </div>
            <div v-if="m.sources?.length" class="msg-sources">
              <span class="src-label">来源：</span>
              <el-tag
                v-for="(s, idx) in m.sources"
                :key="idx"
                size="small"
                :type="sourceTagType(s.sourceType)"
                class="src-tag"
              >
                {{ sourceTypeName(s.sourceType) }}：{{ s.title }}
              </el-tag>
            </div>
            <div class="msg-time">{{ formatTime(m.timestamp) }}</div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入问题，例如：如何申请保函？/ 创业贷款额度多少？"
          @keyup.enter="handleSend"
          :disabled="sending"
        />
        <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEngineStatus, sendMessage, getChatHistory, clearChatHistory } from '@/api/chat'

const inputText = ref('')
const messages = ref([])
const sending = ref(false)
const historyLoading = ref(false)
const engineStatus = ref(null)
const msgBoxRef = ref(null)

function engineModeName(mode) {
  return { local: '本地引擎', agent: 'AI 增强', fallback: 'AI 降级本地' }[mode] || mode
}
function sourceTypeName(t) {
  return { FAQ: 'FAQ', POLICY: '政策', ANTI_FRAUD: '反诈', LLM: 'LLM' }[t] || t
}
function sourceTagType(t) {
  return { FAQ: '', POLICY: 'success', ANTI_FRAUD: 'warning', LLM: 'info' }[t] || ''
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

async function loadEngineStatus() {
  try {
    engineStatus.value = await getEngineStatus()
  } catch (e) { /* 忽略 */ }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const list = await getChatHistory()
    messages.value = list || []
    await scrollBottom()
  } finally {
    historyLoading.value = false
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text) {
    ElMessage.warning('请输入消息内容')
    return
  }
  // 先把 user 消息上屏
  messages.value.push({ role: 'user', content: text, timestamp: new Date().toISOString().replace('Z', '') })
  inputText.value = ''
  sending.value = true
  await scrollBottom()
  try {
    const reply = await sendMessage(text)
    messages.value.push(reply)
    await scrollBottom()
  } finally {
    sending.value = false
  }
}

async function handleClear() {
  await clearChatHistory()
  messages.value = []
  ElMessage.success('历史已清空')
}

async function scrollBottom() {
  await nextTick()
  if (msgBoxRef.value) {
    msgBoxRef.value.scrollTop = msgBoxRef.value.scrollHeight
  }
}

onMounted(async () => {
  await Promise.all([loadEngineStatus(), loadHistory()])
})
</script>

<style scoped>
.chat-page {
  max-width: 900px;
  margin: 0 auto;
}
.chat-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-box {
  height: 460px;
  overflow-y: auto;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.msg-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.msg-user {
  flex-direction: row-reverse;
}
.msg-user .msg-content {
  align-items: flex-end;
}
.msg-avatar {
  flex-shrink: 0;
  background: #409eff;
  color: #fff;
}
.msg-user .msg-avatar {
  background: #67c23a;
}
.msg-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}
.msg-text {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-assistant .msg-text {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
}
.msg-user .msg-text {
  background: #409eff;
  color: #fff;
}
.msg-meta {
  margin-top: 4px;
}
.msg-sources {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.src-label {
  font-size: 12px;
  color: #909399;
}
.src-tag {
  cursor: default;
}
.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: #c0c4cc;
}
.input-area {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  align-items: flex-end;
}
.input-area .el-button {
  height: 60px;
}
</style>
