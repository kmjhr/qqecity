<template>
  <div class="safety-page">
    <!-- 模块头 -->
    <ModuleHeader
      title="数字安全守护 · 反诈学堂"
      desc="实时预警 · 典型案例 · 分类学习 · 情景教学 · 智能甄别"
      icon="safety"
      color="#f39c12"
      tag="演示系统 · 模拟数据"
      tag-type="warning"
    >
      <template #action>
        <el-button type="danger" @click="detectActive = true">
          <el-icon><Search /></el-icon>骗局甄别
        </el-button>
      </template>
    </ModuleHeader>

    <!-- ==================== ① 实时反诈预警 ==================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="card-header">
          <span class="block-title"><i class="live-dot"></i>实时反诈预警</span>
          <el-tag type="warning" size="small" effect="plain">人工维护 · 模拟实时</el-tag>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="17">
          <div v-loading="alertLoading" class="alert-list">
            <div v-for="a in alerts" :key="a.id" class="alert-item" @click="openAlertDetail(a)">
              <div class="alert-time">{{ formatTime(a.publishTime) }}</div>
              <div class="alert-main">
                <div class="alert-head">
                  <el-tag :type="alertTagType(a.alertLevel)" size="small">
                    {{ alertLevelName(a.alertLevel) }}
                  </el-tag>
                  <el-tag size="small" effect="plain" type="info">{{ sceneName(a.relateScene) }}</el-tag>
                  <span class="alert-title">{{ a.title }}</span>
                </div>
                <div class="alert-summary">{{ a.summary }}</div>
              </div>
            </div>
            <el-empty v-if="!alertLoading && !alerts.length" description="暂无预警" />
          </div>
        </el-col>
        <el-col :span="7">
          <div class="official-box">
            <div class="official-title">官方反诈入口</div>
            <div class="official-desc">反诈预警为人工维护的模拟数据，真实信息请认准以下官方渠道：</div>
            <a class="official-link" href="https://www.12321.cn/" target="_blank">
              <div class="ol-name">12321 网络不良与垃圾信息举报中心</div>
              <div class="ol-sub">举报诈骗短信 / 钓鱼链接</div>
            </a>
            <a class="official-link" href="https://www.cpd.com.cn/" target="_blank">
              <div class="ol-name">中国警察网 · 反诈频道</div>
              <div class="ol-sub">公安部权威反诈资讯</div>
            </a>
            <a class="official-link" href="https://weibo.com/u/6473144685" target="_blank">
              <div class="ol-name">国家反诈中心（官方微博）</div>
              <div class="ol-sub">反诈预警与案例发布</div>
            </a>
            <el-alert type="warning" :closable="false" style="margin-top:10px">
              遇骗请立即拨打 96110 / 110
            </el-alert>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- ==================== ② 典型反诈案例（与平台业务强关联前置） ==================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="card-header">
          <span class="block-title">典型反诈案例</span>
          <span class="block-sub">按与「青启e城」业务关联度排序：保函租房 → 征信 → 创业贷 → 理财 → 客服 → 通用高发</span>
        </div>
      </template>
      <div v-loading="caseLoading" class="case-grid">
        <div v-for="c in cases" :key="c.id" class="case-card" @click="viewCase(c)">
          <div class="case-head">
            <el-tag size="small" :type="caseSceneTagType(c.category)" effect="dark">{{ caseScene(c.category) }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ c.category }}</el-tag>
          </div>
          <div class="case-title">{{ c.title }}</div>
          <div class="case-summary">{{ c.summary }}</div>
          <div class="case-footer">
            <el-button type="primary" link>查看话术 / 手法 / 防范 →</el-button>
          </div>
        </div>
        <el-empty v-if="!caseLoading && !cases.length" description="暂无案例" />
      </div>
    </el-card>

    <!-- ==================== ③ 反诈情景教学 ==================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="card-header">
          <span class="block-title">反诈情景教学</span>
          <el-button type="primary" @click="goTeaching">进入反诈学堂完整教学 →</el-button>
        </div>
      </template>
      <div v-loading="teachLoading" class="teach-grid">
        <div v-for="s in teachScenarios" :key="s.id" class="teach-card" @click="goTeaching">
          <div class="teach-head">
            <el-tag size="small" :type="s.contentType === 'SCENARIO_DIALOG' ? 'success' : 'info'">
              {{ s.contentType === 'SCENARIO_DIALOG' ? 'AI 对话演练' : '情景模拟答题' }}
            </el-tag>
            <el-tag size="small" effect="plain" type="warning">{{ s.category || '其他' }}</el-tag>
          </div>
          <div class="teach-title">{{ s.title }}</div>
          <div class="teach-summary">{{ s.summary }}</div>
          <div class="teach-footer">
            <el-button type="danger" link>去学习 →</el-button>
          </div>
        </div>
        <el-empty v-if="!teachLoading && !teachScenarios.length" description="暂无教学任务" />
      </div>
    </el-card>

    <!-- ==================== ④ 反诈知识分类学习（全量，按分类浏览） ==================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="card-header">
          <span class="block-title">反诈知识分类学习</span>
          <span class="block-sub">全量 {{ list.length }} 条反诈知识，按分类浏览（含情景模拟 / 对话演练入口）</span>
        </div>
      </template>
      <div v-loading="loading">
        <div class="learn-cats">
          <span
            v-for="cat in learnCats"
            :key="cat"
            class="learn-cat"
            :class="{ active: learnCat === cat }"
            @click="learnCat = cat"
          >{{ cat === '__ALL__' ? '全部' : cat }}</span>
        </div>
        <div v-if="filteredLearnList.length" class="learn-grid">
          <div v-for="it in filteredLearnList" :key="it.id" class="learn-card" @click="viewCase(it)">
            <div class="learn-head">
              <el-tag size="small" :type="learnTypeTag(it.contentType)">{{ learnTypeName(it.contentType) }}</el-tag>
              <el-tag size="small" effect="plain" type="info">{{ it.category }}</el-tag>
            </div>
            <div class="learn-title">{{ it.title }}</div>
            <div class="learn-summary">{{ it.summary }}</div>
            <div class="learn-footer">
              <el-button type="primary" link>学习 / 详情 →</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="该分类暂无内容" />
      </div>
    </el-card>

    <!-- 详情弹窗（案例/知识/预警通用） -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '详情'" width="720px" top="6vh">
      <div v-if="detail" class="detail-dialog">
        <div class="detail-head">
          <el-tag :type="caseSceneTagType(detail.category || detail.relateScene)" size="small">
            {{ caseScene(detail.category || detail.relateScene) }}
          </el-tag>
          <el-tag v-if="detail.alertLevel" :type="alertTagType(detail.alertLevel)" size="small">
            {{ alertLevelName(detail.alertLevel) }}
          </el-tag>
          <el-tag v-if="detail.publishTime" size="small" effect="plain" type="info">
            {{ formatTime(detail.publishTime) }}
          </el-tag>
        </div>
        <div class="detail-content" style="white-space: pre-line">{{ detail.content || detail.summary }}</div>
        <el-alert type="warning" :closable="false" style="margin-top:12px" :title="detail.linkUrl ? '官方举报/核实入口：' + detail.linkUrl : '遇骗请拨打 96110 / 110'" />
      </div>
    </el-dialog>

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
        <el-alert :type="detectResult.isFraud ? 'error' : 'success'" :closable="false"
          :title="detectResult.isFraud ? `疑似诈骗（匹配 ${detectResult.matchCount} 个特征）` : '未检测到明显诈骗特征'">
          <div v-if="(detectResult.matchKeywords || []).length" style="margin-top:6px">
            命中特征：
            <el-tag v-for="k in detectResult.matchKeywords" :key="k" type="warning" style="margin-right:6px">{{ k }}</el-tag>
          </div>
        </el-alert>
        <div class="suggest-box" v-if="detectResult.warning"><b>提示：</b>{{ detectResult.warning }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAntiFraudList, getAntiFraudDetail, detectFraud, getAlerts, getFeaturedCases } from '@/api/safety'
