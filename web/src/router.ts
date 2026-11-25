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
  // i link con #ancora (es. "Password dimenticata" nella pagina Account) portano alla card giusta
  scrollBehavior: (to, _from, saved) => saved ?? (to.hash ? { el: to.hash, behavior: 'smooth' } : { top: 0 }),
})
