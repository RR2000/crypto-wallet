import HomePage from "../App.vue";
import { createRouter, createWebHistory } from "vue-router";

const CallbackPage = () => import("../pages/callback-page.vue");

const routes = [
    {
        path: "/",
        name: "home",
        component: HomePage,
    },
    {
        path: "/callback",
        name: "callback",
        component: CallbackPage,
    },

    {
        path: "/:catchAll(.*)",
        name: "Not Found",
        component: HomePage,
    },
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

export default router;