import { getScenarioList } from '@/api/teaching'
import ModuleHeader from '@/components/ModuleHeader.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const learnCat = ref('__ALL__')
const detail = ref(null)
const detailVisible = ref(false)

// 实时预警
const alertLoading = ref(false)
const alerts = ref([])

// 典型案例
const caseLoading = ref(false)
const cases = ref([])

// 反诈教学
const teachLoading = ref(false)
const teachScenarios = ref([])

// 骗局甄别
const detectActive = ref(false)
const detecting = ref(false)
const detectText = ref('')
const detectResult = ref(null)

/** 分类导航（保持列表出现顺序） */
const learnCats = computed(() => {
  const cats = list.value.map(x => x.category).filter(Boolean)
  return ['__ALL__', ...new Set(cats)]
})

/** 按当前分类过滤（全量学习区，与典型案例区区分：精选 vs 全量分类） */
const filteredLearnList = computed(() => {
  if (learnCat.value === '__ALL__') return list.value
  return list.value.filter(x => x.category === learnCat.value)
})

function alertTagType(l) {
  return { DANGER: 'danger', WARNING: 'warning', INFO: 'info' }[l] || 'info'
}
function alertLevelName(l) {
  return { DANGER: '高发', WARNING: '提醒', INFO: '提示' }[l] || l || ''
}
function sceneName(scene) {
  return {
    GUARANTEE: '保函租房',
    LOAN: '创业贷',
    CREDIT: '征信',
    WEALTH: '理财',
    PLATFORM: '平台客服',
    OTHER: '通用'
  }[scene] || '通用'
}
function caseScene(cat) {
  return {
    '租房诈骗': '关联·保函租房',
    '征信修复': '关联·信用画像',
    '创业贷款': '关联·青创e贷',
    '虚假投资': '关联·理财',
    '冒充客服': '关联·平台客服',
    '校园贷': '青年高发',
    '套路贷': '青年高发',
    '刷单诈骗': '青年高发',
    '冒充公检法': '青年高发',
    'AI换脸': '新型高发',
    '网络贷款': '青年高发',
    '虚假购物': '青年高发',
    '游戏交易': '青年高发',
    '兼职诈骗': '青年高发',
    '退款理赔': '消费诈骗',
    '色情诱导敲诈': '敲诈勒索',
    '冒充老板领导': '企业诈骗',
    '虚假中奖': '消费诈骗',
    '医保社保诈骗': '钓鱼短信',
    'ETC短信诈骗': '钓鱼短信',
    '机票退改签': '消费诈骗'
  }[cat] || '反诈警示'
}
function caseSceneTagType(cat) {
  return {
    '租房诈骗': 'primary',
    '征信修复': 'warning',
    '创业贷款': 'success',
    '虚假投资': 'danger',
    '冒充客服': 'warning',
    '校园贷': 'danger',
    '套路贷': 'danger',
    '刷单诈骗': 'warning',
    '冒充公检法': 'danger',
    'AI换脸': 'info',
    '网络贷款': 'warning',
    '虚假购物': 'warning',
    '游戏交易': 'warning',
    '兼职诈骗': 'warning',
    '退款理赔': 'danger',
    '色情诱导敲诈': 'danger',
    '冒充老板领导': 'warning',
    '虚假中奖': 'warning',
    '医保社保诈骗': 'info',
    'ETC短信诈骗': 'info',
    '机票退改签': 'warning'
  }[cat] || 'info'
}
function learnTypeName(t) {
  return { ARTICLE: '知识', CASE: '案例', SCENARIO_SIM: '模拟答题', SCENARIO_DIALOG: '对话演练' }[t] || t || '知识'
}
function learnTypeTag(t) {
  return { ARTICLE: 'primary', CASE: 'warning', SCENARIO_SIM: 'info', SCENARIO_DIALOG: 'success' }[t] || 'info'
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(5, 16)
}

