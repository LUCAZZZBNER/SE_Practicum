import http from './http'

export function createUserAddress(data) {
  return http.post('/user-addresses', data)
}

export function listUserAddresses() {
  return http.get('/user-addresses')
}

export function updateUserAddress(addressId, data) {
  return http.patch(`/user-addresses/${addressId}`, data)
}

export function removeUserAddress(addressId) {
  return http.delete(`/user-addresses/${addressId}`)
}
