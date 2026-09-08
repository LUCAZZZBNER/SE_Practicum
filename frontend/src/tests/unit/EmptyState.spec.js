import { mount } from '@vue/test-utils'
import EmptyState from '../../components/common/EmptyState.vue'

describe('EmptyState', () => {
  it('renders default description', () => {
    const wrapper = mount(EmptyState, {
      global: {
        stubs: {
          'el-empty': {
            props: ['description'],
            template: '<div><span class="desc">{{ description }}</span><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.get('.desc').text()).toBe('暂无数据')
  })

  it('renders custom description and slot content', () => {
    const wrapper = mount(EmptyState, {
      props: {
        description: '没有订单',
      },
      slots: {
        default: '<button>去下单</button>',
      },
      global: {
        stubs: {
          'el-empty': {
            props: ['description'],
            template: '<div><span class="desc">{{ description }}</span><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.get('.desc').text()).toBe('没有订单')
    expect(wrapper.text()).toContain('去下单')
  })
})
