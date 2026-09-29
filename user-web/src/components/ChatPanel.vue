<template>
  <div class="chat-panel">
    <!-- 引擎模式选择 + 引擎提示 -->
    <div class="panel-toolbar">
      <el-segmented v-model="selectedMode" :options="engineOptions" size="small" @change="onModeChange" />
      <el-tag type="warning" size="small">模拟对话引擎</el-tag>
    </div>
    <el-alert
      v-if="engineStatus?.notice"
      :title="engineStatus.notice"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 8px"
    />

    <!-- 消息列表 -->
    <div ref="msgBoxRef" class="msg-box" v-loading="historyLoading">
      <div v-if="!messages.length && !historyLoading" class="empty-tip">
        <el-empty description="暂无对话，发送一条消息开始吧" :image-size="60" />
      </div>
      <div
        v-for="(m, i) in messages"
        :key="i"
        class="msg-item"
        :class="m.role === 'user' ? 'msg-user' : 'msg-assistant'"
      >
        <el-avatar :size="28" class="msg-avatar">
          {{ m.role === 'user' ? '我' : 'AI' }}
        </el-avatar>
        <div class="msg-content">
          <div class="msg-text">{{ m.content }}</div>
          <div v-if="m.sources?.length" class="msg-sources">
            <el-tag
              v-for="(src, idx) in m.sources"
              :key="idx"
              size="small"
              :type="sourceTagType(src.sourceType)"
              class="src-tag"
            >
              {{ sourceTypeName(src.sourceType) }}
            </el-tag>
          </div>
          <div v-if="m.action" class="msg-action">
            <el-button size="small" type="primary" plain class="grad-white-btn" @click="goAction(m.action)">{{ m.action.label }}</el-button>
          </div>
          <div class="msg-time">{{ formatTime(m.timestamp) }}</div>
        </div>
      </div>
    </div>

    <!-- 常见疑问推荐（淘宝客服风：点击即发送） -->
    <div v-if="!sending" class="quick-asks">
      <div class="quick-title">猜你想问</div>
      <div class="quick-list">
        <el-tag
          v-for="q in quickQuestions"
          :key="q"
          class="quick-tag"
          effect="plain"
          @click="quickAsk(q)"
        >{{ q }}</el-tag>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入问题，例如：如何申请保函？"
        @keyup.enter="handleSend"
        :disabled="sending"
      />
      <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEngineStatus, sendMessage, getChatHistory } from '@/api/chat'

const inputText = ref('')
const messages = ref([])
const sending = ref(false)
const historyLoading = ref(false)
const engineStatus = ref(null)
const selectedMode = ref('local')
const msgBoxRef = ref(null)
const router = useRouter()

const quickQuestions = [
  '如何申请保函？费率是多少？',
  '我是大学生，能申请青创e贷吗？',
  '收到中奖短信要交手续费，是诈骗吗？',
  '怎么查看我的征信报告？',
  '有哪些创业补贴政策？',
  '预算记账怎么分类？',
]
const engineOptions = [
  { label: '本地模式', value: 'local' },
  { label: 'AI 增强', value: 'agent' },
]

function engineModeName(mode) {
  return { local: '本地引擎', agent: 'AI 增强', fallback: 'AI 降级本地' }[mode] || mode
}

function onModeChange(val) {
  if (val === 'agent' && engineStatus.value && !engineStatus.value.agentAvailable) {
    ElMessage.warning('AI 增强暂不可用（未配置 LLM），回答将自动降级为本地引擎')
  }
}

function goAction(action) {
  if (action?.url) router.push(action.url)
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
    if (engineStatus.value && engineStatus.value.activeEngine) {
      selectedMode.value = engineStatus.value.activeEngine
    }
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
  messages.value.push({ role: 'user', content: text, timestamp: new Date().toISOString().replace('Z', '') })
  inputText.value = ''
  sending.value = true
  await scrollBottom()
  try {
    const reply = await sendMessage(text, selectedMode.value)
    messages.value.push(reply)
    await scrollBottom()
  } catch (e) {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

async function quickAsk(q) {
  if (sending.value) return
  messages.value.push({ role: 'user', content: q, timestamp: new Date().toISOString().replace('Z', '') })
  sending.value = true
  await scrollBottom()
  try {
    const reply = await sendMessage(q, selectedMode.value)
    messages.value.push(reply)
    await scrollBottom()
  } catch (e) {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
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
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.msg-box {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
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
  gap: 8px;
  margin-bottom: 12px;
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
  max-width: 82%;
}
.msg-text {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
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
.msg-sources {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.src-tag {
  cursor: default;
}
.msg-action {
  margin-top: 6px;
}
.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: #c0c4cc;
}
.quick-asks {
  margin-top: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 8px;
  flex-shrink: 0;
  max-height: 96px;
  overflow-y: auto;
}
.quick-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.quick-tag {
  cursor: pointer;
  margin: 0;
}
.quick-tag:hover {
  color: #409eff;
  border-color: #409eff;
}
.input-area {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  align-items: flex-end;
  flex-shrink: 0;
}
.input-area .el-button {
  height: 56px;
}
</style>
