import http from './http'

export function listStores(params) {
  return http.get('/shops', { params })
}

export function getStoreDetail(storeId) {
  return http.get(`/shops/${storeId}`)
}

export function createShop(data) {
  return http.post('/shops', data)
}

export function updateStoreStatus(storeId, data) {
  return http.patch(`/shops/${storeId}`, data)
}

export function updateStoreAddress(storeId, data) {
  return http.patch(`/shops/${storeId}/address`, data)
}
