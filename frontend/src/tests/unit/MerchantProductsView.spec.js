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
  updateProduct: vi.fn(),
  createProduct: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/category', () => ({
  createCategory: mocks.createCategory,
  listCategories: mocks.listCategories,
  removeCategory: mocks.removeCategory,
  updateCategory: mocks.updateCategory,
}))

vi.mock('../../api/merchant', () => ({
  getMerchantProfile: mocks.getMerchantProfile,
}))

vi.mock('../../api/store', () => ({
  listStores: mocks.listStores,
}))

vi.mock('../../api/product', () => ({
  listProducts: mocks.listProducts,
  updateProduct: mocks.updateProduct,
  createProduct: mocks.createProduct,
}))

function seedMerchantProducts({ products = [], categories = [{ id: 21, name: '主食', sortOrder: 1 }] } = {}) {
  mocks.getMerchantProfile.mockResolvedValue({
    id: 2,
    account: 'merchant01',
    name: '示例快餐店商家',
    phone: '13900000000',
    status: 'ACTIVE',
  })
  mocks.listStores.mockResolvedValue({
    items: [{ id: 7 }],
  })
  mocks.listCategories.mockResolvedValue(categories)
  mocks.listProducts.mockResolvedValue({
    items: products,
    total: products.length,
  })
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
          props: ['modelValue', 'placeholder'],
          emits: ['update:modelValue'],
          template: '<input :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
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
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.createCategory.mockReset()
  mocks.listCategories.mockReset()
  mocks.removeCategory.mockReset()
  mocks.updateCategory.mockReset()
  mocks.getMerchantProfile.mockReset()
  mocks.listStores.mockReset()
  mocks.listProducts.mockReset()
  mocks.updateProduct.mockReset()
  mocks.createProduct.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantProductsView', () => {
  it('loads categories as a direct array and renders product data', async () => {
    seedMerchantProducts({
      products: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: 'ON_SALE',
          version: 3,
        },
      ],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getMerchantProfile).toHaveBeenCalledTimes(1)
    expect(mocks.listStores).toHaveBeenCalledWith({ mine: true, page: 1, pageSize: 100 })
    expect(mocks.listCategories).toHaveBeenCalledWith(7)
    expect(mocks.listProducts).toHaveBeenCalledWith(7, {
      page: 1,
      pageSize: 100,
      includeOffSale: true,
    })
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('18.8')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('ON_SALE')
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

  it('submits create product with valid contract fields', async () => {
    seedMerchantProducts()
    mocks.createProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[2].setValue('牛肉饭')
    await inputs[3].setValue('18.8')
    await inputs[4].setValue('20')
    await wrapper.findAll('button').find((button) => button.text() === '新增商品').trigger('click')

    expect(mocks.createProduct).toHaveBeenCalledWith({
      shopId: 7,
      categoryId: 21,
      name: '牛肉饭',
      price: 18.8,
      stock: 20,
    })
  })

  it('shows validation error instead of submitting invalid product payload', async () => {
    seedMerchantProducts()

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '新增商品').trigger('click')

    expect(mocks.createProduct).not.toHaveBeenCalled()
    expect(mocks.messageError).toHaveBeenCalledWith('请填写有效商品信息')
  })

  it('submits product edit with current version', async () => {
    seedMerchantProducts({
      products: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: 'ON_SALE',
          version: 3,
        },
      ],
    })
    mocks.updateProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '编辑').trigger('click')
    const inputs = wrapper.findAll('input')
    await inputs[2].setValue('升级牛肉饭')
    await inputs[3].setValue('20')
    await inputs[4].setValue('18')
    await wrapper.findAll('button').find((button) => button.text() === '保存商品').trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(1, {
      categoryId: 21,
      name: '升级牛肉饭',
      price: 20,
      stock: 18,
      version: 3,
    })
  })

  it('submits off-shelf action for a product with version', async () => {
    seedMerchantProducts({
      products: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: 'ON_SALE',
          version: 3,
        },
      ],
    })
    mocks.updateProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('.confirm-button').trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(1, {
      status: 'OFF_SALE',
      version: 3,
    })
  })

  it('submits on-sale action for an off-sale product with version', async () => {
    seedMerchantProducts({
      products: [
        {
          id: 2,
          name: '暂停售商品',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: 'OFF_SALE',
          version: 4,
        },
      ],
    })
    mocks.updateProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('上架')
    await wrapper.find('.confirm-button').trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(2, {
      status: 'ON_SALE',
      version: 4,
    })
  })
})
