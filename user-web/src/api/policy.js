import request from './request'

// ============================================================
// 政策智能匹配模块接口
// 接口前缀：/api/v1/policy
// 步骤 2 已实现：人才安居 + 创业贴息政策库
// ============================================================

/** 按当前用户资质匹配政策（全部政策 + matched 标注） */
export function matchPolicies() {
  return request.get('/v1/policy/match')
}

/** 仅返回匹配当前用户的政策 */
export function matchedPolicies() {
  return request.get('/v1/policy/matched')
}

/** 按类型筛选政策：HOUSING-安居 / ENTREPRENEUR-创业贴息 */
export function listPolicies(policyType) {
  return request.get('/v1/policy', { params: policyType ? { policyType } : {} })
}

/** 政策详情 */
export function getPolicyDetail(id) {
  return request.get(`/v1/policy/${id}`)
}

/** 一键推送匹配政策到消息中心 */
export function pushPolicies() {
  return request.post('/v1/policy/push')
}
