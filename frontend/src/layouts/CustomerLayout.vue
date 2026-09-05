<script setup>
import { House, List, ShoppingCart, User } from '@element-plus/icons-vue'
import AppHeader from '../components/layout/AppHeader.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'

const menuItems = [
  { path: '/customer/stores', label: '店铺浏览', icon: House },
  { path: '/customer/cart', label: '购物车', icon: ShoppingCart },
  { path: '/customer/orders', label: '我的订单', icon: List },
  { path: '/customer/profile', label: '个人信息', icon: User },
]
</script>

<template>
  <el-container class="app-shell" direction="vertical">
    <AppHeader title="普通用户消费端" status-text="普通用户" />

    <el-container>
      <AppSidebar :menu-items="menuItems" />

      <el-main class="app-main">
        <div class="page-heading">
          <h1>{{ $route.meta.title || '普通用户' }}</h1>
          <span>浏览店铺、购物车、订单</span>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
