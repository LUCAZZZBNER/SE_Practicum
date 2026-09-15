import http from './http'

export function uploadProductImage(formData) {
  return http.post('/files/images', formData)
}
