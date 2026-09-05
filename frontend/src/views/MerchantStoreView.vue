<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStoreDetail, updateStoreStatus } from '../api/store'

const route = useRoute()
const storeId = Number(route.params.id || 7)

const store = reactive({
  name: '',
  status: '',
  notice: '',
})

async function loadStore() {
  try {
    const data = await getStoreDetail(storeId)
    store.name = data?.name || ''
    store.status = data?.status || ''
    store.notice = data?.notice || ''
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  }
}

async function saveStore() {
  try {
    await updateStoreStatus(storeId, {
      name: store.name,
      status: store.status,
      notice: store.notice,
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
    <div class="profile-field">店铺公告：{{ store.notice }}</div>
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
      <el-input v-model="store.notice" type="textarea" />
    </el-form-item>
  </el-form>
</template>
