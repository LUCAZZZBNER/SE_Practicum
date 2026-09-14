import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantProductsView from '../../views/MerchantProductsView.vue'

const mocks = vi.hoisted(() => ({
  createCategory: vi.fn(),
  listCategories: vi.fn(),
  removeCategory: vi.fn(),
  updateCategory: vi.fn(),
  getMerchantProfile: vi.fn(),
  listStores: vi.fn(),
  listProducts: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  createProductSku: vi.fn(),
  updateProductSku: vi.fn(),
  uploadProductImage: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/category', () => ({
  createCategory: mocks.createCategory,
  listCategories: mocks.listCategories,
  removeCategory: mocks.removeCategory,
  updateCategory: mocks.updateCategory,
}))

vi.mock('../../api/merchant', () => ({ getMerchantProfile: mocks.getMerchantProfile }))
vi.mock('../../api/store', () => ({ listStores: mocks.listStores }))
vi.mock('../../api/image', () => ({ uploadProductImage: mocks.uploadProductImage }))
vi.mock('../../api/product', () => ({
  listProducts: mocks.listProducts,
  createProduct: mocks.createProduct,
  updateProduct: mocks.updateProduct,
  createProductSku: mocks.createProductSku,
  updateProductSku: mocks.updateProductSku,
}))

const sku = {
  id: 1001,
  productId: 1,
  name: '大份',
  price: 18.8,
  stock: 20,
  status: 'ON_SALE',
  version: 3,
}

const product = {
  id: 1,
  name: '招牌牛肉饭',
  description: '招牌套餐',
  categoryId: 21,
  image: { id: 301, url: '/uploads/products/301.webp' },
  minPrice: 18.8,
  inStock: true,
  status: 'ON_SALE',
  skus: [sku],
}

function seedMerchantProducts({ products = [], categories = [{ id: 21, name: '主食', sortOrder: 1 }] } = {}) {
  mocks.getMerchantProfile.mockResolvedValue({ id: 2, status: 'ACTIVE' })
  mocks.listStores.mockResolvedValue({ items: [{ id: 7 }] })
  mocks.listCategories.mockResolvedValue(categories)
  mocks.listProducts.mockResolvedValue({ items: products, total: products.length })
}

