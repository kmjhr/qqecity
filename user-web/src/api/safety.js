import request from './request'

// ============================================================
// 青年金融安全模块接口（占位）
// 接口前缀：/api/v1/safety
// ============================================================

/** 骗局甄别 */
export function detectScam(data) {
  return request.post('/v1/safety/detect-scam', data)
}

/** 获取反诈教学内容 */
export function getAntiFraudContent() {
  return request.get('/v1/safety/anti-fraud')
}
