import request from './request'

// ============================================================
// 认证接口（管理端共用 /v1/auth）
// ============================================================

/** 登录 */
export function login(data: { username: string; password: string }): Promise<LoginResult> {
  return request.post('/v1/auth/login', data)
}

/** 登出 */
export function logout(): Promise<void> {
  return request.post('/v1/auth/logout')
}

/** 刷新 Token */
export function refreshToken(refreshToken: string): Promise<LoginResult> {
  return request.post('/v1/auth/refresh', { refreshToken })
}
