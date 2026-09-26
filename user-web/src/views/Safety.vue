<template>
  <div class="module-placeholder">
    <el-card shadow="never" class="module-header">
      <div class="header-content">
        <div class="module-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
          <el-icon :size="32" color="#fff"><Lock /></el-icon>
        </div>
        <div class="module-info">
          <h2>青年金融安全</h2>
          <p>反诈财商教学 · 骗局甄别 · 征信守护</p>
          <el-tag type="warning" size="small">演示系统 · 规则模拟</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>🎓 反诈教学</h3>
          <p>情景化反诈图文内容，提升青年金融安全意识</p>
          <el-button type="primary" plain>去学习</el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="feature-card">
          <h3>🔍 骗局甄别</h3>
          <p>输入话术文本，系统按规则判定是否为诈骗话术</p>
          <el-button type="primary" plain>立即检测</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="模块开发中"
      type="info"
      description="金融安全完整功能（反诈教学+骗局甄别+征信解读）正在开发中，敬请期待。"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { Lock } from '@element-plus/icons-vue'
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
