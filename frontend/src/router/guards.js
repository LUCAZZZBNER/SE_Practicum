const PUBLIC_PATHS = new Set([
  '/',
  '/login/customer',
  '/login/merchant',
  '/register/customer',
  '/register/merchant',
  '/acceptance',
])

function getDashboardPath(role) {
  return role === 'MERCHANT' ? '/merchant/store' : '/customer/stores'
}

export function getAuthState() {
  return {
    token: localStorage.getItem('access_token'),
    role: localStorage.getItem('user_role'),
  }
}

export function resolveNavigation(to, auth = getAuthState()) {
  const { token, role } = auth

  if (PUBLIC_PATHS.has(to.path)) {
    if (token && (to.path === '/' || to.path.startsWith('/login') || to.path.startsWith('/register'))) {
      return getDashboardPath(role)
    }

    return null
  }

  if (!token) {
    return { path: '/', query: { redirect: to.fullPath || to.path } }
  }

  if (to.path.startsWith('/customer') && role !== 'USER') {
    return '/merchant/store'
  }

  if (to.path.startsWith('/merchant') && role !== 'MERCHANT') {
    return '/customer/stores'
  }

  return null
}

export function installRouteGuards(router) {
  router.beforeEach((to) => {
    const result = resolveNavigation(to)
    if (result) return result

    if (to.meta?.title) {
      document.title = `${to.meta.title} - 轻量级外卖服务平台`
    }

    return true
  })
}
