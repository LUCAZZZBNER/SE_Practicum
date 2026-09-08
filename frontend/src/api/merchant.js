import http from './http'

export function getMerchantProfile() {
  return http.get('/merchants/me')
}

export function updateMerchantProfile(data) {
  return http.patch('/merchants/me', data)
}
