<template>
  <div class="claim-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>② 索赔复核队列 - banker 人工复核</span>
          <el-tag size="small" type="warning">MANUAL_REVIEW</el-tag>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="claimNo" label="索赔编号" min-width="160" />
        <el-table-column prop="guaranteeId" label="保函 ID" width="100" align="center" />
        <el-table-column prop="claimAmount" label="索赔金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.claimAmount }}</template>
        </el-table-column>
        <el-table-column prop="claimReason" label="索赔原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="aiReviewResult" label="AI 初审" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.aiReviewResult || 'MANUAL_REVIEW' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="aiConfidence" label="置信度" width="100" align="center" />
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="handleApprove(row)">
              赔付
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

    <!-- 赔付弹窗 -->
    <el-dialog v-model="approveVisible" title="赔付索赔" width="480px">
      <el-form>
        <el-form-item label="索赔编号">
          <span>{{ currentRow?.claimNo }}</span>
        </el-form-item>
        <el-form-item label="赔付金额" required>
          <el-input-number
            v-model="payoutAmount"
            :min="0"
            :precision="2"
            :step="100"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input
            v-model="reviewNote"
            type="textarea"
            :rows="2"
            placeholder="复核说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmApprove">
          确认赔付
        </el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝索赔" width="480px">
      <el-form>
        <el-form-item label="索赔编号">
          <span>{{ currentRow?.claimNo }}</span>
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
        <el-button type="danger" :loading="submitLoading" @click="confirmReject">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getClaimManualReviewQueue,
  reviewClaim
} from '@/api/business/claim'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const approveVisible = ref(false)
const rejectVisible = ref(false)
const currentRow = ref<any>(null)
const payoutAmount = ref(0)
const reviewNote = ref('')
const rejectReason = ref('')

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
    const res = await getClaimManualReviewQueue({
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
  currentRow.value = row
  payoutAmount.value = Number(row.claimAmount) || 0
  reviewNote.value = ''
  approveVisible.value = true
}

async function confirmApprove() {
  if (!currentRow.value) return
  if (!payoutAmount.value || payoutAmount.value <= 0) {
    ElMessage.warning('请填写赔付金额')
    return
  }
  submitLoading.value = true
  try {
    await reviewClaim(currentRow.value.id, {
      decision: 'APPROVED',
      payoutAmount: payoutAmount.value,
      reviewNote: reviewNote.value
    })
    ElMessage.success('已赔付，状态已同步至用户端')
    approveVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
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
    await reviewClaim(currentRow.value.id, {
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
.claim-review-page {
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
