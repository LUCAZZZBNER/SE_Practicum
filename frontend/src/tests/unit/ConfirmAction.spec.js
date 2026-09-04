import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import ConfirmAction from '../../components/common/ConfirmAction.vue'

describe('ConfirmAction', () => {
  it('renders default trigger button', () => {
    const PopconfirmStub = defineComponent({
      name: 'ElPopconfirm',
      props: ['title'],
      template: '<div><slot name="reference" /></div>',
    })

    const wrapper = mount(ConfirmAction, {
      global: {
        stubs: {
          'el-popconfirm': PopconfirmStub,
          'el-button': {
            template: '<button><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('确认')
  })

  it('emits confirm when popconfirm confirms', async () => {
    const PopconfirmStub = defineComponent({
      name: 'ElPopconfirm',
      emits: ['confirm'],
      template: '<div />',
    })

    const wrapper = mount(ConfirmAction, {
      global: {
        stubs: {
          'el-popconfirm': PopconfirmStub,
          'el-button': true,
        },
      },
    })

    await wrapper.findComponent({ name: 'ElPopconfirm' }).vm.$emit('confirm')

    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })
})
