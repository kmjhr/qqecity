<template>
  <div class="loan-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="32" color="#fff"><Money /></el-icon>
        </div>
        <div class="module-info">
          <h2>轻创业智能授信</h2>
          <p>青创e贷 A/B 双轨授信 · 随借随还循环贷 · 两步式受托支付 · 边经营边画像</p>
          <el-tag type="warning" size="small">演示系统 · 银行能力模拟</el-tag>
        </div>
        <el-button class="risk-btn" type="danger" plain @click="riskDialog = true">
          查看完整风险揭示
        </el-button>
      </div>
    </el-card>

    <!-- 办理流程：4 个 tab 的关系与顺序 -->
    <el-card shadow="never" class="flow-card">
      <div class="flow-head">
        <span class="flow-title">青创e贷办理流程</span>
        <span class="flow-hint">B类新手：① 预审 → ② 看额度 → ③ 申请获批 → ④ 受托支付放款；A类老手：直接在「我的额度」随借随还</span>
      </div>
      <el-steps :active="flowStep" align-center finish-status="success" class="flow-steps">
        <el-step v-for="(s, i) in flowSteps" :key="s.key" :title="s.title" :description="s.desc"
          :style="{ cursor: 'pointer' }" @click="activeTab = s.key" />
      </el-steps>
    </el-card>

    <!-- A/B 双轨产品介绍 -->
    <el-row :gutter="20" class="intro-row" v-loading="rulesLoading">
      <el-col :xs="24" :md="12" v-for="p in [rules.productA, rules.productB]" :key="p?.name">
        <el-card shadow="hover" class="product-card" :class="p === rules.productA ? 'card-a' : 'card-b'">
          <template #header>
            <div class="product-head">
              <span class="product-name">{{ p?.name }}</span>
              <el-tag size="small" :type="p === rules.productA ? 'primary' : 'warning'">
                {{ p === rules.productA ? 'A类 · 随借随还' : 'B类 · 受托支付' }}
              </el-tag>
            </div>
            <div class="product-slogan">{{ p?.slogan }}</div>
          </template>
          <el-descriptions :column="1" border size="small" v-if="p">
            <el-descriptions-item label="适用对象">{{ p.target }}</el-descriptions-item>
            <el-descriptions-item label="授信额度">{{ p.limitDesc }}</el-descriptions-item>
            <el-descriptions-item label="年化利率">{{ p.rateDesc }}</el-descriptions-item>
            <el-descriptions-item label="期限">{{ p.termDesc }}</el-descriptions-item>
            <el-descriptions-item label="计息方式">{{ p.interestDesc }}</el-descriptions-item>
            <el-descriptions-item label="还款方式">{{ p.repayDesc }}</el-descriptions-item>
            <el-descriptions-item label="准入规则">{{ p.accessDesc }}</el-descriptions-item>
            <el-descriptions-item label="资金流向">
              <span class="fund-flow">{{ p.fundFlowDesc }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <div class="rule-block" v-if="p">
            <div class="rule-title">详细规则</div>
            <ol class="rule-list">
              <li v-for="(r, i) in p.rules" :key="i">{{ r }}</li>
            </ol>
            <el-button size="small" type="primary" plain @click="openRisk(p)">
              查看 {{ p === rules.productA ? 'A类' : 'B类' }} 专属风险提示
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

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

      <!-- L-2 我的额度（A/B 双轨 + A类随借随还操作） -->
      <el-tab-pane label="我的额度" name="credit">
        <el-alert type="success" :closable="false" style="margin-bottom:16px"
          title="A类 = 5万循环额度可直接提款/还款（随借随还，按日计息）；B类 = 定向小额额度，须走「受托支付」放款" />
        <el-row :gutter="16">
          <el-col :span="15">
            <div class="credit-grid" v-loading="creditLoading">
              <el-card v-for="c in creditList" :key="c.id" shadow="hover" class="credit-card" :class="c.creditType">
                <div class="credit-type">{{ c.creditTypeName }}</div>
                <div class="credit-total">¥{{ c.totalLimit }}</div>
                <div class="credit-row"><span>可用额度</span><b>¥{{ c.availableLimit }}</b></div>
                <div class="credit-row"><span>已用额度</span><span>¥{{ c.usedLimit }}</span></div>
                <div class="credit-row"><span>年化利率</span><span>{{ (Number(c.interestRate) * 100).toFixed(2) }}%</span></div>
                <div class="credit-row"><span>状态</span><el-tag size="small" :type="c.status === 'ACTIVE' ? 'success' : 'info'">{{ c.statusName }}</el-tag></div>
                <div class="credit-row" v-if="c.repayInfo && c.usedLimit > 0">
                  <span>应还利息（试算）</span><span style="color:#f56c6c">¥{{ c.repayInfo.interestPreview }}</span>
                </div>
                <div class="credit-row" v-if="c.repayInfo && c.usedLimit > 0">
                  <span>应还合计（本金+利息）</span><b style="color:#f56c6c">¥{{ c.repayInfo.totalDue }}</b>
                </div>
                <div class="credit-remark">{{ c.remark }}</div>
                <div class="credit-actions" v-if="c.creditType === 'A_TYPE' && c.status === 'ACTIVE'">
                  <el-button size="small" type="primary" @click="openWithdraw(c)">提款</el-button>
                  <el-button size="small" @click="openTxns(c)">流水</el-button>
                  <el-button size="small" type="warning" plain @click="switchRepay(c.creditType)">去还款</el-button>
                </div>
                <template v-else-if="c.creditType === 'B_TYPE'">
                  <div class="credit-actions" v-if="c.status === 'ACTIVE'">
                    <el-button size="small" @click="openTxns(c)">流水</el-button>
                    <el-button size="small" type="warning" :disabled="!c.usedLimit || c.usedLimit <= 0" @click="switchRepay(c.creditType)">去还款</el-button>
                  </div>
                  <el-tag v-if="!c.usedLimit || c.usedLimit <= 0" size="small" type="warning" class="b-flow-tag">放款请到「受托支付」页（放款后可在右侧还款）</el-tag>
                </template>
              </el-card>
            </div>
            <el-empty v-if="!creditLoading && !creditList.length" description="暂无授信额度，可先进行 B类预审" />
          </el-col>
          <el-col :span="9">
            <el-card shadow="never" class="repay-panel" v-loading="repayLoading">
              <template #header><span class="repay-head">还款中心（自动试算利息）</span></template>
              <template v-if="repayPreview && repayPreview.usedLimit > 0">
                <el-radio-group v-model="repayType" size="small" class="repay-types" @change="switchRepay">
                  <el-radio-button :value="'A_TYPE'">A类循环贷</el-radio-button>
                  <el-radio-button :value="'B_TYPE'">B类定向贷</el-radio-button>
                </el-radio-group>
                <div class="bank-total">
                  <div class="bank-total-label">应还合计（元）<span class="bank-total-sub">本金+利息 · 默认全额结清 · 支持部分还款</span></div>
                  <div class="bank-total-num">¥{{ repayPreview.totalDue }}</div>
                </div>
                <div class="repay-rules">
                  <div class="repay-rules-title">还款规则（模拟，与银行贷款一致）</div>
                  <ul class="repay-rules-list">
                    <li><b>支持部分还款</b>：还款金额可小于应还合计（最低 0.01 元），无需一次结清；输入金额即按"部分还款"处理</li>
                    <li>利息按日自动计算：本次利息 = 还款本金 × 年化利率 ÷ 365 × 已用天数，试算实时更新</li>
                    <li><b>计息起始</b>：取最早一笔提款/放款日，之后再次借款<b>不重置</b>计息起始（合并计息，模拟口径）</li>
                    <li><b>还本付息</b>：还款需还清本金+利息——全额结清 = 本金 + 按日累计利息（本息合计）；部分还款自定义金额，利息按日随本金结清</li>
                    <li v-if="currentRepayRule">{{ currentRepayRule }}</li>
                    <li>还款后额度即时恢复（模拟）；A类随借随还可再次提款，无提前还款违约金</li>
                    <li>逾期处理（模拟）：超过额度有效期仍未还清，循环额度停止使用并提示结清</li>
                  </ul>
                </div>
                <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
                  <el-descriptions-item label="应还本金">¥{{ repayPreview.usedLimit }}</el-descriptions-item>
                  <el-descriptions-item label="应还利息">¥{{ repayPreview.interestPreview }}</el-descriptions-item>
                  <el-descriptions-item label="计息天数">{{ repayPreview.borrowDays }} 天</el-descriptions-item>
                  <el-descriptions-item label="年化利率">{{ repayPreview.rateText }}</el-descriptions-item>
                  <el-descriptions-item label="计息起始">{{ repayPreview.earliestDate || '-' }}</el-descriptions-item>
                </el-descriptions>
                <div class="repay-form">
                  <div class="repay-form-label">还款金额（可部分还款，最低 ¥0.01）</div>
                  <el-input-number v-model="repayAmount" :min="0.01" :max="Number(repayPreview.totalDue || 0)"
                    :precision="2" style="width:100%" />
                  <el-button size="small" type="primary" plain style="margin-top:8px;width:100%"
                    @click="setFullRepay">全额结清（快捷）</el-button>
                </div>
                <el-button type="primary" size="large" class="repay-submit" :loading="repaySubmitting" @click="submitRepay">
                  立即还款（模拟）
                </el-button>
                <div class="repay-remark">{{ repayPreview.remark }}</div>
              </template>
              <el-empty v-else description="当前无待还。A类提款或 B类受托支付放款后，可在此一键还清（自动算利息）" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 我的申请（第3步：提交/跟踪审批） -->
      <el-tab-pane label="我的申请" name="applications">
        <el-alert type="info" :closable="false" style="margin-bottom:16px"
          title="B类流程第③步：提交申请并等待审批。审批通过（APPROVED）后，才能到「受托支付」页用获批的额度放款" />
        <el-card shadow="never">
          <el-button size="small" @click="loadApplications" style="margin-bottom:12px">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
          <el-table v-loading="appLoading" :data="applications" stripe empty-text="暂无贷款申请">
            <el-table-column prop="applyNo" label="申请编号" width="160" />
            <el-table-column prop="loanType" label="类型" width="80" />
            <el-table-column prop="crowdType" label="人群" width="100" />
            <el-table-column prop="applyAmount" label="申请金额" width="110" align="right">
              <template #default="{ row }">¥{{ row.applyAmount || '-' }}</template>
            </el-table-column>
            <el-table-column prop="approveAmount" label="获批金额" width="110" align="right">
              <template #default="{ row }">
                <b style="color:#67c23a">¥{{ row.approveAmount || '-' }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="applyStatus" label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.applyStatus)">{{ row.applyStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" min-width="160" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- L-3 受托支付（第4步：审批通过后放款） -->
      <el-tab-pane label="受托支付" name="entrust">
        <el-alert type="warning" :closable="false" style="margin-bottom:16px"
          title="B类放款方式：申请审批通过后，银行不把钱打给您，而是直接打给您选择的商户（专款专用、防挪用）；您仍需按期还本付息（模拟，无真实资金）" />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never">
              <el-form ref="entrustFormRef" :model="entrustForm" :rules="entrustRules" label-width="110px">
                <el-form-item label="关联申请" prop="loanApplicationId">
                  <el-select v-model="entrustForm.loanApplicationId" placeholder="选择已获批的贷款申请" style="width:100%" filterable @change="onAppSelected">
                    <el-option v-for="a in approvedApps" :key="a.id" :label="`${a.applyNo}（获批¥${a.approveAmount || a.preCheckMaxAmount}）`" :value="a.id" />
                  </el-select>
                </el-form-item>
                <div v-if="selectedApp" class="app-amount-tip">
                  该申请可用放款额度：<b>¥{{ selectedApp.approveAmount || selectedApp.preCheckMaxAmount }}</b>
                  <span class="tip-sub">（支付金额不得超过此额度）</span>
                </div>
                <el-form-item label="收款商户" prop="merchantId">
                  <el-select v-model="entrustForm.merchantId" placeholder="选择定向商户" style="width:100%">
                    <el-option v-for="m in merchants" :key="m.id" :label="`${m.merchantName}（${merchantTypeName(m.merchantType)}）`" :value="m.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="支付金额" prop="amount">
                  <el-input-number v-model="entrustForm.amount" :min="0.01" :max="appMaxAmount" :precision="2" style="width:100%" />
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
              <template #header><span>白名单商户（定向打款目标）</span></template>
              <el-table :data="merchants" size="small" empty-text="暂无商户">
                <el-table-column prop="merchantName" label="商户名称" />
                <el-table-column label="分类" width="100">
                  <template #default="{ row }">{{ merchantTypeName(row.merchantType) }}</template>
                </el-table-column>
                <el-table-column label="认证" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.verifyStatus === 'VERIFIED' ? 'success' : 'warning'">
                      {{ row.verifyStatus === 'VERIFIED' ? '白名单' : '审核中' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="contactPhone" label="联系电话" width="120" />
              </el-table>
              <div class="merchant-tip">仅「白名单」商户可收款；灰名单（审核中）商户不可选</div>
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
    </el-tabs>

    <!-- A类随借随还：提款/还款/流水弹窗 -->
    <el-dialog v-model="creditDialog" :title="creditDialogTitle" width="560px">
      <template v-if="creditDialogType === 'withdraw'">
        <div class="dialog-tip">从 A类循环额度提款，按日计息（年化3.85%），可用额度 ¥{{ currentLimit?.availableLimit }}</div>
        <el-form label-width="90px">
          <el-form-item label="提款金额">
            <el-input-number v-model="creditAmount" :min="1000" :max="Number(currentLimit?.availableLimit || 0)"
              :step="1000" style="width:100%" />
          </el-form-item>
          <el-form-item label="计息规则">
            <span class="dialog-tip">利息 = 金额 × 3.85% ÷ 365 × 实际用款天数，随借随还无违约金（模拟）</span>
          </el-form-item>
        </el-form>
      </template>
      <template v-else>
        <el-table :data="txnList" size="small" empty-text="暂无流水" max-height="320">
          <el-table-column prop="txnNo" label="流水号" width="180" />
          <el-table-column prop="txnType" label="类型" width="90" />
          <el-table-column prop="principalAmount" label="金额" align="right" />
          <el-table-column prop="interestAmount" label="利息" align="right" />
          <el-table-column prop="txnTime" label="时间" />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="creditDialog = false">关闭</el-button>
        <el-button v-if="creditDialogType !== 'txns'" type="primary" :loading="creditSubmitting"
          @click="submitCreditAction">确认</el-button>
      </template>
    </el-dialog>

    <!-- 风险揭示弹窗 -->
    <el-dialog v-model="riskDialog" title="贷款风险揭示（与银行信贷产品一致）" width="720px">
      <div v-if="currentRisk" class="risk-panel">
        <div class="risk-panel-title">{{ currentRisk.name }}</div>
        <ul class="risk-list">
          <li v-for="(r, i) in currentRisk.risks" :key="'r' + i">{{ r }}</li>
        </ul>
      </div>
      <div class="risk-panel">
        <div class="risk-panel-title">通用风险提示</div>
        <ul class="risk-list">
          <li v-for="(r, i) in rules.generalRisks" :key="'g' + i">{{ r }}</li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, Refresh } from '@element-plus/icons-vue'
import { getProductRules, preCheck, getCreditLimit, entrustPayment, getMerchants, getLoanApplications,
  withdrawCredit, repayCredit, getCreditTxns, getRepayPreview, entrustRepay } from '@/api/loan'

const activeTab = ref('precheck')

// 办理流程步骤条（与 4 个 tab 联动）
const flowSteps = [
  { key: 'precheck', title: '① B类免费预审', desc: '不查征信测额度，B类第一步' },
  { key: 'credit', title: '② 我的额度', desc: 'A类在此随借随还；B类看定向额度' },
  { key: 'applications', title: '③ 我的申请', desc: '提交申请并跟踪审批，获批才有钱可用' },
  { key: 'entrust', title: '④ 受托支付', desc: '审批通过后：100%直付商户放款' }
]
const flowStep = computed(() => flowSteps.findIndex(s => s.key === activeTab.value))

// 商户类型映射（merchant_type → 中文）
const merchantTypeName = (t) => ({ MATERIAL: '物料采购', STALL: '摊位租赁', PROMOTION: '推广服务', OTHER: '综合服务' }[t] || t || '-')

// 申请状态标签颜色
const statusTagType = (st) => ({ APPROVED: 'success', PRE_CHECK: 'info', REJECTED: 'danger' }[st] || 'info')
const crowdOptions = [
  { label: '在校大学生', value: 'STUDENT' },
  { label: '创业者', value: 'ENTREPRENEUR' },
  { label: '退役军人', value: 'VETERAN' },
  { label: '残疾人', value: 'DISABLED' },
  { label: '农民工', value: 'FARMER' },
  { label: '失业人员', value: 'UNEMPLOYED' },
  { label: '其他', value: 'OTHER' }
]

// 产品规则（A/B 双轨介绍）
const rulesLoading = ref(false)
const rules = ref({ productA: null, productB: null, generalRisks: [] })
const riskDialog = ref(false)
const currentRisk = ref(null)

const openRisk = (p) => {
  currentRisk.value = p
  riskDialog.value = true
}

const loadRules = async () => {
  rulesLoading.value = true
  try { rules.value = await getProductRules() } catch (e) {} finally { rulesLoading.value = false }
}

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

// A类随借随还操作
const creditDialog = ref(false)
const creditDialogType = ref('withdraw')
const creditDialogTitle = ref('')
const creditSubmitting = ref(false)
const creditAmount = ref(1000)
const currentLimit = ref(null)
const txnList = ref([])

const openWithdraw = (c) => {
  currentLimit.value = c
  creditDialogType.value = 'withdraw'
  creditDialogTitle.value = 'A类提款（随借随还）'
  creditAmount.value = 1000
  creditDialog.value = true
}
// ===== 右侧还款中心（银行式：自动试算利息，默认全额） =====
const repayType = ref('A_TYPE')
const repayPreview = ref(null)
const repayAmount = ref(0)
const repaySubmitting = ref(false)
const repayLoading = ref(false)
const currentRepayRule = computed(() => {
  const rule = repayType.value === 'B_TYPE' ? rules.value.productB : rules.value.productA
  return rule ? rule.repayDesc : ''
})
const switchRepay = async (type) => {
  repayType.value = type
  repayLoading.value = true
  try {
    repayPreview.value = await getRepayPreview(type)
    repayAmount.value = (repayPreview.value && repayPreview.value.totalDue > 0)
      ? Number(repayPreview.value.totalDue) : 0
  } catch (e) {} finally { repayLoading.value = false }
}
const loadRepayPanel = async () => {
  // 默认选中有负债的额度类型；都无负债则保持 A 类
  const debt = (creditList.value || []).find(x => x.usedLimit > 0)
  await switchRepay(debt ? debt.creditType : 'A_TYPE')
}
const setFullRepay = () => {
  const full = Number(repayPreview.value?.totalDue || 0)
  if (repayAmount.value && Math.abs(repayAmount.value - full) < 0.005) {
    ElMessage.info('当前已是全额结清金额（本金+利息），可直接点击下方「立即还款」')
  } else {
    repayAmount.value = full
    ElMessage.success('已填入全额结清金额（本金 + 自动计算的利息）')
  }
}
const submitRepay = async () => {
  if (!repayAmount.value || repayAmount.value <= 0) { ElMessage.warning('请输入还款金额'); return }
  repaySubmitting.value = true
  try {
    const res = repayType.value === 'B_TYPE'
      ? await entrustRepay({ amount: repayAmount.value })
      : await repayCredit({ amount: repayAmount.value })
    ElMessage.success(`还款成功（模拟，本金¥${res.principalAmount || 0}，利息¥${res.interestAmount || 0}，额度已恢复）`)
    loadCredit()
    loadRepayPanel()
  } catch (e) {
    ElMessage.error(e?.message || '还款失败，请稍后重试')
  } finally { repaySubmitting.value = false }
}
const openTxns = async (c) => {
  currentLimit.value = c
  creditDialogType.value = 'txns'
  creditDialogTitle.value = '循环贷流水'
  creditDialog.value = true
  try { txnList.value = await getCreditTxns() } catch (e) { txnList.value = [] }
}
const submitCreditAction = async () => {
  creditSubmitting.value = true
  try {
    const amount = creditAmount.value
    if (!amount || amount <= 0) return
    const res = await withdrawCredit({ amount })
    ElMessage.success('提款成功（模拟，按日计息）')
    creditDialog.value = false
    loadCredit()
    loadRepayPanel()
  } catch (e) {} finally { creditSubmitting.value = false }
}

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
const selectedApp = ref(null)
// 已获批申请（只有 APPROVED 才能受托支付）
const approvedApps = computed(() => (applications.value || []).filter(a => a.applyStatus === 'APPROVED'))
// 当前申请可用放款额度上限
const appMaxAmount = computed(() => {
  const app = selectedApp.value
  const max = app ? Number(app.approveAmount || app.preCheckMaxAmount || 0) : 0
  return max > 0 ? max : undefined
})
const onAppSelected = (id) => {
  selectedApp.value = (applications.value || []).find(a => a.id === id) || null
  if (selectedApp.value) entrustForm.amount = undefined
}

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
  try {
    creditList.value = await getCreditLimit()
    // 并行拉 A/B 还款试算，合并到卡片（银行式：卡片直接显示应还利息/应还合计）
    const previews = await Promise.allSettled([getRepayPreview('A_TYPE'), getRepayPreview('B_TYPE')])
    const map = {}
    previews.forEach((p) => {
      if (p.status === 'fulfilled' && p.value) map[p.value.creditType] = p.value
    })
    creditList.value.forEach(c => { c.repayInfo = map[c.creditType] || null })
    loadRepayPanel()
  } catch (e) {} finally { creditLoading.value = false }
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

onMounted(() => { loadRules(); loadCredit(); loadMerchants(); loadApplications() })
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; position: relative; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.risk-btn { margin-left: auto; }
.flow-card { margin-top: 16px; }
.flow-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.flow-title { font-size: 15px; font-weight: 600; color: #303133; }
.flow-hint { font-size: 13px; color: #909399; }
.flow-steps :deep(.el-step__title) { font-size: 14px; cursor: pointer; }
.flow-steps :deep(.el-step__description) { font-size: 12px; color: #909399; }
.credit-actions { margin-top: 12px; display: flex; gap: 8px; }
.b-flow-tag { margin-top: 12px; }
.b-repay-row { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; padding-top: 12px; border-top: 1px dashed #ebeef5; font-size: 13px; color: #606266; }
.bank-total { text-align: center; background: linear-gradient(135deg, #409eff, #337ecc); color: #fff; border-radius: 10px; padding: 18px 0 14px; margin-bottom: 14px; }
.bank-total-label { font-size: 13px; opacity: .92; }
.bank-total-sub { display: block; font-size: 11px; opacity: .78; margin-top: 2px; }
.bank-total-num { font-size: 34px; font-weight: 700; margin-top: 4px; }
.quick-repay { display: flex; align-items: center; }
.repay-panel { border-top: 3px solid #409eff; }
.repay-head { font-weight: 600; color: #303133; }
.repay-types { margin-bottom: 12px; width: 100%; }
.repay-types :deep(.el-radio-button) { width: 50%; }
.repay-types :deep(.el-radio-button__inner) { width: 100%; }
.repay-form { margin-top: 4px; }
.repay-form-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.repay-submit { width: 100%; margin-top: 14px; }
.repay-remark { margin-top: 12px; padding-top: 10px; border-top: 1px dashed #ebeef5; font-size: 12px; color: #909399; line-height: 1.7; }
.repay-rules { margin: 12px 0; padding: 12px 14px; background: #f5f7fa; border-radius: 8px; }
.repay-rules-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.repay-rules-list { margin: 0; padding-left: 16px; font-size: 12px; color: #606266; line-height: 1.9; }
.dialog-tip { font-size: 13px; color: #909399; margin-bottom: 10px; }
.app-amount-tip { font-size: 13px; color: #67c23a; background: #f0f9eb; border-radius: 6px; padding: 8px 12px; margin: -8px 0 14px 110px; }
.app-amount-tip .tip-sub { color: #909399; font-size: 12px; }
.merchant-tip { margin-top: 10px; font-size: 12px; color: #e6a23c; }
.intro-row { margin-top: 16px; }
.product-card { border-top: 3px solid #409eff; }
.product-card.card-a { border-top-color: #409eff; }
.product-card.card-b { border-top-color: #e6a23c; }
.product-head { display: flex; align-items: center; justify-content: space-between; }
.product-name { font-size: 16px; font-weight: 600; color: #303133; }
.product-slogan { font-size: 13px; color: #909399; margin-top: 6px; }
.fund-flow { color: #e6a23c; font-weight: 600; }
.rule-block { margin-top: 14px; }
.rule-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.rule-list { margin: 0; padding-left: 18px; font-size: 13px; color: #606266; line-height: 1.9; }
.rule-list li::marker { color: #409eff; }
.rule-block .el-button { margin-top: 10px; }
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
.risk-panel { margin-bottom: 18px; }
.risk-panel-title { font-size: 15px; font-weight: 600; color: #f56c6c; margin-bottom: 8px; }
.risk-list { margin: 0; padding-left: 18px; font-size: 13px; color: #606266; line-height: 1.9; }
</style>
