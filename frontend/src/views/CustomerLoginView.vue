<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthFormCard from '../components/common/AuthFormCard.vue'
import { loginCustomer } from '../api/user'

const router = useRouter()
const form = reactive({
  account: '',
  password: '',
})

async function submitLogin() {
  try {
    const data = await loginCustomer({
      account: form.account,
      password: form.password,
    })

    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('user_role', data.roles?.[0] || 'USER')
    ElMessage.success('登录成功')
    router.push('/customer/stores')
  } catch (error) {
    ElMessage.error(error?.message || '登录失败')
  }
}
</script>

<template>
  <AuthFormCard title="普通用户登录">
    <el-form :model="form" label-width="80px">
      <el-form-item label="账号">
        <el-input v-model="form.account" placeholder="请输入用户账号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitLogin">登录</el-button>
        <el-button @click="$router.push('/register/customer')">用户注册</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </el-form-item>
    </el-form>
  </AuthFormCard>
</template>
