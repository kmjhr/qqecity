<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="login-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="grid"></div>
    </div>

    <div class="login-wrapper">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-logo" @click="$router.push('/home')">
            <img src="/logo-icon.jpg" alt="青启e城" />
            <div>
              <div class="brand-title">青启e城</div>
              <div class="brand-subtitle">QINGQI eCity</div>
            </div>
          </div>
          <h1 class="brand-slogan">
            让青春在城市
            <span class="gradient-text">轻启金融可能</span>
          </h1>
          <p class="brand-desc">
            面向青年群体的城市金融智能服务平台，覆盖安居、创业、消费、经营与金融安全五大场景。
          </p>

          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-dot blue"></div>
              <span>安居保函 · 租房押金替代</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot green"></div>
              <span>青创e贷 · A/B 双轨授信</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot orange"></div>
              <span>预算消费 · 结余转储蓄</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot purple"></div>
              <span>金融安全 · AI 反诈守护</span>
            </div>
          </div>
        </div>

        <div class="brand-card-float">
          <div class="mini-card">
            <div class="mini-card-head">
              <span></span><span></span><span></span>
            </div>
            <div class="mini-card-body">
              <div class="mini-row">
                <div class="mini-icon blue"><el-icon><Wallet /></el-icon></div>
                <div>
                  <div class="mini-label">保函额度</div>
                  <div class="mini-value">¥ 8,000</div>
                </div>
              </div>
              <div class="mini-chart">
                <div class="mini-bar" style="height: 40%"></div>
                <div class="mini-bar" style="height: 70%"></div>
                <div class="mini-bar active" style="height: 90%"></div>
                <div class="mini-bar" style="height: 55%"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="login-form-card">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录你的青启e城账户</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名 / 手机号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
          </div>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>

          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Wallet } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/home'
      router.push(redirect)
    } catch (e) {
      // 错误已在 request 拦截器中提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
  box-sizing: border-box;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.45;
  animation: float 10s ease-in-out infinite;
}

.blob-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #0ea5e9, #38bdf8);
  top: -150px;
  left: -100px;
}

.blob-2 {
  width: 420px;
  height: 420px;
  background: linear-gradient(135deg, #10b981, #34d399);
  bottom: -120px;
  right: 5%;
  animation-delay: -3s;
}

.blob-3 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #f97316, #fbbf24);
  top: 40%;
  left: 35%;
  opacity: 0.25;
  animation-delay: -6s;
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(14, 165, 233, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30px) scale(1.05); }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  width: 100%;
  max-width: 1100px;
  min-height: 640px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 32px 80px rgba(14, 165, 233, 0.15);
  overflow: hidden;
}

/* 左侧品牌区 */
.login-brand {
  position: relative;
  padding: 56px;
  background: linear-gradient(145deg, rgba(224, 242, 254, 0.6) 0%, rgba(236, 253, 245, 0.5) 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 36px;
  cursor: pointer;
  width: fit-content;
}

.brand-logo img {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(14, 165, 233, 0.25);
}

.brand-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--qq-text);
  line-height: 1.1;
}

.brand-subtitle {
  font-size: 12px;
  color: var(--qq-text-secondary);
  letter-spacing: 1px;
  margin-top: 2px;
}

.brand-slogan {
  font-size: 36px;
  font-weight: 800;
  color: var(--qq-text);
  line-height: 1.2;
  margin: 0 0 16px;
}

.gradient-text {
  background: var(--qq-gradient-hero);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-desc {
  font-size: 15px;
  color: var(--qq-text-secondary);
  line-height: 1.7;
  margin: 0 0 32px;
  max-width: 380px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--qq-text);
  font-weight: 500;
}

.feature-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.feature-dot.blue { background: #0ea5e9; }
.feature-dot.green { background: #10b981; }
.feature-dot.orange { background: #f97316; }
.feature-dot.purple { background: #8b5cf6; }

.brand-card-float {
  position: absolute;
  right: -30px;
  bottom: 60px;
  z-index: 2;
}

.mini-card {
  width: 220px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 16px 40px rgba(14, 165, 233, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.8);
  animation: float 5s ease-in-out infinite;
}

.mini-card-head {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
}

.mini-card-head span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
}

.mini-card-head span:nth-child(1) { background: #f87171; }
.mini-card-head span:nth-child(2) { background: #fbbf24; }
.mini-card-head span:nth-child(3) { background: #34d399; }

.mini-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.mini-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
}

.mini-icon.blue { background: linear-gradient(135deg, #0ea5e9, #38bdf8); }

.mini-label {
  font-size: 11px;
  color: var(--qq-text-muted);
}

.mini-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--qq-text);
}

.mini-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 50px;
}

.mini-bar {
  flex: 1;
  background: #e2e8f0;
  border-radius: 3px;
}

.mini-bar.active {
  background: linear-gradient(180deg, #0ea5e9, #10b981);
}

/* 右侧表单区 */
.login-form-card {
  padding: 56px 52px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 800;
  color: var(--qq-text);
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: var(--qq-text-secondary);
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  padding: 4px 14px;
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--qq-primary) inset, 0 0 0 4px rgba(14, 165, 233, 0.1);
}

.login-form :deep(.el-input__inner) {
  height: 44px;
  font-size: 14px;
}

.login-form :deep(.el-input__icon) {
  color: var(--qq-text-muted);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 20px;
  font-size: 13px;
}

.forgot-link {
  color: var(--qq-primary);
  text-decoration: none;
  font-weight: 500;
}

.forgot-link:hover {
  color: var(--qq-primary-dark);
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: var(--qq-gradient-primary);
  border: none;
  box-shadow: 0 8px 20px rgba(14, 165, 233, 0.3);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.35);
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--qq-text-secondary);
}

.register-link {
  color: var(--qq-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.register-link:hover {
  color: var(--qq-primary-dark);
}

/* 响应式 */
@media (max-width: 960px) {
  .login-wrapper {
    grid-template-columns: 1fr;
    max-width: 480px;
  }

  .login-brand {
    display: none;
  }

  .login-form-card {
    padding: 40px 32px;
  }
}
</style>
