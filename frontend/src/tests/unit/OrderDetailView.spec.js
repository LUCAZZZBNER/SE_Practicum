import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import OrderDetailView from '../../views/OrderDetailView.vue'

function mountView() {
  return mount(OrderDetailView, {
    global: {
      stubs: {
        'el-steps': {
          props: ['active', 'finishStatus'],
          template: '<div class="steps"><slot /></div>',
        },
        'el-step': {
          props: ['title'],
          template: '<div class="step">{{ title }}</div>',
        },
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-table': {
          template: '<div><slot /></div>',
        },
        'el-table-column': {
          template: '<div />',
        },
      },
    },
  })
}

describe('OrderDetailView', () => {
  it('renders order status, summary and item details', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('已创建')
    expect(wrapper.text()).toContain('待支付')
    expect(wrapper.text()).toContain('订单信息')
    expect(wrapper.text()).toContain('10001')
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('43.60 元')
    expect(wrapper.text()).toContain('招牌牛肉饭')
    expect(wrapper.text()).toContain('冰柠檬茶')
  })
})
