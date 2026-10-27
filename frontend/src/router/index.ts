import { createRouter, createWebHistory } from 'vue-router'
import DayPage from '@/pages/DayPage.vue'

/** `meta.titleKey` is the i18n key of the page title (document title is set in App.vue). */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/day' },
    { path: '/day/:date?', name: 'day', component: DayPage, meta: { titleKey: 'nav.day' } },
    {
      path: '/month/:month?',
      name: 'month',
      component: () => import('@/pages/MonthPage.vue'),
      meta: { titleKey: 'nav.month' },
    },
    {
      path: '/events',
      name: 'events',
      component: () => import('@/pages/EventsPage.vue'),
      meta: { titleKey: 'nav.events' },
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('@/pages/CategoriesPage.vue'),
      meta: { titleKey: 'nav.categories' },
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('@/pages/TagsPage.vue'),
      meta: { titleKey: 'nav.tags' },
    },
    {
      path: '/views/:id?',
      name: 'views',
      component: () => import('@/pages/ViewsPage.vue'),
      meta: { titleKey: 'nav.views' },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/pages/SettingsPage.vue'),
      meta: { titleKey: 'nav.settings' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/day' },
  ],
})

export default router
