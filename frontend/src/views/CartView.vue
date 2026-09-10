<script setup>
import { computed, onMounted, ref } from 'vue'
import { Delete, Shop, ShoppingCart } from '@element-plus/icons-vue'
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

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

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
  <section class="cart-page">
    <EmptyState v-if="!loading && cartItems.length === 0" description="购物车暂为空">
      <el-button data-testid="browse-stores" type="primary" @click="$router.push('/customer/stores')">
        <el-icon><Shop /></el-icon>
        去逛店铺
      </el-button>
    </EmptyState>

    <div v-else class="cart-list">
      <div
        v-for="item in cartItems"
        :key="item.id"
        :data-testid="`cart-item-${item.id}`"
        class="cart-row"
      >
        <div class="cart-meta">
          <div class="cart-name">{{ item.product.name }}</div>
          <div class="cart-detail">店铺：{{ item.product.shopId }}</div>
          <div class="cart-detail">单价：{{ formatPrice(item.product.price) }}</div>
          <div class="cart-detail">库存：{{ item.product.stock }}</div>
          <div class="cart-subtotal">小计：{{ formatPrice(item.subtotal) }}</div>
        </div>

        <div class="cart-actions">
          <el-input-number
            v-model="item.quantity"
            data-testid="cart-quantity"
            :min="1"
            :max="item.product.stock"
          />
          <el-button :data-testid="`update-item-${item.id}`" size="small" @click="changeQuantity(item)">
            更新数量
          </el-button>
          <ConfirmAction title="确认删除该购物车项？" type="danger" @confirm="removeItem(item)">
            <el-button
              :data-testid="`delete-item-${item.id}`"
              :aria-label="`删除${item.product.name}`"
              :title="`删除${item.product.name}`"
              circle
              plain
              size="small"
              type="danger"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </ConfirmAction>
        </div>
      </div>
    </div>

    <div v-if="cartItems.length > 0" data-testid="checkout-bar" class="checkout-bar">
      <span class="checkout-total">合计：{{ formatPrice(total) }}</span>
      <el-button
        data-testid="checkout-button"
        type="primary"
        :disabled="submitting"
        @click="submitOrder"
      >
        <el-icon><ShoppingCart /></el-icon>
        提交订单
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.cart-page { display: grid; gap: 18px; }
.cart-list { display: grid; gap: 12px; }
.cart-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.cart-meta { display: grid; grid-template-columns: repeat(3, auto); align-items: baseline; gap: 7px 18px; }
.cart-name { grid-column: 1 / -1; color: #202925; font-size: 17px; font-weight: 600; }
.cart-detail { color: #6c7772; font-size: 14px; }
.cart-subtotal { color: #c8473d; font-weight: 600; }
.cart-actions { display: flex; align-items: center; gap: 8px; }
.checkout-bar {
  position: sticky;
  bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(42, 51, 47, 0.08);
}
.checkout-total { color: #202925; font-size: 18px; font-weight: 700; }

@media (max-width: 720px) {
  .cart-row { align-items: stretch; flex-direction: column; }
  .cart-meta { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .cart-actions { flex-wrap: wrap; }
  .checkout-bar { bottom: 8px; justify-content: space-between; }
}
</style>
