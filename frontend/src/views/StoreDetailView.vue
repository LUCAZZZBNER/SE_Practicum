<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listCategories } from '../api/category'
import { listProducts } from '../api/product'
import { getStoreDetail } from '../api/store'

const route = useRoute()
const shopId = Number(route.params.id)
const store = reactive({ name: '', description: '', status: '' })
const categories = ref([])
const products = ref([])

const storeStatusMap = {
  OPEN: '营业中',
  TEMPORARILY_CLOSED: '暂时休息',
  CLOSED: '已关闭',
}

const productsByCategory = computed(() => categories.value.map((category) => ({
  ...category,
  products: products.value.filter((item) => item.categoryId === category.id),
})))

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

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function formatStoreStatus(status) {
  return storeStatusMap[status] || '状态未知'
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
          <article
            v-for="row in category.products"
            :key="row.id"
            :data-testid="`product-row-${row.id}`"
            class="product-row"
          >
            <img
              v-if="row.image?.url"
              :data-testid="`product-image-${row.id}`"
              :src="row.image.url"
              :alt="row.name"
              class="product-image"
            >
            <div
              v-else
              :data-testid="`product-image-placeholder-${row.id}`"
              class="product-image product-placeholder"
            >
              暂无图片
            </div>

            <div class="product-copy">
              <h3>{{ row.name }}</h3>
              <p>{{ row.description || '暂无描述' }}</p>
              <div class="product-meta">
                <strong>{{ formatPrice(row.minPrice) }} 起</strong>
                <span :class="{ soldOut: !row.inStock }">{{ row.inStock ? '有货' : '已售罄' }}</span>
              </div>
            </div>

            <el-button
              :data-testid="`select-product-${row.id}`"
              type="primary"
              plain
              @click="$router.push(`/customer/products/${row.id}`)"
            >
              选择规格
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.back-button { justify-self: start; padding-left: 0; }
.product-list { display: grid; gap: 10px; }
.product-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #edf0ee;
}
.product-image {
  width: 96px;
  aspect-ratio: 1;
  object-fit: cover;
  border: 1px solid #e1e6e3;
  border-radius: 6px;
}
.product-placeholder {
  display: grid;
  place-items: center;
  color: #8a948f;
  background: #f6f7f6;
  font-size: 12px;
}
.product-copy { min-width: 0; }
.product-copy h3 { margin: 0; color: #202925; font-size: 17px; }
.product-copy p { margin: 7px 0; color: #6c7772; font-size: 14px; }
.product-meta { display: flex; align-items: center; gap: 14px; }
.product-meta strong { color: #c8473d; }
.product-meta span { color: #318258; font-size: 13px; }
.product-meta .soldOut { color: #8a948f; }

@media (max-width: 640px) {
  .product-row { grid-template-columns: 76px minmax(0, 1fr); }
  .product-image { width: 76px; }
  .product-row > button { grid-column: 1 / -1; justify-self: stretch; }
}
</style>
