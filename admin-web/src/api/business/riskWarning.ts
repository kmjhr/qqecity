import request from '../request'

// ============================================================
// 管理端 - 风险预警总览（步骤 8 缺口 #22）
// 接口前缀：/api/v1/admin/risk-warnings
// ============================================================

/** 风险预警全量分页查询 */
export function getRiskWarnings(params: {
  pageNum?: number
  pageSize?: number
  warningType?: string
  warningLevel?: string
  isHandled?: number
}): Promise<PageResult<any>> {
  return request.get('/v1/admin/risk-warnings', { params })
}

/** 标记预警已处理 */
export function handleRiskWarning(id: number, data: {
  handleNote: string
}): Promise<any> {
  return request.put(`/v1/admin/risk-warnings/${id}/handle`, data)
}
