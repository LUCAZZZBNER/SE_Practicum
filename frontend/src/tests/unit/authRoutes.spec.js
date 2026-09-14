import { describe, expect, it } from 'vitest'
import authRoutes from '../../router/modules/auth'

describe('auth routes', () => {
  it.each([
    ['/login/customer', 'customer'],
    ['/login/merchant', 'merchant'],
  ])('redirects the legacy login route %s to the combined login page', (path, role) => {
    const route = authRoutes.find((item) => item.path === path)

    expect(route).toBeDefined()
    expect(route.redirect).toEqual({ path: '/', query: { role } })
    expect(route.component).toBeUndefined()
  })
})
