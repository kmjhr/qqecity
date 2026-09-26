<template>
  <div class="module-placeholder">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="32" color="#fff"><Money /></el-icon>
        </div>
        <div class="module-info">
          <h2>轻创业智能授信</h2>
          <p>青创e贷 A/B 双轨授信 · 受托支付模拟 · 随借随还</p>
          <el-tag type="warning" size="small">演示系统 · 银行能力模拟</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <h3>🔍 B类免费预审</h3>
          <p>不查征信，提交创业计划获取 1-2 万元额度区间</p>
          <el-button type="primary" plain>立即预审</el-button>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <h3>💳 我的额度</h3>
          <p>查看 A/B 类型授信额度与可用余额</p>
          <el-button type="primary" plain>查看额度</el-button>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <h3>💸 受托支付</h3>
          <p>100% 定向打款给指定商户，资金不经个人账户</p>
          <el-button type="primary" plain>去提款</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="模块开发中"
      type="info"
      description="青创e贷完整功能（预审→授信→受托支付→还款）正在开发中，敬请期待。"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { Money } from '@element-plus/icons-vue'
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
