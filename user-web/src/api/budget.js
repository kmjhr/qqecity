import request from './request'

// ============================================================
// 碎片消费治理模块接口（占位）
// 接口前缀：/api/v1/budget
// ============================================================

/** 获取预算设置 */
export function getBudgetSettings() {
  return request.get('/v1/budget/settings')
}

/** 保存预算设置 */
export function saveBudgetSettings(data) {
  return request.put('/v1/budget/settings', data)
}

/** 获取交易记录 */
export function getTransactions(params) {
  return request.get('/v1/budget/transactions', { params })
}

/** 结余转储蓄 */
export function transferToSaving(data) {
  return request.post('/v1/budget/transfer-saving', data)
}
