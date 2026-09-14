<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createShop,
  getStoreDetail,
  listStores,
  updateStoreAddress,
  updateStoreStatus,
} from '../api/store'

const storeId = ref(null)
const saving = ref(false)
const addressSaving = ref(false)

const store = reactive({
  name: '',
  description: '',
  status: 'CLOSED',
  region: '',
  detail: '',
  phone: '',
})

const statusMap = {
  OPEN: '营业中',
  CLOSED: '已关闭',
  TEMPORARILY_CLOSED: '暂时休息',
}

async function loadStore() {
  try {
    const mine = await listStores({ mine: true, page: 1, pageSize: 100 })
    const shop = mine?.items?.[0]

    if (!shop) {
      storeId.value = null
      return
    }

    storeId.value = shop.id

    const data = await getStoreDetail(shop.id)
    store.name = data?.name || ''
    store.description = data?.description || ''
    store.status = data?.status || ''
    store.region = data?.region || ''
    store.detail = data?.detail || ''
    store.phone = data?.phone || ''
  } catch (error) {
    ElMessage.error(error?.message || '店铺加载失败')
  }
}

async function saveStoreAddress() {
  if (!storeId.value || addressSaving.value) return

  const payload = {
    region: store.region.trim(),
    detail: store.detail.trim(),
    phone: store.phone.trim(),
  }

  if (!payload.region || !payload.detail || !payload.phone) {
    ElMessage.error('请完整填写经营地址')
    return
  }

  addressSaving.value = true
  try {
    await updateStoreAddress(storeId.value, payload)
    store.region = payload.region
    store.detail = payload.detail
    store.phone = payload.phone
    ElMessage.success('经营地址已保存')
  } catch (error) {
    ElMessage.error(error?.message || '经营地址保存失败')
  } finally {
    addressSaving.value = false
  }
}

async function saveStore() {
  if (saving.value) return

  saving.value = true
  try {
    if (!storeId.value) {
      const created = await createShop({
        name: store.name,
        description: store.description,
      })
      storeId.value = created?.id || null
      store.status = created?.status || 'CLOSED'
      ElMessage.success('创建成功')
      return
    }

    await updateStoreStatus(storeId.value, {
      name: store.name,
      description: store.description,
      status: store.status,
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function formatStatus(status) {
  return statusMap[status] || '状态未知'
}

onMounted(loadStore)
</script>

<template>
  <el-form :model="store" label-width="90px" class="narrow-form">
    <section data-testid="store-summary" class="store-summary">
      <div>
        <span class="summary-label">店铺名称</span>
        <strong>{{ store.name || '未创建' }}</strong>
      </div>
      <div>
        <span class="summary-label">营业状态</span>
        <strong>{{ formatStatus(store.status) }}</strong>
      </div>
      <div>
        <span class="summary-label">店铺简介</span>
        <span>{{ store.description || '暂无简介' }}</span>
      </div>
    </section>

    <section class="manage-panel">
      <div class="section-heading">
        <div>
          <h2>店铺资料</h2>
          <p v-if="!storeId">请创建你的店铺</p>
        </div>
        <el-button data-testid="save-store" type="primary" :loading="saving" :disabled="saving" @click="saveStore">
          {{ storeId ? '保存店铺' : '创建店铺' }}
        </el-button>
      </div>
      <el-form-item label="店铺名称">
        <el-input v-model="store.name" />
      </el-form-item>
      <el-form-item label="店铺简介">
        <el-input v-model="store.description" type="textarea" />
      </el-form-item>
    </section>

    <section class="manage-panel">
      <div class="section-heading">
        <h2>经营地址</h2>
        <el-button
          data-testid="save-store-address"
          type="primary"
          :loading="addressSaving"
          :disabled="!storeId || addressSaving"
          @click="saveStoreAddress"
        >
          保存地址
        </el-button>
      </div>
      <el-form-item label="所在区域">
        <el-input v-model="store.region" data-testid="store-region" maxlength="80" />
      </el-form-item>
      <el-form-item label="详细地址">
        <el-input v-model="store.detail" data-testid="store-detail" maxlength="120" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="store.phone" data-testid="store-phone" maxlength="20" />
      </el-form-item>
    </section>

    <section class="manage-panel">
      <div class="section-heading">
        <div>
          <h2>营业设置</h2>
          <p>店铺状态会影响用户端是否可正常下单</p>
        </div>
        <span class="status-badge">{{ formatStatus(store.status) }}</span>
      </div>
      <el-form-item label="营业状态">
        <el-radio-group v-model="store.status">
          <el-radio-button label="OPEN">营业</el-radio-button>
          <el-radio-button label="CLOSED">关店</el-radio-button>
          <el-radio-button label="TEMPORARILY_CLOSED">临时闭店</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </section>
  </el-form>
</template>

<style scoped>
.narrow-form { display: grid; gap: 18px; }
.store-summary, .manage-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.store-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.store-summary > div { display: grid; gap: 6px; }
.summary-label, .section-heading p { color: #6c7772; font-size: 13px; }
.store-summary strong { color: #202925; font-size: 18px; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.section-heading h2 { margin: 0; color: #202925; font-size: 18px; }
.section-heading p { margin: 6px 0 0; }
.status-badge { padding: 4px 10px; color: #287a4b; background: #edf8f0; border-radius: 999px; font-size: 13px; }

@media (max-width: 640px) {
  .store-summary { grid-template-columns: 1fr; }
  .section-heading { align-items: stretch; flex-direction: column; }
}
</style>
