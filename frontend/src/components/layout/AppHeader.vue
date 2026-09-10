<script setup>
import { Food, SwitchButton, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

defineProps({
  title: {
    type: String,
    default: '轻量级外卖服务平台',
  },
  profilePath: {
    type: String,
    required: true,
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
      <el-icon><Food /></el-icon>
      <router-link to="/">{{ title }}</router-link>
    </div>
    <div class="header-actions">
      <el-tooltip content="个人信息" placement="bottom">
        <router-link
          :to="profilePath"
          class="header-icon-link"
          data-testid="profile-link"
          aria-label="个人信息"
        >
          <el-icon><User /></el-icon>
        </router-link>
      </el-tooltip>
      <el-tooltip content="退出登录" placement="bottom">
        <el-button
          class="header-icon-button"
          data-testid="logout-button"
          aria-label="退出登录"
          text
          circle
          @click="logout"
        >
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </el-tooltip>
    </div>
  </el-header>
</template>
