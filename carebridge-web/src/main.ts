
import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { initializeAuth } from "./auth/keycloak";

async function startApp() {
  try {
    await initializeAuth();

    const app = createApp(App);

    app.use(router);

    await router.isReady();

    app.mount("#app");
  } catch (error) {
    console.error("Application startup failed:", error);

    const element = document.querySelector("#app");

    if (element) {
      element.textContent =
        "Unable to start CareBridge. Please try again.";
    }
  }
}

startApp();
