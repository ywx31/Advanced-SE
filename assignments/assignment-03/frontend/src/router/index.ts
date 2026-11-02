import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '学习概览' },
        },
        {
          path: 'practice/new',
          name: 'practice-setup',
          component: () => import('../views/PracticeSetupView.vue'),
          meta: { title: '开始练习' },
        },
        {
          path: 'practice/:id',
          name: 'practice-session',
          component: () => import('../views/PracticeSessionView.vue'),
          meta: { title: '答题中' },
        },
        {
          path: 'result/:id',
          name: 'practice-result',
          component: () => import('../views/ResultView.vue'),
          meta: { title: '练习结果' },
        },
        {
          path: 'weekly-report',
          name: 'weekly-report',
          component: () => import('../views/WeeklyReportView.vue'),
          meta: { title: '学习周报' },
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('../views/HistoryView.vue'),
          meta: { title: '练习记录' },
        },
        {
          path: 'wrong-book',
          name: 'wrong-book',
          component: () => import('../views/WrongBookView.vue'),
          meta: { title: '错题本' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('../views/SettingsView.vue'),
          meta: { title: '设置' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'dashboard' }
  }
  document.title = `${String(to.meta.title || 'MathMate')} · MathMate`
  return true
})

export default router
