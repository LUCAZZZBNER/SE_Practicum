import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerLoginView from '../../views/CustomerLoginView.vue'

const mocks = vi.hoisted(() => {
  return {
    loginCustomer: vi.fn(),
    routerPush: vi.fn(),
    messageSuccess: vi.fn(),
    messageError: vi.fn(),
  }
})

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
  loginCustomer: mocks.loginCustomer,
}))

function mountView() {
  return mount(CustomerLoginView, {
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
  localStorage.clear()
  mocks.loginCustomer.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('CustomerLoginView', () => {
  it('renders customer login form and actions', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('普通用户登录')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('用户注册')
    expect(wrapper.text()).toContain('返回首页')
  })

  it('submits login request, persists auth state and redirects to customer home', async () => {
    mocks.loginCustomer.mockResolvedValue({
      accessToken: 'token-123',
      tokenType: 'Bearer',
      expiresIn: 7200,
      user: {
        id: 1,
        account: 'alice01',
        nickname: 'Alice',
        phone: '13800000000',
        status: 'ACTIVE',
      },
      roles: ['USER'],
    })

    const wrapper = mountView()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('alice01')
    await inputs[1].setValue('ExamplePass123!')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.loginCustomer).toHaveBeenCalledWith({
      account: 'alice01',
      password: 'ExamplePass123!',
    })
    expect(localStorage.getItem('access_token')).toBe('token-123')
    expect(localStorage.getItem('user_role')).toBe('USER')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/stores')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('登录成功')
  })

  it('shows backend error and stays on page when login fails', async () => {
    mocks.loginCustomer.mockRejectedValue(new Error('账号或密码错误'))

    const wrapper = mountView()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('alice01')
    await inputs[1].setValue('wrong-password')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('账号或密码错误')
    expect(mocks.routerPush).not.toHaveBeenCalledWith('/customer/stores')
  })
})
