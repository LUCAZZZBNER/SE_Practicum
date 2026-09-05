<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import AuthFormCard from '../components/common/AuthFormCard.vue'

const form = reactive({
  username: '',
  password: '',
  phone: '',
  storeName: '',
  storeCategory: '',
})

function submitRegister() {
  ElMessage.success('商家注册占位：接口确认后接入真实请求')
}
</script>

<template>
  <AuthFormCard title="商家注册">
    <el-form :model="form" label-width="100px">
      <el-form-item label="商家账号">
        <el-input v-model="form.username" placeholder="请输入商家账号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="店铺名称">
        <el-input v-model="form.storeName" placeholder="请输入店铺名称" />
      </el-form-item>
      <el-form-item label="店铺类别">
        <el-input v-model="form.storeCategory" placeholder="例如：简餐、面食、饮品" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitRegister">提交注册</el-button>
        <el-button @click="$router.push('/login/merchant')">去登录</el-button>
        <el-button link @click="$router.push('/')">返回首页</el-button>
      </el-form-item>
    </el-form>
  </AuthFormCard>
</template>
