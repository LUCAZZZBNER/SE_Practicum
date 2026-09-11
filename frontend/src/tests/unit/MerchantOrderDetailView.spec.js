import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantOrderDetailView from '../../views/MerchantOrderDetailView.vue'

const mocks = vi.hoisted(() => ({
  deliverMerchantOrder: vi.fn(),
  getMerchantOrderDetail: vi.fn(),
  prepareMerchantOrder: vi.fn(),
  routerBack: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1001' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  deliverMerchantOrder: mocks.deliverMerchantOrder,
  getMerchantOrderDetail: mocks.getMerchantOrderDetail,
  prepareMerchantOrder: mocks.prepareMerchantOrder,
}))

const baseOrder = {
  id: 1001,
  orderNumber: 'ORD202609110001',
  shopId: 11,
  shopName: '示例快餐店',
  userAddressSnapshot: {
    recipient: '张三',
    phone: '13800000000',
    region: '浙江省杭州市西湖区',
    detail: '文三路 1 号 101 室',
  },
  shopAddressSnapshot: {
    region: '浙江省杭州市西湖区',
    detail: '学院路 2 号',
    phone: '05710000000',
  },
  remark: '少放辣椒',
  lines: [{
    productId: 101,
    skuId: 1001,
    productName: '招牌牛肉饭',
    skuName: '大份',
    imageUrl: '/uploads/products/beef.webp',
    unitPrice: 18.8,
    quantity: 2,
    subtotal: 37.6,
  }],
  total: 37.6,
  status: 'PAID',
  paymentStatus: 'PAID',
  refundStatus: 'NOT_REFUNDED',
  cancelReason: null,
  createdAt: '2026-09-11T09:00:00Z',
}

function mountView() {
  return mount(MerchantOrderDetailView, {
    global: {
      mocks: { $router: { back: mocks.routerBack } },
      stubs: {
        'el-button': {
          inheritAttrs: false,
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': { template: '<i><slot /></i>' },
        ConfirmAction: {
          emits: ['confirm'],
          template: '<div class="confirm"><slot /><button type="button" class="confirm-button" @click="$emit(\'confirm\')">确认</button></div>',
        },
      },
    },
  })
}

beforeEach(() => {
  vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'fulfillment-key-1') })
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MerchantOrderDetailView', () => {
  it('renders transaction, address, remark and product SKU snapshots', async () => {
    mocks.getMerchantOrderDetail.mockResolvedValue(baseOrder)

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getMerchantOrderDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('ORD202609110001')
    expect(wrapper.text()).toContain('已支付')
    expect(wrapper.text()).toContain('未退款')
    expect(wrapper.text()).toContain('张三 13800000000')
    expect(wrapper.text()).toContain('文三路 1 号 101 室')
    expect(wrapper.text()).toContain('少放辣椒')
    expect(wrapper.get('[data-testid="merchant-line-image-1001"]').attributes('src')).toBe('/uploads/products/beef.webp')
    expect(wrapper.get('[data-testid="merchant-order-line-1001"]').text()).toContain('规格：大份')
    expect(wrapper.get('[data-testid="prepare-order"]')).toBeTruthy()
    expect(wrapper.find('[data-testid="deliver-order"]').exists()).toBe(false)
  })

  it('starts preparation from a paid order with an idempotency key', async () => {
    mocks.getMerchantOrderDetail
      .mockResolvedValueOnce(baseOrder)
      .mockResolvedValueOnce({ ...baseOrder, status: 'PREPARING' })
    mocks.prepareMerchantOrder.mockResolvedValue({ orderStatus: 'PREPARING' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="prepare-order"]').trigger('click')
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.prepareMerchantOrder).toHaveBeenCalledWith(1001, 'fulfillment-key-1')
    expect(mocks.getMerchantOrderDetail).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('订单已开始制作')
    expect(wrapper.text()).toContain('制作中')
  })

  it('starts delivery from a preparing order with an idempotency key', async () => {
    mocks.getMerchantOrderDetail
      .mockResolvedValueOnce({ ...baseOrder, status: 'PREPARING' })
      .mockResolvedValueOnce({ ...baseOrder, status: 'DELIVERING' })
    mocks.deliverMerchantOrder.mockResolvedValue({ orderStatus: 'DELIVERING' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="deliver-order"]').trigger('click')
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.deliverMerchantOrder).toHaveBeenCalledWith(1001, 'fulfillment-key-1')
    expect(mocks.getMerchantOrderDetail).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('订单已开始配送')
    expect(wrapper.text()).toContain('配送中')
  })

  it('reuses the same idempotency key when preparation is retried', async () => {
    mocks.getMerchantOrderDetail.mockResolvedValue(baseOrder)
    mocks.prepareMerchantOrder
      .mockRejectedValueOnce(new Error('网络超时'))
      .mockResolvedValueOnce({ orderStatus: 'PREPARING' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(crypto.randomUUID).toHaveBeenCalledTimes(1)
    expect(mocks.prepareMerchantOrder).toHaveBeenCalledTimes(2)
    expect(mocks.prepareMerchantOrder.mock.calls[0][1]).toBe('fulfillment-key-1')
    expect(mocks.prepareMerchantOrder.mock.calls[1][1]).toBe('fulfillment-key-1')
  })

  it('does not expose fulfillment actions for a delivering order', async () => {
    mocks.getMerchantOrderDetail.mockResolvedValue({ ...baseOrder, status: 'DELIVERING' })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="prepare-order"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="deliver-order"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('确认收货')
  })

  it('shows an API error when loading merchant order detail fails', async () => {
    mocks.getMerchantOrderDetail.mockRejectedValue(new Error('订单不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单不存在')
  })
})
