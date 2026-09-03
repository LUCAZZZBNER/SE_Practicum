import { createRouter, createWebHistory } from 'vue-router'
import acceptanceRoutes from './modules/acceptance'
import authRoutes from './modules/auth'
import customerRoutes from './modules/customer'
import merchantRoutes from './modules/merchant'

const routes = [
  ...authRoutes,
  ...customerRoutes,
  ...merchantRoutes,
  ...acceptanceRoutes,
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
