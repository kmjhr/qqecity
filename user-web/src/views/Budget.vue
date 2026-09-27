<template>
  <div class="budget-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
          <el-icon :size="32" color="#fff"><PieChart /></el-icon>
        </div>
        <div class="module-info">
          <h2>碎片消费治理</h2>
          <p>分类预算 · 交易归类 · 分级提醒 · 结余转储蓄</p>
          <el-tag type="warning" size="small">演示系统 · 模拟数据</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 预算概览 -->
    <el-row :gutter="16" class="overview-row" v-if="overview">
      <el-col :span="6"><el-card shadow="never" class="ov-card"><div class="ov-label">预算周期</div><div class="ov-value">{{ overview.period }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="ov-card"><div class="ov-label">总预算</div><div class="ov-value">¥{{ overview.totalBudget }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="ov-card"><div class="ov-label">已用</div><div class="ov-value warn">¥{{ overview.totalUsed }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="ov-card"><div class="ov-label">剩余</div><div class="ov-value ok">¥{{ overview.totalRemain }}</div></el-card></el-col>
    </el-row>
    <el-alert v-if="overview && overview.remindLevel > 0" :type="overview.remindLevel >= 3 ? 'error' : overview.remindLevel >= 2 ? 'warning' : 'info'" :closable="false" style="margin:16px 0">
      当前整体提醒级别：<b>{{ overview.remindName }}</b>，请关注预算使用情况。
    </el-alert>

    <el-tabs v-model="activeTab">
      <!-- 分类预算 -->
      <el-tab-pane label="分类预算" name="list">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>本月分类预算</span>
              <el-button size="small" type="primary" @click="settingDialogVisible = true">
                <el-icon><Plus /></el-icon>设置预算
              </el-button>
            </div>
          </template>
          <el-table v-loading="loading" :data="budgetList" stripe empty-text="暂无预算设置，点击「设置预算」开始">
            <el-table-column prop="categoryName" label="分类" width="120" />
            <el-table-column label="预算/已用/剩余" min-width="260">
              <template #default="{ row }">
                <div class="budget-line">
                  <div class="budget-text">¥{{ row.budgetAmount }} / ¥{{ row.usedAmount }} / ¥{{ row.remainingAmount }}</div>
                  <el-progress :percentage="Number(row.usagePercent)" :color="progressColor(row.remindLevel)" :stroke-width="10" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="使用率" width="90" align="center">
              <template #default="{ row }">{{ row.usagePercent }}%</template>
            </el-table-column>
            <el-table-column label="提醒级别" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.remindLevel" :type="row.remindLevel >= 3 ? 'danger' : row.remindLevel >= 2 ? 'warning' : 'success'" size="small">{{ row.remindName }}</el-tag>
                <span v-else style="color:#909399">正常</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 交易记录 -->
      <el-tab-pane label="模拟交易" name="tx">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>新增模拟交易（按 MCC 自动归类）</span>
            </div>
          </template>
          <el-form :model="txForm" label-width="110px" inline>
            <el-form-item label="交易类型">
              <el-select v-model="txForm.transactionType" style="width:120px">
                <el-option label="支出" value="EXPENSE" />
                <el-option label="收入" value="INCOME" />
              </el-select>
            </el-form-item>
            <el-form-item label="MCC码">
              <el-select v-model="txForm.mccCode" placeholder="选择MCC" style="width:200px">
                <el-option v-for="m in mccOptions" :key="m.code" :label="`${m.code} ${m.name}`" :value="m.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="金额">
              <el-input-number v-model="txForm.amount" :min="0.01" :precision="2" style="width:140px" />
            </el-form-item>
            <el-form-item label="商户">
              <el-input v-model="txForm.merchantName" placeholder="商户名称" style="width:160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="txLoading" @click="submitTx">提交交易（模拟）</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="lastTxResult" :type="lastTxResult.remindLevel >= 3 ? 'error' : lastTxResult.remindLevel >= 2 ? 'warning' : lastTxResult.remindLevel >= 1 ? 'info' : 'success'"
            :closable="false" style="margin-top:12px">
            <div>归类到 <b>{{ lastTxResult.mappedCategory }}</b>，{{ lastTxResult.remindMessage || '未触发提醒' }}</div>
          </el-alert>
        </el-card>
      </el-tab-pane>

      <!-- 心愿储蓄 -->
      <el-tab-pane label="心愿储蓄" name="saving">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>心愿储蓄</span>
              <el-button size="small" type="success" @click="transferDialogVisible = true">
                <el-icon><Coin /></el-icon>结余一键转存
              </el-button>
            </div>
          </template>
          <div v-if="savings.length" class="saving-grid">
            <el-card v-for="s in savings" :key="s.id" shadow="hover" class="saving-card">
              <div class="saving-name">{{ s.goalName }}</div>
              <div class="saving-amount">¥{{ s.currentAmount }} <span class="saving-total">/ ¥{{ s.targetAmount }}</span></div>
              <el-progress :percentage="Number(s.progressPercent)" :stroke-width="10" color="#67c23a" />
              <el-tag size="small" :type="s.status === 'ACTIVE' ? 'success' : 'info'" style="margin-top:10px">{{ s.status === 'ACTIVE' ? '进行中' : s.status }}</el-tag>
            </el-card>
          </div>
          <el-empty v-else description="暂无心愿储蓄，可将预算结余一键转入" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 设置预算弹窗 -->
    <el-dialog v-model="settingDialogVisible" title="设置分类预算" width="480px">
      <el-form ref="settingFormRef" :model="settingForm" :rules="settingRules" label-width="100px">
        <el-form-item label="预算分类" prop="categoryId">
          <el-select v-model="settingForm.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="budgetAmount">
          <el-input-number v-model="settingForm.budgetAmount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingLoading" @click="submitSetting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 结余转储蓄弹窗 -->
    <el-dialog v-model="transferDialogVisible" title="结余一键转入心愿储蓄" width="480px">
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-form-item label="目标储蓄">
          <el-select v-model="transferForm.goalId" placeholder="选择已有目标或新建" clearable style="width:100%">
            <el-option v-for="s in savings" :key="s.id" :label="s.goalName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标名称" v-if="!transferForm.goalId">
          <el-input v-model="transferForm.goalName" placeholder="如：旅行基金" />
        </el-form-item>
        <el-form-item label="目标金额" v-if="!transferForm.goalId">
          <el-input-number v-model="transferForm.targetAmount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-alert type="info" :closable="false" title="系统将汇总本月各分类预算的剩余结余，一次性转入该心愿储蓄（模拟，不涉及真实资金划转）。" />
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="transferLoading" @click="submitTransfer">确认转存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { PieChart, Plus, Coin } from '@element-plus/icons-vue'
import { getCategories, saveBudgetSetting, getBudgetList, addTransaction, transferToSaving, getSavings, getBudgetOverview } from '@/api/budget'

const activeTab = ref('list')
const loading = ref(false)
const budgetList = ref([])
const categories = ref([])
const overview = ref(null)
const savings = ref([])

const mccOptions = [
  { code: '5812', name: '餐饮-就餐场所' },
  { code: '5814', name: '餐饮-快餐店' },
  { code: '7832', name: '影院' },
  { code: '5699', name: '服饰店' },
  { code: '5311', name: '百货商店' },
  { code: '4111', name: '交通-公共交通' },
  { code: '6538', name: '房租' },
  { code: '5411', name: '超市' }
]

const settingDialogVisible = ref(false)
const settingLoading = ref(false)
const settingFormRef = ref()
const settingForm = reactive({ categoryId: null, budgetAmount: undefined })
const settingRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }]
}

