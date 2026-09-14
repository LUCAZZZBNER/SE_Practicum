import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProfileView from '../../views/ProfileView.vue'

const state = vi.hoisted(() => ({
  currentPath: '/customer/profile',
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  getMerchantProfile: vi.fn(),
  updateMerchantProfile: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ path: state.currentPath }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: state.messageSuccess,
    error: state.messageError,
  },
}))

vi.mock('../../api/user', () => ({
  getProfile: state.getProfile,
  updateProfile: state.updateProfile,
}))

vi.mock('../../api/merchant', () => ({
  getMerchantProfile: state.getMerchantProfile,
  updateMerchantProfile: state.updateMerchantProfile,
}))

function mountView() {
  return mount(ProfileView, {
    global: {
      stubs: {
        'el-form': {
          template: '<form><slot /></form>',
        },
        'el-form-item': {
          props: ['label'],
          template: '<label><span v-if="label">{{ label }}</span><slot /></label>',
        },
        'el-input': {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-button': {
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': { template: '<i><slot /></i>' },
      },
    },
  })
}

beforeEach(() => {
  localStorage.clear()
  state.currentPath = '/customer/profile'
  state.getProfile.mockReset()
  state.updateProfile.mockReset()
  state.getMerchantProfile.mockReset()
  state.updateMerchantProfile.mockReset()
  state.messageSuccess.mockReset()
  state.messageError.mockReset()
})

describe('ProfileView', () => {
  it('loads customer profile and shows customer fields', async () => {
    localStorage.setItem('user_role', 'USER')
    state.getProfile.mockResolvedValue({
      account: 'alice01',
      nickname: 'Alice',
      phone: '13800000000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(state.getProfile).toHaveBeenCalledTimes(1)
    const identity = wrapper.get('[data-testid="profile-identity"]')
    expect(identity.text()).toContain('账号：alice01')
    expect(identity.text()).toContain('状态：正常')
    expect(identity.text()).not.toContain('ACTIVE')
    expect(wrapper.text()).toContain('账户信息')
    expect(wrapper.text()).toContain('可编辑资料')
    expect(wrapper.text().match(/昵称/g)).toHaveLength(1)
    expect(wrapper.text().match(/手机号/g)).toHaveLength(1)
    expect(wrapper.findAll('input')).toHaveLength(2)
    expect(wrapper.text()).toContain('保存修改')
  })

  it('submits customer profile updates with nickname and phone only', async () => {
    localStorage.setItem('user_role', 'USER')
    state.getProfile.mockResolvedValue({
      account: 'alice01',
      nickname: 'Alice',
      phone: '13800000000',
      status: 'ACTIVE',
    })
    let resolveUpdate
    state.updateProfile.mockReturnValue(new Promise((resolve) => {
      resolveUpdate = resolve
    }))

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    expect(inputs).toHaveLength(2)
    await inputs[0].setValue('Alice New')
    await inputs[1].setValue('13900000000')
    const saveButton = wrapper.get('[data-testid="save-profile"]')
    const request = saveButton.trigger('click')
    await flushPromises()

    expect(state.updateProfile).toHaveBeenCalledWith({
      nickname: 'Alice New',
      phone: '13900000000',
    })
    expect(saveButton.element.disabled).toBe(true)

    resolveUpdate({
      account: 'alice01',
      nickname: 'Alice Server',
      phone: '13900000001',
      status: 'ACTIVE',
    })
    await request
    await flushPromises()

    expect(wrapper.findAll('input')[0].element.value).toBe('Alice Server')
    expect(wrapper.findAll('input')[1].element.value).toBe('13900000001')
    expect(saveButton.element.disabled).toBe(false)
    expect(state.messageSuccess).toHaveBeenCalledWith('保存成功')
  })

  it('loads merchant profile and shows merchant fields', async () => {
    localStorage.setItem('user_role', 'MERCHANT')
    state.currentPath = '/merchant/profile'
    state.getMerchantProfile.mockResolvedValue({
      account: 'merchant01',
      name: '示例快餐店商家',
      phone: '13900000000',
      status: 'SUSPENDED',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(state.getMerchantProfile).toHaveBeenCalledTimes(1)
    const identity = wrapper.get('[data-testid="profile-identity"]')
    expect(identity.text()).toContain('账号：merchant01')
    expect(identity.text()).toContain('状态：已暂停')
    expect(identity.text()).not.toContain('SUSPENDED')
    expect(wrapper.text()).toContain('账户信息')
    expect(wrapper.text()).toContain('可编辑资料')
    expect(wrapper.text().match(/商家名称/g)).toHaveLength(1)
    expect(wrapper.text().match(/手机号/g)).toHaveLength(1)
    expect(wrapper.findAll('input')).toHaveLength(2)
    expect(wrapper.text()).toContain('保存修改')
  })

  it('submits merchant profile updates with name and phone only', async () => {
    localStorage.setItem('user_role', 'MERCHANT')
    state.currentPath = '/merchant/profile'
    state.getMerchantProfile.mockResolvedValue({
      account: 'merchant01',
      name: '示例快餐店商家',
      phone: '13900000000',
      status: 'ACTIVE',
    })
    let resolveUpdate
    state.updateMerchantProfile.mockReturnValue(new Promise((resolve) => {
      resolveUpdate = resolve
    }))

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    expect(inputs).toHaveLength(2)
    await inputs[0].setValue('示例快餐店商家（新）')
    await inputs[1].setValue('13911110000')
    const saveButton = wrapper.get('[data-testid="save-profile"]')
    const request = saveButton.trigger('click')
    await flushPromises()

    expect(state.updateMerchantProfile).toHaveBeenCalledWith({
      name: '示例快餐店商家（新）',
      phone: '13911110000',
    })
    expect(saveButton.element.disabled).toBe(true)

    resolveUpdate({
      account: 'merchant01',
      name: '服务端商家名称',
      phone: '13911110001',
      status: 'ACTIVE',
    })
    await request
    await flushPromises()

    expect(wrapper.findAll('input')[0].element.value).toBe('服务端商家名称')
    expect(wrapper.findAll('input')[1].element.value).toBe('13911110001')
    expect(saveButton.element.disabled).toBe(false)
    expect(state.messageSuccess).toHaveBeenCalledWith('保存成功')
  })
})
