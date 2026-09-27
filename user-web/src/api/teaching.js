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
