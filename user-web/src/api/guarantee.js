import request from './request'

// ============================================================
// 安居保函模块接口（占位）
// 接口前缀：/api/v1/guarantee
// ============================================================

/** 获取保函列表 */
export function getGuaranteeList(params) {
  return request.get('/v1/guarantee/list', { params })
}

/** 获取保函详情 */
export function getGuaranteeDetail(id) {
  return request.get(`/v1/guarantee/${id}`)
}

/** 提交保函申请 */
export function applyGuarantee(data) {
  return request.post('/v1/guarantee/apply', data)
}
