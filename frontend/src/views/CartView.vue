<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import EmptyState from '../components/common/EmptyState.vue'
import { createOrder } from '../api/order'
import { getCart, removeCartItem, updateCartItem } from '../api/cart'

const cartItems = ref([])
const loading = ref(false)
const selectedIds = ref([])
const idempotencyKey = ref(null)

const total = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
})

async function loadCart() {
  loading.value = true
  try {
    const data = await getCart()
    cartItems.value = data?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '购物车加载失败')
  } finally {
    loading.value = false
  }
}

async function changeQuantity(item) {
  await updateCartItem(item.id, { quantity: item.quantity + 1 })
  idempotencyKey.value = null
  await loadCart()
}

async function removeItem(item) {
  await removeCartItem(item.id)
  selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
  idempotencyKey.value = null
  await loadCart()
}

async function submitOrder() {
  const selected = cartItems.value.filter((item) => selectedIds.value.includes(item.id) && item.available)
  if (selected.length === 0) {
    ElMessage.error('请选择可结算的购物车项')
    return
  }
  const shops = new Set(selected.map((item) => item.product.shopId))
  if (shops.size !== 1) {
    ElMessage.error('一次结算只能选择同一店铺的商品')
    return
  }
  if (!idempotencyKey.value) idempotencyKey.value = crypto.randomUUID()
  try {
    const order = await createOrder(selected.map((item) => ({
      cartItemId: item.id,
      productVersion: item.product.version,
    })), idempotencyKey.value)
    idempotencyKey.value = null
    ElMessage.success(`订单 ${order.orderNumber} 创建成功`)
    await loadCart()
  } catch (error) {
    ElMessage.error(error?.message || '创建订单失败')
  }
}

onMounted(loadCart)
</script>

<template>
  <section class="content-stack">
    <EmptyState v-if="!loading && cartItems.length === 0" description="购物车暂为空" />

    <div v-else class="cart-list">
      <div v-for="item in cartItems" :key="item.id" class="cart-row">
        <el-checkbox v-model="selectedIds" :label="item.id" :disabled="!item.available">选择</el-checkbox>
        <div class="cart-meta">
          <div class="cart-name">{{ item.product.name }}</div>
          <div class="cart-detail">店铺：{{ item.product.shopId }}</div>
          <div class="cart-detail">单价：{{ item.product.price }}</div>
          <div class="cart-detail">数量：{{ item.quantity }}</div>
          <div class="cart-detail">库存：{{ item.product.stock }}</div>
        </div>

        <div class="cart-actions">
          <el-button size="small" @click="changeQuantity(item)">修改数量</el-button>
          <ConfirmAction title="确认删除该购物车项？" type="danger" @confirm="removeItem(item)">
            <el-button size="small" type="danger">删除</el-button>
          </ConfirmAction>
        </div>
      </div>
    </div>

    <div v-if="cartItems.length > 0" class="action-bar">
      <span>合计：{{ total.toFixed(2) }} 元</span>
      <el-button type="primary" @click="submitOrder">创建订单</el-button>
    </div>
  </section>
</template>
