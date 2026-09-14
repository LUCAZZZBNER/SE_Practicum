import http from './http'

export function listProducts(shopId, params) {
  return http.get(`/shops/${shopId}/products`, { params })
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

export function createProductSku(productId, data) {
  return http.post(`/products/${productId}/skus`, data)
}

export function updateProductSku(skuId, data) {
  return http.patch(`/skus/${skuId}`, data)
}
