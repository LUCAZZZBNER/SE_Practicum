<script setup>
import { onMounted, ref } from 'vue'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import { createProduct, listProducts, updateProduct } from '../api/product'
import { listCategories } from '../api/store'
import { useMerchantShopStore } from '../stores/merchantShop'

const products = ref([])
const categories = ref([])
const shopStore = useMerchantShopStore()

async function loadProducts() {
    if (!shopStore.selectedShopId) return
    const categoryData = await listCategories(shopStore.selectedShopId)
    categories.value = categoryData || []
    const data = await listProducts(shopStore.selectedShopId, { page: 1, pageSize: 100, includeOffSale: true })
  products.value = data?.items || []
}

async function offShelf(product) {
  await updateProduct(product.id, { status: 'OFF_SALE', version: product.version })
  await loadProducts()
}

async function addProduct() {
  if (!shopStore.selectedShopId || categories.value.length === 0) return
  await createProduct({
    shopId: shopStore.selectedShopId,
    categoryId: categories.value[0].id,
    name: '新商品',
    price: 0.01,
    stock: 0,
  })
  await loadProducts()
}

onMounted(async () => {
  await shopStore.loadShops()
  await loadProducts()
})
</script>

<template>
  <section class="content-stack">
    <div class="action-bar">
      <el-button type="primary" @click="addProduct">新增商品</el-button>
      <el-button>新增分类</el-button>
    </div>

    <div v-if="products.length === 0" class="empty-list">暂无商品</div>

    <div v-else class="product-list">
      <div v-for="product in products" :key="product.id" class="product-row">
        <div>商品：{{ product.name }}</div>
        <div>分类：{{ categories.find((category) => category.id === product.categoryId)?.name || product.categoryId }}</div>
        <div>价格：{{ product.price }}</div>
        <div>库存：{{ product.stock }}</div>
        <div>状态：{{ product.status }}</div>
        <el-button size="small">编辑</el-button>
        <ConfirmAction title="确认下架该商品？">
          <el-button size="small" type="warning" @click="offShelf(product)">下架</el-button>
        </ConfirmAction>
      </div>
    </div>
  </section>
</template>
