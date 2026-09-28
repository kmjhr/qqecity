<template>
  <div class="loan-page">
    <ModuleHeader
      title="青创e贷"
      desc="青年创业智能授信 · A/B 双轨 · 随借随还循环贷 · 两步式受托支付"
      icon="loan"
      color="#f5576c"
      tag="演示系统 · 银行能力模拟"
      tag-type="warning"
    >
      <template #action>
        <el-button type="danger" plain @click="riskDialog = true">
          查看完整风险揭示
        </el-button>
      </template>
    </ModuleHeader>

    <!-- ================= A/B 两大分区：介绍在流程图标下，同一步骤的功能左右并排 ================= -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <!-- ========== A 区：A类循环贷（随借随还） ========== -->
      <el-tab-pane label="A类 · 循环贷（随借随还）" name="tabA">
        <el-card shadow="never" class="zone-card">
          <div class="steps-wrap" @click="onStepsClick($event, 'tabA')">
            <el-steps :active="aStep" finish-status="finish" align-center class="zone-steps zone-steps-clickable">
              <el-step title="① 提款" description="从循环额度分次提款（单笔≥¥1000）" />
              <el-step title="② 随借随还" description="按实际用款天数计息（3.85%÷365）" />
              <el-step title="③ 还款结清" description="可部分/按笔/全部结清，额度即时恢复" />
            </el-steps>
          </div>
          <div class="zone-head zone-head-a">
            <div class="zone-title">A类循环贷 · 随借随还</div>
            <div class="zone-desc">有画像即可贷：循环额度最高 ¥50,000（年化 3.85%），提款后按日计息，随借随还无违约金；还款后额度即时恢复</div>
          </div>

          <!-- 功能区1：额度与提款（左额度卡 + 右规则折叠） -->
          <div id="a-withdraw" class="zone-block">
            <div class="zone-block-title"><span class="block-no">1</span>额度与提款</div>
            <el-row :gutter="20">
              <el-col :xs="24" :md="13">
                <div v-if="aCredit" class="credit-card card-a">
                  <div class="credit-type">{{ aCredit.creditTypeName }}</div>
                  <div class="credit-total">¥{{ aCredit.totalLimit }}</div>
                  <div class="credit-row"><span>可用额度</span><b>¥{{ aCredit.availableLimit }}</b></div>
                  <div class="credit-row"><span>已用额度</span><span>¥{{ aCredit.usedLimit }}</span></div>
                  <div class="credit-row"><span>年化利率</span><span>{{ (Number(aCredit.interestRate) * 100).toFixed(2) }}%</span></div>
                  <div class="credit-row"><span>状态</span><el-tag size="small" :type="aCredit.status === 'ACTIVE' ? 'success' : 'info'">{{ aCredit.statusName }}</el-tag></div>
                  <div class="credit-row" v-if="aCredit.repayInfo && aCredit.usedLimit > 0">
                    <span>应还利息（试算）</span><span style="color:#f56c6c">¥{{ aCredit.repayInfo.interestPreview }}</span>
                  </div>
                  <div class="credit-row" v-if="aCredit.repayInfo && aCredit.usedLimit > 0">
                    <span>应还合计（本金+利息）</span><b style="color:#f56c6c">¥{{ aCredit.repayInfo.totalDue }}</b>
                  </div>
                  <div class="credit-remark">{{ aCredit.remark }}</div>
                  <div class="credit-actions" v-if="aCredit.status === 'ACTIVE'">
                    <el-button size="small" type="primary" @click="openWithdraw(aCredit)">提款</el-button>
                    <el-button size="small" @click="openTxns(aCredit)">流水</el-button>
                  </div>
                </div>
                <el-empty v-else-if="!creditLoading" description="暂无A类额度（B类观察期数据回流达标后可一键转A）" />
              </el-col>
              <el-col :xs="24" :md="11">
                <el-collapse class="rule-collapse">
                  <el-collapse-item title="A类 · 产品规则与风险提示" name="rulesA">
                    <el-descriptions :column="1" border size="small" v-if="rules.productA">
                      <el-descriptions-item label="适用对象">{{ rules.productA.target }}</el-descriptions-item>
                      <el-descriptions-item label="授信额度">{{ rules.productA.limitDesc }}</el-descriptions-item>
                      <el-descriptions-item label="年化利率">{{ rules.productA.rateDesc }}</el-descriptions-item>
                      <el-descriptions-item label="期限">{{ rules.productA.termDesc }}</el-descriptions-item>
                      <el-descriptions-item label="计息方式">{{ rules.productA.interestDesc }}</el-descriptions-item>
                      <el-descriptions-item label="还款方式">{{ rules.productA.repayDesc }}</el-descriptions-item>
                      <el-descriptions-item label="准入规则">{{ rules.productA.accessDesc }}</el-descriptions-item>
                      <el-descriptions-item label="资金流向"><span class="fund-flow">{{ rules.productA.fundFlowDesc }}</span></el-descriptions-item>
                    </el-descriptions>
                    <div class="rule-block">
                      <div class="rule-title">详细规则</div>
                      <ol class="rule-list">
                        <li v-for="(r, i) in rules.productA?.rules || []" :key="i">{{ r }}</li>
                      </ol>
                      <el-button size="small" type="primary" plain @click="openRisk(rules.productA)">查看 A类 专属风险提示</el-button>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </el-col>
            </el-row>
          </div>

          <!-- 功能区2：还款中心 -->
          <div id="a-repay" class="zone-block">
            <div class="zone-block-title"><span class="block-no">2</span>还款中心（按笔计息 · 还本付息）</div>
                        <el-card shadow="never" class="repay-panel" v-loading="repayLoading">
              <template v-if="repayPreview && repayPreview.usedLimit > 0">
                <el-row :gutter="20">
                  <!-- 左列：应还合计 + 还款规则 + 计息说明 -->
                  <el-col :xs="24" :md="9" class="repay-left">
                    <div class="bank-total">
                      <div class="bank-total-label">应还合计（元）<span class="bank-total-sub">本金 + 按笔累计利息 · 支持部分/按笔/全部结清</span></div>
                      <div class="bank-total-num">¥{{ repayPreview.totalDue }}</div>
                      <div class="bank-total-split">本金 ¥{{ repayPreview.usedLimit }} ＋ 利息 ¥{{ repayPreview.interestPreview }}</div>
                    </div>
                    <div class="repay-rules">
                      <div class="repay-rules-title">还款规则（按笔计息 · 利随本清）</div>
                      <ul class="repay-rules-list">
                        <li><b>按笔计息</b>：每笔借款独立起息，利息=剩余本金×年化÷365×天数，不同日期借款互不影响</li>
                        <li><b>先进先出</b>：部分还款自动冲最早借款；也可点「结清本笔」指定结清某一笔</li>
                        <li><b>利随本清</b>：利息随本金一并支付</li>
                        <li><b>部分还款</b>：金额可小于应还本金（≥¥0.01），无需一次结清，额度即时恢复</li>
                      </ul>
                    </div>
                    <div class="repay-remark">{{ repayPreview.remark }}</div>
                  </el-col>
                  <!-- 右列：借款明细 + 还款操作（固定表格高度 + 操作区贴底） -->
                  <el-col :xs="24" :md="15" class="repay-right">
                    <div class="loan-table-title">未结清借款明细（按笔计息 · 先进先出冲抵）</div>
                    <el-table :data="repayPreview.loans || []" size="small" :height="repayTableHeight" style="margin-bottom:8px">
                      <el-table-column label="借款日期" width="92">
                        <template #default="{ row }">{{ row.loanDate }}</template>
                      </el-table-column>
                      <el-table-column prop="loanNo" label="借款编号" min-width="120" show-overflow-tooltip />
                      <el-table-column label="剩余本金" width="80" align="right">
                        <template #default="{ row }">¥{{ row.remainingPrincipal }}</template>
                      </el-table-column>
                      <el-table-column label="天数" width="50" align="center">
                        <template #default="{ row }">{{ row.borrowDays }}天</template>
                      </el-table-column>
                      <el-table-column label="应还利息" width="74" align="right">
                        <template #default="{ row }">¥{{ row.interestPreview }}</template>
                      </el-table-column>
                      <el-table-column label="本息合计" width="82" align="right">
                        <template #default="{ row }">¥{{ row.totalDue }}</template>
                      </el-table-column>
                      <el-table-column label="操作" width="76" align="center">
                        <template #default="{ row }">
                          <el-button size="small" type="warning" plain @click="settleLoan(row)">结清本笔</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <div class="repay-ops">
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
                      <div class="repay-btns">
                        <el-button type="primary" :loading="repaySubmitting" @click="submitRepay('')">扫码支付还款（本息一并支付）</el-button>
                        <el-button type="success" :loading="repaySubmitting"
                          :disabled="!repayGuard || Number(repayGuard.balance) <= 0" @click="submitRepay('REPAY_GUARD')">
                          还款保障金一键还款<template v-if="repayGuard">（可用 ¥{{ repayGuard.balance }}，覆盖 {{ repayGuard.coverage }}% 待还）</template>
                        </el-button>
                      </div>
                    </div>
                    </div>
                  </el-col>
                </el-row>
              </template>
              <el-empty v-else description="A类当前无待还。提款后在此查看每笔借款并按笔结清（利息自动计算）" />
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ========== B 区：B类定向贷（两步式受托支付） ========== -->
      <el-tab-pane label="B类 · 定向贷（受托支付）" name="tabB">
        <el-card shadow="never" class="zone-card">
          <div class="steps-wrap" @click="onStepsClick($event, 'tabB')">
            <el-steps :active="bStep" finish-status="finish" align-center class="zone-steps zone-steps-clickable">
              <el-step title="① 预审+申请" description="免费预审（不查征信），获批才有钱可用" />
              <el-step title="② 获批额度" description="定向小额额度（¥5,000~¥20,000）" />
              <el-step title="③ 受托支付" description="银行直付商户账户，不经过个人账户" />
              <el-step title="④ 还款结清" description="按期/提前还本付息，支持保障金还款" />
            </el-steps>
          </div>
          <div class="zone-head zone-head-b">
            <div class="zone-title">B类定向贷 · 两步式受托支付</div>
            <div class="zone-desc">零历史也能贷：免费预审（不查征信）→ 审批通过 → 100%受托支付直付白名单商户，专款专用；提款后进入 6 个月观察期，经营数据回流达标可一键转A</div>
          </div>

          <!-- 功能区1：预审 + 申请（左表单 + 右结果/申请） -->
          <div id="b-apply" class="zone-block">
            <div class="zone-block-title"><span class="block-no">1</span>免费预审（不查征信）与申请</div>
            <el-row :gutter="20">
              <el-col :xs="24" :md="13">
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
              <el-col :xs="24" :md="11">
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
                  <el-table v-loading="appLoading" :data="applications" stripe empty-text="暂无贷款申请，请先提交预审" max-height="280">
                    <el-table-column prop="applyNo" label="申请编号" width="150" />
                    <el-table-column prop="loanType" label="类型" width="70" />
                    <el-table-column prop="applyAmount" label="申请金额" width="96" align="right">
                      <template #default="{ row }">¥{{ row.applyAmount || '-' }}</template>
                    </el-table-column>
                    <el-table-column prop="approveAmount" label="获批金额" width="96" align="right">
                      <template #default="{ row }"><b style="color:#67c23a">¥{{ row.approveAmount || '-' }}</b></template>
                    </el-table-column>
                    <el-table-column prop="applyStatus" label="状态" width="90" align="center">
                      <template #default="{ row }"><el-tag size="small" :type="statusTagType(row.applyStatus)">{{ row.applyStatus }}</el-tag></template>
                    </el-table-column>
                    <el-table-column prop="submitTime" label="提交时间" min-width="140" />
                  </el-table>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 功能区2：获批额度 + 观察期（左额度卡 + 右观察期/规则） -->
          <div id="b-credit" class="zone-block">
            <div class="zone-block-title"><span class="block-no">2</span>获批额度与观察期</div>
            <el-row :gutter="20">
              <el-col :xs="24" :md="12">
                <div v-if="bCredit" class="credit-card card-b">
                  <div class="credit-type">{{ bCredit.creditTypeName }}</div>
                  <div class="credit-total">¥{{ bCredit.totalLimit }}</div>
                  <div class="credit-row"><span>可用额度</span><b>¥{{ bCredit.availableLimit }}</b></div>
                  <div class="credit-row"><span>已用额度</span><span>¥{{ bCredit.usedLimit }}</span></div>
                  <div class="credit-row"><span>年化利率</span><span>{{ (Number(bCredit.interestRate) * 100).toFixed(2) }}%</span></div>
                  <div class="credit-row"><span>状态</span><el-tag size="small" :type="bCredit.status === 'ACTIVE' ? 'success' : 'info'">{{ bCredit.statusName }}</el-tag></div>
                  <div class="credit-row" v-if="bCredit.repayInfo && bCredit.usedLimit > 0">
                    <span>应还利息（试算）</span><span style="color:#f56c6c">¥{{ bCredit.repayInfo.interestPreview }}</span>
                  </div>
                  <div class="credit-row" v-if="bCredit.repayInfo && bCredit.usedLimit > 0">
                    <span>应还合计（本金+利息）</span><b style="color:#f56c6c">¥{{ bCredit.repayInfo.totalDue }}</b>
                  </div>
                  <div class="credit-remark">{{ bCredit.remark }}</div>
                  <div class="credit-actions" v-if="bCredit.status === 'ACTIVE'">
                    <el-button size="small" @click="openTxns(bCredit)">流水</el-button>
                  </div>
                </div>
                <el-empty v-else-if="!creditLoading" description="暂无B类额度（请先完成 ① 预审+申请）" />
              </el-col>
              <el-col :xs="24" :md="12">
                <el-card v-if="obsProgress && obsProgress.hasBCredit" shadow="never" class="obs-banner" v-loading="obsLoading">
                  <div class="obs-head">
                    <div class="obs-title">B转A 观察期 · 经营数据回流进度
                      <el-tag size="small" :type="obsProgress.observationStatus === 'PROMOTED' ? 'success' : 'warning'" style="margin-left:8px">
                        {{ obsProgress.observationStatusName }}
                      </el-tag>
                    </div>
                    <div class="obs-total">综合进度 <b :style="{ color: obsProgress.eligible ? '#67c23a' : '#e6a23c' }">{{ obsProgress.totalPercent }}%</b>
                      <span class="obs-sub">（流水40% + 记账30% + 现金流30%，≥60%达标）</span>
                    </div>
                  </div>
                  <el-row :gutter="12" style="margin-top:10px">
                    <el-col :span="8"><div class="obs-item"><span>受托支付回流</span><b>¥{{ obsProgress.flowAmount }}/¥{{ obsProgress.flowThreshold }}</b></div></el-col>
                    <el-col :span="8"><div class="obs-item"><span>AI记账笔数</span><b>{{ obsProgress.bookCount }}/{{ obsProgress.bookThreshold }}笔</b></div></el-col>
                    <el-col :span="8"><div class="obs-item"><span>现金流健康度</span><b>{{ obsProgress.cashScore }}分（{{ obsProgress.cashLevelName }}）</b></div></el-col>
                  </el-row>
                  <el-button v-if="obsProgress.observationStatus === 'OBSERVING' && obsProgress.eligible" type="success" size="small"
                    :loading="obsPromoting" style="margin-top:10px" @click="doApplyPromotion">一键申请转A（提额至5万）</el-button>
                  <div v-if="obsProgress.observationStatus !== 'OBSERVING'" class="obs-tip" style="margin-top:10px">{{ obsProgress.message }}</div>
                  <el-alert v-if="obsProgress.observationStatus === 'OBSERVING' && !obsProgress.eligible" type="info" :closable="false" style="margin-top:10px" :title="obsProgress.message" />
                </el-card>
                <el-collapse class="rule-collapse" style="margin-top:14px">
                  <el-collapse-item title="B类 · 产品规则与风险提示" name="rulesB">
                    <el-descriptions :column="1" border size="small" v-if="rules.productB">
                      <el-descriptions-item label="适用对象">{{ rules.productB.target }}</el-descriptions-item>
                      <el-descriptions-item label="授信额度">{{ rules.productB.limitDesc }}</el-descriptions-item>
                      <el-descriptions-item label="年化利率">{{ rules.productB.rateDesc }}</el-descriptions-item>
                      <el-descriptions-item label="期限">{{ rules.productB.termDesc }}</el-descriptions-item>
                      <el-descriptions-item label="计息方式">{{ rules.productB.interestDesc }}</el-descriptions-item>
                      <el-descriptions-item label="还款方式">{{ rules.productB.repayDesc }}</el-descriptions-item>
                      <el-descriptions-item label="准入规则">{{ rules.productB.accessDesc }}</el-descriptions-item>
                      <el-descriptions-item label="资金流向"><span class="fund-flow">{{ rules.productB.fundFlowDesc }}</span></el-descriptions-item>
                    </el-descriptions>
                    <div class="rule-block">
                      <div class="rule-title">详细规则</div>
                      <ol class="rule-list">
                        <li v-for="(r, i) in rules.productB?.rules || []" :key="i">{{ r }}</li>
                      </ol>
                      <el-button size="small" type="primary" plain @click="openRisk(rules.productB)">查看 B类 专属风险提示</el-button>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </el-col>
            </el-row>
          </div>

          <!-- 功能区3：受托支付放款（左放款 + 右商户/记录） -->
          <div id="b-entrust" class="zone-block">
            <div class="zone-block-title"><span class="block-no">3</span>受托支付放款（银行直付商户）</div>
            <el-row :gutter="20">
              <el-col :xs="24" :md="12">
                <el-alert type="warning" :closable="false" style="margin-bottom:12px"
                  title="银行不把钱打给您，而是直接打给您选择的商户（专款专用、防挪用）；您仍需按期还本付息（模拟，无真实资金）" />
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
                <el-card shadow="never" style="margin-top:4px" v-if="entrustResult">
                  <template #header><span>支付结果</span></template>
                  <el-descriptions :column="2" border size="small">
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
              </el-col>
              <el-col :xs="24" :md="12">
                <el-card shadow="never" style="margin-bottom:16px">
                  <template #header>
                    <div class="app-card-head">
                      <span>定向打款商户</span>
                      <el-button class="merchant-add-btn" @click="merchantDialog = true">
                        <el-icon><Money /></el-icon>&nbsp;添加自定义商户
                      </el-button>
                    </div>
                  </template>
                  <el-table :data="merchants" size="small" empty-text="暂无商户" max-height="180">
                    <el-table-column prop="merchantName" label="商户名称" />
                    <el-table-column label="来源" width="92" align="center">
                      <template #default="{ row }">
                        <el-tag size="small" :type="row.merchantSource === 'USER_CUSTOM' ? 'warning' : 'success'">
                          {{ merchantSourceName(row.merchantSource) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="分类" width="86">
                      <template #default="{ row }">{{ merchantTypeName(row.merchantType) }}</template>
                    </el-table-column>
                    <el-table-column prop="contactPhone" label="联系电话" width="112" />
                  </el-table>
                  <div class="merchant-tip">平台通用商户可直接打款；「我的自定义」商户仅你本人可用，且<b>每次打款前均需银行复核</b>（复核通过后直付商户）</div>
                  <el-collapse style="margin-top:10px">
                    <el-collapse-item title="我的自定义商户（申请/审核状态）" name="my">
                      <el-table :data="myMerchants" size="small" empty-text="暂无自定义商户申请" max-height="160">
                        <el-table-column prop="merchantName" label="商户名称" />
                        <el-table-column label="状态" width="88" align="center">
                          <template #default="{ row }">
                            <el-tag size="small" :type="merchantVerifyTag(row.verifyStatus)">{{ merchantVerifyName(row.verifyStatus) }}</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="reviewRemark" label="审核意见" min-width="110" show-overflow-tooltip />
                      </el-table>
                    </el-collapse-item>
                  </el-collapse>
                </el-card>
                <el-card shadow="never">
                  <template #header><span>打款记录（含复核状态）</span></template>
                  <el-table :data="entrustRecords" size="small" empty-text="暂无打款记录" max-height="180">
                    <el-table-column prop="recordNo" label="记录号" width="176" show-overflow-tooltip />
                    <el-table-column prop="merchantName" label="收款商户" min-width="110" show-overflow-tooltip />
                    <el-table-column label="状态" width="104" align="center">
                      <template #default="{ row }">
                        <el-tag size="small" :type="recordStatusTag(row)">{{ recordStatusName(row) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="amount" label="金额" width="84" align="right">
                      <template #default="{ row }">¥{{ row.amount }}</template>
                    </el-table-column>
                    <el-table-column prop="purpose" label="用途" min-width="90" show-overflow-tooltip />
                    <el-table-column label="时间" width="140">
                      <template #default="{ row }">{{ row.recordTime || '-' }}</template>
                    </el-table-column>
                  </el-table>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 功能区4：还款中心 -->
          <div id="b-repay" class="zone-block">
            <div class="zone-block-title"><span class="block-no">4</span>还款中心（按笔计息 · 还本付息）</div>
                        <el-card shadow="never" class="repay-panel" v-loading="repayLoading">
              <template v-if="repayPreview && repayPreview.usedLimit > 0">
                <el-row :gutter="20">
                  <!-- 左列：应还合计 + 还款规则 + 计息说明 -->
                  <el-col :xs="24" :md="9" class="repay-left">
                    <div class="bank-total">
                      <div class="bank-total-label">应还合计（元）<span class="bank-total-sub">本金 + 按笔累计利息 · 支持部分/按笔/全部结清</span></div>
                      <div class="bank-total-num">¥{{ repayPreview.totalDue }}</div>
                      <div class="bank-total-split">本金 ¥{{ repayPreview.usedLimit }} ＋ 利息 ¥{{ repayPreview.interestPreview }}</div>
                    </div>
                    <div class="repay-rules">
                      <div class="repay-rules-title">还款规则（按笔计息 · 利随本清）</div>
                      <ul class="repay-rules-list">
                        <li><b>按笔计息</b>：每笔受托支付独立起息，利息=剩余本金×年化÷365×天数</li>
                        <li><b>先进先出</b>：部分还款自动冲最早借款；也可点「结清本笔」指定结清某一笔</li>
                        <li><b>利随本清</b>：利息随本金一并支付</li>
                        <li><b>部分还款</b>：金额可小于应还本金（≥¥0.01），无需一次结清，额度即时恢复</li>
                      </ul>
                    </div>
                    <div class="repay-remark">{{ repayPreview.remark }}</div>
                  </el-col>
                  <!-- 右列：借款明细 + 还款操作（固定表格高度 + 操作区贴底） -->
                  <el-col :xs="24" :md="15" class="repay-right">
                    <div class="loan-table-title">未结清借款明细（按笔计息 · 先进先出冲抵）</div>
                    <el-table :data="repayPreview.loans || []" size="small" :height="repayTableHeight" style="margin-bottom:8px">
                      <el-table-column label="借款日期" width="92">
                        <template #default="{ row }">{{ row.loanDate }}</template>
                      </el-table-column>
                      <el-table-column prop="loanNo" label="借款编号" min-width="120" show-overflow-tooltip />
                      <el-table-column label="剩余本金" width="80" align="right">
                        <template #default="{ row }">¥{{ row.remainingPrincipal }}</template>
                      </el-table-column>
                      <el-table-column label="天数" width="50" align="center">
                        <template #default="{ row }">{{ row.borrowDays }}天</template>
                      </el-table-column>
                      <el-table-column label="应还利息" width="74" align="right">
                        <template #default="{ row }">¥{{ row.interestPreview }}</template>
                      </el-table-column>
                      <el-table-column label="本息合计" width="82" align="right">
                        <template #default="{ row }">¥{{ row.totalDue }}</template>
                      </el-table-column>
                      <el-table-column label="操作" width="76" align="center">
                        <template #default="{ row }">
                          <el-button size="small" type="warning" plain @click="settleLoan(row)">结清本笔</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <div class="repay-ops">
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
                      <div class="repay-btns">
                        <el-button type="primary" :loading="repaySubmitting" @click="submitRepay('')">扫码支付还款（本息一并支付）</el-button>
                        <el-button type="success" :loading="repaySubmitting"
                          :disabled="!repayGuard || Number(repayGuard.balance) <= 0" @click="submitRepay('REPAY_GUARD')">
                          还款保障金一键还款<template v-if="repayGuard">（可用 ¥{{ repayGuard.balance }}，覆盖 {{ repayGuard.coverage }}% 待还）</template>
                        </el-button>
                      </div>
                    </div>
                    </div>
                  </el-col>
                </el-row>
              </template>
              <el-empty v-else description="B类当前无待还。受托支付放款后在此查看每笔借款并按笔结清（利息自动计算）" />
            </el-card>
          </div>
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

    <!-- 提款/流水弹窗 -->
    <el-dialog v-model="creditDialog" :title="creditDialogTitle" width="640px">
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
        <el-table :data="txnList" size="small" empty-text="暂无流水" max-height="340">
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ (row.txnTime || '').replace('T', ' ') }}</template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.txnType === 'WITHDRAW' ? 'primary' : 'success'">{{ row.txnTypeName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="180">
            <template #default="{ row }">{{ row.txnDesc || row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" width="90" align="right">
            <template #default="{ row }"><b>¥{{ row.principalAmount }}</b></template>
          </el-table-column>
          <el-table-column label="利息" width="76" align="right">
            <template #default="{ row }">¥{{ row.interestAmount }}</template>
          </el-table-column>
          <el-table-column label="余额" width="88" align="right">
            <template #default="{ row }">¥{{ row.balanceAfter }}</template>
          </el-table-column>
          <el-table-column prop="txnNo" label="流水号" width="170" show-overflow-tooltip />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="creditDialog = false">关闭</el-button>
        <el-button v-if="creditDialogType !== 'txns'" type="primary" :loading="creditSubmitting"
          @click="submitCreditAction">确认</el-button>
      </template>
    </el-dialog>

    <!-- 风险揭示弹窗 -->
    <el-dialog v-model="riskDialog" title="贷款风险揭示" width="720px">
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

    <PayCashier v-model="repayCashierVisible" :order-no="repayCashierOrderNo" @paid="onRepayPaid" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getProductRules, preCheck, getCreditLimit, entrustPayment, getMerchants, applyMerchant, getMyMerchants,
  getEntrustRecords, getLoanApplications,
  withdrawCredit, repayCredit, getCreditTxns, getRepayPreview, entrustRepay, createRepayOrder,
  getObservationProgress, applyPromotion, getRepayGuard } from '@/api/loan'
import PayCashier from '@/components/PayCashier.vue'
import ModuleHeader from '@/components/ModuleHeader.vue'

const activeTab = ref('tabA')

// ---- 还款表格自适应高度：表格变短，使右列按钮底部与左列最后一行字对齐 ----
const repayTableHeight = ref(160)
async function fitRepayTable() {
  await nextTick()
  const left = document.querySelector('.repay-left')
  const ops = document.querySelector('.repay-ops')
  if (!left || !ops) return
  const h = left.offsetHeight - ops.offsetHeight - 36
  repayTableHeight.value = Math.max(48, Math.round(h))
}

// A/B 分区内各自的流程进度（步骤条高亮）
const aCredit = computed(() => creditList.value.find(x => x.creditType === 'A_TYPE') || null)
const bCredit = computed(() => creditList.value.find(x => x.creditType === 'B_TYPE') || null)
const aStep = computed(() => (aCredit.value && aCredit.value.usedLimit > 0) ? 1 : 0)
const bStep = computed(() => {
  const approved = (applications.value || []).some(a => a.applyStatus === 'APPROVED')
  const b = bCredit.value
  const paid = (entrustRecords.value || []).some(r => r.status === 'SUCCESS')
  if (paid) return 3
  if (b && b.usedLimit > 0) return 2
  if (approved) return 1
  return 0
})

// 切换分区时同步还款中心类型
watch(activeTab, (v) => {
  if (v === 'tabA') switchRepay('A_TYPE')
  else switchRepay('B_TYPE')
  setTimeout(fitRepayTable, 120)
})

// 窗口尺寸变化时重新适配还款表格高度
if (typeof window !== 'undefined') {
  window.addEventListener('resize', () => setTimeout(fitRepayTable, 80))
}

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
  // 由所在分区决定还款中心类型
  await switchRepay(activeTab.value === 'tabA' ? 'A_TYPE' : 'B_TYPE')
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
const repayGuard = ref(null)
const obsProgress = ref(null)
const obsLoading = ref(false)
const obsPromoting = ref(false)

const loadRepayGuard = async () => {
  try { repayGuard.value = await getRepayGuard().catch(() => null) } catch (e) {}
}
const loadObsProgress = async () => {
  obsLoading.value = true
  try { obsProgress.value = await getObservationProgress().catch(() => null) } catch (e) {} finally { obsLoading.value = false }
}
const doApplyPromotion = async () => {
  obsPromoting.value = true
  try {
    const vo = await applyPromotion()
    ElMessage.success('转A申请通过：已升级A类循环贷并提额至5万（模拟）')
    loadCredit()
    loadObsProgress()
    loadRepayGuard()
  } catch (e) { ElMessage.error(e?.message || '转A申请失败') } finally { obsPromoting.value = false }
}
const settleLoan = (loan) => {
  // 结清指定借款：本息合计一次付清（本金 + 该笔按天利息）
  repayLoanNo.value = loan.loanNo
  repayAmount.value = Number(loan.totalDue)
  ElMessage.info(`将结清借款 ${loan.loanNo}：本息合计 ¥${loan.totalDue}（本金 ¥${loan.remainingPrincipal} ＋ 利息 ¥${loan.interestPreview}，${loan.borrowDays} 天）`)
}
const submitRepay = async (source) => {
  if (!repayAmount.value || repayAmount.value <= 0) { ElMessage.warning('请输入还款金额'); return }
  repaySubmitting.value = true
  try {
    // 两步式还款：创建还款支付订单 → 收银台扫码支付（微信/银行）或 还款保障金一键支付（模拟）
    // 金额口径=TOTAL（本息合计）：后端按先进先出拆本金+利息，订单金额=实际本息
    const payload = { amount: repayAmount.value, amountType: 'TOTAL' }
    if (source === 'REPAY_GUARD') payload.source = 'REPAY_GUARD'
    if (repayLoanNo.value) payload.loanNo = repayLoanNo.value
    const order = await createRepayOrder(repayType.value, payload)
    if (source === 'REPAY_GUARD') {
      // 保障金一键支付：订单直接 PAID，额度已恢复
      ElMessage.success(`还款成功（还款保障金 ¥${order.amount || repayAmount.value}，额度已恢复）`)
      loadCredit()
      loadRepayPanel()
      loadRepayGuard()
      return
    }
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
const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView(true)
}

// 步骤条点击：按步骤序号定位到对应功能区
const onStepsClick = (e, zone) => {
  const step = e.target.closest('.el-step')
  if (!step) return
  const idx = Array.prototype.indexOf.call(step.parentElement.children, step)
  const map = zone === 'tabA'
    ? { 0: 'a-withdraw', 1: 'a-repay', 2: 'a-repay' }
    : { 0: 'b-apply', 1: 'b-credit', 2: 'b-entrust', 3: 'b-repay' }
  const id = map[idx]
  if (id) scrollTo(id)
}

const openTxns = async (c) => {
  currentLimit.value = c
  creditDialogType.value = 'txns'
  creditDialogTitle.value = c.creditType === 'A_TYPE' ? 'A类循环贷流水' : 'B类定向贷流水'
  creditDialog.value = true
  try { txnList.value = await getCreditTxns(c.creditType) } catch (e) { txnList.value = [] }
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

onMounted(() => { loadRules(); loadCredit(); loadMerchants(); loadApplications(); loadMyMerchants(); loadEntrustRecords(); loadObsProgress(); loadRepayGuard(); setTimeout(fitRepayTable, 300) })
</script>

<style scoped>
.zone-card { margin-bottom: 4px; }
.zone-steps-clickable :deep(.el-step__head), .zone-steps-clickable :deep(.el-step__title) { cursor: pointer; }
.zone-steps-clickable :deep(.el-step__icon) { background: #fff; border-color: #909399 !important; color: #000 !important; }
.zone-steps-clickable :deep(.el-step__icon.is-text) { background: #fff; }
.zone-steps-clickable :deep(.el-step__icon-inner) { color: #000 !important; font-weight: 600; }
.zone-steps-clickable :deep(.el-step__title) { color: #303133; font-weight: 600; }
.zone-steps-clickable :deep(.el-step__description) { color: #909399; }
.zone-steps-clickable :deep(.el-step__head.is-finish .el-step__icon), .zone-steps-clickable :deep(.el-step__head.is-process .el-step__icon) { border-color: #909399 !important; background: #fff; }
/* 连接线容器本身也统一灰色（EP 默认已完成段为主题蓝边框，全状态统一） */
.zone-steps-clickable :deep(.el-step__head .el-step__line) { border-color: #909399 !important; }
/* 连接线完全静态统一灰色（无任何交互指示；不区分完成/进行中/未完成）
   Element Plus 线填充色由 line-inner 的 border-top-color 控制（默认主题蓝）；
   去掉进度填充线，所有连接线只显示统一灰容器线 */
.zone-steps-clickable :deep(.el-step__head .el-step__line-inner) { display: none; }
.zone-steps-clickable :deep(.el-step__head .el-step__icon-inner) { color: #303133 !important; }
.rule-collapse { border: 1px solid #ebeef5; border-radius: 8px; }
.steps-wrap { cursor: pointer; }
.zone-steps-clickable :deep(.el-step) { cursor: pointer; }
.zone-block { background: #fff; border: 1px solid #ebeef5; border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0, 0, 0, .03); scroll-margin-top: 90px; }
.zone-block-title { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 700; color: #303133; margin-bottom: 16px; }
.block-no { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; background: linear-gradient(135deg, #4f6b9a, #6d8fc0); color: #fff; font-size: 13px; flex: none; }
.zone-card .zone-head { margin: 0 4px 18px; padding: 12px 16px; background: #f7f8fa; border-radius: 8px; }
.zone-head-a { border-left: 4px solid #409eff; }
.zone-head-b { border-left: 4px solid #e6a23c; }
.zone-title { font-size: 16px; font-weight: 700; color: #303133; }
.zone-desc { font-size: 13px; color: #909399; margin-top: 4px; line-height: 1.6; }
.zone-steps { margin: 10px 0 16px; }
.repay-panel :deep(.el-card__body) { padding: 18px; }
.zone-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 6px; }
.zone-head-a { border-left: 4px solid #409eff; padding-left: 10px; }
.zone-head-b { border-left: 4px solid #e6a23c; padding-left: 10px; }
.zone-title { font-size: 16px; font-weight: 700; color: #303133; }
.zone-desc { font-size: 13px; color: #909399; }
.zone-steps { margin: 14px 0 20px; }
.zone-row { margin-bottom: 20px; }
.credit-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 18px 20px; box-shadow: 0 2px 8px rgba(0,0,0,.04); }
.card-a { border-top: 3px solid #409eff; }
.card-b { border-top: 3px solid #e6a23c; }
.flow-card { margin-top: 20px; }
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
.repay-fee-tip { margin: 0; font-size: 13px; color: #606266; background: #fef0f0; border-radius: 6px; padding: 8px 10px; line-height: 1.6; }
.repay-fee-sub { display: block; font-size: 12px; color: #909399; margin-top: 2px; }
.repay-head { font-weight: 600; color: #303133; }
.repay-types { margin-bottom: 12px; width: 100%; }
.bank-total-split { font-size: 12px; color: #909399; margin-top: 4px; }
.loan-table-title { font-size: 13px; font-weight: 600; color: #606266; margin: 4px 0 8px; }
.repay-types :deep(.el-radio-button) { width: 50%; }
.repay-types :deep(.el-radio-button__inner) { width: 100%; }
.repay-form { margin-top: 4px; display: flex; flex-direction: column; gap: 10px; }
.repay-form-label { font-size: 13px; color: #909399; margin: 0; }
.repay-btns { display: flex; flex-direction: column; gap: 10px; margin-top: auto; padding-top: 14px; align-items: stretch; }
.repay-btns .el-button { width: 100%; min-width: 0; margin: 0; white-space: normal; height: 44px; font-size: 14px; font-weight: 600; padding: 0 16px; border-radius: 10px; }
.repay-btns .el-button + .el-button { margin-left: 0; margin-top: 0; }
.repay-right { display: flex; flex-direction: column; min-width: 0; height: 100%; }
.repay-left { display: flex; flex-direction: column; min-width: 0; }
.repay-left .repay-remark { margin-top: 12px; }
.repay-ops { margin-top: auto; padding-top: 4px; }
.repay-right :deep(.el-table__body-wrapper) { overflow-y: auto; }
.obs-banner { margin-bottom: 16px; border-top: 3px solid #67c23a; }
.obs-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.obs-title { font-size: 15px; font-weight: 600; color: #303133; }
.obs-total { font-size: 13px; color: #606266; }
.obs-sub { font-size: 12px; color: #909399; }
.obs-item { background: #f7f8fa; border-radius: 6px; padding: 8px 12px; font-size: 13px; color: #606266; display: flex; justify-content: space-between; }
.obs-tip { font-size: 13px; color: #67c23a; }
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
/* A/B 选项卡选中态改灰色（交互指示不要蓝色）：选中文字深灰、指示条灰、hover 灰 */
.main-tabs :deep(.el-tabs__item.is-active) { color: #303133; }
.main-tabs :deep(.el-tabs__active-bar) { background: #909399; }
.main-tabs :deep(.el-tabs__item:hover) { color: #606266; }
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

/* 添加自定义商户按钮：蓝绿渐变 + 固定白字 */
.merchant-add-btn {
  background: linear-gradient(135deg, #0ea5e9, #10b981) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  border-radius: 8px;
}
.merchant-add-btn:hover,
.merchant-add-btn:focus {
  background: linear-gradient(135deg, #0ea5e9, #10b981) !important;
  border: none !important;
  color: #fff !important;
}
</style>
