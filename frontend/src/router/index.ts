import { createRouter, createWebHistory } from "vue-router";

import HomeView from "@/views/HomeView.vue";
import BeastsView from "@/views/BeastsView.vue";
import TravelersView from "@/views/TravelersView.vue";
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
    {
      path: "/travelers",
      name: "travelers",
      component: TravelersView,
    },
    {
      path: "/beasts",
      name: "beasts",
      component: BeastsView,
    },
  ],
});
