import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductDetailView from '../../views/ProductDetailView.vue'

const mocks = vi.hoisted(() => ({
  getProductDetail: vi.fn(),
  addCartItem: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '11' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/product', () => ({
  getProductDetail: mocks.getProductDetail,
}))

vi.mock('../../api/cart', () => ({
  addCartItem: mocks.addCartItem,
}))

function mountView() {
  return mount(ProductDetailView, {
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
        'el-input-number': {
          props: ['modelValue', 'min', 'max'],
          emits: ['update:modelValue'],
          template: '<input type="number" :value="modelValue" />',
        },
        'el-button': {
          props: ['disabled'],
          template: '<button :disabled="disabled"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.getProductDetail.mockReset()
  mocks.addCartItem.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageError.mockReset()
})

describe('ProductDetailView', () => {
  it('loads product detail by route product id and renders price and stock', async () => {
    mocks.getProductDetail.mockResolvedValue({
      id: 11,
      name: '招牌牛肉饭',
      shopName: '示例快餐店',
      categoryName: '主食',
      price: 18.8,
      stock: 20,
      status: 'ON_SALE',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getProductDetail).toHaveBeenCalledWith(11)
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('18.8')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('ON_SALE')
  })

  it('disables add-to-cart when stock is zero', async () => {
    mocks.getProductDetail.mockResolvedValue({
      id: 12,
      name: '售罄商品',
      shopName: '示例快餐店',
      categoryName: '主食',
      price: 9.9,
      stock: 0,
      status: 'ON_SALE',
    })

    const wrapper = mountView()
    await flushPromises()

    const buttons = wrapper.findAll('button')
    expect(buttons[1].element.disabled).toBe(true)
    expect(mocks.addCartItem).not.toHaveBeenCalled()
  })

  it('shows an error when loading product detail fails', async () => {
    mocks.getProductDetail.mockRejectedValue(new Error('商品不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('商品不存在')
  })
})
