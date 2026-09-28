<template>
  <div class="loan-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="32" color="#fff"><Money /></el-icon>
        </div>
        <div class="module-info">
          <h2>青创e贷</h2>
          <p>青年创业智能授信 · A/B 双轨 · 随借随还循环贷 · 两步式受托支付</p>
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
        <span class="flow-hint">B类新手：① 申请办理（预审+申请）→ ② 我的额度 → ③ 受托支付放款；A类老手：直接在「我的额度」随借随还</span>
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
      <!-- L-1 申请办理（预审 + 申请合并） -->
      <el-tab-pane label="申请办理" name="apply">
        <el-alert type="info" :closable="false" style="margin-bottom:16px"
          title="① 申请办理：B类免费预审（不查征信）→ 提交申请 → 审批通过后到「受托支付」放款；A类老手可直接到「我的额度」随借随还" />
        <el-row :gutter="20">
          <el-col :xs="24" :md="14">
            <el-card shadow="never">
              <template #header><span>B类免费预审（不查征信 · 模拟）</span></template>
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
                  <el-input v-model="precheckForm.businessPlan" type="textarea" :rows="4" placeholder="简述你的创业计划、经营内容、还款来源等" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="prechecking" @click="submitPrecheck">提交预审（不查征信 · 模拟）</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-card shadow="never" v-loading="prechecking" style="margin-bottom:16px">
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
              <el-empty v-else description="提交预审后查看结果（不查征信）" :image-size="70" />
            </el-card>
            <el-card shadow="never">
              <template #header>
                <div class="app-card-head">
                  <span>我的申请（跟踪审批）</span>
                  <el-button size="small" @click="loadApplications"><el-icon><Refresh /></el-icon>刷新</el-button>
                </div>
              </template>
              <el-table v-loading="appLoading" :data="applications" stripe empty-text="暂无贷款申请，请先提交预审" max-height="300">
                <el-table-column prop="applyNo" label="申请编号" width="150" />
                <el-table-column prop="loanType" label="类型" width="70" />
                <el-table-column prop="applyAmount" label="申请金额" width="100" align="right">
                  <template #default="{ row }">¥{{ row.applyAmount || '-' }}</template>
                </el-table-column>
                <el-table-column prop="approveAmount" label="获批金额" width="100" align="right">
                  <template #default="{ row }"><b style="color:#67c23a">¥{{ row.approveAmount || '-' }}</b></template>
                </el-table-column>
                <el-table-column prop="applyStatus" label="状态" width="100" align="center">
                  <template #default="{ row }"><el-tag size="small" :type="statusTagType(row.applyStatus)">{{ row.applyStatus }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="submitTime" label="提交时间" min-width="150" />
              </el-table>
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
              <template #header><span class="repay-head">还款中心（按笔计息 · 还本付息）</span></template>
              <template v-if="repayPreview && repayPreview.usedLimit > 0">
                <el-radio-group v-model="repayType" size="small" class="repay-types" @change="switchRepay">
                  <el-radio-button :value="'A_TYPE'">A类循环贷</el-radio-button>
                  <el-radio-button :value="'B_TYPE'">B类定向贷</el-radio-button>
                </el-radio-group>
                <div class="bank-total">
                  <div class="bank-total-label">应还合计（元）<span class="bank-total-sub">本金 + 按笔累计利息 · 支持部分/按笔/全部结清</span></div>
                  <div class="bank-total-num">¥{{ repayPreview.totalDue }}</div>
                  <div class="bank-total-split">本金 ¥{{ repayPreview.usedLimit }} ＋ 利息 ¥{{ repayPreview.interestPreview }}</div>
                </div>
                <div class="loan-table-title">未结清借款明细（按笔计息 · 先进先出冲抵）</div>
                <el-table :data="repayPreview.loans || []" size="small" max-height="240" style="margin-bottom:8px">
                  <el-table-column label="借款日期" width="92">
                    <template #default="{ row }">{{ row.loanDate }}</template>
                  </el-table-column>
                  <el-table-column prop="loanNo" label="借款编号" min-width="130" show-overflow-tooltip />
                  <el-table-column label="剩余本金" width="84" align="right">
                    <template #default="{ row }">¥{{ row.remainingPrincipal }}</template>
                  </el-table-column>
                  <el-table-column label="天数" width="54" align="center">
                    <template #default="{ row }">{{ row.borrowDays }}天</template>
                  </el-table-column>
                  <el-table-column label="日利率" width="84">
                    <template #default="{ row }">{{ row.dailyRateText }}</template>
                  </el-table-column>
                  <el-table-column label="应还利息" width="78" align="right">
                    <template #default="{ row }">¥{{ row.interestPreview }}</template>
                  </el-table-column>
                  <el-table-column label="本息合计" width="86" align="right">
                    <template #default="{ row }">¥{{ row.totalDue }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="78" align="center">
                    <template #default="{ row }">
                      <el-button size="small" type="warning" plain @click="settleLoan(row)">结清本笔</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="repay-rules">
                  <div class="repay-rules-title">还款规则（按笔计息 · 利随本清）</div>
                  <ul class="repay-rules-list">
                    <li><b>按笔计息</b>：每笔借款独立起息，利息=剩余本金×年化÷365×天数，不同日期借款互不影响</li>
                    <li><b>先进先出</b>：部分还款自动冲最早借款；也可点「结清本笔」指定结清某一笔</li>
                    <li><b>利随本清</b>：利息随本金一并支付（如借款 10 天结清，利息=本金×利率÷365×10）</li>
                    <li><b>部分还款</b>：金额可小于应还本金（≥¥0.01），无需一次结清，额度即时恢复</li>
                    <li v-if="currentRepayRule">{{ currentRepayRule }}</li>
                  </ul>
                </div>
                <div class="repay-form">
                  <div class="repay-form-label" v-if="repayLoanNo">
                    本次操作：结清借款 <b style="color:#e6a23c">{{ repayLoanNo }}</b>（本息合计，一次付清）
                  </div>
                  <div class="repay-form-label" v-else>还款金额（本息合计：本金 + 利息，输入多少付多少）</div>
                  <el-input-number v-model="repayAmount" :min="0.01" :max="Number(repayPreview.totalDue || 0)"
                    :precision="2" style="width:100%" />
                  <div class="repay-fee-tip">
                    支付 <b style="color:#e6a23c">¥{{ repayTotalPreview.actual }}</b>
                    ＝ 本金 <b>¥{{ repayTotalPreview.principal }}</b> ＋ 利息 <b style="color:#f56c6c">¥{{ repayTotalPreview.interest }}</b>
                    <span class="repay-fee-sub">（输入含息金额，系统按先进先出自动拆分，利息随还）</span>
                  </div>
                  <el-button size="small" type="primary" plain style="margin-top:8px;width:100%"
                    @click="setFullRepay">全部结清（本息合计 ¥{{ repayPreview.totalDue }}）</el-button>
                </div>
                <el-button type="primary" size="large" class="repay-submit" :loading="repaySubmitting" @click="submitRepay">
                  扫码支付还款（本息一并支付）
                </el-button>
                <div class="repay-remark">{{ repayPreview.remark }}</div>
              </template>
              <el-empty v-else description="当前无待还。A类提款或 B类受托支付放款后，可在此查看每笔借款并按笔结清（利息自动计算）" />
            </el-card>
            <PayCashier v-model="repayCashierVisible" :order-no="repayCashierOrderNo" @paid="onRepayPaid" />
          </el-col>
        </el-row>
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
                    <el-option v-for="m in merchants" :key="m.id"
                      :label="`${m.merchantName}（${merchantTypeName(m.merchantType)}·${merchantSourceName(m.merchantSource)}）`" :value="m.id" />
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
              <template #header>
                <div class="app-card-head">
                  <span>定向打款商户</span>
                  <el-button size="small" type="primary" plain @click="merchantDialog = true">
                    <el-icon><Money /></el-icon>&nbsp;添加自定义商户
                  </el-button>
                </div>
              </template>
              <el-table :data="merchants" size="small" empty-text="暂无商户" max-height="220">
                <el-table-column prop="merchantName" label="商户名称" />
                <el-table-column label="来源" width="96" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.merchantSource === 'USER_CUSTOM' ? 'warning' : 'success'">
                      {{ merchantSourceName(row.merchantSource) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="分类" width="90">
                  <template #default="{ row }">{{ merchantTypeName(row.merchantType) }}</template>
                </el-table-column>
                <el-table-column prop="contactPhone" label="联系电话" width="118" />
              </el-table>
              <div class="merchant-tip">平台通用商户可直接打款；「我的自定义」商户仅你本人可用，且<b>每次打款前均需银行复核</b>（复核通过后直付商户）</div>

              <el-collapse style="margin-top:10px">
                <el-collapse-item title="我的自定义商户（申请/审核状态）" name="my">
                  <el-table :data="myMerchants" size="small" empty-text="暂无自定义商户申请" max-height="200">
                    <el-table-column prop="merchantName" label="商户名称" />
                    <el-table-column label="状态" width="90" align="center">
                      <template #default="{ row }">
                        <el-tag size="small" :type="merchantVerifyTag(row.verifyStatus)">{{ merchantVerifyName(row.verifyStatus) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="reviewRemark" label="审核意见" min-width="120" show-overflow-tooltip />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </el-card>

            <el-card shadow="never" style="margin-top:16px">
              <template #header><span>打款记录（含复核状态）</span></template>
              <el-table :data="entrustRecords" size="small" empty-text="暂无打款记录" max-height="220">
                <el-table-column prop="recordNo" label="记录号" width="180" show-overflow-tooltip />
                <el-table-column prop="merchantName" label="收款商户" min-width="120" show-overflow-tooltip />
                <el-table-column label="状态" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="recordStatusTag(row)">{{ recordStatusName(row) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="amount" label="金额" width="90" align="right">
                  <template #default="{ row }">¥{{ row.amount }}</template>
                </el-table-column>
                <el-table-column prop="purpose" label="用途" min-width="100" show-overflow-tooltip />
                <el-table-column label="时间" width="150">
                  <template #default="{ row }">{{ row.recordTime || '-' }}</template>
                </el-table-column>
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
            <el-descriptions-item v-if="entrustResult.reviewNo" label="复核单号">{{ entrustResult.reviewNo }}</el-descriptions-item>
            <el-descriptions-item label="资金路径" :span="2">{{ entrustResult.fundPath }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加自定义商户弹窗（管理端 banker 审核，仅本人可用，每单复核） -->
    <el-dialog v-model="merchantDialog" title="添加自定义商户（需管理端审核）" width="560px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="提交后进入灰名单，由管理端 banker 审核；通过后仅你本人可用于受托支付，且每次打款前仍会复核【模拟】" />
      <el-form label-width="96px">
        <el-form-item label="商户名称" required>
          <el-input v-model="merchantForm.merchantName" placeholder="如：城西五金建材店" />
        </el-form-item>
        <el-form-item label="商户分类">
          <el-select v-model="merchantForm.merchantType" style="width:100%">
            <el-option v-for="o in merchantTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="merchantForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="merchantForm.contactPhone" />
        </el-form-item>
        <el-form-item label="营业执照号">
          <el-input v-model="merchantForm.businessLicense" />
        </el-form-item>
        <el-form-item label="收款账号" required>
          <el-input v-model="merchantForm.bankAccount" placeholder="对公/对私收款账户" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="merchantForm.bankName" placeholder="如：中国工商银行广州分行" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="merchantForm.applyRemark" type="textarea" :rows="2" placeholder="说明与该商户的交易背景、用途" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="merchantSubmitting" @click="submitMerchantApply">提交审核</el-button>
          <el-button @click="merchantDialog = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

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
import { getProductRules, preCheck, getCreditLimit, entrustPayment, getMerchants, applyMerchant, getMyMerchants,
  getEntrustRecords, getLoanApplications,
  withdrawCredit, repayCredit, getCreditTxns, getRepayPreview, entrustRepay, createRepayOrder } from '@/api/loan'
import PayCashier from '@/components/PayCashier.vue'

const activeTab = ref('apply')

// 办理流程步骤条（与 3 个 tab 联动）
const flowSteps = [
  { key: 'apply', title: '① 申请办理', desc: 'B类免费预审 + 提交申请，获批才有钱可用' },
  { key: 'credit', title: '② 我的额度', desc: 'A类随借随还；B类看定向额度' },
  { key: 'entrust', title: '③ 受托支付', desc: '审批通过后：100%直付商户放款' }
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
// 本息合计 → 本金+利息 拆分试算（先进先出，与后端 splitRepay 同规则）
const splitTotal = (amount, loans) => {
  let remain = Math.round(Number(amount) * 100) / 100
  let principal = 0
  let interest = 0
  for (const l of (loans || [])) {
    if (remain <= 0) break
    const rem = Number(l.remainingPrincipal)
    const days = l.borrowDays
    const daily = Number(l.rate) / 365
    const settle = Math.round(rem * (1 + daily * days) * 100) / 100
    if (remain + 1e-6 >= settle) {
      principal += rem
      interest += Number(l.interestPreview || 0)
      remain = Math.round((remain - settle) * 100) / 100
    } else {
      const x = Math.round(remain / (1 + daily * days) * 100) / 100
      if (x >= 0.01) {
        principal += x
        interest += Math.round(x * daily * days * 100) / 100
      }
      remain = 0
      break
    }
  }
  principal = Math.round(principal * 100) / 100
  interest = Math.round(interest * 100) / 100
  return { principal, interest, actual: Math.round((principal + interest) * 100) / 100 }
}
const repayTotalPreview = computed(() => {
  if (!repayAmount.value || repayAmount.value <= 0) return { principal: 0, interest: 0, actual: 0 }
  const loans = (repayPreview.value && repayPreview.value.loans) || []
  if (repayLoanNo.value) {
    const t = loans.find(l => l.loanNo === repayLoanNo.value)
    if (t) return { principal: Number(t.remainingPrincipal), interest: Number(t.interestPreview || 0), actual: Number(t.totalDue) }
  }
  return splitTotal(repayAmount.value, loans)
})
const repaySubmitting = ref(false)
const repayLoading = ref(false)
const currentRepayRule = computed(() => {
  const rule = repayType.value === 'B_TYPE' ? rules.value.productB : rules.value.productA
  return rule ? rule.repayDesc : ''
})
const switchRepay = async (type) => {
  repayType.value = type
  repayLoanNo.value = ''
  repayLoading.value = true
  try {
    repayPreview.value = await getRepayPreview(type)
    repayAmount.value = (repayPreview.value && repayPreview.value.usedLimit > 0)
      ? Number(repayPreview.value.totalDue) : 0
  } catch (e) {} finally { repayLoading.value = false }
}
const loadRepayPanel = async () => {
  // 默认选中有负债的额度类型；都无负债则保持 A 类
  const debt = (creditList.value || []).find(x => x.usedLimit > 0)
  await switchRepay(debt ? debt.creditType : 'A_TYPE')
}
const setFullRepay = () => {
  // 全部结清：金额 = 待还本息合计（本金 + 按笔利息），一次付清
  const full = Number(repayPreview.value?.totalDue || 0)
  repayLoanNo.value = ''
  if (repayAmount.value && Math.abs(repayAmount.value - full) < 0.005) {
    ElMessage.info('当前已是全部结清金额（本息合计），可直接扫码支付')
  } else {
    repayAmount.value = full
    ElMessage.success('已填入全部结清本息 ¥' + full + '（本金 ¥' + (repayPreview.value?.principalTotal ?? 0)
      + ' ＋ 利息 ¥' + (repayPreview.value?.interestPreview ?? 0) + '）')
  }
}
const repayCashierVisible = ref(false)
const repayCashierOrderNo = ref('')
const repayLoanNo = ref('')
const settleLoan = (loan) => {
  // 结清指定借款：本息合计一次付清（本金 + 该笔按天利息）
  repayLoanNo.value = loan.loanNo
  repayAmount.value = Number(loan.totalDue)
  ElMessage.info(`将结清借款 ${loan.loanNo}：本息合计 ¥${loan.totalDue}（本金 ¥${loan.remainingPrincipal} ＋ 利息 ¥${loan.interestPreview}，${loan.borrowDays} 天）`)
}
const submitRepay = async () => {
  if (!repayAmount.value || repayAmount.value <= 0) { ElMessage.warning('请输入还款金额'); return }
  repaySubmitting.value = true
  try {
    // 两步式还款：创建还款支付订单 → 收银台扫码支付（微信/银行）
    // 金额口径=TOTAL（本息合计）：后端按先进先出拆本金+利息，订单金额=实际本息
    const payload = { amount: repayAmount.value, amountType: 'TOTAL' }
    if (repayLoanNo.value) payload.loanNo = repayLoanNo.value
    const order = await createRepayOrder(repayType.value, payload)
    repayCashierOrderNo.value = order.orderNo
    repayCashierVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '创建还款订单失败，请稍后重试')
  } finally { repaySubmitting.value = false }
}
const onRepayPaid = () => {
  ElMessage.success('还款成功（模拟扫码支付，额度已恢复）')
  loadCredit()
  loadRepayPanel()
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
    ElMessage.success('提款成功（模拟），贷款资金已放款到账（演示口径：不经平台钱包），按日计息')
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
const myMerchants = ref([])
const entrustRecords = ref([])
const merchantDialog = ref(false)
const merchantSubmitting = ref(false)
const merchantForm = reactive({
  merchantName: '', merchantType: 'MATERIAL', contactName: '', contactPhone: '',
  businessLicense: '', bankAccount: '', bankName: '', applyRemark: ''
})
const merchantTypeOptions = [
  { value: 'MATERIAL', label: '物料采购' },
  { value: 'STALL', label: '摊位租赁' },
  { value: 'PROMOTION', label: '推广服务' },
  { value: 'OTHER', label: '综合服务' }
]
const merchantSourceName = (src) => (src === 'USER_CUSTOM' ? '我的自定义' : '平台通用')
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

const loadMyMerchants = async () => {
  try { myMerchants.value = await getMyMerchants() } catch (e) {}
}

const loadEntrustRecords = async () => {
  try { entrustRecords.value = await getEntrustRecords() } catch (e) {}
}

const submitMerchantApply = async () => {
  if (!merchantForm.merchantName) { ElMessage.warning('请填写商户名称'); return }
  if (!merchantForm.bankAccount) { ElMessage.warning('请填写收款账号'); return }
  merchantSubmitting.value = true
  try {
    await applyMerchant({ ...merchantForm })
    ElMessage.success('已提交，进入灰名单待管理端 banker 审核；通过后仅你本人可用，且每次打款前仍会复核')
    merchantDialog.value = false
    merchantForm.merchantName = ''; merchantForm.contactName = ''; merchantForm.contactPhone = ''
    merchantForm.businessLicense = ''; merchantForm.bankAccount = ''; merchantForm.bankName = ''; merchantForm.applyRemark = ''
    loadMyMerchants()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally { merchantSubmitting.value = false }
}

const merchantVerifyName = (v) => ({ PENDING: '审核中', VERIFIED: '已通过', REJECTED: '已拒绝' }[v] || v)
const merchantVerifyTag = (v) => ({ PENDING: 'warning', VERIFIED: 'success', REJECTED: 'danger' }[v] || 'info')
const recordStatusName = (r) => r.statusName || r.status
const recordStatusTag = (r) => ({ SUCCESS: 'success', PENDING_REVIEW: 'warning', REJECTED: 'danger' }[r.status] || 'info')

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
    if (entrustResult.value.pendingReview) {
      ElMessage.warning('已提交银行复核（自定义商户每单复核）：复核通过后 100% 直付商户账户，请关注打款记录')
    } else {
      ElMessage.success('受托支付完成（模拟），资金已直付商户账户（不经过个人账户）')
    }
    loadCredit()
    loadEntrustRecords()
  } catch (e) {
    ElMessage.error(e?.message || '受托支付失败')
  } finally { paying.value = false }
}

onMounted(() => { loadRules(); loadCredit(); loadMerchants(); loadApplications(); loadMyMerchants(); loadEntrustRecords() })
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
.app-card-head { display: flex; justify-content: space-between; align-items: center; }
.repay-fee-tip { margin-top: 8px; font-size: 13px; color: #606266; background: #fef0f0; border-radius: 6px; padding: 6px 10px; }
.repay-fee-sub { font-size: 12px; color: #909399; }
.repay-head { font-weight: 600; color: #303133; }
.repay-types { margin-bottom: 12px; width: 100%; }
.bank-total-split { font-size: 12px; color: #909399; margin-top: 4px; }
.loan-table-title { font-size: 13px; font-weight: 600; color: #606266; margin: 4px 0 8px; }
.repay-types :deep(.el-radio-button) { width: 50%; }
.repay-types :deep(.el-radio-button__inner) { width: 100%; }
.repay-form { margin-top: 4px; }
.repay-form-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.repay-submit { width: 100%; margin-top: 14px; }
.repay-remark { margin-top: 12px; padding-top: 10px; border-top: 1px dashed #ebeef5; font-size: 12px; color: #909399; line-height: 1.7; }
.repay-rules { margin: 8px 0; padding: 8px 12px; background: #f5f7fa; border-radius: 8px; }
.repay-rules-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.repay-rules-list { margin: 0; padding-left: 16px; font-size: 12px; color: #606266; line-height: 1.6; }
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
