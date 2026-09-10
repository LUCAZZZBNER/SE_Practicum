import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerStoresView from '../../views/CustomerStoresView.vue'

const mocks = vi.hoisted(() => ({
  listStores: vi.fn(),
  routerPush: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.messageError,
  },
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
        EmptyState: {
          props: ['description'],
          template: '<div data-testid="empty-state">{{ description }}</div>',
        },
        'el-input': {
          props: ['modelValue', 'placeholder'],
          emits: ['update:modelValue', 'keyup'],
          template: '<input :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup\', $event)" />',
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
        'el-icon': {
          template: '<i><slot /></i>',
        },
      },
    },
  })
}

beforeEach(() => {
  mocks.listStores.mockReset()
  mocks.routerPush.mockReset()
  mocks.messageError.mockReset()
})

describe('CustomerStoresView', () => {
  it('loads stores with the exact first-page query and renders customer-facing shop data', async () => {
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

    expect(mocks.listStores).toHaveBeenCalledWith({ page: 1, pageSize: 10 })
    expect(wrapper.get('[data-testid="store-visual"]')).toBeTruthy()
    expect(wrapper.get('.store-name').text()).toBe('示例快餐店')
    expect(wrapper.text()).toContain('校园简餐')
    expect(wrapper.text()).toContain('营业中')
    expect(wrapper.text()).not.toContain('OPEN')
    expect(wrapper.get('[data-testid="store-action"]').text()).toBe('查看菜单')
  })

  it('searches stores by a trimmed keyword', async () => {
    mocks.listStores.mockResolvedValue({ items: [], total: 0 })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('input[placeholder="搜索店铺"]').setValue('  快餐  ')
    await wrapper.get('input[placeholder="搜索店铺"]').trigger('keyup.enter')
    await flushPromises()

    expect(mocks.listStores).toHaveBeenLastCalledWith({
      page: 1,
      pageSize: 10,
      keyword: '快餐',
    })
  })

  it('shows the shared empty state when the shop list is empty', async () => {
    mocks.listStores.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 10,
      total: 0,
      totalPages: 0,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="empty-state"]').text()).toBe('暂无可浏览店铺')
  })

  it.each([
    ['TEMPORARILY_CLOSED', '暂时休息'],
    ['CLOSED', '已关闭'],
  ])('localizes %s and disables entering an unavailable shop', async (status, statusText) => {
    mocks.listStores.mockResolvedValue({
      items: [
        { id: 2, name: '休息店铺', description: null, status },
      ],
      page: 1,
      pageSize: 10,
      total: 1,
      totalPages: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain(statusText)
    expect(wrapper.text()).not.toContain(status)
    expect(wrapper.get('[data-testid="store-card"]').classes()).toContain('is-unavailable')
    expect(wrapper.get('[data-testid="store-action"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="store-action"]').text()).toBe('暂不可浏览')
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })

  it('enters the menu of an open shop', async () => {
    mocks.listStores.mockResolvedValue({
      items: [{ id: 7, name: '示例快餐店', description: '校园简餐', status: 'OPEN' }],
      total: 1,
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="store-action"]').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith('/customer/stores/7')
  })
})
