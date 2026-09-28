import request from './request'

// ============================================================
// 模拟支付中台 API（模块0）
// ============================================================

// 钱包
export const getWallet = () => request.get('/v1/pay/wallet')
export const rechargeWallet = (data) => request.post('/v1/pay/wallet/recharge', data)
export const setPayPassword = (password) => request.post(`/v1/pay/wallet/password?password=${password}`)
export const getWalletTransactions = (params) => request.get('/v1/pay/wallet/transactions', { params })

// 订单 / 收银台
export const consumeOrder = (data) => request.post('/v1/pay/consume', data)
export const getOrders = (params) => request.get('/v1/pay/orders', { params })
export const getOrder = (orderNo) => request.get(`/v1/pay/orders/${orderNo}`)
export const payOrder = (orderNo, data) => request.post(`/v1/pay/orders/${orderNo}/pay`, data)
export const closeOrder = (orderNo) => request.post(`/v1/pay/orders/${orderNo}/close`)
export const refundOrder = (orderNo, reason) =>
  request.post(`/v1/pay/orders/${orderNo}/refund?reason=${encodeURIComponent(reason || '')}`)

// 商户（青创集市消费）
export const getPayMerchants = () => request.get('/v1/loan/merchants')
