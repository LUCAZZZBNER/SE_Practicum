import { describe, expect, it } from 'vitest'
import customerRoutes from '../../router/modules/customer'

describe('customer routes', () => {
  it('exposes the protected customer address page', () => {
    const route = customerRoutes[0].children.find((item) => item.path === 'addresses')

    expect(route).toBeTruthy()
    expect(route.name).toBe('CustomerAddresses')
    expect(route.meta.title).toBe('收货地址')
    expect(route.component).toEqual(expect.any(Function))
  })
})
