<template>
  <div class="portal-manage-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>政策门户管理（政策专区·官方入口导航）</span>
          <el-button type="primary" @click="openEdit()">+ 新增入口</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="portalName" label="官网名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="入口类型" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.portalType] ?? row.portalType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="120" />
        <el-table-column prop="url" label="官网链接" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="row.url" target="_blank" :disabled="!row.url">{{ row.url }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggle(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑入口' : '新增入口'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="官网名称" required>
          <el-input v-model="form.portalName" placeholder="如：国家社会保险公共服务平台" />
        </el-form-item>
        <el-form-item label="入口类型" required>
          <el-select v-model="form.portalType" style="width: 100%">
            <el-option label="人社 GOV_HR" value="GOV_HR" />
            <el-option label="住建房管 GOV_HOUSING" value="GOV_HOUSING" />
            <el-option label="政务服务 GOV_AFFAIR" value="GOV_AFFAIR" />
            <el-option label="税务 GOV_TAX" value="GOV_TAX" />
            <el-option label="教育高校 GOV_EDU" value="GOV_EDU" />
            <el-option label="其他 GOV_OTHER" value="GOV_OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="form.region" placeholder="如：全国 / 广东省 / 浙江省" />
        </el-form-item>
        <el-form-item label="官网链接" required>
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="入口说明（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-value="ACTIVE"
            inactive-value="INACTIVE"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPortals, createPortal, updatePortal, togglePortal, deletePortal } from '@/api/business/content'

const typeMap: Record<string, string> = {
  GOV_HR: '人社',
  GOV_HOUSING: '住建房管',
  GOV_AFFAIR: '政务服务',
  GOV_TAX: '税务',
  GOV_EDU: '教育高校',
  GOV_OTHER: '其他'
}

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const editVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<any>({
  portalName: '',
  portalType: 'GOV_HR',
  region: '',
  url: '',
  description: '',
  sortOrder: 0,
  status: 'ACTIVE'
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    tableData.value = await listPortals()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.portalName = ''
  form.portalType = 'GOV_HR'
  form.region = ''
  form.url = ''
  form.description = ''
  form.sortOrder = 0
  form.status = 'ACTIVE'
}

function openEdit(row?: any) {
  editingId.value = row?.id ?? null
  if (row) {
    Object.assign(form, {
      portalName: row.portalName,
      portalType: row.portalType,
      region: row.region,
      url: row.url,
      description: row.description,
      sortOrder: row.sortOrder,
      status: row.status
    })
  } else {
    resetForm()
  }
  editVisible.value = true
}

async function handleSave() {
  if (!form.portalName.trim() || !form.url.trim()) {
    ElMessage.warning('官网名称与链接必填')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updatePortal(editingId.value, { ...form })
      ElMessage.success('入口已更新')
    } else {
      await createPortal({ ...form })
      ElMessage.success('入口已新增')
    }
    editVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleToggle(row: any) {
  await togglePortal(row.id, row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
  ElMessage.success(row.status === 'ACTIVE' ? '已停用' : '已启用')
  loadData()
}

async function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除入口「${row.portalName}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deletePortal(row.id)
      ElMessage.success('已删除')
      loadData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.portal-manage-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
