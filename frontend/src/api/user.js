import http from './http'

export function loginCustomer(data) {
  return http.post('/users/login', data)
}

export function registerCustomer(data) {
  return http.post('/users', data)
}

export function loginMerchant(data) {
  return http.post('/merchants/login', data)
}

export function registerMerchant(data) {
  return http.post('/merchants', data)
}

export function getProfile() {
  return http.get('/users/me')
}

export function updateProfile(data) {
  return http.patch('/users/me', data)
}
