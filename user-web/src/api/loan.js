import request from './request'

// ============================================================
// 轻创业智能授信模块接口
// 接口前缀：/api/v1/loan
// L-0 产品规则 → L-1 预审 → L-2 双轨额度 → L-3 受托支付
// ============================================================

/** L-0 青创e贷 A/B 双轨产品规则与风险揭示 */
export function getProductRules() {
  return request.get('/v1/loan/product-rules')
}

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

/** 提交自定义商户申请（进入灰名单，管理端 banker 审核） */
export function applyMerchant(data) {
  return request.post('/v1/loan/merchants/apply', data)
}

/** 我的自定义商户申请列表（PENDING/VERIFIED/REJECTED） */
export function getMyMerchants() {
  return request.get('/v1/loan/merchants/my')
}

/** 打款记录（受托支付成功流水 + 自定义商户复核单） */
export function getEntrustRecords() {
  return request.get('/v1/loan/entrust-records')
}

/** 分页查询我的贷款申请 */
export function getLoanApplications(params) {
  return request.get('/v1/loan/applications', { params })
}

/** 查询贷款申请详情 */
export function getLoanApplicationDetail(id) {
  return request.get(`/v1/loan/applications/${id}`)
}

/** A类循环贷提款（随借随还） */
export function withdrawCredit(data) {
  return request.post('/v1/loan/withdraw', data)
}

/** A类循环贷还款 */
export function createRepayOrder(creditType, data) {
  return request.post(`/v1/loan/repay-order?creditType=${creditType}`, data)
}
export function repayCredit(data) {
  return request.post('/v1/loan/repay', data)
}

/** 还款试算预览（A/B双轨：待还本金/计息天数/预估利息/合计） */
export function getRepayPreview(creditType) {
  return request.get('/v1/loan/repay-preview', { params: { creditType } })
}

/** B类受托支付还款（还定向贷款本金+利息） */
export function entrustRepay(data) {
  return request.post('/v1/loan/entrust-repay', data)
}

/** 查询循环贷流水（提款/还款明细） */
export function getCreditTxns() {
  return request.get('/v1/loan/credit-txns')
}

/** 观察期·经营数据回流进度（模块3/4联动看板） */
export function getObservationProgress() {
  return request.get('/v1/loan/observation-progress')
}

/** 数据回流达标一键申请转A */
export function applyPromotion() {
  return request.post('/v1/loan/observation/apply-promotion')
}

/** 还款保障金概览（模块4×贷款联动） */
export function getRepayGuard() {
  return request.get('/v1/budget/repay-guard')
}
