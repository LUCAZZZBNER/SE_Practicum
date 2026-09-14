import { describe, expect, it } from 'vitest'
import customerRoutes from '../../router/modules/customer'

describe('customer routes', () => {
  it('defines one protected customer route layer', () => {
    const root = customerRoutes[0]

    expect(root.path).toBe('/customer')
    expect(root.redirect).toBe('/customer/stores')
    expect(root.meta).toEqual({ requiresAuth: true, role: 'USER' })
    expect(root.component).toEqual(expect.any(Function))
  })

  it.each([
    ['stores', 'CustomerStores', '店铺浏览'],
    ['stores/:id', 'CustomerStoreDetail', '店铺详情'],
    ['products/:id', 'CustomerProductDetail', '商品详情'],
    ['cart', 'CustomerCart', '购物车'],
    ['orders', 'CustomerOrders', '我的订单'],
    ['orders/:id', 'CustomerOrderDetail', '订单详情'],
    ['addresses', 'CustomerAddresses', '收货地址'],
    ['profile', 'CustomerProfile', '个人信息'],
  ])('registers customer page %s', (path, name, title) => {
    const route = customerRoutes[0].children.find((item) => item.path === path)

    expect(route).toBeTruthy()
    expect(route.name).toBe(name)
    expect(route.meta.title).toBe(title)
    expect(route.component).toEqual(expect.any(Function))
  })
})
