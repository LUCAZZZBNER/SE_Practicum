import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CartView from '../../views/CartView.vue'

const mocks = vi.hoisted(() => ({
  getCart: vi.fn(),
  updateCartItem: vi.fn(),
  removeCartItem: vi.fn(),
  createOrder: vi.fn(),
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

vi.mock('../../api/order', () => ({
  createOrder: mocks.createOrder,
}))

function mountView() {
  return mount(CartView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
        EmptyState: {
          props: ['description'],
          template: '<div class="empty">{{ description }}<slot /></div>',
        },
        ConfirmAction: {
          emits: ['confirm'],
          template: '<div class="confirm"><slot /><button class="confirm-button" @click="$emit(\'confirm\')">确认</button></div>',
        },
        'el-table': {
          template: '<div><slot /></div>',
        },
        'el-table-column': {
          template: '<div />',
        },
        'el-input-number': {
          props: ['modelValue', 'min', 'max'],
          emits: ['update:modelValue'],
          template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
        },
        'el-button': {
          emits: ['click'],
          props: ['disabled'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-icon': { template: '<i><slot /></i>' },
      },
    },
  })
}

beforeEach(() => {
  vi.stubGlobal('crypto', {
    randomUUID: vi.fn(() => 'order-key-1'),
  })
  mocks.getCart.mockReset()
  mocks.updateCartItem.mockReset()
  mocks.removeCartItem.mockReset()
  mocks.createOrder.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('CartView', () => {
  it('loads cart items and renders cart rows', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getCart).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="cart-item-1"]')).toBeTruthy()
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('¥18.80')
    expect(wrapper.text()).toContain('小计：¥37.60')
    expect(wrapper.get('[data-testid="checkout-bar"]').text()).toContain('合计：¥37.60')
    expect(wrapper.get('[data-testid="checkout-button"]').text()).toBe('提交订单')
  })

  it('shows empty state when cart is empty', async () => {
    mocks.getCart.mockResolvedValue({ items: [], total: 0 })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('购物车暂为空')
    const browseButton = wrapper.get('[data-testid="browse-stores"]')
    expect(browseButton.text()).toBe('去逛店铺')
    await browseButton.trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/stores')
  })

  it('submits quantity update for a cart item', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    mocks.updateCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="update-item-1"]').trigger('click')

    expect(mocks.updateCartItem).toHaveBeenCalled()
    expect(mocks.getCart).toHaveBeenCalledTimes(2)
  })

  it('submits the selected target quantity for a cart item', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    mocks.updateCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    const quantityInput = wrapper.find('input[data-testid="cart-quantity"]')
    await quantityInput.setValue('5')
    await wrapper.get('[data-testid="update-item-1"]').trigger('click')

    expect(mocks.updateCartItem).toHaveBeenCalledWith(1, { quantity: 5 })
  })

  it('submits delete action for a cart item', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    mocks.removeCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    const deleteButton = wrapper.get('[data-testid="delete-item-1"]')
    expect(deleteButton.attributes('aria-label')).toBe('删除招牌牛肉饭')
    await wrapper.find('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.removeCartItem).toHaveBeenCalledWith(1)
    expect(mocks.getCart).toHaveBeenCalledTimes(2)
  })

  it('submits create order action from cart', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
            version: 3,
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    mocks.createOrder.mockResolvedValue({ id: 1001 })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()

    expect(mocks.createOrder).toHaveBeenCalledWith(
      {
        items: [
          {
            cartItemId: 1,
            productVersion: 3,
          },
        ],
      },
      {
        headers: {
          'X-Idempotency-Key': 'order-key-1',
        },
      },
    )
    expect(mocks.getCart).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('订单创建成功')
  })

  it('reuses idempotency key when retrying the same checkout after failure', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
            version: 3,
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    mocks.createOrder.mockRejectedValueOnce(new Error('网络超时')).mockResolvedValueOnce({ id: 1001 })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="checkout-button"]').trigger('click')
    await flushPromises()

    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
    expect(mocks.createOrder).toHaveBeenCalledTimes(2)
    expect(mocks.createOrder.mock.calls[0][1].headers['X-Idempotency-Key']).toBe('order-key-1')
    expect(mocks.createOrder.mock.calls[1][1].headers['X-Idempotency-Key']).toBe('order-key-1')
  })

  it('disables checkout while order request is pending', async () => {
    mocks.getCart.mockResolvedValue({
      items: [
        {
          id: 1,
          product: {
            id: 11,
            name: '招牌牛肉饭',
            shopId: 7,
            price: 18.8,
            stock: 20,
            status: 'ON_SALE',
            version: 3,
          },
          quantity: 2,
          subtotal: 37.6,
          available: true,
        },
      ],
      total: 37.6,
    })
    let resolveOrder
    mocks.createOrder.mockReturnValue(new Promise((resolve) => {
      resolveOrder = resolve
    }))

    const wrapper = mountView()
    await flushPromises()

    const checkoutButton = wrapper.get('[data-testid="checkout-button"]')
    const request = checkoutButton.trigger('click')
    await flushPromises()

    expect(checkoutButton.element.disabled).toBe(true)

    resolveOrder({ id: 1001 })
    await request
    await flushPromises()
    expect(checkoutButton.element.disabled).toBe(false)
  })
})
