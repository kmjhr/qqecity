<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>注册新账号</h2>
        <p>加入青启e城，开启城市新生活</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请设置用户名/手机号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称（选填）"
            size="large"
            :prefix-icon="Avatar"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请设置密码（6-20位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（选填）"
            size="large"
            :prefix-icon="Phone"
          />
        </el-form-item>

        <el-form-item prop="userType">
          <el-select
            v-model="registerForm.userType"
            placeholder="请选择您的人群类型"
            size="large"
            style="width: 100%"
          >
            <el-option label="在校大学生" value="STUDENT" />
            <el-option label="毕业2年内青年" value="GRADUATE" />
            <el-option label="青年创业者" value="ENTREPRENEUR" />
            <el-option label="其他城市青年" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注 册
        </el-button>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">去登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Avatar } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  userType: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname,
        phone: registerForm.phone,
        userType: registerForm.userType
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (e) {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.register-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-card {
  width: 460px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.register-header h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.register-header p {
  font-size: 14px;
  color: #909399;
}

.register-btn {
  width: 100%;
  margin-top: 10px;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-top: 16px;
}

.login-link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
</style>
