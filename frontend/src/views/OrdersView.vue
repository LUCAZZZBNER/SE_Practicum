<script setup>
import EmptyState from '../components/common/EmptyState.vue'

const orders = [
  { id: 10001, store: '示例快餐店', amount: 43.6, status: '待支付', createdAt: '2026-09-03 10:30' },
  { id: 10002, store: '校园面馆', amount: 22.0, status: '已完成', createdAt: '2026-09-02 18:12' },
]
</script>

<template>
  <EmptyState v-if="orders.length === 0" description="暂无订单" />

  <el-table v-else :data="orders" border>
    <el-table-column prop="id" label="订单号" width="120" />
    <el-table-column prop="store" label="店铺" />
    <el-table-column prop="amount" label="金额" width="100" />
    <el-table-column prop="status" label="状态" width="120" />
    <el-table-column prop="createdAt" label="创建时间" width="180" />
    <el-table-column label="操作" width="120">
      <template #default="{ row }">
        <el-button size="small" type="primary" @click="$router.push(`/customer/orders/${row.id}`)">查看</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
