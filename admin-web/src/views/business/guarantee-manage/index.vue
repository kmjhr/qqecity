<template>
  <div class="guarantee-manage-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>⑥ 保函管理 - 全量申请（含代房东确认）</span>
          <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="SUBMITTED">待房东确认</el-radio-button>
            <el-radio-button value="LANDLORD_CONFIRM">待确认</el-radio-button>
            <el-radio-button value="AI_REVIEW">AI复审中</el-radio-button>
            <el-radio-button value="MANUAL_REVIEW">人工复审中</el-radio-button>
            <el-radio-button value="PENDING_PAY">待缴费</el-radio-button>
            <el-radio-button value="ISSUED">已开立</el-radio-button>
            <el-radio-button value="EXPIRED">已失效</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="applyNo" label="申请编号" min-width="170" />
        <el-table-column prop="applicantName" label="租客姓名" width="110" />
        <el-table-column prop="landlordName" label="房东姓名" width="110" />
        <el-table-column prop="depositAmount" label="押金" width="100" align="right">
          <template #default="{ row }">¥{{ row.depositAmount }}</template>
        </el-table-column>
        <el-table-column prop="guaranteeFee" label="保函费" width="100" align="right">
          <template #default="{ row }">¥{{ row.guaranteeFee }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.applyStatus]?.type ?? 'info'" size="small">
              {{ statusMap[row.applyStatus]?.label ?? row.applyStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.applyStatus === 'SUBMITTED'"
              type="primary"
              link
              size="small"
              @click="handleConfirm(row)"
            >
              代房东确认
            </el-button>
            <el-button type="primary" link size="small" @click="openDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 代房东确认弹窗 -->
    <el-dialog v-model="confirmVisible" title="代房东确认并电子签署" width="520px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="银行/运营代房东操作（演示）"
        description="确认后状态由「待确认」流转，自动触发 AI 合同复审；签名方式记录为 ADMIN_AGENT_CONFIRM，并站内信通知租客。"
        style="margin-bottom: 16px"
      />
      <el-form label-width="100px">
        <el-form-item label="申请编号">
          <span>{{ currentRow?.applyNo }}</span>
        </el-form-item>
        <el-form-item label="房东">
          <span>{{ currentRow?.landlordName }}</span>
        </el-form-item>
        <el-form-item label="代签备注">
          <el-input v-model="signContent" placeholder="可选，留空默认 ADMIN_AGENT_CONFIRM" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmLandlordConfirm">
          确认并签署
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="保函申请详情" size="480px">
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="申请编号">{{ currentRow.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="租客">{{ currentRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="房东">{{ currentRow.landlordName }}</el-descriptions-item>
        <el-descriptions-item label="押金/保函额">¥{{ currentRow.depositAmount }}</el-descriptions-item>
        <el-descriptions-item label="保函费">¥{{ currentRow.guaranteeFee }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[currentRow.applyStatus]?.type ?? 'info'" size="small">
            {{ statusMap[currentRow.applyStatus]?.label ?? currentRow.applyStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="AI 复审结果">
          {{ currentRow.aiReviewResult ? `${currentRow.aiReviewResult}（${currentRow.aiReviewScore ?? '-'}分）` : '未复审' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.aiReviewDetail" label="复审明细">
          {{ currentRow.aiReviewDetail }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.signTime" label="签署时间">
          {{ currentRow.signTime }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentRow.submitTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGuaranteeApplications,
  adminLandlordConfirm
} from '@/api/business/guarantee'

// 状态映射（与后端 statusName 一致）
const statusMap: Record<string, { label: string; type: string }> = {
  SUBMITTED: { label: '申请中·待房东确认', type: 'warning' },
  LANDLORD_CONFIRM: { label: '待确认', type: 'warning' },
  AI_REVIEW: { label: 'AI复审中', type: 'primary' },
  MANUAL_REVIEW: { label: '人工复审中', type: 'danger' },
  PENDING_PAY: { label: '待缴费', type: 'warning' },
  ISSUED: { label: '已开立', type: 'success' },
  EXPIRED: { label: '已失效', type: 'info' },
  CLOSED: { label: '已关闭', type: 'info' }
}

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const statusFilter = ref('')
const confirmVisible = ref(false)
const detailVisible = ref(false)
const currentRow = ref<any>(null)
const signContent = ref('')

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

function handleFilterChange() {
  pagination.pageNum = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getGuaranteeApplications({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: statusFilter.value || undefined
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function handleConfirm(row: any) {
  currentRow.value = row
  signContent.value = ''
  confirmVisible.value = true
}

async function confirmLandlordConfirm() {
  if (!currentRow.value) return
  submitLoading.value = true
  try {
    const res = await adminLandlordConfirm(currentRow.value.id, signContent.value || undefined)
    const next = res?.applyStatus
    ElMessage.success(
      next === 'MANUAL_REVIEW'
        ? '已代房东确认，AI 复审存疑，已转人工复审队列'
        : '已代房东确认，AI 复审通过，状态已同步至用户端（待缴费）'
    )
    confirmVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function openDetail(row: any) {
  currentRow.value = row
  detailVisible.value = true
}
</script>

<style scoped>
.guarantee-manage-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
