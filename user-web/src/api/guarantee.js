import request from './request'

// ============================================================
// 安居金融风控模块接口
// 接口前缀：/api/v1/guarantee
// 状态：申请中→待确认→待缴费→已开立→已失效
// ============================================================

/** G-1 提交保函申请 */
export function applyGuarantee(data) {
  return request.post('/v1/guarantee/apply', data)
}

/** G-2 房东在线确认与电子签署 */
export function landlordConfirm(id) {
  return request.put(`/v1/guarantee/${id}/landlord-confirm`)
}

/** G-4 缴纳保函费并开立电子保函 */
export function payGuarantee(id) {
  return request.post(`/v1/guarantee/${id}/pay`)
}

/** G-5 分页查询保函申请列表 */
export function getGuaranteePage(params) {
  return request.get('/v1/guarantee/page', { params })
}

/** G-5 查询保函申请详情 */
export function getGuaranteeDetail(id) {
  return request.get(`/v1/guarantee/${id}`)
}

/** G-5 查询电子保函详情 */
export function getGuaranteeLetter(id) {
  return request.get(`/v1/guarantee/guarantee/${id}`)
}

/** 保函状态流转说明 */
export function getStatusFlow() {
  return request.get('/v1/guarantee/status-flow')
}

/** 租客名下已开立保函列表（退租留档选函用） */
export function getMyGuarantees() {
  return request.get('/v1/guarantee/mine')
}

/** 退租留档提交（上传房屋照片，AI 合格审核模拟） */
export function submitMoveoutRecord(data) {
  return request.post('/v1/guarantee/moveout/record', data)
}

/** 我的退租留档记录（分页） */
export function getMoveoutRecords(params) {
  return request.get('/v1/guarantee/moveout/records', { params })
}
