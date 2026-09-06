<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStoreDetail, listCategories } from '../api/store'
import { addCartItem } from '../api/cart'
import { listProducts } from '../api/product'

const route = useRoute()
const store = reactive({
  name: '',
  description: '',
  status: '',
})
const categories = ref([])
const products = ref([])

async function loadStoreDetail() {
  try {
    const data = await getStoreDetail(Number(route.params.id))
    store.name = data.name || ''
    store.description = data.description || ''
    store.status = data.status || ''
    const [categoryData, productData] = await Promise.all([
      listCategories(Number(route.params.id)),
      listProducts(Number(route.params.id), { page: 1, pageSize: 100 }),
    ])
    categories.value = categoryData || []
    products.value = productData?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '店铺详情加载失败')
  }
}

async function addProduct(product) {
  try {
    await addCartItem({ productId: product.id, quantity: 1 })
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.message || '加入购物车失败')
  }
}

onMounted(loadStoreDetail)
</script>

<template>
  <section class="content-stack">
    <el-descriptions :title="store.name" border>
      <el-descriptions-item label="店铺状态">{{ store.status }}</el-descriptions-item>
      <el-descriptions-item label="店铺简介">{{ store.description }}</el-descriptions-item>
      <el-descriptions-item label="分类数量">3</el-descriptions-item>
    </el-descriptions>

    <el-tabs>
      <el-tab-pane v-for="category in categories" :key="category.id" :label="category.name">
        <el-table :data="products.filter((item) => item.categoryId === category.id)" border>
          <el-table-column prop="name" label="商品" />
          <el-table-column prop="price" label="价格" width="100" />
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/customer/products/${row.id}`)">详情</el-button>
              <el-button size="small" type="primary" :disabled="row.stock <= 0 || row.status !== 'ON_SALE'" @click="addProduct(row)">加入购物车</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
