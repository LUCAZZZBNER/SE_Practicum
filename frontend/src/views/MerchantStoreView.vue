<script setup>
import { reactive } from 'vue'

const store = reactive({
  name: '示例快餐店',
  status: 'OPEN',
  notice: '欢迎下单',
})
</script>

<template>
  <el-form :model="store" label-width="90px" class="narrow-form">
    <el-form-item label="店铺名称">
      <el-input v-model="store.name" />
    </el-form-item>
    <el-form-item label="营业状态">
      <el-radio-group v-model="store.status">
        <el-radio-button label="OPEN">营业</el-radio-button>
        <el-radio-button label="CLOSED">关店</el-radio-button>
        <el-radio-button label="TEMP_CLOSED">临时闭店</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="店铺公告">
      <el-input v-model="store.notice" type="textarea" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary">保存店铺</el-button>
    </el-form-item>
  </el-form>
</template>