async function loadAlerts() {
  alertLoading.value = true
  try { alerts.value = await getAlerts() || [] } catch (e) {} finally { alertLoading.value = false }
}

async function loadCases() {
  caseLoading.value = true
  try { cases.value = await getFeaturedCases() || [] } catch (e) {} finally { caseLoading.value = false }
}

async function loadTeaching() {
  teachLoading.value = true
  try { teachScenarios.value = (await getScenarioList() || []).slice(0, 6) } catch (e) {} finally { teachLoading.value = false }
}

async function loadList() {
  loading.value = true
  try { list.value = await getAntiFraudList() || [] } catch (e) {} finally { loading.value = false }
}

function goTeaching() {
  router.push('/teaching')
}

function openAlertDetail(a) {
  detail.value = { ...a, content: a.summary }
  detailVisible.value = true
}

async function viewCase(row) {
  try {
    const d = await getAntiFraudDetail(row.id)
    detail.value = d || row
  } catch (e) {
    detail.value = row
  }
  detailVisible.value = true
}

async function doDetect() {
  if (!detectText.value.trim()) return
  detecting.value = true
  detectResult.value = null
  try {
    const res = await detectFraud({ inputText: detectText.value })
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
  const t = (s || '').trim()
  if (t === '[]' || t === '') return []
  return t.replace(/^\[|\]$/g, '').split(',').map(x => x.trim()).filter(Boolean)
}

onMounted(() => {
  loadAlerts()
  loadCases()
  loadTeaching()
  loadList()
})
</script>

<style scoped>
.safety-page {
  max-width: 1400px;
  margin: 0 auto;
}

.block-card { margin-bottom: 20px; border-radius: var(--qq-radius-xl); }
.block-card :deep(.el-card__header) { padding: 16px 24px; }
.card-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.block-title { font-weight: 600; font-size: 15px; display: inline-flex; align-items: center; gap: 8px; }
.block-sub { font-size: 12px; color: #909399; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; display: inline-block; animation: pulse 1.2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.25; } }

/* 预警 */
.alert-list { max-height: 420px; overflow-y: auto; }
.alert-item { display: flex; gap: 12px; padding: 10px 6px; border-bottom: 1px dashed #ebeef5; cursor: pointer; }
.alert-item:hover { background: #fafafa; }
.alert-time { width: 88px; flex-shrink: 0; color: #909399; font-size: 12px; padding-top: 3px; }
.alert-main { flex: 1; min-width: 0; }
.alert-head { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.alert-title { font-size: 14px; font-weight: 600; color: #303133; }
.alert-summary { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.official-box { background: #f5f7fa; border-radius: 10px; padding: 14px; }
.official-title { font-weight: 600; font-size: 14px; margin-bottom: 6px; }
.official-desc { font-size: 12px; color: #909399; line-height: 1.6; margin-bottom: 12px; }
.official-link { display: block; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; text-decoration: none; transition: all 0.15s; }
.official-link:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12); }
.ol-name { font-size: 13px; font-weight: 600; color: #303133; }
.ol-sub { font-size: 12px; color: #909399; margin-top: 2px; }

/* 典型案例 */
.case-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.case-card { border: 1px solid #ebeef5; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.15s; border-left: 4px solid #409eff; background: #fff; }
.case-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
.case-head { display: flex; gap: 6px; margin-bottom: 8px; }
.case-title { font-size: 14px; font-weight: 600; color: #303133; line-height: 1.4; min-height: 40px; }
.case-summary { font-size: 12px; color: #909399; line-height: 1.6; margin: 6px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.case-footer { text-align: right; }

/* 教学 */
.teach-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.teach-card { border: 1px solid #ebeef5; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.15s; border-top: 3px solid #f56c6c; }
.teach-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
.teach-head { display: flex; gap: 6px; margin-bottom: 8px; }
.teach-title { font-size: 14px; font-weight: 600; color: #303133; min-height: 40px; }
.teach-summary { font-size: 12px; color: #909399; line-height: 1.5; margin: 6px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.teach-footer { text-align: right; }

/* 分类学习 */
.learn-cats { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.learn-cat { padding: 5px 14px; border-radius: 16px; font-size: 13px; cursor: pointer; background: #f5f7fa; color: #606266; border: 1px solid #ebeef5; transition: all 0.15s; }
.learn-cat:hover { color: #409eff; border-color: #a0cfff; }
.learn-cat.active { background: #409eff; color: #fff; border-color: #409eff; }
.learn-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.learn-card { border: 1px solid #ebeef5; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.15s; background: #fff; }
.learn-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
.learn-head { display: flex; gap: 6px; margin-bottom: 8px; }
.learn-title { font-size: 14px; font-weight: 600; color: #303133; min-height: 40px; line-height: 1.4; }
.learn-summary { font-size: 12px; color: #909399; line-height: 1.6; margin: 6px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.learn-footer { text-align: right; }

.detail-dialog .detail-head { display: flex; gap: 8px; margin-bottom: 12px; }
.detail-content { background: #f5f7fa; border-radius: 8px; padding: 14px; font-size: 14px; line-height: 1.9; }

.detect-result { margin-top: 16px; }
.suggest-box { margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 8px; font-size: 14px; line-height: 1.6; }

@media (max-width: 1200px) {
  .case-grid, .teach-grid, .learn-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
