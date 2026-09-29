<template>
  <div class="claim-review-page">
    <el-tabs v-model="activeTab">
      <!-- ============ Tab1：代房东发起索赔 ============ -->
      <el-tab-pane label="① 代房东发起索赔" name="submit">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>代房东发起违约索赔（管理端代理 · 模拟）</span>
              <el-tag size="small" type="warning">ADMIN / banker</el-tag>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px"
            title="系统无房东端（仅白名单可注册），由银行运营岗/管理员在管理端代理房东联系并发起索赔；提交后自动触发 AI 初审（模拟）。" />
          <el-form ref="claimFormRef" :model="claimForm" :rules="claimRules" label-width="120px">
            <el-form-item label="代房东（可选）">
              <el-select v-model="claimLandlordId" placeholder="不选则显示全部房东保函" style="width:100%" clearable filterable
                :loading="landlordLoading" @change="onLandlordChange">
                <el-option v-for="l in landlordOptions" :key="l.id"
                  :label="l.realName + '（' + (l.phone || '') + '）'"
                  :value="l.id" />
              </el-select>
              <div class="form-hint">选择后显示该房东名下全部保函（含已被索赔/已过期，灰色不可选），仅可索赔保函可提交；索赔以该房东名义提交</div>
            </el-form-item>
            <el-form-item label="选择保函" prop="guaranteeId">
              <el-select v-model="claimForm.guaranteeId" placeholder="选择可索赔保函" style="width:100%" filterable
                :loading="claimableLoading" @change="onGuaranteeSelected" :disabled="!filteredGuarantees.length">
                <el-option v-for="g in filteredGuarantees" :key="g.id"
                  :label="g.guaranteeNo + '（房东 ' + (g.landlordName || '?') + ' · 租客 ' + (g.tenantName || '?') + ' · 保函额 ¥' + g.guaranteeAmount + ' · ' + (g.claimableName || g.guaranteeStatus) + '）'"
                  :value="g.id" :disabled="g.claimable === false" />
              </el-select>
              <div class="form-hint" v-if="!filteredGuarantees.length">所选房东名下暂无保函</div>
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="索赔金额" prop="claimAmount">
                  <el-input-number v-model="claimForm.claimAmount" :min="0.01" :max="guaranteeMaxAmount || undefined"
                    :precision="2" style="width:100%" />
                  <div class="form-hint" v-if="guaranteeMaxAmount">不超过保函金额 ¥{{ guaranteeMaxAmount }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="索赔人（房东）">
                  <el-input :model-value="selectedLandlordName || '—'" disabled placeholder="保函归属房东" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="索赔原因" prop="claimReason">
              <el-input v-model="claimForm.claimReason" type="textarea" :rows="3"
                placeholder="如：租客拖欠房租 2 个月 / 提前退租 / 房屋损坏" />
            </el-form-item>
            <el-form-item label="证据材料">
              <el-upload v-model:file-list="claimEvidenceList" :auto-upload="false" :limit="6"
                accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :on-exceed="onClaimExceed">
                <el-button size="small" type="primary" plain>选择证据文件</el-button>
                <template #tip><div class="form-hint">支持图片 / PDF / Word / Excel，最多 6 个；提交后证据名称留痕（模拟）</div></template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="claimSubmitting" @click="submitClaimAction">提交索赔（模拟）</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ============ Tab2：复核队列 ============ -->
      <el-tab-pane label="② 索赔复核队列" name="review">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>索赔复核队列 - banker 人工复核</span>
              <el-tag size="small" type="warning">MANUAL_REVIEW</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="claimNo" label="索赔编号" min-width="160" />
            <el-table-column prop="guaranteeNo" label="保函编号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="claimantName" label="房东" width="100" />
            <el-table-column prop="tenantName" label="租客" width="100" />
            <el-table-column prop="claimAmount" label="索赔金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.claimAmount }}</template>
            </el-table-column>
            <el-table-column prop="claimReason" label="索赔原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link size="small" @click="handleApprove(row)">赔付</el-button>
                <el-button type="danger" link size="small" @click="handleReject(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>

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
      </el-tab-pane>

      <!-- ============ Tab3：退租留档确认 ============ -->
      <el-tab-pane label="③ 房屋状态确认" name="moveout-confirm">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>退租留档 · 代房东确认无需索赔</span>
              <el-tag size="small" type="success">留档合格 → 房东确认（管理端代）→ 完美结束</el-tag>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px"
            title="退租留档照片仅为系统防纠纷留档；租期结束后由管理端代房东确认不索赔，保函房屋状态即更新为「租后·确认无需索赔」（完美结束）。保函到期后房东未联系管理端发起索赔的，自动过期、不可再发起索赔。" />
          <el-table :data="moveoutConfirmData" v-loading="moveoutConfirmLoading" border stripe empty-text="暂无待确认留档">
            <el-table-column prop="recordNo" label="留档编号" width="160" />
            <el-table-column prop="guaranteeNo" label="保函编号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="tenantName" label="租客" width="100" />
            <el-table-column prop="houseAddress" label="房屋地址" min-width="180" show-overflow-tooltip />
            <el-table-column prop="guaranteeAmount" label="保函额" width="100" align="right">
              <template #default="{ row }">¥{{ row.guaranteeAmount }}</template>
            </el-table-column>
            <el-table-column prop="photos" label="照片" width="80" align="center">
              <template #default="{ row }">{{ countPhoto(row.photos) }} 张</template>
            </el-table-column>
            <el-table-column label="租期" width="150" align="center">
              <template #default="{ row }">{{ row.rentStartDate }} ~ {{ row.rentEndDate }}</template>
            </el-table-column>
            <el-table-column prop="checkDetail" label="留档审核明细" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="留档时间" width="170" />
            <el-table-column label="操作" width="170" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link size="small" @click="handleLandlordConfirm(row)">代房东确认无需索赔</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="mcPage.pageNum"
              v-model:page-size="mcPage.pageSize"
              :total="mcPage.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadMoveoutConfirm"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- AI 初审结果弹窗 -->
    <el-dialog v-model="aiResultVisible" title="AI 初审结果（模拟）" width="560px">
      <template v-if="aiResult">
        <el-alert :type="aiResult.aiReviewResult === 'PASS' ? 'success' : 'warning'" :closable="false" show-icon
          :title="aiResult.aiReviewResult === 'PASS' ? 'AI 初审通过 · 低风险速赔（演示自动赔付）' : 'AI 初审存疑 · 转入申辩期，等待租客反证'" />
        <div class="review-detail" v-if="aiResult.aiReviewDetail">{{ aiResult.aiReviewDetail }}</div>
        <el-descriptions :column="1" border style="margin-top:12px">
          <el-descriptions-item label="索赔编号">{{ aiResult.claimNo }}</el-descriptions-item>
          <el-descriptions-item label="索赔状态">
            <el-tag :type="claimTagType(aiResult.claimStatus)" size="small">{{ aiResult.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="赔付金额" v-if="aiResult.payoutAmount">¥{{ aiResult.payoutAmount }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button type="primary" @click="aiResultVisible = false; loadData()">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 赔付弹窗 -->
    <el-dialog v-model="approveVisible" title="赔付索赔" width="480px">
      <el-form>
        <el-form-item label="索赔编号">
          <span>{{ currentRow?.claimNo }}</span>
        </el-form-item>
        <el-form-item label="赔付金额" required>
          <el-input-number
            v-model="payoutAmount"
            :min="0"
            :precision="2"
            :step="100"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input
            v-model="reviewNote"
            type="textarea"
            :rows="2"
            placeholder="复核说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmApprove">
          确认赔付
        </el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝索赔" width="480px">
      <el-form>
        <el-form-item label="索赔编号">
          <span>{{ currentRow?.claimNo }}</span>
        </el-form-item>
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="confirmReject">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getClaimManualReviewQueue,
  reviewClaim,
  getClaimableGuarantees,
  submitClaim,
  getPendingLandlordConfirm,
  landlordConfirmMoveout
} from '@/api/business/claim'
import { getLandlordPage, type LandlordInfo } from '@/api/landlord'

const activeTab = ref('submit')

// ===== Tab3：退租留档确认 =====
const moveoutConfirmLoading = ref(false)
const moveoutConfirmData = ref<any[]>([])
const mcPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const countPhoto = (photos?: string) => {
  try { return JSON.parse(photos || '[]').length } catch { return 0 }
}

async function loadMoveoutConfirm() {
  moveoutConfirmLoading.value = true
  try {
    const data = await getPendingLandlordConfirm({ pageNum: mcPage.pageNum, pageSize: mcPage.pageSize })
    moveoutConfirmData.value = data.records || []
    mcPage.total = data.total || 0
  } finally {
    moveoutConfirmLoading.value = false
  }
}

async function handleLandlordConfirm(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认代房东「${row.landlordName || '—'}」确认该保函（${row.guaranteeNo}）租后无需索赔？确认后保函房屋状态更新为「租后·确认无需索赔」（完美结束）。`,
      '代房东确认无需索赔（模拟）',
      { type: 'warning', confirmButtonText: '确认无需索赔', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const res = await landlordConfirmMoveout(row.recordId, '代房东确认无需索赔（模拟）')
    ElMessage.success(res?.message || '已确认，保函房屋状态更新为：租后·确认无需索赔')
    loadMoveoutConfirm()
  } catch (e) {}
}

watch(activeTab, (v) => {
  if (v === 'moveout-confirm') loadMoveoutConfirm()
})

// ===== Tab1：代房东发起索赔 =====
const claimableLoading = ref(false)
const claimableGuarantees = ref<any[]>([])
const landlordLoading = ref(false)
const landlordOptions = ref<LandlordInfo[]>([])
const claimLandlordId = ref<number | undefined>(undefined)
const claimSubmitting = ref(false)
const claimFormRef = ref()
const guaranteeMaxAmount = ref(0)
const selectedLandlordName = ref('')
const aiResultVisible = ref(false)
const aiResult = ref<any>(null)
const claimEvidenceList = ref<any[]>([])

const filteredGuarantees = computed(() => {
  if (!claimLandlordId.value) return claimableGuarantees.value
  return claimableGuarantees.value.filter(g => g.landlordId === claimLandlordId.value)
})

const claimForm = reactive({
  guaranteeId: undefined as number | undefined,
  claimAmount: undefined as number | undefined,
  claimReason: ''
})
const claimRules = {
  guaranteeId: [{ required: true, message: '请选择保函', trigger: 'change' }],
  claimAmount: [{ required: true, message: '请输入索赔金额', trigger: 'blur' }],
  claimReason: [{ required: true, message: '请输入索赔原因', trigger: 'blur' }]
}

async function loadClaimable() {
  claimableLoading.value = true
  try {
    claimableGuarantees.value = await getClaimableGuarantees() || []
  } finally {
    claimableLoading.value = false
  }
}

async function loadLandlords() {
  landlordLoading.value = true
  try {
    const res = await getLandlordPage({ pageNum: 1, pageSize: 100 })
    landlordOptions.value = res.records || []
  } finally {
    landlordLoading.value = false
  }
}

function onLandlordChange() {
  claimForm.guaranteeId = undefined
  guaranteeMaxAmount.value = 0
  selectedLandlordName.value = ''
}

function onGuaranteeSelected(id: number) {
  const g = claimableGuarantees.value.find(x => x.id === id)
  guaranteeMaxAmount.value = g ? Number(g.guaranteeAmount) : 0
  selectedLandlordName.value = g?.landlordName || ''
}

function onClaimExceed() {
  ElMessage.warning('最多上传 6 个证据文件')
}

async function submitClaimAction() {
  await claimFormRef.value.validate()
  claimSubmitting.value = true
  try {
    const names = claimEvidenceList.value.map((f: any) => f.name)
    const res = await submitClaim({
      guaranteeId: claimForm.guaranteeId!,
      claimAmount: claimForm.claimAmount!,
      claimReason: claimForm.claimReason,
      evidenceFiles: JSON.stringify(names)
    })
    aiResult.value = res
    aiResultVisible.value = true
    claimForm.guaranteeId = undefined
    claimForm.claimAmount = undefined
    claimForm.claimReason = ''
    claimEvidenceList.value = []
    guaranteeMaxAmount.value = 0
    selectedLandlordName.value = ''
  } finally {
    claimSubmitting.value = false
  }
}

const claimTagType = (s: string) => ({
  SUBMITTED: 'info', AI_REVIEW: 'primary', DEFENSE_PERIOD: 'warning',
  MANUAL_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', CLOSED: 'info'
}[s] || 'info')

// ===== Tab2：复核队列 =====
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const approveVisible = ref(false)
const rejectVisible = ref(false)
const currentRow = ref<any>(null)
const payoutAmount = ref(0)
const reviewNote = ref('')
const rejectReason = ref('')

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function loadData() {
  loading.value = true
  try {
    const res = await getClaimManualReviewQueue({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function handleApprove(row: any) {
  currentRow.value = row
  payoutAmount.value = Number(row.claimAmount) || 0
  reviewNote.value = ''
  approveVisible.value = true
}

async function confirmApprove() {
  if (!currentRow.value) return
  if (!payoutAmount.value || payoutAmount.value <= 0) {
    ElMessage.warning('请填写赔付金额')
    return
  }
  submitLoading.value = true
  try {
    await reviewClaim(currentRow.value.id, {
      decision: 'APPROVED',
      payoutAmount: payoutAmount.value,
      reviewNote: reviewNote.value
    })
    ElMessage.success('已赔付，状态已同步至用户端')
    approveVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function handleReject(row: any) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  if (!currentRow.value) return
  submitLoading.value = true
  try {
    await reviewClaim(currentRow.value.id, {
      decision: 'REJECTED',
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝，状态已同步至用户端')
    rejectVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadClaimable()
  loadLandlords()
  loadData()
})
</script>

<style scoped>
.claim-review-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.review-detail {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  white-space: pre-wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
