<template>
  <div class="loan-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>③ 贷款审批 - A 类提款 / B 类受托支付</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="贷款申请审批" name="applications">
          <div class="filter-bar">
            <el-select v-model="appFilter.loanType" placeholder="贷款类型" clearable style="width: 160px" @change="loadApplications">
              <el-option label="A 类循环贷" value="A_TYPE" />
              <el-option label="B 类小额定向" value="B_TYPE" />
            </el-select>
          </div>
          <el-table :data="appList" v-loading="appLoading" border stripe style="margin-top: 12px">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="applyNo" label="申请编号" min-width="160" />
            <el-table-column prop="userId" label="申请人 ID" width="100" align="center" />
            <el-table-column prop="loanType" label="类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.loanType === 'A_TYPE' ? 'primary' : 'success'" size="small">
                  {{ row.loanType === 'A_TYPE' ? 'A 类' : 'B 类' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applyAmount" label="申请金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.applyAmount }}</template>
            </el-table-column>
            <el-table-column label="AI 审核结论" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.applyStatus)" size="small">
                  {{ statusName(row.applyStatus) }}
                </el-tag>
                <span style="margin-left:4px; font-size:11px; color:#909399">AI</span>
              </template>
            </el-table-column>
            <el-table-column label="AI 审核意见" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <span :style="{ color: row.applyStatus === 'REJECTED' ? '#f56c6c' : (row.applyStatus === 'APPROVED' ? '#67c23a' : '#e6a23c') }">
                  {{ aiOpinion(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openAppDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="appPagination.pageNum"
              v-model:page-size="appPagination.pageSize"
              :total="appPagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadApplications"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="A 类提款流水" name="creditTxns">
          <el-table :data="txnList" v-loading="txnLoading" border stripe>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="txnNo" label="交易编号" min-width="160" />
            <el-table-column prop="userId" label="用户 ID" width="100" align="center" />
            <el-table-column prop="txnType" label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.txnType === 'WITHDRAW' ? 'warning' : 'success'" size="small">
                  {{ row.txnType === 'WITHDRAW' ? '提款' : '还款' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="principalAmount" label="本金" width="120" align="right">
              <template #default="{ row }">¥{{ row.principalAmount }}</template>
            </el-table-column>
            <el-table-column prop="interestAmount" label="利息" width="120" align="right">
              <template #default="{ row }">¥{{ row.interestAmount }}</template>
            </el-table-column>
            <el-table-column prop="borrowDays" label="计息天数" width="100" align="center" />
            <el-table-column prop="balanceAfter" label="可用余额" width="120" align="right">
              <template #default="{ row }">¥{{ row.balanceAfter }}</template>
            </el-table-column>
            <el-table-column prop="txnTime" label="交易时间" width="180" />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="txnPagination.pageNum"
              v-model:page-size="txnPagination.pageSize"
              :total="txnPagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadTxns"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="B转A观察期" name="observation">
          <div class="filter-bar">
            <el-input
              v-model="obsKeyword"
              placeholder="用户名 / 姓名"
              clearable
              style="width: 220px"
              @change="loadObservation"
              @clear="loadObservation"
            />
            <el-button type="primary" plain @click="loadObservation">查询</el-button>
          </div>
          <el-table :data="obsList" v-loading="obsLoading" border stripe style="margin-top: 12px">
            <el-table-column prop="userId" label="用户 ID" width="90" align="center" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">B 类 · 定向贷</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="观察状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="obsStatusType(row.observationStatus)" size="small">
                  {{ obsStatusName(row.observationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="observationMonths" label="观察月数" width="90" align="center" />
            <el-table-column prop="observationScore" label="观察评分" width="90" align="center" />
            <el-table-column label="转A进度" width="150" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.promotionProgress || 0" :stroke-width="10" />
              </template>
            </el-table-column>
            <el-table-column label="授信额度" min-width="180" align="right">
              <template #default="{ row }">总额 ¥{{ row.totalLimit }} / 已用 ¥{{ row.usedLimit }} / 可用 ¥{{ row.availableLimit }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="进入时间" width="180" />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="obsPagination.pageNum"
              v-model:page-size="obsPagination.pageSize"
              :total="obsPagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadObservation"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="B 类受托支付流水" name="entrustPayments">
          <el-table :data="epList" v-loading="epLoading" border stripe>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="paymentNo" label="支付编号" min-width="160" />
            <el-table-column prop="userId" label="借款人 ID" width="100" align="center" />
            <el-table-column prop="loanApplicationId" label="申请 ID" width="100" align="center" />
            <el-table-column prop="merchantName" label="收款商户" min-width="160" />
            <el-table-column prop="amount" label="支付金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
            <el-table-column prop="paymentStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="paymentStatusType(row.paymentStatus)" size="small">
                  {{ paymentStatusName(row.paymentStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="paymentTime" label="支付时间" width="180" />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="epPagination.pageNum"
              v-model:page-size="epPagination.pageSize"
              :total="epPagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadEntrustPayments"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI审核记录" name="aiReviews">
          <div class="filter-bar">
            <el-input v-model="aiUserId" placeholder="用户 ID" clearable style="width: 120px" @change="loadAiReviews" @clear="loadAiReviews" />
            <el-select v-model="aiResult" placeholder="审查结果" clearable style="width: 130px" @change="loadAiReviews">
              <el-option label="通过" value="PASS" />
              <el-option label="拒绝" value="REJECT" />
            </el-select>
            <el-select v-model="aiCreditType" placeholder="授信类型" clearable style="width: 130px" @change="loadAiReviews">
              <el-option label="A 类循环贷" value="A_TYPE" />
              <el-option label="B 类定向贷" value="B_TYPE" />
            </el-select>
            <el-button type="primary" plain @click="loadAiReviews">查询</el-button>
          </div>
          <el-table :data="aiList" v-loading="aiLoading" border stripe style="margin-top: 12px">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="userName" label="申请人" min-width="100" />
            <el-table-column prop="userId" label="用户 ID" width="90" align="center" />
            <el-table-column label="授信类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.creditType === 'A_TYPE' ? 'primary' : 'success'" size="small">
                  {{ row.creditType === 'A_TYPE' ? 'A类循环贷' : 'B类定向贷' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="业务场景" width="100" align="center">
              <template #default="{ row }">{{ row.bizType === 'WITHDRAW' ? '提款' : '打款' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">
                  {{ row.result === 'PASS' ? '通过' : '拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="aiScore" label="评分" width="70" align="center" />
            <el-table-column label="风险项" min-width="240">
              <template #default="{ row }">
                <div v-if="aiRejected(row).length" style="color:#f56c6c; font-size:12px; line-height:1.5">
                  <div v-for="(it, i) in aiRejected(row)" :key="i">· {{ it }}</div>
                </div>
                <span v-else style="color:#67c23a">无风险项</span>
              </template>
            </el-table-column>
            <el-table-column label="通过项" min-width="200">
              <template #default="{ row }">
                <div style="color:#606266; font-size:12px; line-height:1.5">
                  <div v-for="(it, i) in aiPassed(row)" :key="i">· {{ it }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="审查时间" width="180" />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="aiPagination.pageNum"
              v-model:page-size="aiPagination.pageSize"
              :total="aiPagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadAiReviews"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 贷款申请 AI 审核留痕详情 -->
    <el-dialog v-model="appDetailVisible" title="贷款申请 AI 审核留痕" width="540px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请编号">{{ currentApp?.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人 ID">{{ currentApp?.userId }}</el-descriptions-item>
        <el-descriptions-item label="贷款类型">
          <el-tag :type="currentApp?.loanType === 'A_TYPE' ? 'primary' : 'success'" size="small">
            {{ currentApp?.loanType === 'A_TYPE' ? 'A 类循环贷' : 'B 类小额定向' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请金额 / 用途">¥{{ currentApp?.applyAmount }} / {{ currentApp?.purpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="人群资质">{{ currentApp?.crowdType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="AI 审核结论">
          <el-tag :type="statusTagType(currentApp?.applyStatus)" size="small">{{ statusName(currentApp?.applyStatus) }}</el-tag>
          <span style="margin-left:6px; font-size:12px; color:#909399">AI 智能审核（模拟）</span>
        </el-descriptions-item>
        <el-descriptions-item label="AI 审核意见">
          <div style="white-space:pre-wrap; line-height:1.6">{{ aiOpinion(currentApp) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="预审详情">
          <div style="white-space:pre-wrap; line-height:1.6">{{ currentApp?.preCheckDetail || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentApp?.submitTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ currentApp?.approveTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="reviewVisible" :title="reviewTitle" width="500px">
      <el-form>
        <el-form-item label="申请编号">
          <span>{{ currentApp?.applyNo }}</span>
        </el-form-item>
        <el-form-item v-if="currentDecision === 'APPROVED'" label="审批金额">
          <el-input-number
            v-model="approveAmount"
            :min="0"
            :precision="2"
            :step="1000"
            style="width: 220px"
          />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">
            默认等于申请金额 ¥{{ currentApp?.applyAmount }}
          </span>
        </el-form-item>
        <el-form-item v-if="currentDecision === 'REJECTED'" label="拒绝原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
        <el-form-item v-if="currentDecision === 'RETURNED'" label="退回原因" required>
          <el-input v-model="returnReason" type="textarea" :rows="3" placeholder="请输入退回原因" />
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="remark" type="textarea" :rows="2" placeholder="审批备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmReview">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getLoanApplications,
  reviewLoanApplication,
  getCreditTxns,
  getEntrustPayments,
  getObservationUsers,
  getAiReviewLogs
} from '@/api/business/loan'

const activeTab = ref('applications')

// ---- 贷款申请审批 ----
const appLoading = ref(false)
const appList = ref<any[]>([])
const appFilter = reactive({ status: '', loanType: '' })
const appPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadApplications() {
  appLoading.value = true
  try {
    const res = await getLoanApplications({
      pageNum: appPagination.pageNum,
      pageSize: appPagination.pageSize,
      status: appFilter.status,
      loanType: appFilter.loanType
    })
    appList.value = res.records
    appPagination.total = res.total
  } finally {
    appLoading.value = false
  }
}

// ---- A 类提款流水 ----
const txnLoading = ref(false)
const txnList = ref<any[]>([])
const txnPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadTxns() {
  txnLoading.value = true
  try {
    const res = await getCreditTxns({
      pageNum: txnPagination.pageNum,
      pageSize: txnPagination.pageSize
    })
    txnList.value = res.records
    txnPagination.total = res.total
  } finally {
    txnLoading.value = false
  }
}

// ---- B 类受托支付流水 ----
const epLoading = ref(false)
const epList = ref<any[]>([])
const epPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadEntrustPayments() {
  epLoading.value = true
  try {
    const res = await getEntrustPayments({
      pageNum: epPagination.pageNum,
      pageSize: epPagination.pageSize
    })
    epList.value = res.records
    epPagination.total = res.total
  } finally {
    epLoading.value = false
  }
}

// ---- B转A观察期 ----
const obsLoading = ref(false)
const obsList = ref<any[]>([])
const obsKeyword = ref('')
const obsPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadObservation() {
  obsLoading.value = true
  try {
    const res = await getObservationUsers({
      pageNum: obsPagination.pageNum,
      pageSize: obsPagination.pageSize,
      keyword: obsKeyword.value || undefined
    })
    obsList.value = res.records
    obsPagination.total = res.total
  } finally {
    obsLoading.value = false
  }
}

function obsStatusName(s: string) {
  const m: Record<string, string> = {
    OBSERVING: '观察中',
    PROMOTED: '已转A',
    EXITED: '已退出'
  }
  return m[s] || s || '-'
}
function obsStatusType(s: string): any {
  const m: Record<string, string> = {
    OBSERVING: 'warning',
    PROMOTED: 'success',
    EXITED: 'info'
  }
  return m[s] || 'info'
}

// ---- AI 审核记录 ----
const aiLoading = ref(false)
const aiList = ref<any[]>([])
const aiUserId = ref('')
const aiResult = ref('')
const aiCreditType = ref('')
const aiPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadAiReviews() {
  aiLoading.value = true
  try {
    const res = await getAiReviewLogs({
      pageNum: aiPagination.pageNum,
      pageSize: aiPagination.pageSize,
      userId: aiUserId.value ? Number(aiUserId.value) : undefined,
      result: aiResult.value || undefined,
      creditType: aiCreditType.value || undefined
    })
    aiList.value = res.records
    aiPagination.total = res.total
  } finally {
    aiLoading.value = false
  }
}

function parseItems(str?: string): string[] {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr.map(String) : []
  } catch {
    return []
  }
}
function aiRejected(row: any): string[] { return parseItems(row.rejectedItems) }
function aiPassed(row: any): string[] { return parseItems(row.passedItems) }

function handleTabChange(name: string | number) {
  if (name === 'applications') loadApplications()
  else if (name === 'creditTxns') loadTxns()
  else if (name === 'entrustPayments') loadEntrustPayments()
  else if (name === 'observation') loadObservation()
  else if (name === 'aiReviews') loadAiReviews()
}

// ---- 审批操作 ----
const appDetailVisible = ref(false)
const reviewVisible = ref(false)
const submitLoading = ref(false)
const currentApp = ref<any>(null)
const currentDecision = ref<'APPROVED' | 'REJECTED' | 'RETURNED'>('APPROVED')
const approveAmount = ref(0)
const rejectReason = ref('')
const returnReason = ref('')
const remark = ref('')

const reviewTitle = computed(() => {
  if (currentDecision.value === 'APPROVED') return '审批通过 - 贷款申请'
  if (currentDecision.value === 'REJECTED') return '审批拒绝 - 贷款申请'
  return '退回补充资料 - 贷款申请'
})

function reviewApp(row: any, decision: 'APPROVED' | 'REJECTED' | 'RETURNED') {
  currentApp.value = row
  currentDecision.value = decision
  approveAmount.value = Number(row.applyAmount) || 0
  rejectReason.value = ''
  returnReason.value = ''
  remark.value = ''
  reviewVisible.value = true
}

async function confirmReview() {
  if (!currentApp.value) return
  if (currentDecision.value === 'REJECTED' && !rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  if (currentDecision.value === 'RETURNED' && !returnReason.value.trim()) {
    ElMessage.warning('请填写退回原因')
    return
  }
  submitLoading.value = true
  try {
    await reviewLoanApplication(currentApp.value.id, {
      decision: currentDecision.value,
      approveAmount: currentDecision.value === 'APPROVED' ? approveAmount.value : undefined,
      rejectReason: rejectReason.value || undefined,
      returnReason: returnReason.value || undefined,
      remark: remark.value || undefined
    })
    ElMessage.success('审批已提交，状态已同步至用户端')
    reviewVisible.value = false
    loadApplications()
  } finally {
    submitLoading.value = false
  }
}

function statusName(s: string) {
  const m: Record<string, string> = {
    PRE_CHECK: '退回补充',
    PENDING_APPROVAL: '待审批',
    APPROVED: 'AI通过 · 已放款',
    REJECTED: 'AI拒绝',
    CANCELLED: '已取消'
  }
  return m[s] || s
}
function statusTagType2(s: string): any {
  const m: Record<string, string> = {
    PRE_CHECK: 'warning',
    PENDING_APPROVAL: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return m[s] || 'info'
}
function aiOpinion(row: any): string {
  if (row.applyStatus === 'REJECTED') return row.rejectReason || 'AI 审核拒绝（模拟）'
  if (row.applyStatus === 'APPROVED') return 'AI 审核通过（模拟）：预审达标且无风险项，自动放款'
  if (row.applyStatus === 'PRE_CHECK') return '预审提示补充：' + (row.preCheckDetail || '请补充创业计划等资料后重新提交')
  return 'AI 审核（模拟）'
}
function openAppDetail(row: any) {
  currentApp.value = row
  appDetailVisible.value = true
}
function statusTagType(s: string): any {
  const m: Record<string, string> = {
    PRE_CHECK: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return m[s] || 'info'
}
function paymentStatusName(s: string) {
  const m: Record<string, string> = {
    PENDING: '待支付',
    PROCESSING: '处理中',
    SUCCESS: '已支付',
    FAILED: '失败'
  }
  return m[s] || s
}
function paymentStatusType(s: string): any {
  const m: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: 'primary',
    SUCCESS: 'success',
    FAILED: 'danger'
  }
  return m[s] || 'info'
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.loan-review-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
