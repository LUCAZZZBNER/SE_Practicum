<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Delete, Edit, LocationFilled, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createUserAddress,
  listUserAddresses,
  removeUserAddress,
  updateUserAddress,
} from '../api/address'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import EmptyState from '../components/common/EmptyState.vue'

const addresses = ref([])
const loading = ref(false)
const saving = ref(false)
const activeActionId = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)

const form = reactive({
  recipient: '',
  phone: '',
  region: '',
  detail: '',
  isDefault: false,
})

function resetForm() {
  editingId.value = null
  form.recipient = ''
  form.phone = ''
  form.region = ''
  form.detail = ''
  form.isDefault = false
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(address) {
  editingId.value = address.id
  form.recipient = address.recipient
  form.phone = address.phone
  form.region = address.region
  form.detail = address.detail
  form.isDefault = address.isDefault
  dialogVisible.value = true
}

async function loadAddresses() {
  loading.value = true
  try {
    const data = await listUserAddresses()
    addresses.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.message || '地址加载失败')
  } finally {
    loading.value = false
  }
}

function getPayload() {
  return {
    recipient: form.recipient.trim(),
    phone: form.phone.trim(),
    region: form.region.trim(),
    detail: form.detail.trim(),
    isDefault: form.isDefault,
  }
}

function validate(payload) {
  if (!payload.recipient || !payload.phone || !payload.region || !payload.detail) {
    ElMessage.error('请完整填写收货地址')
    return false
  }

  if (!/^1\d{10}$/.test(payload.phone)) {
    ElMessage.error('手机号格式不正确')
    return false
  }

  return true
}

async function saveAddress() {
  if (saving.value) return

  const payload = getPayload()
  if (!validate(payload)) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateUserAddress(editingId.value, payload)
      ElMessage.success('地址已更新')
    } else {
      await createUserAddress(payload)
      ElMessage.success('地址已新增')
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error?.message || '地址保存失败')
  } finally {
    saving.value = false
  }
}

async function setDefault(address) {
  if (activeActionId.value) return

  activeActionId.value = address.id
  try {
    await updateUserAddress(address.id, { isDefault: true })
    ElMessage.success('默认地址已更新')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error?.message || '默认地址设置失败')
  } finally {
    activeActionId.value = null
  }
}

async function deleteAddress(address) {
  if (activeActionId.value) return

  activeActionId.value = address.id
  try {
    await removeUserAddress(address.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error?.message || '地址删除失败')
  } finally {
    activeActionId.value = null
  }
}

onMounted(loadAddresses)
</script>

<template>
  <section class="addresses-page" :aria-busy="loading">
    <div class="addresses-toolbar">
      <span>{{ addresses.length }} 个地址</span>
      <el-button data-testid="add-address" type="primary" :icon="Plus" @click="openCreate">
        新增地址
      </el-button>
    </div>

    <EmptyState v-if="!loading && addresses.length === 0" description="暂无收货地址" />

    <div v-else class="address-list">
      <article
        v-for="address in addresses"
        :key="address.id"
        :data-testid="`address-${address.id}`"
        class="address-item"
      >
        <div class="address-main">
          <div class="address-contact">
            <strong>{{ address.recipient }}</strong>
            <span>{{ address.phone }}</span>
            <el-tag v-if="address.isDefault" size="small" type="success">默认地址</el-tag>
          </div>
          <p>{{ address.region }} {{ address.detail }}</p>
        </div>

        <div class="address-actions">
          <el-button
            v-if="!address.isDefault"
            :data-testid="`default-address-${address.id}`"
            :icon="LocationFilled"
            text
            :loading="activeActionId === address.id"
            @click="setDefault(address)"
          >
            设为默认
          </el-button>
          <el-button
            :data-testid="`edit-address-${address.id}`"
            :icon="Edit"
            text
            @click="openEdit(address)"
          >
            编辑
          </el-button>
          <ConfirmAction title="确认删除该收货地址？" type="danger" @confirm="deleteAddress(address)">
            <el-button
              :data-testid="`delete-address-${address.id}`"
              :icon="Delete"
              text
              type="danger"
            >
              删除
            </el-button>
          </ConfirmAction>
        </div>
      </article>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑收货地址' : '新增收货地址'"
      width="min(520px, 92vw)"
    >
      <el-form :model="form" label-width="82px">
        <el-form-item label="收货人">
          <el-input v-model="form.recipient" data-testid="address-recipient" maxlength="30" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" data-testid="address-phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在区域">
          <el-input v-model="form.region" data-testid="address-region" maxlength="80" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detail" data-testid="address-detail" maxlength="120" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isDefault" data-testid="address-default">
            设为默认地址
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          data-testid="save-address"
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="saveAddress"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.addresses-page { display: grid; gap: 16px; }
.addresses-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.addresses-toolbar span { color: #6c7772; font-size: 14px; }
.address-list { display: grid; gap: 12px; }
.address-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.address-main { min-width: 0; }
.address-contact { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.address-contact strong { color: #202925; font-size: 17px; }
.address-contact span { color: #59655f; }
.address-main p { margin: 9px 0 0; color: #59655f; line-height: 1.6; overflow-wrap: anywhere; }
.address-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 4px; }

@media (max-width: 640px) {
  .address-item { align-items: stretch; flex-direction: column; }
  .address-actions { justify-content: flex-end; flex-wrap: wrap; }
}
</style>
