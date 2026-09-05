import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AuthHomeView from '../../views/AuthHomeView.vue'

const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
}))

function mountView() {
  return mount(AuthHomeView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
        'el-card': {
          template: '<section><slot /></section>',
        },
        'el-button': {
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

describe('AuthHomeView', () => {
  it('renders separate entry points for customer and merchant users', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('轻量级外卖服务平台')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('商家')
    expect(wrapper.text()).toContain('用户登录')
    expect(wrapper.text()).toContain('用户注册')
    expect(wrapper.text()).toContain('商家登录')
    expect(wrapper.text()).toContain('商家注册')
  })

  it('navigates to customer login and register routes from the homepage', async () => {
    const wrapper = mountView()

    await wrapper.findAll('button')[0].trigger('click')
    await wrapper.findAll('button')[1].trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/login/customer')
    expect(mocks.routerPush).toHaveBeenCalledWith('/register/customer')
  })

  it('navigates to merchant login and register routes from the homepage', async () => {
    const wrapper = mountView()

    await wrapper.findAll('button')[2].trigger('click')
    await wrapper.findAll('button')[3].trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/login/merchant')
    expect(mocks.routerPush).toHaveBeenCalledWith('/register/merchant')
  })
})