const txLoading = ref(false)
const lastTxResult = ref(null)
const txForm = reactive({ transactionType: 'EXPENSE', mccCode: '', amount: undefined, merchantName: '', description: '' })

const transferDialogVisible = ref(false)
const transferLoading = ref(false)
const transferFormRef = ref()
const transferForm = reactive({ goalId: null, goalName: '', targetAmount: undefined })
const transferRules = {}

const progressColor = (level) => level >= 3 ? '#f56c6c' : level >= 2 ? '#e6a23c' : level >= 1 ? '#409eff' : '#67c23a'

const loadAll = async () => {
  loading.value = true
  try {
    const [list, cats, ov, sv] = await Promise.all([
      getBudgetList(), getCategories(), getBudgetOverview().catch(() => null), getSavings().catch(() => [])
    ])
    budgetList.value = list || []
    categories.value = cats || []
    overview.value = ov
    savings.value = sv || []
  } catch (e) {} finally { loading.value = false }
}

const submitSetting = async () => {
  await settingFormRef.value.validate()
  settingLoading.value = true
  try {
    await saveBudgetSetting({ ...settingForm })
    ElMessage.success('预算设置成功')
    settingDialogVisible.value = false
    settingFormRef.value.resetFields()
    Object.assign(settingForm, { categoryId: null, budgetAmount: undefined })
    loadAll()
  } catch (e) {} finally { settingLoading.value = false }
}

const submitTx = async () => {
  if (!txForm.amount) { ElMessage.warning('请输入金额'); return }
  txLoading.value = true
  try {
    const data = await addTransaction({ ...txForm })
    lastTxResult.value = data
    ElMessage.success('交易已记录（模拟）')
    loadAll()
  } catch (e) {} finally { txLoading.value = false }
}

const submitTransfer = async () => {
  transferLoading.value = true
  try {
    const data = await transferToSaving({ ...transferForm })
    ElMessage.success(`已转入 ¥${data.transferAmount} 到「${data.goalName}」（模拟）`)
    transferDialogVisible.value = false
    Object.assign(transferForm, { goalId: null, goalName: '', targetAmount: undefined })
    loadAll()
  } catch (e) {} finally { transferLoading.value = false }
}

onMounted(loadAll)
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.overview-row { margin-top: 16px; }
.ov-card { text-align: center; }
.ov-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.ov-value { font-size: 22px; font-weight: 600; color: #303133; }
.ov-value.warn { color: #e6a23c; }
.ov-value.ok { color: #67c23a; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.budget-line { padding: 4px 0; }
.budget-text { font-size: 12px; color: #606266; margin-bottom: 4px; }
.saving-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.saving-name { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.saving-amount { font-size: 20px; color: #67c23a; margin-bottom: 8px; }
.saving-total { font-size: 13px; color: #909399; }
</style>
