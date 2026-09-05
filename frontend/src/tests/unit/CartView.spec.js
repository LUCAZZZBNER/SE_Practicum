import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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
          props: ['disabled'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.getCart.mockReset()
  mocks.updateCartItem.mockReset()
  mocks.removeCartItem.mockReset()
  mocks.createOrder.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
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

    expect(wrapper.text()).toContain('创建订单')
    expect(mocks.createOrder).not.toHaveBeenCalled()
  })
})
