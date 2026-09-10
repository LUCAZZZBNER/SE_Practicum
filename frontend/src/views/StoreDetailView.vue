<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addCartItem } from '../api/cart'
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

const storeStatusMap = {
  OPEN: '营业中',
  TEMPORARILY_CLOSED: '暂时休息',
  CLOSED: '已关闭',
}

const productStatusMap = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
}

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
      listProducts(shopId, { page: 1, pageSize: 100 }),
    ])

    categories.value = categoryData || []
    products.value = productData?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '店铺详情加载失败')
  }
}

async function addToCart(product) {
  try {
    await addCartItem({
      productId: product.id,
      quantity: 1,
    })
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.message || '加入购物车失败')
  }
}

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function formatStoreStatus(status) {
  return storeStatusMap[status] || '状态未知'
}

function formatProductStatus(status) {
  return productStatusMap[status] || '状态未知'
}

onMounted(loadStoreDetail)
</script>

<template>
  <section class="content-stack">
    <el-button data-testid="back-to-stores" class="back-button" text @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回店铺列表
    </el-button>

    <el-descriptions :title="store.name" border>
      <el-descriptions-item label="店铺状态">{{ formatStoreStatus(store.status) }}</el-descriptions-item>
      <el-descriptions-item label="店铺简介">{{ store.description }}</el-descriptions-item>
      <el-descriptions-item label="分类数量">{{ categories.length }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs>
      <el-tab-pane v-for="category in productsByCategory" :key="category.id" :label="category.name">
        <div class="product-list">
          <div v-for="row in category.products" :key="row.id" class="product-row">
            <div>商品：{{ row.name }}</div>
            <div v-if="row.description" class="product-description">{{ row.description }}</div>
            <div>价格：{{ formatPrice(row.price) }}</div>
            <div>库存：{{ row.stock }}</div>
            <div>状态：{{ formatProductStatus(row.status) }}</div>
            <el-button size="small" @click="$router.push(`/customer/products/${row.id}`)">详情</el-button>
            <el-button
              :data-testid="`add-product-${row.id}`"
              size="small"
              type="primary"
              :disabled="row.stock <= 0 || row.status === 'OFF_SALE'"
              @click="addToCart(row)"
            >
              <el-icon><ShoppingCart /></el-icon>
              加入购物车
            </el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.back-button { justify-self: start; padding-left: 0; }
.product-description { color: #6c7772; }
</style>
