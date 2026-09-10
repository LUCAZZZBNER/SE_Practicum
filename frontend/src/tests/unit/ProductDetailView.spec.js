import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductDetailView from '../../views/ProductDetailView.vue'

const mocks = vi.hoisted(() => ({
  getProductDetail: vi.fn(),
  getStoreDetail: vi.fn(),
  listCategories: vi.fn(),
  addCartItem: vi.fn(),
  routerPush: vi.fn(),
  routerBack: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '11' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/product', () => ({
  getProductDetail: mocks.getProductDetail,
}))

vi.mock('../../api/category', () => ({
  listCategories: mocks.listCategories,
}))

vi.mock('../../api/store', () => ({
  getStoreDetail: mocks.getStoreDetail,
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
        'el-input-number': {
          props: ['modelValue', 'min', 'max'],
          emits: ['update:modelValue'],
          template: '<input type="number" :value="modelValue" />',
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
  mocks.getProductDetail.mockReset()
  mocks.getStoreDetail.mockReset()
  mocks.listCategories.mockReset()
  mocks.addCartItem.mockReset()
  mocks.routerPush.mockReset()
  mocks.routerBack.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('ProductDetailView', () => {
  it('loads product detail by route product id and renders price and stock', async () => {
    mocks.getProductDetail.mockResolvedValue({
      id: 11,
      name: '招牌牛肉饭',
      description: '招牌套餐，现做现卖',
      shopId: 7,
      categoryId: 21,
      price: 18.8,
      stock: 20,
      status: 'ON_SALE',
    })
    mocks.getStoreDetail.mockResolvedValue({ id: 7, name: '示例快餐店' })
    mocks.listCategories.mockResolvedValue([{ id: 21, name: '主食' }])

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getProductDetail).toHaveBeenCalledWith(11)
    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('招牌套餐，现做现卖')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('¥18.80')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('在售')
    expect(wrapper.text()).not.toContain('ON_SALE')
    expect(wrapper.get('[data-testid="back-to-store"]')).toBeTruthy()
  })

  it('disables add-to-cart when stock is zero', async () => {
    mocks.getProductDetail.mockResolvedValue({
      id: 12,
      name: '售罄商品',
      shopId: 7,
      categoryId: 21,
      price: 9.9,
      stock: 0,
      status: 'ON_SALE',
    })
    mocks.getStoreDetail.mockResolvedValue({ id: 7, name: '示例快餐店' })
    mocks.listCategories.mockResolvedValue([{ id: 21, name: '主食' }])

    const wrapper = mountView()
    await flushPromises()

    const button = wrapper.get('[data-testid="add-to-cart"]')
    expect(button.element.disabled).toBe(true)
    expect(button.text()).toBe('暂不可购买')
    expect(mocks.addCartItem).not.toHaveBeenCalled()
  })

  it('adds the selected quantity to cart and shows success feedback', async () => {
    mocks.getProductDetail.mockResolvedValue({
      id: 11,
      name: '招牌牛肉饭',
      shopId: 7,
      categoryId: 21,
      price: 18.8,
      stock: 20,
      status: 'ON_SALE',
    })
    mocks.getStoreDetail.mockResolvedValue({ id: 7, name: '示例快餐店' })
    mocks.listCategories.mockResolvedValue([{ id: 21, name: '主食' }])
    mocks.addCartItem.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('[data-testid="add-to-cart"]').trigger('click')
    await flushPromises()

    expect(mocks.addCartItem).toHaveBeenCalledWith({ productId: 11, quantity: 1 })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已加入购物车')
  })

  it('shows an error when loading product detail fails', async () => {
    mocks.getProductDetail.mockRejectedValue(new Error('商品不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('商品不存在')
  })
})
