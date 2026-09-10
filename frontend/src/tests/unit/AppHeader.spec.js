import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppHeader from '../../components/layout/AppHeader.vue'

const mocks = vi.hoisted(() => ({
  routerReplace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplace,
  }),
}))

function mountHeader() {
  return mount(AppHeader, {
    props: {
      title: '轻量级外卖',
      profilePath: '/customer/profile',
    },
    global: {
      stubs: {
        'el-header': {
          template: '<header><slot /></header>',
        },
        'el-icon': {
          template: '<i><slot /></i>',
        },
        'router-link': {
          props: ['to'],
          emits: ['click'],
          template: '<a :data-to="typeof to === \'string\' ? to : to.path" @click="$emit(\'click\')"><slot /></a>',
        },
        'el-tooltip': {
          template: '<span><slot /></span>',
        },
        'el-button': {
          props: ['ariaLabel'],
          emits: ['click'],
          template: '<button type="button" :aria-label="ariaLabel" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

describe('AppHeader', () => {
  beforeEach(() => {
    localStorage.clear()
    mocks.routerReplace.mockReset()
  })

  it('renders the platform brand and customer profile entry', () => {
    const wrapper = mountHeader()

    expect(wrapper.text()).toContain('轻量级外卖')
    expect(wrapper.get('[data-testid="profile-link"]').attributes('data-to')).toBe('/customer/profile')
    expect(wrapper.get('[data-testid="logout-button"]').attributes('aria-label')).toBe('退出登录')
  })

  it('clears auth state and returns to home when logging out', async () => {
    localStorage.setItem('access_token', 'token-123')
    localStorage.setItem('user_role', 'USER')

    const wrapper = mountHeader()
    await wrapper.get('[data-testid="logout-button"]').trigger('click')

    expect(localStorage.getItem('access_token')).toBeNull()
    expect(localStorage.getItem('user_role')).toBeNull()
    expect(mocks.routerReplace).toHaveBeenCalledWith('/')
  })
})
