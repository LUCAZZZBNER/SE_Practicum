<script setup>
import ConfirmAction from '../components/common/ConfirmAction.vue'
import EmptyState from '../components/common/EmptyState.vue'

const cartItems = [
  { id: 1, name: '招牌牛肉饭', store: '示例快餐店', price: 18.8, quantity: 2, stock: 20 },
  { id: 2, name: '冰柠檬茶', store: '示例快餐店', price: 6.0, quantity: 1, stock: 35 },
]
</script>

<template>
  <section class="content-stack">
    <EmptyState v-if="cartItems.length === 0" description="购物车暂为空" />

    <el-table v-else :data="cartItems" border>
      <el-table-column prop="name" label="商品" />
      <el-table-column prop="store" label="店铺" />
      <el-table-column prop="price" label="单价" width="100" />
      <el-table-column prop="quantity" label="数量" width="120" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column label="操作" width="180">
        <template #default>
          <el-button size="small">修改数量</el-button>
          <ConfirmAction title="确认删除该购物车项？" type="danger">
            <el-button size="small" type="danger">删除</el-button>
          </ConfirmAction>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="cartItems.length > 0" class="action-bar">
      <span>合计：43.60 元</span>
      <el-button type="primary">创建订单</el-button>
    </div>
  </section>
</template>
