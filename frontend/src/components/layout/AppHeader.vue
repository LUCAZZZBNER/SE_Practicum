<script setup>
import { ShoppingCart, User } from '@element-plus/icons-vue'
import { clearSession } from '../../auth/session'
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  clearSession()
  router.push('/')
}

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
      <a href="/" @click.prevent="logout">退出</a>
    </div>
  </el-header>
</template>
