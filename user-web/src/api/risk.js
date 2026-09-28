import request from './request'

// ============================================================
// 风险预警聚合接口（用户端）
// 接口前缀：/api/v1/{各模块}
// 聚合：逾期风险 + 高频借贷 + 征信异常 + 预算超支 + 现金流预警
// ============================================================

/** 风险预警总览（一次返回 5 类 + 未处理数，直查共享表 biz_risk_warning） */
export function getRiskOverview() {
  return request.get('/v1/risk/overview')
}

/** 历史逾期风险预警 */
export function getOverdueRiskWarnings() {
  return request.get('/v1/safety/overdue-risk/warnings')
}

/** 历史高频借贷预警 */
export function getHighFreqBorrowWarnings() {
  return request.get('/v1/consumption/high-freq-borrow/history')
}

/** 历史征信异常预警 */
export function getCreditAbnormalWarnings() {
  return request.get('/v1/consumption/credit-monitor/warnings')
}

/** 历史现金流预警 */
export function getCashflowWarnings() {
  return request.get('/v1/operation/cashflow-warning/history')
}

/** 触发逾期风险预判 */
export function predictOverdueRisk(aheadDays = 7) {
  return request.post('/v1/safety/overdue-risk/predict', null, { params: { aheadDays } })
}

/** 触发高频借贷检测 */
export function detectHighFreqBorrow() {
  return request.post('/v1/consumption/high-freq-borrow/detect')
}

/** 触发现金流预警检测 */
export function detectCashflowWarning() {
  return request.post('/v1/operation/cashflow-warning/detect')
}
