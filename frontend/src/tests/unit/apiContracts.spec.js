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
import { createProduct, updateProduct } from '../../api/product'
import { getMerchantProfile, updateMerchantProfile } from '../../api/merchant'
import {
  getProfile,
  loginCustomer,
  loginMerchant,
  registerCustomer,
  registerMerchant,
  updateProfile,
} from '../../api/user'
import { createShop, getStoreDetail, listStores, updateStoreStatus } from '../../api/store'

beforeEach(() => {
  mocks.get.mockReset()
  mocks.post.mockReset()
  mocks.patch.mockReset()
  mocks.delete.mockReset()
})

describe('api contracts', () => {
  it('uses independent customer account routes', () => {
    const registerBody = {
      account: 'user01',
      password: 'pass123456',
      name: '普通用户',
      phone: '13800000000',
      address: '教学楼 A',
    }
    const loginBody = { account: 'user01', password: 'pass123456' }
    const profileBody = { name: '新用户', phone: '13900000000', address: '宿舍楼 B' }

    registerCustomer(registerBody)
    loginCustomer(loginBody)
    getProfile()
    updateProfile(profileBody)

    expect(mocks.post).toHaveBeenCalledWith('/users', registerBody)
    expect(mocks.post).toHaveBeenCalledWith('/users/login', loginBody)
    expect(mocks.get).toHaveBeenCalledWith('/users/me')
    expect(mocks.patch).toHaveBeenCalledWith('/users/me', profileBody)
  })

  it('uses independent merchant account routes', () => {
    const registerBody = {
      account: 'merchant01',
      password: 'pass123456',
      name: '商家用户',
      phone: '13800000000',
    }
    const loginBody = { account: 'merchant01', password: 'pass123456' }
    const profileBody = { name: '新商家', phone: '13900000000' }

    registerMerchant(registerBody)
    loginMerchant(loginBody)
    getMerchantProfile()
    updateMerchantProfile(profileBody)

    expect(mocks.post).toHaveBeenCalledWith('/merchants', registerBody)
    expect(mocks.post).toHaveBeenCalledWith('/merchants/login', loginBody)
    expect(mocks.get).toHaveBeenCalledWith('/merchants/me')
    expect(mocks.patch).toHaveBeenCalledWith('/merchants/me', profileBody)
  })

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
    const createBody = {
      shopId: 7,
      categoryId: 21,
      name: '牛肉饭',
      price: 18.8,
      stock: 20,
    }
    const patchBody = { price: 20, stock: 18, version: 3 }

    createProduct(createBody)
    listProducts(7, { page: 1 })
    getProductDetail(11)
    updateProduct(11, patchBody)

    expect(mocks.post).toHaveBeenCalledWith('/products', createBody)
    expect(mocks.get).toHaveBeenCalledWith('/shops/7/products', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/products/11')
    expect(mocks.patch).toHaveBeenCalledWith('/products/11', patchBody)
  })

  it('uses the shop create, update, detail and list routes', () => {
    createShop({ name: '示例快餐店', description: '午晚餐简餐' })
    listStores({ page: 1 })
    getStoreDetail(7)
    updateStoreStatus(7, { status: 'OPEN' })

    expect(mocks.post).toHaveBeenCalledWith('/shops', {
      name: '示例快餐店',
      description: '午晚餐简餐',
    })
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
