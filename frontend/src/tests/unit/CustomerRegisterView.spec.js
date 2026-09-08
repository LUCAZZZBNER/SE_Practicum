import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerRegisterView from '../../views/CustomerRegisterView.vue'

const mocks = vi.hoisted(() => ({
  registerCustomer: vi.fn(),
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
  registerCustomer: mocks.registerCustomer,
}))

function mountView() {
  return mount(CustomerRegisterView, {
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
  mocks.registerCustomer.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageSuccess.mockReset()
  mocks.messageError.mockReset()
})

describe('CustomerRegisterView', () => {
  it('renders customer register form and actions', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('普通用户注册')
    expect(wrapper.text()).toContain('提交注册')
    expect(wrapper.text()).toContain('去登录')
    expect(wrapper.text()).toContain('返回首页')
  })

  it('submits customer registration with contract fields and redirects to login', async () => {
    mocks.registerCustomer.mockResolvedValue({
      id: 1,
      account: 'alice01',
      nickname: 'Alice',
      phone: '13800000000',
      status: 'ACTIVE',
    })

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    expect(inputs).toHaveLength(5)
    await inputs[0].setValue('alice01')
    await inputs[1].setValue('ExamplePass123!')
    await inputs[2].setValue('ExamplePass123!')
    await inputs[3].setValue('Alice')
    await inputs[4].setValue('13800000000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.registerCustomer).toHaveBeenCalledWith({
      account: 'alice01',
      password: 'ExamplePass123!',
      passwordConfirm: 'ExamplePass123!',
      nickname: 'Alice',
      phone: '13800000000',
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('注册成功')
    expect(mocks.routerPush).toHaveBeenCalledWith('/login/customer')
  })

  it('shows backend error when customer registration fails', async () => {
    mocks.registerCustomer.mockRejectedValue(new Error('用户账号已存在'))

    const wrapper = mountView()
    const inputs = wrapper.findAll('input')

    expect(inputs).toHaveLength(5)
    await inputs[0].setValue('alice01')
    await inputs[1].setValue('ExamplePass123!')
    await inputs[2].setValue('ExamplePass123!')
    await inputs[3].setValue('Alice')
    await inputs[4].setValue('13800000000')
    await wrapper.find('button').trigger('click')

    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('用户账号已存在')
    expect(mocks.routerPush).not.toHaveBeenCalledWith('/login/customer')
  })
})
