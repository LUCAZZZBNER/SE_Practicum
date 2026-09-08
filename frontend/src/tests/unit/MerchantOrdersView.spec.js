import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantOrdersView from '../../views/MerchantOrdersView.vue'

const mocks = vi.hoisted(() => ({
  listMerchantOrders: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/order', () => ({
  listMerchantOrders: mocks.listMerchantOrders,
}))

function mountView() {
  return mount(MerchantOrdersView, {
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
        'el-button': {
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listMerchantOrders.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantOrdersView', () => {
  it('loads merchant orders from the API and renders order rows', async () => {
    mocks.listMerchantOrders.mockResolvedValue({
      items: [
        {
          id: 1001,
          orderNumber: 'MO202609060001',
          shopName: '示例快餐店',
          total: 48.6,
          status: '待接单',
          createdAt: '2026-09-06 10:30',
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listMerchantOrders).toHaveBeenCalledWith({ page: 1, pageSize: 10 })
    expect(wrapper.text()).toContain('MO202609060001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('48.6')
    expect(wrapper.text()).toContain('待接单')
    expect(wrapper.text()).toContain('2026-09-06 10:30')
    expect(wrapper.text()).toContain('查看')
  })

  it('shows empty state when no merchant orders exist', async () => {
    mocks.listMerchantOrders.mockResolvedValue({
      items: [],
      total: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无订单')
  })

  it('navigates to merchant order detail when clicking view', async () => {
    mocks.listMerchantOrders.mockResolvedValue({
      items: [
        {
          id: 1001,
          orderNumber: 'MO202609060001',
          shopName: '示例快餐店',
          total: 48.6,
          status: '待接单',
          createdAt: '2026-09-06 10:30',
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('button').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/merchant/orders/1001')
  })

  it('shows an error when loading merchant orders fails', async () => {
    mocks.listMerchantOrders.mockRejectedValue(new Error('订单加载失败'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('订单加载失败')
  })
})
