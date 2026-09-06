<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listStores } from '../api/store'

const stores = ref([])
const loading = ref(false)
const keyword = ref('')

async function loadStores() {
  loading.value = true
  try {
    const data = await listStores({
      page: 1,
      pageSize: 10,
      keyword: keyword.value || undefined,
    })
    stores.value = data?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  } finally {
    loading.value = false
  }
}

function searchStores() {
  loadStores()
}

function isOpen(store) {
  return store.status === 'OPEN'
}

onMounted(loadStores)
</script>

<template>
  <section>
    <el-input v-model="keyword" clearable placeholder="搜索店铺" class="search-box" @keyup.enter="searchStores" />
    <el-button @click="searchStores">搜索</el-button>

    <div v-if="loading" class="store-list">加载中...</div>
    <div v-else-if="stores.length === 0" class="store-list">暂无店铺</div>
    <div v-else class="store-list">
      <el-card v-for="store in stores" :key="store.id" class="store-item" shadow="never">
        <div class="store-info">
          <div>
            <h2>{{ store.name }}</h2>
            <p>{{ store.description || '暂无简介' }}</p>
          </div>
          <el-tag :type="isOpen(store) ? 'success' : 'info'">{{ store.status }}</el-tag>
        </div>
        <el-button
          type="primary"
          :disabled="!isOpen(store)"
          @click="$router.push(`/customer/stores/${store.id}`)"
        >
          查看商品
        </el-button>
      </el-card>
    </div>
  </section>
</template>
