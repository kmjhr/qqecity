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
