import request from '../request'

// ============================================================
// 管理端 - 用户管理接口
// 接口前缀：/api/admin/v1/user
// ============================================================

/** 分页查询用户 */
export function getUserPage(params: PageParams): Promise<PageResult<UserInfo>> {
  return request.get('/admin/v1/user/page', { params })
}

/** 获取用户详情 */
export function getUserById(id: number): Promise<UserInfo> {
  return request.get(`/admin/v1/user/${id}`)
}

/** 新增用户 */
export function createUser(data: Partial<UserInfo> & { password: string }): Promise<void> {
  return request.post('/admin/v1/user', data)
}

/** 修改用户 */
export function updateUser(id: number, data: Partial<UserInfo>): Promise<void> {
  return request.put(`/admin/v1/user/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: number): Promise<void> {
  return request.delete(`/admin/v1/user/${id}`)
}
