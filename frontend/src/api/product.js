import http from './http'

export function listProducts(params) {
  return http.get('/products', { params })
}

export function getProductDetail(productId) {
  return http.get(`/products/${productId}`)
}

export function createProduct(data) {
  return http.post('/products', data)
}

export function updateProduct(productId, data) {
  return http.patch(`/products/${productId}`, data)
}
