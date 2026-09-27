import request from './request'

// ============================================================
// 轻创业智能授信模块接口
// 接口前缀：/api/v1/loan
// L-1 预审 → L-2 双轨额度 → L-3 受托支付
// ============================================================

/** L-1 B类免费预审（不查征信） */
export function preCheck(data) {
  return request.post('/v1/loan/precheck', data)
}

/** L-2 查询我的A/B双轨授信额度 */
export function getCreditLimit() {
  return request.get('/v1/loan/credit')
}

/** L-3 受托支付 */
export function entrustPayment(data) {
  return request.post('/v1/loan/entrust-pay', data)
}

/** 查询受托支付商户列表 */
export function getMerchants() {
  return request.get('/v1/loan/merchants')
}

/** 分页查询我的贷款申请 */
export function getLoanApplications(params) {
  return request.get('/v1/loan/applications', { params })
}

/** 查询贷款申请详情 */
export function getLoanApplicationDetail(id) {
  return request.get(`/v1/loan/applications/${id}`)
}
