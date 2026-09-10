<script setup>
import { reactive, ref } from 'vue'
import { Food, Lock, Shop, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { loginCustomer, loginMerchant } from '../api/user'

const router = useRouter()
const route = useRoute()
const activeRole = ref(route.query.role === 'merchant' ? 'merchant' : 'customer')
const submitting = ref(false)
const forms = reactive({
  customer: { account: '', password: '' },
  merchant: { account: '', password: '' },
})

const roleConfig = {
  customer: {
    role: 'USER',
    accountPlaceholder: '请输入用户账号',
    submitText: '用户登录',
    registerPath: '/register/customer',
    homePath: '/customer/stores',
    login: loginCustomer,
  },
  merchant: {
    role: 'MERCHANT',
    accountPlaceholder: '请输入商家账号',
    submitText: '商家登录',
    registerPath: '/register/merchant',
    homePath: '/merchant/store',
    login: loginMerchant,
  },
}

function selectRole(role) {
  activeRole.value = role
}

async function submitLogin() {
  if (submitting.value) return

  const config = roleConfig[activeRole.value]
  const form = forms[activeRole.value]
  submitting.value = true

  try {
    const data = await config.login({ account: form.account, password: form.password })
    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('user_role', data.roles?.[0] || config.role)
    ElMessage.success('登录成功')
    router.push(config.homePath)
  } catch (error) {
    ElMessage.error(error?.message || '登录失败')
  } finally {
    submitting.value = false
  }
}

function goToRegister() {
  router.push(roleConfig[activeRole.value].registerPath)
}
</script>

<template>
  <main class="auth-page auth-home-page">
    <section class="auth-home-shell" :class="`role-${activeRole}`" aria-labelledby="platform-title">
      <header class="auth-brand-block">
        <div class="auth-brand-mark" aria-hidden="true">
          <Food />
        </div>
        <h1 id="platform-title">轻量级外卖服务平台</h1>
      </header>

      <el-card shadow="never" class="login-card">
        <div class="role-tabs" role="tablist" aria-label="登录身份">
          <button
            type="button"
            role="tab"
            data-testid="customer-tab"
            :aria-selected="activeRole === 'customer'"
            :class="{ active: activeRole === 'customer' }"
            @click="selectRole('customer')"
          >
            <UserFilled aria-hidden="true" />
            <span>普通用户登录</span>
          </button>
          <button
            type="button"
            role="tab"
            data-testid="merchant-tab"
            :aria-selected="activeRole === 'merchant'"
            :class="{ active: activeRole === 'merchant' }"
            @click="selectRole('merchant')"
          >
            <Shop aria-hidden="true" />
            <span>商家登录</span>
          </button>
        </div>

        <div class="login-heading">
          <p>{{ activeRole === 'customer' ? '发现附近店铺，开始便捷点餐' : '管理店铺、商品与订单' }}</p>
        </div>

        <el-form class="login-form" :model="forms[activeRole]" label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="账号">
            <el-input
              v-model="forms[activeRole].account"
              :prefix-icon="UserFilled"
              :placeholder="roleConfig[activeRole].accountPlaceholder"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="forms[activeRole].password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submitLogin"
            />
          </el-form-item>

          <el-button
            class="login-submit"
            data-testid="login-submit"
            type="primary"
            :loading="submitting"
            :disabled="submitting"
            @click="submitLogin"
          >
            {{ roleConfig[activeRole].submitText }}
          </el-button>
        </el-form>

        <div class="register-row">
          <span>{{ activeRole === 'customer' ? '还没有用户账号？' : '还没有商家账号？' }}</span>
          <el-button data-testid="register-link" link @click="goToRegister">
            {{ activeRole === 'customer' ? '注册用户账号' : '注册商家账号' }}
          </el-button>
        </div>
      </el-card>
    </section>
  </main>
</template>

<style scoped>
.auth-home-page {
  align-items: flex-start;
  padding-top: 20px;
  background: #f2f5f3;
  background-image: url('../assets/food-pattern.svg');
  background-size: 280px 280px;
}
.auth-home-shell { width: min(460px, 100%); }
.auth-brand-block {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-bottom: 14px;
}
.auth-brand-mark {
  display: grid;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  place-items: center;
  color: #c8473d;
  background: #ffffff;
  border: 1px solid #dfe5e1;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.08);
}
.auth-brand-mark svg { width: 27px; height: 27px; }
.auth-brand-block h1 {
  margin: 0;
  color: #202925;
  font-size: 28px;
  line-height: 1.3;
  letter-spacing: 0;
}
.login-card {
  border: 1px solid #dfe5e1;
  border-radius: 8px;
  box-shadow:
    0 2px 8px rgba(31, 41, 55, 0.04),
    0 12px 32px rgba(31, 41, 55, 0.06);
}
.role-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  padding: 4px;
  background: #f1f3f2;
  border-radius: 6px;
}
.role-tabs button {
  display: flex;
  min-width: 0;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 8px 10px;
  color: #66716c;
  font: inherit;
  font-weight: 600;
  letter-spacing: 0;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 5px;
}
.role-tabs button svg { width: 17px; height: 17px; flex: 0 0 auto; }
.role-tabs button.active {
  color: #a83029;
  background: #ffffff;
  border-color: #e3d2cf;
  box-shadow: 0 2px 6px rgba(31, 41, 55, 0.06);
}
.role-merchant .role-tabs button.active { color: #176554; border-color: #c9ddd6; }
.role-tabs button:focus-visible { outline: 2px solid #2563eb; outline-offset: 2px; }
.login-heading { margin: 20px 0 18px; }
.login-heading p { margin: 0; color: #75807b; font-size: 16px; }
.login-form :deep(.el-form-item__label) { color: #3d4742; font-weight: 600; }
.login-form :deep(.el-input__wrapper) { min-height: 42px; }
.login-submit {
  width: 100%;
  min-height: 42px;
  margin-top: 4px;
  --el-button-bg-color: #c83f36;
  --el-button-border-color: #c83f36;
  --el-button-hover-bg-color: #ae342d;
  --el-button-hover-border-color: #ae342d;
  --el-button-active-bg-color: #962c26;
  --el-button-active-border-color: #962c26;
}
.role-merchant .login-submit {
  --el-button-bg-color: #176554;
  --el-button-border-color: #176554;
  --el-button-hover-bg-color: #125344;
  --el-button-hover-border-color: #125344;
  --el-button-active-bg-color: #0e4539;
  --el-button-active-border-color: #0e4539;
}
.register-row {
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-top: 18px;
  color: #75807b;
  font-size: 14px;
}
.register-row :deep(.el-button) { padding-right: 4px; padding-left: 4px; }

@media (max-width: 520px) {
  .auth-home-page { padding: 20px 16px; }
  .auth-brand-block h1 { font-size: 24px; }
  .role-tabs button { font-size: 13px; }
}
</style>
