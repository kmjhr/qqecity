<template>
  <div class="teaching-page">
    <el-row :gutter="20">
      <!-- 左侧：情景列表 -->
      <el-col :span="8">
        <el-card shadow="never" class="list-card">
          <template #header>
            <span>反诈情景模拟</span>
          </template>
          <div v-loading="listLoading">
            <div
              v-for="s in scenarios"
              :key="s.id"
              class="scenario-item"
              :class="{ active: current?.id === s.id }"
              @click="selectScenario(s)"
            >
              <el-tag :type="categoryTagType(s.category)" size="small">
                {{ s.category || '其他' }}
              </el-tag>
              <div class="scenario-title">{{ s.title }}</div>
              <div class="scenario-summary">{{ s.summary }}</div>
            </div>
            <el-empty v-if="!listLoading && !scenarios.length" description="暂无情景内容" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：作答区 -->
      <el-col :span="16">
        <el-card shadow="never" class="detail-card" v-loading="detailLoading">
          <template #header>
            <div class="detail-header">
              <span>{{ current?.title || '请选择左侧情景开始学习' }}</span>
              <el-tag v-if="current" type="warning" size="small">模拟教学</el-tag>
            </div>
          </template>

          <div v-if="!current" class="empty-detail">
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
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getScenarioList,
  getScenarioDetail,
  submitScenarioAnswers
} from '@/api/teaching'

const scenarios = ref([])
const current = ref(null)
const submitted = ref(false)
const listLoading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const answers = reactive({})
const submitResult = ref(null)

function categoryTagType(c) {
  if (!c) return 'info'
  if (c.includes('刷单')) return 'danger'
  if (c.includes('公检法') || c.includes('冒充')) return 'warning'
  if (c.includes('征信')) return 'primary'
  return 'info'
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

async function handleSubmit() {
  if (!current.value) return
  submitLoading.value = true
  try {
    const ans = { ...answers }
    Object.keys(ans).forEach(k => {
      ans[k] = ans[k]
    })
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
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.bg-block h4 {
  margin: 0 0 6px;
  font-size: 14px;
  color: #606266;
}
.bg-block p {
  margin: 0;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}
.question-block {
  margin-bottom: 20px;
}
.question-block h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #303133;
}
.option-group {
  display: flex;
  flex-direction: column;
}
.option-item {
  margin: 6px 0;
  font-size: 13px;
}
.submit-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.submit-hint {
  font-size: 12px;
  color: #909399;
}

.result-question {
  padding: 12px;
  border-left: 4px solid #dcdfe6;
  margin-bottom: 12px;
  background: #fafafa;
  border-radius: 4px;
}
.result-question.right {
  border-left-color: #67c23a;
  background: #f0f9eb;
}
.result-question.wrong {
  border-left-color: #f56c6c;
  background: #fef0f0;
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
  font-weight: 500;
}
.rq-ans {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.rq-correction {
  color: #f56c6c;
  margin-top: 4px;
}
.rq-explanation {
  color: #606266;
  margin-top: 4px;
}
</style>
