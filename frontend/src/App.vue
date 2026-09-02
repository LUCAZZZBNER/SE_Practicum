<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Menu, ShoppingCart, User } from '@element-plus/icons-vue'

const route = useRoute()
const currentTitle = computed(() => route.meta.title || '外卖服务平台')
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">
        <el-icon><ShoppingCart /></el-icon>
        <span>轻量级外卖服务平台</span>
      </div>
      <div class="header-user">
        <el-icon><User /></el-icon>
        <span>未登录</span>
      </div>
    </el-header>

    <el-container>
      <el-aside width="220px" class="app-aside">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/home">
            <el-icon><Menu /></el-icon>
            <span>店铺浏览</span>
          </el-menu-item>
          <el-menu-item index="/cart">
            <el-icon><ShoppingCart /></el-icon>
            <span>购物车</span>
          </el-menu-item>
          <el-menu-item index="/orders">
            <el-icon><Menu /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="app-main">
        <div class="page-heading">
          <h1>{{ currentTitle }}</h1>
          <span>阶段 1 基础功能</span>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
