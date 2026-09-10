<script setup>
import { onMounted, ref } from 'vue'
import { Search, Shop } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/common/EmptyState.vue'
import { listStores } from '../api/store'

const stores = ref([])
const loading = ref(false)
const keyword = ref('')

const statusMap = {
  OPEN: { text: '营业中', type: 'success' },
  TEMPORARILY_CLOSED: { text: '暂时休息', type: 'warning' },
  CLOSED: { text: '已关闭', type: 'info' },
}

async function loadStores() {
  loading.value = true
  try {
    const params = {
      page: 1,
      pageSize: 10,
    }
    const trimmedKeyword = keyword.value.trim()
    if (trimmedKeyword) params.keyword = trimmedKeyword

    const data = await listStores(params)
    stores.value = data?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  } finally {
    loading.value = false
  }
}

function isOpen(store) {
  return store.status === 'OPEN'
}

function getStatus(store) {
  return statusMap[store.status] || { text: '状态未知', type: 'info' }
}

onMounted(loadStores)
</script>

<template>
  <section class="customer-stores">
    <div class="store-toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索店铺"
        class="search-box"
        :prefix-icon="Search"
        @keyup.enter="loadStores"
        @clear="loadStores"
      />
      <el-button type="primary" :loading="loading" @click="loadStores">搜索</el-button>
    </div>

    <div v-if="loading" class="store-loading" aria-live="polite">正在加载店铺...</div>
    <EmptyState v-else-if="stores.length === 0" description="暂无可浏览店铺" />
    <div v-else class="store-list">
      <el-card
        v-for="store in stores"
        :key="store.id"
        data-testid="store-card"
        class="store-item"
        :class="{ 'is-unavailable': !isOpen(store) }"
        shadow="never"
      >
        <div class="store-card-content">
          <div class="store-visual" data-testid="store-visual" aria-hidden="true">
            <el-icon><Shop /></el-icon>
          </div>
          <div class="store-copy">
            <div class="store-title-row">
              <h2 class="store-name">{{ store.name }}</h2>
              <el-tag :type="getStatus(store).type" effect="light">
                {{ getStatus(store).text }}
              </el-tag>
            </div>
            <p>{{ store.description || '暂无简介' }}</p>
          </div>
          <el-button
            data-testid="store-action"
            type="primary"
            :disabled="!isOpen(store)"
            @click="$router.push(`/customer/stores/${store.id}`)"
          >
            {{ isOpen(store) ? '查看菜单' : '暂不可浏览' }}
          </el-button>
        </div>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.customer-stores { display: grid; gap: 18px; }
.store-toolbar { display: flex; max-width: 640px; gap: 10px; }
.search-box { max-width: none; margin: 0; }
.store-loading {
  padding: 36px 0;
  color: #7a8580;
  text-align: center;
}
.store-list { display: grid; gap: 12px; }
.store-item {
  border: 1px solid #e1e6e3;
  border-radius: 8px;
  transition: border-color 160ms ease, box-shadow 160ms ease, opacity 160ms ease;
}
.store-item:not(.is-unavailable):hover {
  border-color: #d5b9b5;
  box-shadow: 0 8px 24px rgba(42, 51, 47, 0.07);
}
.store-item.is-unavailable { opacity: 0.66; background: #fafbfa; }
.store-card-content {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
}
.store-visual {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  color: #c8473d;
  background: #f8efed;
  border-radius: 8px;
}
.store-visual .el-icon { font-size: 28px; }
.store-copy { min-width: 0; }
.store-title-row { display: flex; align-items: center; gap: 10px; }
.store-name {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #202925;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.store-copy p {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  color: #6c7772;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

@media (max-width: 640px) {
  .store-toolbar { align-items: stretch; }
  .store-card-content { grid-template-columns: 48px minmax(0, 1fr); gap: 12px; }
  .store-visual { width: 48px; height: 48px; }
  .store-card-content > .el-button { grid-column: 1 / -1; width: 100%; margin: 0; }
  .store-title-row { align-items: flex-start; justify-content: space-between; }
}
</style>
