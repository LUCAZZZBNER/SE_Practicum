<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMerchantOrderDetail } from '../api/order'
import { formatOrderPrice, formatOrderTime, getOrderStatus } from '../utils/orderPresentation'

const route = useRoute()
const loading = ref(false)
const order = reactive({
  orderNumber: '',
  shopName: '',
  status: '',
  total: 0,
  createdAt: '',
  lines: [],
})

async function loadOrder() {
  loading.value = true
  try {
    const data = await getMerchantOrderDetail(Number(route.params.id))
    order.orderNumber = data?.orderNumber || ''
    order.shopName = data?.shopName || ''
    order.status = data?.status || ''
    order.total = data?.total ?? 0
    order.createdAt = data?.createdAt || ''
    order.lines = data?.lines || []
  } catch (error) {
    ElMessage.error(error?.message || '订单详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadOrder)
</script>

<template>
  <section class="order-detail">
    <el-button data-testid="back-to-merchant-orders" class="back-button" text @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回订单列表
    </el-button>

    <div v-if="loading">加载中...</div>
    <template v-else>
      <el-descriptions title="订单信息" border>
        <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getOrderStatus(order.status).type" effect="light">
            {{ getOrderStatus(order.status).text }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatOrderPrice(order.total) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatOrderTime(order.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <div class="order-items">
        <div v-for="item in order.lines" :key="item.productId" class="order-item">
          <strong>{{ item.productName }}</strong>
          <span>数量：{{ item.quantity }}</span>
          <span>单价：{{ formatOrderPrice(item.unitPrice) }}</span>
          <span>小计：{{ formatOrderPrice(item.subtotal) }}</span>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.order-detail { display: grid; gap: 18px; }
.back-button { justify-self: start; padding-left: 0; }
.order-items { display: grid; gap: 10px; }
.order-item {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) repeat(3, auto);
  align-items: center;
  gap: 18px;
  padding: 14px 16px;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
  color: #6c7772;
}
.order-item strong { color: #202925; }

@media (max-width: 640px) {
  .order-item { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .order-item strong { grid-column: 1 / -1; }
}
</style>
