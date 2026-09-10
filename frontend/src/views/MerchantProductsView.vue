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
import { getMerchantProfile } from '../api/merchant'
import { listStores } from '../api/store'
import { createProduct, listProducts, updateProduct } from '../api/product'

const products = ref([])
const categories = ref([])
const shopId = ref(null)
const categoryName = ref('')
const productForm = reactive({
  id: null,
  categoryId: null,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  version: null,
})

const productStatusMap = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
}

const onSaleCount = computed(() => products.value.filter((product) => product.status === 'ON_SALE').length)

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function formatStatus(status) {
  return productStatusMap[status] || '状态未知'
}

function resetProductForm() {
  productForm.id = null
  productForm.categoryId = categories.value[0]?.id || null
  productForm.name = ''
  productForm.description = ''
  productForm.price = 0
  productForm.stock = 0
  productForm.version = null
}

function fillProductForm(product) {
  productForm.id = product.id
  productForm.categoryId = product.categoryId
  productForm.name = product.name
  productForm.description = product.description || ''
  productForm.price = product.price
  productForm.stock = product.stock
  productForm.version = product.version
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

async function offShelf(product) {
  const status = product.status === 'OFF_SALE' ? 'ON_SALE' : 'OFF_SALE'
  await updateProduct(product.id, { status, version: product.version })
  await loadProducts()
}

async function saveProduct() {
  const payload = {
    categoryId: Number(productForm.categoryId),
    name: productForm.name.trim(),
    description: productForm.description.trim(),
    price: Number(productForm.price),
    stock: Number(productForm.stock),
  }

  if (!shopId.value || !payload.categoryId || !payload.name || payload.price <= 0) {
    ElMessage.error('请填写有效商品信息')
    return
  }

  if (productForm.id) {
    await updateProduct(productForm.id, {
      ...payload,
      version: productForm.version,
    })
  } else {
    await createProduct({
      shopId: shopId.value,
      ...payload,
    })
  }

  resetProductForm()
  await loadProducts()
}

onMounted(loadProducts)
</script>

<template>
  <section class="content-stack">
    <section class="product-summary">
      <div>
        <span>商品总数</span>
        <strong>{{ products.length }}</strong>
      </div>
      <div>
        <span>在售商品</span>
        <strong>{{ onSaleCount }}</strong>
      </div>
      <div>
        <span>已下架</span>
        <strong>{{ products.length - onSaleCount }}</strong>
      </div>
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
          <p>完善价格和库存后再保存商品</p>
        </div>
      </div>
      <div class="inline-form product-form">
        <el-select v-model="productForm.categoryId" placeholder="商品分类">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
        <el-input v-model="productForm.name" placeholder="商品名称" />
        <el-input v-model="productForm.description" type="textarea" placeholder="商品描述" />
        <label class="product-field">
          <span>商品价格（元）</span>
          <el-input-number v-model="productForm.price" :min="0" />
        </label>
        <label class="product-field">
          <span>库存数量（个）</span>
          <el-input-number v-model="productForm.stock" :min="0" />
        </label>
        <el-button
          :data-testid="productForm.id ? 'save-product' : 'add-product'"
          type="primary"
          @click="saveProduct"
        >
          {{ productForm.id ? '保存商品' : '新增商品' }}
        </el-button>
      </div>
    </div>

    <div v-if="products.length === 0" class="empty-list">暂无商品</div>

    <div v-else class="product-list">
      <div
        v-for="product in products"
        :key="product.id"
        :data-testid="`product-row-${product.id}`"
        class="product-row"
      >
        <div>商品：{{ product.name }}</div>
        <div>分类：{{ product.categoryId }}</div>
        <div>价格：{{ formatPrice(product.price) }}</div>
        <div>库存：{{ product.stock }} 个</div>
        <div>状态：{{ formatStatus(product.status) }}</div>
        <el-button
          :data-testid="`product-edit-${product.id}`"
          size="small"
          @click="fillProductForm(product)"
        >编辑</el-button>
        <ConfirmAction
          :title="product.status === 'OFF_SALE' ? '确认上架该商品？' : '确认下架该商品？'"
          @confirm="offShelf(product)"
        >
          <el-button
            :data-testid="`product-toggle-${product.id}`"
            size="small"
            type="warning"
          >
            {{ product.status === 'OFF_SALE' ? '上架' : '下架' }}
          </el-button>
        </ConfirmAction>
      </div>
    </div>
  </section>
</template>

<style scoped>
.product-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.product-summary > div {
  display: grid;
  gap: 8px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.product-summary span, .section-heading p { color: #6c7772; font-size: 13px; }
.product-summary strong { color: #c8473d; font-size: 22px; }
.section-heading { display: flex; justify-content: space-between; margin-bottom: 12px; }
.section-heading h2 { margin: 0; }
.section-heading p { margin: 6px 0 0; }
.product-row { padding: 14px 0; border-top: 1px solid #edf0ee; }

@media (max-width: 640px) {
  .product-summary { grid-template-columns: 1fr; }
}
</style>
