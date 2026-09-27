<template>
  <div class="policy-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>政策智能匹配 - 人才安居 + 创业贴息</span>
          <el-button type="primary" size="small" @click="handlePush" :loading="pushLoading">
            一键推送匹配政策到消息中心
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="匹配我的政策" name="matched" />
        <el-tab-pane label="全部政策" name="all" />
      </el-tabs>

      <div v-loading="loading" class="policy-list">
        <el-row :gutter="16">
          <el-col :span="12" v-for="p in policies" :key="p.id">
            <el-card class="policy-card" :class="{ matched: p.matched }" shadow="hover">
              <div class="policy-head">
                <el-tag :type="p.policyType === 'HOUSING' ? 'success' : 'primary'" size="small">
                  {{ p.policyType === 'HOUSING' ? '人才安居' : '创业贴息' }}
                </el-tag>
                <el-tag v-if="p.matched" type="success" size="small" effect="dark">已匹配</el-tag>
              </div>
              <h3 class="policy-title">{{ p.title }}</h3>
              <p class="policy-desc">{{ p.summary || p.description }}</p>
              <div v-if="p.crowdTags" class="crowd-tags">
                <el-tag v-for="tag in parseCrowdTags(p.crowdTags)" :key="tag" size="small" type="info">
                  {{ crowdTagName(tag) }}
                </el-tag>
              </div>
              <div class="policy-footer">
                <el-button type="primary" link size="small" @click="viewDetail(p)">
                  查看详情
                </el-button>
                <span v-if="p.matched" class="matched-tip">✓ 您符合申报条件</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!loading && policies.length === 0" description="暂无匹配政策" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="current?.title" width="640px">
      <div v-if="current">
        <p style="color: #606266; margin-bottom: 12px">{{ current.summary || current.description }}</p>
        <el-divider />
        <h4>申报条件</h4>
        <p style="white-space: pre-line">{{ current.conditions }}</p>
        <h4>政策内容</h4>
        <p style="white-space: pre-line">{{ current.content }}</p>
        <h4>申报入口</h4>
        <p>
          <el-link type="primary" :underline="false">
            {{ current.applyUrl || '演示入口（模拟）' }}
          </el-link>
        </p>
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
  pushPolicies
} from '@/api/policy'

const loading = ref(false)
const pushLoading = ref(false)
const policies = ref([])
const activeTab = ref('matched')
const detailVisible = ref(false)
const current = ref(null)

async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'matched') {
      policies.value = await matchedPolicies()
    } else {
      policies.value = await matchPolicies()
    }
  } finally {
    loading.value = false
  }
}

watch(activeTab, loadData)

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
    const matched = await pushPolicies()
    ElMessage.success(`已推送 ${matched?.length || 0} 条匹配政策至消息中心【模拟】`)
  } finally {
    pushLoading.value = false
  }
}

function parseCrowdTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    return JSON.parse(tags)
  } catch (e) {
    return String(tags).split(/[，,、]/).filter(Boolean)
  }
}
function crowdTagName(tag) {
  const m = {
    STUDENT: '在校生',
    GRADUATE: '应届毕业生',
    ENTREPRENEUR: '创业者',
    VETERAN: '退伍军人',
    DISABLED: '残疾人',
    FARMER: '农民',
    OTHER: '其他'
  }
  return m[tag] || tag
}

onMounted(loadData)
</script>

<style scoped>
.policy-page {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-list {
  min-height: 200px;
}

.policy-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border-left: 4px solid #dcdfe6;
}
.policy-card.matched {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.policy-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.policy-title {
  margin: 8px 0;
  font-size: 16px;
  color: #303133;
}

.policy-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.crowd-tags {
  margin-bottom: 8px;
}

.policy-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.matched-tip {
  color: #67c23a;
  font-size: 12px;
}
</style>
