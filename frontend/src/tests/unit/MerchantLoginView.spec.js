import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MerchantLoginView from '../../views/MerchantLoginView.vue'

const mocks = vi.hoisted(() => ({
  loginMerchant: vi.fn(),
  routerPush: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPush,
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
  },
}))

vi.mock('../../api/user', () => ({
  loginMerchant: mocks.loginMerchant,
}))

function mountView() {
  return mount(MerchantLoginView, {
    global: {
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
  mocks.loginMerchant.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('MerchantLoginView', () => {
  it('renders merchant login form and actions', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('商家登录')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('商家注册')
    expect(wrapper.text()).toContain('返回首页')
  })

  it('submits merchant login and redirects to merchant home', async () => {
    mocks.loginMerchant.mockResolvedValue({
      accessToken: 'token-merchant',
      tokenType: 'Bearer',
      expiresIn: 7200,
      merchant: {
        id: 2,
        account: 'merchant01',
        name: '示例快餐店商家',
        phone: '13900000000',
        status: 'ACTIVE',
      },
      roles: ['MERCHANT'],
    })

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('merchant01')
    await inputs[1].setValue('ExamplePass123!')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.loginMerchant).toHaveBeenCalledWith({
      account: 'merchant01',
      password: 'ExamplePass123!',
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('登录成功')
    expect(mocks.routerPush).toHaveBeenCalledWith('/merchant/store')
  })

  it('shows backend error when merchant login fails', async () => {
    mocks.loginMerchant.mockRejectedValue(new Error('账号或密码错误'))

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('merchant01')
    await inputs[1].setValue('wrong-password')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('账号或密码错误')
    expect(mocks.routerPush).not.toHaveBeenCalledWith('/merchant/store')
  })
})
