import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import OrderDetailView from '../../views/OrderDetailView.vue'

const mocks = vi.hoisted(() => ({
  cancelOrder: vi.fn(),
  confirmOrderReceipt: vi.fn(),
  getOrderDetail: vi.fn(),
  getOrderRefund: vi.fn(),
  payOrder: vi.fn(),
  routerBack: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '10001' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  cancelOrder: mocks.cancelOrder,
  confirmOrderReceipt: mocks.confirmOrderReceipt,
  getOrderDetail: mocks.getOrderDetail,
  getOrderRefund: mocks.getOrderRefund,
  payOrder: mocks.payOrder,
}))

const baseOrder = {
  id: 10001,
  orderNumber: 'ORD202609110001',
  userId: 1,
  shopId: 11,
  shopName: '示例快餐店',
  shopAddressSnapshot: {
    region: '浙江省杭州市西湖区',
    detail: '学院路 2 号',
    phone: '05710000000',
  },
  userAddressSnapshot: {
    recipient: '张三',
    phone: '13800000000',
    region: '浙江省杭州市西湖区',
    detail: '文三路 1 号 101 室',
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
  status: 'PENDING_PAYMENT',
  paymentStatus: 'UNPAID',
  refundStatus: 'NOT_REFUNDED',
  cancelReason: null,
  cancelledAt: null,
  completedAt: null,
  createdAt: '2026-09-11T09:00:00Z',
}

function mountView() {
  return mount(OrderDetailView, {
    global: {
      mocks: { $router: { back: mocks.routerBack } },
      stubs: {
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-dialog': {
          props: ['modelValue', 'title'],
          template: '<section v-if="modelValue" data-testid="cancel-dialog"><h2>{{ title }}</h2><slot /><slot name="footer" /></section>',
        },
        'el-input': {
          inheritAttrs: false,
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<textarea :data-testid="$attrs[\'data-testid\']" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-button': {
          inheritAttrs: false,
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
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
  vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'action-key-1') })
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('OrderDetailView', () => {
  it('renders order, address, remark and product SKU snapshots', async () => {
    mocks.getOrderDetail.mockResolvedValue(baseOrder)

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getOrderDetail).toHaveBeenCalledWith(10001)
    expect(wrapper.text()).toContain('ORD202609110001')
    expect(wrapper.text()).toContain('待支付')
    expect(wrapper.text()).toContain('未支付')
    expect(wrapper.text()).toContain('未退款')
    expect(wrapper.text()).toContain('张三 13800000000')
    expect(wrapper.text()).toContain('文三路 1 号 101 室')
    expect(wrapper.text()).toContain('学院路 2 号')
    expect(wrapper.text()).toContain('少放辣椒')
    expect(wrapper.get('[data-testid="line-image-1001"]').attributes('src')).toBe('/uploads/products/beef.webp')
    expect(wrapper.get('[data-testid="order-line-1001"]').text()).toContain('招牌牛肉饭')
    expect(wrapper.get('[data-testid="order-line-1001"]').text()).toContain('规格：大份')
    expect(wrapper.get('[data-testid="order-line-1001"]').text()).toContain('小计：¥37.60')
    expect(wrapper.get('[data-testid="pay-order"]')).toBeTruthy()
    expect(wrapper.get('[data-testid="open-cancel"]')).toBeTruthy()
  })

  it('pays a pending order with an idempotency key and reloads it', async () => {
    mocks.getOrderDetail
      .mockResolvedValueOnce(baseOrder)
      .mockResolvedValueOnce({ ...baseOrder, status: 'PAID', paymentStatus: 'PAID' })
    mocks.payOrder.mockResolvedValue({ orderStatus: 'PAID' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="pay-order"]').trigger('click')
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.payOrder).toHaveBeenCalledWith(10001, 'action-key-1')
    expect(mocks.getOrderDetail).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('模拟支付成功')
    expect(wrapper.text()).toContain('已支付')
  })

  it('cancels a paid order with a trimmed reason and idempotency key', async () => {
    mocks.getOrderDetail
      .mockResolvedValueOnce({ ...baseOrder, status: 'PAID', paymentStatus: 'PAID' })
      .mockResolvedValueOnce({
        ...baseOrder,
        status: 'CANCELLED',
        paymentStatus: 'PAID',
        refundStatus: 'REFUNDED',
        cancelReason: '临时有事',
      })
    mocks.cancelOrder.mockResolvedValue({ orderStatus: 'CANCELLED', refundStatus: 'REFUNDED' })
    mocks.getOrderRefund.mockResolvedValue({
      refundNumber: 'REF202609110001',
      amount: 37.6,
      status: 'REFUNDED',
      completedAt: '2026-09-11T09:10:00Z',
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="open-cancel"]').trigger('click')
    await wrapper.get('[data-testid="cancel-reason"]').setValue('  临时有事  ')
    await wrapper.get('[data-testid="confirm-cancel"]').trigger('click')
    await flushPromises()

    expect(mocks.cancelOrder).toHaveBeenCalledWith(
      10001,
      { reason: '临时有事' },
      'action-key-1',
    )
    expect(mocks.messageSuccess).toHaveBeenCalledWith('订单已取消')
    expect(wrapper.text()).toContain('已退款')
  })

  it('rejects a cancellation reason longer than 200 characters', async () => {
    mocks.getOrderDetail.mockResolvedValue(baseOrder)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="open-cancel"]').trigger('click')
    await wrapper.get('[data-testid="cancel-reason"]').setValue('a'.repeat(201))

    expect(wrapper.get('[data-testid="cancel-reason-error"]').text()).toContain('不能超过 200 个字符')
    expect(wrapper.get('[data-testid="confirm-cancel"]').element.disabled).toBe(true)
    expect(mocks.cancelOrder).not.toHaveBeenCalled()
  })

  it('confirms receipt for a delivering order', async () => {
    mocks.getOrderDetail
      .mockResolvedValueOnce({ ...baseOrder, status: 'DELIVERING', paymentStatus: 'PAID' })
      .mockResolvedValueOnce({
        ...baseOrder,
        status: 'COMPLETED',
        paymentStatus: 'PAID',
        completedAt: '2026-09-11T10:00:00Z',
      })
    mocks.confirmOrderReceipt.mockResolvedValue({ orderStatus: 'COMPLETED' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="confirm-receipt"]').trigger('click')
    await wrapper.get('.confirm-button').trigger('click')
    await flushPromises()

    expect(mocks.confirmOrderReceipt).toHaveBeenCalledWith(10001, 'action-key-1')
    expect(mocks.getOrderDetail).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已确认收货')
    expect(wrapper.text()).toContain('已完成')
  })

  it('loads and renders the refund record for a refunded order', async () => {
    mocks.getOrderDetail.mockResolvedValue({
      ...baseOrder,
      status: 'CANCELLED',
      paymentStatus: 'PAID',
      refundStatus: 'REFUNDED',
      cancelReason: '临时有事',
    })
    mocks.getOrderRefund.mockResolvedValue({
      refundNumber: 'REF202609110001',
      amount: 37.6,
      status: 'REFUNDED',
      completedAt: '2026-09-11T09:10:00Z',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getOrderRefund).toHaveBeenCalledWith(10001)
    const refund = wrapper.get('[data-testid="refund-detail"]')
    expect(refund.text()).toContain('REF202609110001')
    expect(refund.text()).toContain('¥37.60')
    expect(refund.text()).toContain('已退款')
    expect(wrapper.find('[data-testid="pay-order"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="open-cancel"]').exists()).toBe(false)
  })

  it('shows an API error when order detail loading fails', async () => {
    mocks.getOrderDetail.mockRejectedValue(new Error('订单不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单不存在')
  })
})
