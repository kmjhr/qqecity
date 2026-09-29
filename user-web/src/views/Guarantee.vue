<template>
  <div class="guarantee-page">
    <!-- 模块头部 -->
    <ModuleHeader
      title="安居金融风控"
      desc="租房履约保函全流程 · AI合同复审 · 违约索赔"
      icon="guarantee"
      color="#764ba2"
      tag="演示系统 · 银行能力模拟"
      tag-type="warning"
    >
      <template #action>
        <el-button type="primary" @click="applyDialogVisible = true">
          <el-icon><Plus /></el-icon>申请保函
        </el-button>
      </template>
    </ModuleHeader>

    <el-tabs v-model="activeSection" class="sec-tabs">
      <!-- ============ Tab1：保函申请 ============ -->
      <el-tab-pane label="保函申请" name="apply">
    <!-- 状态流转说明 -->
    <el-card shadow="never" class="flow-card" v-if="statusFlow.length">
      <div class="flow-title">保函状态流转</div>
      <el-steps :active="activeStep" finish-status="finish" align-center>
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

      <el-table v-loading="loading" :data="list" empty-text="暂无保函申请，点击上方「申请保函」开始" stripe @row-click="onRowClick">
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
      <PayCashier v-model="cashierVisible" :order-no="cashierOrderNo" @paid="onPaid" />

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
      </el-tab-pane>

      <!-- ============ Tab2：保函房屋状态（租期前 → 租期中 → 租期后） ============ -->
      <el-tab-pane label="房屋状态" name="moveout">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>保函房屋状态 · 租期前 → 租期中 → 租期后</span>
              <el-button size="small" @click="loadMoveoutGuarantees"><el-icon><Refresh /></el-icon>刷新</el-button>
            </div>
          </template>
          <div class="sit-flow">
            <div v-for="(step, i) in situationSteps" :key="step.key" class="sit-flow-item">
              <span class="sit-flow-dot" :style="{ background: step.color }"></span>
              <div class="sit-flow-text">
                <div class="sit-flow-name">{{ step.name }}</div>
                <div class="sit-flow-desc">{{ step.desc }}</div>
              </div>
              <span v-if="i < situationSteps.length - 1" class="sit-flow-line"></span>
            </div>
          </div>
        </el-card>

        <el-card v-for="group in situationGroups" :key="group.key" shadow="never" class="list-card" style="margin-top:16px">
          <template #header>
            <div class="card-header">
              <span>{{ group.name }}
                <el-tag size="small" :type="group.tagType" style="margin-left:8px">{{ group.list.length }} 张保函</el-tag>
              </span>
              <span class="group-desc">{{ group.desc }}</span>
            </div>
          </template>
          <div v-if="!group.list.length" class="empty-tip">暂无该阶段保函</div>
          <div v-else class="sit-grid">
            <div v-for="g in group.list" :key="g.id" class="sit-card">
              <div class="sit-icon" :style="{ background: sitStyle(g).bg, color: sitStyle(g).color }">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor" aria-hidden="true">
                  <path d="M12 3 2 12h3v8h6v-6h2v6h6v-8h3L12 3z" />
                </svg>
                <span v-if="sitStyle(g).warn" class="sit-badge warn">!</span>
                <span v-else-if="sitStyle(g).wait" class="sit-badge wait">⏱</span>
                <span v-else-if="sitStyle(g).check" class="sit-badge ok">✓</span>
              </div>
              <div class="sit-info">
                <div class="sit-no">{{ g.guaranteeNo }}</div>
                <div class="sit-addr">{{ g.houseAddress || '地址待补' }}</div>
                <div class="sit-meta">
                  <span>保函额 ¥{{ g.guaranteeAmount }}</span>
                  <span v-if="g.rentStartDate && g.rentEndDate">租期 {{ g.rentStartDate }} ~ {{ g.rentEndDate }}</span>
                </div>
              </div>
              <div class="sit-tag" :style="{ color: sitStyle(g).color, background: sitStyle(g).bg }">{{ sitStyle(g).label }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="list-card moveout-card" style="margin-top:16px">
          <template #header>
            <div class="card-header">
              <span>退租留档 · 房屋照片提交</span>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
            title="结束租房（退租）时上传房屋照片留档（系统防纠纷），留档通过审核后房屋才归入租期后分区；管理端代房东确认不索赔即「已确认」（完美结束），保函到期未发起索赔自动过期（已过期），不可再索赔。" />
          <el-form label-width="100px" style="margin-bottom:16px">
            <el-form-item label="选择保函">
              <el-select v-model="moveoutForm.guaranteeId" placeholder="选择名下已开立保函" style="width:100%" @change="onMoveoutGuaranteeSelected">
                <el-option v-for="g in moveoutGuarantees" :key="g.id"
                  :label="g.guaranteeNo + '（' + (g.houseAddress || g.houseId) + ' · 保函额 ¥' + g.guaranteeAmount + ' · ' + (g.houseSituationName || g.statusName || g.guaranteeStatus) + '）'"
                  :value="g.id" />
              </el-select>
              <div class="form-hint" v-if="!moveoutGuarantees.length">暂无已开立保函，请先在「保函申请」完成申请并缴费出函</div>
            </el-form-item>
            <el-form-item label="房屋照片" required>
              <el-upload v-model:file-list="moveoutPhotoList" :auto-upload="false" :limit="6" accept="image/*"
                :on-exceed="onMoveoutExceed">
                <el-button size="small" type="primary" plain>选择房屋照片</el-button>
                <template #tip><div class="form-hint">至少 3 张房屋现状照片（客厅/卧室/厨房等），提交后照片名称留档（模拟）</div></template>
              </el-upload>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="moveoutForm.remark" type="textarea" :rows="2" placeholder="如：退租日期、交接情况（可选）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="moveoutSubmitting" @click="submitMoveoutAction">提交留档（模拟）</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="list-card" style="margin-top:16px">
          <template #header>
            <div class="card-header">
              <span>我的留档记录</span>
            </div>
          </template>
          <el-table v-loading="moveoutLoading" :data="moveoutRecords" empty-text="暂无留档记录" stripe>
            <el-table-column prop="recordNo" label="留档编号" width="160" />
            <el-table-column prop="guaranteeNo" label="保函编号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="photos" label="照片" width="90" align="center">
              <template #default="{ row }">{{ countPhotos(row.photos) }} 张</template>
            </el-table-column>
            <el-table-column label="审核结果" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.checkResult === 'PASS' ? 'success' : 'warning'" size="small">
                  {{ row.checkResult === 'PASS' ? '合格留档' : '需补拍/复核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="checkDetail" label="审核明细" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="提交时间" width="170" />
          </el-table>
          <div class="pagination" v-if="moveoutTotal > 0">
            <el-pagination background layout="prev, pager, next" :total="moveoutTotal" :page-size="moveoutPageSize"
              :current-page="moveoutPageNum" @current-change="p => { moveoutPageNum = p; loadMoveoutRecords() }" />
          </div>
        </el-card>
      </el-tab-pane>
      <!-- ============ Tab2：违约索赔（G-6） ============ -->
      <el-tab-pane label="违约索赔" name="claim">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>我的索赔</span>
              <el-button size="small" @click="loadClaims">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
            title="您作为租客可查看针对您的索赔；状态为「申辩期」时可提交反证。索赔由银行运营岗在管理端代房东发起（演示）" />
          <el-table v-loading="claimLoading" :data="claimList" empty-text="暂无索赔记录" stripe>
            <el-table-column prop="claimNo" label="索赔编号" width="150" />
            <el-table-column prop="guaranteeNo" label="保函编号" width="150" show-overflow-tooltip />
            <el-table-column label="索赔房东" width="110">
              <template #default="{ row }">{{ row.claimantName }}</template>
            </el-table-column>
            <el-table-column prop="claimAmount" label="索赔金额" width="100" align="right">
              <template #default="{ row }">¥{{ row.claimAmount }}</template>
            </el-table-column>
            <el-table-column label="AI初审" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.aiReviewResult" size="small" :type="row.aiReviewResult === 'PASS' ? 'success' : 'warning'">
                  {{ row.aiReviewResult === 'PASS' ? '通过' : '存疑' }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="statusName" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="claimTagType(row.claimStatus)" size="small">{{ row.statusName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="160" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="showClaimDetail(row)">详情</el-button>
                <el-button v-if="row.claimStatus === 'DEFENSE_PERIOD'" link type="warning" size="small" @click="openDefense(row)">申辩</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination" v-if="claimTotal > 0">
            <el-pagination background layout="prev, pager, next" :total="claimTotal" :page-size="claimPageSize"
              :current-page="claimPageNum" @current-change="p => { claimPageNum = p; loadClaims() }" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 退租留档 AI 审核结果弹窗 -->
    <el-dialog v-model="moveoutResultVisible" title="照片审核结果（模拟）" width="540px">
      <template v-if="moveoutResult">
        <el-alert :type="moveoutResult.checkResult === 'PASS' ? 'success' : 'warning'" :closable="false" show-icon
          :title="moveoutResult.checkResult === 'PASS' ? '照片合格，已留档归档' : '照片未通过合格审核，需补拍后重新提交或转人工复核'" />
        <div class="review-detail" style="margin-top:12px">{{ moveoutResult.checkDetail }}</div>
        <el-descriptions :column="1" border style="margin-top:12px">
          <el-descriptions-item label="留档编号">{{ moveoutResult.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="保函编号">{{ moveoutResult.guaranteeNo }}</el-descriptions-item>
          <el-descriptions-item label="照片">{{ countPhotos(moveoutResult.photos) }} 张</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button type="primary" @click="moveoutResultVisible = false; loadMoveoutRecords()">知道了</el-button>
      </template>
    </el-dialog>

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

    <!-- 索赔详情抽屉（双方可见） -->
    <el-drawer v-model="claimDetailVisible" title="索赔详情" size="520px">
      <template v-if="claimDetail">
        <el-descriptions :column="1" border label-width="110px">
          <el-descriptions-item label="索赔编号">{{ claimDetail.claimNo }}</el-descriptions-item>
          <el-descriptions-item label="保函编号">{{ claimDetail.guaranteeNo }}</el-descriptions-item>
          <el-descriptions-item label="房东">{{ claimDetail.claimantName }}</el-descriptions-item>
          <el-descriptions-item label="租客">{{ claimDetail.tenantName || claimDetail.tenantId }}</el-descriptions-item>
          <el-descriptions-item label="索赔金额">¥{{ claimDetail.claimAmount }}</el-descriptions-item>
          <el-descriptions-item label="索赔原因">{{ claimDetail.claimReason }}</el-descriptions-item>
          <el-descriptions-item label="证据材料" v-if="claimDetail.evidenceFiles">
            {{ (claimDetail.evidenceFiles || '').replace(/[\[\]\"]/g, '').split(',').filter(Boolean).join('、') || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="AI初审">
            <el-tag v-if="claimDetail.aiReviewResult" :type="claimDetail.aiReviewResult === 'PASS' ? 'success' : 'warning'" size="small">
              {{ claimDetail.aiReviewResult === 'PASS' ? '通过' : '存疑' }}
            </el-tag>
            <span v-else>-</span>
            <div class="review-detail" v-if="claimDetail.aiReviewDetail">{{ claimDetail.aiReviewDetail }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="claimTagType(claimDetail.claimStatus)" size="small">{{ claimDetail.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申辩内容" v-if="claimDetail.defenseContent">{{ claimDetail.defenseContent }}</el-descriptions-item>
          <el-descriptions-item label="申辩佐证" v-if="claimDetail.defenseFiles">{{ claimDetail.defenseFiles }}</el-descriptions-item>
          <el-descriptions-item label="拒绝原因" v-if="claimDetail.rejectReason">
            <span style="color:#f56c6c">{{ claimDetail.rejectReason }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="赔付金额" v-if="claimDetail.payoutAmount">¥{{ claimDetail.payoutAmount }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ claimDetail.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="结案时间" v-if="claimDetail.closeTime">{{ claimDetail.closeTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-actions" v-if="claimDetail.claimStatus === 'DEFENSE_PERIOD'">
          <el-button type="warning" @click="openDefense(claimDetail)">提交申辩（模拟）</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 租客申辩弹窗 -->
    <el-dialog v-model="defenseVisible" title="提交申辩（模拟）" width="520px">
      <el-form ref="defenseFormRef" :model="defenseForm" :rules="defenseRules" label-width="90px">
        <el-form-item label="申辩内容" prop="defenseContent">
          <el-input v-model="defenseForm.defenseContent" type="textarea" :rows="4"
            placeholder="提交反证说明，如：已提前 7 天告知退租，不构成违约" />
        </el-form-item>
        <el-form-item label="佐证材料">
          <el-upload v-model:file-list="defenseEvidenceList" :auto-upload="false" :limit="6"
            accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :on-exceed="onDefenseExceed">
            <el-button size="small" type="primary" plain>选择佐证文件</el-button>
            <template #tip><div class="form-hint">支持图片 / PDF / Word / Excel，最多 6 个；提交后文件名称留痕（模拟）</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defenseVisible = false">取消</el-button>
        <el-button type="warning" :loading="defenseSubmitting" @click="submitDefenseAction">提交申辩</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { applyGuarantee, getGuaranteePage, getGuaranteeDetail, payGuarantee, getStatusFlow,
  getMyGuarantees, submitMoveoutRecord, getMoveoutRecords } from '@/api/guarantee'
import { getClaimPage, getClaimDetail, submitDefense } from '@/api/claim'
import PayCashier from '@/components/PayCashier.vue'
import ModuleHeader from '@/components/ModuleHeader.vue'

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

// ===== 违约索赔（G-6） =====
const activeSection = ref('apply')
const claimLoading = ref(false)
const claimList = ref([])
const claimTotal = ref(0)
const claimPageNum = ref(1)
const claimPageSize = 10
const claimDetailVisible = ref(false)
const claimDetail = ref(null)
const defenseVisible = ref(false)
const defenseSubmitting = ref(false)
const defenseFormRef = ref()
const defenseForm = reactive({ defenseContent: '' })
const defenseRules = { defenseContent: [{ required: true, message: '请输入申辩内容', trigger: 'blur' }] }
const defenseEvidenceList = ref([])
const onDefenseExceed = () => ElMessage.warning('最多上传 6 个佐证文件')
const currentClaim = ref(null)

const claimTagType = (s) => ({
  SUBMITTED: 'info', AI_REVIEW: 'primary', DEFENSE_PERIOD: 'warning',
  MANUAL_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: 'info'
}[s] || 'info')

const loadClaims = async () => {
  claimLoading.value = true
  try {
    const data = await getClaimPage({ pageNum: claimPageNum.value, pageSize: claimPageSize })
    claimList.value = data.records || []
    claimTotal.value = data.total || 0
  } catch (e) {} finally {
    claimLoading.value = false
  }
}

const showClaimDetail = async (row) => {
  claimDetailVisible.value = true
  claimDetail.value = null
  try {
    claimDetail.value = await getClaimDetail(row.id)
  } catch (e) {}
}

const openDefense = (row) => {
  currentClaim.value = row
  defenseForm.defenseContent = ''
  defenseEvidenceList.value = []
  defenseVisible.value = true
}

const submitDefenseAction = async () => {
  await defenseFormRef.value.validate()
  defenseSubmitting.value = true
  try {
    const names = defenseEvidenceList.value.map(f => f.name)
    await submitDefense(currentClaim.value.id, {
      defenseContent: defenseForm.defenseContent,
      defenseFiles: names.length ? JSON.stringify(names) : undefined
    })
    ElMessage.success('申辩已提交（模拟），索赔转入人工复核')
    defenseVisible.value = false
    loadClaims()
    if (claimDetailVisible.value && claimDetail.value) showClaimDetail(claimDetail.value)
  } catch (e) {} finally {
    defenseSubmitting.value = false
  }
}

// ===== 退租留档（G-5） =====
const moveoutGuarantees = ref([])
const moveoutForm = reactive({ guaranteeId: undefined, remark: '' })
const moveoutPhotoList = ref([])
const moveoutSubmitting = ref(false)
const moveoutResultVisible = ref(false)
const moveoutResult = ref(null)
const moveoutLoading = ref(false)
const moveoutRecords = ref([])
const moveoutTotal = ref(0)
const moveoutPageNum = ref(1)
const moveoutPageSize = 10

// 租期前 → 租期中 → 租期后 流程步骤
const situationSteps = [
  { key: 'pre', name: '租期前', desc: '保函已开立 · 待入住', color: '#2563eb' },
  { key: 'renting', name: '租期中', desc: '租住进行中 · 可能出险', color: '#16a34a' },
  { key: 'ended', name: '租期后', desc: '租期结束 · 退租留档确认', color: '#6b7280' }
]

// 每个状态的图标样式（图片化状态：房子图标 + 警示/确认徽标 + 状态色）
const sitStyle = (g) => {
  switch (g.houseSituation) {
    case 'PRE_RENTAL': return { bg: '#e8f1ff', color: '#2563eb', label: '待入住' }
    case 'RENTING_NORMAL': return { bg: '#e6f7ee', color: '#16a34a', label: '正常' }
    case 'RENTING_CLAIMED': return { bg: '#fff3e0', color: '#ea580c', warn: true, label: '被索赔' }
    case 'ENDED_CLAIMED': return { bg: '#fee2e2', color: '#dc2626', warn: true, label: '被索赔' }
    case 'ENDED_PENDING_CONFIRM': return { bg: '#fef9e7', color: '#d97706', wait: true, label: '待确认' }
    case 'ENDED_CONFIRMED': return { bg: '#e6f7ee', color: '#16a34a', check: true, label: '已确认' }
    case 'ENDED_EXPIRED': return { bg: '#f3f4f6', color: '#6b7280', label: '已过期' }
    default: return { bg: '#f3f4f6', color: '#9ca3af', label: g.statusName || g.guaranteeStatus || '—' }
  }
}

// 按租期阶段分组
const situationGroups = computed(() => {
  const pre = [], renting = [], ended = []
  for (const g of moveoutGuarantees.value) {
    const st = g.houseSituation
    if (!st) continue // 无状态（如租后未提交留档）不归入分区
    if (st === 'PRE_RENTAL') pre.push(g)
    else if (st === 'RENTING_NORMAL' || st === 'RENTING_CLAIMED') renting.push(g)
    else ended.push(g)
  }
  return [
    { key: 'pre', name: '租期前', desc: '保函已开立，等待租期开始', tagType: 'primary', list: pre },
    { key: 'renting', name: '租期中', desc: '租住进行中，实时显示房屋状态', tagType: 'success', list: renting },
    { key: 'ended', name: '租期后', desc: '提交留档并通过审核后归入，房东确认即完美结束', tagType: 'info', list: ended }
  ]
})

const loadMoveoutGuarantees = async () => {
  try {
    moveoutGuarantees.value = await getMyGuarantees() || []
    if (moveoutGuarantees.value.length && !moveoutForm.guaranteeId) {
      moveoutForm.guaranteeId = moveoutGuarantees.value[0]?.id
    }
  } catch (e) {}
}

const onMoveoutGuaranteeSelected = () => {}

const onMoveoutExceed = () => ElMessage.warning('最多上传 6 张照片')

const submitMoveoutAction = async () => {
  if (!moveoutForm.guaranteeId) { ElMessage.warning('请选择保函'); return }
  if (!moveoutPhotoList.value.length) { ElMessage.warning('请上传房屋照片（至少 3 张）'); return }
  if (moveoutPhotoList.value.length < 3) { ElMessage.warning('房屋照片至少 3 张，当前 ' + moveoutPhotoList.value.length + ' 张'); return }
  moveoutSubmitting.value = true
  try {
    const photos = moveoutPhotoList.value.map(f => f.name)
    const res = await submitMoveoutRecord({
      guaranteeId: moveoutForm.guaranteeId,
      photos: JSON.stringify(photos),
      remark: moveoutForm.remark
    })
    moveoutResult.value = res
    moveoutResultVisible.value = true
    moveoutForm.remark = ''
    moveoutPhotoList.value = []
  } catch (e) {} finally {
    moveoutSubmitting.value = false
  }
}

const loadMoveoutRecords = async () => {
  moveoutLoading.value = true
  try {
    const data = await getMoveoutRecords({ pageNum: moveoutPageNum.value, pageSize: moveoutPageSize })
    moveoutRecords.value = data.records || []
    moveoutTotal.value = data.total || 0
  } catch (e) {} finally {
    moveoutLoading.value = false
  }
}

const countPhotos = (photosJson) => {
  try { return JSON.parse(photosJson || '[]').length } catch (e) { return 0 }
}

const statusTagType = (status) => ({
  SUBMITTED: 'info', LANDLORD_CONFIRM: 'warning', PENDING_PAY: 'warning',
  APPROVED: 'success', EXPIRED: 'info'
}[status] || 'info')

const loadFlow = async () => {
  try {
    const data = await getStatusFlow()
    statusFlow.value = data.flow || []
    // 状态流加载完成后重新对齐进度条（避免与列表并发时的竞态导致高亮错位）
    updateActiveStep()
  } catch (e) {}
}

const updateActiveStep = (status) => {
  if (!statusFlow.value.length) return
  // 传入 status 时按该保函状态对齐（点击行/详情时）；缺省跟随列表第一条（最新一条）
  const target = status || list.value[0]?.applyStatus
  if (!target) return
  const idx = statusFlow.value.findIndex(s => s.status === target)
  activeStep.value = idx >= 0 ? idx : 0
}

// 点击列表行：进度条高亮跟随该行状态（修复“高亮与点击状态不匹配”）
const onRowClick = (row) => {
  updateActiveStep(row?.applyStatus)
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await getGuaranteePage({ pageNum: pageNum.value, pageSize, status: filterStatus.value })
    list.value = data.records || []
    total.value = data.total || 0
    updateActiveStep()
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
  // 点击详情：进度条高亮跟随该行状态
  updateActiveStep(row?.applyStatus)
  detailVisible.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await getGuaranteeDetail(row.id)
  } catch (e) {}
}

const cashierVisible = ref(false)
const cashierOrderNo = ref('')
const doPay = async (row) => {
  try {
    // 第一步：创建保函费支付订单（待支付）
    const order = await payGuarantee(row.id)
    cashierOrderNo.value = order.orderNo
    cashierVisible.value = true
  } catch (e) {
    if (e !== 'cancel') {}
  }
}
const onPaid = () => {
  ElMessage.success('保函费支付成功，电子保函已开立（模拟）')
  loadList()
  if (detailVisible.value && currentDetail.value?.id) showDetail(currentDetail.value)
}

onMounted(() => { loadFlow(); loadList() })
// 切到违约索赔 tab 时刷新索赔列表
watch(activeSection, (v) => {
  if (v === 'claim') loadClaims()
  if (v === 'moveout') { loadMoveoutGuarantees(); loadMoveoutRecords() }
})
</script>

<style scoped>
.flow-card { margin-top: 20px; border-radius: var(--qq-radius-xl); }

/* 保函房屋状态视图 */
.sit-flow { display: flex; align-items: flex-start; gap: 0; }
.sit-flow-item { display: flex; align-items: flex-start; gap: 10px; position: relative; flex: 1; min-width: 0; }
.sit-flow-dot { width: 14px; height: 14px; border-radius: 50%; margin-top: 3px; flex: none; box-shadow: 0 0 0 4px rgba(0,0,0,.04); }
.sit-flow-text { flex: 1; min-width: 0; }
.sit-flow-name { font-weight: 600; font-size: 14px; }
.sit-flow-desc { font-size: 12px; color: var(--qq-text-secondary, #6b7280); margin-top: 2px; }
.sit-flow-line { flex: 1; height: 2px; background: #e5e7eb; margin: 8px 12px 0 4px; min-width: 24px; }
.sit-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.sit-card { display: flex; align-items: center; gap: 12px; border: 1px solid #eef0f4; border-radius: 12px; padding: 12px 14px; background: #fff; }
.sit-icon { position: relative; width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex: none; }
.sit-badge { position: absolute; top: -5px; right: -5px; width: 18px; height: 18px; border-radius: 50%; color: #fff; font-size: 12px; line-height: 18px; text-align: center; font-weight: 700; }
.sit-badge.warn { background: #dc2626; }
.sit-badge.wait { background: #d97706; font-size: 10px; line-height: 18px; }
.sit-badge.ok { background: #16a34a; }
.sit-info { flex: 1; min-width: 0; }
.sit-no { font-weight: 600; font-size: 13px; word-break: break-all; }
.sit-addr { font-size: 12px; color: var(--qq-text-secondary, #6b7280); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sit-meta { font-size: 12px; color: var(--qq-text-tertiary, #9ca3af); margin-top: 4px; display: flex; gap: 10px; flex-wrap: wrap; }
.sit-tag { flex: none; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 999px; white-space: nowrap; }
.group-desc { font-size: 12px; color: var(--qq-text-secondary, #6b7280); }
.empty-tip { color: var(--qq-text-tertiary, #9ca3af); font-size: 13px; padding: 18px 0; text-align: center; }
.flow-card :deep(.el-card__header) { padding: 18px 24px; }
.flow-card :deep(.el-card__body) { padding: 24px; }
.flow-title { font-weight: 600; margin-bottom: 12px; color: #303133; }
/* 状态流转：完全静态展示，无任何交互指示——所有节点/连接线/文字统一灰色系
   （不区分完成/进行中/未完成；状态类在 .el-step__head 上，全状态覆盖）
   Element Plus 线填充色由 line-inner 的 border-top-color 控制（默认主题蓝），
   需同时覆盖 background 与 border-top-color，加 !important 压过组件默认 */
.flow-card :deep(.el-step__head .el-step__line-inner) {
  display: none; /* 去掉进度填充线：连接线无已完成段/未完成段之分，全段同一种颜色 */
}
/* 连接线容器本身统一灰色（EP 默认已完成段为主题蓝边框，必须一并覆盖） */
.flow-card :deep(.el-step__head .el-step__line) {
  border-color: #909399 !important;
}
.flow-card :deep(.el-step__head .el-step__icon) {
  background: #fff;
  border-color: #909399 !important;
}
.flow-card :deep(.el-step__head .el-step__icon-inner) {
  color: #303133 !important;
}
.flow-card :deep(.el-step__title) { color: #303133 !important; }
.flow-card :deep(.el-step__description) { color: #909399; }
.list-card { margin-top: 20px; }
.list-card :deep(.el-card__header) { padding: 16px 24px; }

/* 退租留档表单：仅按钮文字固定白色（背景等保持原样） */
.moveout-card :deep(.el-upload button),
.moveout-card :deep(.el-button--primary) { color: #fff !important; background: linear-gradient(135deg, #0ea5e9, #10b981) !important; border: none !important; }
.card-header { display: flex; align-items: center; gap: 12px; }
.card-header > span:first-child { font-size: 16px; font-weight: 700; color: var(--qq-text); }
/* 状态筛选 radio 按钮选中态改灰色（交互指示不要蓝色） */
.card-header :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #909399;
  border-color: #909399;
  box-shadow: -1px 0 0 0 #909399;
  color: #fff;
}
.card-header :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner:hover) { color: #fff; }
.card-header > span:first-child { font-weight: 600; }
.card-header .el-radio-group { margin-left: auto; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.review-detail { margin-top: 6px; padding: 8px; background: #f5f7fa; border-radius: 4px; font-size: 12px; color: #606266; white-space: pre-wrap; }
.drawer-actions { margin-top: 20px; text-align: center; }
.sec-tabs { margin-top: 20px; }
.sec-tabs :deep(.el-tabs__nav-wrap) { padding: 0 4px; }
.form-hint { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
