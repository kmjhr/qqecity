<template>
  <div class="guarantee-page">
    <!-- 模块头部 -->
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon :size="32" color="#fff"><Wallet /></el-icon>
        </div>
        <div class="module-info">
          <h2>安居金融风控</h2>
          <p>租房履约保函全流程 · AI合同复审 · 违约索赔</p>
          <el-tag type="warning" size="small">演示系统 · 银行能力模拟</el-tag>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="applyDialogVisible = true">
            <el-icon><Plus /></el-icon>申请保函
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 状态流转说明 -->
    <el-card shadow="never" class="flow-card" v-if="statusFlow.length">
      <div class="flow-title">保函状态流转</div>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="(s, i) in statusFlow" :key="s.status" :title="s.name" :description="s.desc" />
      </el-steps>
    </el-card>

    <!-- 我的保函申请列表 -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span>我的保函申请</span>
          <el-radio-group v-model="filterStatus" size="small" @change="loadList">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button v-for="s in statusFlow" :key="s.status" :value="s.status">{{ s.name }}</el-radio-button>
          </el-radio-group>
          <el-button size="small" @click="loadList">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" empty-text="暂无保函申请，点击上方「申请保函」开始" stripe>
        <el-table-column prop="applyNo" label="申请编号" width="160" />
        <el-table-column prop="houseTitle" label="房屋" min-width="140" />
        <el-table-column prop="landlordName" label="房东" width="90" />
        <el-table-column prop="depositAmount" label="押金/保函额" width="110" align="right">
          <template #default="{ row }">¥{{ row.depositAmount }}</template>
        </el-table-column>
        <el-table-column prop="guaranteeFee" label="保函费" width="90" align="right">
          <template #default="{ row }">¥{{ row.guaranteeFee }}</template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.applyStatus)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.applyStatus === 'PENDING_PAY'" link type="success" size="small" @click="doPay(row)">缴费出函</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="p => { pageNum = p; loadList() }"
        />
      </div>
    </el-card>

    <!-- 保函申请弹窗 -->
    <el-dialog v-model="applyDialogVisible" title="申请租房履约保函" width="720px" top="6vh">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="110px">
        <el-divider content-position="left">房东信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="房东姓名" prop="landlordName"><el-input v-model="applyForm.landlordName" placeholder="请输入房东姓名" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="房东电话" prop="landlordPhone"><el-input v-model="applyForm.landlordPhone" placeholder="11位手机号" maxlength="11" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">房屋信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="房屋标题" prop="houseTitle"><el-input v-model="applyForm.houseTitle" placeholder="如：朝阳区精装一居" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="房屋类型"><el-select v-model="applyForm.houseType" placeholder="选择类型" style="width:100%"><el-option label="公寓" value="APARTMENT" /><el-option label="住宅" value="HOUSE" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="详细地址" prop="address"><el-input v-model="applyForm.address" placeholder="省市区+详细地址" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="面积(㎡)"><el-input-number v-model="applyForm.area" :min="0" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="居室数"><el-input-number v-model="applyForm.roomCount" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">租赁要素</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="月租金" prop="monthlyRent"><el-input-number v-model="applyForm.monthlyRent" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="押金金额" prop="depositAmount"><el-input-number v-model="applyForm.depositAmount" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="付款方式"><el-select v-model="applyForm.payMethod" placeholder="选择" style="width:100%"><el-option label="月付" value="MONTHLY" /><el-option label="季付" value="QUARTERLY" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="租期开始" prop="rentStartDate"><el-date-picker v-model="applyForm.rentStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="租期结束" prop="rentEndDate"><el-date-picker v-model="applyForm.rentEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="合同条款"><el-input v-model="applyForm.contractTerms" type="textarea" :rows="3" placeholder="可选，AI 复审将扫描关键词（模拟）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请（模拟）</el-button>
      </template>
    </el-dialog>

    <!-- 申请详情抽屉 -->
    <el-drawer v-model="detailVisible" title="保函申请详情" size="480px">
      <el-descriptions v-if="currentDetail" :column="1" border>
        <el-descriptions-item label="申请编号">{{ currentDetail.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentDetail.applyStatus)">{{ currentDetail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房屋">{{ currentDetail.houseTitle }}（{{ currentDetail.houseAddress }}）</el-descriptions-item>
        <el-descriptions-item label="房东">{{ currentDetail.landlordName }}（{{ currentDetail.landlordPhone }}）</el-descriptions-item>
        <el-descriptions-item label="月租金">¥{{ currentDetail.monthlyRent }}</el-descriptions-item>
        <el-descriptions-item label="押金/保函额">¥{{ currentDetail.depositAmount }}</el-descriptions-item>
        <el-descriptions-item label="保函费率">{{ currentDetail.guaranteeRate }}%</el-descriptions-item>
        <el-descriptions-item label="保函费">¥{{ currentDetail.guaranteeFee }}</el-descriptions-item>
        <el-descriptions-item label="租期">{{ currentDetail.rentStartDate }} 至 {{ currentDetail.rentEndDate }}</el-descriptions-item>
        <el-descriptions-item label="AI复审" v-if="currentDetail.aiReviewResult">
          {{ currentDetail.aiReviewResult }}（评分 {{ currentDetail.aiReviewScore }}）
          <div class="review-detail" v-if="currentDetail.aiReviewDetail">{{ currentDetail.aiReviewDetail }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="保函编号" v-if="currentDetail.guaranteeNo">{{ currentDetail.guaranteeNo }}</el-descriptions-item>
      </el-descriptions>
      <div class="drawer-actions" v-if="currentDetail">
        <el-button v-if="currentDetail.applyStatus === 'PENDING_PAY'" type="success" @click="doPay(currentDetail)">缴纳保函费并开立（模拟）</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Plus, Refresh } from '@element-plus/icons-vue'
import { applyGuarantee, getGuaranteePage, getGuaranteeDetail, payGuarantee, getStatusFlow } from '@/api/guarantee'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const filterStatus = ref('')
const statusFlow = ref([])
const activeStep = ref(0)

const applyDialogVisible = ref(false)
const submitting = ref(false)
const applyFormRef = ref()
const applyForm = reactive({
  landlordName: '', landlordPhone: '', landlordIdCard: '',
  houseTitle: '', province: '', city: '', district: '', address: '',
  houseType: '', area: undefined, roomCount: undefined,
  monthlyRent: undefined, depositAmount: undefined,
  rentStartDate: '', rentEndDate: '', payMethod: 'MONTHLY', contractTerms: ''
})
const applyRules = {
  landlordName: [{ required: true, message: '请输入房东姓名', trigger: 'blur' }],
  landlordPhone: [
    { required: true, message: '请输入房东电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  houseTitle: [{ required: true, message: '请输入房屋标题', trigger: 'blur' }],
  address: [{ required: true, message: '请输入房屋详细地址', trigger: 'blur' }],
  monthlyRent: [{ required: true, message: '请输入月租金', trigger: 'blur' }],
  depositAmount: [{ required: true, message: '请输入押金金额', trigger: 'blur' }],
  rentStartDate: [{ required: true, message: '请选择租期开始日', trigger: 'change' }],
  rentEndDate: [{ required: true, message: '请选择租期结束日', trigger: 'change' }]
}

const detailVisible = ref(false)
const currentDetail = ref(null)

const statusTagType = (status) => ({
  SUBMITTED: 'info', LANDLORD_CONFIRM: 'warning', PENDING_PAY: 'warning',
  APPROVED: 'success', EXPIRED: 'info'
}[status] || 'info')

const loadFlow = async () => {
  try {
    const data = await getStatusFlow()
    statusFlow.value = data.flow || []
  } catch (e) {}
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await getGuaranteePage({ pageNum: pageNum.value, pageSize, status: filterStatus.value })
    list.value = data.records || []
    total.value = data.total || 0
    if (list.value.length) {
      const idx = statusFlow.value.findIndex(s => s.status === list.value[0].applyStatus)
      activeStep.value = idx >= 0 ? idx : 0
    }
  } catch (e) {} finally {
    loading.value = false
  }
}

const submitApply = async () => {
  await applyFormRef.value.validate()
  submitting.value = true
  try {
    await applyGuarantee({ ...applyForm })
    ElMessage.success('保函申请已提交（模拟），等待房东确认')
    applyDialogVisible.value = false
    applyFormRef.value?.resetFields()
    Object.assign(applyForm, { houseType: '', area: undefined, roomCount: undefined, monthlyRent: undefined, depositAmount: undefined, rentStartDate: '', rentEndDate: '', payMethod: 'MONTHLY', contractTerms: '' })
    loadList()
  } catch (e) {} finally {
    submitting.value = false
  }
}

const showDetail = async (row) => {
  detailVisible.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await getGuaranteeDetail(row.id)
  } catch (e) {}
}

const doPay = async (row) => {
  try {
    await ElMessageBox.confirm(`确认缴纳保函费 ¥${row.guaranteeFee} 并开立电子保函？（模拟缴费，不发生真实资金往来）`, '缴费确认', { type: 'warning' })
    await payGuarantee(row.id)
    ElMessage.success('缴费成功，电子保函已开立（模拟）')
    loadList()
    if (detailVisible.value) showDetail(row)
  } catch (e) {
    if (e !== 'cancel') {}
  }
}

onMounted(() => { loadFlow(); loadList() })
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.header-actions { margin-left: auto; }
.flow-card { margin-top: 16px; }
.flow-title { font-weight: 600; margin-bottom: 12px; color: #303133; }
.list-card { margin-top: 16px; }
.card-header { display: flex; align-items: center; gap: 12px; }
.card-header > span:first-child { font-weight: 600; }
.card-header .el-radio-group { margin-left: auto; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.review-detail { margin-top: 6px; padding: 8px; background: #f5f7fa; border-radius: 4px; font-size: 12px; color: #606266; white-space: pre-wrap; }
.drawer-actions { margin-top: 20px; text-align: center; }
</style>
