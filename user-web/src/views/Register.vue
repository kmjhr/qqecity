<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>注册新账号</h2>
        <p>加入青启e城，开启城市新生活 · 注册需通过 AI 智能审核（模拟）</p>
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

        <el-form-item prop="nickname">
          <template #label>昵称</template>
          <el-input v-model="registerForm.nickname" placeholder="选填" :prefix-icon="Avatar" />
        </el-form-item>

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
            <el-radio-button value="OTHER">其他（非白名单）</el-radio-button>
          </el-radio-group>
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
              <el-radio-button value="STUDENT_CARD">方式B · 学生证照片识别（模拟）</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item prop="studentNo">
            <template #label>
              <span v-if="registerForm.verifyType === 'XUE_XIN_WANG'">学信档案验证码<span class="req">*</span>（模拟）</span>
              <span v-else>学号<span class="req">*</span>（模拟识别回填）</span>
            </template>
            <div class="student-no-row">
              <el-input
                v-model="registerForm.studentNo"
                :placeholder="registerForm.verifyType === 'XUE_XIN_WANG' ? '请输入学信档案在线验证码' : '请输入学号'"
              />
              <el-button v-if="registerForm.verifyType === 'STUDENT_CARD'" :loading="ocrLoading" @click="handleMockOcr">
                模拟上传学生证识别
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

        <el-button type="primary" plain size="large" class="ai-btn" :loading="reviewLoading" @click="handleAiReview">
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
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Phone, Avatar, Postcard, CreditCard,
  SuccessFilled, CircleCloseFilled, MagicStick
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
  nickname: '',
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
  studentNo: [{ required: true, message: '请输入学信档案验证码/学号', trigger: 'blur' }]
}

/** 组装提交/预审数据 */
function buildPayload() {
  return {
    username: registerForm.username,
    password: registerForm.password,
    nickname: registerForm.nickname,
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

/** 学生证照片 AI 识别（模拟） */
async function handleMockOcr() {
  ocrLoading.value = true
  try {
    const data = await studentCardOcr({
      demoSchool: registerForm.school || '中山大学'
    })
    registerForm.school = data.school
    registerForm.studentNo = data.studentNo
    ocrMessage.value = `已模拟识别：${data.school} · 学号 ${data.studentNo}（置信度 ${data.confidence}%）`
    ElMessage.success('学生证照片识别完成（模拟）')
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
.register-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 30px 0;
  box-sizing: border-box;
}

.register-card {
  width: 620px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-header {
  text-align: center;
  margin-bottom: 10px;
}

.register-header h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.register-header p {
  font-size: 13px;
  color: #909399;
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
  border-radius: 8px;
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
}

.register-btn {
  width: 100%;
  margin-top: 12px;
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
  margin-top: 16px;
}

.login-link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
</style>
