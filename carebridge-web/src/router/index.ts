
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
      path: "/appointments",
      name: "appointments",
      component: () => import("../views/AppointmentsView.vue"),
    },
    {
      path: "/calendar",
      name: "calendar",
      component: () => import("../views/CalendarView.vue"),
    },
    {
      path: "/healthcare-units",
      name: "healthcare-units",
      component: () => import("../views/HealthcareUnitsView.vue"),
    },
  ],
});

export default router;
