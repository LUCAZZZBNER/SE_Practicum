import http from './http'

export function createOrder(data, config = {}) {
  return http.post('/orders', data, config)
}

export function listOrders(params) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderId) {
  return http.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId) {
  return http.post(`/orders/${orderId}/cancel`)
}

export function listMerchantOrders(params) {
  return http.get('/merchant/orders', { params })
}

export function getMerchantOrderDetail(orderId) {
  return http.get(`/merchant/orders/${orderId}`)
}
