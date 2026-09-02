<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const stores = [
  { name: '示例快餐店', category: '简餐', status: '营业中', delivery: '约 30 分钟' },
  { name: '校园面馆', category: '面食', status: '营业中', delivery: '约 25 分钟' },
  { name: '晚间烧烤', category: '烧烤', status: '休息中', delivery: '暂不可用' },
]

function addToCart(storeName) {
  ElMessage.success(`${storeName}：已加入购物车（示例操作）`)
}
</script>

<template>
  <section>
    <el-input v-model="keyword" clearable placeholder="搜索店铺或商品" class="search-box" />

    <div class="store-list">
      <el-card v-for="store in stores" :key="store.name" class="store-item" shadow="never">
        <div class="store-info">
          <div>
            <h2>{{ store.name }}</h2>
            <p>{{ store.category }} · {{ store.delivery }}</p>
          </div>
          <el-tag :type="store.status === '营业中' ? 'success' : 'info'">{{ store.status }}</el-tag>
        </div>
        <el-button type="primary" :disabled="store.status !== '营业中'" @click="addToCart(store.name)">
          查看商品
        </el-button>
      </el-card>
    </div>
  </section>
</template>
