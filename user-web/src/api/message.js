import request from './request'

// ============================================================
// 消息中心接口
// 接口前缀：/api/v1/message
// ============================================================

/** 分页查询消息列表 */
export function getMessagePage(params) {
  return request.get('/v1/message/page', { params })
}

/** 标记单条消息已读 */
export function markRead(id) {
  return request.put(`/v1/message/${id}/read`)
}

/** 全部标记已读 */
export function markAllRead() {
  return request.put('/v1/message/read-all')
}

/** 获取未读消息数量 */
export function getUnreadCount() {
  return request.get('/v1/message/unread-count')
}
