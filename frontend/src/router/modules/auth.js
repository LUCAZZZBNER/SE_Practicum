export default [
  {
    path: '/',
    name: 'AuthHome',
    component: () => import('../../views/AuthHomeView.vue'),
    meta: { title: '登录与注册' },
  },
  {
    path: '/login/customer',
    name: 'CustomerLogin',
    component: () => import('../../views/CustomerLoginView.vue'),
    meta: { title: '普通用户登录' },
  },
  {
    path: '/login/merchant',
    name: 'MerchantLogin',
    component: () => import('../../views/MerchantLoginView.vue'),
    meta: { title: '商家登录' },
  },
  {
    path: '/register/customer',
    name: 'CustomerRegister',
    component: () => import('../../views/CustomerRegisterView.vue'),
    meta: { title: '普通用户注册' },
  },
  {
    path: '/register/merchant',
    name: 'MerchantRegister',
    component: () => import('../../views/MerchantRegisterView.vue'),
    meta: { title: '商家注册' },
  },
]
