<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import EmptyState from '../components/common/EmptyState.vue'
import { createOrder } from '../api/order'
import { getCart, removeCartItem, updateCartItem } from '../api/cart'

const cartItems = ref([])
const loading = ref(false)
const submitting = ref(false)
const checkoutKey = ref(null)

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
  await updateCartItem(item.id, { quantity: Number(item.quantity) })
  checkoutKey.value = null
  await loadCart()
}

async function removeItem(item) {
  await removeCartItem(item.id)
  checkoutKey.value = null
  await loadCart()
}

async function submitOrder() {
  if (submitting.value) return

  if (!checkoutKey.value) {
    checkoutKey.value = crypto.randomUUID()
  }

  submitting.value = true
  try {
    await createOrder(
      {
        items: cartItems.value.map((item) => ({
          cartItemId: item.id,
          productVersion: item.product.version,
        })),
      },
      {
        headers: {
          'X-Idempotency-Key': checkoutKey.value,
        },
      },
    )
    checkoutKey.value = null
    await loadCart()
    ElMessage.success('订单创建成功')
  } catch (error) {
    ElMessage.error(error?.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCart)
</script>

<template>
  <section class="content-stack">
    <EmptyState v-if="!loading && cartItems.length === 0" description="购物车暂为空" />

    <div v-else class="cart-list">
      <div v-for="item in cartItems" :key="item.id" class="cart-row">
        <div class="cart-meta">
          <div class="cart-name">{{ item.product.name }}</div>
          <div class="cart-detail">店铺：{{ item.product.shopId }}</div>
          <div class="cart-detail">单价：{{ item.product.price }}</div>
          <div class="cart-detail">数量：{{ item.quantity }}</div>
          <div class="cart-detail">库存：{{ item.product.stock }}</div>
        </div>

        <div class="cart-actions">
          <el-input-number
            v-model="item.quantity"
            data-testid="cart-quantity"
            :min="1"
            :max="item.product.stock"
          />
          <el-button size="small" @click="changeQuantity(item)">修改数量</el-button>
          <ConfirmAction title="确认删除该购物车项？" type="danger" @confirm="removeItem(item)">
            <el-button size="small" type="danger">删除</el-button>
          </ConfirmAction>
        </div>
      </div>
    </div>

    <div v-if="cartItems.length > 0" class="action-bar">
      <span>合计：{{ total.toFixed(2) }} 元</span>
      <el-button type="primary" :disabled="submitting" @click="submitOrder">创建订单</el-button>
    </div>
  </section>
</template>
