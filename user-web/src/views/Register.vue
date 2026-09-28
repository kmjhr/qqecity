<template>
  <div class="register-page">
    <!-- 动态背景 -->
    <div class="register-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="grid"></div>
    </div>

    <div class="register-wrapper">
      <!-- 左侧品牌区 -->
      <div class="register-brand">
        <div class="brand-content">
          <div class="brand-logo" @click="$router.push('/home')">
            <img src="/logo-icon.jpg" alt="青启e城" />
            <div>
              <div class="brand-title">青启e城</div>
              <div class="brand-subtitle">QINGQI eCity</div>
            </div>
          </div>
          <h1 class="brand-slogan">
            加入青启e城
            <span class="gradient-text">开启城市新生活</span>
          </h1>
          <p class="brand-desc">
            专为在校大学生、毕业2年内青年、青年创业者打造的金融智能服务平台，注册需通过 AI 智能审核（模拟）。
          </p>

          <div class="register-steps">
            <div class="step-item">
              <div class="step-num">1</div>
              <div>
                <div class="step-title">填写资料</div>
                <div class="step-desc">基础信息 + 实名 + 人群类型</div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">2</div>
              <div>
                <div class="step-title">AI 智能审核</div>
                <div class="step-desc">模拟学历 / 身份 / 人群资质核验</div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">3</div>
              <div>
                <div class="step-title">完成注册</div>
                <div class="step-desc">通过后即可登录使用全部服务</div>
              </div>
            </div>
          </div>
        </div>

        <div class="brand-card-float">
          <div class="mini-card">
            <div class="mini-card-head">
              <span></span><span></span><span></span>
            </div>
            <div class="mini-card-body">
              <div class="ai-review-demo">
                <div class="ai-icon"><el-icon><MagicStick /></el-icon></div>
                <div>
                  <div class="mini-label">AI 智能审核</div>
                  <div class="mini-value">模拟 · 通过</div>
                </div>
              </div>
              <div class="check-list">
                <div class="check-item"><el-icon><CircleCheckFilled /></el-icon><span>白名单人群</span></div>
                <div class="check-item"><el-icon><CircleCheckFilled /></el-icon><span>学历核验</span></div>
                <div class="check-item"><el-icon><CircleCheckFilled /></el-icon><span>身份查重</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="register-form-card">
        <div class="form-scroll">
          <div class="form-header">
            <h2>注册新账号</h2>
            <p>填写真实信息，AI 审核通过后即可加入</p>
          </div>

          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="register-form"
            label-position="top"
          >
            <el-divider content-position="left">基础信息</el-divider>

            <div class="form-row">
              <el-form-item prop="username" class="half">
                <template #label>用户名<span class="req">*</span></template>
                <el-input v-model="registerForm.username" placeholder="3-20位用户名" :prefix-icon="User" />
              </el-form-item>

              <el-form-item prop="phone" class="half">
                <template #label>手机号<span class="req">*</span></template>
                <el-input v-model="registerForm.phone" placeholder="用于同一材料查重" :prefix-icon="Phone" />
              </el-form-item>
            </div>

            <div class="form-row">
              <el-form-item prop="password" class="half">
                <template #label>密码<span class="req">*</span></template>
                <el-input v-model="registerForm.password" type="password" placeholder="6-20位" :prefix-icon="Lock" show-password />
              </el-form-item>

              <el-form-item prop="confirmPassword" class="half">
                <template #label>确认密码<span class="req">*</span></template>
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password />
              </el-form-item>
            </div>

            <el-divider content-position="left">实名与人群（AI 审核）</el-divider>

            <div class="form-row">
              <el-form-item prop="realName" class="half">
                <template #label>真实姓名<span class="req">*</span></template>
                <el-input v-model="registerForm.realName" placeholder="与身份证一致" :prefix-icon="Postcard" />
              </el-form-item>

              <el-form-item prop="idCard" class="half">
                <template #label>身份证号<span class="req">*</span></template>
                <el-input v-model="registerForm.idCard" placeholder="用于同一人查重（仅存哈希）" :prefix-icon="CreditCard" maxlength="18" />
              </el-form-item>
            </div>

            <el-form-item prop="userType">
              <template #label>人群类型<span class="req">*</span>（白名单：在校大学生 / 毕业2年内 / 青年创业者）</template>
              <el-radio-group v-model="registerForm.userType" class="user-type-group">
                <el-radio-button value="STUDENT">在校大学生</el-radio-button>
                <el-radio-button value="GRADUATE">毕业2年内</el-radio-button>
                <el-radio-button value="ENTREPRENEUR">青年创业者</el-radio-button>
              </el-radio-group>
              <div class="whitelist-tip">仅白名单人群可注册（在校大学生 / 毕业2年内 / 青年创业者），非白名单不可注册</div>
            </el-form-item>

            <template v-if="needEducation">
              <el-divider content-position="left">学历核验（AI 学历审查·模拟）</el-divider>

              <div class="form-row">
                <el-form-item prop="school" class="half">
                  <template #label>学校<span class="req">*</span>（须命中高校库）</template>
                  <el-select v-model="registerForm.school" filterable placeholder="选择学校" style="width: 100%">
                    <el-option v-for="s in schools" :key="s.id" :label="s.schoolName" :value="s.schoolName" />
                  </el-select>
                </el-form-item>

                <el-form-item prop="educationLevel" class="half">
                  <template #label>学历层次<span class="req">*</span></template>
                  <el-select v-model="registerForm.educationLevel" placeholder="选择学历" style="width: 100%">
                    <el-option label="本科" value="UNDERGRADUATE" />
                    <el-option label="硕士" value="MASTER" />
                    <el-option label="博士" value="DOCTOR" />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item prop="graduationDate">
                <template #label>
                  毕业日期<span class="req">*</span>
                  <span v-if="registerForm.userType === 'GRADUATE'" class="tip">（须毕业2年内）</span>
                  <span v-else class="tip">（在校生，须未毕业）</span>
                </template>
                <el-date-picker
                  v-model="registerForm.graduationDate"
                  type="date"
                  placeholder="选择毕业日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item prop="verifyType">
                <template #label>核验方式<span class="req">*</span>（模拟）</template>
                <el-radio-group v-model="registerForm.verifyType">
                  <el-radio-button value="XUE_XIN_WANG">方式A · 学信网在线核验（模拟）</el-radio-button>
                  <el-radio-button v-if="registerForm.userType === 'GRADUATE'" value="GRAD_CERT">方式B · 毕业证照片识别（模拟）</el-radio-button>
                  <el-radio-button v-else value="STUDENT_CARD">方式B · 学生证照片识别（模拟）</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item prop="studentNo">
                <template #label>
                  <span v-if="registerForm.verifyType === 'XUE_XIN_WANG'">学信档案验证码<span class="req">*</span>（模拟）</span>
                  <span v-else-if="registerForm.verifyType === 'GRAD_CERT'">毕业证编号<span class="req">*</span>（模拟识别回填）</span>
                  <span v-else>学号<span class="req">*</span>（模拟识别回填）</span>
                </template>
                <div class="student-no-row">
                  <el-input
                    v-model="registerForm.studentNo"
                    :placeholder="registerForm.verifyType === 'XUE_XIN_WANG' ? '请输入学信档案在线验证码' : (registerForm.verifyType === 'GRAD_CERT' ? '请输入毕业证编号' : '请输入学号')"
                  />
                  <el-button v-if="registerForm.verifyType === 'STUDENT_CARD' || registerForm.verifyType === 'GRAD_CERT'" :loading="ocrLoading" @click="handleMockOcr">
                    {{ registerForm.verifyType === 'GRAD_CERT' ? '模拟上传毕业证识别' : '模拟上传学生证识别' }}
                  </el-button>
                </div>
              </el-form-item>
              <div v-if="ocrMessage" class="ocr-message">
                <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                <span>{{ ocrMessage }}</span>
              </div>
            </template>

            <!-- AI 智能审核结果面板 -->
            <el-card v-if="reviewResult" class="review-panel" :class="reviewResult.passed ? 'review-pass' : 'review-fail'" shadow="never">
              <template #header>
                <div class="review-header">
                  <el-icon :size="18" :color="reviewResult.passed ? '#67c23a' : '#f56c6c'">
                    <component :is="reviewResult.passed ? 'SuccessFilled' : 'CircleCloseFilled'" />
                  </el-icon>
                  <span>AI 智能审核结果（模拟）{{ reviewResult.passed ? '· 通过' : '· 未通过' }}</span>
                  <span class="review-no">审核编号 {{ reviewResult.reviewNo }}</span>
                </div>
              </template>
              <ul class="review-items">
                <li v-for="item in reviewResult.items" :key="item.code">
                  <el-icon :size="16" :color="item.pass ? '#67c23a' : '#f56c6c'">
                    <component :is="item.pass ? 'SuccessFilled' : 'CircleCloseFilled'" />
                  </el-icon>
                  <div class="item-body">
                    <div class="item-name">{{ item.name }}</div>
                    <div class="item-detail">{{ item.detail }}</div>
                  </div>
                </li>
              </ul>
            </el-card>

            <el-button type="primary" size="large" class="ai-btn" :loading="reviewLoading" @click="handleAiReview">
              <el-icon style="margin-right: 6px"><MagicStick /></el-icon>
              AI 智能审核（模拟）
            </el-button>

            <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="loading"
              :disabled="!reviewPassed"
              @click="handleRegister"
            >
              注 册
            </el-button>
            <div v-if="!reviewPassed" class="register-tip">请先通过 AI 智能审核后再注册</div>

            <div class="register-footer">
              <span>已有账号？</span>
              <router-link to="/login" class="login-link">去登录</router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Phone, Postcard, CreditCard,
  SuccessFilled, CircleCloseFilled, MagicStick, CircleCheckFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { aiReview, getSchools, studentCardOcr } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)
