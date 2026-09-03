export default [
  {
    path: '/customer',
    component: () => import('../../layouts/CustomerLayout.vue'),
    redirect: '/customer/stores',
    children: [
      {
        path: 'stores',
        name: 'CustomerStores',
        component: () => import('../../views/CustomerStoresView.vue'),
        meta: { title: '店铺浏览' },
      },
      {
        path: 'stores/:id',
        name: 'CustomerStoreDetail',
        component: () => import('../../views/StoreDetailView.vue'),
        meta: { title: '店铺详情' },
      },
      {
        path: 'products/:id',
        name: 'CustomerProductDetail',
        component: () => import('../../views/ProductDetailView.vue'),
        meta: { title: '商品详情' },
      },
      {
        path: 'cart',
        name: 'CustomerCart',
        component: () => import('../../views/CartView.vue'),
        meta: { title: '购物车' },
      },
      {
        path: 'orders',
        name: 'CustomerOrders',
        component: () => import('../../views/OrdersView.vue'),
        meta: { title: '我的订单' },
      },
      {
        path: 'orders/:id',
        name: 'CustomerOrderDetail',
        component: () => import('../../views/OrderDetailView.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'profile',
        name: 'CustomerProfile',
        component: () => import('../../views/ProfileView.vue'),
        meta: { title: '个人信息' },
      },
    ],
  },
]
