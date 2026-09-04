import { describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => {
  const routerInstances = []

  return {
    routerInstances,
  }
})

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => {
    const router = {
      beforeEach: vi.fn(),
    }
    mocks.routerInstances.push(router)
    return router
  }),
  createWebHistory: vi.fn(() => ({})),
}))

vi.mock('../../router/modules/auth', () => ({ default: [{ path: '/' }] }))
vi.mock('../../router/modules/customer', () => ({ default: [{ path: '/customer' }] }))
vi.mock('../../router/modules/merchant', () => ({ default: [{ path: '/merchant' }] }))
vi.mock('../../router/modules/acceptance', () => ({ default: [{ path: '/acceptance' }] }))

import router from '../../router'

describe('router integration', () => {
  it('installs guards on the exported router instance', () => {
    expect(mocks.routerInstances).toHaveLength(1)
    expect(router.beforeEach).toHaveBeenCalledTimes(1)
  })
})
