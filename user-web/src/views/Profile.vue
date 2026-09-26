<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左侧：基本信息卡片 -->
      <el-col :span="8">
        <el-card class="profile-card" shadow="never">
          <div class="avatar-section">
            <el-avatar :size="80" :src="userInfo?.avatar">
              {{ userInfo?.nickname?.charAt(0) || userInfo?.username?.charAt(0) || 'U' }}
            </el-avatar>
            <h3>{{ userInfo?.nickname || userInfo?.username }}</h3>
            <el-tag :type="userInfo?.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </div>

          <el-divider />

          <div class="info-list">
            <div class="info-item">
              <span class="label">用户名</span>
              <span class="value">{{ userInfo?.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ userInfo?.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ userInfo?.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">注册时间</span>
              <span class="value">{{ formatDate(userInfo?.createTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：编辑表单 + 修改密码 -->
      <el-col :span="16">
        <el-card shadow="never" class="edit-card">
          <template #header>
            <div class="card-header">
              <span>修改个人信息</span>
            </div>
          </template>

          <el-form :model="editForm" label-width="80px" style="max-width: 500px">
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="password-card">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="80px"
            style="max-width: 500px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword } from '@/api/user'

const userStore = useUserStore()

const userInfo = ref(null)
const saving = ref(false)
const pwdLoading = ref(false)
const passwordFormRef = ref(null)

const editForm = reactive({
  nickname: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  try {
    const data = await userStore.fetchProfile()
    userInfo.value = data
    editForm.nickname = data.nickname || ''
    editForm.phone = data.phone || ''
    editForm.email = data.email || ''
  } catch (e) {
    // 错误已处理
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      nickname: editForm.nickname,
      phone: editForm.phone,
      email: editForm.email
    })
    ElMessage.success('保存成功')
    await loadProfile()
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async valid => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      await userStore.logout()
      window.location.href = '/login'
    } finally {
      pwdLoading.value = false
    }
  })
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}
</script>

<style scoped>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 12px;
}

.avatar-section {
  text-align: center;
  padding: 20px 0;
}

.avatar-section h3 {
  margin: 12px 0 8px;
  font-size: 18px;
  color: #303133;
}

.info-list {
  padding: 10px 0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  border-bottom: 1px solid #f2f2f2;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #909399;
}

.info-item .value {
  color: #303133;
}

.edit-card,
.password-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  font-weight: 600;
  color: #303133;
}
</style>
