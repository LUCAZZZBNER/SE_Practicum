import http from './http'

export function listProducts(shopId, params) {
  return http.get(`/shops/${shopId}/products`, { params })
}

export function getProductDetail(productId, params) {
  return http.get(`/products/${productId}`, { params })
}

export function createProduct(data) {
  return http.post('/products', data)
}

export function updateProduct(productId, data) {
  return http.patch(`/products/${productId}`, data)
}
