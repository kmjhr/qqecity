import request from './request'

// ============================================================
// 青年成长信用画像接口
// 接口前缀：/api/v1/profile
// 步骤 7 已实现：三场景聚合 + 三维评分 + 联动提额
// ============================================================

/** 查询青年成长信用画像 */
export function getCreditProfile() {
  return request.get('/v1/profile')
}

/** 应用联动提额（演示级） */
export function applyLinkage() {
  return request.post('/v1/profile/linkage/apply')
}
