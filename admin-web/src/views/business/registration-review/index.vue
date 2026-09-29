<template>
  <div class="registration-review-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>⑦ 注册审核记录 - 白名单 AI 审核留痕</span>
          <div class="filters">
            <el-input
              v-model="keyword"
              placeholder="用户名 / 姓名 / 手机号 / 审核编号"
              clearable
              style="width: 260px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-select v-model="resultFilter" placeholder="审核结论" clearable style="width: 140px" @change="handleSearch">
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="reviewNo" label="审核编号" min-width="170" />
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="人群类型" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ userTypeMap[row.userType] ?? row.userType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="140" show-overflow-tooltip />
        <el-table-column label="核验方式" width="120">
          <template #default="{ row }">
            {{ verifyTypeMap[row.verifyType] ?? row.verifyType }}
          </template>
        </el-table-column>
        <el-table-column label="证件照片" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.certPhoto"
              :src="row.certPhoto"
              :preview-src-list="[row.certPhoto]"
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 4px"
            />
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结论" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'APPROVED' ? 'success' : 'danger'" size="small">
              {{ row.result === 'APPROVED' ? '通过' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="审核时间" width="170" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 审核详情抽屉 -->
    <el-drawer v-model="detailVisible" title="注册 AI 审核详情" size="520px">
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="审核编号">{{ currentRow.reviewNo }}</el-descriptions-item>
        <el-descriptions-item label="用户名 / 姓名">
          {{ currentRow.username }} / {{ currentRow.realName }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="人群类型">
          <el-tag size="small">{{ userTypeMap[currentRow.userType] ?? currentRow.userType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="学校">{{ currentRow.school }}</el-descriptions-item>
        <el-descriptions-item label="学历层次">
          {{ educationMap[currentRow.educationLevel] ?? currentRow.educationLevel }}
        </el-descriptions-item>
        <el-descriptions-item label="毕业日期">{{ currentRow.graduationDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="核验方式">
          {{ verifyTypeMap[currentRow.verifyType] ?? currentRow.verifyType }}
        </el-descriptions-item>
        <el-descriptions-item label="核验凭证">
          {{ currentRow.studentNo ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="证件照片">
          <el-image
            v-if="currentRow.certPhoto"
            :src="currentRow.certPhoto"
            :preview-src-list="[currentRow.certPhoto]"
            fit="contain"
            style="width: 200px; max-height: 160px; border-radius: 6px"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="白名单审核">
          <el-tag :type="currentRow.whitelistPass === 1 ? 'success' : 'danger'" size="small">
            {{ currentRow.whitelistPass === 1 ? '通过' : '拒绝' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="白名单明细">{{ currentRow.whitelistDetail }}</el-descriptions-item>
        <el-descriptions-item label="材料核验">
          <el-tag :type="currentRow.materialPass === 1 ? 'success' : 'danger'" size="small">
            {{ currentRow.materialPass === 1 ? '通过' : '命中重复' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="材料明细">{{ currentRow.materialDetail }}</el-descriptions-item>
        <el-descriptions-item label="结论">
          <el-tag :type="currentRow.result === 'APPROVED' ? 'success' : 'danger'" size="small">
            {{ currentRow.result === 'APPROVED' ? '已通过（已注册）' : '已拒绝' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.rejectReason" label="拒绝原因">
          {{ currentRow.rejectReason }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentRow.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getRegistrationReviews } from '@/api/business/registrationReview'

const userTypeMap: Record<string, string> = {
  STUDENT: '在校生',
  GRADUATE: '毕业2年内',
  ENTREPRENEUR: '青年创业者',
  OTHER: '非白名单'
}

const verifyTypeMap: Record<string, string> = {
  XUE_XIN_WANG: '学信网在线核验',
  STUDENT_CARD: '学生证识别（仅在校生）',
  GRAD_CERT: '毕业证识别（仅毕业2年内）'
}

const educationMap: Record<string, string> = {
  UNDERGRADUATE: '本科',
  MASTER: '硕士',
  DOCTOR: '博士'
}

const loading = ref(false)
const tableData = ref<any[]>([])
const keyword = ref('')
const resultFilter = ref('')
const detailVisible = ref(false)
const currentRow = ref<any>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRegistrationReviews({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
      result: resultFilter.value || undefined
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function openDetail(row: any) {
  currentRow.value = row
  detailVisible.value = true
}
</script>

<style scoped>
.registration-review-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
