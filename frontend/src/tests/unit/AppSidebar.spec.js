import { mount } from '@vue/test-utils'
import { vi } from 'vitest'

vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/customer/stores' }),
}))

import AppSidebar from '../../components/layout/AppSidebar.vue'

describe('AppSidebar', () => {
  it('renders menu items and active route', () => {
    const wrapper = mount(AppSidebar, {
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
            template: '<nav><slot /></nav>',
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

    expect(wrapper.text()).toContain('店铺')
    expect(wrapper.text()).toContain('订单')
  })
})
