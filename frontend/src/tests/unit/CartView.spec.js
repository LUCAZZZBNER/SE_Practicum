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
          template: '<div class="empty">{{ description }}</div>',
        },
        ConfirmAction: {
          template: '<div class="confirm"><slot /></div>',
        },
        'el-table': {
          template: '<div><slot /></div>',
        },
        'el-table-column': {
          template: '<div />',
        },
        'el-button': {
          emits: ['click'],
          props: ['disabled'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
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
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('37.6')
    expect(wrapper.text()).toContain('创建订单')
  })

  it('shows empty state when cart is empty', async () => {
    mocks.getCart.mockResolvedValue({ items: [], total: 0 })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('购物车暂为空')
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

    await wrapper.findAll('button')[0].trigger('click')

    expect(mocks.updateCartItem).toHaveBeenCalled()
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

    expect(wrapper.text()).toContain('删除')
    expect(mocks.removeCartItem).not.toHaveBeenCalled()
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

    await wrapper.findAll('button')[2].trigger('click')

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

    await wrapper.findAll('button')[2].trigger('click')
    await flushPromises()
    await wrapper.findAll('button')[2].trigger('click')
    await flushPromises()

    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
    expect(mocks.createOrder).toHaveBeenCalledTimes(2)
    expect(mocks.createOrder.mock.calls[0][1].headers['X-Idempotency-Key']).toBe('order-key-1')
    expect(mocks.createOrder.mock.calls[1][1].headers['X-Idempotency-Key']).toBe('order-key-1')
  })
})
