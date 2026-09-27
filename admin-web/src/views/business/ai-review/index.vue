<template>
  <div class="ai-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>① AI 复审队列 - 保函存疑转人工</span>
          <el-tag size="small" type="warning">MANUAL_REVIEW</el-tag>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="applyNo" label="申请编号" min-width="160" />
        <el-table-column prop="applicantName" label="租客姓名" width="120" />
        <el-table-column prop="landlordName" label="房东姓名" width="120" />
        <el-table-column prop="depositAmount" label="押金金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.depositAmount }}</template>
        </el-table-column>
        <el-table-column prop="aiReviewScore" label="AI 评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.aiReviewScore ?? '-' }}分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="aiReviewResult" label="AI 结果" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.aiReviewResult }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button type="danger" link size="small" @click="handleReject(row)">
              拒绝
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

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝保函申请" width="480px">
      <el-form>
        <el-form-item label="申请编号">
          <span>{{ currentRow?.applyNo }}</span>
        </el-form-item>
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmReject">
          确定拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGuaranteeManualReviewQueue,
  manualReviewGuarantee
} from '@/api/business/guarantee'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref<any>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getGuaranteeManualReviewQueue({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function handleApprove(row: any) {
  ElMessageBox.confirm(
    `确认通过申请「${row.applyNo}」放行至待缴费？`,
    '人工复审 - 通过',
    { type: 'warning', confirmButtonText: '通过', cancelButtonText: '取消' }
  ).then(async () => {
    submitLoading.value = true
    try {
      await manualReviewGuarantee(row.id, { decision: 'APPROVED' })
      ElMessage.success('已通过，状态已同步至用户端')
      loadData()
    } finally {
      submitLoading.value = false
    }
  }).catch(() => {})
}

function handleReject(row: any) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  if (!currentRow.value) return
  submitLoading.value = true
  try {
    await manualReviewGuarantee(currentRow.value.id, {
      decision: 'REJECTED',
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝，状态已同步至用户端')
    rejectVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.ai-review-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
