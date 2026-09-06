import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OrderDetailView from '../../views/OrderDetailView.vue'

const mocks = vi.hoisted(() => ({
  getOrderDetail: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '10001' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  getOrderDetail: mocks.getOrderDetail,
}))

function mountView() {
  return mount(OrderDetailView, {
    global: {
      stubs: {
        'el-steps': {
          props: ['active', 'finishStatus'],
          template: '<div class="steps"><slot /></div>',
        },
        'el-step': {
          props: ['title'],
          template: '<div class="step">{{ title }}</div>',
        },
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.getOrderDetail.mockReset()
  mocks.messageError.mockReset()
})

describe('OrderDetailView', () => {
  it('loads order detail by route id and renders lines', async () => {
    mocks.getOrderDetail.mockResolvedValue({
      id: 10001,
      orderNumber: 'OD202609030001',
      shopName: '示例快餐店',
      status: 'PENDING_PAYMENT',
      total: 43.6,
      lines: [
        {
          productId: 11,
          productName: '招牌牛肉饭',
          unitPrice: 18.8,
          quantity: 2,
          subtotal: 37.6,
        },
        {
          productId: 12,
          productName: '冰柠檬茶',
          unitPrice: 6.0,
          quantity: 1,
          subtotal: 6.0,
        },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getOrderDetail).toHaveBeenCalledWith(10001)
    expect(wrapper.text()).toContain('OD202609030001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('43.60 元')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('冰柠檬茶')
  })

  it('shows an error when loading order detail fails', async () => {
    mocks.getOrderDetail.mockRejectedValue(new Error('订单不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单不存在')
  })
})
