<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import {
  createCategory,
  listCategories,
  removeCategory,
  updateCategory,
} from '../api/category'
import { uploadProductImage } from '../api/image'
import { getMerchantProfile } from '../api/merchant'
import {
  createProduct,
  createProductSku,
  listProducts,
  updateProduct,
  updateProductSku,
} from '../api/product'
import { listStores } from '../api/store'

const products = ref([])
const categories = ref([])
const shopId = ref(null)
const categoryName = ref('')
const uploadingImage = ref(false)
const productForm = reactive({
  id: null,
  categoryId: null,
  name: '',
  description: '',
  imageId: null,
  imageUrl: '',
  initialSkuName: '',
  initialSkuPrice: 0,
  initialSkuStock: 0,
})
const skuForm = reactive({
  id: null,
  productId: null,
  name: '',
  price: 0,
  stock: 0,
  version: null,
})

const productStatusMap = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
}

const skuStatusMap = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
}

const onSaleCount = computed(() => products.value.filter((product) => product.status === 'ON_SALE').length)

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function formatStatus(status, statusMap = productStatusMap) {
  return statusMap[status] || '状态未知'
}

function resetProductForm() {
  productForm.id = null
  productForm.categoryId = categories.value[0]?.id || null
  productForm.name = ''
  productForm.description = ''
  productForm.imageId = null
  productForm.imageUrl = ''
  productForm.initialSkuName = ''
  productForm.initialSkuPrice = 0
  productForm.initialSkuStock = 0
}

function fillProductForm(product) {
  productForm.id = product.id
  productForm.categoryId = product.categoryId
  productForm.name = product.name
  productForm.description = product.description || ''
  productForm.imageId = product.image?.id || null
  productForm.imageUrl = product.image?.url || ''
}

function resetSkuForm() {
  skuForm.id = null
  skuForm.productId = null
  skuForm.name = ''
  skuForm.price = 0
  skuForm.stock = 0
  skuForm.version = null
}

function startCreateSku(product) {
  resetSkuForm()
  skuForm.productId = product.id
}

function fillSkuForm(sku) {
  skuForm.id = sku.id
  skuForm.productId = sku.productId
  skuForm.name = sku.name
  skuForm.price = sku.price
  skuForm.stock = sku.stock
  skuForm.version = sku.version
}

async function loadProducts() {
  const merchant = await getMerchantProfile()
  const shops = await listStores({ mine: true, page: 1, pageSize: 100 })
  shopId.value = shops?.items?.[0]?.id || merchant?.shopId || null

  if (!shopId.value) {
    products.value = []
    categories.value = []
    return
  }

  const [categoryData, productData] = await Promise.all([
    listCategories(shopId.value),
    listProducts(shopId.value, {
      page: 1,
      pageSize: 100,
      includeOffSale: true,
    }),
  ])
  categories.value = categoryData || []
  products.value = productData?.items || []

  if (!productForm.categoryId) {
    resetProductForm()
  }
}

async function addCategory() {
  if (!shopId.value || !categoryName.value.trim()) {
    ElMessage.error('请填写分类名称')
    return
  }

  await createCategory(shopId.value, {
    name: categoryName.value.trim(),
    sortOrder: categories.value.length + 1,
  })
  categoryName.value = ''
  await loadProducts()
}

async function renameCategory(category) {
  await updateCategory(category.id, {
    name: category.name,
    sortOrder: category.sortOrder,
  })
  await loadProducts()
}

async function deleteCategory(category) {
  await removeCategory(category.id)
  await loadProducts()
}

async function handleImageChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)
  uploadingImage.value = true
  try {
    const image = await uploadProductImage(formData)
    productForm.imageId = image.id
    productForm.imageUrl = image.url
  } catch (error) {
    ElMessage.error(error?.message || '商品图片上传失败')
  } finally {
    uploadingImage.value = false
  }
}

async function toggleProductStatus(product) {
  const status = product.status === 'OFF_SALE' ? 'ON_SALE' : 'OFF_SALE'
  if (status === 'ON_SALE' && !product.image) {
    ElMessage.error('商品上架前必须上传主图')
    return
  }

  await updateProduct(product.id, { status })
  await loadProducts()
}

async function saveProduct() {
  const payload = {
    categoryId: Number(productForm.categoryId),
    name: productForm.name.trim(),
    description: productForm.description.trim(),
    imageId: productForm.imageId,
  }

  if (productForm.id) {
    if (!payload.categoryId || !payload.name) {
      ElMessage.error('请填写有效商品信息')
      return
    }
    await updateProduct(productForm.id, payload)
  } else {
    const initialSku = {
      name: productForm.initialSkuName.trim(),
      price: Number(productForm.initialSkuPrice),
      stock: Number(productForm.initialSkuStock),
    }
    if (!shopId.value || !payload.categoryId || !payload.name || !initialSku.name || initialSku.price <= 0 || initialSku.stock < 0) {
      ElMessage.error('请填写有效商品和规格信息')
      return
    }
    await createProduct({
      shopId: shopId.value,
      ...payload,
      skus: [initialSku],
    })
  }

  resetProductForm()
  await loadProducts()
}

