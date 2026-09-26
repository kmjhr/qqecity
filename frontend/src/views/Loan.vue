<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>② 青创e贷申请（B类两步式演示）</template>
          <el-form :model="form" label-width="90px">
            <el-form-item label="贷款类型">
              <el-select v-model="form.loanType" style="width:100%;">
                <el-option label="A类（创业经营，最高5万）" value="A" />
                <el-option label="B类（轻创业，1—2万）" value="B" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请金额"><el-input-number v-model="form.applyAmount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
            <el-form-item label="贷款用途"><el-input v-model="form.loanPurpose" placeholder="限合法经营用途" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="applying" @click="onApply">
                提交申请（免费预审·不查征信）
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" title="B类两步式（设计口径）"
            description="第一步：免费预审，不触发征信硬查询；第二步：审批通过后贷款 100% 受托支付至商户，不进入个人账户。" />
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>我的贷款</template>
          <el-table :data="list" v-loading="loading" size="small">
            <el-table-column prop="loanApplyId" label="申请号" width="80" />
            <el-table-column prop="loanType" label="类型" width="60" />
            <el-table-column prop="applyAmount" label="申请金额" width="110" />
            <el-table-column prop="precheckRange" label="预审区间" width="110" />
            <el-table-column prop="approveAmount" label="审批额度" width="100">
              <template #default="{ row }">{{ row.approveAmount || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.approveStatus === 1" type="success">已审批</el-tag>
                <el-tag v-else-if="row.approveStatus === 2" type="danger">已拒绝</el-tag>
                <el-tag v-else type="warning">待审批</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="180">
              <template #default="{ row }">
                <el-button v-if="row.approveStatus === 0" size="small" type="primary" @click="onApprove(row)">模拟审批</el-button>
                <el-button v-if="row.approveStatus === 1" size="small" type="success" @click="openPay(row)">受托支付</el-button>
                <el-button size="small" link type="primary" @click="onDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card v-if="detailData" shadow="never" style="margin-top:16px;">
          <template #header>贷款详情与受托支付记录</template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="预审状态">{{ detailData.apply.precheckStatus === 1 ? '通过（未查征信）' : '未预审' }}</el-descriptions-item>
            <el-descriptions-item label="利率">{{ detailData.apply.interestRate ? (detailData.apply.interestRate * 100).toFixed(2) + '%' : '—' }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="detailData.payments?.length" :data="detailData.payments" size="small" style="margin-top:10px;">
            <el-table-column prop="merchantName" label="收款商户" />
            <el-table-column prop="payAmount" label="支付金额" />
            <el-table-column prop="payTime" label="支付时间" />
            <el-table-column prop="voucherUrl" label="凭证" />
          </el-table>
          <el-empty v-else description="暂无受托支付记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="payVisible" title="100% 受托支付（模拟）" width="420px">
      <el-form label-width="80px">
        <el-form-item label="收款商户"><el-input v-model="payForm.merchantName" placeholder="演示白名单商户" /></el-form-item>
        <el-form-item label="支付金额"><el-input-number v-model="payForm.amount" :min="0" :precision="2" style="width:100%;" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="onPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { loanApi } from '../api'

const form = reactive({ loanType: 'B', applyAmount: 15000, loanPurpose: '市集摊位物料采购' })
const list = ref([])
const loading = ref(false)
const applying = ref(false)
const detailData = ref(null)
const payVisible = ref(false)
const payForm = reactive({ merchantName: '', amount: 0 })
let currentApplyId = null

async function load() {
  loading.value = true
  try {
    list.value = await loanApi.list()
  } finally {
    loading.value = false
  }
}

async function onApply() {
  applying.value = true
  try {
    const id = await loanApi.apply(form)
    ElMessage.success(`申请成功（预审已通过，未查征信），申请号 ${id}`)
    await load()
  } finally {
    applying.value = false
  }
}

async function onApprove(row) {
  await loanApi.approve(row.loanApplyId)
  ElMessage.success('审批通过（模拟）')
  await load()
}

function openPay(row) {
  currentApplyId = row.loanApplyId
  payForm.merchantName = '演示商户·市集材料行'
  payForm.amount = Number(row.approveAmount || 0)
  payVisible.value = true
}

async function onPay() {
  await loanApi.entrustedPay(currentApplyId, payForm)
  payVisible.value = false
  ElMessage.success('受托支付成功，款项直达商户（模拟）')
  await load()
}

async function onDetail(row) {
  detailData.value = await loanApi.detail(row.loanApplyId)
}

onMounted(load)
</script>
