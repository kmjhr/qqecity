<template>
  <div class="merchant-audit-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>④ 商户白名单审核 - AI 审核留痕（模拟）</span>
          <el-radio-group v-model="verifyStatus" size="small" @change="loadData">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="VERIFIED">白名单</el-radio-button>
            <el-radio-button label="REJECTED">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="商户提交后由模拟 AI 智能审核：信息完整且有佐证 → 自动加入白名单；名称异常/缺营业执照与佐证 → 自动拒绝，审核意见留痕可查【模拟】" />

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="merchantName" label="商户名称" min-width="170" />
        <el-table-column prop="merchantType" label="类型" width="110" />
        <el-table-column prop="contactName" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column label="AI 审核结论" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="verifyTagType(row.verifyStatus)" size="small">
              {{ verifyStatusName(row.verifyStatus) }}
            </el-tag>
            <span style="margin-left:4px; font-size:11px; color:#909399">AI</span>
          </template>
        </el-table-column>
        <el-table-column label="佐证材料" width="200" align="center">
          <template #default="{ row }">
            <template v-if="parseEvidence(row.evidenceMaterial).length">
              <el-image
                v-for="(ev, i) in parseEvidence(row.evidenceMaterial).slice(0, 3)"
                :key="i"
                :src="ev.isImage ? ev.data : ''"
                :preview-src-list="parseEvidence(row.evidenceMaterial).filter(e => e.isImage).map(e => e.data)"
                :initial-index="i"
                fit="cover"
                style="width: 44px; height: 44px; margin: 2px; border-radius: 4px"
              >
                <template #error>
                  <div style="font-size: 10px; color: #909399; text-align: center; line-height: 44px">{{ ev.name }}</div>
                </template>
              </el-image>
            </template>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime || createTime" label="审核时间" width="180" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- AI 审核留痕详情 -->
    <el-dialog v-model="detailVisible" title="商户 AI 审核留痕" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="商户名称">{{ current?.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="商户类型">{{ current?.merchantType }}</el-descriptions-item>
        <el-descriptions-item label="联系人 / 电话">{{ current?.contactName }} / {{ current?.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="商户来源">
          {{ current?.merchantSource === 'USER_CUSTOM' ? '用户自定义' : '平台预置' }}
          <span v-if="current?.applicantUserId" style="color:#909399">（申请人 ID：{{ current?.applicantUserId }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="AI 审核结论">
          <el-tag :type="verifyTagType(current?.verifyStatus)" size="small">
            {{ verifyStatusName(current?.verifyStatus) }}
          </el-tag>
          <span style="margin-left:6px; font-size:12px; color:#909399">
            {{ current?.reviewerId === 0 ? 'AI 智能审核（模拟）' : '人工兜底审核' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="AI 审核意见">
          <div style="white-space:pre-wrap; line-height:1.6">{{ current?.reviewRemark || '暂无' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ current?.reviewTime || current?.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="parseEvidence(current?.evidenceMaterial).length" label="佐证材料">
          <div style="display:flex; flex-wrap:wrap; gap:6px">
            <el-image
              v-for="(ev, i) in parseEvidence(current?.evidenceMaterial)"
              :key="i"
              :src="ev.isImage ? ev.data : ''"
              :preview-src-list="parseEvidence(current?.evidenceMaterial).filter(e => e.isImage).map(e => e.data)"
              :initial-index="i"
              fit="cover"
              style="width:56px; height:56px; border-radius:4px"
            >
              <template #error>
                <div style="font-size:10px; color:#909399; text-align:center; line-height:56px">{{ ev.name }}</div>
              </template>
            </el-image>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchantsByStatus } from '@/api/business/loan'

const loading = ref(false)
const tableData = ref<any[]>([])
const verifyStatus = ref('')
const detailVisible = ref(false)
const current = ref<any>(null)

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getMerchantsByStatus(verifyStatus.value || undefined)
  } finally {
    loading.value = false
  }
}

function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

function parseEvidence(raw?: string | null): { name: string; data: string; isImage: boolean }[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return []
    return arr
      .filter((e: any) => e && e.data)
      .map((e: any) => ({
        name: e.name || '附件',
        data: e.data,
        isImage: typeof e.data === 'string' && e.data.startsWith('data:image')
      }))
  } catch {
    return []
  }
}

function verifyStatusName(s: string) {
  const m: Record<string, string> = {
    VERIFIED: '通过 · 白名单',
    REJECTED: '拒绝'
  }
  return m[s] || s || '-'
}
function verifyTagType(s: string): any {
  const m: Record<string, string> = {
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
