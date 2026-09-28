import request from './request'

// ============================================================
// 认证相关接口
// 接口前缀：/api/v1/auth
// ============================================================

/** 用户注册 */
export function register(data) {
  return request.post('/v1/auth/register', data)
}

/** 注册 AI 预审（模拟，不落库不建号） */
export function aiReview(data) {
  return request.post('/v1/auth/register/ai-review', data)
}

/** 高校库列表（注册学历核验白名单，模拟） */
export function getSchools() {
  return request.get('/v1/auth/schools')
}

/** 学生证照片 AI 识别（模拟） */
export function studentCardOcr(data) {
  return request.post('/v1/auth/student-card/ocr', data)
}

/** 用户登录 */
export function login(data) {
  return request.post('/v1/auth/login', data)
}

/** 刷新 Token */
export function refreshToken(refreshToken) {
  return request.post('/v1/auth/refresh', { refreshToken })
}

/** 登出 */
export function logout() {
  return request.post('/v1/auth/logout')
}
