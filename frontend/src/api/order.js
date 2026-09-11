import http from './http'

function withIdempotencyKey(idempotencyKey) {
  return { headers: { 'X-Idempotency-Key': idempotencyKey } }
}

export function createOrder(data, idempotencyKey) {
  return http.post('/orders', data, withIdempotencyKey(idempotencyKey))
}

export function listOrders(params) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderId) {
  return http.get(`/orders/${orderId}`)
}

export function payOrder(orderId, idempotencyKey) {
  return http.post(`/orders/${orderId}/pay`, {}, withIdempotencyKey(idempotencyKey))
}

export function cancelOrder(orderId, data, idempotencyKey) {
  return http.post(`/orders/${orderId}/cancel`, data, withIdempotencyKey(idempotencyKey))
}

export function confirmOrderReceipt(orderId, idempotencyKey) {
  return http.post(
    `/orders/${orderId}/confirm-receipt`,
    {},
    withIdempotencyKey(idempotencyKey),
  )
}

export function getOrderRefund(orderId) {
  return http.get(`/orders/${orderId}/refund`)
}

export function listMerchantOrders(params) {
  return http.get('/merchant/orders', { params })
}

export function getMerchantOrderDetail(orderId) {
  return http.get(`/merchant/orders/${orderId}`)
}

export function prepareMerchantOrder(orderId, idempotencyKey) {
  return http.post(
    `/merchant/orders/${orderId}/prepare`,
    {},
    withIdempotencyKey(idempotencyKey),
  )
}

export function deliverMerchantOrder(orderId, idempotencyKey) {
  return http.post(
    `/merchant/orders/${orderId}/deliver`,
    {},
    withIdempotencyKey(idempotencyKey),
  )
}