const reviewLoading = ref(false)
const ocrLoading = ref(false)

const schools = ref([])
const reviewResult = ref(null)
const ocrMessage = ref('')

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  userType: '',
  realName: '',
  idCard: '',
  school: '',
  educationLevel: '',
  graduationDate: '',
  verifyType: '',
  studentNo: ''
})

/** STUDENT / GRADUATE 需要学历核验 */
const needEducation = computed(() =>
  registerForm.userType === 'STUDENT' || registerForm.userType === 'GRADUATE')

/** 切换人群类型时重置核验方式（学生证仅在校生 / 毕业证仅毕业2年内，避免残留无效值） */
watch(() => registerForm.userType, () => {
  registerForm.verifyType = ''
  registerForm.studentNo = ''
  ocrMessage.value = ''
})

/** AI 审核是否通过（通过后才允许注册） */
const reviewPassed = computed(() => !!reviewResult.value?.passed)

onMounted(async () => {
  try {
    schools.value = await getSchools()
  } catch (e) {
    // 高校库拉取失败不阻塞注册
  }
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
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择人群类型', trigger: 'change' }],
  school: [{ required: true, message: '请选择学校', trigger: 'change' }],
  educationLevel: [{ required: true, message: '请选择学历层次', trigger: 'change' }],
  graduationDate: [{ required: true, message: '请选择毕业日期', trigger: 'change' }],
  verifyType: [{ required: true, message: '请选择核验方式', trigger: 'change' }],
  studentNo: [{ required: true, message: '请输入学信档案验证码/学号/毕业证编号', trigger: 'blur' }]
}

/** 组装提交/预审数据 */
function buildPayload() {
  return {
    username: registerForm.username,
    password: registerForm.password,
    phone: registerForm.phone,
    email: registerForm.email,
    userType: registerForm.userType,
    realName: registerForm.realName,
    idCard: registerForm.idCard,
    school: registerForm.school,
    educationLevel: registerForm.educationLevel,
    graduationDate: registerForm.graduationDate,
    verifyType: registerForm.verifyType,
    studentNo: registerForm.studentNo
  }
}

/** AI 智能审核（模拟预审，不落库） */
async function handleAiReview() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async valid => {
    if (!valid) return
    reviewLoading.value = true
    try {
      reviewResult.value = await aiReview(buildPayload())
      if (reviewResult.value.passed) {
        ElMessage.success('AI 智能审核通过（模拟），可点击注册')
      } else {
        ElMessage.warning(reviewResult.value.rejectReason)
      }
    } catch (e) {
      // 错误已在拦截器中提示
    } finally {
      reviewLoading.value = false
    }
  })
}

