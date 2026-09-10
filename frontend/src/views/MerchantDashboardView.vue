<template>
  <section class="merchant-dashboard">
    <div class="dashboard-summary">
      <article v-for="item in summaryItems" :key="item.label" class="summary-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <section class="dashboard-panel">
      <div class="panel-heading">
        <h2>待处理事项</h2>
        <span>数据将在联调后显示</span>
      </div>
      <p class="panel-empty">暂无待处理事项</p>
    </section>
  </section>
</template>

<script setup>
const summaryItems = [
  { label: '今日订单', value: '--' },
  { label: '待处理订单', value: '--' },
  { label: '今日销售额', value: '--' },
  { label: '在售商品', value: '--' },
]
</script>

<style scoped>
.merchant-dashboard { display: grid; gap: 18px; }
.dashboard-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.summary-item, .dashboard-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.summary-item { display: grid; gap: 10px; }
.summary-item span, .panel-heading span, .panel-empty { color: #6c7772; font-size: 14px; }
.summary-item strong { color: #c8473d; font-size: 24px; }
.panel-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; }
.panel-heading h2 { margin: 0; color: #202925; font-size: 18px; }
.panel-empty { margin: 28px 0 10px; text-align: center; }

@media (max-width: 760px) {
  .dashboard-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
