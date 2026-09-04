<script setup>
import { getCurrentInstance, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import AuthFormCard from '../components/common/AuthFormCard.vue'
import { registerCustomer } from '../api/user'

const form = reactive({
  account: '',
  password: '',
  passwordConfirm: '',
  nickname: '',
  phone: '',
})
const instance = getCurrentInstance()

async function submitRegister() {
  try {
    await registerCustomer({
      account: form.account,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
      nickname: form.nickname,
      phone: form.phone,
    })

    ElMessage.success('注册成功')
    instance?.proxy?.$router?.push('/login/customer')
  } catch (error) {
    ElMessage.error(error?.message || '注册失败')
  }
}
</script>

<template>
  <AuthFormCard title="普通用户注册">
    <el-form :model="form" label-width="100px">
      <el-form-item label="账号">
        <el-input v-model="form.account" placeholder="请输入用户账号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.passwordConfirm" type="password" show-password placeholder="请再次输入密码" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitRegister">提交注册</el-button>
        <el-button @click="$router.push('/login/customer')">去登录</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </el-form-item>
    </el-form>
  </AuthFormCard>
</template>
