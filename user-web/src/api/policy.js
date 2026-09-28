import request from './request'

// ============================================================
// 政策智能匹配模块接口（全国政策库）
// 接口前缀：/api/v1/policy
// 支持地区筛选：region=浙江/广东/全国...
// ============================================================

/** 按当前用户资质匹配政策（全部政策 + matched 标注，自动推送） */
export function matchPolicies(region) {
  return request.get('/v1/policy/match', { params: region ? { region } : {} })
}

/** 仅返回匹配当前用户的政策 */
export function matchedPolicies(region) {
  return request.get('/v1/policy/matched', { params: region ? { region } : {} })
}

/** 按类型/地区筛选政策：HOUSING-安居 / ENTREPRENEUR-创业贴息 */
export function listPolicies(policyType, region) {
  return request.get('/v1/policy', {
    params: { ...(policyType ? { policyType } : {}), ...(region ? { region } : {}) }
  })
}

/** 政策详情 */
export function getPolicyDetail(id) {
  return request.get(`/v1/policy/${id}`)
}

/** 一键推送匹配政策到消息中心 */
export function pushPolicies(region) {
  return request.post('/v1/policy/push', null, { params: region ? { region } : {} })
}
