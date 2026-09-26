import request from './request'

// ============================================================
// 认证相关接口
// 接口前缀：/api/v1/auth
// ============================================================

/** 用户注册 */
export function register(data) {
  return request.post('/v1/auth/register', data)
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
