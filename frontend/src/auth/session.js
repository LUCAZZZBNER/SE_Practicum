const TOKEN_KEY = 'access_token'
const ROLE_KEY = 'user_role'
const SHOP_KEY = 'merchant_shop_id'

export function getSession() {
  return {
    token: localStorage.getItem(TOKEN_KEY),
    role: localStorage.getItem(ROLE_KEY),
  }
}

export function saveSession(session) {
  localStorage.setItem(TOKEN_KEY, session.accessToken)
  localStorage.setItem(ROLE_KEY, session.roles?.[0] || '')
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(SHOP_KEY)
}

export function markSelectedShop(shopId) {
  if (shopId == null) localStorage.removeItem(SHOP_KEY)
  else localStorage.setItem(SHOP_KEY, String(shopId))
}

export function getSelectedShopId() {
  const value = Number(localStorage.getItem(SHOP_KEY))
  return Number.isInteger(value) && value > 0 ? value : null
}
