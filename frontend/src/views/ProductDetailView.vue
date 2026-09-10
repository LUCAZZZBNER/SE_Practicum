<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addCartItem } from '../api/cart'
import { listCategories } from '../api/category'
import { getStoreDetail } from '../api/store'
import { getProductDetail } from '../api/product'

const route = useRoute()
const loading = ref(false)
const product = reactive({
  name: '',
  description: '',
  shopName: '',
  categoryId: null,
  categoryName: '',
  shopId: null,
  price: 0,
  stock: 0,
  status: '',
})

const unavailable = computed(() => product.stock <= 0 || product.status === 'OFF_SALE')
const quantity = ref(1)

const statusMap = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
}

const formattedPrice = computed(() => `¥${Number(product.price || 0).toFixed(2)}`)
const statusText = computed(() => statusMap[product.status] || '状态未知')

async function loadProductDetail() {
  loading.value = true
  try {
    const data = await getProductDetail(Number(route.params.id))
    product.name = data.name || ''
    product.description = data.description || ''
    product.shopName = data.shopName || ''
    product.categoryId = data.categoryId ?? null
    product.shopId = data.shopId ?? null
    product.price = data.price ?? 0
    product.stock = data.stock ?? 0
    product.status = data.status || ''

    if (product.shopId) {
      const [categories, shop] = await Promise.all([
        listCategories(product.shopId),
        getStoreDetail(product.shopId),
      ])
      const matched = categories?.find((item) => item.id === product.categoryId)
      product.categoryName = matched?.name || ''
      product.shopName = shop?.name || ''
    }
  } catch (error) {
    ElMessage.error(error?.message || '商品详情加载失败')
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  try {
    await addCartItem({
      productId: Number(route.params.id),
      quantity: quantity.value,
    })
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.message || '加入购物车失败')
  }
}

onMounted(loadProductDetail)
</script>

<template>
  <section class="content-stack">
    <el-button data-testid="back-to-store" class="back-button" text @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回店铺
    </el-button>

    <div v-if="loading">加载中...</div>
    <template v-else>
      <el-descriptions :title="product.name" border>
        <el-descriptions-item label="所属店铺">{{ product.shopName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ product.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="商品描述">{{ product.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ formattedPrice }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ product.stock }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-bar">
        <el-input-number v-model="quantity" :min="1" :max="product.stock" />
        <el-button data-testid="add-to-cart" type="primary" :disabled="unavailable" @click="addToCart">
          <el-icon><ShoppingCart /></el-icon>
          {{ unavailable ? '暂不可购买' : '加入购物车' }}
        </el-button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.back-button { justify-self: start; padding-left: 0; }
</style>
