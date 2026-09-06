<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMerchantOrderDetail } from '../api/order'

const route = useRoute()
const order = ref(null)

onMounted(async () => {
  try {
    order.value = await getMerchantOrderDetail(Number(route.params.id))
  } catch (error) {
    ElMessage.error(error?.message || '订单加载失败')
  }
})
</script>

<template>
  <section v-if="order" class="content-stack">
    <el-descriptions title="订单详情" border>
      <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
      <el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ order.total }}</el-descriptions-item>
    </el-descriptions>
    <div v-for="line in order.lines" :key="line.productId" class="order-item">
      {{ line.productName }} × {{ line.quantity }}，{{ line.unitPrice }} 元
    </div>
  </section>
  <section v-else>加载中...</section>
</template>
