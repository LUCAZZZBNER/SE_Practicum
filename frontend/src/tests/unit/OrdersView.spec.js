import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OrdersView from '../../views/OrdersView.vue'

const mocks = vi.hoisted(() => ({
  listOrders: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: mocks.messageError },
}))
vi.mock('../../api/order', () => ({ listOrders: mocks.listOrders }))

function mountView() {
  return mount(OrdersView, {
    global: {
      mocks: { $router: { push: mocks.routerPush } },
      stubs: {
        'el-button': {
          inheritAttrs: false,
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': { template: '<i><slot /></i>' },
        EmptyState: {
          props: ['description'],
          template: '<div class="empty">{{ description }}</div>',
        },
      },
    },
  })
}

const orderSummary = {
  id: 10001,
  orderNumber: 'ORD202609110001',
  shopId: 11,
  shopName: '示例快餐店',
  total: 37.6,
  status: 'PENDING_PAYMENT',
  paymentStatus: 'UNPAID',
  refundStatus: 'NOT_REFUNDED',
  createdAt: '2026-09-11T09:00:00Z',
}

beforeEach(() => {
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

describe('OrdersView', () => {
  it('loads order summaries with the default paging and sorting contract', async () => {
    mocks.listOrders.mockResolvedValue({ items: [orderSummary], total: 1 })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listOrders).toHaveBeenCalledWith({
      page: 1,
      pageSize: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc',
    })
    const card = wrapper.get('[data-testid="order-card-10001"]')
    expect(card.text()).toContain('ORD202609110001')
    expect(card.text()).toContain('示例快餐店')
    expect(card.text()).toContain('¥37.60')
    expect(card.text()).toContain('待支付')
    expect(card.text()).toContain('未支付')
    expect(card.text()).toContain('未退款')
    expect(card.text()).not.toContain('PENDING_PAYMENT')
  })

  it('renders refunded status from an order summary', async () => {
    mocks.listOrders.mockResolvedValue({
      items: [{
        ...orderSummary,
        status: 'CANCELLED',
        paymentStatus: 'PAID',
        refundStatus: 'REFUNDED',
      }],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已取消')
    expect(wrapper.text()).toContain('已支付')
    expect(wrapper.text()).toContain('已退款')
  })

  it('navigates to order detail', async () => {
    mocks.listOrders.mockResolvedValue({ items: [orderSummary], total: 1 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="view-order-10001"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/orders/10001')
  })

  it('shows the empty state and API errors', async () => {
    mocks.listOrders.mockResolvedValueOnce({ items: [], total: 0 })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无订单')

    mocks.listOrders.mockRejectedValueOnce(new Error('订单加载失败'))
    mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('订单加载失败')
  })
})
