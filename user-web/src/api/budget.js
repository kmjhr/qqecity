import request from './request'

// ============================================================
// 碎片消费治理模块接口
// 接口前缀：/api/v1/budget
// C-1 预算设置 → C-2 交易扣减 → C-3 三级提醒 → C-4 结余转储蓄
// ============================================================

/** 获取预算分类列表 */
export function getCategories() {
  return request.get('/v1/budget/categories')
}

/** C-1 设置/更新分类预算 */
export function saveBudgetSetting(data) {
  return request.post('/v1/budget/setting', data)
}

/** 获取当月预算列表（含提醒级别） */
export function getBudgetList(params) {
  return request.get('/v1/budget/list', { params })
}

/** C-2 新增模拟交易（按 MCC 自动归类扣减预算） */
export function addTransaction(data) {
  return request.post('/v1/budget/transaction', data)
}

/** C-4 结余一键转入心愿储蓄 */
export function transferToSaving(data) {
  return request.post('/v1/budget/transfer-saving', data)
}

/** 获取心愿储蓄列表 */
export function getSavings() {
  return request.get('/v1/budget/savings')
}

/** 预算概览 */
export function getBudgetOverview() {
  return request.get('/v1/budget/overview')
}
