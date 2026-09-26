import request from './request'

// ============================================================
// 青创e贷模块接口（占位）
// 接口前缀：/api/v1/loan
// ============================================================

/** B 类免费预审 */
export function preCheck(data) {
  return request.post('/v1/loan/pre-check', data)
}

/** 获取授信额度 */
export function getCreditLimit() {
  return request.get('/v1/loan/credit')
}

/** 受托支付申请 */
export function entrustPayment(data) {
  return request.post('/v1/loan/entrust-pay', data)
}
