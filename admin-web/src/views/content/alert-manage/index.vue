<template>
  <div class="alert-manage-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>实时预警管理（安全教育平台）</span>
          <el-button type="primary" @click="openEdit()">+ 新增预警</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="levelMap[row.alertLevel]?.type ?? 'info'" size="small">
              {{ levelMap[row.alertLevel]?.label ?? row.alertLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="预警标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
        <el-table-column prop="region" label="地区" width="110" />
        <el-table-column prop="summary" label="内容摘要" min-width="240" show-overflow-tooltip />
        <el-table-column label="场景" width="110">
          <template #default="{ row }">
            {{ sceneMap[row.relateScene] ?? row.relateScene }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '发布' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggle(row)"
            >
              {{ row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑预警' : '新增预警'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="预警标题" required>
          <el-input v-model="form.title" placeholder="如：警惕「征信洗白」骗局" />
        </el-form-item>
        <el-form-item label="预警级别" required>
          <el-select v-model="form.alertLevel" style="width: 100%">
            <el-option label="危险 DANGER" value="DANGER" />
            <el-option label="警告 WARNING" value="WARNING" />
            <el-option label="提示 INFO" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="如：公安反诈中心（模拟）" />
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="form.region" placeholder="如：全国" />
        </el-form-item>
        <el-form-item label="内容摘要" required>
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="预警内容" />
        </el-form-item>
        <el-form-item label="官方链接">
          <el-input v-model="form.linkUrl" placeholder="可填举报/提示入口（可选）" />
        </el-form-item>
        <el-form-item label="关联场景">
          <el-select v-model="form.relateScene" style="width: 100%">
            <el-option label="保函 GUARANTEE" value="GUARANTEE" />
            <el-option label="创业贷 LOAN" value="LOAN" />
            <el-option label="征信 CREDIT" value="CREDIT" />
            <el-option label="理财 WEALTH" value="WEALTH" />
            <el-option label="平台客服 PLATFORM" value="PLATFORM" />
            <el-option label="其他 OTHER" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="下架" />
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
import { listAlerts, createAlert, updateAlert, toggleAlert, deleteAlert } from '@/api/business/content'

const levelMap: Record<string, { label: string; type: string }> = {
  DANGER: { label: '危险', type: 'danger' },
  WARNING: { label: '警告', type: 'warning' },
  INFO: { label: '提示', type: 'info' }
}

const sceneMap: Record<string, string> = {
  GUARANTEE: '保函',
  LOAN: '创业贷',
  CREDIT: '征信',
  WEALTH: '理财',
  PLATFORM: '平台客服',
  OTHER: '其他'
}

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const editVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<any>({
  title: '',
  alertLevel: 'WARNING',
  source: '',
  region: '',
  summary: '',
  linkUrl: '',
  relateScene: 'OTHER',
  status: 1
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    tableData.value = await listAlerts()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.alertLevel = 'WARNING'
  form.source = ''
  form.region = ''
  form.summary = ''
  form.linkUrl = ''
  form.relateScene = 'OTHER'
  form.status = 1
}

function openEdit(row?: any) {
  editingId.value = row?.id ?? null
  if (row) {
    Object.assign(form, {
      title: row.title,
      alertLevel: row.alertLevel,
      source: row.source,
      region: row.region,
      summary: row.summary,
      linkUrl: row.linkUrl,
      relateScene: row.relateScene,
      status: row.status
    })
  } else {
    resetForm()
  }
  editVisible.value = true
}

async function handleSave() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('预警标题与内容摘要必填')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updateAlert(editingId.value, { ...form })
      ElMessage.success('预警已更新')
    } else {
      await createAlert({ ...form })
      ElMessage.success('预警已新增')
    }
    editVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleToggle(row: any) {
  await toggleAlert(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已发布')
  loadData()
}

async function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除预警「${row.title}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteAlert(row.id)
      ElMessage.success('已删除')
      loadData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.alert-manage-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
