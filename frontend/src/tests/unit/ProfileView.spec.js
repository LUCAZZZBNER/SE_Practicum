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
          template: '<div><slot /></div>',
        },
        'el-input': {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-button': {
          template: '<button type="button"><slot /></button>',
        },
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
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('昵称')
    expect(wrapper.text()).toContain('手机号')
    expect(wrapper.text()).toContain('状态')
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
    state.updateProfile.mockResolvedValue({
      account: 'alice01',
      nickname: 'Alice New',
      phone: '13900000000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    expect(inputs).toHaveLength(2)
    await inputs[0].setValue('Alice New')
    await inputs[1].setValue('13900000000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(state.updateProfile).toHaveBeenCalledWith({
      nickname: 'Alice New',
      phone: '13900000000',
    })
    expect(state.messageSuccess).toHaveBeenCalledWith('保存成功')
  })

  it('loads merchant profile and shows merchant fields', async () => {
    localStorage.setItem('user_role', 'MERCHANT')
    state.currentPath = '/merchant/profile'
    state.getMerchantProfile.mockResolvedValue({
      account: 'merchant01',
      name: '示例快餐店商家',
      phone: '13900000000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(state.getMerchantProfile).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('商家名称')
    expect(wrapper.text()).toContain('手机号')
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('状态')
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
    state.updateMerchantProfile.mockResolvedValue({
      account: 'merchant01',
      name: '示例快餐店商家（新）',
      phone: '13911110000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    await flushPromises()

    const inputs = wrapper.findAll('input')
    expect(inputs).toHaveLength(2)
    await inputs[0].setValue('示例快餐店商家（新）')
    await inputs[1].setValue('13911110000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(state.updateMerchantProfile).toHaveBeenCalledWith({
      name: '示例快餐店商家（新）',
      phone: '13911110000',
    })
    expect(state.messageSuccess).toHaveBeenCalledWith('保存成功')
  })
})
