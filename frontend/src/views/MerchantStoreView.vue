<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStoreDetail, listStores, updateStoreStatus } from '../api/store'

const storeId = ref(null)

const store = reactive({
  name: '',
  description: '',
  status: '',
})

async function loadStore() {
  try {
    const mine = await listStores({ mine: true, page: 1, pageSize: 100 })
    const shop = mine?.items?.[0]

    if (!shop) {
      ElMessage.error('未找到店铺')
      return
    }

    storeId.value = shop.id

    const data = await getStoreDetail(shop.id)
    store.name = data?.name || ''
    store.description = data?.description || ''
    store.status = data?.status || ''
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  }
}

async function saveStore() {
  try {
    if (!storeId.value) {
      throw new Error('未找到店铺')
    }

    await updateStoreStatus(storeId.value, {
      name: store.name,
      description: store.description,
      status: store.status,
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  }
}

onMounted(loadStore)
</script>

<template>
  <el-form :model="store" label-width="90px" class="narrow-form">
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
        <el-radio-button label="TEMPORARILY_CLOSED">临时闭店</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="店铺简介">
      <el-input v-model="store.description" type="textarea" />
    </el-form-item>
  </el-form>
</template>
