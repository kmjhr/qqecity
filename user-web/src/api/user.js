import request from './request'

// ============================================================
// 用户端 - 个人信息接口
// 接口前缀：/api/v1/user
// ============================================================

/** 获取当前用户信息 */
export function getProfile() {
  return request.get('/v1/user/profile')
}

/** 修改个人信息 */
export function updateProfile(data) {
  return request.put('/v1/user/profile', data)
}

/** 修改密码 */
export function changePassword(data) {
  return request.put('/v1/user/password', data)
}
