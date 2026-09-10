<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/common/EmptyState.vue'
import { listMerchantOrders } from '../api/order'
import { formatOrderPrice, formatOrderTime, getOrderStatus } from '../utils/orderPresentation'

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
    <div v-for="row in orders" :key="row.id" :data-testid="`merchant-order-card-${row.id}`" class="order-row">
      <div class="order-copy">
        <div class="order-heading">
          <span class="shop-name">{{ row.shopName }}</span>
          <el-tag :type="getOrderStatus(row.status).type" effect="light">
            {{ getOrderStatus(row.status).text }}
          </el-tag>
        </div>
        <div class="order-number">订单号：{{ row.orderNumber }}</div>
        <div class="order-time"><el-icon><Clock /></el-icon>{{ formatOrderTime(row.createdAt) }}</div>
      </div>
      <div class="order-side">
        <strong>{{ formatOrderPrice(row.total) }}</strong>
        <el-button
          :data-testid="`view-merchant-order-${row.id}`"
          text
          type="primary"
          @click="$router.push(`/merchant/orders/${row.id}`)"
        >
          查看详情
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-list { display: grid; gap: 12px; }
.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.order-copy { display: grid; gap: 7px; min-width: 0; }
.order-heading { display: flex; align-items: center; gap: 10px; }
.shop-name { overflow: hidden; color: #202925; font-size: 17px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.order-number, .order-time { color: #6c7772; font-size: 14px; }
.order-time { display: flex; align-items: center; gap: 5px; }
.order-side { display: grid; justify-items: end; gap: 8px; }
.order-side strong { color: #c8473d; font-size: 19px; }

@media (max-width: 560px) {
  .order-row { align-items: stretch; flex-direction: column; }
  .order-side { grid-template-columns: 1fr auto; align-items: center; justify-items: start; }
}
</style>
