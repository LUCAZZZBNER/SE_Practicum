<script setup>
import { Shop, ShoppingCart, Tickets, User } from '@element-plus/icons-vue'
import AppHeader from '../components/layout/AppHeader.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'

const menuItems = [
  { path: '/customer/stores', label: '店铺浏览', shortLabel: '店铺', icon: Shop },
  { path: '/customer/cart', label: '购物车', shortLabel: '购物车', icon: ShoppingCart },
  { path: '/customer/orders', label: '我的订单', shortLabel: '订单', icon: Tickets },
  { path: '/customer/profile', label: '个人信息', shortLabel: '我的', icon: User },
]

function getActivePath(path) {
  if (path.startsWith('/customer/products/')) return '/customer/stores'
  if (path.startsWith('/customer/stores')) return '/customer/stores'
  if (path.startsWith('/customer/orders')) return '/customer/orders'
  return path
}
</script>

<template>
  <el-container class="app-shell" direction="vertical">
    <AppHeader title="轻量级外卖" profile-path="/customer/profile" />

    <el-container class="app-body">
      <AppSidebar :menu-items="menuItems" />

      <el-main class="app-main">
        <div class="page-heading">
          <h1>{{ $route.meta.title || '普通用户' }}</h1>
        </div>
        <router-view />
      </el-main>
    </el-container>

    <nav class="customer-mobile-nav" data-testid="mobile-navigation" aria-label="用户端导航">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        :class="{ active: getActivePath($route.path) === item.path }"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.shortLabel }}</span>
      </router-link>
    </nav>
  </el-container>
</template>
