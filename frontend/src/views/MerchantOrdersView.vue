<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMerchantOrders } from '../api/order'
import { useMerchantShopStore } from '../stores/merchantShop'

const router = useRouter()
const shopStore = useMerchantShopStore()
const orders = ref([])

async function loadOrders() {
  if (!shopStore.selectedShopId) return
  try {
    const data = await listMerchantOrders({ shopId: shopStore.selectedShopId, page: 1, pageSize: 100 })
    orders.value = data?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '订单加载失败')
  }
}

onMounted(async () => {
  await shopStore.loadShops()
  await loadOrders()
})
</script>

<template>
  <section v-if="orders.length === 0" class="content-stack">暂无订单</section>
  <section v-else class="content-stack">
    <div v-for="order in orders" :key="order.id" class="order-row">
      <div>订单号：{{ order.orderNumber }}</div>
      <div>店铺：{{ order.shopName }}</div>
      <div>金额：{{ order.total }}</div>
      <div>状态：{{ order.status }}</div>
      <el-button size="small" @click="router.push(`/merchant/orders/${order.id}`)">查看</el-button>
    </div>
  </section>
</template>
