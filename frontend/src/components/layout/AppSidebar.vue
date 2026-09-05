<script setup>
import { useRoute } from 'vue-router'

defineProps({
  menuItems: {
    type: Array,
    required: true,
  },
})

const route = useRoute()
</script>

<template>
  <el-aside width="220px" class="app-aside">
    <el-menu :default-active="route.path" router>
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>
