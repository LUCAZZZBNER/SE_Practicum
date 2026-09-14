import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantOrdersView from '../../views/MerchantOrdersView.vue'

const mocks = vi.hoisted(() => ({
  listMerchantOrders: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({ ElMessage: { error: mocks.messageError } }))
vi.mock('../../api/order', () => ({ listMerchantOrders: mocks.listMerchantOrders }))

const orderSummary = {
  id: 1001,
  orderNumber: 'ORD202609110001',
  shopId: 11,
  shopName: '示例快餐店',
  total: 48.6,
  status: 'PREPARING',
  paymentStatus: 'PAID',
  refundStatus: 'NOT_REFUNDED',
  createdAt: '2026-09-11T02:30:00Z',
}

function mountView() {
  return mount(MerchantOrdersView, {
    global: {
      mocks: { $router: { push: mocks.routerPush } },
      stubs: {
        EmptyState: {
          props: ['description'],
          template: '<div class="empty">{{ description }}</div>',
        },
        'el-button': {
          inheritAttrs: false,
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': { template: '<i><slot /></i>' },
      },
    },
  })
}

beforeEach(() => {
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

describe('MerchantOrdersView', () => {
  it('loads merchant order summaries with default paging and sorting', async () => {
    mocks.listMerchantOrders.mockResolvedValue({ items: [orderSummary], total: 1 })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listMerchantOrders).toHaveBeenCalledWith({
      page: 1,
      pageSize: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc',
    })
    const card = wrapper.get('[data-testid="merchant-order-card-1001"]')
    expect(card.text()).toContain('ORD202609110001')
    expect(card.text()).toContain('示例快餐店')
    expect(card.text()).toContain('¥48.60')
    expect(card.text()).toContain('制作中')
    expect(card.text()).toContain('已支付')
    expect(card.text()).toContain('未退款')
    expect(card.text()).not.toContain('PREPARING')
  })

  it('renders cancelled and refunded transaction states', async () => {
    mocks.listMerchantOrders.mockResolvedValue({
      items: [{
        ...orderSummary,
        status: 'CANCELLED',
        refundStatus: 'REFUNDED',
      }],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已取消')
    expect(wrapper.text()).toContain('已退款')
  })

  it('navigates to merchant order detail', async () => {
    mocks.listMerchantOrders.mockResolvedValue({ items: [orderSummary], total: 1 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="view-merchant-order-1001"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/merchant/orders/1001')
  })

  it('shows the empty state and API errors', async () => {
    mocks.listMerchantOrders.mockResolvedValueOnce({ items: [], total: 0 })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无订单')

    mocks.listMerchantOrders.mockRejectedValueOnce(new Error('订单加载失败'))
    mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('订单加载失败')
  })
})
