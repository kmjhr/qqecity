import request from './request'

// ============================================================
// 青年金融安全模块接口
// 接口前缀：/api/v1/safety
// S-1 反诈内容 → S-2 骗局甄别
// ============================================================

/** S-1 反诈内容列表 */
export function getAntiFraudList(params) {
  return request.get('/v1/safety/anti-fraud/list', { params })
}

/** S-1 反诈内容详情 */
export function getAntiFraudDetail(id) {
  return request.get(`/v1/safety/anti-fraud/${id}`)
}

/** S-2 骗局甄别（话术命中即拦截警示并落库） */
export function detectFraud(data) {
  return request.post('/v1/safety/fraud-detect', data)
}