/** 学生证/毕业证照片 AI 识别（模拟） */
async function handleMockOcr() {
  ocrLoading.value = true
  const isGradCert = registerForm.verifyType === 'GRAD_CERT'
  try {
    const data = await studentCardOcr({
      demoSchool: registerForm.school || '中山大学'
    })
    registerForm.school = data.school
    registerForm.studentNo = data.studentNo
    ocrMessage.value = `已模拟识别：${data.school} · ${isGradCert ? '毕业证编号' : '学号'} ${data.studentNo}（置信度 ${data.confidence}%）`
    ElMessage.success(isGradCert ? '毕业证照片识别完成（模拟）' : '学生证照片识别完成（模拟）')
  } catch (e) {
    // 错误已在拦截器中提示
  } finally {
    ocrLoading.value = false
  }
}

/** 正式注册（后端再次执行 AI 审核，通过才落库） */
async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      const data = await userStore.register(buildPayload())
      ElMessage.success(`注册成功，AI 审核编号 ${data.reviewNo}（模拟）`)
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
.register-page {
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

.register-bg {
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

.register-wrapper {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  width: 100%;
  max-width: 1200px;
  height: calc(100vh - 80px);
  max-height: 860px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 32px 80px rgba(14, 165, 233, 0.15);
  overflow: hidden;
}

/* 左侧品牌区 */
.register-brand {
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
  font-size: 34px;
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
  margin: 0 0 36px;
  max-width: 380px;
}

.register-steps {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.step-num {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #fff;
  color: var(--qq-primary);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15);
  flex-shrink: 0;
}

.step-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--qq-text);
  margin-bottom: 2px;
}

