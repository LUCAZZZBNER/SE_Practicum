<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, CircleCheck, CloseBold, CreditCard } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  cancelOrder,
  confirmOrderReceipt,
  getOrderDetail,
  getOrderRefund,
  payOrder,
} from '../api/order'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import {
  formatOrderPrice,
  formatOrderTime,
  getOrderStatus,
  getPaymentStatus,
  getRefundStatus,
} from '../utils/orderPresentation'

const route = useRoute()
const orderId = Number(route.params.id)
const loading = ref(false)
const actionLoading = ref(false)
const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const paymentKey = ref(null)
const cancelKey = ref(null)
const receiptKey = ref(null)
const refund = ref(null)
const order = reactive({
  id: '',
  orderNumber: '',
  shopName: '',
  shopAddressSnapshot: null,
  userAddressSnapshot: null,
  remark: '',
  status: '',
  paymentStatus: '',
  refundStatus: '',
  cancelReason: '',
  total: 0,
  createdAt: '',
  completedAt: '',
  lines: [],
})

const cancellable = computed(() => ['PENDING_PAYMENT', 'PAID', 'PREPARING'].includes(order.status))
const cancelReasonTooLong = computed(() => cancelReason.value.length > 200)

function copyOrder(data) {
  order.id = data?.id || ''
  order.orderNumber = data?.orderNumber || ''
  order.shopName = data?.shopName || ''
  order.shopAddressSnapshot = data?.shopAddressSnapshot || null
  order.userAddressSnapshot = data?.userAddressSnapshot || null
  order.remark = data?.remark || ''
  order.status = data?.status || ''
  order.paymentStatus = data?.paymentStatus || ''
  order.refundStatus = data?.refundStatus || ''
  order.cancelReason = data?.cancelReason || ''
  order.total = data?.total ?? 0
  order.createdAt = data?.createdAt || ''
  order.completedAt = data?.completedAt || ''
  order.lines = data?.lines || []
}

function formatUserAddress(address) {
  if (!address) return '-'
  return `${address.recipient} ${address.phone} ${address.region} ${address.detail}`
}

function formatShopAddress(address) {
  if (!address) return '-'
  return `${address.region} ${address.detail} ${address.phone || ''}`.trim()
}

async function loadOrder() {
  loading.value = true
  try {
    const data = await getOrderDetail(orderId)
    copyOrder(data)
    refund.value = null
    if (order.status === 'CANCELLED' && order.refundStatus === 'REFUNDED') {
      refund.value = await getOrderRefund(orderId)
    }
  } catch (error) {
    ElMessage.error(error?.message || '订单详情加载失败')
  } finally {
    loading.value = false
  }
}

async function payCurrentOrder() {
  if (actionLoading.value || order.status !== 'PENDING_PAYMENT') return
  if (!paymentKey.value) paymentKey.value = crypto.randomUUID()

  actionLoading.value = true
  try {
    await payOrder(orderId, paymentKey.value)
    paymentKey.value = null
    await loadOrder()
    ElMessage.success('模拟支付成功')
  } catch (error) {
    ElMessage.error(error?.message || '模拟支付失败')
  } finally {
    actionLoading.value = false
  }
}

