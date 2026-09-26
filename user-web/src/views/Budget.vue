<template>
  <div class="module-placeholder">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
          <el-icon :size="32" color="#fff"><PieChart /></el-icon>
        </div>
        <div class="module-info">
          <h2>碎片消费治理</h2>
          <p>分类预算 · 交易归类 · 分级提醒 · 结余转储蓄</p>
          <el-tag type="warning" size="small">演示系统 · 模拟数据</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>💰 分类预算</h3>
          <p>设置餐饮/娱乐/购物/交通等分类预算，智能推荐额度</p>
          <el-button type="primary" plain>设置预算</el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>🏦 结余转储蓄</h3>
          <p>月底预算结余一键转入心愿储蓄，积少成多</p>
          <el-button type="primary" plain>立即转存</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="模块开发中"
      type="info"
      description="预算治理完整功能（分类设置→交易归类→分级提醒→结余转储蓄）正在开发中，敬请期待。"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { PieChart } from '@element-plus/icons-vue'
</script>

<style scoped>
.module-header :deep(.el-card__body) {
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
}

.module-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-info h2 {
  font-size: 22px;
  margin-bottom: 6px;
  color: #303133;
}

.module-info p {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.feature-card {
  height: 100%;
}

.feature-card h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #303133;
}

.feature-card p {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}
</style>
