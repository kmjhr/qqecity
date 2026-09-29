import request from './request'

// ============================================================
// 管理端 - 房东管理接口（房东分区）
// 接口前缀：/api/v1/admin/landlord
// ============================================================

export interface LandlordInfo {
  id: number
  userId: number
  username: string
  realName: string
  idCard: string
  phone: string
  bankAccount: string
  bankName: string
  verifyStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
  houseCount: number
  status: number
  createTime: string
}

/** 分页查询房东 */
export function getLandlordPage(params: {
  pageNum: number
  pageSize: number
  keyword?: string
  verifyStatus?: string
}): Promise<PageResult<LandlordInfo>> {
  return request.get('/v1/admin/landlord/page', { params })
}

/** 房东详情 */
export function getLandlordById(id: number): Promise<LandlordInfo> {
  return request.get(`/v1/admin/landlord/${id}`)
}

/** 编辑房东信息 */
export function updateLandlord(id: number, data: Partial<LandlordInfo>): Promise<void> {
  return request.put(`/v1/admin/landlord/${id}`, data)
}

/** 认证裁决 */
export function verifyLandlord(id: number, verifyStatus: string): Promise<void> {
  return request.put(`/v1/admin/landlord/${id}/verify`, null, { params: { verifyStatus } })
}

/** 删除房东 */
export function deleteLandlord(id: number): Promise<void> {
  return request.delete(`/v1/admin/landlord/${id}`)
}
