import request from '../request'

// ============================================================
// 管理端 - 保函 AI 复审队列（步骤 8 缺口 #22）
// 接口前缀：/api/v1/admin/guarantee + /v1/guarantee
// 复审操作复用既有 /v1/guarantee/{id}/manual-review
// ============================================================

/** 保函人工复审队列（MANUAL_REVIEW 状态全量分页） */
export function getGuaranteeManualReviewQueue(params: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/guarantee/manual-review-queue', { params })
}

/** banker 人工复审保函申请（复用既有接口） */
export function manualReviewGuarantee(id: number, data: {
  decision: 'APPROVED' | 'REJECTED'
  rejectReason?: string
}): Promise<any> {
  // 既有接口为 RequestParam 形式，使用 query 传参
  return request.put(`/v1/guarantee/${id}/manual-review`, null, {
    params: data
  })
}

// ============================================================
// 管理端 - 保函管理（全量申请 + 代房东确认）
// 接口前缀：/api/v1/admin/guarantee
// ============================================================

/** 保函申请全量分页（status 为空查全部，SUBMITTED=待房东确认） */
export function getGuaranteeApplications(params: {
  pageNum?: number
  pageSize?: number
  status?: string
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/guarantee/applications', { params })
}

/** 代房东确认（银行/运营代操作，触发 AI 复审） */
export function adminLandlordConfirm(id: number, signContent?: string): Promise<any> {
  return request.put(`/v1/admin/guarantee/${id}/landlord-confirm`, null, {
    params: signContent ? { signContent } : {}
  })
}
