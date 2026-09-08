import http from './http'

export function listCategories(shopId, params) {
  return http.get(`/shops/${shopId}/categories`, { params })
}

export function createCategory(shopId, data) {
  return http.post(`/shops/${shopId}/categories`, data)
}

export function updateCategory(categoryId, data) {
  return http.patch(`/categories/${categoryId}`, data)
}

export function removeCategory(categoryId) {
  return http.delete(`/categories/${categoryId}`)
}
