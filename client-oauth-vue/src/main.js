import {createApp} from 'vue'
import App from './App.vue'

import './assets/main.css'

import {createRouter, createWebHistory} from 'vue-router'
import Home from "./components/Home.vue";
import Callback from "./components/Callback.vue";


const router = createRouter({
    history: createWebHistory(),
    routes: [
        {path: '/', component: Home},
        {path: '/callback', component: Callback},
    ]
})

createApp(App)
    .use(router)
    .mount('#app')
