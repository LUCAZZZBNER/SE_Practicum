import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthHomeView from '../../views/AuthHomeView.vue'

const mocks = vi.hoisted(() => ({
  loginCustomer: vi.fn(),
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
  loginCustomer: mocks.loginCustomer,
  loginMerchant: mocks.loginMerchant,
}))

function mountView() {
  return mount(AuthHomeView, {
    global: {
      stubs: {
        'el-card': {
          template: '<section><slot /></section>',
        },
        'el-form': {
          template: '<form><slot /></form>',
        },
        'el-form-item': {
          template: '<div><slot /></div>',
        },
        'el-input': {
          props: ['modelValue', 'placeholder', 'type'],
          emits: ['update:modelValue'],
          template: '<input :type="type || \'text\'" :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        'el-button': {
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  localStorage.clear()
  mocks.loginCustomer.mockReset()
  mocks.loginMerchant.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('AuthHomeView', () => {
  it('shows customer login by default', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('轻量级外卖服务平台')
    expect(wrapper.get('[data-testid="customer-tab"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('input[placeholder="请输入用户账号"]')).toBeTruthy()
    expect(wrapper.find('input[placeholder="请输入商家账号"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="login-submit"]').text()).toBe('用户登录')
  })

  it('switches to the merchant login form', async () => {
    const wrapper = mountView()

    await wrapper.get('[data-testid="merchant-tab"]').trigger('click')

    expect(wrapper.get('[data-testid="merchant-tab"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('input[placeholder="请输入商家账号"]')).toBeTruthy()
    expect(wrapper.find('input[placeholder="请输入用户账号"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="login-submit"]').text()).toBe('商家登录')
  })

  it('uses the customer login contract and enters the customer area', async () => {
    mocks.loginCustomer.mockResolvedValue({ accessToken: 'customer-token', roles: ['USER'] })
    const wrapper = mountView()

    await wrapper.get('input[placeholder="请输入用户账号"]').setValue('alice01')
    await wrapper.get('input[placeholder="请输入密码"]').setValue('ExamplePass123!')
    await wrapper.get('[data-testid="login-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.loginCustomer).toHaveBeenCalledWith({
      account: 'alice01',
      password: 'ExamplePass123!',
    })
    expect(mocks.loginMerchant).not.toHaveBeenCalled()
    expect(localStorage.getItem('access_token')).toBe('customer-token')
    expect(localStorage.getItem('user_role')).toBe('USER')
    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/stores')
  })

  it('uses the merchant login contract and enters the merchant area', async () => {
    mocks.loginMerchant.mockResolvedValue({ accessToken: 'merchant-token', roles: ['MERCHANT'] })
    const wrapper = mountView()

    await wrapper.get('[data-testid="merchant-tab"]').trigger('click')
    await wrapper.get('input[placeholder="请输入商家账号"]').setValue('merchant01')
    await wrapper.get('input[placeholder="请输入密码"]').setValue('ExamplePass123!')
    await wrapper.get('[data-testid="login-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.loginMerchant).toHaveBeenCalledWith({
      account: 'merchant01',
      password: 'ExamplePass123!',
    })
    expect(mocks.loginCustomer).not.toHaveBeenCalled()
    expect(localStorage.getItem('access_token')).toBe('merchant-token')
    expect(localStorage.getItem('user_role')).toBe('MERCHANT')
    expect(mocks.routerPush).toHaveBeenCalledWith('/merchant/store')
  })

  it('keeps customer and merchant registration routes independent', async () => {
    const wrapper = mountView()

    await wrapper.get('[data-testid="register-link"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenLastCalledWith('/register/customer')

    await wrapper.get('[data-testid="merchant-tab"]').trigger('click')
    await wrapper.get('[data-testid="register-link"]').trigger('click')
    expect(mocks.routerPush).toHaveBeenLastCalledWith('/register/merchant')
  })

  it('shows a login error and stays on the page when authentication fails', async () => {
    mocks.loginCustomer.mockRejectedValue(new Error('账号或密码错误'))
    const wrapper = mountView()

    await wrapper.get('input[placeholder="请输入用户账号"]').setValue('alice01')
    await wrapper.get('input[placeholder="请输入密码"]').setValue('wrong-password')
    await wrapper.get('[data-testid="login-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('账号或密码错误')
    expect(mocks.routerPush).not.toHaveBeenCalledWith('/customer/stores')
  })
})
