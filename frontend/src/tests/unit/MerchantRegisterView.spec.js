import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantRegisterView from '../../views/MerchantRegisterView.vue'

const mocks = vi.hoisted(() => ({
  registerMerchant: vi.fn(),
  routerPush: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/user', () => ({
  registerMerchant: mocks.registerMerchant,
}))

function mountView() {
  return mount(MerchantRegisterView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
        AuthFormCard: {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
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
  mocks.registerMerchant.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantRegisterView', () => {
  it('renders merchant register form and actions', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('商家注册')
    expect(wrapper.text()).toContain('提交注册')
    expect(wrapper.text()).toContain('去登录')
    expect(wrapper.text()).toContain('返回首页')
  })

  it('submits merchant registration with contract fields and redirects to login', async () => {
    mocks.registerMerchant.mockResolvedValue({
      id: 2,
      account: 'merchant01',
      name: '示例快餐店商家',
      phone: '13900000000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    expect(inputs).toHaveLength(5)
    await inputs[0].setValue('merchant01')
    await inputs[1].setValue('ExamplePass123!')
    await inputs[2].setValue('ExamplePass123!')
    await inputs[3].setValue('示例快餐店商家')
    await inputs[4].setValue('13900000000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.registerMerchant).toHaveBeenCalledWith({
      account: 'merchant01',
      password: 'ExamplePass123!',
      passwordConfirm: 'ExamplePass123!',
      name: '示例快餐店商家',
      phone: '13900000000',
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('注册成功')
    expect(mocks.routerPush).toHaveBeenCalledWith('/login/merchant')
  })

  it('shows backend error when merchant registration fails', async () => {
    mocks.registerMerchant.mockRejectedValue(new Error('商家账号已存在'))

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    expect(inputs).toHaveLength(5)
    await inputs[0].setValue('merchant01')
    await inputs[1].setValue('ExamplePass123!')
    await inputs[2].setValue('ExamplePass123!')
    await inputs[3].setValue('示例快餐店商家')
    await inputs[4].setValue('13900000000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('商家账号已存在')
    expect(mocks.routerPush).not.toHaveBeenCalledWith('/login/merchant')
  })
})
