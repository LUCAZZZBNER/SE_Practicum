import http from './http'

export function uploadProductImage(formData) {
  return http.post('/files/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
