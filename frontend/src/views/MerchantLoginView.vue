<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthFormCard from '../components/common/AuthFormCard.vue'
import { loginMerchant } from '../api/user'

const router = useRouter()
const form = reactive({
  account: '',
  password: '',
})

async function submitLogin() {
  try {
    const data = await loginMerchant({
      account: form.account,
      password: form.password,
    })

    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('user_role', data.roles?.[0] || 'MERCHANT')
    ElMessage.success('登录成功')
    router.push('/merchant/store')
  } catch (error) {
    ElMessage.error(error?.message || '登录失败')
  }
}
</script>

<template>
  <AuthFormCard title="商家登录">
    <el-form :model="form" label-width="80px">
      <el-form-item label="账号">
        <el-input v-model="form.account" placeholder="请输入商家账号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitLogin">登录</el-button>
        <el-button @click="$router.push('/register/merchant')">商家注册</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </el-form-item>
    </el-form>
  </AuthFormCard>
</template>
