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
  confirmOrderReceipt,
  createOrder,
  deliverMerchantOrder,
  getMerchantOrderDetail,
  getOrderDetail,
  getOrderRefund,
  listMerchantOrders,
  listOrders,
  payOrder,
  prepareMerchantOrder,
} from '../../api/order'
import {
  createProduct,
  createProductSku,
  getProductDetail,
  listProducts,
  updateProduct,
  updateProductSku,
} from '../../api/product'
import { getMerchantProfile, updateMerchantProfile } from '../../api/merchant'
import {
  getProfile,
  loginCustomer,
  loginMerchant,
  registerCustomer,
  registerMerchant,
  updateProfile,
} from '../../api/user'
import {
  createShop,
  getStoreDetail,
  listStores,
  updateStoreAddress,
  updateStoreStatus,
} from '../../api/store'
import {
  createUserAddress,
  listUserAddresses,
  removeUserAddress,
  updateUserAddress,
} from '../../api/address'
import { uploadProductImage } from '../../api/image'

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
      passwordConfirm: 'pass123456',
      nickname: '普通用户',
      phone: '13800000000',
    }
    const loginBody = { account: 'user01', password: 'pass123456' }
    const profileBody = { nickname: '新用户', phone: '13900000000' }

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
    addCartItem({ skuId: 1001, quantity: 2 })
    updateCartItem(88, { quantity: 3 })
    removeCartItem(88)

    expect(mocks.get).toHaveBeenCalledWith('/cart-items')
    expect(mocks.post).toHaveBeenCalledWith('/cart-items', { skuId: 1001, quantity: 2 })
    expect(mocks.patch).toHaveBeenCalledWith('/cart-items/88', { quantity: 3 })
    expect(mocks.delete).toHaveBeenCalledWith('/cart-items/88')
  })

  it('uses shop-scoped product list and existing product detail routes', () => {
    const createBody = {
      shopId: 7,
      categoryId: 21,
      name: '牛肉饭',
      description: '招牌套餐',
      imageId: 301,
      skus: [{ name: '默认规格', price: 18.8, stock: 20 }],
    }
    const patchBody = { name: '招牌牛肉饭', imageId: 302, status: 'ON_SALE' }
    const skuBody = { name: '大份', price: 22, stock: 10 }
    const skuPatchBody = { price: 23, stock: 8, status: 'ON_SALE', version: 3 }

    createProduct(createBody)
    listProducts(7, { page: 1 })
    getProductDetail(11)
    updateProduct(11, patchBody)
    createProductSku(11, skuBody)
    updateProductSku(1001, skuPatchBody)

    expect(mocks.post).toHaveBeenCalledWith('/products', createBody)
    expect(mocks.get).toHaveBeenCalledWith('/shops/7/products', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/products/11')
    expect(mocks.patch).toHaveBeenCalledWith('/products/11', patchBody)
    expect(mocks.post).toHaveBeenCalledWith('/products/11/skus', skuBody)
    expect(mocks.patch).toHaveBeenCalledWith('/skus/1001', skuPatchBody)
  })

  it('uses the shop create, update, detail and list routes', () => {
    createShop({ name: '示例快餐店', description: '午晚餐简餐' })
    listStores({ page: 1 })
    getStoreDetail(7)
    updateStoreStatus(7, { status: 'OPEN' })
    const addressBody = { region: '浙江省杭州市西湖区', detail: '学院路 2 号', phone: '05710000000' }
    updateStoreAddress(7, addressBody)

    expect(mocks.post).toHaveBeenCalledWith('/shops', {
      name: '示例快餐店',
      description: '午晚餐简餐',
    })
    expect(mocks.get).toHaveBeenCalledWith('/shops', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/shops/7')
    expect(mocks.patch).toHaveBeenCalledWith('/shops/7', { status: 'OPEN' })
    expect(mocks.patch).toHaveBeenCalledWith('/shops/7/address', addressBody)
  })

  it('keeps order APIs aligned with the new contract surface', () => {
    const createBody = {
      items: [{ cartItemId: 1, skuVersion: 3 }],
      addressId: 51,
      remark: '少放辣椒',
    }
    createOrder(createBody, 'order-key')
    listOrders({ page: 1 })
    getOrderDetail(1001)
    payOrder(1001, 'pay-key')
    cancelOrder(1001, { reason: '临时有事' }, 'cancel-key')
    confirmOrderReceipt(1001, 'receipt-key')
    getOrderRefund(1001)
    listMerchantOrders({ page: 1 })
    getMerchantOrderDetail(1001)
    prepareMerchantOrder(1001, 'prepare-key')
    deliverMerchantOrder(1001, 'deliver-key')

    expect(mocks.post).toHaveBeenCalledWith(
      '/orders',
      createBody,
      { headers: { 'X-Idempotency-Key': 'order-key' } },
    )
    expect(mocks.get).toHaveBeenCalledWith('/orders', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/orders/1001')
    expect(mocks.post).toHaveBeenCalledWith(
      '/orders/1001/pay',
      {},
      { headers: { 'X-Idempotency-Key': 'pay-key' } },
    )
    expect(mocks.post).toHaveBeenCalledWith(
      '/orders/1001/cancel',
      { reason: '临时有事' },
      { headers: { 'X-Idempotency-Key': 'cancel-key' } },
    )
    expect(mocks.post).toHaveBeenCalledWith(
      '/orders/1001/confirm-receipt',
      {},
      { headers: { 'X-Idempotency-Key': 'receipt-key' } },
    )
    expect(mocks.get).toHaveBeenCalledWith('/orders/1001/refund')
    expect(mocks.get).toHaveBeenCalledWith('/merchant/orders', { params: { page: 1 } })
    expect(mocks.get).toHaveBeenCalledWith('/merchant/orders/1001')
    expect(mocks.post).toHaveBeenCalledWith(
      '/merchant/orders/1001/prepare',
      {},
      { headers: { 'X-Idempotency-Key': 'prepare-key' } },
    )
    expect(mocks.post).toHaveBeenCalledWith(
      '/merchant/orders/1001/deliver',
      {},
      { headers: { 'X-Idempotency-Key': 'deliver-key' } },
    )
  })

  it('uses the user-address contract', () => {
    const createBody = {
      recipient: '张三',
      phone: '13800000000',
      region: '浙江省杭州市西湖区',
      detail: '文三路 1 号 101 室',
      isDefault: true,
    }
    const patchBody = { detail: '文三路 2 号 201 室', isDefault: false }

    createUserAddress(createBody)
    listUserAddresses()
    updateUserAddress(51, patchBody)
    removeUserAddress(51)

    expect(mocks.post).toHaveBeenCalledWith('/user-addresses', createBody)
    expect(mocks.get).toHaveBeenCalledWith('/user-addresses')
    expect(mocks.patch).toHaveBeenCalledWith('/user-addresses/51', patchBody)
    expect(mocks.delete).toHaveBeenCalledWith('/user-addresses/51')
  })

  it('uploads a product image as multipart form data', () => {
    const formData = new FormData()
    formData.append('file', new Blob(['image'], { type: 'image/png' }), 'meal.png')

    uploadProductImage(formData)

    expect(mocks.post).toHaveBeenCalledWith('/files/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
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
