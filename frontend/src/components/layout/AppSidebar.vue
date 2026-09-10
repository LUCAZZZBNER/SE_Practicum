<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  menuItems: {
    type: Array,
    required: true,
  },
})

const route = useRoute()

const activePath = computed(() => {
  if (route.path.startsWith('/customer/products/')) {
    return '/customer/stores'
  }

  const matches = props.menuItems
    .map((item) => item.path)
    .filter((path) => route.path === path || route.path.startsWith(`${path}/`))
    .sort((left, right) => right.length - left.length)

  return matches[0] || route.path
})
</script>

<template>
  <el-aside width="220px" class="app-aside">
    <el-menu :default-active="activePath" router>
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>
