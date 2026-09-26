import request from './request'

// 用户与授权中心
export const authApi = {
  register: data => request.post('/auth/register', data),
  login: data => request.post('/auth/login', data),
  me: () => request.get('/auth/me')
}

// 模块1 安居保函
export const guaranteeApi = {
  apply: data => request.post('/guarantee/apply', data),
  confirm: applyId => request.post('/guarantee/landlord/confirm', { applyId }),
  pay: applyId => request.post('/guarantee/fee/pay', { applyId }),
  list: () => request.get('/guarantee/list'),
  detail: applyId => request.get(`/guarantee/${applyId}`)
}

// 模块2 青创e贷
export const loanApi = {
  apply: data => request.post('/loan/apply', data),
  approve: applyId => request.post(`/loan/${applyId}/approve`),
  entrustedPay: (applyId, data) => request.post(`/loan/${applyId}/entrusted-pay`, data),
  list: () => request.get('/loan/list'),
  detail: applyId => request.get(`/loan/${applyId}`)
}

// 模块3 经营赋能（占位）
export const bookkeepingApi = {
  report: () => request.get('/bookkeeping/report')
}

// 模块4 碎片消费治理
export const budgetApi = {
  setBudget: data => request.post('/budget/set', data),
  expense: data => request.post('/budget/expense', data),
  carryOver: data => request.post('/budget/carry-over', data),
  list: month => request.get('/budget/list', { params: { month } })
}

// 模块5 金融安全
export const safetyApi = {
  fraudContent: () => request.get('/safety/fraud-content'),
  verifyText: text => request.post('/safety/verify-text', { text }),
  learn: data => request.post('/safety/learn', data)
}

// 公共支撑：消息中心
export const messageApi = {
  list: readStatus => request.get('/message/list', { params: { readStatus } }),
  read: msgId => request.post(`/message/${msgId}/read`)
}
