import request from './request'

// ============================================================
// 反诈情景化教学接口
// 接口前缀：/api/v1/safety/scenario
// 步骤 6 已实现：刷单诈骗 / 冒充公检法 / 征信洗白
// 互动问答：选错有纠偏文案
// ============================================================

/** 情景模拟内容列表（可按 category 过滤） */
export function getScenarioList(category) {
  return request.get('/v1/safety/scenario/list', { params: category ? { category } : {} })
}

/** 情景详情（含问题与选项，不暴露正确答案） */
export function getScenarioDetail(scenarioId) {
  return request.get(`/v1/safety/scenario/${scenarioId}`)
}

/** 提交答案并评分 */
export function submitScenarioAnswers(scenarioId, answers) {
  return request.post(`/v1/safety/scenario/${scenarioId}/submit`, { answers })
}

// ============================================================
// L3 对话式反诈演练（AI 扮演诈骗分子，用户自由发言对抗）
// 接口前缀：/api/v1/safety/scenario/practice/**
// ============================================================

/** 开始对话演练（情景须为 SCENARIO_DIALOG） */
export function startPractice(scenarioId) {
  return request.post(`/v1/safety/scenario/${scenarioId}/practice/start`)
}

/** 用户发言回合：AI 接招 + 安全判定 */
export function turnPractice(practiceNo, content) {
  return request.post(`/v1/safety/scenario/practice/${practiceNo}/turn`, { content })
}

/** 主动结束 / 获取复盘 */
export function finishPractice(practiceNo, reason) {
  return request.post(`/v1/safety/scenario/practice/${practiceNo}/finish`, { reason })
}

/** 我的演练历史 */
export function practiceHistory(pageNum, pageSize) {
  return request.get('/v1/safety/scenario/practice/history', { params: { pageNum, pageSize } })
}

/** 演练回放（主表 + 回合明细） */
export function practiceDetail(practiceNo) {
  return request.get(`/v1/safety/scenario/practice/${practiceNo}`)
}
