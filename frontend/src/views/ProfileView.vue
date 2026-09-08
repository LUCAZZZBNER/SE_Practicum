<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '../api/user'
import { getMerchantProfile, updateMerchantProfile } from '../api/merchant'

const route = useRoute()
const isMerchant = computed(() => route.path.startsWith('/merchant'))

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
  try {
    if (isMerchant.value) {
      await updateMerchantProfile({
        name: profile.name,
        phone: profile.phone,
      })
    } else {
      await updateProfile({
        nickname: profile.nickname,
        phone: profile.phone,
      })
    }

    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  }
}

onMounted(loadProfile)
</script>

<template>
  <el-form :model="profile" label-width="90px" class="narrow-form">
    <template v-if="isMerchant">
      <div class="profile-field">账号：{{ profile.account }}</div>
      <div class="profile-field">状态：{{ profile.status }}</div>
      <div class="profile-field">商家名称</div>
      <el-form-item label="商家名称">
        <el-input v-model="profile.name" />
      </el-form-item>
      <div class="profile-field">手机号</div>
      <el-form-item label="手机号">
        <el-input v-model="profile.phone" />
      </el-form-item>
    </template>
    <template v-else>
      <div class="profile-field">账号：{{ profile.account }}</div>
      <div class="profile-field">状态：{{ profile.status }}</div>
      <div class="profile-field">昵称</div>
      <el-form-item label="昵称">
        <el-input v-model="profile.nickname" />
      </el-form-item>
      <div class="profile-field">手机号</div>
      <el-form-item label="手机号">
        <el-input v-model="profile.phone" />
      </el-form-item>
    </template>
    <el-form-item>
      <el-button type="primary" @click="saveProfile">保存修改</el-button>
    </el-form-item>
  </el-form>
</template>
