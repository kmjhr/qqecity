import request from '../request'

// ============================================================
// 管理端 - 注册审核记录（白名单 AI 审核留痕）
// 接口前缀：/api/v1/admin/registration-reviews
// ============================================================

/** 注册审核记录分页（keyword 模糊：用户名/姓名/手机号/审核编号；result: APPROVED/REJECTED） */
export function getRegistrationReviews(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  result?: string
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/registration-reviews', { params })
}
