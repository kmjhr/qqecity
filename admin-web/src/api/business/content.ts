import request from '../request'

// ============================================================
// 管理端 - 内容管理（安全教育平台 + 政策专区）
// ① 实时预警：/api/v1/admin/safety/alerts
// ② 反诈教学：/api/v1/admin/safety/contents
// ③ 政策门户：/api/v1/admin/policy/portals
// ============================================================

// ---------- ① 实时预警（biz_anti_fraud_alert） ----------

export function listAlerts(): Promise<any[]> {
  return request.get('/v1/admin/safety/alerts')
}

export function createAlert(data: any): Promise<any> {
  return request.post('/v1/admin/safety/alerts', data)
}

export function updateAlert(id: number, data: any): Promise<any> {
  return request.put(`/v1/admin/safety/alerts/${id}`, data)
}

export function toggleAlert(id: number, status: number): Promise<any> {
  return request.put(`/v1/admin/safety/alerts/${id}/status`, null, { params: { status } })
}

export function deleteAlert(id: number): Promise<any> {
  return request.delete(`/v1/admin/safety/alerts/${id}`)
}

// ---------- ② 反诈教学（biz_anti_fraud_content） ----------

export function listContents(): Promise<any[]> {
  return request.get('/v1/admin/safety/contents')
}

export function createContent(data: any): Promise<any> {
  return request.post('/v1/admin/safety/contents', data)
}

export function updateContent(id: number, data: any): Promise<any> {
  return request.put(`/v1/admin/safety/contents/${id}`, data)
}

export function toggleContent(id: number, status: number): Promise<any> {
  return request.put(`/v1/admin/safety/contents/${id}/status`, null, { params: { status } })
}

export function deleteContent(id: number): Promise<any> {
  return request.delete(`/v1/admin/safety/contents/${id}`)
}

// ---------- ③ 政策门户（biz_policy_portal） ----------

export function listPortals(params?: { portalType?: string; region?: string }): Promise<any[]> {
  return request.get('/v1/admin/policy/portals', { params })
}

export function createPortal(data: any): Promise<any> {
  return request.post('/v1/admin/policy/portals', data)
}

export function updatePortal(id: number, data: any): Promise<any> {
  return request.put(`/v1/admin/policy/portals/${id}`, data)
}

export function togglePortal(id: number, status: string): Promise<any> {
  return request.put(`/v1/admin/policy/portals/${id}/status`, null, { params: { status } })
}

export function deletePortal(id: number): Promise<any> {
  return request.delete(`/v1/admin/policy/portals/${id}`)
}
