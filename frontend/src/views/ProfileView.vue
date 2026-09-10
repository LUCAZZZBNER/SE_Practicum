<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '../api/user'
import { getMerchantProfile, updateMerchantProfile } from '../api/merchant'

const route = useRoute()
const isMerchant = computed(() => route.path.startsWith('/merchant'))
const saving = ref(false)

const userStatusMap = {
  ACTIVE: '正常',
  DISABLED: '已禁用',
}

const merchantStatusMap = {
  ACTIVE: '正常',
  SUSPENDED: '已暂停',
}

const profile = reactive({
  account: '',
  nickname: '',
  name: '',
  phone: '',
  status: '',
})

async function loadProfile() {
  const data = isMerchant.value ? await getMerchantProfile() : await getProfile()

  profile.account = data.account || ''
  profile.nickname = data.nickname || ''
  profile.name = data.name || ''
  profile.phone = data.phone || ''
  profile.status = data.status || ''
}

async function saveProfile() {
  if (saving.value) return

  saving.value = true
  try {
    let data
    if (isMerchant.value) {
      data = await updateMerchantProfile({
        name: profile.name,
        phone: profile.phone,
      })
    } else {
      data = await updateProfile({
        nickname: profile.nickname,
        phone: profile.phone,
      })
    }

    profile.account = data?.account || profile.account
    profile.nickname = data?.nickname || profile.nickname
    profile.name = data?.name || profile.name
    profile.phone = data?.phone || profile.phone
    profile.status = data?.status || profile.status
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function formatStatus(status) {
  const statusMap = isMerchant.value ? merchantStatusMap : userStatusMap
  return statusMap[status] || '状态未知'
}

onMounted(loadProfile)
</script>

<template>
  <el-form :model="profile" label-width="90px" class="narrow-form">
    <section class="profile-section">
      <h2>账户信息</h2>
      <div data-testid="profile-identity" class="profile-identity">
        <div>账号：{{ profile.account }}</div>
        <div>状态：{{ formatStatus(profile.status) }}</div>
      </div>
    </section>

    <section class="profile-section">
      <h2>可编辑资料</h2>
      <template v-if="isMerchant">
        <el-form-item label="商家名称">
          <el-input v-model="profile.name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" />
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" />
        </el-form-item>
      </template>
    </section>
    <el-form-item>
      <el-button data-testid="save-profile" type="primary" :loading="saving" :disabled="saving" @click="saveProfile">保存修改</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.narrow-form { display: grid; max-width: 560px; gap: 18px; }
.profile-section {
  padding: 18px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.profile-section h2 { margin: 0 0 16px; color: #202925; font-size: 18px; }
.profile-identity {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 14px 16px;
  color: #59655f;
  background: #f7f9f8;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
</style>
