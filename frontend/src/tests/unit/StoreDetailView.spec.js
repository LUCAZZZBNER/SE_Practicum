import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StoreDetailView from '../../views/StoreDetailView.vue'

const mocks = vi.hoisted(() => ({
  addCartItem: vi.fn(),
  getStoreDetail: vi.fn(),
  listCategories: vi.fn(),
  listProducts: vi.fn(),
  routerPush: vi.fn(),
  routerBack: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/cart', () => ({
  addCartItem: mocks.addCartItem,
}))

vi.mock('../../api/store', () => ({
  getStoreDetail: mocks.getStoreDetail,
}))

vi.mock('../../api/category', () => ({
  listCategories: mocks.listCategories,
}))

vi.mock('../../api/product', () => ({
  listProducts: mocks.listProducts,
}))

function mountView() {
  return mount(StoreDetailView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
          back: mocks.routerBack,
        },
      },
      stubs: {
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-tabs': {
          template: '<div><slot /></div>',
        },
        'el-tab-pane': {
          props: ['label'],
          template: '<div><h2>{{ label }}</h2><slot /></div>',
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
        'el-icon': { template: '<i><slot /></i>' },
      },
    },
  })
}

beforeEach(() => {
  mocks.addCartItem.mockReset()
  mocks.getStoreDetail.mockReset()
  mocks.listCategories.mockReset()
  mocks.listProducts.mockReset()
  mocks.routerPush.mockReset()
  mocks.routerBack.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('StoreDetailView', () => {
  it('loads store detail by route store id and renders shop status', async () => {
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      description: '校园简餐',
      status: 'OPEN',
    })
    mocks.listCategories.mockResolvedValue([
      { id: 21, name: '主食' },
      { id: 22, name: '饮品' },
    ])
    mocks.listProducts.mockResolvedValue({
      items: [
        { id: 1, name: '招牌牛肉饭', description: '现做现卖', categoryId: 21, price: 18.8, stock: 20, status: 'ON_SALE' },
        { id: 2, name: '冰柠檬茶', description: '清爽解腻', categoryId: 22, price: 6.0, stock: 35, status: 'ON_SALE' },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(mocks.listProducts).toHaveBeenCalledWith(7, {
      page: 1,
      pageSize: 100,
    })
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('校园简餐')
    expect(wrapper.text()).toContain('营业中')
    expect(wrapper.text()).not.toContain('OPEN')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('饮品')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('冰柠檬茶')
    expect(wrapper.text()).toContain('现做现卖')
    expect(wrapper.text()).toContain('¥18.80')
    expect(wrapper.get('[data-testid="back-to-stores"]')).toBeTruthy()
  })

  it('submits add-to-cart from a store product row', async () => {
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      description: '校园简餐',
      status: 'OPEN',
    })
    mocks.listCategories.mockResolvedValue([{ id: 21, name: '主食' }])
    mocks.listProducts.mockResolvedValue({
      items: [
        { id: 1, name: '招牌牛肉饭', categoryId: 21, price: 18.8, stock: 20, status: 'ON_SALE' },
      ],
    })
    mocks.addCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="add-product-1"]').trigger('click')
    await flushPromises()

    expect(mocks.addCartItem).toHaveBeenCalledWith({
      productId: 1,
      quantity: 1,
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已加入购物车')
  })

  it('shows an error when loading store detail fails', async () => {
    mocks.getStoreDetail.mockRejectedValue(new Error('店铺不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('店铺不存在')
  })
})
