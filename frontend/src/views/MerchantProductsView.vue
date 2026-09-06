<script setup>
import { onMounted, ref } from 'vue'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import { getMerchantProfile } from '../api/merchant'
import { listStores } from '../api/store'
import { createProduct, listProducts, updateProduct } from '../api/product'

const products = ref([])
const shopId = ref(null)

async function loadProducts() {
  const merchant = await getMerchantProfile()
  const shops = await listStores({ mine: true, page: 1, pageSize: 100 })
  shopId.value = shops?.items?.[0]?.id || merchant?.shopId || null

  if (!shopId.value) {
    products.value = []
    return
  }

  const data = await listProducts(shopId.value, {
    page: 1,
    pageSize: 100,
    includeOffSale: true,
  })
  products.value = data?.items || []
}

async function offShelf(product) {
  await updateProduct(product.id, { status: 'OFF_SALE', version: product.version })
}

async function addProduct() {
  await createProduct({
    shopId: shopId.value,
    categoryId: null,
    name: '',
    price: 0,
    stock: 0,
  })
}

onMounted(loadProducts)
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
        <div>分类：{{ product.categoryId }}</div>
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
