import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantOrderDetailView from '../../views/MerchantOrderDetailView.vue'

const mocks = vi.hoisted(() => ({
  getMerchantOrderDetail: vi.fn(),
  routerBack: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1001' } }),
  useRouter: () => ({ back: mocks.routerBack }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  getMerchantOrderDetail: mocks.getMerchantOrderDetail,
}))

function mountView() {
  return mount(MerchantOrderDetailView, {
    global: {
      mocks: {
        $router: { back: mocks.routerBack },
      },
      stubs: {
        'el-button': {
          template: '<button type="button"><slot /></button>',
        },
        'el-descriptions': {
          props: ['title', 'border'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': { template: '<i><slot /></i>' },
      },
    },
  })
}

beforeEach(() => {
  mocks.getMerchantOrderDetail.mockReset()
  mocks.routerBack.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantOrderDetailView', () => {
  it('loads merchant order detail by route id and renders lines', async () => {
    mocks.getMerchantOrderDetail.mockResolvedValue({
      orderNumber: 'MO202609060001',
      shopName: '示例快餐店',
      status: 'PREPARING',
      total: 48.6,
      createdAt: '2026-09-06T02:30:00Z',
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
          unitPrice: 11,
          quantity: 1,
          subtotal: 11,
        },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getMerchantOrderDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('MO202609060001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('制作中')
    expect(wrapper.text()).not.toContain('PREPARING')
    expect(wrapper.text()).toContain('¥48.60')
    expect(wrapper.text()).toContain('2026/09/06 10:30')
    expect(wrapper.text()).toContain('单价：¥18.80')
    expect(wrapper.text()).toContain('小计：¥37.60')
    expect(wrapper.get('[data-testid="back-to-merchant-orders"]')).toBeTruthy()
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('冰柠檬茶')
  })

  it('shows an error when loading merchant order detail fails', async () => {
    mocks.getMerchantOrderDetail.mockRejectedValue(new Error('订单不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单不存在')
  })
})
