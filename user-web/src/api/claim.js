import request from './request'

// ============================================================
// 违约索赔模块接口（G-6）
// 接口前缀：/api/v1/guarantee/claims
// 状态机：SUBMITTED → AI_REVIEW →（速赔 APPROVED → CLOSED |
//         存疑 DEFENSE_PERIOD → MANUAL_REVIEW → APPROVED/REJECTED → CLOSED）
// 所有银行能力（赔付）均为模拟桩，标注"模拟"
// ============================================================

/** G-6-1 房东发起索赔（自动触发 AI 初审） */
export function submitClaim(data) {
  return request.post('/v1/guarantee/claims', data)
}

/** G-6-5 分页查询索赔列表（房东看发起、租客看被索赔） */
export function getClaimPage(params) {
  return request.get('/v1/guarantee/claims', { params })
}

/** G-6-5 查询索赔详情 */
export function getClaimDetail(id) {
  return request.get(`/v1/guarantee/claims/${id}`)
}

/** G-6-3 租客提交申辩 */
export function submitDefense(id, data) {
  return request.put(`/v1/guarantee/claims/${id}/defense`, data)
}

/** 索赔状态流转说明 */
export function getClaimStatusFlow() {
  return request.get('/v1/guarantee/claims/status-flow')
}

/** G-6 房东名下保函列表（索赔入口选择保函） */
export function getLandlordGuarantees() {
  return request.get('/v1/guarantee/landlord/guarantees')
}
