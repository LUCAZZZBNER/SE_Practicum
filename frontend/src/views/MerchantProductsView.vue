<script setup>
import ConfirmAction from '../components/common/ConfirmAction.vue'

const products = [
  { id: 1, name: '招牌牛肉饭', category: '主食', price: 18.8, stock: 20, status: '上架' },
  { id: 2, name: '冰柠檬茶', category: '饮品', price: 6.0, stock: 35, status: '上架' },
]
</script>

<template>
  <section class="content-stack">
    <div class="action-bar">
      <el-button type="primary">新增商品</el-button>
      <el-button>新增分类</el-button>
    </div>

    <el-table :data="products" border>
      <el-table-column prop="name" label="商品" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="220">
        <template #default>
          <el-button size="small">编辑</el-button>
          <el-button size="small">调整库存</el-button>
          <ConfirmAction title="确认下架该商品？">
            <el-button size="small" type="warning">下架</el-button>
          </ConfirmAction>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
