
<script setup lang="ts">
import { keycloak } from "./auth/keycloak";

const authenticated = keycloak.authenticated === true;
const username =
  keycloak.tokenParsed?.preferred_username ?? "User";

async function login() {
  await keycloak.login({
    redirectUri: window.location.origin,
  });
}

async function logout() {
  await keycloak.logout({
    redirectUri: window.location.origin,
  });
}
</script>

<template>
  <div v-if="authenticated" class="app-layout">
    <aside class="sidebar">
      <div class="brand">
        <h2>CareBridge</h2>
        <p>Healthcare Platform</p>
      </div>

      <nav>
        <RouterLink to="/">Dashboard</RouterLink>
        <RouterLink to="/patients">Patients</RouterLink>
        <RouterLink to="/healthcare-units">
          Healthcare Units
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <p>Signed in as</p>
        <strong>{{ username }}</strong>
        <button @click="logout">Log out</button>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>

  <div v-else class="login-page">
    <h1>CareBridge</h1>
    <p>Digital healthcare management platform</p>
    <button @click="login">Sign in with Keycloak</button>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  font-family: Arial, sans-serif;
  background: #f8fafc;
}

.sidebar {
  width: 250px;
  flex-shrink: 0;
  background: #14243a;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 24px 18px;
  box-sizing: border-box;
}

.brand {
  margin-bottom: 40px;
}

.brand h2 {
  margin: 0;
}

.brand p {
  color: #94a3b8;
  font-size: 13px;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

nav a {
  color: #cbd5e1;
  text-decoration: none;
  padding: 14px;
  border-radius: 8px;
}

nav a:hover,
nav a.router-link-active {
  background: #2563eb;
  color: white;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 25px;
}

.sidebar-footer p {
  color: #94a3b8;
  font-size: 13px;
}

button {
  background: #2563eb;
  color: white;
  border: none;
  padding: 12px 18px;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 15px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: Arial, sans-serif;
}

@media (max-width: 650px) {
  .app-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }
}
</style>
