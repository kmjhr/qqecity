<template>
  <div class="credit-profile-page">
    <!-- 顶部：综合评分 -->
    <el-card shadow="never" class="overview-card">
      <div class="overview-row">
        <div class="overview-left">
          <div class="score-ring" :style="ringStyle(profile?.overallScore || 0)">
            <div class="score-num">{{ profile?.overallScore ?? '--' }}</div>
            <div class="score-label">综合评分</div>
          </div>
          <div class="overview-meta">
            <h2>{{ profile?.username || '青年用户' }} 的成长画像</h2>
            <el-tag size="small" type="info">
              {{ userTypeMap[profile?.userType] || '其他' }}
            </el-tag>
            <el-tag v-if="profile?.simulated" size="small" type="warning">模拟数据</el-tag>
          </div>
        </div>
        <div class="overview-right">
          <el-button
            type="primary"
            :loading="linkageLoading"
            :disabled="!profile?.linkage?.eligible"
            @click="handleLinkage"
          >
            {{ profile?.linkage?.eligible ? '应用联动提额' : '未达联动条件' }}
          </el-button>
          <p class="linkage-hint">{{ profile?.linkage?.explanation || '稳定性≥60 且 经营力≥60 可触发授信提额（演示级）' }}</p>
        </div>
      </div>
    </el-card>

    <!-- 三维评分 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8" v-for="item in scoreList" :key="item.key">
        <el-card shadow="hover" class="score-card" :class="`level-${item.level?.toLowerCase()}`">
          <div class="score-head">
            <span class="scene-tag">{{ sceneMap[item.scene] || item.scene }}</span>
            <el-tag :type="levelTagType(item.level)" size="small">{{ levelMap[item.level] || item.level }}</el-tag>
          </div>
          <h3 class="dimension-name">{{ item.dimensionName }}</h3>
          <div class="score-bar">
            <el-progress :percentage="item.score || 0" :color="levelColor(item.level)" :stroke-width="14" />
          </div>
          <div class="evidences">
            <div class="block-title">评分依据</div>
            <ul>
              <li v-for="(ev, i) in item.evidences || []" :key="i">{{ ev }}</li>
            </ul>
          </div>
          <div class="data-points" v-if="item.dataPoints?.length">
            <div class="block-title">关键指标</div>
            <div class="dp-row" v-for="dp in item.dataPoints" :key="dp.label">
              <span class="dp-label">{{ dp.label }}</span>
              <span class="dp-value">{{ dp.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-empty v-if="!loading && !scoreList.length" description="暂无评分数据" />
    </el-row>

    <!-- 成长轨迹 -->
    <el-card shadow="never" class="track-card">
      <template #header>
        <span>成长轨迹（按月）</span>
      </template>
      <el-timeline v-if="profile?.growthTrack?.length">
        <el-timeline-item
          v-for="(t, i) in profile.growthTrack"
          :key="i"
          :timestamp="t.period"
          :type="sceneTimelineType(t.scene)"
          placement="top"
        >
          <div class="track-event">
            <span>{{ t.event }}</span>
            <el-tag v-if="t.scoreDelta > 0" type="success" size="small">+{{ t.scoreDelta }}</el-tag>
            <el-tag v-else-if="t.scoreDelta < 0" type="danger" size="small">{{ t.scoreDelta }}</el-tag>
            <span class="track-scene">{{ sceneMap[t.scene] || t.scene }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无成长轨迹" />
    </el-card>

    <!-- 联动提额结果弹窗 -->
    <el-dialog v-model="linkageResultVisible" title="联动提额结果（模拟）" width="480px">
      <div v-if="linkageResult" class="linkage-result">
        <el-alert :title="linkageResult.notice" :type="linkageResult.applied ? 'success' : 'warning'" :closable="false" />
        <div class="result-grid">
          <div><span class="r-label">提额后总额度</span><span class="r-value">¥ {{ linkageResult.totalLimit }}</span></div>
          <div><span class="r-label">可用额度</span><span class="r-value">¥ {{ linkageResult.availableLimit }}</span></div>
          <div><span class="r-label">优惠后利率</span><span class="r-value">{{ linkageResult.interestRate }}%</span></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCreditProfile, applyLinkage } from '@/api/creditProfile'

const loading = ref(false)
const linkageLoading = ref(false)
const linkageResultVisible = ref(false)
const profile = ref(null)
const linkageResult = ref(null)

const userTypeMap = { STUDENT: '在校生', GRADUATE: '应届毕业生', ENTREPRENEUR: '创业者', OTHER: '其他' }
const sceneMap = { HOUSING: '安居场景', ENTREPRENEUR: '创业场景', CONSUMPTION: '消费场景' }
const levelMap = { LOW: '偏低', MEDIUM: '中等', HIGH: '良好' }

const scoreList = computed(() => {
  if (!profile.value) return []
  return [
    { key: 'stability', ...(profile.value.stability || {}) },
    { key: 'operation', ...(profile.value.operation || {}) },
    { key: 'fundHealth', ...(profile.value.fundHealth || {}) }
  ].filter(s => s.dimensionName || s.score != null)
})

function levelTagType(level) {
  return { LOW: 'danger', MEDIUM: 'warning', HIGH: 'success' }[level] || 'info'
}
function levelColor(level) {
  return { LOW: '#f56c6c', MEDIUM: '#e6a23c', HIGH: '#67c23a' }[level] || '#409eff'
}
function sceneTimelineType(scene) {
  return { HOUSING: 'success', ENTREPRENEUR: 'primary', CONSUMPTION: 'warning' }[scene] || ''
}

function ringStyle(score) {
  const deg = (score / 100) * 360
  return {
    background: `conic-gradient(#409eff ${deg}deg, #e6e8eb ${deg}deg)`
  }
}

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getCreditProfile()
  } finally {
    loading.value = false
  }
}

