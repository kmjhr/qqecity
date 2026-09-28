<template>
  <div class="entrust-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>④-3 受托支付复核（自定义商户 · 每单复核）</span>
          <el-radio-group v-model="status" size="small" @change="loadData">
            <el-radio-button label="PENDING">待复核</el-radio-button>
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="APPROVED">已放款</el-radio-button>
            <el-radio-button label="REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="用户自定义商户每次受托支付均需复核：通过后执行放款（扣额度+EP流水+商户收款入账）；驳回不打款、额度不动【模拟】" />

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="reviewNo" label="复核单号" width="190" />
        <el-table-column prop="merchantName" label="收款商户（自定义）" min-width="160" />
        <el-table-column prop="userId" label="借款人ID" width="90" align="center" />
        <el-table-column prop="amount" label="打款金额" width="110" align="right">
          <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="140" show-overflow-tooltip />
        <el-table-column prop="tradeProof" label="交易凭证说明" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="复核意见" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link size="small" @click="handleAudit(row, true)">通过并放款</el-button>
              <el-button type="danger" link size="small" @click="handleAudit(row, false)">驳回</el-button>
            </template>
            <span v-else style="color:#909399;font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEntrustReviews, auditEntrustReview } from '@/api/business/loan'

const loading = ref(false)
const tableData = ref<any[]>([])
const status = ref('PENDING')

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getEntrustReviews(status.value || undefined)
  } finally {
    loading.value = false
  }
}

function handleAudit(row: any, approve: boolean) {
  ElMessageBox.prompt(
    approve ? '复核通过后将立即向该商户放款（模拟，资金不经过借款人个人账户）。可填写复核意见：' : '驳回后该笔受托支付不执行，额度不受影响。请填写驳回原因（用户端可见）：',
    approve ? '受托支付复核 - 通过并放款' : '受托支付复核 - 驳回',
    { inputType: 'textarea', inputPlaceholder: '复核意见 / 驳回原因（可选）', type: approve ? 'success' : 'warning' }
  ).then(async ({ value }) => {
    await auditEntrustReview(row.id, approve, value || undefined)
    ElMessage.success(approve ? '已通过并放款，用户端打款记录将显示「已复核放款」' : '已驳回，该笔受托支付未执行')
    loadData()
  }).catch(() => {})
}

function statusName(s: string) {
  const m: Record<string, string> = {
    PENDING: '待复核',
    APPROVED: '已放款',
    REJECTED: '已驳回'
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
