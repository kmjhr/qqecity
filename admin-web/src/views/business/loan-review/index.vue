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
            <el-select v-model="appFilter.status" placeholder="申请状态" clearable style="width: 160px" @change="loadApplications">
              <el-option label="待审批" value="PENDING_APPROVAL" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="退回" value="PRE_CHECK" />
            </el-select>
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
            <el-table-column prop="applyStatus" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.applyStatus)" size="small">
                  {{ statusName(row.applyStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="240" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.applyStatus === 'PENDING_APPROVAL'"
                  type="success" link size="small"
                  @click="reviewApp(row, 'APPROVED')"
                >通过</el-button>
                <el-button
                  v-if="row.applyStatus === 'PENDING_APPROVAL'"
                  type="danger" link size="small"
                  @click="reviewApp(row, 'REJECTED')"
                >拒绝</el-button>
                <el-button
                  v-if="row.applyStatus === 'PENDING_APPROVAL'"
                  type="warning" link size="small"
                  @click="reviewApp(row, 'RETURNED')"
                >退回</el-button>
                <span v-else style="color: #909399; font-size: 12px">已处理</span>
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
      </el-tabs>
    </el-card>

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
  getEntrustPayments
} from '@/api/business/loan'

const activeTab = ref('applications')

// ---- 贷款申请审批 ----
const appLoading = ref(false)
const appList = ref<any[]>([])
const appFilter = reactive({ status: 'PENDING_APPROVAL', loanType: '' })
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

function handleTabChange(name: string | number) {
  if (name === 'applications') loadApplications()
  else if (name === 'creditTxns') loadTxns()
  else if (name === 'entrustPayments') loadEntrustPayments()
}

// ---- 审批操作 ----
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
    PRE_CHECK: '预审',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消'
  }
  return m[s] || s
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
