<template>
  <section class="content-stack">
    <el-alert
      title="这里用于整理第一周联调和验收材料，后续可与 docs/test 中的记录保持一致。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-table
      :data="[
        { module: '用户模块', page: '登录/注册/个人信息', status: '页面骨架完成', risk: '待确认接口' },
        { module: '店铺模块', page: '店铺列表/详情/状态', status: '页面骨架完成', risk: '待确认状态枚举' },
        { module: '商品模块', page: '商品列表/详情/商家管理', status: '页面骨架完成', risk: '待确认分类字段' },
        { module: '购物车订单', page: '购物车/订单列表/详情', status: '页面骨架完成', risk: '待确认创建订单规则' },
      ]"
      border
    >
      <el-table-column prop="module" label="模块" width="140" />
      <el-table-column prop="page" label="页面范围" />
      <el-table-column prop="status" label="当前状态" width="160" />
      <el-table-column prop="risk" label="待确认项" />
    </el-table>
  </section>
</template>
