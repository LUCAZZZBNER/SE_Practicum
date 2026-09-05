import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StoreDetailView from '../../views/StoreDetailView.vue'

const mocks = vi.hoisted(() => ({
  getStoreDetail: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
}))

vi.mock('../../api/store', () => ({
  getStoreDetail: mocks.getStoreDetail,
}))

function mountView() {
  return mount(StoreDetailView, {
    global: {
      mocks: {
        $router: {
          push: mocks.routerPush,
        },
      },
      stubs: {
        'el-descriptions': {
          props: ['title'],
          template: '<section><h1>{{ title }}</h1><slot /></section>',
        },
        'el-descriptions-item': {
          props: ['label'],
          template: '<div>{{ label }}<slot /></div>',
        },
        'el-tabs': {
          template: '<div><slot /></div>',
        },
        'el-tab-pane': {
          props: ['label'],
          template: '<div><h2>{{ label }}</h2><slot /></div>',
        },
        'el-table': {
          template: '<div><slot /></div>',
        },
        'el-table-column': {
          template: '<div />',
        },
        'el-button': {
          props: ['disabled'],
          template: '<button :disabled="disabled"><slot /></button>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.getStoreDetail.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageError.mockReset()
})

describe('StoreDetailView', () => {
  it('loads store detail by route store id and renders shop status', async () => {
    mocks.getStoreDetail.mockResolvedValue({
      id: 7,
      name: '示例快餐店',
      description: '校园简餐',
      status: 'OPEN',
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mocks.getStoreDetail).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('示例快餐店')
    expect(wrapper.text()).toContain('校园简餐')
    expect(wrapper.text()).toContain('OPEN')
  })

  it('shows an error when loading store detail fails', async () => {
    mocks.getStoreDetail.mockRejectedValue(new Error('店铺不存在'))

    mountView()
    await flushPromises()

    expect(mocks.messageError).toHaveBeenCalledWith('店铺不存在')
  })
})
