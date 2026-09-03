<template>
  <section class="content-stack">
    <el-descriptions title="招牌牛肉饭" border>
      <el-descriptions-item label="所属店铺">示例快餐店</el-descriptions-item>
      <el-descriptions-item label="分类">主食</el-descriptions-item>
      <el-descriptions-item label="价格">18.80 元</el-descriptions-item>
      <el-descriptions-item label="库存">20</el-descriptions-item>
      <el-descriptions-item label="状态">在售</el-descriptions-item>
    </el-descriptions>

    <div class="action-bar">
      <el-input-number :model-value="1" :min="1" :max="20" />
      <el-button type="primary">加入购物车</el-button>
    </div>
  </section>
</template>
