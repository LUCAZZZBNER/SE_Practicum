import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const mocks = vi.hoisted(() => ({
  listStores: vi.fn(),
  createStore: vi.fn(),
}))

vi.mock('../../api/store', () => ({
  listStores: mocks.listStores,
  createStore: mocks.createStore,
}))

import { useMerchantShopStore } from '../../stores/merchantShop'

beforeEach(() => {
  setActivePinia(createPinia())
  localStorage.clear()
  mocks.listStores.mockReset()
  mocks.createStore.mockReset()
})

describe('merchant shop store', () => {
  it('restores a selected owned shop and rejects stale persisted IDs', async () => {
    localStorage.setItem('merchant_shop_id', '9')
    mocks.listStores.mockResolvedValue({ items: [{ id: 7 }, { id: 9 }] })
    const store = useMerchantShopStore()

    await store.loadShops()
    expect(store.selectedShopId).toBe(9)

    localStorage.setItem('merchant_shop_id', '99')
    const freshStore = useMerchantShopStore(createPinia())
    await freshStore.loadShops()
    expect(freshStore.selectedShopId).toBe(7)
    expect(localStorage.getItem('merchant_shop_id')).toBe('7')
  })

  it('creates and selects a new shop', async () => {
    mocks.listStores.mockResolvedValue({ items: [] })
    mocks.createStore.mockResolvedValue({ id: 12, name: 'New shop' })
    const store = useMerchantShopStore()
    await store.loadShops()

    await store.createShop({ name: 'New shop' })

    expect(store.selectedShopId).toBe(12)
    expect(store.selectedShop.name).toBe('New shop')
    expect(localStorage.getItem('merchant_shop_id')).toBe('12')
  })
})
