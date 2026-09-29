<template>
  <div class="dashboard-page">
    <!-- 演示模式：人工审核自动通过开关 -->
    <el-card shadow="never" class="demo-switch-card">
      <div class="demo-switch-content">
        <div>
          <p class="demo-switch-title">
            演示模式：人工审核自动通过
            <el-tag size="small" :type="autoApprove.enabled ? 'success' : 'info'" style="margin-left: 8px">
              {{ autoApprove.enabled ? '已开启' : '已关闭' }}
            </el-tag>
          </p>
          <p class="demo-switch-desc">
            开启后：待房东确认 / AI复审转人工 / 索赔存疑与申辩 / 大额受托支付 / 商户审核 全部自动通过，评委演示无需逐单点击；关闭后恢复真实人工审核流程。
          </p>
        </div>
        <el-switch
          v-model="autoApprove.enabled"
          active-text="自动通过"
          inactive-text="人工审核"
          @change="toggleAutoApprove"
        />
      </div>
    </el-card>

    <!-- 数据概览卡片（对接真实统计接口） -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card shadow="hover" class="stat-card" @click="item.path && $router.push(item.path)">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-title">{{ item.title }}</p>
              <p class="stat-value">{{ loading ? '—' : item.value }}</p>
              <p class="stat-desc" :style="{ color: item.color }">
                {{ item.desc }}
              </p>
            </div>
            <div class="stat-icon" :style="{ background: item.bgColor }">
              <el-icon :size="28" color="#fff">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办工作台（审核队列快捷入口） -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span>业务待办工作台</span>
          </template>
          <el-row :gutter="16">
            <el-col :span="6" v-for="todo in todos" :key="todo.title" style="margin-bottom: 12px">
              <el-card shadow="hover" class="todo-card" @click="$router.push(todo.path)">
                <div class="todo-content">
                  <div>
                    <p class="todo-title">{{ todo.title }}</p>
                    <p class="todo-count">{{ loading ? '—' : todo.count }}</p>
                  </div>
                  <el-tag :type="todo.type" size="small">{{ todo.tag }}</el-tag>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardStats, getDemoAutoApprove, setDemoAutoApprove } from '@/api/dashboard'

const loading = ref(false)
const data = ref<any>({})
const autoApprove = reactive<{ enabled: boolean }>({ enabled: true })

async function toggleAutoApprove(val: boolean) {
  try {
    const res = await setDemoAutoApprove(val)
    autoApprove.enabled = res.enabled
    ElMessage.success(res.enabled ? '演示自动审核已开启（所有人工审核自动通过）' : '演示自动审核已关闭（恢复人工审核）')
  } catch (e: any) {
    autoApprove.enabled = !val
    ElMessage.error('切换失败：' + (e?.message || '未知错误'))
  }
}

const stats = computed(() => [
  {
    title: '用户总数',
    value: data.value.userCount ?? 0,
    desc: `今日新增 ${data.value.userToday ?? 0}`,
    color: '#67c23a',
    icon: 'User',
    bgColor: 'linear-gradient(135deg, #667eea, #764ba2)',
    path: '/system/user'
  },
  {
    title: '保函申请',
    value: data.value.guaranteeTotal ?? 0,
    desc: `待房东确认 ${data.value.guaranteePendingConfirm ?? 0}`,
    color: '#e6a23c',
    icon: 'Document',
    bgColor: 'linear-gradient(135deg, #f093fb, #f5576c)',
    path: '/business/guarantee-manage'
  },
  {
    title: '贷款待审批',
    value: data.value.loanPending ?? 0,
    desc: '待 banker 审批',
    color: '#e6a23c',
    icon: 'Money',
    bgColor: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    path: '/business/loan-review'
  },
  {
    title: '预警未处理',
    value: data.value.riskUnhandled ?? 0,
    desc: '需及时处置',
    color: '#f56c6c',
    icon: 'Warning',
    bgColor: 'linear-gradient(135deg, #fa709a, #fee140)',
    path: '/business/risk-overview'
  }
])

const todos = computed(() => [
  {
    title: '代房东确认',
    count: data.value.guaranteePendingConfirm ?? 0,
    tag: '保函',
    type: 'warning',
    path: '/business/guarantee-manage'
  },
  {
    title: '保函人工复审',
    count: data.value.guaranteeManualReview ?? 0,
    tag: '存疑转人工',
    type: 'danger',
    path: '/business/ai-review'
  },
  {
    title: '贷款审批',
    count: data.value.loanPending ?? 0,
    tag: '待审批',
    type: 'warning',
    path: '/business/loan-review'
  },
  {
    title: '注册审核记录',
    count: data.value.registrationReviews ?? 0,
    tag: '白名单留痕',
    type: 'info',
    path: '/business/registration-review'
  }
])

onMounted(() => {
  loadData()
  loadAutoApprove()
})

async function loadAutoApprove() {
  try {
    const res = await getDemoAutoApprove()
    autoApprove.enabled = res.enabled
  } catch (e) {
    // 开关接口异常时保持默认开启展示
  }
}

async function loadData() {
  loading.value = true
  try {
    data.value = await getDashboardStats()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

.demo-switch-card {
  border-radius: 8px;
  margin-bottom: 20px;
  background: linear-gradient(90deg, #f0f9ff, #ecf5ff);
  border-color: #b3d8ff;
}

.demo-switch-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.demo-switch-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.demo-switch-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  max-width: 900px;
}

.stat-card {
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-desc {
  font-size: 12px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.todo-card {
  border-radius: 8px;
  cursor: pointer;
}

.todo-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.todo-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.todo-count {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
</style>
