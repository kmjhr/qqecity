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

// ============================================================
// 演示模式：人工审核自动通过开关
// 接口：GET/PUT /api/v1/admin/demo-auto-approve
// ============================================================

export interface DemoAutoApproveStatus {
  enabled: boolean
  desc: string
}

export function getDemoAutoApprove(): Promise<DemoAutoApproveStatus> {
  return request.get('/v1/admin/demo-auto-approve')
}

export function setDemoAutoApprove(enabled: boolean): Promise<DemoAutoApproveStatus> {
  return request.put('/v1/admin/demo-auto-approve', { enabled })
}