.step-desc {
  font-size: 13px;
  color: var(--qq-text-secondary);
}

.brand-card-float {
  position: absolute;
  right: -30px;
  bottom: 50px;
  z-index: 2;
}

.mini-card {
  width: 230px;
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

.ai-review-demo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.ai-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
}

.mini-label {
  font-size: 11px;
  color: var(--qq-text-muted);
}

.mini-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--qq-text);
}

.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--qq-text-secondary);
}

.check-item .el-icon {
  color: #10b981;
  font-size: 14px;
}

/* 右侧表单区 */
.register-form-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.form-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 48px 52px;
  box-sizing: border-box;
}

.form-scroll::-webkit-scrollbar {
  width: 6px;
}

.form-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.form-header {
  margin-bottom: 28px;
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

.register-form :deep(.el-input__wrapper),
.register-form :deep(.el-textarea__inner),
.register-form :deep(.el-select .el-input__wrapper),
.register-form :deep(.el-date-editor.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.3s;
}

.register-form :deep(.el-input__wrapper.is-focus),
.register-form :deep(.el-select .el-input.is-focus .el-input__wrapper),
.register-form :deep(.el-date-editor.el-input__wrapper.is-active) {
  box-shadow: 0 0 0 1px var(--qq-primary) inset, 0 0 0 4px rgba(14, 165, 233, 0.1);
}

.register-form :deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
}

.register-form :deep(.el-input__icon) {
  color: var(--qq-text-muted);
}

.register-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--qq-text);
  font-weight: 600;
  padding-bottom: 6px;
}

.register-form :deep(.el-divider__text) {
  font-size: 13px;
  font-weight: 700;
  color: var(--qq-text-secondary);
  background: #fff;
}

.register-form :deep(.el-button + .el-button) {
  margin-left: 0;
}

.register-form :deep(.el-radio-button__inner) {
  font-size: 13px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .half {
  flex: 1;
  min-width: 0;
}

.req {
  color: #f56c6c;
  margin-left: 2px;
}

.tip {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}

.user-type-group {
  width: 100%;
}

.whitelist-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.user-type-group :deep(.el-radio-button) {
  flex: 1;
}

.user-type-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.student-no-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.student-no-row .el-input {
  flex: 1;
}

.ocr-message {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
  font-size: 13px;
  margin-top: -12px;
  margin-bottom: 6px;
}

.review-panel {
  margin: 4px 0 12px;
  border-radius: 12px;
}

.review-pass {
  border: 1px solid #67c23a;
}

.review-fail {
  border: 1px solid #f56c6c;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.review-no {
  margin-left: auto;
  font-size: 12px;
  font-weight: normal;
  color: #909399;
}

.review-items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.review-items li {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.review-items li:last-child {
  border-bottom: none;
}

.review-items li .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.item-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.item-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  line-height: 1.5;
}

.ai-btn {
  width: 100%;
  margin-top: 4px;
  margin-left: 0;
  border-radius: 12px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
  border: none;
  box-shadow: 0 8px 20px rgba(139, 92, 246, 0.3);
}

.ai-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(139, 92, 246, 0.35);
}

.register-btn {
  width: 100%;
  margin-top: 12px;
  margin-left: 0 !important;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: var(--qq-gradient-primary);
  border: none;
  box-shadow: 0 8px 20px rgba(14, 165, 233, 0.3);
}

.register-btn:disabled {
  background: linear-gradient(135deg, #93c5fd, #a5c8f7);
  color: #fff;
  border: none;
  opacity: 0.9;
}

.register-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.35);
}

.register-tip {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-top: 18px;
}

.login-link {
  color: var(--qq-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.login-link:hover {
  color: var(--qq-primary-dark);
}

/* 响应式 */
@media (max-width: 960px) {
  .register-wrapper {
    grid-template-columns: 1fr;
    max-width: 560px;
    height: auto;
    max-height: none;
  }

  .register-brand {
    display: none;
  }

  .form-scroll {
    padding: 32px;
    overflow-y: visible;
  }
}

@media (max-width: 560px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .form-scroll {
    padding: 24px 20px;
  }
}
</style>
