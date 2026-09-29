<template>
  <div class="entrust-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>④-3 受托支付复核 - AI 审核留痕（模拟）</span>
          <el-radio-group v-model="status" size="small" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">AI转人工</el-radio-button>
            <el-radio-button label="APPROVED">AI通过 · 已放款</el-radio-button>
            <el-radio-button label="REJECTED">AI拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="每单提交后由模拟 AI 智能复核：用途正常 → 直接放款（扣额度+EP流水+商户收款入账）；用途异常/缺失 → 自动拒绝；单笔超 ¥10,000 → AI 预审转人工兜底复核【模拟】" />

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="reviewNo" label="复核单号" width="190" />
        <el-table-column prop="merchantName" label="收款商户（自定义）" min-width="150" />
        <el-table-column prop="userId" label="借款人ID" width="90" align="center" />
        <el-table-column prop="amount" label="打款金额" width="110" align="right">
          <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="130" show-overflow-tooltip />
        <el-table-column prop="tradeProof" label="交易凭证说明" min-width="110" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="AI 审核结论" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
            <span style="margin-left:4px; font-size:11px; color:#909399">AI</span>
          </template>
        </el-table-column>
        <el-table-column label="AI 审核意见" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :style="{ color: row.status === 'REJECTED' ? '#f56c6c' : (row.status === 'APPROVED' ? '#67c23a' : '#e6a23c') }">
              {{ row.reviewRemark || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link size="small" @click="handleAudit(row, true)">人工通过</el-button>
              <el-button type="danger" link size="small" @click="handleAudit(row, false)">人工驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- AI 审核留痕详情 -->
    <el-dialog v-model="detailVisible" title="受托支付 AI 审核留痕" width="540px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="复核单号">{{ current?.reviewNo }}</el-descriptions-item>
        <el-descriptions-item label="收款商户">{{ current?.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="打款金额 / 用途">¥{{ current?.amount }} / {{ current?.purpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易凭证说明">{{ current?.tradeProof || '-' }}</el-descriptions-item>
        <el-descriptions-item label="AI 审核结论">
          <el-tag :type="statusTagType(current?.status)" size="small">{{ statusName(current?.status) }}</el-tag>
          <span style="margin-left:6px; font-size:12px; color:#909399">
            {{ current?.status === 'PENDING' ? 'AI 预审转人工（模拟）' : 'AI 智能复核（模拟）' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="AI 审核意见">
          <div style="white-space:pre-wrap; line-height:1.6">{{ current?.reviewRemark || '暂无' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="复核时间">{{ current?.reviewTime || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="current?.paymentId" label="放款流水ID">{{ current?.paymentId }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEntrustReviews, auditEntrustReview } from '@/api/business/loan'

const loading = ref(false)
const tableData = ref<any[]>([])
const status = ref('')
const detailVisible = ref(false)
const current = ref<any>(null)

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getEntrustReviews(status.value || undefined)
  } finally {
    loading.value = false
  }
}

function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

function handleAudit(row: any, approve: boolean) {
  ElMessageBox.prompt(
    approve ? 'AI 已预审（大额转人工兜底），通过后将立即向该商户放款（模拟，资金不经过借款人个人账户）。可填写人工复核意见：' : '驳回后该笔受托支付不执行，额度不受影响。请填写驳回原因（用户端可见）：',
    approve ? '人工兜底复核 - 通过并放款' : '人工兜底复核 - 驳回',
    { inputType: 'textarea', inputPlaceholder: '复核意见 / 驳回原因（可选）', type: approve ? 'success' : 'warning' }
  ).then(async ({ value }) => {
    await auditEntrustReview(row.id, approve, value || undefined)
    ElMessage.success(approve ? '已通过并放款，用户端打款记录将显示「已复核放款」' : '已驳回，该笔受托支付未执行')
    loadData()
  }).catch(() => {})
}

function statusName(s: string) {
  const m: Record<string, string> = {
    PENDING: 'AI预审 · 转人工',
    APPROVED: 'AI通过 · 已放款',
    REJECTED: 'AI拒绝'
  }
  return m[s] || s
}
function statusTagType(s: string): any {
  const m: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return m[s] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.entrust-review-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
