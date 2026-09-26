<template>
  <div class="module-placeholder">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
          <el-icon :size="32" color="#fff"><Notebook /></el-icon>
        </div>
        <div class="module-info">
          <h2>创业经营赋能</h2>
          <p>AI 简易记账 · 现金流报表 · 风险预警</p>
          <el-tag type="info" size="small">占位模块 · 演示数据</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>📊 AI 记账</h3>
          <p>自动记录分类、模糊交易确认、现金流报表（演示数据）</p>
          <el-button type="primary" plain>查看报表</el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>⚠️ 风险预警</h3>
          <p>账面盈利但现金紧张预警，帮你把控经营现金流</p>
          <el-button type="primary" plain>查看预警</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="模块占位中"
      type="info"
      description="经营赋能模块为占位功能，当前返回模拟数据，二期迭代完善。"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { Notebook } from '@element-plus/icons-vue'
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
