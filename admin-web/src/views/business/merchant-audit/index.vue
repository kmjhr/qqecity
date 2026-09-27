<template>
  <div class="merchant-audit-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>④ 商户白名单审核</span>
          <el-radio-group v-model="verifyStatus" size="small" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">灰名单</el-radio-button>
            <el-radio-button label="VERIFIED">白名单</el-radio-button>
            <el-radio-button label="REJECTED">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="merchantName" label="商户名称" min-width="160" />
        <el-table-column prop="merchantType" label="类型" width="120" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="verifyStatus" label="认证状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="verifyTagType(row.verifyStatus)" size="small">
              {{ verifyStatusName(row.verifyStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.verifyStatus === 'PENDING'"
              type="success" link size="small"
              @click="handleAudit(row, 'VERIFIED')"
            >通过</el-button>
            <el-button
              v-if="row.verifyStatus === 'PENDING'"
              type="danger" link size="small"
              @click="handleAudit(row, 'REJECTED')"
            >拒绝</el-button>
            <span v-else style="color: #909399; font-size: 12px">已审核</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantsByStatus, auditMerchant } from '@/api/business/loan'

const loading = ref(false)
const tableData = ref<any[]>([])
const verifyStatus = ref('PENDING')

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getMerchantsByStatus(verifyStatus.value || undefined)
  } finally {
    loading.value = false
  }
}

function handleAudit(row: any, decision: 'VERIFIED' | 'REJECTED') {
  ElMessageBox.confirm(
    `确认将商户「${row.merchantName}」${decision === 'VERIFIED' ? '加入白名单' : '拒绝'}？`,
    decision === 'VERIFIED' ? '商户白名单审核 - 通过' : '商户白名单审核 - 拒绝',
    { type: decision === 'VERIFIED' ? 'success' : 'warning' }
  ).then(async () => {
    await auditMerchant(row.id, decision)
    ElMessage.success('审核已提交，状态已同步至用户端')
    loadData()
  }).catch(() => {})
}

function verifyStatusName(s: string) {
  const m: Record<string, string> = {
    PENDING: '灰名单',
    VERIFIED: '白名单',
    REJECTED: '已拒绝'
  }
  return m[s] || s
}
function verifyTagType(s: string): any {
  const m: Record<string, string> = {
    PENDING: 'warning',
    VERIFIED: 'success',
    REJECTED: 'danger'
  }
  return m[s] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.merchant-audit-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
