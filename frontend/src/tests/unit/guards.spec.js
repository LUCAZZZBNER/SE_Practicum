import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAuthState, installRouteGuards, resolveNavigation } from '../../router/guards'

describe('router guards', () => {
  beforeEach(() => {
    localStorage.clear()
    document.title = '初始标题'
  })

  it('reads token and role from localStorage', () => {
    localStorage.setItem('access_token', 'token-abc')
    localStorage.setItem('user_role', 'USER')

    expect(getAuthState()).toEqual({
      token: 'token-abc',
      role: 'USER',
    })
  })

  it('keeps public pages accessible without token', () => {
    const result = resolveNavigation({ path: '/', fullPath: '/' }, { token: null, role: null })

    expect(result).toBeNull()
  })

  it('redirects anonymous users to home with redirect query when entering protected pages', () => {
    const result = resolveNavigation(
      { path: '/customer/orders', fullPath: '/customer/orders' },
      { token: null, role: null },
    )

    expect(result).toEqual({
      path: '/',
      query: { redirect: '/customer/orders' },
    })
  })

  it('redirects logged-in users away from auth pages to the matching dashboard', () => {
    const customerResult = resolveNavigation(
      { path: '/login/customer', fullPath: '/login/customer' },
      { token: 'token-1', role: 'USER' },
    )
    const merchantResult = resolveNavigation(
      { path: '/register/merchant', fullPath: '/register/merchant' },
      { token: 'token-2', role: 'MERCHANT' },
    )

    expect(customerResult).toBe('/customer/stores')
    expect(merchantResult).toBe('/merchant/store')
  })

  it('blocks user role from merchant routes and merchant role from customer routes', () => {
    const userToMerchant = resolveNavigation(
      { path: '/merchant/products', fullPath: '/merchant/products' },
      { token: 'token-1', role: 'USER' },
    )
    const merchantToCustomer = resolveNavigation(
      { path: '/customer/cart', fullPath: '/customer/cart' },
      { token: 'token-2', role: 'MERCHANT' },
    )

    expect(userToMerchant).toBe('/customer/stores')
    expect(merchantToCustomer).toBe('/merchant/store')
  })

  it('updates document title for navigable protected pages', () => {
    localStorage.setItem('access_token', 'token-abc')
    localStorage.setItem('user_role', 'USER')

    const beforeEachSpy = vi.fn()
    const router = { beforeEach: beforeEachSpy }

    installRouteGuards(router)

    const guard = beforeEachSpy.mock.calls[0][0]
    const result = guard(
      { path: '/customer/profile', meta: { title: '个人信息' } },
      { path: '/customer/stores' },
    )

    expect(result).toBe(true)
    expect(document.title).toBe('个人信息 - 轻量级外卖服务平台')
  })

  it('returns login redirect before title update when route is protected and anonymous', () => {
    const beforeEachSpy = vi.fn()
    const router = { beforeEach: beforeEachSpy }

    installRouteGuards(router)

    const guard = beforeEachSpy.mock.calls[0][0]
    const result = guard(
      { path: '/customer/profile', fullPath: '/customer/profile', meta: { title: '个人信息' } },
      { path: '/customer/stores' },
    )

    expect(result).toEqual({
      path: '/',
      query: { redirect: '/customer/profile' },
    })
    expect(document.title).toBe('初始标题')
  })
})
