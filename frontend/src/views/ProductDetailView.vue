<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '../api/product'

const route = useRoute()
const loading = ref(false)
const product = reactive({
  name: '',
  shopName: '',
  categoryName: '',
  price: 0,
  stock: 0,
  status: '',
})

const unavailable = computed(() => product.stock <= 0 || product.status === 'OFF_SALE')

async function loadProductDetail() {
  loading.value = true
  try {
    const data = await getProductDetail(Number(route.params.id))
    product.name = data.name || ''
    product.shopName = data.shopName || ''
    product.categoryName = data.categoryName || ''
    product.price = data.price ?? 0
    product.stock = data.stock ?? 0
    product.status = data.status || ''
  } catch (error) {
    ElMessage.error(error?.message || '商品详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProductDetail)
</script>

<template>
  <section class="content-stack">
    <div v-if="loading">加载中...</div>
    <template v-else>
      <el-descriptions :title="product.name" border>
        <el-descriptions-item label="所属店铺">{{ product.shopName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ product.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ product.price }} 元</el-descriptions-item>
        <el-descriptions-item label="库存">{{ product.stock }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ product.status }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-bar">
        <el-input-number :model-value="1" :min="1" :max="product.stock" />
        <el-button type="primary" :disabled="unavailable">加入购物车</el-button>
      </div>
    </template>
  </section>
</template>
