import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('../views/LayoutView.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/DashboardView.vue')
        },
        {
          path: 'cities',
          name: 'Cities',
          component: () => import('../views/CityManageView.vue')
        },
        {
          path: 'weather',
          name: 'Weather',
          component: () => import('../views/WeatherView.vue')
        },
        {
          path: 'analysis',
          name: 'Analysis',
          component: () => import('../views/AnalysisView.vue')
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('../views/UserManageView.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else if (to.path === '/users') {
    const perm = Number(localStorage.getItem('permission') || 1)
    if (perm !== 2) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
