<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthFormCard from '../components/common/AuthFormCard.vue'

const router = useRouter()
const form = reactive({
  username: '',
  password: '',
})

function submitLogin() {
  ElMessage.success('普通用户登录占位：接口确认后接入真实请求')
  router.push('/customer/stores')
}
</script>

<template>
  <AuthFormCard title="普通用户登录">
    <el-form :model="form" label-width="80px">
      <el-form-item label="账号">
        <el-input v-model="form.username" placeholder="请输入用户账号" />
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
