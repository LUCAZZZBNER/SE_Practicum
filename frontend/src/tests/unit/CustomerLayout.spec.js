import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CustomerLayout from '../../layouts/CustomerLayout.vue'

function mountLayout() {
  return mount(CustomerLayout, {
    global: {
      mocks: {
        $route: {
          path: '/customer/stores',
          meta: { title: '店铺浏览' },
        },
      },
      stubs: {
        AppHeader: {
          props: ['title', 'profilePath'],
          template: '<header :data-title="title" :data-profile-path="profilePath" />',
        },
        AppSidebar: {
          props: ['menuItems'],
          template: '<aside data-testid="desktop-navigation">{{ menuItems.length }}</aside>',
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

describe('CustomerLayout', () => {
  it('uses the customer brand header without repeated helper copy', () => {
    const wrapper = mountLayout()

    expect(wrapper.get('header').attributes('data-title')).toBe('轻量级外卖')
    expect(wrapper.get('header').attributes('data-profile-path')).toBe('/customer/profile')
    expect(wrapper.text()).toContain('店铺浏览')
    expect(wrapper.text()).not.toContain('浏览店铺、购物车、订单')
  })

  it('provides desktop and mobile navigation for all customer sections', () => {
    const wrapper = mountLayout()

    expect(wrapper.get('[data-testid="desktop-navigation"]')).toBeTruthy()
    const mobileNavigation = wrapper.get('[data-testid="mobile-navigation"]')
    const destinations = mobileNavigation.findAll('a').map((link) => link.attributes('data-to'))

    expect(destinations).toEqual([
      '/customer/stores',
      '/customer/cart',
      '/customer/orders',
      '/customer/addresses',
      '/customer/profile',
    ])
    expect(mobileNavigation.text()).toContain('店铺')
    expect(mobileNavigation.text()).toContain('购物车')
    expect(mobileNavigation.text()).toContain('订单')
    expect(mobileNavigation.text()).toContain('地址')
    expect(mobileNavigation.text()).toContain('我的')
  })
})
