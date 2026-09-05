import http from './http'

export function getCart() {
  return http.get('/cart')
}

export function addCartItem(data) {
  return http.post('/cart/items', data)
}

export function updateCartItem(cartItemId, data) {
  return http.patch(`/cart/items/${cartItemId}`, data)
}

export function removeCartItem(cartItemId) {
  return http.delete(`/cart/items/${cartItemId}`)
}
