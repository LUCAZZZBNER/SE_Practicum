import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({ routePath: '/customer/stores' }))

vi.mock('vue-router', () => ({
  useRoute: () => ({ get path() { return mocks.routePath } }),
}))

import AppSidebar from '../../components/layout/AppSidebar.vue'

describe('AppSidebar', () => {
  beforeEach(() => {
    mocks.routePath = '/customer/stores'
  })

  function mountSidebar() {
    return mount(AppSidebar, {
      props: {
        menuItems: [
          { path: '/customer/stores', label: '店铺', icon: 'div' },
          { path: '/customer/orders', label: '订单', icon: 'div' },
        ],
      },
      global: {
        stubs: {
          'el-aside': {
            template: '<aside><slot /></aside>',
          },
          'el-menu': {
            props: ['defaultActive'],
            template: '<nav :data-active="defaultActive"><slot /></nav>',
          },
          'el-menu-item': {
            props: ['index'],
            template: '<div class="menu-item"><slot /></div>',
          },
          'el-icon': {
            template: '<i><slot /></i>',
          },
        },
      },
    })
  }

  it('renders menu items and active route', () => {
    const wrapper = mountSidebar()

    expect(wrapper.text()).toContain('店铺')
    expect(wrapper.text()).toContain('订单')
    expect(wrapper.get('nav').attributes('data-active')).toBe('/customer/stores')
  })

  it.each([
    ['/customer/stores/7', '/customer/stores'],
    ['/customer/products/11', '/customer/stores'],
    ['/customer/orders/1001', '/customer/orders'],
  ])('keeps the parent menu active for detail route %s', (routePath, menuPath) => {
    mocks.routePath = routePath

    const wrapper = mountSidebar()

    expect(wrapper.get('nav').attributes('data-active')).toBe(menuPath)
  })
})
