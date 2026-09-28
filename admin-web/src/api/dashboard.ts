import request from './request'

// ============================================================
// 管理端 - 数据看板统计
// 接口前缀：/api/v1/admin/dashboard/stats
// ============================================================

export interface DashboardStats {
  userCount: number
  userToday: number
  guaranteeTotal: number
  guaranteePendingConfirm: number
  guaranteeManualReview: number
  loanPending: number
  riskUnhandled: number
  registrationReviews: number
}

export function getDashboardStats(): Promise<DashboardStats> {
  return request.get('/v1/admin/dashboard/stats')
}
