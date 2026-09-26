<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>④ 分类预算设置</template>
          <el-form :model="budgetForm" label-width="80px">
            <el-form-item label="月份">
              <el-date-picker v-model="budgetForm.budgetMonth" type="month" value-format="YYYY-MM" style="width:100%;" />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="budgetForm.category" style="width:100%;">
                <el-option v-for="c in ['餐饮', '娱乐', '购物', '交通', '其他']" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item label="预算金额"><el-input-number v-model="budgetForm.budgetAmount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSetBudget">保存预算</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top:16px;">
          <template #header>记录一笔支出（自动归类）</template>
          <el-form :model="expenseForm" label-width="80px">
            <el-form-item label="商户名称"><el-input v-model="expenseForm.merchantName" placeholder="如：美团外卖" /></el-form-item>
            <el-form-item label="金额"><el-input-number v-model="expenseForm.amount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
            <el-form-item>
              <el-button type="success" @click="onExpense">记一笔</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" description="MCC 模拟归类：按商户关键词自动归入餐饮/娱乐/购物/交通/其他。" />
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="never">
          <template #header>本月预算与分级提醒（50% / 20% / 超支）</template>
          <el-table :data="budgets" v-loading="loading" size="small">
            <el-table-column prop="category" label="分类" width="80" />
            <el-table-column prop="budgetAmount" label="预算" width="100" />
            <el-table-column prop="usedAmount" label="已用" width="100" />
            <el-table-column prop="remainRatio" label="剩余%" width="90">
              <template #default="{ row }">{{ row.remainRatio ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="提醒等级" width="100">
              <template #default="{ row }">
                <el-tag :type="levelType(row.remindLevel)">{{ levelText(row.remindLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="130">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" size="small" type="warning" @click="onCarryOver(row)">结余转储蓄</el-button>
                <el-tag v-else size="small" type="info">已结转</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { budgetApi } from '../api'

const budgetForm = reactive({ budgetMonth: currentMonth(), category: '餐饮', budgetAmount: 1500 })
const expenseForm = reactive({ merchantName: '美团外卖', amount: 60 })
const budgets = ref([])
const loading = ref(false)

function currentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const levelMap = { 0: ['info', '正常'], 1: ['warning', '温和提醒'], 2: ['danger', '紧张'], 3: ['danger', '已超支'] }
const levelText = l => levelMap[l]?.[1] || '正常'
const levelType = l => levelMap[l]?.[0] || 'info'

async function load() {
  loading.value = true
  try {
    budgets.value = await budgetApi.list(budgetForm.budgetMonth)
  } finally {
    loading.value = false
  }
}

async function onSetBudget() {
  await budgetApi.setBudget(budgetForm)
  ElMessage.success('预算已保存')
  await load()
}

async function onExpense() {
  const tx = await budgetApi.expense(expenseForm)
  ElMessage.success(`已归类为「${tx.category}」，预算已扣减`)
  await load()
}

async function onCarryOver(row) {
  const res = await budgetApi.carryOver({ budgetId: row.budgetId })
  ElMessage.success(`已转入心愿储蓄 ${res.carryoverAmount} 元（模拟）`)
  await load()
}

onMounted(load)
</script>
