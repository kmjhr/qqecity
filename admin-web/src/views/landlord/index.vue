<template>
  <div class="landlord-manage">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="姓名/手机号/证件号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="searchForm.verifyStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待认证" value="PENDING" />
            <el-option label="已认证" value="VERIFIED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 房东列表 -->
    <el-card shadow="never" class="table-card" style="margin-top: 16px">
      <div class="card-tip">
        房东为独立业务主体（类似商户），不与普通用户混列，在此分区统一管理。
      </div>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="username" label="登录账号" min-width="120">
          <template #default="{ row }">
            <span>{{ row.username || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="idCard" label="身份证号" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.idCard || '-' }}</template>
        </el-table-column>
        <el-table-column prop="bankName" label="开户银行" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bankName || '-' }}</template>
        </el-table-column>
        <el-table-column label="认证状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.verifyStatus === 'VERIFIED' ? 'success' : row.verifyStatus === 'REJECTED' ? 'danger' : 'warning'"
            >
              {{ verifyName(row.verifyStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="houseCount" label="房屋数" width="90" align="center" />
        <el-table-column label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.verifyStatus !== 'VERIFIED'"
              type="success"
              link
              size="small"
              @click="handleVerify(row, 'VERIFIED')"
            >
              通过认证
            </el-button>
            <el-button
              v-if="row.verifyStatus !== 'REJECTED'"
              type="warning"
              link
              size="small"
              @click="handleVerify(row, 'REJECTED')"
            >
              驳回
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑房东信息" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="form.bankName" placeholder="请输入开户银行" />
        </el-form-item>
        <el-form-item label="收款账号">
          <el-input v-model="form.bankAccount" placeholder="请输入收款银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  getLandlordPage,
  updateLandlord,
  verifyLandlord,
  deleteLandlord,
  type LandlordInfo
} from '@/api/landlord'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableData = ref<LandlordInfo[]>([])

const searchForm = reactive({
  keyword: '',
  verifyStatus: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive<Partial<LandlordInfo>>({
  id: undefined,
  realName: '',
  phone: '',
  idCard: '',
  bankName: '',
  bankAccount: ''
})

const formRules: FormRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const verifyName = (v: string) => ({ PENDING: '待认证', VERIFIED: '已认证', REJECTED: '已驳回' }[v] || v)

async function loadData() {
  loading.value = true
  try {
    const data = await getLandlordPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      verifyStatus: searchForm.verifyStatus || undefined
    })
    tableData.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.verifyStatus = ''
  pagination.pageNum = 1
  loadData()
}

function handleEdit(row: LandlordInfo) {
  form.id = row.id
  form.realName = row.realName
  form.phone = row.phone
  form.idCard = row.idCard
  form.bankName = row.bankName
  form.bankAccount = row.bankAccount
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!form.id) return
  submitLoading.value = true
  try {
    await updateLandlord(form.id, {
      realName: form.realName,
      phone: form.phone,
      idCard: form.idCard,
      bankName: form.bankName,
      bankAccount: form.bankAccount
    })
    ElMessage.success('已保存')
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleVerify(row: LandlordInfo, verifyStatus: string) {
  const action = verifyStatus === 'VERIFIED' ? '通过认证' : '驳回认证'
  await ElMessageBox.confirm(`确认对房东「${row.realName}」${action}？`, '提示', { type: 'warning' })
  await verifyLandlord(row.id, verifyStatus)
  ElMessage.success(`已${action}`)
  loadData()
}

async function handleDelete(row: LandlordInfo) {
  await ElMessageBox.confirm(`确认删除房东「${row.realName}」？关联账号将同步停用。`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteLandlord(row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
