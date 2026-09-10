const statusMap = {
  PENDING_PAYMENT: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  PREPARING: { text: '制作中', type: 'primary' },
  DELIVERING: { text: '配送中', type: 'primary' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
}

const chinaDateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export function getOrderStatus(status) {
  return statusMap[status] || { text: '状态未知', type: 'info' }
}

export function formatOrderPrice(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

export function formatOrderTime(value) {
  if (!value) return '-'

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : chinaDateTimeFormatter.format(date)
}
