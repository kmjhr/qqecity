import request from './request'

// ============================================================
// 创业经营赋能模块接口（占位）
// 接口前缀：/api/v1/bookkeeping
// ============================================================

/** 获取记账列表 */
export function getBookkeepingList(params) {
  return request.get('/v1/bookkeeping/list', { params })
}

/** 获取现金流报表 */
export function getCashFlowReport() {
  return request.get('/v1/bookkeeping/cashflow-report')
}
