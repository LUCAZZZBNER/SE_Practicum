import http from './http'

export function listStores(params) {
  return http.get('/stores', { params })
}

export function getStoreDetail(storeId) {
  return http.get(`/stores/${storeId}`)
}

export function updateStoreStatus(storeId, data) {
  return http.patch(`/stores/${storeId}/status`, data)
}
