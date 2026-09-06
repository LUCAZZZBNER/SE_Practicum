<script setup>
import { onMounted, reactive, ref } from 'vue'
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
  price: 0,
  stock: 0,
  version: null,
})

function resetProductForm() {
  productForm.id = null
  productForm.categoryId = categories.value[0]?.id || null
  productForm.name = ''
  productForm.price = 0
  productForm.stock = 0
  productForm.version = null
}

function fillProductForm(product) {
  productForm.id = product.id
  productForm.categoryId = product.categoryId
  productForm.name = product.name
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
  await updateProduct(product.id, { status: 'OFF_SALE', version: product.version })
  await loadProducts()
}

async function saveProduct() {
  const payload = {
    categoryId: Number(productForm.categoryId),
    name: productForm.name.trim(),
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
      <h2>{{ productForm.id ? '编辑商品' : '新增商品' }}</h2>
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
        <el-input-number v-model="productForm.price" :min="0" />
        <el-input-number v-model="productForm.stock" :min="0" />
        <el-button type="primary" @click="saveProduct">{{ productForm.id ? '保存商品' : '新增商品' }}</el-button>
      </div>
    </div>

    <div v-if="products.length === 0" class="empty-list">暂无商品</div>

    <div v-else class="product-list">
      <div v-for="product in products" :key="product.id" class="product-row">
        <div>商品：{{ product.name }}</div>
        <div>分类：{{ product.categoryId }}</div>
        <div>价格：{{ product.price }}</div>
        <div>库存：{{ product.stock }}</div>
        <div>状态：{{ product.status }}</div>
        <el-button size="small" @click="fillProductForm(product)">编辑</el-button>
        <ConfirmAction title="确认下架该商品？" @confirm="offShelf(product)">
          <el-button size="small" type="warning">下架</el-button>
        </ConfirmAction>
      </div>
    </div>
  </section>
</template>
