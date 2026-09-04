import { mount } from '@vue/test-utils'
import AuthFormCard from '../../components/common/AuthFormCard.vue'

describe('AuthFormCard', () => {
  it('renders title and default slot content', () => {
    const wrapper = mount(AuthFormCard, {
      props: {
        title: '用户登录',
      },
      slots: {
        default: '<form>登录表单</form>',
      },
      global: {
        stubs: {
          'el-card': {
            template: '<section><slot /></section>',
          },
        },
      },
    })

    expect(wrapper.get('h1').text()).toBe('用户登录')
    expect(wrapper.text()).toContain('登录表单')
  })
})
