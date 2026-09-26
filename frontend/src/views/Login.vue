<template>
  <div style="height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#1f3a68 0%,#2d5aa0 100%);">
    <el-card style="width:420px;">
      <div style="text-align:center;margin-bottom:20px;">
        <div style="font-size:22px;font-weight:700;color:#1f3a68;">青启e城</div>
        <div style="font-size:13px;color:#8a919c;margin-top:4px;">青年金融智能服务平台（演示系统）</div>
      </div>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.phone" placeholder="手机号" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="onLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%;" :loading="loading" @click="onLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" style="margin-top:8px;">
        <div style="font-size:12px;line-height:1.6;">
          演示账号：13800000000 / 123456<br />
          演示数据均为模拟，不构成真实金融服务
        </div>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ phone: '13800000000', password: '123456' })

async function onLogin() {
  if (!form.phone || !form.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.phone, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>
