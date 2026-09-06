import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { createStore, listStores } from '../api/store'
import { getSelectedShopId, markSelectedShop } from '../auth/session'

export const useMerchantShopStore = defineStore('merchantShop', () => {
  const shops = ref([])
  const selectedShopId = ref(getSelectedShopId())
  const loading = ref(false)

  const selectedShop = computed(() => shops.value.find((shop) => shop.id === selectedShopId.value) || null)

  function selectShop(shopId) {
    const shop = shops.value.find((item) => item.id === shopId)
    selectedShopId.value = shop ? shop.id : (shops.value[0]?.id || null)
    markSelectedShop(selectedShopId.value)
  }

  async function loadShops() {
    loading.value = true
    try {
      const data = await listStores({ mine: true, page: 1, pageSize: 100 })
      shops.value = data?.items || []
      selectShop(selectedShopId.value)
      return shops.value
    } finally {
      loading.value = false
    }
  }

  async function addShop(data) {
    const shop = await createStore(data)
    shops.value = [...shops.value, shop]
    selectShop(shop.id)
    return shop
  }

  return { shops, selectedShopId, selectedShop, loading, selectShop, loadShops, createShop: addShop }
})
