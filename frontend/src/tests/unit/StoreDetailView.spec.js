import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StoreDetailView from '../../views/StoreDetailView.vue'

const mocks = vi.hoisted(() => ({
  addCartItem: vi.fn(),
  getStoreDetail: vi.fn(),
  listCategories: vi.fn(),
  listProducts: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
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
        { id: 1, name: '招牌牛肉饭', categoryId: 21, price: 18.8, stock: 20, status: '在售' },
        { id: 2, name: '冰柠檬茶', categoryId: 22, price: 6.0, stock: 35, status: '在售' },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(mocks.listProducts).toHaveBeenCalledWith(7, {
      page: 1,
      pageSize: 100,
      includeOffSale: true,
    })
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('校园简餐')
    expect(wrapper.text()).toContain('OPEN')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('饮品')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('冰柠檬茶')
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
        { id: 1, name: '招牌牛肉饭', categoryId: 21, price: 18.8, stock: 20, status: '在售' },
      ],
    })
    mocks.addCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button')[1].trigger('click')

    expect(mocks.addCartItem).toHaveBeenCalledWith({
      productId: 1,
      quantity: 1,
    })
  })

  it('shows an error when loading store detail fails', async () => {
    mocks.getStoreDetail.mockRejectedValue(new Error('店铺不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('店铺不存在')
  })
})
