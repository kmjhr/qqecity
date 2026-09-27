<template>
  <div class="bookkeeping-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
          <el-icon :size="32" color="#fff"><Notebook /></el-icon>
        </div>
        <div class="module-info">
          <h2>创业经营赋能</h2>
          <p>AI 简易记账 · 现金流报表 · 风险预警</p>
          <el-tag type="warning" size="small">演示系统 · 模拟数据</el-tag>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="addDialogVisible = true">
            <el-icon><Plus /></el-icon>记一笔
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <div class="card-header">
              <span>现金流报表（{{ reportPeriod }}）</span>
              <el-button size="small" @click="loadReport">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </template>
          <div v-if="report" class="report-body">
            <div class="report-row">
              <div class="report-item income"><div class="r-label">总收入</div><div class="r-value">¥{{ report.totalIncome }}</div></div>
              <div class="report-item expense"><div class="r-label">总支出</div><div class="r-value">¥{{ report.totalExpense }}</div></div>
            </div>
            <div class="report-net" :class="report.netCashFlow >= 0 ? 'pos' : 'neg'">
              净现金流：¥{{ report.netCashFlow }}
            </div>
            <el-row :gutter="12" class="report-meta">
              <el-col :span="12"><div class="meta"><span>利润率</span><b>{{ report.profitMargin }}%</b></div></el-col>
              <el-col :span="12"><div class="meta"><span>预警等级</span>
                <el-tag size="small" :type="report.warningLevel === 'NORMAL' ? 'success' : report.warningLevel === 'WARNING' ? 'warning' : 'danger'">{{ report.warningLevel === 'NORMAL' ? '正常' : report.warningLevel === 'WARNING' ? '预警' : '风险' }}</el-tag>
              </div></el-col>
            </el-row>
            <el-alert :type="report.warningLevel === 'NORMAL' ? 'success' : report.warningLevel === 'WARNING' ? 'warning' : 'error'"
              :closable="false" style="margin-top:12px" :title="report.warningContent" />
          </div>
          <el-empty v-else description="暂无报表数据，点击「记一笔」开始记账" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header><span>记账筛选</span></template>
          <el-form inline label-width="70px">
            <el-form-item label="类型">
              <el-select v-model="filterType" placeholder="全部" clearable style="width:120px" @change="loadRecords">
                <el-option label="收入" value="INCOME" />
                <el-option label="支出" value="EXPENSE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="loadRecords">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </el-form-item>
          </el-form>
          <el-table :data="records" v-loading="listLoading" size="small" stripe empty-text="暂无记账记录">
            <el-table-column prop="happenDate" label="日期" width="110" />
            <el-table-column label="类型" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.recordType === 'INCOME' ? 'success' : 'danger'">{{ row.recordType === 'INCOME' ? '收入' : '支出' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="90" />
            <el-table-column prop="amount" label="金额" width="100" align="right">
              <template #default="{ row }"><b :class="row.recordType === 'INCOME' ? 'pos' : 'neg'">¥{{ row.amount }}</b></template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="120" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增记账弹窗 -->
    <el-dialog v-model="addDialogVisible" title="记一笔" width="520px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="recordType">
              <el-radio-group v-model="addForm.recordType">
                <el-radio-button value="INCOME">收入</el-radio-button>
                <el-radio-button value="EXPENSE">支出</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期" prop="happenDate">
              <el-date-picker v-model="addForm.happenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="addForm.category" placeholder="选择或输入" allow-create filterable style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额" prop="amount">
              <el-input-number v-model="addForm.amount" :min="0.01" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="对方/来源">
          <el-input v-model="addForm.relatedParty" placeholder="交易对方或来源" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="addForm.description" type="textarea" :rows="2" placeholder="备注说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="submitAdd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Notebook, Plus, Refresh } from '@element-plus/icons-vue'
import { getBookkeepingList, addBookkeepingRecord, getCashFlowReport } from '@/api/bookkeeping'

const listLoading = ref(false)
const records = ref([])
const report = ref(null)
const reportPeriod = ref(new Date().toISOString().slice(0, 7))
const filterType = ref('')

const addDialogVisible = ref(false)
const adding = ref(false)
const addFormRef = ref()
const today = new Date().toISOString().slice(0, 10)
const addForm = reactive({ recordType: 'EXPENSE', category: '', amount: undefined, happenDate: today, relatedParty: '', description: '' })
const addRules = {
  recordType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  happenDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const categoryOptions = computed(() => {
  const set = new Set(records.value.map(r => r.category).filter(Boolean))
  return ['餐饮', '交通', '购物', '房租', '兼职', '工资', '物料采购', '商品销售', '服务收入', ...set]
})

const loadRecords = async () => {
  listLoading.value = true
  try {
    records.value = await getBookkeepingList({ type: filterType.value }) || []
  } catch (e) {} finally { listLoading.value = false }
}

const loadReport = async () => {
  try {
    report.value = await getCashFlowReport()
    if (report.value) reportPeriod.value = report.value.reportPeriod
  } catch (e) {}
}

const submitAdd = async () => {
  await addFormRef.value.validate()
  adding.value = true
  try {
    await addBookkeepingRecord({ ...addForm })
    ElMessage.success('记账成功')
    addDialogVisible.value = false
    addFormRef.value.resetFields()
    Object.assign(addForm, { recordType: 'EXPENSE', category: '', amount: undefined, happenDate: today, relatedParty: '', description: '' })
    loadRecords()
    loadReport()
  } catch (e) {} finally { adding.value = false }
}

onMounted(() => { loadRecords(); loadReport() })
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.header-actions { margin-left: auto; }
.report-card { min-height: 360px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.report-row { display: flex; gap: 12px; margin-bottom: 12px; }
.report-item { flex: 1; padding: 14px; border-radius: 8px; text-align: center; }
.report-item.income { background: #f0f9eb; }
.report-item.expense { background: #fef0f0; }
.r-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.r-value { font-size: 22px; font-weight: 700; }
.report-item.income .r-value { color: #67c23a; }
.report-item.expense .r-value { color: #f56c6c; }
.report-net { text-align: center; font-size: 20px; font-weight: 700; padding: 12px; border-radius: 8px; margin-bottom: 12px; }
.report-net.pos { background: #ecf5ff; color: #409eff; }
.report-net.neg { background: #fef0f0; color: #f56c6c; }
.report-meta .meta { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #f5f7fa; border-radius: 6px; }
.pos { color: #67c23a; }
.neg { color: #f56c6c; }
</style>
