import { createRouter, createWebHistory } from "vue-router";

import DashboardView from "../views/DashboardView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "dashboard",
      component: DashboardView,
    },
    {
      path: "/patients",
      name: "patients",
      component: () => import("../views/PatientsView.vue"),
    },
    {
      path: "/healthcare-units",
      name: "healthcare-units",
      component: () => import("../views/HealthcareUnitsView.vue"),
    },
  ],
});

export default router;
