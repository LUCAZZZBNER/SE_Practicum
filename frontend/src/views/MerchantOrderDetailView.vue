<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Goods, Van } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  deliverMerchantOrder,
  getMerchantOrderDetail,
  prepareMerchantOrder,
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
const prepareKey = ref(null)
const deliveryKey = ref(null)
const order = reactive({
  orderNumber: '',
  shopName: '',
  userAddressSnapshot: null,
  shopAddressSnapshot: null,
  remark: '',
  status: '',
  paymentStatus: '',
  refundStatus: '',
  cancelReason: '',
  total: 0,
  createdAt: '',
  lines: [],
})

function copyOrder(data) {
  order.orderNumber = data?.orderNumber || ''
  order.shopName = data?.shopName || ''
  order.userAddressSnapshot = data?.userAddressSnapshot || null
  order.shopAddressSnapshot = data?.shopAddressSnapshot || null
  order.remark = data?.remark || ''
  order.status = data?.status || ''
  order.paymentStatus = data?.paymentStatus || ''
  order.refundStatus = data?.refundStatus || ''
  order.cancelReason = data?.cancelReason || ''
  order.total = data?.total ?? 0
  order.createdAt = data?.createdAt || ''
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
    copyOrder(await getMerchantOrderDetail(orderId))
  } catch (error) {
    ElMessage.error(error?.message || '订单详情加载失败')
  } finally {
    loading.value = false
  }
}

async function startPreparation() {
  if (actionLoading.value || order.status !== 'PAID') return
  if (!prepareKey.value) prepareKey.value = crypto.randomUUID()

  actionLoading.value = true
  try {
    await prepareMerchantOrder(orderId, prepareKey.value)
    prepareKey.value = null
    await loadOrder()
    ElMessage.success('订单已开始制作')
  } catch (error) {
    ElMessage.error(error?.message || '开始制作失败')
  } finally {
    actionLoading.value = false
  }
}

async function startDelivery() {
  if (actionLoading.value || order.status !== 'PREPARING') return
  if (!deliveryKey.value) deliveryKey.value = crypto.randomUUID()

  actionLoading.value = true
  try {
    await deliverMerchantOrder(orderId, deliveryKey.value)
    deliveryKey.value = null
    await loadOrder()
    ElMessage.success('订单已开始配送')
  } catch (error) {
    ElMessage.error(error?.message || '开始配送失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadOrder)
</script>

<template>
  <section class="order-detail">
    <el-button data-testid="back-to-merchant-orders" class="back-button" text @click="$router.back()">
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
      </el-descriptions>

      <div v-if="order.status === 'PAID' || order.status === 'PREPARING'" class="action-bar">
        <ConfirmAction
          v-if="order.status === 'PAID'"
          title="确认开始制作该订单？"
          @confirm="startPreparation"
        >
          <el-button data-testid="prepare-order" type="primary" :loading="actionLoading">
            <el-icon><Goods /></el-icon>
            开始制作
          </el-button>
        </ConfirmAction>
        <ConfirmAction
          v-if="order.status === 'PREPARING'"
          title="确认该订单开始配送？"
          @confirm="startDelivery"
        >
          <el-button data-testid="deliver-order" type="primary" :loading="actionLoading">
            <el-icon><Van /></el-icon>
            开始配送
          </el-button>
        </ConfirmAction>
      </div>

      <section class="order-items">
        <article
          v-for="item in order.lines"
          :key="item.skuId"
          :data-testid="`merchant-order-line-${item.skuId}`"
          class="order-item"
        >
          <img
            v-if="item.imageUrl"
            :data-testid="`merchant-line-image-${item.skuId}`"
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
    </template>
  </section>
</template>

<style scoped>
.order-detail { display: grid; gap: 18px; }
.back-button { justify-self: start; padding-left: 0; }
.action-bar { display: flex; justify-content: flex-end; }
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

@media (max-width: 720px) {
  .order-item { grid-template-columns: 56px minmax(0, 1fr); }
  .order-item > span { grid-column: 2; }
  .order-item img, .line-image-placeholder { width: 56px; }
}
</style>
