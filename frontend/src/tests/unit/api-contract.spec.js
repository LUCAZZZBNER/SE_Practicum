import { beforeEach, describe, expect, it, vi } from 'vitest'

const http = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('../../api/http', () => ({ default: http }))

import {
  addCartItem,
  cancelOrder,
  createCategory,
  createOrder,
  createProduct,
  createStore,
  deleteCategory,
  getCart,
  getMerchantOrderDetail,
  getOrderDetail,
  getProductDetail,
  getStoreDetail,
  listCategories,
  listMerchantOrders,
  listOrders,
  listProducts,
  listStores,
  removeCartItem,
  updateCartItem,
  updateCategory,
  updateProduct,
  updateStore,
} from '../../api'

beforeEach(() => {
  Object.values(http).forEach((mock) => mock.mockReset())
})

describe('canonical API wrappers', () => {
  it('uses canonical shop and product resources', () => {
    listStores({ page: 1 })
    createStore({ name: 'Shop' })
    getStoreDetail(7)
    updateStore(7, { status: 'OPEN' })
    listProducts(7, { includeOffSale: true })
    getProductDetail(11, { includeOffSale: true })
    createProduct({ shopId: 7 })
    updateProduct(11, { version: 2, stock: 9 })

    expect(http.get).toHaveBeenNthCalledWith(1, '/shops', { params: { page: 1 } })
    expect(http.post).toHaveBeenCalledWith('/shops', { name: 'Shop' })
    expect(http.get).toHaveBeenNthCalledWith(2, '/shops/7')
    expect(http.patch).toHaveBeenNthCalledWith(1, '/shops/7', { status: 'OPEN' })
    expect(http.get).toHaveBeenNthCalledWith(3, '/shops/7/products', {
      params: { includeOffSale: true },
    })
    expect(http.get).toHaveBeenNthCalledWith(4, '/products/11', {
      params: { includeOffSale: true },
    })
    expect(http.post).toHaveBeenCalledWith('/products', { shopId: 7 })
    expect(http.patch).toHaveBeenNthCalledWith(2, '/products/11', { version: 2, stock: 9 })
  })

  it('covers category and cart resources', () => {
    listCategories(7)
    createCategory(7, { name: 'Meals' })
    updateCategory(5, { sortOrder: 2 })
    deleteCategory(5)
    getCart()
    addCartItem({ productId: 11, quantity: 1 })
    updateCartItem(3, { quantity: 2 })
    removeCartItem(3)

    expect(http.get).toHaveBeenNthCalledWith(1, '/shops/7/categories')
    expect(http.post).toHaveBeenNthCalledWith(1, '/shops/7/categories', { name: 'Meals' })
    expect(http.patch).toHaveBeenNthCalledWith(1, '/categories/5', { sortOrder: 2 })
    expect(http.delete).toHaveBeenNthCalledWith(1, '/categories/5')
    expect(http.get).toHaveBeenNthCalledWith(2, '/cart-items')
    expect(http.post).toHaveBeenNthCalledWith(2, '/cart-items', { productId: 11, quantity: 1 })
    expect(http.patch).toHaveBeenNthCalledWith(2, '/cart-items/3', { quantity: 2 })
    expect(http.delete).toHaveBeenNthCalledWith(2, '/cart-items/3')
  })

  it('sends strict customer and merchant order requests', () => {
    const items = [{ cartItemId: 3, productVersion: 4 }]
    createOrder(items, 'checkout-key')
    listOrders({ status: 'PENDING_PAYMENT' })
    getOrderDetail(8)
    cancelOrder(8)
    listMerchantOrders({ shopId: 7 })
    getMerchantOrderDetail(8)

    expect(http.post).toHaveBeenNthCalledWith(1, '/orders', { items }, {
      headers: { 'X-Idempotency-Key': 'checkout-key' },
    })
    expect(http.get).toHaveBeenNthCalledWith(1, '/orders', {
      params: { status: 'PENDING_PAYMENT' },
    })
    expect(http.get).toHaveBeenNthCalledWith(2, '/orders/8')
    expect(http.post).toHaveBeenNthCalledWith(2, '/orders/8/cancel')
    expect(http.get).toHaveBeenNthCalledWith(3, '/merchant/orders', { params: { shopId: 7 } })
    expect(http.get).toHaveBeenNthCalledWith(4, '/merchant/orders/8')
  })
})
