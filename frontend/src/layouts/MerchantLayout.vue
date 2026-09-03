<script setup>
import { Goods, Setting, User } from '@element-plus/icons-vue'
import AppHeader from '../components/layout/AppHeader.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'

const menuItems = [
  { path: '/merchant/store', label: '店铺管理', icon: Setting },
  { path: '/merchant/products', label: '商品管理', icon: Goods },
  { path: '/merchant/profile', label: '个人信息', icon: User },
]
</script>

<template>
  <el-container class="app-shell" direction="vertical">
    <AppHeader title="商家管理端" status-text="商家" />

    <el-container>
      <AppSidebar :menu-items="menuItems" />

      <el-main class="app-main">
        <div class="page-heading">
          <h1>{{ $route.meta.title || '商家管理' }}</h1>
          <span>店铺、商品、库存管理</span>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
