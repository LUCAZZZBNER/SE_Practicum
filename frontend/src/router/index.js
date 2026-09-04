import { createRouter, createWebHistory } from 'vue-router'
import acceptanceRoutes from './modules/acceptance'
import authRoutes from './modules/auth'
import customerRoutes from './modules/customer'
import { installRouteGuards } from './guards'
import merchantRoutes from './modules/merchant'

const routes = [
  ...authRoutes,
  ...customerRoutes,
  ...merchantRoutes,
  ...acceptanceRoutes,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

installRouteGuards(router)

export default router
