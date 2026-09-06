import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantProductsView from '../../views/MerchantProductsView.vue'

const mocks = vi.hoisted(() => ({
  getMerchantProfile: vi.fn(),
  listStores: vi.fn(),
  listProducts: vi.fn(),
  updateProduct: vi.fn(),
  createProduct: vi.fn(),
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
  mocks.getMerchantProfile.mockReset()
  mocks.listStores.mockReset()
  mocks.listProducts.mockReset()
  mocks.updateProduct.mockReset()
  mocks.createProduct.mockReset()
})

describe('MerchantProductsView', () => {
  it('loads merchant products and renders product data', async () => {
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
    mocks.listProducts.mockResolvedValue({
      items: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: '上架',
          version: 3,
        },
      ],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getMerchantProfile).toHaveBeenCalledTimes(1)
    expect(mocks.listStores).toHaveBeenCalledWith({ mine: true, page: 1, pageSize: 100 })
    expect(mocks.listProducts).toHaveBeenCalledWith(7, {
      page: 1,
      pageSize: 100,
      includeOffSale: true,
    })
    expect(mocks.listProducts).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('21')
    expect(wrapper.text()).toContain('18.8')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('上架')
    expect(wrapper.text()).toContain('新增商品')
    expect(wrapper.text()).toContain('新增分类')
  })

  it('submits off-shelf action for a product', async () => {
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
    mocks.listProducts.mockResolvedValue({
      items: [
        {
          id: 1,
          name: '招牌牛肉饭',
          categoryId: 21,
          price: 18.8,
          stock: 20,
          status: '上架',
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
    mocks.listProducts.mockResolvedValue({
      items: [],
      total: 0,
    })
    mocks.createProduct.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('button').trigger('click')

    expect(mocks.createProduct).toHaveBeenCalledWith({
      shopId: 7,
      categoryId: null,
      name: '',
      price: 0,
      stock: 0,
    })
  })
})
