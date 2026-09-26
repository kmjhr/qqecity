<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>① 发起保函申请（含 AI 合同复审）</template>
          <el-form :model="form" label-width="90px">
            <el-form-item label="房东姓名"><el-input v-model="form.landlordName" placeholder="如：张先生" /></el-form-item>
            <el-form-item label="房东电话"><el-input v-model="form.landlordPhone" placeholder="用于线上确认" /></el-form-item>
            <el-form-item label="房源地址"><el-input v-model="form.houseAddress" placeholder="小区/门牌" /></el-form-item>
            <el-form-item label="月租金"><el-input-number v-model="form.monthlyRent" :min="0" :precision="2" style="width:100%;" /></el-form-item>
            <el-form-item label="押金金额"><el-input-number v-model="form.depositAmount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
            <el-form-item label="租期开始"><el-date-picker v-model="form.leaseStart" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></el-form-item>
            <el-form-item label="租期结束"><el-date-picker v-model="form.leaseEnd" type="date" value-format="YYYY-MM-DD" style="width:100%;" /></el-form-item>
            <el-form-item label="保函期限"><el-input-number v-model="form.termMonths" :min="1" :max="36" style="width:100%;" />（月）</el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="applying" @click="onApply">提交申请</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" title="演示提示" description="押金超过3个月租金、月租异常或命中风险词时将转人工复核（模拟规则）。" />
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>我的保函</template>
          <el-table :data="list" v-loading="loading" size="small">
            <el-table-column prop="applyId" label="申请单号" width="80" />
            <el-table-column prop="guaranteeAmount" label="保函金额" width="110" />
            <el-table-column prop="feeAmount" label="保函费" width="100" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.applyStatus)">{{ statusText(row.applyStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="170">
              <template #default="{ row }">
                <el-button v-if="row.applyStatus === 0" size="small" type="success" @click="onConfirm(row)">房东确认</el-button>
                <el-button v-if="row.applyStatus === 1" size="small" type="primary" @click="onPay(row)">缴费出函</el-button>
                <el-button size="small" link type="primary" @click="onDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card v-if="detailData" shadow="never" style="margin-top:16px;">
          <template #header>保函详情（含复审意见）</template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="AI复审状态">
              {{ detailData.contract?.reviewStatus === 1 ? '自动通过' : (detailData.contract?.reviewStatus === 2 ? '转人工' : '待审') }}
            </el-descriptions-item>
            <el-descriptions-item label="复审意见">{{ detailData.contract?.reviewResult }}</el-descriptions-item>
            <el-descriptions-item label="受益人">{{ detailData.guarantee?.beneficiary || '—' }}</el-descriptions-item>
            <el-descriptions-item label="电子保函编号">{{ detailData.guarantee?.guaranteeNo || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { guaranteeApi } from '../api'

const form = reactive({
  landlordName: '张先生', landlordPhone: '13900000001', houseAddress: '朝阳区演示小区3号楼',
  monthlyRent: 2500, depositAmount: 2500,
  leaseStart: '2026-10-01', leaseEnd: '2027-09-30', termMonths: 12
})
const list = ref([])
const loading = ref(false)
const applying = ref(false)
const detailData = ref(null)

const statusMap = {
  0: ['info', '待房东确认'], 1: ['warning', '待缴费'], 2: ['success', '已开函'],
  3: ['info', '已失效'], 4: ['danger', '赔付中']
}
const statusText = s => statusMap[s]?.[1] || '未知'
const statusType = s => statusMap[s]?.[0] || 'info'

async function load() {
  loading.value = true
  try {
    list.value = await guaranteeApi.list()
  } finally {
    loading.value = false
  }
}

async function onApply() {
  applying.value = true
  try {
    const id = await guaranteeApi.apply(form)
    ElMessage.success(`申请成功，申请单号 ${id}`)
    await load()
  } finally {
    applying.value = false
  }
}

async function onConfirm(row) {
  await guaranteeApi.confirm(row.applyId)
  ElMessage.success('房东已确认')
  await load()
}

async function onPay(row) {
  await guaranteeApi.pay(row.applyId)
  ElMessage.success('缴费成功，电子保函已开出（模拟）')
  await load()
}

async function onDetail(row) {
  detailData.value = await guaranteeApi.detail(row.applyId)
}

onMounted(load)
</script>
