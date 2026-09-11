import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CartView from '../../views/CartView.vue'

const mocks = vi.hoisted(() => ({
  getCart: vi.fn(),
  updateCartItem: vi.fn(),
  removeCartItem: vi.fn(),
  createOrder: vi.fn(),
  listUserAddresses: vi.fn(),
  routerPush: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/cart', () => ({
  getCart: mocks.getCart,
  updateCartItem: mocks.updateCartItem,
  removeCartItem: mocks.removeCartItem,
}))
vi.mock('../../api/order', () => ({ createOrder: mocks.createOrder }))
vi.mock('../../api/address', () => ({ listUserAddresses: mocks.listUserAddresses }))

const address = {
  id: 51,
  recipient: '张三',
  phone: '13800000000',
  region: '浙江省杭州市西湖区',
  detail: '文三路 1 号 101 室',
  isDefault: true,
}

const cartItem = {
  id: 31,
  product: {
    id: 11,
    shopId: 7,
    name: '招牌牛肉饭',
    imageUrl: '/uploads/products/beef.webp',
    status: 'ON_SALE',
  },
  sku: {
    id: 1001,
    productId: 11,
    name: '大份',
    price: 18.8,
    stock: 20,
    status: 'ON_SALE',
    version: 3,
  },
  quantity: 2,
  subtotal: 37.6,
  available: true,
  unavailableReason: null,
}

function seedCart({ items = [cartItem], addresses = [address], total = 37.6 } = {}) {
  mocks.getCart.mockResolvedValue({ items, total })
  mocks.listUserAddresses.mockResolvedValue(addresses)
}

