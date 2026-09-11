import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerAddressesView from '../../views/CustomerAddressesView.vue'

const mocks = vi.hoisted(() => ({
  createUserAddress: vi.fn(),
  listUserAddresses: vi.fn(),
  removeUserAddress: vi.fn(),
  updateUserAddress: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/address', () => ({
  createUserAddress: mocks.createUserAddress,
  listUserAddresses: mocks.listUserAddresses,
  removeUserAddress: mocks.removeUserAddress,
  updateUserAddress: mocks.updateUserAddress,
}))

function mountView() {
  return mount(CustomerAddressesView, {
    global: {
      stubs: {
        EmptyState: {
          props: ['description'],
          template: '<div data-testid="empty-state">{{ description }}</div>',
        },
        ConfirmAction: {
          emits: ['confirm'],
          template: '<div><slot /><button data-testid="confirm-delete" @click="$emit(\'confirm\')">确认删除</button></div>',
        },
        'el-dialog': {
          props: ['modelValue', 'title'],
          emits: ['update:modelValue'],
          template: '<section v-if="modelValue" data-testid="address-dialog"><h2>{{ title }}</h2><slot /><slot name="footer" /></section>',
        },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<label><slot /></label>' },
        'el-input': {
          inheritAttrs: false,
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<input :data-testid="$attrs[\'data-testid\']" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-checkbox': {
          inheritAttrs: false,
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<input type="checkbox" :data-testid="$attrs[\'data-testid\']" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
        },
        'el-button': {
          inheritAttrs: false,
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button type="button" :data-testid="$attrs[\'data-testid\']" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-tag': { template: '<span><slot /></span>' },
      },
    },
  })
}

const address = {
  id: 51,
  recipient: '张三',
  phone: '13800000000',
  region: '浙江省杭州市西湖区',
  detail: '文三路 1 号 101 室',
  isDefault: true,
}

beforeEach(() => {
  mocks.createUserAddress.mockReset()
  mocks.listUserAddresses.mockReset()
  mocks.removeUserAddress.mockReset()
  mocks.updateUserAddress.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
  mocks.listUserAddresses.mockResolvedValue([address])
})

describe('CustomerAddressesView', () => {
  it('loads addresses and marks the default address', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listUserAddresses).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="address-51"]').text()).toContain('张三')
    expect(wrapper.get('[data-testid="address-51"]').text()).toContain('文三路 1 号 101 室')
    expect(wrapper.get('[data-testid="address-51"]').text()).toContain('默认地址')
  })

  it('shows the shared empty state when no address exists', async () => {
    mocks.listUserAddresses.mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="empty-state"]').text()).toContain('暂无收货地址')
  })

  it('creates an address from the dialog and reloads the list', async () => {
    mocks.createUserAddress.mockResolvedValue({ id: 52 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="add-address"]').trigger('click')
    await wrapper.get('[data-testid="address-recipient"]').setValue('李四')
    await wrapper.get('[data-testid="address-phone"]').setValue('13900000000')
    await wrapper.get('[data-testid="address-region"]').setValue('浙江省杭州市拱墅区')
    await wrapper.get('[data-testid="address-detail"]').setValue('湖墅南路 8 号')
    await wrapper.get('[data-testid="address-default"]').setValue(true)
    await wrapper.get('[data-testid="save-address"]').trigger('click')
    await flushPromises()

    expect(mocks.createUserAddress).toHaveBeenCalledWith({
      recipient: '李四',
      phone: '13900000000',
      region: '浙江省杭州市拱墅区',
      detail: '湖墅南路 8 号',
      isDefault: true,
    })
    expect(mocks.listUserAddresses).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('地址已新增')
  })

  it('edits an existing address using its complete current values', async () => {
    mocks.updateUserAddress.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="edit-address-51"]').trigger('click')
    await wrapper.get('[data-testid="address-detail"]').setValue('文三路 2 号 201 室')
    await wrapper.get('[data-testid="save-address"]').trigger('click')
    await flushPromises()

    expect(mocks.updateUserAddress).toHaveBeenCalledWith(51, {
      recipient: '张三',
      phone: '13800000000',
      region: '浙江省杭州市西湖区',
      detail: '文三路 2 号 201 室',
      isDefault: true,
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('地址已更新')
  })

  it('sets a non-default address as default and reloads the list', async () => {
    mocks.listUserAddresses.mockResolvedValue([{ ...address, isDefault: false }])
    mocks.updateUserAddress.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="default-address-51"]').trigger('click')
    await flushPromises()

    expect(mocks.updateUserAddress).toHaveBeenCalledWith(51, { isDefault: true })
    expect(mocks.listUserAddresses).toHaveBeenCalledTimes(2)
  })

  it('deletes an address after confirmation', async () => {
    mocks.removeUserAddress.mockResolvedValue({ id: 51, deleted: true })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="confirm-delete"]').trigger('click')
    await flushPromises()

    expect(mocks.removeUserAddress).toHaveBeenCalledWith(51)
    expect(mocks.listUserAddresses).toHaveBeenCalledTimes(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('地址已删除')
  })

  it('shows the backend message when loading fails', async () => {
    mocks.listUserAddresses.mockRejectedValue(new Error('地址加载失败'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('地址加载失败')
  })
})
