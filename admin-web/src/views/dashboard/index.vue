<template>
  <div class="dashboard-page">
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
import { ref, computed, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'

const loading = ref(false)
const data = ref<any>({})

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
})

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