function mountView() {
  return mount(MerchantProductsView, {
    global: {
      stubs: {
        ConfirmAction: {
          emits: ['confirm'],
          template: '<div class="confirm"><slot /><button type="button" class="confirm-button" @click="$emit(\'confirm\')">确认</button></div>',
        },
        'el-input': {
          props: ['modelValue', 'placeholder', 'type'],
          emits: ['update:modelValue'],
          template: '<textarea v-if="type === \'textarea\'" :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" /><input v-else :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-input-number': {
          props: ['modelValue', 'min'],
          emits: ['update:modelValue'],
          template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
        },
        'el-select': {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><slot /></select>',
        },
        'el-option': {
          props: ['label', 'value'],
          template: '<option :value="value">{{ label }}</option>',
        },
        'el-button': {
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-tag': { template: '<span><slot /></span>' },
      },
    },
  })
}

beforeEach(() => {
  Object.values(mocks).forEach((mock) => mock.mockReset())
})

describe('MerchantProductsView', () => {
  it('loads and renders product images and SKU inventory', async () => {
    seedMerchantProducts({ products: [product] })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listProducts).toHaveBeenCalledWith(7, {
      page: 1,
      pageSize: 100,
      includeOffSale: true,
    })
    expect(wrapper.get('[data-testid="product-image-1"]').attributes('src')).toBe('/uploads/products/301.webp')
    expect(wrapper.get('[data-testid="sku-row-1001"]').text()).toContain('大份')
    expect(wrapper.get('[data-testid="sku-row-1001"]').text()).toContain('¥18.80')
    expect(wrapper.get('[data-testid="sku-row-1001"]').text()).toContain('库存：20 个')
    expect(wrapper.text()).toContain('在售')
  })

  it('submits category create, rename and delete actions', async () => {
    seedMerchantProducts()
    mocks.createCategory.mockResolvedValue({})
    mocks.updateCategory.mockResolvedValue({})
    mocks.removeCategory.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('饮品')
    await wrapper.findAll('button').find((button) => button.text() === '新增分类').trigger('click')
    await inputs[1].setValue('热销主食')
    await wrapper.findAll('button').find((button) => button.text() === '保存分类').trigger('click')
    await wrapper.findAll('button').find((button) => button.text() === '删除分类').trigger('click')

    expect(mocks.createCategory).toHaveBeenCalledWith(7, { name: '饮品', sortOrder: 2 })
    expect(mocks.updateCategory).toHaveBeenCalledWith(21, { name: '热销主食', sortOrder: 1 })
    expect(mocks.removeCategory).toHaveBeenCalledWith(21)
  })

  it('uploads a main image and stores its returned reference', async () => {
    seedMerchantProducts()
    mocks.uploadProductImage.mockResolvedValue({ id: 301, url: '/uploads/products/301.webp' })
    const wrapper = mountView()
    await flushPromises()

    const file = new File(['image'], 'meal.png', { type: 'image/png' })
    const input = wrapper.get('[data-testid="product-image-input"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    const formData = mocks.uploadProductImage.mock.calls[0][0]
    expect(formData.get('file')).toBe(file)
    expect(wrapper.get('[data-testid="product-image-preview"]').attributes('src')).toBe('/uploads/products/301.webp')
  })

  it('creates a product with its image and initial SKU', async () => {
    seedMerchantProducts()
    mocks.uploadProductImage.mockResolvedValue({ id: 301, url: '/uploads/products/301.webp' })
    mocks.createProduct.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    const imageInput = wrapper.get('[data-testid="product-image-input"]')
    const file = new File(['image'], 'meal.png', { type: 'image/png' })
    Object.defineProperty(imageInput.element, 'files', { value: [file] })
    await imageInput.trigger('change')
    await flushPromises()
    await wrapper.get('[data-testid="product-name"]').setValue('牛肉饭')
    await wrapper.get('[data-testid="initial-sku-name"]').setValue('默认规格')
    await wrapper.get('[data-testid="initial-sku-price"]').setValue('18.8')
    await wrapper.get('[data-testid="initial-sku-stock"]').setValue('20')
    await wrapper.get('[data-testid="add-product"]').trigger('click')

    expect(mocks.createProduct).toHaveBeenCalledWith({
      shopId: 7,
      categoryId: 21,
      name: '牛肉饭',
      description: '',
      imageId: 301,
      skus: [{ name: '默认规格', price: 18.8, stock: 20 }],
    })
  })

  it('rejects a product without a valid initial SKU', async () => {
    seedMerchantProducts()
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="product-name"]').setValue('牛肉饭')
    await wrapper.get('[data-testid="add-product"]').trigger('click')

    expect(mocks.createProduct).not.toHaveBeenCalled()
    expect(mocks.messageError).toHaveBeenCalledWith('请填写有效商品和规格信息')
  })

  it('updates only product-level editable fields', async () => {
    seedMerchantProducts({ products: [product] })
    mocks.updateProduct.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="product-edit-1"]').trigger('click')
    await wrapper.get('[data-testid="product-name"]').setValue('升级牛肉饭')
    await wrapper.get('[data-testid="save-product"]').trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(1, {
      categoryId: 21,
      name: '升级牛肉饭',
      description: '招牌套餐',
      imageId: 301,
    })
  })

  it('toggles product status without a product version', async () => {
    seedMerchantProducts({ products: [product] })
    mocks.updateProduct.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="product-toggle-1"]').trigger('click')
    await wrapper.findAll('.confirm-button')[0].trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(1, { status: 'OFF_SALE' })
  })

  it('blocks product on-sale when the main image is missing', async () => {
    seedMerchantProducts({ products: [{ ...product, image: null, status: 'OFF_SALE' }] })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="product-toggle-1"]').trigger('click')
    await wrapper.findAll('.confirm-button')[0].trigger('click')

    expect(mocks.updateProduct).not.toHaveBeenCalled()
    expect(mocks.messageError).toHaveBeenCalledWith('商品上架前必须上传主图')
  })

  it('creates another SKU for an existing product', async () => {
    seedMerchantProducts({ products: [product] })
    mocks.createProductSku.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="add-sku-1"]').trigger('click')
    await wrapper.get('[data-testid="sku-name"]').setValue('小份')
    await wrapper.get('[data-testid="sku-price"]').setValue('15.8')
    await wrapper.get('[data-testid="sku-stock"]').setValue('12')
    await wrapper.get('[data-testid="save-sku"]').trigger('click')

    expect(mocks.createProductSku).toHaveBeenCalledWith(1, {
      name: '小份',
      price: 15.8,
      stock: 12,
    })
  })

  it('updates an existing SKU with its current version', async () => {
    seedMerchantProducts({ products: [product] })
    mocks.updateProductSku.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="sku-edit-1001"]').trigger('click')
    await wrapper.get('[data-testid="sku-price"]').setValue('20')
    await wrapper.get('[data-testid="save-sku"]').trigger('click')

    expect(mocks.updateProductSku).toHaveBeenCalledWith(1001, {
      name: '大份',
      price: 20,
      stock: 20,
      version: 3,
    })
  })

  it('toggles SKU status with optimistic-lock version', async () => {
    seedMerchantProducts({ products: [product] })
    mocks.updateProductSku.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="sku-toggle-1001"]').trigger('click')
    await wrapper.findAll('.confirm-button')[1].trigger('click')

    expect(mocks.updateProductSku).toHaveBeenCalledWith(1001, {
      status: 'OFF_SALE',
      version: 3,
    })
  })
})
