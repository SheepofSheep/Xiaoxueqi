import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('../views/mobile/HomeView.vue') },
  { path: '/search', name: 'search', component: () => import('../views/mobile/SearchView.vue') },
  { path: '/recommendations', name: 'recommendations', component: () => import('../views/mobile/RecommendationView.vue') },
  { path: '/restaurants/:id', name: 'restaurant-detail', component: () => import('../views/mobile/RestaurantDetailView.vue') },
  { path: '/shake', name: 'shake', component: () => import('../views/mobile/ShakeView.vue') },
  { path: '/nearby', name: 'nearby', component: () => import('../views/mobile/NearbyView.vue') },
  { path: '/discover', name: 'discover', component: () => import('../views/mobile/DiscoverView.vue') },
  { path: '/categories', redirect: '/discover' },
  { path: '/favorites', name: 'favorites', component: () => import('../views/mobile/CollectionView.vue') },
  { path: '/history', name: 'history', component: () => import('../views/mobile/CollectionView.vue') },
  { path: '/profile', name: 'profile', component: () => import('../views/mobile/ProfileView.vue') },
  { path: '/taste-mirror', name: 'taste-mirror', component: () => import('../views/mobile/TasteMirrorView.vue') },
  { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/AdminLoginView.vue') },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'restaurants', name: 'admin-restaurants', component: () => import('../views/admin/AdminRestaurantView.vue') },
      { path: 'import', name: 'admin-import', component: () => import('../views/admin/AdminImportView.vue') },
      { path: 'amap-fetch', name: 'admin-amap-fetch', component: () => import('../views/admin/AdminAmapFetch.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('auth_token')
    const role = localStorage.getItem('auth_role')
    if (!token || !role || role !== to.meta.role) {
      return '/admin/login'
    }
  }
  if (to.path === '/admin/login' && localStorage.getItem('auth_token')) {
    return '/admin'
  }
  return true
})

export default router
