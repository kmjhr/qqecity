<template>
  <div class="policy-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>政策智能匹配 - 全国青年政策库</span>
          <div class="header-right">
            <el-select v-model="region" placeholder="按地区筛选" clearable style="width:160px" @change="loadData">
              <el-option v-for="r in regions" :key="r" :label="r === '全部' ? '全部地区' : r" :value="r === '全部' ? '' : r" />
            </el-select>
            <el-button v-if="activeTab !== 'portals'" type="primary" size="small" @click="handlePush" :loading="pushLoading">
              重新推送匹配政策
            </el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom:14px"
        :title="activeTab === 'portals'
          ? '官方入口专区：汇总全国及各省人社、住建房管、政务、税务等官方政策网站，点击卡片直接跳转官网申报页面'
          : '政策库覆盖国家层面及15省市真实政策（人工维护、官方来源），仅展示有效期内的政策；系统提取您的人群资质与创业行为关键词自动匹配，符合即自动推送消息中心'" />

      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="匹配我的政策" name="matched" />
        <el-tab-pane label="全部政策" name="all" />
        <el-tab-pane label="官方入口" name="portals" />
      </el-tabs>

      <!-- 官方入口专区 -->
      <div v-if="activeTab === 'portals'" v-loading="loading">
        <div class="portal-filter">
          <el-select v-model="portalType" placeholder="按入口类型筛选" clearable style="width:180px" @change="loadPortals">
            <el-option v-for="t in portalTypes" :key="t.value" :label="t.label" :value="t.value === '全部' ? '' : t.value" />
          </el-select>
        </div>
        <el-row :gutter="16">
          <el-col :span="8" v-for="p in portals" :key="p.id">
            <el-card class="portal-card" shadow="hover" @click="openPortal(p)">
              <div class="policy-head">
                <el-tag :type="portalTagType(p.portalType)" size="small">{{ p.portalTypeName }}</el-tag>
                <el-tag type="info" size="small" effect="plain">{{ p.region }}</el-tag>
              </div>
              <h3 class="portal-title">{{ p.portalName }}</h3>
              <p class="portal-desc">{{ p.description }}</p>
              <div class="policy-footer">
                <el-button type="primary" link size="small">进入官网 →</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!loading && portals.length === 0" description="暂无官方入口" />
      </div>

      <!-- 政策列表 -->
      <div v-else v-loading="loading" class="policy-list">
        <el-row :gutter="16">
          <el-col :span="8" v-for="p in policies" :key="p.id">
            <el-card class="policy-card" :class="{ matched: p.matched }" shadow="hover">
              <div class="policy-head">
                <el-tag :type="p.policyType === 'HOUSING' ? 'success' : 'warning'" size="small">
                  {{ p.policyTypeName }}
                </el-tag>
                <el-tag type="info" size="small" effect="plain">{{ p.region }}</el-tag>
                <el-tag v-if="p.matched" type="success" size="small" effect="dark">已匹配</el-tag>
              </div>
              <h3 class="policy-title">{{ p.policyName }}</h3>
              <p class="policy-summary">{{ p.policySummary }}</p>
              <div class="policy-benefit">
                {{ p.subsidyRate }}
                <template v-if="p.maxAmount > 0">｜最高 ¥{{ p.maxAmount }}</template>
              </div>
              <div v-if="p.hitKeywords && p.hitKeywords.length" class="crowd-tags">
                <el-tag v-for="k in p.hitKeywords" :key="k" size="small" type="success" effect="light">
                  命中：{{ k }}
                </el-tag>
              </div>
              <div class="policy-footer">
                <el-button type="primary" link size="small" @click="viewDetail(p)">详情 / 去申报</el-button>
                <span v-if="p.matched" class="matched-tip">✓ 您符合</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!loading && policies.length === 0" description="暂无政策" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="current?.policyName" width="680px">
      <div v-if="current">
        <el-descriptions :column="2" border size="small" style="margin-bottom:14px">
          <el-descriptions-item label="政策类型">{{ current.policyTypeName }}</el-descriptions-item>
          <el-descriptions-item label="适用地区">{{ current.region || '-' }}</el-descriptions-item>
          <el-descriptions-item label="有效期">
            {{ current.validFrom || '-' }} ~ {{ current.validTo || '长期有效' }}
          </el-descriptions-item>
          <el-descriptions-item label="补贴标准">{{ current.subsidyRate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="政策来源" :span="2">{{ current.policySource || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4>政策概要</h4>
        <p class="cond-text">{{ current.policySummary }}</p>
        <h4>申报条件</h4>
        <p class="cond-text">{{ current.conditions }}</p>
        <h4>政策内容</h4>
        <p class="cond-text">
          适用人群：{{ current.targetCrowdList?.join('、') || '-' }}
          <template v-if="current.hitKeywords && current.hitKeywords.length">
            ｜系统命中：{{ current.hitKeywords.join('、') }} → {{ current.matchReason }}
          </template>
        </p>
        <h4>申报入口（官方真实入口，点击跳转）</h4>
        <p>
          <el-link type="primary" :href="current.applyUrl" target="_blank" :underline="false">
            {{ current.applyUrl || '官方申报页面' }}
          </el-link>
        </p>
        <el-alert type="success" :closable="false" style="margin-top:10px"
          title="演示说明：平台仅做政策匹配与跳转演示，实际申报请以官方页面为准（模拟）" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  matchPolicies,
  matchedPolicies,
  getPolicyDetail,
  pushPolicies,
  listPortals
} from '@/api/policy'

const loading = ref(false)
const pushLoading = ref(false)
const policies = ref([])
const portals = ref([])
const activeTab = ref('matched')
const region = ref('')
const portalType = ref('')
const regions = ['全部', '全国', '浙江', '广东', '江苏', '上海', '北京', '四川', '湖北',
  '福建', '山东', '湖南', '河南', '安徽', '重庆', '广西', '陕西']
const portalTypes = [
  { label: '全部类型', value: '全部' },
  { label: '人社', value: 'GOV_HR' },
  { label: '住建房管', value: 'GOV_HOUSING' },
  { label: '政务服务', value: 'GOV_AFFAIR' },
  { label: '税务', value: 'GOV_TAX' },
  { label: '教育高校', value: 'GOV_EDU' }
]
const detailVisible = ref(false)
const current = ref(null)

async function loadData() {
  if (activeTab.value === 'portals') {
    loadPortals()
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'matched') {
      policies.value = await matchedPolicies(region.value || undefined)
    } else {
      policies.value = await matchPolicies(region.value || undefined)
    }
    if (activeTab.value === 'matched' && policies.value.length) {
      ElMessage.success(`已自动匹配 ${policies.value.length} 条政策并推送至消息中心【模拟】`)
    }
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function loadPortals() {
  loading.value = true
  try {
    portals.value = await listPortals(region.value || undefined, portalType.value || undefined)
  } catch (e) {
  } finally {
    loading.value = false
  }
}

watch(activeTab, loadData)

function openPortal(p) {
  window.open(p.url, '_blank')
}
function portalTagType(t) {
  return {
    GOV_HR: 'primary',
    GOV_HOUSING: 'success',
    GOV_AFFAIR: 'warning',
    GOV_TAX: 'danger',
    GOV_EDU: 'info',
    GOV_OTHER: 'info'
  }[t] || 'info'
}

async function viewDetail(p) {
  try {
    const detail = await getPolicyDetail(p.id)
    current.value = detail || p
    detailVisible.value = true
  } catch (e) {
    current.value = p
    detailVisible.value = true
  }
}

async function handlePush() {
  pushLoading.value = true
  try {
    const matched = await pushPolicies(region.value || undefined)
    ElMessage.success(`已重新推送 ${matched?.length || 0} 条匹配政策至消息中心【模拟】`)
  } finally {
    pushLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.policy-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.policy-list {
  min-height: 200px;
}

.policy-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border-left: 4px solid #dcdfe6;
  height: 100%;
  box-sizing: border-box;
}
.policy-card.matched {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.policy-head {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.policy-title {
  margin: 6px 0;
  font-size: 15px;
  color: #303133;
  line-height: 1.4;
  min-height: 42px;
}

.policy-summary {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.policy-benefit {
  color: #e6a23c;
  font-size: 12px;
  margin-bottom: 8px;
}

.crowd-tags {
  margin-bottom: 8px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.policy-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.matched-tip {
  color: #67c23a;
  font-size: 12px;
  font-weight: 600;
}
.cond-text {
  white-space: pre-line;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 12px;
}

/* 官方入口 */
.portal-filter {
  margin-bottom: 14px;
}
.portal-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  cursor: pointer;
  height: 100%;
  box-sizing: border-box;
  transition: transform 0.15s;
}
.portal-card:hover {
  transform: translateY(-2px);
}
.portal-title {
  margin: 6px 0;
  font-size: 15px;
  color: #303133;
  line-height: 1.4;
  min-height: 42px;
}
.portal-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