function mountView() {
  return mount(CartView, {
    global: {
      mocks: { $router: { push: mocks.routerPush } },
      stubs: {
        EmptyState: {
          props: ['description'],
          template: '<div class="empty">{{ description }}<slot /></div>',
        },
        ConfirmAction: {
          emits: ['confirm'],
          template: '<div class="confirm"><slot /><button class="confirm-button" @click="$emit(\'confirm\')">确认</button></div>',
        },
        'el-input-number': {
          inheritAttrs: false,
          props: ['modelValue', 'min', 'max', 'disabled'],
          emits: ['update:modelValue'],
          template: '<input type="number" :data-testid="$attrs[\'data-testid\']" :value="modelValue" :min="min" :max="max" :disabled="disabled" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
        },
        'el-select': {
          inheritAttrs: false,
          props: ['modelValue'],
          emits: ['update:modelValue', 'change'],
          template: '<select :data-testid="$attrs[\'data-testid\']" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value)); $emit(\'change\')"><slot /></select>',
        },
        'el-option': {
          props: ['label', 'value'],
          template: '<option :value="value">{{ label }}</option>',
        },
        'el-input': {
          inheritAttrs: false,
          props: ['modelValue', 'type'],
          emits: ['update:modelValue', 'input'],
          template: '<textarea :data-testid="$attrs[\'data-testid\']" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value); $emit(\'input\')" />',
        },
        'el-button': {
          inheritAttrs: false,
          emits: ['click'],
          props: ['disabled', 'loading'],
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" :aria-label="$attrs[\'aria-label\']" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-icon': { template: '<i><slot /></i>' },
        'el-tag': { template: '<span><slot /></span>' },
      },
    },
  })
}

beforeEach(() => {
  vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'order-key-1') })
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('CartView', () => {
  it('renders the product summary, selected SKU and default address', async () => {
    seedCart()

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getCart).toHaveBeenCalledTimes(1)
    expect(mocks.listUserAddresses).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="product-image-31"]').attributes('src')).toBe('/uploads/products/beef.webp')
    expect(wrapper.get('[data-testid="cart-item-31"]').text()).toContain('招牌牛肉饭')
    expect(wrapper.get('[data-testid="cart-item-31"]').text()).toContain('规格：大份')
    expect(wrapper.get('[data-testid="cart-item-31"]').text()).toContain('¥18.80')
    expect(wrapper.get('[data-testid="cart-item-31"]').text()).toContain('库存：20')
    expect(wrapper.get('[data-testid="checkout-bar"]').text()).toContain('合计：¥37.60')
    expect(wrapper.get('[data-testid="checkout-address"]').element.value).toBe('51')
  })

  it('marks an unavailable cart item and prevents checkout', async () => {
    seedCart({
      items: [{ ...cartItem, available: false, unavailableReason: 'SKU 已下架' }],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="cart-item-31"]').text()).toContain('SKU 已下架')
    expect(wrapper.get('[data-testid="cart-quantity-31"]').element.disabled).toBe(true)
    expect(wrapper.get('[data-testid="checkout-button"]').element.disabled).toBe(true)
  })

  it('shows the shared empty state when the cart is empty', async () => {
    seedCart({ items: [], total: 0 })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('购物车暂为空')
    await wrapper.get('[data-testid="browse-stores"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/stores')
  })

  it('updates quantity using the selected SKU stock limit', async () => {
    seedCart()
    mocks.updateCartItem.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    const quantityInput = wrapper.get('[data-testid="cart-quantity-31"]')
    expect(quantityInput.attributes('max')).toBe('20')
    await quantityInput.setValue('5')
    await wrapper.get('[data-testid="update-item-31"]').trigger('click')
    await flushPromises()

    expect(mocks.updateCartItem).toHaveBeenCalledWith(31, { quantity: 5 })
    expect(mocks.getCart).toHaveBeenCalledTimes(2)
  })

  it('removes a cart item after confirmation', async () => {
    seedCart()
    mocks.removeCartItem.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="delete-item-31"]').attributes('aria-label')).toBe('删除招牌牛肉饭大份')
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.removeCartItem).toHaveBeenCalledWith(31)
    expect(mocks.getCart).toHaveBeenCalledTimes(2)
  })

  it('requires a delivery address and links to address management', async () => {
    seedCart({ addresses: [] })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('请先添加收货地址')
    expect(wrapper.get('[data-testid="checkout-button"]').element.disabled).toBe(true)
    await wrapper.get('[data-testid="manage-addresses"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/addresses')
  })

  it('creates an order with SKU versions, address, trimmed remark and idempotency key', async () => {
    seedCart()
    mocks.createOrder.mockResolvedValue({ id: 901, status: 'PENDING_PAYMENT' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="order-remark"]').setValue('  少放辣椒  ')
    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()

    expect(mocks.createOrder).toHaveBeenCalledWith(
      {
        items: [{ cartItemId: 31, skuVersion: 3 }],
        addressId: 51,
        remark: '少放辣椒',
      },
      'order-key-1',
    )
    expect(mocks.messageSuccess).toHaveBeenCalledWith('订单创建成功')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/orders/901')
  })

  it('rejects a remark longer than 200 characters', async () => {
    seedCart()
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="order-remark"]').setValue('a'.repeat(201))

    expect(wrapper.get('[data-testid="remark-error"]').text()).toContain('不能超过 200 个字符')
    expect(wrapper.get('[data-testid="checkout-button"]').element.disabled).toBe(true)
    expect(mocks.createOrder).not.toHaveBeenCalled()
  })

  it('reuses the idempotency key when retrying the same checkout', async () => {
    seedCart()
    mocks.createOrder.mockRejectedValueOnce(new Error('网络超时')).mockResolvedValueOnce({ id: 901 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()

    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
    expect(mocks.createOrder).toHaveBeenCalledTimes(2)
    expect(mocks.createOrder.mock.calls[0][1]).toBe('order-key-1')
    expect(mocks.createOrder.mock.calls[1][1]).toBe('order-key-1')
  })

  it('disables checkout while the order request is pending', async () => {
    seedCart()
    let resolveOrder
    mocks.createOrder.mockReturnValue(new Promise((resolve) => {
      resolveOrder = resolve
    }))
    const wrapper = mountView()
    await flushPromises()

    wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="checkout-button"]').element.disabled).toBe(true)

    resolveOrder({ id: 901 })
    await flushPromises()
    expect(wrapper.get('[data-testid="checkout-button"]').element.disabled).toBe(false)
  })
})
