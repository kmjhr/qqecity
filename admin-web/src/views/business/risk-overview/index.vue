<template>
  <div class="risk-overview-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>⑤ 风险预警总览 - 各用户预警聚合</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="filter.warningType" placeholder="预警类型" clearable style="width: 200px" @change="loadData">
          <el-option label="逾期风险 OVERDUE_RISK" value="OVERDUE_RISK" />
          <el-option label="高频借贷 HIGH_FREQ_BORROW" value="HIGH_FREQ_BORROW" />
          <el-option label="征信异常 CREDIT_ABNORMAL" value="CREDIT_ABNORMAL" />
          <el-option label="预算超支 BUDGET_OVER" value="BUDGET_OVER" />
          <el-option label="现金流预警 CASHFLOW_WARNING" value="CASHFLOW_WARNING" />
        </el-select>
        <el-select v-model="filter.warningLevel" placeholder="预警等级" clearable style="width: 160px" @change="loadData">
          <el-option label="低 LOW" value="LOW" />
          <el-option label="中 MEDIUM" value="MEDIUM" />
          <el-option label="高 HIGH" value="HIGH" />
          <el-option label="紧急 CRITICAL" value="CRITICAL" />
        </el-select>
        <el-select v-model="filter.isHandled" placeholder="处理状态" clearable style="width: 160px" @change="loadData">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="margin-top: 12px">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="userId" label="用户 ID" width="100" align="center" />
        <el-table-column prop="warningType" label="类型" width="180" align="center">
          <template #default="{ row }">
            <el-tag :type="warningTypeTag(row.warningType)" size="small">
              {{ warningTypeName(row.warningType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warningLevel" label="等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="warningLevelTag(row.warningLevel)" size="small">
              {{ row.warningLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warningTitle" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="warningContent" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="warningTime" label="预警时间" width="180" />
        <el-table-column prop="isHandled" label="处理状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isHandled === 1 ? 'success' : 'danger'" size="small">
              {{ row.isHandled === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleNote" label="处置备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="handleTime" label="处置时间" width="180" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isHandled !== 1"
              type="primary" link size="small"
              @click="openHandle(row)"
            >处置</el-button>
            <span v-else style="color: #909399; font-size: 12px">已完成</span>
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

    <!-- 处置弹窗 -->
    <el-dialog v-model="handleVisible" title="处置风险预警" width="500px">
      <el-form>
        <el-form-item label="预警标题">
          <span>{{ currentRow?.warningTitle }}</span>
        </el-form-item>
        <el-form-item label="预警内容">
          <span style="color: #606266">{{ currentRow?.warningContent }}</span>
        </el-form-item>
        <el-form-item label="处置备注" required>
          <el-input
            v-model="handleNote"
            type="textarea"
            :rows="4"
            placeholder="请说明处置措施，如已电话联系、已协助处理等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmHandle">
          确认处置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRiskWarnings,
  handleRiskWarning
} from '@/api/business/riskWarning'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const handleVisible = ref(false)
const currentRow = ref<any>(null)
const handleNote = ref('')

const filter = reactive({
  warningType: '',
  warningLevel: '',
  isHandled: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function loadData() {
  loading.value = true
  try {
    const res = await getRiskWarnings({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      warningType: filter.warningType || undefined,
      warningLevel: filter.warningLevel || undefined,
      isHandled: filter.isHandled
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function openHandle(row: any) {
  currentRow.value = row
  handleNote.value = ''
  handleVisible.value = true
}

async function confirmHandle() {
  if (!handleNote.value.trim()) {
    ElMessage.warning('请填写处置备注')
    return
  }
  if (!currentRow.value) return
  submitLoading.value = true
  try {
    await handleRiskWarning(currentRow.value.id, { handleNote: handleNote.value })
    ElMessage.success('已处置，状态已同步至用户端')
    handleVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function warningTypeName(s: string) {
  const m: Record<string, string> = {
    OVERDUE_RISK: '逾期风险',
    HIGH_FREQ_BORROW: '高频借贷',
    CREDIT_ABNORMAL: '征信异常',
    BUDGET_OVER: '预算超支',
    CASHFLOW_WARNING: '现金流预警'
  }
  return m[s] || s
}
function warningTypeTag(s: string): any {
  const m: Record<string, string> = {
    OVERDUE_RISK: 'danger',
    HIGH_FREQ_BORROW: 'warning',
    CREDIT_ABNORMAL: 'danger',
    BUDGET_OVER: 'warning',
    CASHFLOW_WARNING: 'warning'
  }
  return m[s] || 'info'
}
function warningLevelTag(s: string): any {
  const m: Record<string, string> = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return m[s] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.risk-overview-page {
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
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
