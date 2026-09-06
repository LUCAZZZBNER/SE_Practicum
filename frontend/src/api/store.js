import http from './http'

export function listStores(params) {
  return http.get('/shops', { params })
}

export function createStore(data) {
  return http.post('/shops', data)
}

export function getStoreDetail(storeId) {
  return http.get(`/shops/${storeId}`)
}

export function updateStoreStatus(storeId, data) {
  return http.patch(`/shops/${storeId}`, data)
}

export function updateStore(storeId, data) {
  return http.patch(`/shops/${storeId}`, data)
}

export function listCategories(shopId) {
  return http.get(`/shops/${shopId}/categories`)
}

export function createCategory(shopId, data) {
  return http.post(`/shops/${shopId}/categories`, data)
}

export function updateCategory(categoryId, data) {
  return http.patch(`/categories/${categoryId}`, data)
}

export function deleteCategory(categoryId) {
  return http.delete(`/categories/${categoryId}`)
}
