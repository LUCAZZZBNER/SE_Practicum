<script setup>
import { ShoppingCart, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

defineProps({
  title: {
    type: String,
    default: '轻量级外卖服务平台',
  },
  statusText: {
    type: String,
    default: '未登录',
  },
})

function logout() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('user_role')
  router.replace('/')
}
</script>

<template>
  <el-header class="app-header">
    <div class="brand">
      <el-icon><ShoppingCart /></el-icon>
      <router-link to="/">{{ title }}</router-link>
    </div>
    <div class="header-user">
      <el-icon><User /></el-icon>
      <span>{{ statusText }}</span>
      <router-link to="/" @click="logout">退出</router-link>
    </div>
  </el-header>
</template>
