import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductDetailView from '../../views/ProductDetailView.vue'

const mocks = vi.hoisted(() => ({
  getProductDetail: vi.fn(),
  getStoreDetail: vi.fn(),
  listCategories: vi.fn(),
  addCartItem: vi.fn(),
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

vi.mock('../../api/product', () => ({ getProductDetail: mocks.getProductDetail }))
vi.mock('../../api/category', () => ({ listCategories: mocks.listCategories }))
vi.mock('../../api/store', () => ({ getStoreDetail: mocks.getStoreDetail }))
vi.mock('../../api/cart', () => ({ addCartItem: mocks.addCartItem }))

const product = {
  id: 11,
  name: '招牌牛肉饭',
  description: '招牌套餐，现做现卖',
  shopId: 7,
  categoryId: 21,
  image: { id: 301, url: '/uploads/products/beef.webp' },
  minPrice: 18.8,
  inStock: true,
  status: 'ON_SALE',
  skus: [
    { id: 1001, productId: 11, name: '小份', price: 18.8, stock: 20, status: 'ON_SALE', version: 3 },
    { id: 1002, productId: 11, name: '大份', price: 22.8, stock: 8, status: 'ON_SALE', version: 1 },
    { id: 1003, productId: 11, name: '超大份', price: 26.8, stock: 0, status: 'ON_SALE', version: 2 },
  ],
}

function seedProduct(data = product) {
  mocks.getProductDetail.mockResolvedValue(data)
  mocks.getStoreDetail.mockResolvedValue({ id: 7, name: '示例快餐店' })
  mocks.listCategories.mockResolvedValue([{ id: 21, name: '主食' }])
}

function mountView() {
  return mount(ProductDetailView, {
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
        'el-button': {
          props: ['disabled'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-input-number': {
          props: ['modelValue', 'min', 'max'],
          emits: ['update:modelValue'],
          template: '<input type="number" :value="modelValue" :min="min" :max="max" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
        },
        'el-icon': { template: '<i><slot /></i>' },
        'el-tag': { template: '<span><slot /></span>' },
      },
    },
  })
}

beforeEach(() => {
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

describe('ProductDetailView', () => {
  it('renders the product image and all returned SKU options', async () => {
    seedProduct()

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getProductDetail).toHaveBeenCalledWith(11)
    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(wrapper.get('[data-testid="product-image"]').attributes('src')).toBe('/uploads/products/beef.webp')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.get('[data-testid="sku-option-1001"]').text()).toContain('小份')
    expect(wrapper.get('[data-testid="sku-option-1001"]').text()).toContain('¥18.80')
    expect(wrapper.get('[data-testid="sku-option-1002"]').text()).toContain('大份')
    expect(wrapper.get('[data-testid="sku-option-1003"]').text()).toContain('已售罄')
    expect(wrapper.get('[data-testid="sku-option-1003"]').attributes('disabled')).toBeDefined()
  })

  it('requires an available SKU to be selected before adding to cart', async () => {
    seedProduct()
    const wrapper = mountView()
    await flushPromises()

    const addButton = wrapper.get('[data-testid="add-to-cart"]')
    expect(addButton.element.disabled).toBe(true)
    expect(addButton.text()).toContain('请选择规格')
    expect(mocks.addCartItem).not.toHaveBeenCalled()
  })

  it('updates price and stock when the user selects a SKU', async () => {
    seedProduct()
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="sku-option-1002"]').trigger('click')

    expect(wrapper.get('[data-testid="selected-price"]').text()).toBe('¥22.80')
    expect(wrapper.get('[data-testid="selected-stock"]').text()).toContain('库存 8')
    expect(wrapper.get('[data-testid="quantity-input"]').attributes('max')).toBe('8')
    expect(wrapper.get('[data-testid="add-to-cart"]').element.disabled).toBe(false)
  })

  it('adds the selected SKU and quantity to cart', async () => {
    seedProduct()
    mocks.addCartItem.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="sku-option-1001"]').trigger('click')
    await wrapper.get('[data-testid="quantity-input"]').setValue('3')
    await wrapper.get('[data-testid="add-to-cart"]').trigger('click')
    await flushPromises()

    expect(mocks.addCartItem).toHaveBeenCalledWith({ skuId: 1001, quantity: 3 })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已加入购物车')
  })

  it('shows a placeholder when the product has no image', async () => {
    seedProduct({ ...product, image: null })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="product-image-placeholder"]').text()).toContain('暂无图片')
  })

  it('shows an error when loading product detail fails', async () => {
    mocks.getProductDetail.mockRejectedValue(new Error('商品不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('商品不存在')
  })
})
