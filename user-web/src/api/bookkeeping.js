import request from './request'

// ============================================================
// 创业经营赋能模块接口
// 接口前缀：/api/v1/bookkeeping
// B-1 记账列表 + 现金流报表
// ============================================================

/** B-1 记账列表 */
export function getBookkeepingList(params) {
  return request.get('/v1/bookkeeping/records', { params })
}

/** B-1 新增记账记录 */
export function addBookkeepingRecord(data) {
  return request.post('/v1/bookkeeping/records', data)
}

/** B-1 生成/获取现金流报表 */
export function getCashFlowReport(params) {
  return request.get('/v1/bookkeeping/cashflow-report', { params })
}

/** 模块3/4×贷款联动预警聚合（现金流/预算/高频借贷等未处理预警 + 在贷余额 + 影响提示） */
export function getLoanLinkedWarnings() {
  return request.get('/v1/operation/loan-linked/warnings')
}

/** 观察期·经营数据回流进度（三指标加权看板） */
export function getObservationProgress() {
  return request.get('/v1/loan/observation-progress')
}

/** 数据回流达标一键申请转A */
export function applyPromotion() {
  return request.post('/v1/loan/observation/apply-promotion')
}

