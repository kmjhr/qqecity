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

/** 管理端可索赔保函列表（ADMIN/banker 代房东发起索赔选择保函） */
export function getClaimableGuarantees(): Promise<any[]> {
  return request.get('/v1/guarantee/claims/claimable-guarantees')
}

/** 代房东发起索赔（后台角色，自动触发 AI 初审） */
export function submitClaim(data: {
  guaranteeId: number
  claimAmount: number
  claimReason: string
  evidenceFiles?: string
}): Promise<any> {
  return request.post('/v1/guarantee/claims', data)
}

/** 待房东确认的退租留档列表（照片合格留档，未确认无需索赔） */
export function getPendingLandlordConfirm(params: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<any>> {
  return request.get('/v1/guarantee/moveout/pending-confirm', { params })
}

/** 代房东确认无需索赔（完美结束） */
export function landlordConfirmMoveout(id: number, remark?: string): Promise<any> {
  return request.post(`/v1/guarantee/moveout/${id}/landlord-confirm`, { remark })
}
