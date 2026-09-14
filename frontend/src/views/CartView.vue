<script setup>
import { computed, onMounted, ref } from 'vue'
import { Delete, Location, Shop, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listUserAddresses } from '../api/address'
import { getCart, removeCartItem, updateCartItem } from '../api/cart'
import { createOrder } from '../api/order'
import ConfirmAction from '../components/common/ConfirmAction.vue'
import EmptyState from '../components/common/EmptyState.vue'

const cartItems = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
const cartTotal = ref(0)
const loading = ref(false)
const submitting = ref(false)
const checkoutKey = ref(null)

const remarkTooLong = computed(() => remark.value.length > 200)
const allItemsAvailable = computed(() => (
  cartItems.value.length > 0 && cartItems.value.every((item) => item.available)
))
const singleShop = computed(() => (
  new Set(cartItems.value.map((item) => item.product.shopId)).size <= 1
))
const canCheckout = computed(() => (
  allItemsAvailable.value
  && singleShop.value
  && Boolean(selectedAddressId.value)
  && !remarkTooLong.value
  && !submitting.value
))

function formatPrice(price) {
  return `¥${Number(price || 0).toFixed(2)}`
}

function formatAddress(address) {
  return `${address.recipient} ${address.phone} ${address.region} ${address.detail}`
}

function resetCheckoutKey() {
  checkoutKey.value = null
}

async function loadCart() {
  const data = await getCart()
  cartItems.value = data?.items || []
  cartTotal.value = Number(data?.total || 0)
}

async function loadInitialData() {
  loading.value = true
  try {
    const [, addressData] = await Promise.all([loadCart(), listUserAddresses()])
    addresses.value = Array.isArray(addressData) ? addressData : []
    selectedAddressId.value = addresses.value.find((address) => address.isDefault)?.id || null
  } catch (error) {
    ElMessage.error(error?.message || '购物车加载失败')
  } finally {
    loading.value = false
  }
}

async function changeQuantity(item) {
  try {
    await updateCartItem(item.id, { quantity: Number(item.quantity) })
    resetCheckoutKey()
    await loadCart()
  } catch (error) {
    ElMessage.error(error?.message || '购物车数量更新失败')
  }
}

async function removeItem(item) {
  try {
    await removeCartItem(item.id)
    resetCheckoutKey()
    await loadCart()
  } catch (error) {
    ElMessage.error(error?.message || '购物车商品删除失败')
  }
}

async function submitOrder() {
  if (!canCheckout.value) return

  if (!checkoutKey.value) {
    checkoutKey.value = crypto.randomUUID()
  }

  submitting.value = true
  try {
    const order = await createOrder(
      {
        items: cartItems.value.map((item) => ({
          cartItemId: item.id,
          skuVersion: item.sku.version,
        })),
        addressId: selectedAddressId.value,
        remark: remark.value.trim(),
      },
      checkoutKey.value,
    )
    checkoutKey.value = null
    ElMessage.success('订单创建成功')
    return order
  } catch (error) {
    ElMessage.error(error?.message || '创建订单失败')
    return null
  } finally {
    submitting.value = false
  }
}

async function checkout(router) {
  const order = await submitOrder()
  if (order?.id) {
    router.push(`/customer/orders/${order.id}`)
  }
}

onMounted(loadInitialData)
</script>

