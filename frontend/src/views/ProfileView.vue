<script setup>
import { reactive } from 'vue'

const profile = reactive({
  username: 'alice',
  phone: '13800000000',
  address: '软件学院 1 号楼',
})
</script>

<template>
  <el-form :model="profile" label-width="90px" class="narrow-form">
    <el-form-item label="用户名">
      <el-input v-model="profile.username" />
    </el-form-item>
    <el-form-item label="手机号">
      <el-input v-model="profile.phone" />
    </el-form-item>
    <el-form-item label="默认地址">
      <el-input v-model="profile.address" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary">保存修改</el-button>
    </el-form-item>
  </el-form>
</template>
