<template>
  <div class="loan-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="32" color="#fff"><Money /></el-icon>
        </div>
        <div class="module-info">
          <h2>轻创业智能授信</h2>
          <p>青创e贷 A/B 双轨授信 · 受托支付模拟 · 随借随还</p>
          <el-tag type="warning" size="small">演示系统 · 银行能力模拟</el-tag>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="main-tabs">
      <!-- L-1 预审 -->
      <el-tab-pane label="B类免费预审" name="precheck">
        <el-alert type="info" :closable="false" style="margin-bottom:16px"
          title="B类预审不查征信，按创业计划+人群资质规则给出 1—2 万元额度区间（模拟）" />
        <el-row :gutter="20">
          <el-col :span="14">
            <el-card shadow="never">
              <el-form ref="precheckFormRef" :model="precheckForm" :rules="precheckRules" label-width="110px">
                <el-form-item label="人群资质" prop="crowdType">
                  <el-select v-model="precheckForm.crowdType" placeholder="请选择" style="width:100%">
                    <el-option v-for="c in crowdOptions" :key="c.value" :label="c.label" :value="c.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="贷款用途">
                  <el-select v-model="precheckForm.purpose" placeholder="请选择" style="width:100%">
                    <el-option label="设备采购" value="EQUIPMENT" />
                    <el-option label="原材料进货" value="MATERIAL" />
                    <el-option label="场地租金" value="RENT" />
                    <el-option label="运营周转" value="OPERATION" />
                    <el-option label="其他" value="OTHER" />
                  </el-select>
                </el-form-item>
                <el-form-item label="期望金额">
                  <el-input-number v-model="precheckForm.applyAmount" :min="1000" :max="50000" :step="1000" style="width:100%" />
                </el-form-item>
                <el-form-item label="创业计划" prop="businessPlan">
                  <el-input v-model="precheckForm.businessPlan" type="textarea" :rows="5" placeholder="简述你的创业计划、经营内容、还款来源等" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="prechecking" @click="submitPrecheck">提交预审（不查征信 · 模拟）</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never" v-loading="prechecking">
              <template #header><span>预审结果</span></template>
              <div v-if="precheckResult" class="precheck-result">
                <el-result :icon="precheckResult.preCheckResult === 'ELIGIBLE' ? 'success' : 'warning'"
                  :title="precheckResult.preCheckResultName"
                  :sub-title="`申请编号：${precheckResult.applyNo}`">
                  <template #extra>
                    <div class="amount-range">
                      <div class="amount-item">
                        <span class="label">预审额度区间</span>
                        <span class="value">¥{{ precheckResult.preCheckMinAmount }} ~ ¥{{ precheckResult.preCheckMaxAmount }}</span>
                      </div>
                      <div class="detail" v-if="precheckResult.preCheckDetail">{{ precheckResult.preCheckDetail }}</div>
                    </div>
                  </template>
                </el-result>
              </div>
              <el-empty v-else description="提交预审后查看结果" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- L-2 我的额度 -->
      <el-tab-pane label="我的额度" name="credit">
        <el-card shadow="never" v-loading="creditLoading">
          <div v-if="creditList.length" class="credit-grid">
            <el-card v-for="c in creditList" :key="c.id" shadow="hover" class="credit-card" :class="c.creditType">
              <div class="credit-type">{{ c.creditTypeName }}</div>
              <div class="credit-total">¥{{ c.totalLimit }}</div>
              <div class="credit-row"><span>可用额度</span><b>¥{{ c.availableLimit }}</b></div>
              <div class="credit-row"><span>已用额度</span><span>¥{{ c.usedLimit }}</span></div>
              <div class="credit-row"><span>年化利率</span><span>{{ c.interestRate }}%</span></div>
              <div class="credit-row"><span>状态</span><el-tag size="small" :type="c.status === 'ACTIVE' ? 'success' : 'info'">{{ c.statusName }}</el-tag></div>
              <div class="credit-remark">{{ c.remark }}</div>
            </el-card>
          </div>
          <el-empty v-else description="暂无授信额度，可先进行 B类预审" />
        </el-card>
      </el-tab-pane>

      <!-- L-3 受托支付 -->
      <el-tab-pane label="受托支付" name="entrust">
        <el-alert type="warning" :closable="false" style="margin-bottom:16px"
          title="受托支付 100% 定向打款给预置商户，资金不经过借款人个人账户（模拟，不发生真实资金往来）" />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never">
              <el-form ref="entrustFormRef" :model="entrustForm" :rules="entrustRules" label-width="110px">
                <el-form-item label="关联申请" prop="loanApplicationId">
                  <el-select v-model="entrustForm.loanApplicationId" placeholder="选择贷款申请" style="width:100%" filterable>
                    <el-option v-for="a in applications" :key="a.id" :label="`${a.applyNo}（${a.loanType || 'B类'}）`" :value="a.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="收款商户" prop="merchantId">
                  <el-select v-model="entrustForm.merchantId" placeholder="选择定向商户" style="width:100%">
                    <el-option v-for="m in merchants" :key="m.id" :label="`${m.merchantName}（${m.category || ''}）`" :value="m.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="支付金额" prop="amount">
                  <el-input-number v-model="entrustForm.amount" :min="0.01" :precision="2" style="width:100%" />
                </el-form-item>
                <el-form-item label="用途说明">
                  <el-input v-model="entrustForm.purpose" placeholder="资金用途说明" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="paying" @click="submitEntrust">确认受托支付（模拟）</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header><span>商户列表（定向打款目标）</span></template>
              <el-table :data="merchants" size="small" empty-text="暂无商户">
                <el-table-column prop="merchantName" label="商户名称" />
                <el-table-column prop="category" label="分类" width="100" />
                <el-table-column prop="contactPhone" label="联系电话" width="130" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" style="margin-top:16px" v-if="entrustResult">
          <template #header><span>支付结果</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="支付编号">{{ entrustResult.paymentNo }}</el-descriptions-item>
            <el-descriptions-item label="收款商户">{{ entrustResult.merchantName }}</el-descriptions-item>
            <el-descriptions-item label="支付金额">¥{{ entrustResult.amount }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="entrustResult.paymentStatus === 'SUCCESS' ? 'success' : 'warning'">{{ entrustResult.paymentStatusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="资金路径" :span="2">{{ entrustResult.fundPath }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <!-- 我的申请 -->
      <el-tab-pane label="我的申请" name="applications">
        <el-card shadow="never">
          <el-button size="small" @click="loadApplications" style="margin-bottom:12px">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
          <el-table v-loading="appLoading" :data="applications" stripe empty-text="暂无贷款申请">
            <el-table-column prop="applyNo" label="申请编号" width="160" />
            <el-table-column prop="loanType" label="类型" width="80" />
            <el-table-column prop="crowdType" label="人群" width="100" />
            <el-table-column prop="applyAmount" label="金额" width="110" align="right">
              <template #default="{ row }">¥{{ row.applyAmount || '-' }}</template>
            </el-table-column>
            <el-table-column prop="applyStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.applyStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" min-width="160" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, Refresh } from '@element-plus/icons-vue'
import { preCheck, getCreditLimit, entrustPayment, getMerchants, getLoanApplications } from '@/api/loan'

const activeTab = ref('precheck')
const crowdOptions = [
  { label: '在校大学生', value: 'STUDENT' },
  { label: '创业者', value: 'ENTREPRENEUR' },
  { label: '退役军人', value: 'VETERAN' },
  { label: '残疾人', value: 'DISABLED' },
  { label: '农民工', value: 'FARMER' },
  { label: '失业人员', value: 'UNEMPLOYED' },
  { label: '其他', value: 'OTHER' }
]

// 预审
const precheckFormRef = ref()
const prechecking = ref(false)
const precheckResult = ref(null)
const precheckForm = reactive({ crowdType: '', businessPlan: '', purpose: '', applyAmount: 10000 })
const precheckRules = {
  crowdType: [{ required: true, message: '请选择人群资质', trigger: 'change' }],
  businessPlan: [{ required: true, message: '请填写创业计划', trigger: 'blur' }]
}

// 额度
const creditLoading = ref(false)
const creditList = ref([])

// 受托支付
const entrustFormRef = ref()
const paying = ref(false)
const entrustResult = ref(null)
const entrustForm = reactive({ loanApplicationId: null, merchantId: null, amount: undefined, purpose: '' })
const entrustRules = {
  loanApplicationId: [{ required: true, message: '请选择贷款申请', trigger: 'change' }],
  merchantId: [{ required: true, message: '请选择收款商户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入支付金额', trigger: 'blur' }]
}
const merchants = ref([])
const applications = ref([])
const appLoading = ref(false)

const submitPrecheck = async () => {
  await precheckFormRef.value.validate()
  prechecking.value = true
  try {
    precheckResult.value = await preCheck({ ...precheckForm })
    ElMessage.success('预审完成（模拟，不查征信）')
    loadCredit()
  } catch (e) {} finally { prechecking.value = false }
}

const loadCredit = async () => {
  creditLoading.value = true
  try { creditList.value = await getCreditLimit() } catch (e) {} finally { creditLoading.value = false }
}

const loadMerchants = async () => {
  try { merchants.value = await getMerchants() } catch (e) {}
}

const loadApplications = async () => {
  appLoading.value = true
  try {
    const data = await getLoanApplications({ pageNum: 1, pageSize: 50 })
    applications.value = data.records || []
  } catch (e) {} finally { appLoading.value = false }
}

const submitEntrust = async () => {
  await entrustFormRef.value.validate()
  paying.value = true
  try {
    entrustResult.value = await entrustPayment({ ...entrustForm })
    ElMessage.success('受托支付完成（模拟）')
    loadCredit()
  } catch (e) {} finally { paying.value = false }
}

onMounted(() => { loadCredit(); loadMerchants(); loadApplications() })
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.main-tabs { margin-top: 16px; }
.amount-range { width: 100%; }
.amount-item { display: flex; justify-content: space-between; padding: 8px 16px; background: #f5f7fa; border-radius: 6px; }
.amount-item .label { color: #909399; }
.amount-item .value { font-size: 18px; font-weight: 600; color: #f5576c; }
.detail { margin-top: 10px; padding: 10px; background: #ecf5ff; border-radius: 6px; font-size: 13px; color: #409eff; white-space: pre-wrap; }
.credit-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.credit-card { border-left: 4px solid #f5576c; }
.credit-card.A_TYPE { border-left-color: #409eff; }
.credit-type { font-size: 14px; color: #909399; margin-bottom: 6px; }
.credit-total { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 12px; }
.credit-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; color: #606266; }
.credit-remark { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #ebeef5; font-size: 12px; color: #909399; }
</style>
