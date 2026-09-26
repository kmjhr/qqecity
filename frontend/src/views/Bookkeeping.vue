<template>
  <div>
    <el-card shadow="never">
      <template #header>③ 创业经营赋能（占位）</template>
      <el-descriptions :column="2" border size="small" v-if="report">
        <el-descriptions-item label="统计周期">{{ report.period }}</el-descriptions-item>
        <el-descriptions-item label="净现金流">{{ report.netCashflow }}</el-descriptions-item>
        <el-descriptions-item label="总收入">{{ report.totalIncome }}</el-descriptions-item>
        <el-descriptions-item label="总支出">{{ report.totalExpense }}</el-descriptions-item>
        <el-descriptions-item label="简易利润测算">{{ report.profitEstimate }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ report.tip }}</el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" style="margin-top:12px;" :loading="loading" @click="load">重新加载（模拟报表）</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { bookkeepingApi } from '../api'

const report = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    report.value = await bookkeepingApi.report()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
