import request from './request'

// ============================================================
// AI 对话引擎接口（双模式：local / agent）
// 接口前缀：/api/v1/chat
// 步骤 7 已实现：local 默认离线 + agent LLM 增强（需 API Key）
// API Key 由用户在项目根 .env 自行填写，Key 空自动禁用 agent
// ============================================================

/** 查询对话引擎状态（local/agent 模式 + AI 增强是否开启） */
export function getEngineStatus() {
  return request.get('/v1/chat/engine-status')
}

/** 发送消息（mode 可选：local / agent，不传则用服务端默认） */
export function sendMessage(content, mode) {
  return request.post('/v1/chat/messages', mode ? { message: content, mode } : { message: content })
}

/** 查询历史对话（最近 20 条） */
export function getChatHistory() {
  return request.get('/v1/chat/history')
}

/** 清空历史对话 */
export function clearChatHistory() {
  return request.delete('/v1/chat/history')
}
