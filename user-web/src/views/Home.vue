<template>
  <div class="home-page">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="never">
      <div class="banner-content">
        <div class="banner-text">
          <h1>你好，{{ userStore.username }} 👋</h1>
          <p>欢迎来到青启e城，你的专属青年金融服务平台</p>
        </div>
        <div class="banner-icon">
          <el-icon :size="64" color="#fff"><Sunny /></el-icon>
        </div>
      </div>
    </el-card>

    <!-- 功能入口卡片 -->
    <div class="feature-grid">
      <el-card
        v-for="item in features"
        :key="item.path"
        class="feature-card"
        shadow="hover"
        @click="$router.push(item.path)"
      >
        <div class="feature-icon" :style="{ background: item.bgColor }">
          <el-icon :size="32" color="#fff">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </el-card>
    </div>

    <!-- 模块占位提示 -->
    <el-empty
      v-if="features.length === 0"
      description="业务模块开发中..."
      image-size="100"
    />
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { Sunny } from '@element-plus/icons-vue'

const userStore = useUserStore()

// 业务模块入口配置，对齐《需求清单》三大场景 + 金融安全
// AI 生成新模块时在此添加一项即可
const features = [
  {
    title: '安居保函',
    desc: '租房履约保函，降低租房押金压力',
    icon: 'Wallet',
    path: '/guarantee',
    bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    title: '青创e贷',
    desc: '青年创业智能授信，助力城市轻创业',
    icon: 'Money',
    path: '/loan',
    bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    title: '预算消费',
    desc: '分类预算管理，结余一键转储蓄',
    icon: 'PieChart',
    path: '/budget',
    bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    title: '金融安全',
    desc: '反诈财商教学，骗局智能甄别',
    icon: 'Shield',
    path: '/safety',
    bgColor: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
  },
  {
    title: '经营赋能',
    desc: 'AI 简易记账，现金流风险预警',
    icon: 'Notebook',
    path: '/bookkeeping',
    bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  },
  {
    title: '消息中心',
    desc: '预算提醒、业务通知，一览无余',
    icon: 'Bell',
    path: '/message',
    bgColor: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
  }
]
</script>

<style scoped>
.home-page {
  max-width: 1100px;
  margin: 0 auto;
}

.welcome-banner {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.welcome-banner :deep(.el-card__body) {
  padding: 0;
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 36px;
  color: #fff;
}

.banner-text h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.banner-text p {
  font-size: 15px;
  opacity: 0.9;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.feature-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.feature-card :deep(.el-card__body) {
  padding: 24px;
  text-align: center;
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.feature-card h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
</style>
