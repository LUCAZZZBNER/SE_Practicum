import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantProductsView from '../../views/MerchantProductsView.vue'

const mocks = vi.hoisted(() => ({
  listProducts: vi.fn(),
  updateProduct: vi.fn(),
  createProduct: vi.fn(),
  listStores: vi.fn(),
  listCategories: vi.fn(),
}))

vi.mock('../../api/product', () => ({
  listProducts: mocks.listProducts,
  updateProduct: mocks.updateProduct,
  createProduct: mocks.createProduct,
}))

vi.mock('../../api/store', () => ({
  listStores: mocks.listStores,
  listCategories: mocks.listCategories,
  createStore: vi.fn(),
}))

function mountView() {
  return mount(MerchantProductsView, {
    global: {
      stubs: {
        ConfirmAction: {
          template: '<div class="confirm"><slot /></div>',
        },
        'el-table': {
          template: '<div><slot /></div>',
        },
        'el-table-column': {
          template: '<div />',
        },
        'el-button': {
          template: '<button type="button"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listProducts.mockReset()
  mocks.updateProduct.mockReset()
  mocks.createProduct.mockReset()
  mocks.listStores.mockReset()
  mocks.listCategories.mockReset()
  mocks.listStores.mockResolvedValue({ items: [{ id: 7, name: '示例快餐店' }] })
  mocks.listCategories.mockResolvedValue([{ id: 5, name: '主食' }])
})

describe('MerchantProductsView', () => {
  it('loads merchant products and renders product data', async () => {
    mocks.listProducts.mockResolvedValue({
      items: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 5,
          price: 18.8,
          stock: 20,
          status: 'ON_SALE',
          version: 3,
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listProducts).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('主食')
    expect(wrapper.text()).toContain('18.8')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('ON_SALE')
    expect(wrapper.text()).toContain('新增商品')
    expect(wrapper.text()).toContain('新增分类')
  })

  it('submits off-shelf action for a product', async () => {
    mocks.listProducts.mockResolvedValue({
      items: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 5,
          price: 18.8,
          stock: 20,
          status: 'ON_SALE',
          version: 3,
        },
      ],
      total: 1,
    })
    mocks.updateProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button')[3].trigger('click')

    expect(mocks.updateProduct).toHaveBeenCalledWith(1, {
      status: 'OFF_SALE',
      version: 3,
    })
  })

  it('submits create product action from the primary button', async () => {
    mocks.listProducts.mockResolvedValue({
      items: [],
      total: 0,
    })
    mocks.createProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('button').trigger('click')

    expect(mocks.createProduct).toHaveBeenCalled()
  })
})
