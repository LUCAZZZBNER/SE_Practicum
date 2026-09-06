import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantOrderDetailView from '../../views/MerchantOrderDetailView.vue'

const mocks = vi.hoisted(() => ({
  getMerchantOrderDetail: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1001' } }),
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
      stubs: {
        'el-descriptions': {
          props: ['title', 'border'],
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
  mocks.getMerchantOrderDetail.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantOrderDetailView', () => {
  it('loads merchant order detail by route id and renders lines', async () => {
    mocks.getMerchantOrderDetail.mockResolvedValue({
      orderNumber: 'MO202609060001',
      shopName: '示例快餐店',
      status: '待接单',
      total: 48.6,
      lines: [
        {
          productId: 11,
          productName: '招牌牛肉饭',
          unitPrice: 18.8,
          quantity: 2,
        },
        {
          productId: 12,
          productName: '冰柠檬茶',
          unitPrice: 11,
          quantity: 1,
        },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getMerchantOrderDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('MO202609060001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('待接单')
    expect(wrapper.text()).toContain('48.60 元')
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
