import http from './http'

export function createOrder(data) {
  return http.post('/orders', data)
}

export function listOrders(params) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderId) {
  return http.get(`/orders/${orderId}`)
}
