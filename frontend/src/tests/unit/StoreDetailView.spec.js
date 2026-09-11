import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StoreDetailView from '../../views/StoreDetailView.vue'

const mocks = vi.hoisted(() => ({
  getStoreDetail: vi.fn(),
  listCategories: vi.fn(),
  listProducts: vi.fn(),
  routerPush: vi.fn(),
  routerBack: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: mocks.messageError },
}))

vi.mock('../../api/store', () => ({ getStoreDetail: mocks.getStoreDetail }))
vi.mock('../../api/category', () => ({ listCategories: mocks.listCategories }))
vi.mock('../../api/product', () => ({ listProducts: mocks.listProducts }))
vi.mock('../../api/cart', () => ({ addCartItem: vi.fn() }))

const products = [
  {
    id: 1,
    name: '招牌牛肉饭',
    description: '现做现卖',
    categoryId: 21,
    image: { id: 301, url: '/uploads/products/beef.webp' },
    minPrice: 18.8,
    inStock: true,
    status: 'ON_SALE',
    skus: [
      { id: 1001, name: '小份', price: 18.8, stock: 20, status: 'ON_SALE', version: 3 },
      { id: 1002, name: '大份', price: 22.8, stock: 10, status: 'ON_SALE', version: 1 },
    ],
  },
  {
    id: 2,
    name: '冰柠檬茶',
    description: '清爽解腻',
    categoryId: 22,
    image: null,
    minPrice: 6,
    inStock: false,
    status: 'ON_SALE',
    skus: [{ id: 2001, name: '标准杯', price: 6, stock: 0, status: 'ON_SALE', version: 2 }],
  },
]

function seedStore() {
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
  mocks.listProducts.mockResolvedValue({ items: products })
}

function mountView() {
  return mount(StoreDetailView, {
    global: {
      mocks: {
        $router: { push: mocks.routerPush, back: mocks.routerBack },
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
        'el-tabs': { template: '<div><slot /></div>' },
        'el-tab-pane': {
          props: ['label'],
          template: '<div><h2>{{ label }}</h2><slot /></div>',
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
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

describe('StoreDetailView', () => {
  it('loads public products and renders image, starting price and stock state', async () => {
    seedStore()

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(mocks.listProducts).toHaveBeenCalledWith(7, { page: 1, pageSize: 100 })
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('营业中')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('饮品')
    expect(wrapper.get('[data-testid="product-image-1"]').attributes('src')).toBe('/uploads/products/beef.webp')
    expect(wrapper.get('[data-testid="product-row-1"]').text()).toContain('¥18.80 起')
    expect(wrapper.get('[data-testid="product-row-1"]').text()).toContain('有货')
    expect(wrapper.get('[data-testid="product-image-placeholder-2"]').text()).toContain('暂无图片')
    expect(wrapper.get('[data-testid="product-row-2"]').text()).toContain('已售罄')
  })

  it('opens product detail so the user can select a SKU', async () => {
    seedStore()
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="select-product-1"]').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/products/1')
  })

  it('shows an error when loading store detail fails', async () => {
    mocks.getStoreDetail.mockRejectedValue(new Error('店铺不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('店铺不存在')
  })
})