async function handleLinkage() {
  linkageLoading.value = true
  try {
    linkageResult.value = await applyLinkage()
    linkageResultVisible.value = true
    await loadProfile()
  } finally {
    linkageLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.credit-profile-page {
  max-width: 1200px;
  margin: 0 auto;
}

.overview-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.overview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.overview-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.score-ring {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}
.score-ring::before {
  content: '';
  position: absolute;
  inset: 8px;
  background: #fff;
  border-radius: 50%;
}
.score-num {
  position: relative;
  font-size: 30px;
  font-weight: 700;
  color: #409eff;
}
.score-label {
  position: relative;
  font-size: 12px;
  color: #909399;
}
.overview-meta h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
}
.overview-meta .el-tag {
  margin-right: 6px;
}
.overview-right {
  text-align: right;
}
.linkage-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  max-width: 320px;
}

.score-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.score-card.level-low { border-top: 3px solid #f56c6c; }
.score-card.level-medium { border-top: 3px solid #e6a23c; }
.score-card.level-high { border-top: 3px solid #67c23a; }

.score-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.scene-tag {
  font-size: 13px;
  color: #606266;
}
.dimension-name {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
}
.score-bar {
  margin-bottom: 12px;
}
.block-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.evidences ul {
  margin: 0 0 12px;
  padding-left: 18px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.dp-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
  border-bottom: 1px dashed #ebeef5;
}
.dp-row:last-child { border-bottom: none; }
.dp-label { color: #909399; }
.dp-value { color: #303133; font-weight: 500; }

.track-card {
  border-radius: 12px;
  margin-top: 8px;
}
.track-event {
  display: flex;
  align-items: center;
  gap: 8px;
}
.track-scene {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.linkage-result .result-grid {
  margin-top: 16px;
}
.result-grid > div {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f2f2f2;
  font-size: 14px;
}
.r-label { color: #909399; }
.r-value { color: #303133; font-weight: 600; }
</style>
