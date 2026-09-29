<template>
  <div class="anti-fraud-manage-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>反诈教学内容管理（安全教育平台）</span>
          <el-button type="primary" @click="openEdit()">+ 新增内容</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.contentType] ?? row.contentType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览量" width="90" align="right" />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
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
    <el-dialog v-model="editVisible" :title="editingId ? '编辑内容' : '新增内容'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="反诈文章/视频/案例标题" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.contentType" style="width: 100%">
            <el-option label="文章 ARTICLE" value="ARTICLE" />
            <el-option label="视频 VIDEO" value="VIDEO" />
            <el-option label="案例 CASE" value="CASE" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如：电信诈骗 / 刷单返利 / 冒充客服" />
        </el-form-item>
        <el-form-item label="摘要" required>
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="内容摘要" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="正文内容" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="图片 URL（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { listContents, createContent, updateContent, toggleContent, deleteContent } from '@/api/business/content'

const typeMap: Record<string, string> = {
  ARTICLE: '文章',
  VIDEO: '视频',
  CASE: '案例'
}

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const editVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<any>({
  title: '',
  contentType: 'ARTICLE',
  category: '',
  summary: '',
  content: '',
  coverImage: '',
  sortOrder: 0,
  status: 1
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    tableData.value = await listContents()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.contentType = 'ARTICLE'
  form.category = ''
  form.summary = ''
  form.content = ''
  form.coverImage = ''
  form.sortOrder = 0
  form.status = 1
}

function openEdit(row?: any) {
  editingId.value = row?.id ?? null
  if (row) {
    Object.assign(form, {
      title: row.title,
      contentType: row.contentType,
      category: row.category,
      summary: row.summary,
      content: row.content,
      coverImage: row.coverImage,
      sortOrder: row.sortOrder,
      status: row.status
    })
  } else {
    resetForm()
  }
  editVisible.value = true
}

async function handleSave() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('标题与摘要必填')
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await updateContent(editingId.value, { ...form })
      ElMessage.success('内容已更新')
    } else {
      await createContent({ ...form })
      ElMessage.success('内容已新增')
    }
    editVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleToggle(row: any) {
  await toggleContent(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已发布')
  loadData()
}

async function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除内容「${row.title}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteContent(row.id)
      ElMessage.success('已删除')
      loadData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.anti-fraud-manage-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
