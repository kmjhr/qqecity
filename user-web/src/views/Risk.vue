<template>
  <div class="risk-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            风险预警总览（个人聚合）
            <el-badge
              v-if="unhandledCount > 0"
              :value="`${unhandledCount} 条未处理`"
              type="danger"
              class="unhandled-badge"
            />
          </span>
          <div class="header-actions">
            <el-button type="primary" size="small" :loading="detectLoading" @click="handleDetect">
              立即检测当前风险
            </el-button>
            <el-button size="small" @click="loadAll">刷新历史</el-button>
          </div>
        </div>
      </template>

      <el-alert
        title="本页聚合 5 类风险预警：逾期风险 / 高频借贷 / 征信异常 / 预算超支 / 现金流预警，均为模拟口径；点击预警行可跳转对应业务模块"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" @tab-change="loadActive">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.key"
          :label="`${tab.name} (${(tab.data || []).length})`"
          :name="tab.key"
        />
      </el-tabs>

      <div v-loading="activeLoading" class="risk-list">
        <el-table :data="activeData" stripe highlight-current-row @row-click="handleRowClick" class="risk-table">
          <el-table-column prop="warningType" label="类型" width="160">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.warningType)" size="small">
                {{ typeName(row.warningType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="warningLevel" label="等级" width="90">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.warningLevel)" size="small" effect="dark">
                {{ levelName(row.warningLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="warningTitle" label="标题" min-width="180" />
          <el-table-column prop="warningContent" label="内容" min-width="280" show-overflow-tooltip />
          <el-table-column prop="relatedModule" label="模块" width="120">
            <template #default="{ row }">
              {{ moduleName(row.relatedModule) }}
            </template>
          </el-table-column>
          <el-table-column label="处理状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.isHandled" type="success" size="small">已处理</el-tag>
              <el-tag v-else type="warning" size="small">待处理</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="预警时间" width="160">
            <template #default="{ row }">{{ formatTime(row.warningTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default>
              <el-link type="primary" :underline="false">查看详情 ›</el-link>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!activeLoading && !activeData.length" description="暂无此类风险预警" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getRiskOverview,
  predictOverdueRisk,
  detectHighFreqBorrow,
  detectCashflowWarning
} from '@/api/risk'

const router = useRouter()

// 预警类型 → 业务模块路由（行点击跳转，模拟联动）
const moduleRouteMap = {
  OVERDUE_RISK: '/loan',          // 逾期风险 → 青创e贷（还款中心）
  HIGH_FREQ_BORROW: '/loan',       // 高频借贷 → 青创e贷（额度管理）
  CREDIT_ABNORMAL: '/credit-profile', // 征信异常 → 青年信用画像
  BUDGET_OVER: '/budget',          // 预算超支 → 预算消费
  CASHFLOW_WARNING: '/bookkeeping' // 现金流预警 → 经营赋能
}

const tabs = ref([
  { key: 'OVERDUE_RISK', name: '逾期风险', data: [] },
  { key: 'HIGH_FREQ_BORROW', name: '高频借贷', data: [] },
  { key: 'CREDIT_ABNORMAL', name: '征信异常', data: [] },
  { key: 'BUDGET_OVER', name: '预算超支', data: [] },
  { key: 'CASHFLOW_WARNING', name: '现金流预警', data: [] }
])
const activeTab = ref('OVERDUE_RISK')
const activeLoading = ref(false)
const detectLoading = ref(false)
const unhandledCount = ref(0)

const activeData = computed(() => {
  const t = tabs.value.find(x => x.key === activeTab.value)
  return t ? t.data : []
})

const typeMap = {
  OVERDUE_RISK: '逾期风险',
  HIGH_FREQ_BORROW: '高频借贷',
  CREDIT_ABNORMAL: '征信异常',
  BUDGET_OVER: '预算超支',
  CASHFLOW_WARNING: '现金流预警'
}
const levelMap = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '紧急' }
const moduleMap = {
  SAFETY: '金融安全',
  CONSUMPTION: '消费治理',
  BUDGET: '预算消费',
  OPERATION: '经营赋能',
  GUARANTEE: '安居保函',
  LOAN: '青创e贷'
}

function typeName(t) { return typeMap[t] || t }
function levelName(l) { return levelMap[l] || l }
function moduleName(m) { return moduleMap[m] || m || '-' }
function typeTagType(t) {
  return { OVERDUE_RISK: 'danger', HIGH_FREQ_BORROW: 'warning', CREDIT_ABNORMAL: 'danger', BUDGET_OVER: 'warning', CASHFLOW_WARNING: 'primary' }[t] || ''
}
function levelTagType(l) {
  return { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[l] || 'info'
}
function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 16)
}

async function loadAll() {
  activeLoading.value = true
  try {
    const data = await getRiskOverview()
    tabs.value[0].data = data.overdue || []
    tabs.value[1].data = data.highFreqBorrow || []
    tabs.value[2].data = data.creditAbnormal || []
    tabs.value[3].data = data.budgetOver || []
    tabs.value[4].data = data.cashflow || []
    unhandledCount.value = data.unhandledCount || 0
  } catch (e) {
    // 请求失败保持空列表，不阻塞页面
  } finally {
    activeLoading.value = false
  }
}

async function loadActive() {
  // 已在 loadAll 中加载，切换 Tab 时无需重复请求
}

/** 行点击跳转对应业务模块（模拟联动） */
function handleRowClick(row) {
  const path = moduleRouteMap[row.warningType]
  if (path) router.push(path)
}

async function handleDetect() {
  detectLoading.value = true
  try {
    const [overdue, highFreq, cashflow] = await Promise.all([
      predictOverdueRisk(7).catch(e => null),
      detectHighFreqBorrow().catch(e => null),
      detectCashflowWarning().catch(e => null)
    ])
    const parts = []
    if (overdue) parts.push('逾期风险检测完成')
    if (highFreq) parts.push('高频借贷检测完成')
    if (cashflow) parts.push('现金流检测完成')
    ElMessage.success(`${parts.join('、')}（模拟）`)
    await loadAll()
  } finally {
    detectLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.risk-page {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.unhandled-badge {
  margin-left: 8px;
  vertical-align: 1px;
}
.risk-table {
  cursor: pointer;
}
.risk-list {
  min-height: 240px;
}
</style>
