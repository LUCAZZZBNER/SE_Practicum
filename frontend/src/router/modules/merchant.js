export default [
  {
    path: '/merchant',
    component: () => import('../../layouts/MerchantLayout.vue'),
    redirect: '/merchant/store',
    meta: { requiresAuth: true, role: 'MERCHANT' },
    children: [
      {
        path: 'store',
        name: 'MerchantStore',
        component: () => import('../../views/MerchantStoreView.vue'),
        meta: { title: '店铺管理' },
      },
      {
        path: 'products',
        name: 'MerchantProducts',
        component: () => import('../../views/MerchantProductsView.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: () => import('../../views/MerchantOrdersView.vue'),
        meta: { title: '店铺订单' },
      },
      {
        path: 'orders/:id',
        name: 'MerchantOrderDetail',
        component: () => import('../../views/MerchantOrderDetailView.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'profile',
        name: 'MerchantProfile',
        component: () => import('../../views/ProfileView.vue'),
        meta: { title: '个人信息' },
      },
    ],
  },
]
