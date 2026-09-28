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

    <!-- 经营数据回流看板（模块3×贷款联动） -->
    <el-card shadow="never" class="linkage-card" v-loading="linkageLoading">
      <template #header>
        <div class="card-header">
          <span>经营数据回流看板 <el-tag size="small" type="success" style="margin-left:8px">B转A 观察期联动</el-tag></span>
          <el-button size="small" @click="loadLinkage">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>
      <div v-if="progress" class="linkage-body">
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="linkage-metric">
              <div class="lm-label">经营流水回流 <el-tooltip content="B类受托支付成功金额累计（阈值 ¥10,000）" placement="top"><el-icon style="vertical-align:-2px"><InfoFilled /></el-icon></el-tooltip></div>
              <div class="lm-value">¥{{ progress.flowAmount }}<span class="lm-threshold"> / ¥{{ progress.flowThreshold }}</span></div>
              <el-progress :percentage="progress.flowPercent" :stroke-width="10" :color="progress.flowPercent >= 100 ? '#67c23a' : '#409eff'" />
              <div class="lm-sub">完成 {{ progress.flowPercent }}%</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="linkage-metric">
              <div class="lm-label">AI记账笔数 <el-tooltip content="经营账本累计记账笔数（阈值 12 笔）" placement="top"><el-icon style="vertical-align:-2px"><InfoFilled /></el-icon></el-tooltip></div>
              <div class="lm-value">{{ progress.bookCount }}<span class="lm-threshold"> / {{ progress.bookThreshold }} 笔</span></div>
              <el-progress :percentage="progress.bookPercent" :stroke-width="10" :color="progress.bookPercent >= 100 ? '#67c23a' : '#409eff'" />
              <div class="lm-sub">完成 {{ progress.bookPercent }}%</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="linkage-metric">
              <div class="lm-label">现金流健康度 <el-tooltip content="最新现金流月报等级：NORMAL=100 / WARNING=55 / DANGER=20" placement="top"><el-icon style="vertical-align:-2px"><InfoFilled /></el-icon></el-tooltip></div>
              <div class="lm-value">{{ progress.cashScore }}<span class="lm-threshold"> 分（{{ progress.cashLevelName }}）</span></div>
              <el-progress :percentage="progress.cashScore" :stroke-width="10" :color="progress.cashScore >= 80 ? '#67c23a' : progress.cashScore >= 50 ? '#e6a23c' : '#f56c6c'" />
              <div class="lm-sub">按最新月报评级折算</div>
            </div>
          </el-col>
        </el-row>
        <el-alert :type="progress.eligible ? 'success' : 'info'" :closable="false" class="linkage-alert">
          <div class="linkage-msg">
            <div>
              <b>综合回流进度 {{ progress.totalPercent }}%</b>
              <span style="color:#909399;margin-left:8px">＝ 流水 40% + 记账 30% + 现金流 30%，≥60% 达标可转A</span>
            </div>
            <div style="margin-top:4px;font-size:13px">{{ progress.message }}</div>
            <el-button v-if="progress.eligible && progress.observationStatus === 'OBSERVING'" type="success" size="small" style="margin-top:8px"
              :loading="promoting" @click="doApplyPromotion">
              一键申请转A（提额至5万）
            </el-button>
          </div>
        </el-alert>
      </div>

      <!-- 联动预警 -->
      <div v-if="linkage && linkage.warningCount > 0" class="linkage-warnings">
        <div class="lw-title"><el-icon color="#e6a23c"><Warning /></el-icon> 联动风险预警（{{ linkage.warningCount }} 条未处理，在贷 ¥{{ linkage.outstandingAmount }}）</div>
        <el-alert v-for="w in linkage.warnings.slice(0, 3)" :key="w.id" :type="w.warningLevel === 'HIGH' ? 'error' : w.warningLevel === 'MEDIUM' ? 'warning' : 'info'"
          :closable="false" style="margin-top:6px" :title="w.warningTitle" :description="w.warningContent" />
        <div class="lw-impact">{{ linkage.impact }}</div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Notebook, Plus, Refresh, InfoFilled, Warning } from '@element-plus/icons-vue'
import { getBookkeepingList, addBookkeepingRecord, getCashFlowReport, getLoanLinkedWarnings, getObservationProgress, applyPromotion } from '@/api/bookkeeping'

const listLoading = ref(false)
const records = ref([])
const report = ref(null)
const reportPeriod = ref(new Date().toISOString().slice(0, 7))
const filterType = ref('')

const linkageLoading = ref(false)
const progress = ref(null)
const linkage = ref(null)
const promoting = ref(false)

const loadLinkage = async () => {
  linkageLoading.value = true
  try {
    const [p, lk] = await Promise.all([
      getObservationProgress().catch(() => null),
      getLoanLinkedWarnings().catch(() => null)
    ])
    progress.value = p
    linkage.value = lk
  } catch (e) {} finally { linkageLoading.value = false }
}

const doApplyPromotion = async () => {
  try {
    await ElMessageBox.confirm('确认一键申请转A？将通过数据回流达标路径升级为A类循环贷并提额至5万（模拟）。', '转A申请', { type: 'success', confirmButtonText: '确认申请', cancelButtonText: '取消' })
  } catch (e) { return }
  promoting.value = true
  try {
    const vo = await applyPromotion()
    ElMessage.success('转A申请通过：已升级A类循环贷并提额至5万（模拟）')
    loadLinkage()
    loadRecords()
    loadReport()
  } catch (e) { ElMessage.error(e?.message || '转A申请失败') } finally { promoting.value = false }
}

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

onMounted(() => { loadRecords(); loadReport(); loadLinkage() })
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
.linkage-card { margin-top: 16px; }
.linkage-body { padding: 4px 0; }
.linkage-metric { background: #f7f8fa; border-radius: 8px; padding: 14px 16px; height: 100%; box-sizing: border-box; }
.lm-label { font-size: 13px; color: #606266; margin-bottom: 8px; }
.lm-value { font-size: 24px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.lm-threshold { font-size: 13px; color: #909399; font-weight: 400; }
.lm-sub { font-size: 12px; color: #909399; margin-top: 6px; }
.linkage-alert { margin-top: 16px; }
.linkage-msg { font-size: 13px; }
.linkage-warnings { margin-top: 16px; border: 1px solid #fbe6e2; background: #fef7f5; border-radius: 8px; padding: 12px 14px; }
.lw-title { font-size: 14px; font-weight: 600; color: #b86b58; margin-bottom: 4px; }
.lw-impact { font-size: 12px; color: #909399; margin-top: 8px; line-height: 1.6; }
</style>
