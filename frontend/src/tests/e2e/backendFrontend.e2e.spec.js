/**
 * @vitest-environment jsdom
 *
 * This is an API-level end-to-end flow: the frontend's real Axios wrappers
 * talk to a running backend, with no mocked HTTP layer.
 */
import { afterEach, describe, expect, it } from 'vitest'

import {
  addCartItem,
  createCategory,
  createOrder,
  createProduct,
  createShop,
  createUserAddress,
  getCart,
  getOrderDetail,
  loginCustomer,
  loginMerchant,
  payOrder,
  registerCustomer,
  registerMerchant,
  updateProduct,
  updateProductSku,
  updateStoreAddress,
  updateStoreStatus,
  uploadProductImage,
} from '../../api'

const createdTokens = []

afterEach(() => {
  localStorage.removeItem('access_token')
  localStorage.removeItem('user_role')
  createdTokens.length = 0
})

describe('frontend/backend checkout flow', () => {
  it('registers both roles, publishes a SKU, checks out, and pays an order', async () => {
    const suffix = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    const merchantCredentials = {
      account: `e2e-merchant-${suffix}`,
      password: 'ExamplePass123!',
      passwordConfirm: 'ExamplePass123!',
      name: 'E2E 商家',
      phone: '13900000000',
    }
    const userCredentials = {
      account: `e2e-user-${suffix}`,
      password: 'ExamplePass123!',
      passwordConfirm: 'ExamplePass123!',
      nickname: 'E2E 用户',
      phone: '13800000000',
    }

    await registerMerchant(merchantCredentials)
    const merchantSession = await loginMerchant({
      account: merchantCredentials.account,
      password: merchantCredentials.password,
    })
    expect(merchantSession.roles).toContain('MERCHANT')
    localStorage.setItem('access_token', merchantSession.accessToken)
    localStorage.setItem('user_role', 'MERCHANT')
    createdTokens.push(merchantSession.accessToken)

    const shop = await createShop({ name: `E2E 店铺 ${suffix}`, description: '联调测试店铺' })
    await updateStoreAddress(shop.id, { region: '杭州', detail: '学院路 1 号', phone: '05711234567' })
    const openShop = await updateStoreStatus(shop.id, { status: 'OPEN' })
    expect(openShop.status).toBe('OPEN')

    const category = await createCategory(shop.id, { name: `E2E 分类 ${suffix}`, sortOrder: 1 })
    const imageBytes = new Uint8Array([82, 73, 70, 70, 0, 0, 0, 0, 87, 69, 66, 80])
    const imageForm = new FormData()
    imageForm.append('file', new Blob([imageBytes], { type: 'image/webp' }), 'e2e.webp')
    const image = await uploadProductImage(imageForm)
    const product = await createProduct({
      shopId: shop.id,
      categoryId: category.id,
      name: `E2E 商品 ${suffix}`,
      description: '联调商品',
      imageId: image.id,
      skus: [{ name: '默认规格', price: 18.8, stock: 4 }],
    })
    const published = await updateProduct(product.id, { status: 'ON_SALE' })
    const sku = published.skus[0]
    const onSaleSku = await updateProductSku(sku.id, { status: 'ON_SALE', version: sku.version })

    await registerCustomer(userCredentials)
    const userSession = await loginCustomer({
      account: userCredentials.account,
      password: userCredentials.password,
    })
    expect(userSession.roles).toContain('USER')
    localStorage.setItem('access_token', userSession.accessToken)
    localStorage.setItem('user_role', 'USER')
    createdTokens.push(userSession.accessToken)

    const address = await createUserAddress({
      recipient: 'E2E 用户',
      phone: '13800000000',
      region: '杭州',
      detail: '学院路 2 号',
      isDefault: true,
    })
    const cartItem = await addCartItem({ skuId: onSaleSku.id, quantity: 2 })
    expect(cartItem.id).toBeGreaterThan(0)
    expect((await getCart()).items).toHaveLength(1)

    const order = await createOrder(
      { items: [{ cartItemId: cartItem.id, skuVersion: onSaleSku.version }], addressId: address.id, remark: '少放辣椒' },
      `order-${suffix}`,
    )
    expect(order.status).toBe('PENDING_PAYMENT')
    expect(order.lines[0].skuId).toBe(onSaleSku.id)

    const paid = await payOrder(order.id, `payment-${suffix}`)
    expect(paid.status).toBe('PAID')
    expect(paid.paymentStatus).toBe('PAID')
    expect((await getOrderDetail(order.id)).id).toBe(order.id)
  })
})
