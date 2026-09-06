import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearSession,
  getSession,
  saveSession,
} from '../../auth/session'

beforeEach(() => localStorage.clear())

describe('session storage', () => {
  it('saves and reads the backend authentication session', () => {
    saveSession({ accessToken: 'token-1', roles: ['MERCHANT'] })

    expect(getSession()).toEqual({ token: 'token-1', role: 'MERCHANT' })
  })

  it('clears authentication and selected merchant shop together', () => {
    localStorage.setItem('access_token', 'token-1')
    localStorage.setItem('user_role', 'MERCHANT')
    localStorage.setItem('merchant_shop_id', '7')

    clearSession()

    expect(getSession()).toEqual({ token: null, role: null })
    expect(localStorage.getItem('merchant_shop_id')).toBeNull()
  })
})