<template>
  <section class="cart-page" :aria-busy="loading">
    <EmptyState v-if="!loading && cartItems.length === 0" description="购物车暂为空">
      <el-button data-testid="browse-stores" type="primary" @click="$router.push('/customer/stores')">
        <el-icon><Shop /></el-icon>
        去逛店铺
      </el-button>
    </EmptyState>

    <div v-else class="cart-list">
      <article
        v-for="item in cartItems"
        :key="item.id"
        :data-testid="`cart-item-${item.id}`"
        class="cart-row"
      >
        <img
          v-if="item.product.imageUrl"
          :data-testid="`product-image-${item.id}`"
          :src="item.product.imageUrl"
          :alt="item.product.name"
          class="cart-image"
        >
        <div v-else class="cart-image image-placeholder">暂无图片</div>

        <div class="cart-meta">
          <div class="cart-name">{{ item.product.name }}</div>
          <div class="cart-detail">规格：{{ item.sku.name }}</div>
          <div class="cart-detail">单价：{{ formatPrice(item.sku.price) }}</div>
          <div class="cart-detail">库存：{{ item.sku.stock }}</div>
          <div class="cart-subtotal">小计：{{ formatPrice(item.subtotal) }}</div>
          <el-tag v-if="!item.available" type="danger">
            {{ item.unavailableReason || '当前商品不可购买' }}
          </el-tag>
        </div>

        <div class="cart-actions">
          <el-input-number
            v-model="item.quantity"
            :data-testid="`cart-quantity-${item.id}`"
            :min="1"
            :max="item.sku.stock"
            :disabled="!item.available"
          />
          <el-button
            :data-testid="`update-item-${item.id}`"
            size="small"
            :disabled="!item.available"
            @click="changeQuantity(item)"
          >
            更新数量
          </el-button>
          <ConfirmAction title="确认删除该购物车项？" type="danger" @confirm="removeItem(item)">
            <el-button
              :data-testid="`delete-item-${item.id}`"
              :aria-label="`删除${item.product.name}${item.sku.name}`"
              :title="`删除${item.product.name}${item.sku.name}`"
              circle
              plain
              size="small"
              type="danger"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </ConfirmAction>
        </div>
      </article>
    </div>

    <section v-if="cartItems.length > 0" class="checkout-panel">
      <div class="checkout-field">
        <label for="checkout-address">收货地址</label>
        <el-select
          v-if="addresses.length > 0"
          id="checkout-address"
          v-model="selectedAddressId"
          data-testid="checkout-address"
          placeholder="请选择收货地址"
          @change="resetCheckoutKey"
        >
          <el-option
            v-for="address in addresses"
            :key="address.id"
            :label="formatAddress(address)"
            :value="address.id"
          />
        </el-select>
        <div v-else class="address-empty">
          <span>请先添加收货地址</span>
          <el-button data-testid="manage-addresses" text @click="$router.push('/customer/addresses')">
            <el-icon><Location /></el-icon>
            管理地址
          </el-button>
        </div>
      </div>

      <div class="checkout-field">
        <label for="order-remark">订单备注</label>
        <el-input
          id="order-remark"
          v-model="remark"
          data-testid="order-remark"
          type="textarea"
          :rows="3"
          placeholder="选填，最多 200 个字符"
          @input="resetCheckoutKey"
        />
        <span class="remark-count">{{ remark.length }}/200</span>
        <span v-if="remarkTooLong" data-testid="remark-error" class="field-error">
          订单备注不能超过 200 个字符
        </span>
      </div>

      <div v-if="!singleShop" class="field-error">不同店铺的商品需要分别下单</div>
    </section>

    <div v-if="cartItems.length > 0" data-testid="checkout-bar" class="checkout-bar">
      <span class="checkout-total">合计：{{ formatPrice(cartTotal) }}</span>
      <el-button
        data-testid="checkout-button"
        type="primary"
        :loading="submitting"
        :disabled="!canCheckout"
        @click="checkout($router)"
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
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 16px;
  border: 1px solid #e1e6e3;
  border-radius: 8px;
}
.cart-image { width: 88px; aspect-ratio: 1; object-fit: cover; border: 1px solid #e1e6e3; border-radius: 6px; }
.image-placeholder { display: grid; place-items: center; color: #8a948f; background: #f6f7f6; font-size: 12px; }
.cart-meta { display: grid; grid-template-columns: repeat(3, auto); align-items: baseline; gap: 7px 18px; }
.cart-name { grid-column: 1 / -1; color: #202925; font-size: 17px; font-weight: 600; }
.cart-detail { color: #6c7772; font-size: 14px; }
.cart-subtotal { color: #c8473d; font-weight: 600; }
.cart-actions { display: flex; align-items: center; gap: 8px; }
.checkout-panel { display: grid; gap: 16px; padding: 18px 0; border-top: 1px solid #e1e6e3; }
.checkout-field { display: grid; gap: 8px; }
.checkout-field > label { color: #303b36; font-weight: 600; }
.address-empty { display: flex; align-items: center; gap: 12px; color: #8a5d24; }
.remark-count { justify-self: end; color: #8a948f; font-size: 12px; }
.field-error { color: #c8473d; font-size: 13px; }
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

@media (max-width: 760px) {
  .cart-row { grid-template-columns: 72px minmax(0, 1fr); }
  .cart-image { width: 72px; }
  .cart-meta { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .cart-actions { grid-column: 1 / -1; flex-wrap: wrap; }
  .checkout-bar { bottom: 8px; justify-content: space-between; }
}
</style>
