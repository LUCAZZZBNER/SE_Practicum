import http from './http'

export function listStores(params) {
  return http.get('/shops', { params })
}

export function getStoreDetail(storeId) {
  return http.get(`/shops/${storeId}`)
}

export function updateStoreStatus(storeId, data) {
  return http.patch(`/stores/${storeId}/status`, data)
}
