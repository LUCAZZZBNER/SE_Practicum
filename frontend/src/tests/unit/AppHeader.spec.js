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
      title: '外卖平台',
      statusText: '普通用户',
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
          template: '<a @click="$emit(\'click\')"><slot /></a>',
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

  it('renders title and status text', () => {
    const wrapper = mountHeader()

    expect(wrapper.text()).toContain('外卖平台')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('退出')
  })

  it('clears auth state and returns to home when logging out', async () => {
    localStorage.setItem('access_token', 'token-123')
    localStorage.setItem('user_role', 'USER')

    const wrapper = mountHeader()
    await wrapper.findAll('a')[1].trigger('click')

    expect(localStorage.getItem('access_token')).toBeNull()
    expect(localStorage.getItem('user_role')).toBeNull()
    expect(mocks.routerReplace).toHaveBeenCalledWith('/')
  })
})
