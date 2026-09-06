<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories } from '../api/category'
import { getStoreDetail } from '../api/store'
import { listProducts } from '../api/product'

const route = useRoute()
const store = reactive({
  name: '',
  description: '',
  status: '',
})
const shopId = Number(route.params.id)
const categories = ref([])
const products = ref([])

const productsByCategory = computed(() => {
  const grouped = categories.value.map((category) => ({
    ...category,
    products: products.value.filter((item) => item.categoryId === category.id),
  }))

  return grouped
})

async function loadStoreDetail() {
  try {
    const data = await getStoreDetail(shopId)
    store.name = data.name || ''
    store.description = data.description || ''
    store.status = data.status || ''

    const [categoryData, productData] = await Promise.all([
      listCategories(shopId),
      listProducts(shopId, { page: 1, pageSize: 100, includeOffSale: true }),
    ])

    categories.value = categoryData?.items || []
    products.value = productData?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '店铺详情加载失败')
  }
}

onMounted(loadStoreDetail)
</script>

<template>
  <section class="content-stack">
    <el-descriptions :title="store.name" border>
      <el-descriptions-item label="店铺状态">{{ store.status }}</el-descriptions-item>
      <el-descriptions-item label="店铺简介">{{ store.description }}</el-descriptions-item>
      <el-descriptions-item label="分类数量">{{ categories.length }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs>
      <el-tab-pane v-for="category in productsByCategory" :key="category.id" :label="category.name">
        <div class="product-list">
          <div v-for="row in category.products" :key="row.id" class="product-row">
            <div>商品：{{ row.name }}</div>
            <div>价格：{{ row.price }}</div>
            <div>库存：{{ row.stock }}</div>
            <div>状态：{{ row.status }}</div>
            <el-button size="small" @click="$router.push(`/customer/products/${row.id}`)">详情</el-button>
            <el-button size="small" type="primary" :disabled="row.stock <= 0">加入购物车</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
