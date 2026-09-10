<script setup>
import { Goods, House, Setting, Tickets, User } from '@element-plus/icons-vue'
import AppHeader from '../components/layout/AppHeader.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'

const menuItems = [
  { path: '/merchant/dashboard', label: '工作台', shortLabel: '工作台', icon: House },
  { path: '/merchant/store', label: '店铺管理', shortLabel: '店铺', icon: Setting },
  { path: '/merchant/products', label: '商品管理', shortLabel: '商品', icon: Goods },
  { path: '/merchant/orders', label: '订单管理', shortLabel: '订单', icon: Tickets },
  { path: '/merchant/profile', label: '个人信息', shortLabel: '我的', icon: User },
]

function getActivePath(path) {
  if (path.startsWith('/merchant/orders')) return '/merchant/orders'
  if (path.startsWith('/merchant/products')) return '/merchant/products'
  if (path.startsWith('/merchant/store')) return '/merchant/store'
  if (path.startsWith('/merchant/profile')) return '/merchant/profile'
  return '/merchant/dashboard'
}
</script>

<template>
  <el-container class="app-shell" direction="vertical">
    <AppHeader title="商家工作台" profile-path="/merchant/profile" />

    <el-container>
      <AppSidebar :menu-items="menuItems" />

      <el-main class="app-main">
        <div class="page-heading">
          <h1>{{ $route.meta.title || '商家工作台' }}</h1>
        </div>
        <router-view />
      </el-main>
    </el-container>

    <nav class="merchant-mobile-nav" data-testid="mobile-navigation" aria-label="商家端导航">
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
