<template>
  <div class="module-placeholder">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon :size="32" color="#fff"><Wallet /></el-icon>
        </div>
        <div class="module-info">
          <h2>安居金融风控</h2>
          <p>租房履约保函全流程 · AI合同复审 · 违约索赔</p>
          <el-tag type="warning" size="small">演示系统 · 银行能力模拟</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>📝 保函申请</h3>
          <p>录入房东信息、租赁要素、押金金额，上传租赁合同与房屋照片</p>
          <el-button type="primary" plain>去申请</el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>📋 保函查询</h3>
          <p>查看保函状态流转：申请中→待确认→待缴费→已开立→已失效</p>
          <el-button type="primary" plain>查看列表</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="模块开发中"
      type="info"
      description="安居保函完整功能（申请→房东确认→AI复审→缴费出函→索赔）正在开发中，敬请期待。"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { Wallet } from '@element-plus/icons-vue'
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
