import {createApp} from 'vue'
import App from './App.vue'

import './assets/main.css'

import {createAuth0} from "@auth0/auth0-vue";
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
    .use(
        createAuth0({
            domain: import.meta.env.VITE_AUTH0_DOMAIN,
            clientId: import.meta.env.VITE_AUTH0_CLIENT_ID,
            authorizationParams: {
                redirect_uri: import.meta.env.VITE_AUTH0_CALLBACK_URL,
                scope: 'openid profile'
            },
        })
    )
    .mount('#app')
