import { describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('../../api/http', () => ({
  default: {
    get: mocks.get,
    post: mocks.post,
    patch: mocks.patch,
    delete: mocks.delete,
  },
}))

import {
  addCartItem,
  getCart,
  removeCartItem,
  updateCartItem,
} from '../../api/cart'
import {
  createCategory,
  listCategories,
  removeCategory,
  updateCategory,
} from '../../api/category'
import {
  cancelOrder,
  createOrder,
  getMerchantOrderDetail,
  getOrderDetail,
  listMerchantOrders,
  listOrders,
} from '../../api/order'
import { getProductDetail, listProducts } from '../../api/product'
import { getStoreDetail, listStores, updateStoreStatus } from '../../api/store'

beforeEach(() => {
  mocks.get.mockReset()
  mocks.post.mockReset()
  mocks.patch.mockReset()
  mocks.delete.mockReset()
})

describe('api contracts', () => {
  it('uses the cart-items contract for cart APIs', () => {
    getCart()
    addCartItem({ productId: 11, quantity: 2 })
    updateCartItem(88, { quantity: 3 })
    removeCartItem(88)

    expect(mocks.get).toHaveBeenCalledWith('/cart-items')
    expect(mocks.post).toHaveBeenCalledWith('/cart-items', { productId: 11, quantity: 2 })
    expect(mocks.patch).toHaveBeenCalledWith('/cart-items/88', { quantity: 3 })
    expect(mocks.delete).toHaveBeenCalledWith('/cart-items/88')
  })

  it('uses shop-scoped product list and existing product detail routes', () => {
    listProducts(7, { page: 1 })
    getProductDetail(11)

    expect(mocks.get).toHaveBeenCalledWith('/shops/7/products', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/products/11')
  })

  it('uses the shop update contract and shop list route', () => {
    listStores({ page: 1 })
    getStoreDetail(7)
    updateStoreStatus(7, { status: 'OPEN' })

    expect(mocks.get).toHaveBeenCalledWith('/shops', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/shops/7')
    expect(mocks.patch).toHaveBeenCalledWith('/shops/7', { status: 'OPEN' })
  })

  it('keeps order APIs aligned with the new contract surface', () => {
    createOrder(
      { items: [{ cartItemId: 1, productVersion: 3 }] },
      { headers: { 'X-Idempotency-Key': 'abc-123' } },
    )
    listOrders({ page: 1 })
    getOrderDetail(1001)
    cancelOrder(1001)
    listMerchantOrders({ page: 1 })
    getMerchantOrderDetail(1001)

    expect(mocks.post).toHaveBeenCalledWith(
      '/orders',
      { items: [{ cartItemId: 1, productVersion: 3 }] },
      { headers: { 'X-Idempotency-Key': 'abc-123' } },
    )
    expect(mocks.get).toHaveBeenCalledWith('/orders', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/orders/1001')
    expect(mocks.post).toHaveBeenCalledWith('/orders/1001/cancel')
    expect(mocks.get).toHaveBeenCalledWith('/merchant/orders', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/merchant/orders/1001')
  })

  it('exposes category APIs under shop-scoped routes', () => {
    listCategories(7, { page: 1 })
    createCategory(7, { name: '主食' })
    updateCategory(3, { name: '饮品' })
    removeCategory(3)

    expect(mocks.get).toHaveBeenCalledWith('/shops/7/categories', { params: { page: 1 } })
    expect(mocks.post).toHaveBeenCalledWith('/shops/7/categories', { name: '主食' })
    expect(mocks.patch).toHaveBeenCalledWith('/categories/3', { name: '饮品' })
    expect(mocks.delete).toHaveBeenCalledWith('/categories/3')
  })
})
