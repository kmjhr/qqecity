<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>反诈教学（演示内容）</template>
          <el-collapse v-if="contents.length">
            <el-collapse-item v-for="c in contents" :key="c.id" :title="c.title">
              <div style="font-size:14px;line-height:1.7;">{{ c.summary }}</div>
              <el-button size="small" type="primary" style="margin-top:8px;" @click="onLearn(c)">标记完成（学习打卡）</el-button>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>骗局话术甄别（规则模拟）</template>
          <el-input
            v-model="text"
            type="textarea"
            :rows="4"
            placeholder="粘贴可疑话术，如：专业征信修复，包过包洗白，先付定金……"
          />
          <el-button type="primary" style="margin-top:10px;" :loading="verifying" @click="onVerify">开始甄别</el-button>

          <el-result v-if="verified && hits.length === 0" icon="success" title="未命中已知骗局特征" sub-title="该话术通过演示规则检查，但仍需谨慎核实身份与渠道。" />
          <template v-else-if="hits.length">
            <el-table :data="hits" size="small" style="margin-top:12px;">
              <el-table-column prop="type" label="风险类型" width="140" />
              <el-table-column prop="keyword" label="命中词" width="110" />
              <el-table-column prop="advice" label="建议" />
            </el-table>
            <el-alert type="error" :closable="false" title="已拦截：命中骗局特征，请勿转账、勿提供验证码。" style="margin-top:10px;" />
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { safetyApi } from '../api'

const contents = ref([])
const text = ref('')
const hits = ref([])
const verified = ref(false)
const verifying = ref(false)

async function load() {
  contents.value = await safetyApi.fraudContent()
}

async function onVerify() {
  if (!text.value.trim()) {
    ElMessage.warning('请输入话术内容')
    return
  }
  verifying.value = true
  try {
    hits.value = await safetyApi.verifyText(text.value)
    verified.value = true
  } finally {
    verifying.value = false
  }
}

async function onLearn(c) {
  await safetyApi.learn({ contentId: c.id, learnType: '情景模拟', score: 100 })
  ElMessage.success('学习完成，已记录')
}

onMounted(load)
</script>
