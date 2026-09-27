import request from '../request'

// ============================================================
// 管理端 - 索赔复核队列（步骤 8 缺口 #22）
// 复用既有 /v1/guarantee/claims 接口
// ============================================================

/** 索赔人工复核队列（MANUAL_REVIEW 状态全量分页） */
export function getClaimManualReviewQueue(params: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<any>> {
  return request.get('/v1/guarantee/claims/manual-review-queue', { params })
}

/** banker 人工复核索赔（APPROVED 赔付 / REJECTED 拒绝） */
export function reviewClaim(id: number, data: {
  decision: 'APPROVED' | 'REJECTED'
  payoutAmount?: number
  rejectReason?: string
  reviewNote?: string
}): Promise<any> {
  return request.put(`/v1/guarantee/claims/${id}/review`, data)
}
