import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerStoresView from '../../views/CustomerStoresView.vue'

const mocks = vi.hoisted(() => ({
  listStores: vi.fn(),
  routerPush: vi.fn(),
}))

vi.mock('../../api/store', () => ({
  listStores: mocks.listStores,
}))

function mountView() {
  return mount(CustomerStoresView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
        'el-input': {
          template: '<input />',
        },
        'el-card': {
          template: '<article><slot /></article>',
        },
        'el-tag': {
          props: ['type'],
          template: '<span class="tag"><slot /></span>',
        },
        'el-button': {
          props: ['disabled'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listStores.mockReset()
  mocks.routerPush.mockReset()
})

describe('CustomerStoresView', () => {
  it('loads stores with the API query and renders shop data', async () => {
    mocks.listStores.mockResolvedValue({
      items: [
        {
          id: 1,
          name: '示例快餐店',
          description: '校园简餐',
          status: 'OPEN',
        },
      ],
      page: 1,
      pageSize: 10,
      total: 1,
      totalPages: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.listStores).toHaveBeenCalledWith(expect.any(Object))
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('OPEN')
  })

  it('shows empty state when the shop list is empty', async () => {
    mocks.listStores.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 10,
      total: 0,
      totalPages: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无店铺')
  })

  it('disables entering a shop that is not open', async () => {
    mocks.listStores.mockResolvedValue({
      items: [
        { id: 2, name: '休息店铺', description: null, status: 'CLOSED' },
      ],
      page: 1,
      pageSize: 10,
      total: 1,
      totalPages: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('button').element.disabled).toBe(true)
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })
})
