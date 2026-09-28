import request from '../request'

// ============================================================
// 管理端 - 贷款审批（步骤 8 缺口 #22）
// 接口前缀：/api/v1/admin/loan
// ============================================================

/** 贷款申请审批队列（全量分页） */
export function getLoanApplications(params: {
  pageNum?: number
  pageSize?: number
  status?: string
  loanType?: string
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/loan/applications', { params })
}

/** 审批贷款申请 */
export function reviewLoanApplication(id: number, data: {
  decision: 'APPROVED' | 'REJECTED' | 'RETURNED'
  rejectReason?: string
  returnReason?: string
  approveAmount?: number
  remark?: string
}): Promise<any> {
  return request.put(`/v1/admin/loan/applications/${id}/review`, data)
}

/** A 类提款流水（全量分页） */
export function getCreditTxns(params: {
  pageNum?: number
  pageSize?: number
  txnType?: string
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/loan/credit-txns', { params })
}

/** B 类受托支付流水（全量分页） */
export function getEntrustPayments(params: {
  pageNum?: number
  pageSize?: number
  paymentStatus?: string
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/loan/entrust-payments', { params })
}

/** 商户白名单列表（按状态） */
export function getMerchantsByStatus(verifyStatus?: string): Promise<any[]> {
  return request.get('/v1/admin/merchants', {
    params: verifyStatus ? { verifyStatus } : {}
  })
}

/** banker 审核商户白名单 */
export function auditMerchant(id: number, verifyStatus: 'VERIFIED' | 'REJECTED', reason?: string): Promise<any> {
  return request.put(`/v1/admin/merchants/${id}/audit`, null, {
    params: { verifyStatus, ...(reason ? { reason } : {}) }
  })
}

/** 受托支付复核单队列（自定义商户每单复核） */
export function getEntrustReviews(status?: string): Promise<any[]> {
  return request.get('/v1/admin/entrust-reviews', {
    params: status ? { status } : {}
  })
}

/** banker 复核受托支付（通过并放款 / 驳回） */
export function auditEntrustReview(id: number, approve: boolean, reason?: string): Promise<any> {
  return request.put(`/v1/admin/entrust-reviews/${id}/audit`, null, {
    params: { approve, ...(reason ? { reason } : {}) }
  })
}
