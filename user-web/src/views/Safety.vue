<template>
  <div class="safety-page">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
          <el-icon :size="32" color="#fff"><Warning /></el-icon>
        </div>
        <div class="module-info">
          <h2>数字安全守护</h2>
          <p>反诈知识库 · 智能骗局甄别</p>
          <el-tag type="warning" size="small">演示系统 · 模拟数据</el-tag>
        </div>
        <div class="header-actions">
          <el-button type="danger" @click="detectActive = true">
            <el-icon><Search /></el-icon>骗局甄别
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="15">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>反诈知识列表</span>
              <el-select v-model="filterType" placeholder="分类" clearable style="width:140px" @change="loadList">
                <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
          </template>
          <el-table :data="list" v-loading="loading" stripe @row-click="viewDetail" highlight-current-row empty-text="暂无反诈内容">
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fraudType" label="类型" width="110">
              <template #default="{ row }"><el-tag size="small">{{ row.fraudType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.riskLevel === 'HIGH' ? 'danger' : row.riskLevel === 'MEDIUM' ? 'warning' : 'info'">
                  {{ row.riskLevel === 'HIGH' ? '高' : row.riskLevel === 'MEDIUM' ? '中' : '低' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="80" align="center">
              <template #default><el-button type="primary" link>查看</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card shadow="never" v-if="!detail" style="min-height:420px">
          <template #header><span>详情</span></template>
          <el-empty description="点击左侧列表查看详情" />
        </el-card>
        <el-card shadow="never" v-else class="detail-card" style="min-height:420px">
          <template #header>
            <div class="card-header">
              <span>{{ detail.title }}</span>
              <el-tag :type="detail.riskLevel === 'HIGH' ? 'danger' : detail.riskLevel === 'MEDIUM' ? 'warning' : 'info'">
                {{ detail.fraudType }}
              </el-tag>
            </div>
          </template>
          <div class="detail-body">
            <div class="detail-section">
              <div class="s-label">常见话术</div>
              <div class="s-box warn">{{ detail.commonWords }}</div>
            </div>
            <div class="detail-section">
              <div class="s-label">诈骗手法</div>
              <div class="s-box">{{ detail.modusOperandi }}</div>
            </div>
            <div class="detail-section">
              <div class="s-label">防范建议</div>
              <div class="s-box ok">{{ detail.prevention }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 骗局甄别 -->
    <el-drawer v-model="detectActive" title="智能骗局甄别（模拟）" size="520px">
      <el-alert type="info" :closable="false" style="margin-bottom:16px">
        将文本粘贴到下方，系统将根据关键字与话术特征进行模拟甄别（不调用真实风控）。
      </el-alert>
      <el-input v-model="detectText" type="textarea" :rows="10" placeholder="粘贴可疑短信、转账说明、聊天内容…" />
      <el-button type="danger" :loading="detecting" style="margin-top:12px;width:100%" :disabled="!detectText.trim()" @click="doDetect">
        开始甄别（模拟）
      </el-button>
      <div v-if="detectResult" class="detect-result">
        <el-alert :type="detectResult.isFraud ? 'error' : 'success'" :closable="false" :title="detectResult.isFraud ? `疑似诈骗（匹配 ${detectResult.matchCount} 个特征）` : '未检测到明显诈骗特征'">
          <div v-if="(detectResult.matchKeywords || []).length" style="margin-top:6px">
            命中特征：
            <el-tag v-for="k in detectResult.matchKeywords" :key="k" type="warning" style="margin-right:6px">{{ k }}</el-tag>
          </div>
        </el-alert>
        <div class="suggest-box" v-if="detectResult.warning">
          <b>提示：</b>{{ detectResult.warning }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Search } from '@element-plus/icons-vue'
import { getAntiFraudList, getAntiFraudDetail, detectFraud } from '@/api/safety'

const loading = ref(false)
const list = ref([])
const detail = ref(null)
const filterType = ref('')
const typeOptions = ['刷单返利', '虚假投资', '冒充公检法', '冒充客服', '杀猪盘', '冒充熟人', '中奖诈骗', '网络贷款']

const detectActive = ref(false)
const detecting = ref(false)
const detectText = ref('')
const detectResult = ref(null)

const loadList = async () => {
  loading.value = true
  try {
    list.value = await getAntiFraudList({ type: filterType.value }) || []
  } catch (e) {} finally { loading.value = false }
}

const viewDetail = async (row) => {
  try {
    detail.value = await getAntiFraudDetail(row.id)
  } catch (e) {}
}

const doDetect = async () => {
  if (!detectText.value.trim()) return
  detecting.value = true
  detectResult.value = null
  try {
    const res = await detectFraud({ inputText: detectText.value })
    // 后端返回 BizFraudDetectionLog：detectResult / riskLevel / matchedRules / warningContent
    const rules = parseMatchedRules(res.matchedRules || '')
    detectResult.value = {
      isFraud: res.detectResult !== 'SAFE',
      matchCount: rules.length,
      matchKeywords: rules,
      warning: res.warningContent || ''
    }
  } catch (e) {
    ElMessage.error(e?.message || '甄别失败，请稍后重试')
  } finally { detecting.value = false }
}

/** 后端 matchedRules 形如 "[刷单, 垫付]"，解析为数组 */
function parseMatchedRules(s) {
  const t = s.trim()
  if (t === '[]' || t === '') return []
  return t.replace(/^\[|\]$/g, '').split(',').map(x => x.trim()).filter(Boolean)
}

onMounted(loadList)
</script>

<style scoped>
.module-header :deep(.el-card__body) { padding: 0; }
.header-content { display: flex; align-items: center; gap: 20px; padding: 24px; }
.module-icon { width: 72px; height: 72px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.module-info h2 { font-size: 22px; margin: 0 0 6px; color: #303133; }
.module-info p { font-size: 14px; color: #909399; margin: 0 0 8px; }
.header-actions { margin-left: auto; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
:deep(.el-table__row) { cursor: pointer; }
.detail-body .detail-section { margin-bottom: 16px; }
.s-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.s-box { padding: 12px; border-radius: 8px; background: #f5f7fa; font-size: 14px; line-height: 1.6; }
.s-box.warn { background: #fef6ec; border-left: 3px solid #e6a23c; }
.s-box.ok { background: #f0f9eb; border-left: 3px solid #67c23a; }
.detect-result { margin-top: 16px; }
.suggest-box { margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 8px; font-size: 14px; line-height: 1.6; }
</style>
