<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addCartItem } from '../api/cart'
import { listCategories } from '../api/category'
import { getProductDetail } from '../api/product'
import { getStoreDetail } from '../api/store'

const route = useRoute()
const loading = ref(false)
const quantity = ref(1)
const selectedSkuId = ref(null)
const product = reactive({
  name: '',
  description: '',
  shopName: '',
  categoryId: null,
  categoryName: '',
  shopId: null,
  image: null,
  minPrice: 0,
  inStock: false,
  status: '',
  skus: [],
})

const selectedSku = computed(() => product.skus.find((sku) => sku.id === selectedSkuId.value) || null)
const canPurchase = computed(() => (
  product.status === 'ON_SALE'
  && selectedSku.value?.status === 'ON_SALE'
  && selectedSku.value.stock > 0
))
const buttonText = computed(() => {
  if (!selectedSku.value) return '请选择规格'
  return canPurchase.value ? '加入购物车' : '暂不可购买'
})

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function isSkuAvailable(sku) {
  return product.status === 'ON_SALE' && sku.status === 'ON_SALE' && sku.stock > 0
}

function selectSku(sku) {
  if (!isSkuAvailable(sku)) return
  selectedSkuId.value = sku.id
  quantity.value = 1
}

async function loadProductDetail() {
  loading.value = true
  try {
    const data = await getProductDetail(Number(route.params.id))
    product.name = data.name || ''
    product.description = data.description || ''
    product.shopName = data.shopName || ''
    product.categoryId = data.categoryId ?? null
    product.shopId = data.shopId ?? null
    product.image = data.image || null
    product.minPrice = data.minPrice ?? 0
    product.inStock = Boolean(data.inStock)
    product.status = data.status || ''
    product.skus = data.skus || []
    selectedSkuId.value = null
    quantity.value = 1

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
  if (!canPurchase.value) return

  try {
    await addCartItem({
      skuId: selectedSku.value.id,
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
      <div class="product-detail">
        <img
          v-if="product.image?.url"
          data-testid="product-image"
          :src="product.image.url"
          :alt="product.name"
          class="product-image"
        >
        <div v-else data-testid="product-image-placeholder" class="product-image product-placeholder">
          暂无图片
        </div>

        <div class="product-info">
          <h1>{{ product.name }}</h1>
          <p class="description">{{ product.description || '暂无描述' }}</p>
          <dl class="metadata">
            <div><dt>所属店铺</dt><dd>{{ product.shopName }}</dd></div>
            <div><dt>分类</dt><dd>{{ product.categoryName }}</dd></div>
          </dl>

          <div class="sku-section">
            <h2>选择规格</h2>
            <div class="sku-options">
              <el-button
                v-for="sku in product.skus"
                :key="sku.id"
                :data-testid="`sku-option-${sku.id}`"
                :disabled="!isSkuAvailable(sku)"
                :type="selectedSkuId === sku.id ? 'primary' : 'default'"
                :plain="selectedSkuId !== sku.id"
                class="sku-option"
                @click="selectSku(sku)"
              >
                <span>{{ sku.name }}</span>
                <strong>{{ formatPrice(sku.price) }}</strong>
                <small>{{ isSkuAvailable(sku) ? `库存 ${sku.stock}` : '已售罄' }}</small>
              </el-button>
            </div>
          </div>

          <div class="selection-summary">
            <strong data-testid="selected-price">
              {{ formatPrice(selectedSku?.price ?? product.minPrice) }}
            </strong>
            <span data-testid="selected-stock">
              {{ selectedSku ? `库存 ${selectedSku.stock}` : '选择规格后查看库存' }}
            </span>
          </div>

          <div class="action-bar">
            <el-input-number
              v-model="quantity"
              data-testid="quantity-input"
              :min="1"
              :max="selectedSku?.stock || 1"
              :disabled="!canPurchase"
            />
            <el-button
              data-testid="add-to-cart"
              type="primary"
              :disabled="!canPurchase"
              @click="addToCart"
            >
              <el-icon><ShoppingCart /></el-icon>
              {{ buttonText }}
            </el-button>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.back-button { justify-self: start; padding-left: 0; }
.product-detail { display: grid; grid-template-columns: minmax(240px, 360px) minmax(0, 1fr); gap: 28px; }
.product-image { width: 100%; aspect-ratio: 1; object-fit: cover; border: 1px solid #e1e6e3; border-radius: 8px; }
.product-placeholder { display: grid; place-items: center; color: #8a948f; background: #f6f7f6; }
.product-info { min-width: 0; }
.product-info h1 { margin: 0; color: #202925; font-size: 26px; }
.description { margin: 10px 0 18px; color: #6c7772; }
.metadata { display: flex; gap: 28px; margin: 0 0 22px; }
.metadata div { display: flex; gap: 8px; }
.metadata dt { color: #8a948f; }
.metadata dd { margin: 0; color: #3f4944; }
.sku-section h2 { margin: 0 0 10px; font-size: 16px; }
.sku-options { display: flex; flex-wrap: wrap; gap: 10px; }
.sku-option { min-width: 128px; height: auto; padding: 10px 14px; }
.sku-option :deep(span) { display: grid; gap: 3px; }
.sku-option small { font-weight: 400; }
.selection-summary { display: flex; align-items: baseline; gap: 14px; margin-top: 22px; }
.selection-summary strong { color: #c8473d; font-size: 24px; }
.selection-summary span { color: #6c7772; font-size: 14px; }
.action-bar { display: flex; align-items: center; gap: 12px; margin-top: 14px; }

@media (max-width: 720px) {
  .product-detail { grid-template-columns: 1fr; }
  .product-image { max-width: 360px; }
  .metadata, .action-bar { align-items: stretch; flex-direction: column; }
}
</style>
