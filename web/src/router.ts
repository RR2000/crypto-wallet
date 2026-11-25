import { createRouter, createWebHistory } from 'vue-router'
import AccountPage from './pages/AccountPage.vue'
import CallbackPage from './pages/CallbackPage.vue'
import HomePage from './pages/HomePage.vue'
import WalletPage from './pages/WalletPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/wallet', component: WalletPage, meta: { auth: true } },
    { path: '/account', component: AccountPage, meta: { auth: true } },
    { path: '/callback', component: CallbackPage },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
