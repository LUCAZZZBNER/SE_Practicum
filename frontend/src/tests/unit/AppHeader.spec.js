import { mount } from '@vue/test-utils'
import AppHeader from '../../components/layout/AppHeader.vue'

describe('AppHeader', () => {
  it('renders title and status text', () => {
    const wrapper = mount(AppHeader, {
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
            template: '<a><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('外卖平台')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('退出')
  })
})
