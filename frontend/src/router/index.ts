import { createRouter, createWebHistory } from "vue-router";

import HomeView from "@/views/HomeView.vue";
import WagonsView from "@/views/WagonsView.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/wagons",
      name: "wagons",
      component: WagonsView,
    },
  ],
});
