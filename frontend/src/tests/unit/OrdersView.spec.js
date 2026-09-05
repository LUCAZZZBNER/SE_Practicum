import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OrdersView from '../../views/OrdersView.vue'

const mocks = vi.hoisted(() => ({
  listOrders: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  listOrders: mocks.listOrders,
}))

function mountView() {
  return mount(OrdersView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
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
        EmptyState: {
          props: ['description'],
          template: '<div class="empty">{{ description }}</div>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listOrders.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageError.mockReset()
})

describe('OrdersView', () => {
  it('loads customer orders from the API and renders order rows', async () => {
    mocks.listOrders.mockResolvedValue({
      items: [
        {
          id: 10001,
          store: '示例快餐店',
          amount: 43.6,
          status: '待支付',
          createdAt: '2026-09-03 10:30',
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listOrders).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('10001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('43.6')
    expect(wrapper.text()).toContain('待支付')
    expect(wrapper.text()).toContain('2026-09-03 10:30')
    expect(wrapper.text()).toContain('查看')
  })

  it('shows empty state when no orders exist', async () => {
    mocks.listOrders.mockResolvedValue({
      items: [],
      total: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无订单')
  })

  it('navigates to order detail when clicking view', async () => {
    mocks.listOrders.mockResolvedValue({
      items: [
        {
          id: 10001,
          store: '示例快餐店',
          amount: 43.6,
          status: '待支付',
          createdAt: '2026-09-03 10:30',
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('button').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/orders/10001')
  })

  it('shows an error when loading orders fails', async () => {
    mocks.listOrders.mockRejectedValue(new Error('订单加载失败'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单加载失败')
  })
})
