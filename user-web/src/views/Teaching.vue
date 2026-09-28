<template>
  <div class="teaching-page">
    <el-row :gutter="20">
      <!-- 左侧：情景列表 -->
      <el-col :span="8">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="list-header">
              <span>反诈情景模拟</span>
              <el-button class="history-btn" @click="openHistory">演练记录</el-button>
            </div>
          </template>
          <div v-loading="listLoading">
            <div
              v-for="s in scenarios"
              :key="s.id"
              class="scenario-item"
              :class="{ active: current?.id === s.id }"
              @click="selectScenario(s)"
            >
              <div class="scenario-item-top">
                <el-tag :type="categoryTagType(s.category)" size="small">
                  {{ s.category || '其他' }}
                </el-tag>
                <el-tag v-if="s.contentType === 'SCENARIO_DIALOG'" type="success" size="small">
                  对话演练
                </el-tag>
                <el-tag v-else type="info" size="small">答题</el-tag>
              </div>
              <div class="scenario-title">{{ s.title }}</div>
              <div class="scenario-summary">{{ s.summary }}</div>
            </div>
            <el-empty v-if="!listLoading && !scenarios.length" description="暂无情景内容" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：演练/作答区 -->
      <el-col :span="16">
        <el-card shadow="never" class="detail-card" v-loading="detailLoading">
          <template #header>
            <div class="detail-header">
              <span>{{ current?.title || '请选择左侧情景开始学习' }}</span>
              <el-tag v-if="current" type="warning" size="small">AI 模拟教学</el-tag>
            </div>
          </template>

          <!-- ==================== 对话式演练（SCENARIO_DIALOG） ==================== -->
          <div v-if="current && current.contentType === 'SCENARIO_DIALOG'" class="dialog-area">
            <el-alert
              title="AI 扮演诈骗分子与你实时对话，用你自己的话应对（可输入：挂断、拨打96110、拒绝转账、核实身份…）。安全分 ≥ 70 判识破，连续 2 回合低分判被诱骗，10 回合未定判超时。"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />

            <div v-if="!practice" class="dialog-start">
              <div class="bg-block">
                <h4>情景背景</h4>
                <p style="white-space: pre-line">{{ current.background }}</p>
              </div>
              <el-button type="primary" size="large" :loading="startLoading" @click="enterDialog(current)">
                开始对话演练
              </el-button>
            </div>

            <template v-else>
              <div ref="chatBox" class="chat-box">
                <div
                  v-for="(m, i) in messages"
                  :key="i"
                  class="msg-row"
                  :class="m.speaker === 'USER' ? 'row-user' : 'row-fraud'"
                >
                  <div class="msg-avatar" :class="m.speaker === 'USER' ? 'av-user' : 'av-fraud'">
                    {{ m.speaker === 'USER' ? '我' : '骗' }}
                  </div>
                  <div class="msg-body">
                    <div class="msg-bubble" :class="m.speaker === 'USER' ? 'bubble-user' : 'bubble-fraud'">
                      {{ m.content }}
                    </div>
                    <div v-if="m.speaker === 'USER' && m.score !== undefined" class="msg-meta">
                      <el-tag
                        :type="m.level === 'SAFE' ? 'success' : m.level === 'DANGEROUS' ? 'danger' : 'info'"
                        size="small"
                      >安全分 {{ m.score }}</el-tag>
                      <el-alert
                        v-if="m.hint"
                        :title="m.hint"
                        type="error"
                        :closable="false"
                        show-icon
                        class="msg-hint"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="!practiceOver" class="dialog-input">
                <el-input
                  v-model="userInput"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="输入你的应对（回车发送）"
                  @keydown.enter.exact.prevent="handleSend"
                  :disabled="sending"
                />
                <div class="dialog-input-bar">
                  <el-button type="primary" :loading="sending" :disabled="!userInput.trim()" @click="handleSend">
                    发送
                  </el-button>
                  <el-button plain @click="handleFinish">提前结束</el-button>
                  <span class="user-turn-hint">已发言 {{ userTurnNo }} / 10 回合</span>
                </div>
              </div>

              <!-- 复盘卡片 -->
              <div v-if="practiceOver && finishResult" class="review-card">
                <el-result
                  :icon="finishResult.result === 'SAFE' ? 'success' : finishResult.result === 'LURED' ? 'error' : 'warning'"
                  :title="`演练结果：${finishResult.resultName}`"
                  :sub-title="`综合风险分 ${finishResult.riskScore}/100（${finishResult.warningLevel}）`"
                >
                  <template #extra>
                    <el-button type="primary" @click="enterDialog(current)">再演练一次</el-button>
                    <el-button plain @click="openHistory">查看演练记录</el-button>
                  </template>
                </el-result>
                <el-alert
                  v-if="finishResult.resultDesc"
                  :title="finishResult.resultDesc"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin: 12px 0"
                />
                <div v-if="finishResult.reviewPoints?.length" class="review-points">
                  <h4>复盘要点</h4>
                  <el-tag
                    v-for="(p, i) in finishResult.reviewPoints"
                    :key="i"
                    type="warning"
                    effect="plain"
                    class="review-tag"
                  >{{ p }}</el-tag>
                </div>
              </div>
            </template>
          </div>

          <!-- ==================== 选择题作答（SCENARIO_SIM） ==================== -->
          <div v-else-if="!current" class="empty-detail">
            <el-empty description="点击左侧情景开始模拟演练" />
          </div>

          <div v-else-if="!submitted" class="answer-area">
            <el-alert
              v-if="current.simulationNotice"
              :title="current.simulationNotice"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 16px"
            />
            <div class="bg-block">
              <h4>情景背景</h4>
              <p style="white-space: pre-line">{{ current.background }}</p>
            </div>
            <div
              v-for="q in current.questions || []"
              :key="q.questionNo"
              class="question-block"
            >
              <h4>{{ q.questionNo }}. {{ q.stem }}</h4>
              <el-radio-group v-model="answers[q.questionNo]" class="option-group">
                <el-radio
                  v-for="opt in q.options || []"
                  :key="opt.key"
                  :label="opt.key"
                  class="option-item"
                >
                  {{ opt.key }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </div>
            <div class="submit-bar">
              <el-button
                type="primary"
                :loading="submitLoading"
                :disabled="(current.questions || []).some(q => !answers[q.questionNo])"
                @click="handleSubmit"
              >
                提交答卷
              </el-button>
              <span class="submit-hint">全部作答后可提交</span>
            </div>
          </div>

          <div v-else class="result-area">
            <el-result
              :icon="submitResult?.allCorrect ? 'success' : 'warning'"
              :title="`得分 ${submitResult?.score ?? 0} 分（${submitResult?.correctCount || 0}/${submitResult?.totalQuestions || 0} 答对）`"
              :sub-title="`正确率 ${submitResult?.accuracy ?? 0}%`"
            >
              <template #extra>
                <el-button type="primary" @click="resetAnswer">再答一次</el-button>
              </template>
            </el-result>

            <el-alert
              v-if="submitResult?.overallExplanation"
              :title="submitResult.overallExplanation"
              type="info"
              :closable="false"
              show-icon
              style="margin: 12px 0"
            />
            <el-alert
              v-if="submitResult?.preventionTips"
              :title="'防范要点：' + submitResult.preventionTips"
              type="success"
              :closable="false"
              show-icon
              style="margin: 12px 0"
            />

            <div
              v-for="(q, i) in submitResult?.questions || []"
              :key="i"
              class="result-question"
              :class="q.correct ? 'right' : 'wrong'"
            >
              <div class="rq-head">
                <el-tag :type="q.correct ? 'success' : 'danger'" size="small">
                  {{ q.correct ? '答对' : '答错' }}
                </el-tag>
                <span class="rq-stem">{{ q.questionNo }}. {{ q.stem }}</span>
              </div>
              <div class="rq-ans">
                <div>你的选择：{{ q.userChoice || '未选' }}</div>
                <div v-if="q.correction" class="rq-correction">纠偏：{{ q.correction }}</div>
                <div v-if="q.explanation" class="rq-explanation">解析：{{ q.explanation }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 演练历史抽屉 -->
    <el-drawer v-model="historyVisible" title="我的演练记录" size="420px">
      <div v-loading="historyLoading">
        <el-empty v-if="!historyLoading && !historyList.length" description="暂无演练记录" />
        <div
          v-for="h in historyList"
          :key="h.practiceNo"
          class="history-item"
          @click="openReplay(h.practiceNo)"
        >
          <div class="hi-top">
            <el-tag :type="historyTagType(h.result)" size="small">{{ resultName(h.result) }}</el-tag>
            <span class="hi-time">{{ (h.endedAt || h.startedAt || '').replace('T', ' ') }}</span>
          </div>
          <div class="hi-title">{{ h.scenarioTitle }}</div>
          <div class="hi-meta">风险分 {{ h.riskScore }}/100 · 共 {{ h.roundCount }} 条消息</div>
        </div>
      </div>
    </el-drawer>

    <!-- 回放抽屉 -->
    <el-drawer v-model="replayVisible" title="演练回放" size="420px">
      <div v-if="replayData" v-loading="replayLoading">
        <div class="replay-head">
          <el-tag :type="historyTagType(replayData.result)" size="small">{{ resultName(replayData.result) }}</el-tag>
          <span class="hi-meta">风险分 {{ replayData.riskScore }}/100</span>
        </div>
        <div class="replay-desc">{{ replayData.resultDesc }}</div>
        <div
          v-for="r in replayData.rounds || []"
          :key="r.roundNo"
          class="replay-round"
          :class="r.speaker === 'USER' ? 'rr-user' : 'rr-fraud'"
        >
          <div class="rr-head">
            <span class="rr-speaker">{{ r.speaker === 'USER' ? '我' : '诈骗分子' }}</span>
            <el-tag
              v-if="r.speaker === 'USER' && r.safeScore !== null && r.safeScore !== undefined"
              :type="r.safeScore >= 70 ? 'success' : r.safeScore <= 40 ? 'danger' : 'info'"
              size="small"
            >{{ r.safeScore }}</el-tag>
          </div>
          <div class="rr-content">{{ r.content }}</div>
          <div v-if="r.hitWords" class="rr-hit">命中危险词：{{ r.hitWords }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getScenarioList,
  getScenarioDetail,
  submitScenarioAnswers,
  startPractice,
  turnPractice,
  finishPractice,
  practiceHistory,
  practiceDetail
} from '@/api/teaching'

const scenarios = ref([])
const current = ref(null)
const submitted = ref(false)
const listLoading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const answers = reactive({})
const submitResult = ref(null)

// ---- L3 对话演练状态 ----
const practice = ref(null)
const startLoading = ref(false)
const messages = ref([])
const userInput = ref('')
const sending = ref(false)
const practiceOver = ref(false)
const finishResult = ref(null)
const chatBox = ref(null)

// ---- 历史 / 回放 ----
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref([])
const replayVisible = ref(false)
const replayLoading = ref(false)
const replayData = ref(null)

function categoryTagType(c) {
  if (!c) return 'info'
  if (c.includes('刷单')) return 'danger'
  if (c.includes('公检法') || c.includes('冒充')) return 'warning'
  if (c.includes('征信')) return 'primary'
  return 'info'
}

function resultName(r) {
  return { SAFE: '成功识破', LURED: '被诱骗', TIMEOUT: '超时未定', FINISHED: '主动结束' }[r] || r || '-'
}

function historyTagType(r) {
  return { SAFE: 'success', LURED: 'danger', TIMEOUT: 'warning', FINISHED: 'info' }[r] || 'info'
}

function userTurnNo() {
  return messages.value.filter(m => m.speaker === 'USER').length
}

async function loadList() {
  listLoading.value = true
  try {
    scenarios.value = await getScenarioList() || []
    if (scenarios.value.length && !current.value) {
      await selectScenario(scenarios.value[0])
    }
  } finally {
    listLoading.value = false
  }
}

async function selectScenario(s) {
  if (s.contentType === 'SCENARIO_DIALOG') {
    await enterDialog(s)
    return
  }
  submitted.value = false
  submitResult.value = null
  Object.keys(answers).forEach(k => delete answers[k])
  detailLoading.value = true
  try {
    current.value = await getScenarioDetail(s.id)
  } catch (e) {
    current.value = s
  } finally {
    detailLoading.value = false
  }
}

// ---- L3 对话演练 ----
async function enterDialog(s) {
  current.value = s
  practice.value = null
  practiceOver.value = false
  finishResult.value = null
  messages.value = []
  userInput.value = ''
  startLoading.value = true
  try {
    const r = await startPractice(s.id)
    practice.value = r
    messages.value.push({ speaker: 'FRAUD', content: r.openingLine })
    await scrollBottom()
  } catch (e) {
    ElMessage.error('开始演练失败：' + (e?.message || '请稍后重试'))
  } finally {
    startLoading.value = false
  }
}

async function handleSend() {
  const content = userInput.value.trim()
  if (!content || sending.value || !practice.value || practiceOver.value) return
  sending.value = true
  try {
    const r = await turnPractice(practice.value.practiceNo, content)
    messages.value.push({ speaker: 'USER', content, score: r.safeScore, level: r.riskLevel, hint: r.dangerHint })
    if (r.gameOver) {
      practiceOver.value = true
      if (r.aiReply) messages.value.push({ speaker: 'FRAUD', content: r.aiReply })
      const fr = await finishPractice(practice.value.practiceNo, 'GAME_OVER')
      finishResult.value = fr
    } else {
      messages.value.push({ speaker: 'FRAUD', content: r.aiReply })
    }
    userInput.value = ''
    await scrollBottom()
  } catch (e) {
    ElMessage.error(e?.message || '发送失败，请重试')
  } finally {
    sending.value = false
  }
}

async function handleFinish() {
  if (!practice.value || practiceOver.value) return
  sending.value = true
  try {
    const fr = await finishPractice(practice.value.practiceNo, 'GIVE_UP')
    practiceOver.value = true
    finishResult.value = fr
    await scrollBottom()
  } catch (e) {
    ElMessage.error(e?.message || '结束失败，请重试')
  } finally {
    sending.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

// ---- 历史 / 回放 ----
async function openHistory() {
  historyVisible.value = true
  historyLoading.value = true
  try {
    const r = await practiceHistory(1, 10)
    historyList.value = r?.list || []
  } catch (e) {
    ElMessage.error(e?.message || '加载记录失败')
  } finally {
    historyLoading.value = false
  }
}

async function openReplay(practiceNo) {
  replayVisible.value = true
  replayLoading.value = true
  replayData.value = null
  try {
    replayData.value = await practiceDetail(practiceNo)
  } catch (e) {
    ElMessage.error(e?.message || '回放加载失败')
  } finally {
    replayLoading.value = false
  }
}

// ---- 原有作答逻辑 ----
async function handleSubmit() {
  if (!current.value) return
  submitLoading.value = true
  try {
    const ans = { ...answers }
    submitResult.value = await submitScenarioAnswers(current.value.id, ans)
    submitted.value = true
    ElMessage.success('答卷已提交')
  } finally {
    submitLoading.value = false
  }
}

function resetAnswer() {
  submitted.value = false
  submitResult.value = null
  Object.keys(answers).forEach(k => delete answers[k])
}

// 初始化
loadList()
</script>

<style scoped>
.teaching-page {
  max-width: 1200px;
  margin: 0 auto;
}
.list-card,
.detail-card {
  border-radius: 12px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.scenario-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.scenario-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.1);
}
.scenario-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.scenario-item-top {
  display: flex;
  gap: 6px;
}
.scenario-title {
  margin: 6px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.scenario-summary {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-detail {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bg-block {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.bg-block h4 {
  margin: 0 0 6px;
  color: #606266;
  font-size: 14px;
}
.bg-block p {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.7;
}
.question-block {
  margin-bottom: 18px;
}
.question-block h4 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 14px;
}
.option-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.option-item {
  height: auto;
  white-space: normal;
  margin-right: 0;
}
.submit-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.submit-hint {
  font-size: 12px;
  color: #909399;
}
.result-question {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.result-question.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}
.result-question.right {
  border-color: #67c23a;
  background: #f0f9eb;
}
.rq-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.rq-stem {
  font-size: 14px;
  color: #303133;
}
.rq-ans {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}
.rq-correction {
  color: #f56c6c;
  font-weight: 600;
}
.rq-explanation {
  color: #909399;
}

/* ---- L3 对话演练样式 ---- */
.dialog-area {
  min-height: 420px;
}
.dialog-start {
  text-align: center;
  padding: 24px 0;
}
.dialog-start .bg-block {
  text-align: left;
}
.chat-box {
  height: 380px;
  overflow-y: auto;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 14px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.msg-row {
  display: flex;
  gap: 8px;
}
.msg-row.row-user {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}
.av-fraud {
  background: #dcdfe6;
  color: #606266;
}
.av-user {
  background: #409eff;
  color: #fff;
}
.msg-body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.msg-bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-line;
}
.bubble-fraud {
  background: #fff;
  border: 1px solid #e4e7ed;
  color: #303133;
}
.bubble-user {
  background: #ecf5ff;
  border: 1px solid #a0cfff;
  color: #303133;
}
.msg-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.msg-hint {
  margin: 0;
  padding: 4px 8px;
}
.dialog-input {
  margin-top: 12px;
}
.dialog-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.user-turn-hint {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}
.review-card {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}
.review-points h4 {
  margin: 8px 0;
  font-size: 14px;
  color: #303133;
}
.review-tag {
  margin: 0 8px 8px 0;
  white-space: normal;
  height: auto;
  line-height: 1.6;
}
.history-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.history-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.1);
}
.hi-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.hi-time {
  font-size: 12px;
  color: #909399;
}
.hi-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.hi-meta {
  font-size: 12px;
  color: #909399;
}
.replay-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.replay-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
}
.replay-round {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.replay-round.rr-user {
  border-color: #a0cfff;
  background: #ecf5ff;
}
.rr-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.rr-speaker {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.rr-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}
.rr-hit {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}

/* 演练记录按钮：蓝绿渐变 + 固定白字 */
.history-btn {
  background: linear-gradient(135deg, #0ea5e9, #10b981) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  border-radius: 8px;
  padding: 8px 16px;
}
.history-btn:hover,
.history-btn:focus {
  background: linear-gradient(135deg, #0ea5e9, #10b981) !important;
  border: none !important;
  color: #fff !important;
}
</style>