function openCancelDialog() {
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

async function cancelCurrentOrder() {
  if (actionLoading.value || !cancellable.value || cancelReasonTooLong.value) return
  if (!cancelKey.value) cancelKey.value = crypto.randomUUID()

  actionLoading.value = true
  try {
    await cancelOrder(orderId, { reason: cancelReason.value.trim() }, cancelKey.value)
    cancelKey.value = null
    cancelDialogVisible.value = false
    await loadOrder()
    ElMessage.success('订单已取消')
  } catch (error) {
    ElMessage.error(error?.message || '取消订单失败')
  } finally {
    actionLoading.value = false
  }
}

async function confirmReceipt() {
  if (actionLoading.value || order.status !== 'DELIVERING') return
  if (!receiptKey.value) receiptKey.value = crypto.randomUUID()

  actionLoading.value = true
  try {
    await confirmOrderReceipt(orderId, receiptKey.value)
    receiptKey.value = null
    await loadOrder()
    ElMessage.success('已确认收货')
  } catch (error) {
    ElMessage.error(error?.message || '确认收货失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadOrder)
</script>

<template>
  <section class="order-detail">
    <el-button data-testid="back-to-orders" class="back-button" text @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回订单列表
    </el-button>

    <div v-if="loading">加载中...</div>
    <template v-else>
      <el-descriptions title="订单信息" border>
        <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatus(order.status).type">{{ getOrderStatus(order.status).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPaymentStatus(order.paymentStatus).type">{{ getPaymentStatus(order.paymentStatus).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款状态">
          <el-tag :type="getRefundStatus(order.refundStatus).type">{{ getRefundStatus(order.refundStatus).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatOrderPrice(order.total) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatOrderTime(order.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ formatUserAddress(order.userAddressSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="店铺地址">{{ formatShopAddress(order.shopAddressSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="订单备注">{{ order.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="order.cancelReason" label="取消原因">{{ order.cancelReason }}</el-descriptions-item>
        <el-descriptions-item v-if="order.completedAt" label="完成时间">{{ formatOrderTime(order.completedAt) }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="order.status === 'PENDING_PAYMENT' || cancellable || order.status === 'DELIVERING'" class="action-bar">
        <ConfirmAction
          v-if="order.status === 'PENDING_PAYMENT'"
          title="确认进行模拟支付？"
          @confirm="payCurrentOrder"
        >
          <el-button data-testid="pay-order" type="primary" :loading="actionLoading">
            <el-icon><CreditCard /></el-icon>
            模拟支付
          </el-button>
        </ConfirmAction>
        <el-button
          v-if="cancellable"
          data-testid="open-cancel"
          type="danger"
          plain
          :icon="CloseBold"
          :disabled="actionLoading"
          @click="openCancelDialog"
        >
          取消订单
        </el-button>
        <ConfirmAction
          v-if="order.status === 'DELIVERING'"
          title="确认已经收到商品？"
          @confirm="confirmReceipt"
        >
          <el-button data-testid="confirm-receipt" type="primary" :loading="actionLoading">
            <el-icon><CircleCheck /></el-icon>
            确认收货
          </el-button>
        </ConfirmAction>
      </div>

      <section class="order-items">
        <article
          v-for="item in order.lines"
          :key="item.skuId"
          :data-testid="`order-line-${item.skuId}`"
          class="order-item"
        >
          <img
            v-if="item.imageUrl"
            :data-testid="`line-image-${item.skuId}`"
            :src="item.imageUrl"
            :alt="item.productName"
          >
          <div v-else class="line-image-placeholder">暂无图片</div>
          <div class="line-copy">
            <strong>{{ item.productName }}</strong>
            <span>规格：{{ item.skuName }}</span>
          </div>
          <span>数量：{{ item.quantity }}</span>
          <span>单价：{{ formatOrderPrice(item.unitPrice) }}</span>
          <span class="line-subtotal">小计：{{ formatOrderPrice(item.subtotal) }}</span>
        </article>
      </section>

      <section v-if="refund" data-testid="refund-detail" class="refund-detail">
        <h2>退款信息</h2>
        <div>退款单号：{{ refund.refundNumber }}</div>
        <div>退款金额：{{ formatOrderPrice(refund.amount) }}</div>
        <div>退款状态：{{ getRefundStatus(refund.status).text }}</div>
        <div>完成时间：{{ formatOrderTime(refund.completedAt) }}</div>
      </section>
    </template>

    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="min(480px, 92vw)">
      <div class="cancel-form">
        <label for="cancel-reason">取消原因（选填）</label>
        <el-input
          id="cancel-reason"
          v-model="cancelReason"
          data-testid="cancel-reason"
          type="textarea"
          :rows="3"
          placeholder="最多 200 个字符"
        />
        <span class="reason-count">{{ cancelReason.length }}/200</span>
        <span v-if="cancelReasonTooLong" data-testid="cancel-reason-error" class="field-error">
          取消原因不能超过 200 个字符
        </span>
      </div>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">返回</el-button>
        <el-button
          data-testid="confirm-cancel"
          type="danger"
          :loading="actionLoading"
          :disabled="cancelReasonTooLong || actionLoading"
          @click="cancelCurrentOrder"
        >
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.order-detail { display: grid; gap: 18px; }
.back-button { justify-self: start; padding-left: 0; }
.action-bar { display: flex; justify-content: flex-end; gap: 10px; }
.order-items { display: grid; gap: 10px; }
.order-item {
  display: grid;
  grid-template-columns: 64px minmax(150px, 1fr) repeat(3, auto);
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
  color: #6c7772;
}
.order-item img, .line-image-placeholder { width: 64px; aspect-ratio: 1; object-fit: cover; border: 1px solid #e1e6e3; border-radius: 6px; }
.line-image-placeholder { display: grid; place-items: center; background: #f6f7f6; font-size: 11px; }
.line-copy { display: grid; gap: 5px; }
.line-copy strong { color: #202925; }
.line-subtotal { color: #c8473d; font-weight: 600; }
.refund-detail { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px 18px; padding: 16px 0; border-top: 1px solid #e1e6e3; color: #59655f; }
.refund-detail h2 { grid-column: 1 / -1; margin: 0; color: #202925; font-size: 18px; }
.cancel-form { display: grid; gap: 8px; }
.cancel-form label { color: #303b36; font-weight: 600; }
.reason-count { justify-self: end; color: #8a948f; font-size: 12px; }
.field-error { color: #c8473d; font-size: 13px; }

@media (max-width: 720px) {
  .action-bar { align-items: stretch; flex-direction: column; }
  .order-item { grid-template-columns: 56px minmax(0, 1fr); }
  .order-item > span { grid-column: 2; }
  .order-item img, .line-image-placeholder { width: 56px; }
  .refund-detail { grid-template-columns: 1fr; }
  .refund-detail h2 { grid-column: auto; }
}
</style>