async function saveSku() {
  const payload = {
    name: skuForm.name.trim(),
    price: Number(skuForm.price),
    stock: Number(skuForm.stock),
  }
  if (!skuForm.productId || !payload.name || payload.price <= 0 || payload.stock < 0) {
    ElMessage.error('请填写有效规格信息')
    return
  }

  if (skuForm.id) {
    await updateProductSku(skuForm.id, {
      ...payload,
      version: skuForm.version,
    })
  } else {
    await createProductSku(skuForm.productId, payload)
  }

  resetSkuForm()
  await loadProducts()
}

async function toggleSkuStatus(sku) {
  await updateProductSku(sku.id, {
    status: sku.status === 'OFF_SALE' ? 'ON_SALE' : 'OFF_SALE',
    version: sku.version,
  })
  await loadProducts()
}

onMounted(loadProducts)
</script>

<template>
  <section class="content-stack">
    <section class="product-summary">
      <div><span>商品总数</span><strong>{{ products.length }}</strong></div>
      <div><span>在售商品</span><strong>{{ onSaleCount }}</strong></div>
      <div><span>已下架</span><strong>{{ products.length - onSaleCount }}</strong></div>
    </section>

    <div class="manage-panel">
      <h2>分类管理</h2>
      <div class="inline-form">
        <el-input v-model="categoryName" placeholder="分类名称" />
        <el-button type="primary" @click="addCategory">新增分类</el-button>
      </div>
      <div v-for="category in categories" :key="category.id" class="category-row">
        <el-input v-model="category.name" />
        <el-button size="small" @click="renameCategory(category)">保存分类</el-button>
        <el-button size="small" type="danger" @click="deleteCategory(category)">删除分类</el-button>
      </div>
    </div>

    <div class="manage-panel">
      <div class="section-heading">
        <div>
          <h2>{{ productForm.id ? '编辑商品' : '新增商品' }}</h2>
          <p>{{ productForm.id ? '维护商品基础信息与主图' : '设置商品信息和首个销售规格' }}</p>
        </div>
      </div>
      <div class="product-editor">
        <label class="image-uploader">
          <span>商品主图</span>
          <input
            data-testid="product-image-input"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            :disabled="uploadingImage"
            @change="handleImageChange"
          >
          <img
            v-if="productForm.imageUrl"
            data-testid="product-image-preview"
            :src="productForm.imageUrl"
            alt="商品主图预览"
          >
        </label>
        <div class="inline-form product-form">
          <el-select v-model="productForm.categoryId" placeholder="商品分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
          <el-input data-testid="product-name" v-model="productForm.name" placeholder="商品名称" />
          <el-input v-model="productForm.description" type="textarea" placeholder="商品描述" />
          <template v-if="!productForm.id">
            <label class="product-field">
              <span>首个规格名称</span>
              <el-input data-testid="initial-sku-name" v-model="productForm.initialSkuName" placeholder="例如：默认规格" />
            </label>
            <label class="product-field">
              <span>规格价格（元）</span>
              <el-input-number data-testid="initial-sku-price" v-model="productForm.initialSkuPrice" :min="0" />
            </label>
            <label class="product-field">
              <span>库存数量（个）</span>
              <el-input-number data-testid="initial-sku-stock" v-model="productForm.initialSkuStock" :min="0" />
            </label>
          </template>
          <el-button
            :data-testid="productForm.id ? 'save-product' : 'add-product'"
            type="primary"
            @click="saveProduct"
          >
            {{ productForm.id ? '保存商品' : '新增商品' }}
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="skuForm.productId" class="manage-panel sku-editor">
      <h2>{{ skuForm.id ? '编辑规格' : '新增规格' }}</h2>
      <div class="inline-form">
        <el-input data-testid="sku-name" v-model="skuForm.name" placeholder="规格名称" />
        <label class="product-field">
          <span>价格（元）</span>
          <el-input-number data-testid="sku-price" v-model="skuForm.price" :min="0" />
        </label>
        <label class="product-field">
          <span>库存（个）</span>
          <el-input-number data-testid="sku-stock" v-model="skuForm.stock" :min="0" />
        </label>
        <el-button data-testid="save-sku" type="primary" @click="saveSku">保存规格</el-button>
      </div>
    </div>

    <div v-if="products.length === 0" class="empty-list">暂无商品</div>

    <div v-else class="product-list">
      <article
        v-for="product in products"
        :key="product.id"
        :data-testid="`product-row-${product.id}`"
        class="product-row"
      >
        <div class="product-main">
          <img
            v-if="product.image?.url"
            :data-testid="`product-image-${product.id}`"
            :src="product.image.url"
            :alt="product.name"
          >
          <div v-else class="image-placeholder">暂无主图</div>
          <div class="product-copy">
            <strong>{{ product.name }}</strong>
            <span>分类：{{ product.categoryId }}</span>
            <span>{{ product.description || '暂无描述' }}</span>
          </div>
          <el-tag>{{ formatStatus(product.status) }}</el-tag>
          <div class="row-actions">
            <el-button :data-testid="`product-edit-${product.id}`" size="small" @click="fillProductForm(product)">编辑</el-button>
            <ConfirmAction
              :title="product.status === 'OFF_SALE' ? '确认上架该商品？' : '确认下架该商品？'"
              @confirm="toggleProductStatus(product)"
            >
              <el-button :data-testid="`product-toggle-${product.id}`" size="small" type="warning">
                {{ product.status === 'OFF_SALE' ? '上架' : '下架' }}
              </el-button>
            </ConfirmAction>
          </div>
        </div>

        <div class="sku-list">
          <div class="sku-heading">
            <strong>销售规格</strong>
            <el-button :data-testid="`add-sku-${product.id}`" size="small" @click="startCreateSku(product)">新增规格</el-button>
          </div>
          <div
            v-for="sku in product.skus || []"
            :key="sku.id"
            :data-testid="`sku-row-${sku.id}`"
            class="sku-row"
          >
            <span>{{ sku.name }}</span>
            <span>{{ formatPrice(sku.price) }}</span>
            <span>库存：{{ sku.stock }} 个</span>
            <el-tag>{{ formatStatus(sku.status, skuStatusMap) }}</el-tag>
            <el-button :data-testid="`sku-edit-${sku.id}`" size="small" @click="fillSkuForm(sku)">编辑</el-button>
            <ConfirmAction
              :title="sku.status === 'OFF_SALE' ? '确认上架该规格？' : '确认下架该规格？'"
              @confirm="toggleSkuStatus(sku)"
            >
              <el-button :data-testid="`sku-toggle-${sku.id}`" size="small" type="warning">
                {{ sku.status === 'OFF_SALE' ? '上架' : '下架' }}
              </el-button>
            </ConfirmAction>
          </div>
          <div v-if="!product.skus?.length" class="empty-skus">暂无销售规格</div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.product-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.product-summary > div { display: grid; gap: 8px; padding: 16px; background: #fff; border: 1px solid #e1e6e3; border-radius: 8px; }
.product-summary span, .section-heading p { color: #6c7772; font-size: 13px; }
.product-summary strong { color: #c8473d; font-size: 22px; }
.section-heading { display: flex; justify-content: space-between; margin-bottom: 12px; }
.section-heading h2, .sku-editor h2 { margin: 0; }
.section-heading p { margin: 6px 0 0; }
.product-editor { display: grid; grid-template-columns: 180px minmax(0, 1fr); gap: 20px; }
.image-uploader { display: grid; align-content: start; gap: 8px; color: #56615c; font-size: 13px; }
.image-uploader img, .product-main > img, .image-placeholder { width: 120px; aspect-ratio: 1; object-fit: cover; border: 1px solid #e1e6e3; border-radius: 6px; }
.image-placeholder { display: grid; place-items: center; color: #8a948f; background: #f6f7f6; font-size: 12px; }
.product-row { padding: 18px 0; border-top: 1px solid #edf0ee; }
.product-main { display: grid; grid-template-columns: 120px minmax(160px, 1fr) auto auto; align-items: center; gap: 16px; }
.product-copy { display: grid; gap: 6px; color: #6c7772; font-size: 13px; }
.product-copy strong { color: #25302b; font-size: 16px; }
.row-actions, .sku-heading { display: flex; align-items: center; gap: 8px; }
.sku-list { margin: 14px 0 0 136px; border-top: 1px solid #edf0ee; }
.sku-heading { justify-content: space-between; padding: 12px 0 6px; }
.sku-row { display: grid; grid-template-columns: minmax(100px, 1fr) 90px 120px 70px auto auto; align-items: center; gap: 10px; padding: 10px 0; color: #56615c; }
.empty-skus { padding: 10px 0; color: #8a948f; font-size: 13px; }

@media (max-width: 760px) {
  .product-summary { grid-template-columns: 1fr; }
  .product-editor, .product-main { grid-template-columns: 1fr; }
  .sku-list { margin-left: 0; }
  .sku-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
