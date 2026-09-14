import { describe, expect, it } from 'vitest'
import merchantRoutes from '../../router/modules/merchant'

describe('merchant routes', () => {
  it('defines one protected merchant route layer', () => {
    const root = merchantRoutes[0]

    expect(root.path).toBe('/merchant')
    expect(root.redirect).toBe('/merchant/store')
    expect(root.meta).toEqual({ requiresAuth: true, role: 'MERCHANT' })
    expect(root.component).toEqual(expect.any(Function))
  })

  it.each([
    ['store', 'MerchantStore', '店铺管理'],
    ['products', 'MerchantProducts', '商品管理'],
    ['orders', 'MerchantOrders', '店铺订单'],
    ['orders/:id', 'MerchantOrderDetail', '订单详情'],
    ['profile', 'MerchantProfile', '个人信息'],
  ])('registers merchant page %s', (path, name, title) => {
    const route = merchantRoutes[0].children.find((item) => item.path === path)

    expect(route).toBeTruthy()
    expect(route.name).toBe(name)
    expect(route.meta.title).toBe(title)
    expect(route.component).toEqual(expect.any(Function))
  })
})
