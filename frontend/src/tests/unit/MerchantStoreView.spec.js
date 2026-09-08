import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantStoreView from '../../views/MerchantStoreView.vue'

const mocks = vi.hoisted(() => ({
  listStores: vi.fn(),
  getStoreDetail: vi.fn(),
  createShop: vi.fn(),
  updateStoreStatus: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/store', () => ({
  listStores: mocks.listStores,
  getStoreDetail: mocks.getStoreDetail,
  createShop: mocks.createShop,
  updateStoreStatus: mocks.updateStoreStatus,
}))

function mountView() {
  return mount(MerchantStoreView, {
    global: {
      stubs: {
        'el-form': {
          template: '<form><slot /></form>',
        },
        'el-form-item': {
          template: '<div><slot /></div>',
        },
        'el-input': {
          props: ['modelValue', 'type'],
          emits: ['update:modelValue'],
          template:
            '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-radio-group': {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<div><slot /></div>',
        },
        'el-radio-button': {
          props: ['label'],
          template: '<button type="button"><slot /></button>',
        },
        'el-button': {
          template: '<button type="button"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listStores.mockReset()
  mocks.getStoreDetail.mockReset()
  mocks.createShop.mockReset()
  mocks.updateStoreStatus.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantStoreView', () => {
  it('loads merchant store detail and renders current status', async () => {
    mocks.listStores.mockResolvedValue({
      items: [{ id: 7 }],
    })
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      status: 'OPEN',
      description: '欢迎下单',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listStores).toHaveBeenCalledWith({ mine: true, page: 1, pageSize: 100 })
    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('OPEN')
    expect(wrapper.text()).toContain('欢迎下单')
    expect(wrapper.text()).toContain('保存店铺')
  })

  it('submits store status change with edited form values', async () => {
    mocks.listStores.mockResolvedValue({
      items: [{ id: 7 }],
    })
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      status: 'OPEN',
      description: '欢迎下单',
    })
    mocks.updateStoreStatus.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('新店铺名')
    await inputs[1].setValue('临时休息')
    await wrapper.find('button').trigger('click')

    expect(mocks.updateStoreStatus).toHaveBeenCalledWith(7, {
      name: '新店铺名',
      status: 'OPEN',
      description: '临时休息',
    })
  })

  it('creates a store when merchant has no existing shop', async () => {
    mocks.listStores.mockResolvedValue({
      items: [],
      total: 0,
    })
    mocks.createShop.mockResolvedValue({
      id: 9,
      name: '新店铺',
      description: '新店介绍',
      status: 'CLOSED',
    })

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('新店铺')
    await inputs[1].setValue('新店介绍')
    await wrapper.find('button').trigger('click')

    expect(mocks.getStoreDetail).not.toHaveBeenCalled()
    expect(mocks.createShop).toHaveBeenCalledWith({
      name: '新店铺',
      description: '新店介绍',
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('shows backend error when store update fails', async () => {
    mocks.listStores.mockResolvedValue({
      items: [{ id: 7 }],
    })
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      status: 'OPEN',
      description: '欢迎下单',
    })
    mocks.updateStoreStatus.mockRejectedValue(new Error('保存失败'))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('保存失败')
  })
})
