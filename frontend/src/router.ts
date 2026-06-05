import { createRouter, createWebHistory } from 'vue-router'
import HomeMap from './views/HomeMap.vue'
import RouteDetail from './views/RouteDetail.vue'
import FlowAnalysis from './views/FlowAnalysis.vue'
import AiAssistant from './views/AiAssistant.vue'
import AdminLogin from './views/admin/AdminLogin.vue'
import AdminDashboard from './views/admin/AdminDashboard.vue'
import AdminData from './views/admin/AdminData.vue'
import AdminDispatch from './views/admin/AdminDispatch.vue'
import { getToken } from './services/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeMap },
    { path: '/routes/:id', component: RouteDetail },
    { path: '/analysis', component: FlowAnalysis },
    { path: '/ai', component: AiAssistant },
    { path: '/admin/login', component: AdminLogin },
    { path: '/admin/dashboard', component: AdminDashboard, meta: { admin: true } },
    { path: '/admin/data', component: AdminData, meta: { admin: true } },
    { path: '/admin/dispatch', component: AdminDispatch, meta: { admin: true } }
  ]
})

router.beforeEach((to) => {
  if (to.meta.admin && !getToken()) {
    return '/admin/login'
  }
})
