<script setup>
const order = {
  id: 10001,
  store: '示例快餐店',
  status: '待支付',
  amount: 43.6,
  items: [
    { name: '招牌牛肉饭', quantity: 2, price: 18.8 },
    { name: '冰柠檬茶', quantity: 1, price: 6.0 },
  ],
}
</script>

<template>
  <section class="content-stack">
    <el-steps :active="1" finish-status="success">
      <el-step title="已创建" />
      <el-step title="待支付" />
      <el-step title="已完成" />
    </el-steps>

    <el-descriptions title="订单信息" border>
      <el-descriptions-item label="订单号">{{ order.id }}</el-descriptions-item>
      <el-descriptions-item label="店铺">{{ order.store }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ order.amount.toFixed(2) }} 元</el-descriptions-item>
    </el-descriptions>

    <div class="order-items">
      <div v-for="item in order.items" :key="item.name" class="order-item">
        <span>{{ item.name }}</span>
        <span>数量：{{ item.quantity }}</span>
        <span>成交单价：{{ item.price }}</span>
      </div>
    </div>
  </section>
</template>
