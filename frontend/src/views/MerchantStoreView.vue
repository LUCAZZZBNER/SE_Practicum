<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStoreDetail, updateStore } from '../api/store'
import { useMerchantShopStore } from '../stores/merchantShop'

const route = useRoute()
const shopStore = useMerchantShopStore()
const storeId = ref(null)
const creating = ref(false)

const store = reactive({
  name: '',
  status: '',
  description: '',
})

async function loadStore() {
  if (!shopStore.selectedShopId) return
  storeId.value = shopStore.selectedShopId
  try {
    const data = await getStoreDetail(storeId.value)
    store.name = data?.name || ''
    store.status = data?.status || ''
    store.description = data?.description || ''
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  }
}

async function saveStore() {
  try {
    await updateStore(storeId.value, {
      name: store.name,
      status: store.status,
      description: store.description,
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  }
}

async function loadShops() {
  await shopStore.loadShops()
  await loadStore()
}

async function addShop() {
  creating.value = true
  try {
    await shopStore.createShop({ name: '新店铺', description: '' })
    await loadStore()
  } finally {
    creating.value = false
  }
}

async function selectShop(id) {
  shopStore.selectShop(id)
  await loadStore()
}

onMounted(loadShops)
</script>

<template>
  <section v-if="!shopStore.loading && shopStore.shops.length === 0" class="content-stack">
    <el-empty description="暂无店铺" />
    <el-button type="primary" :loading="creating" @click="addShop">创建店铺</el-button>
  </section>
  <el-form v-else :model="store" label-width="90px" class="narrow-form">
    <el-form-item label="管理店铺">
      <el-select :model-value="shopStore.selectedShopId" @change="selectShop">
        <el-option v-for="shop in shopStore.shops" :key="shop.id" :label="shop.name" :value="shop.id" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="saveStore">保存店铺</el-button>
    </el-form-item>
    <div class="profile-field">店铺名称：{{ store.name }}</div>
    <div class="profile-field">营业状态：{{ store.status }}</div>
    <div class="profile-field">店铺简介：{{ store.description }}</div>
    <el-form-item label="店铺名称">
      <el-input v-model="store.name" />
    </el-form-item>
    <el-form-item label="营业状态">
      <el-radio-group v-model="store.status">
        <el-radio-button label="OPEN">营业</el-radio-button>
        <el-radio-button label="CLOSED">关店</el-radio-button>
        <el-radio-button label="TEMP_CLOSED">临时闭店</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="店铺公告">
      <el-input v-model="store.description" type="textarea" />
    </el-form-item>
  </el-form>
</template>
