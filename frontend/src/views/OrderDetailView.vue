<template>
  <section class="content-stack">
    <el-steps :active="1" finish-status="success">
      <el-step title="已创建" />
      <el-step title="待支付" />
      <el-step title="已完成" />
    </el-steps>

    <el-descriptions title="订单信息" border>
      <el-descriptions-item label="订单号">10001</el-descriptions-item>
      <el-descriptions-item label="店铺">示例快餐店</el-descriptions-item>
      <el-descriptions-item label="状态">待支付</el-descriptions-item>
      <el-descriptions-item label="金额">43.60 元</el-descriptions-item>
    </el-descriptions>

    <el-table :data="[{ name: '招牌牛肉饭', quantity: 2, price: 18.8 }, { name: '冰柠檬茶', quantity: 1, price: 6.0 }]" border>
      <el-table-column prop="name" label="商品" />
      <el-table-column prop="quantity" label="数量" width="100" />
      <el-table-column prop="price" label="成交单价" width="120" />
    </el-table>
  </section>
</template>
