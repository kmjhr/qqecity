<template>
  <div class="module-header" :style="{ '--module-color': color }">
    <div class="header-bg">
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
    </div>
    <div class="header-content">
      <div class="icon-wrap">
        <AppIcons v-if="icon" :name="icon" set="flat" :size="56" />
        <el-icon v-else :size="32" color="#fff"><component :is="fallbackIcon" /></el-icon>
      </div>
      <div class="text-wrap">
        <div class="title-row">
          <h2 class="title">{{ title }}</h2>
          <el-tag v-if="tag" :type="tagType" effect="light" class="module-tag" size="small">
            {{ tag }}
          </el-tag>
        </div>
        <p class="desc">{{ desc }}</p>
      </div>
      <div class="action-wrap" v-if="$slots.action">
        <slot name="action" />
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcons from './AppIcons.vue'

defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  icon: { type: String, default: '' }, // guarantee | loan | business | budget | safety
  fallbackIcon: { type: String, default: 'Sunny' },
  color: { type: String, default: '#0ea5e9' },
  tag: { type: String, default: '' },
  tagType: { type: String, default: 'warning' }
})
</script>

<style scoped>
.module-header {
  position: relative;
  border-radius: var(--qq-radius-xl);
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--qq-border);
  box-shadow: var(--qq-shadow-md);
  margin-bottom: 20px;
}

.header-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: var(--qq-radius-xl);
  opacity: 0.08;
  pointer-events: none;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  background: var(--module-color);
}

.blob-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -80px;
}

.blob-2 {
  width: 160px;
  height: 160px;
  left: -40px;
  bottom: -60px;
  background: var(--qq-secondary);
  opacity: 0.5;
}

.header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 32px;
}

.icon-wrap {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: var(--qq-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--module-color) 0%, color-mix(in srgb, var(--module-color) 70%, var(--qq-secondary)) 100%);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--module-color) 25%, transparent);
}

.text-wrap {
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: var(--qq-text);
  margin: 0;
}

.module-tag {
  font-weight: 500;
}

.desc {
  font-size: 14px;
  color: var(--qq-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.action-wrap {
  margin-left: auto;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
  }

  .action-wrap {
    margin-left: 0;
    margin-top: 16px;
  }
}
</style>
