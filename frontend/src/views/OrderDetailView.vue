<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '../api/order'

const route = useRoute()
const loading = ref(false)
const order = reactive({
  id: '',
  orderNumber: '',
  shopName: '',
  status: '',
  total: 0,
  lines: [],
})

async function loadOrder() {
  loading.value = true
  try {
    const data = await getOrderDetail(Number(route.params.id))
    order.id = data?.id || ''
    order.orderNumber = data?.orderNumber || ''
    order.shopName = data?.shopName || ''
    order.status = data?.status || ''
    order.total = data?.total ?? 0
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
  <section class="content-stack">
    <div v-if="loading">加载中...</div>
    <template v-else>
      <el-steps :active="1" finish-status="success">
      <el-step title="已创建" />
      <el-step title="待支付" />
      <el-step title="已完成" />
      </el-steps>

      <el-descriptions title="订单信息" border>
        <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ order.total.toFixed(2) }} 元</el-descriptions-item>
      </el-descriptions>

      <div class="order-items">
        <div v-for="item in order.lines" :key="item.productId" class="order-item">
          <span>{{ item.productName }}</span>
          <span>数量：{{ item.quantity }}</span>
          <span>成交单价：{{ item.unitPrice }}</span>
        </div>
      </div>
    </template>
  </section>
</template>
