<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cancelOrder, getOrderDetail } from '../api/order'

const route = useRoute()
const order = ref(null)
const loading = ref(true)

async function loadOrder() {
  loading.value = true
  try {
    order.value = await getOrderDetail(Number(route.params.id))
  } catch (error) {
    ElMessage.error(error?.message || '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function cancel() {
  try {
    order.value = await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
  } catch (error) {
    ElMessage.error(error?.message || '取消订单失败')
  }
}

onMounted(loadOrder)
</script>

<template>
  <section v-if="loading" class="content-stack">加载中...</section>
  <section v-else-if="order" class="content-stack">
    <el-steps :active="1" finish-status="success">
      <el-step title="已创建" />
      <el-step title="待支付" />
      <el-step title="已完成" />
    </el-steps>

    <el-descriptions title="订单信息" border>
      <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
      <el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ Number(order.total).toFixed(2) }} 元</el-descriptions-item>
    </el-descriptions>

    <div class="order-items">
      <div v-for="item in order.lines" :key="item.productId" class="order-item">
        <span>{{ item.productName }}</span>
        <span>数量：{{ item.quantity }}</span>
        <span>成交单价：{{ item.unitPrice }}</span>
      </div>
    </div>
    <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" @click="cancel">取消订单</el-button>
  </section>
</template>
