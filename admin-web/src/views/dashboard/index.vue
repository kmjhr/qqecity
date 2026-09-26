<template>
  <div class="dashboard-page">
    <!-- 数据概览卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-title">{{ item.title }}</p>
              <p class="stat-value">{{ item.value }}</p>
              <p class="stat-desc" :style="{ color: item.color }">
                {{ item.trend }}
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

    <!-- 图表区域（占位，实际项目接入 ECharts） -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span>用户增长趋势</span>
          </template>
          <div class="chart-placeholder">
            <el-empty description="图表占位 - 可接入 ECharts" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>用户角色分布</span>
          </template>
          <div class="chart-placeholder">
            <el-empty description="图表占位" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 统计卡片数据（骨架占位，实际对接后端接口）
const stats = ref([
  {
    title: '用户总数',
    value: '1,286',
    trend: '↑ 12.5% 较上周',
    color: '#67c23a',
    icon: 'User',
    bgColor: 'linear-gradient(135deg, #667eea, #764ba2)'
  },
  {
    title: '活跃用户',
    value: '856',
    trend: '↑ 8.2% 较上周',
    color: '#67c23a',
    icon: 'Avatar',
    bgColor: 'linear-gradient(135deg, #f093fb, #f5576c)'
  },
  {
    title: '今日新增',
    value: '42',
    trend: '↑ 5.0% 较昨日',
    color: '#e6a23c',
    icon: 'Plus',
    bgColor: 'linear-gradient(135deg, #4facfe, #00f2fe)'
  },
  {
    title: '管理员',
    value: '3',
    trend: '数量正常',
    color: '#909399',
    icon: 'Setting',
    bgColor: 'linear-gradient(135deg, #43e97b, #38f9d7)'
  }
])
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

.stat-card {
  border-radius: 8px;
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

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
