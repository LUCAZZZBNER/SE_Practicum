<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStoreDetail } from '../api/store'

const route = useRoute()
const store = reactive({
  name: '',
  description: '',
  status: '',
})
const categories = ['热销', '主食', '饮品']
const products = [
  { id: 1, name: '招牌牛肉饭', category: '主食', price: 18.8, stock: 20, status: '在售' },
  { id: 2, name: '冰柠檬茶', category: '饮品', price: 6.0, stock: 35, status: '在售' },
  { id: 3, name: '鸡腿套餐', category: '热销', price: 24.8, stock: 0, status: '售罄' },
]

async function loadStoreDetail() {
  try {
    const data = await getStoreDetail(Number(route.params.id))
    store.name = data.name || ''
    store.description = data.description || ''
    store.status = data.status || ''
  } catch (error) {
    ElMessage.error(error?.message || '店铺详情加载失败')
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
      <el-tab-pane v-for="category in categories" :key="category" :label="category">
        <el-table :data="products.filter((item) => category === '热销' || item.category === category)" border>
          <el-table-column prop="name" label="商品" />
          <el-table-column prop="price" label="价格" width="100" />
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/customer/products/${row.id}`)">详情</el-button>
              <el-button size="small" type="primary" :disabled="row.stock <= 0">加入购物车</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
