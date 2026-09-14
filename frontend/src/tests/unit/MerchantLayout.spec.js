import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MerchantLayout from '../../layouts/MerchantLayout.vue'

function mountLayout() {
  return mount(MerchantLayout, {
    global: {
      mocks: {
        $route: {
          path: '/merchant/store',
          meta: { title: '店铺管理' },
        },
      },
      stubs: {
        AppHeader: {
          props: ['title', 'profilePath'],
          template: '<header :data-title="title" :data-profile-path="profilePath" />',
        },
        AppSidebar: {
          props: ['menuItems'],
          template: '<aside data-testid="desktop-navigation"><span v-for="item in menuItems" :key="item.path">{{ item.label }}</span></aside>',
        },
        'el-container': {
          template: '<div><slot /></div>',
        },
        'el-main': {
          template: '<main><slot /></main>',
        },
        'el-icon': {
          template: '<i><slot /></i>',
        },
        'router-link': {
          props: ['to'],
          template: '<a :data-to="to"><slot /></a>',
        },
        'router-view': {
          template: '<div />',
        },
      },
    },
  })
}

describe('MerchantLayout', () => {
  it('uses the merchant workbench brand and concise page heading', () => {
    const wrapper = mountLayout()

    expect(wrapper.get('header').attributes('data-title')).toBe('商家工作台')
    expect(wrapper.get('header').attributes('data-profile-path')).toBe('/merchant/profile')
    expect(wrapper.text()).toContain('店铺管理')
    expect(wrapper.text()).not.toContain('店铺、商品、库存管理')
  })

  it('provides merchant sections in operational order on desktop and mobile', () => {
    const wrapper = mountLayout()

    expect(wrapper.get('[data-testid="desktop-navigation"]').text()).toBe('店铺管理商品管理订单管理个人信息')
    const mobileNavigation = wrapper.get('[data-testid="mobile-navigation"]')
    const destinations = mobileNavigation.findAll('a').map((link) => link.attributes('data-to'))

    expect(destinations).toEqual([
      '/merchant/store',
      '/merchant/products',
      '/merchant/orders',
      '/merchant/profile',
    ])
    expect(mobileNavigation.text()).toContain('店铺')
    expect(mobileNavigation.text()).toContain('商品')
    expect(mobileNavigation.text()).toContain('订单')
    expect(mobileNavigation.text()).toContain('我的')
  })
})
