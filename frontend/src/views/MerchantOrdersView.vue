<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/common/EmptyState.vue'
import { listMerchantOrders } from '../api/order'

const orders = ref([])

async function loadOrders() {
  try {
    const data = await listMerchantOrders({ page: 1, pageSize: 10 })
    orders.value = data?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '订单加载失败')
  }
}

onMounted(loadOrders)
</script>

<template>
  <EmptyState v-if="orders.length === 0" description="暂无订单" />

  <div v-else class="order-list">
    <div v-for="row in orders" :key="row.id" class="order-row">
      <div>订单号：{{ row.orderNumber }}</div>
      <div>店铺：{{ row.shopName }}</div>
      <div>金额：{{ row.total }}</div>
      <div>状态：{{ row.status }}</div>
      <div>创建时间：{{ row.createdAt }}</div>
      <el-button size="small" type="primary" @click="$router.push(`/merchant/orders/${row.id}`)">查看</el-button>
    </div>
  </div>
</template>
