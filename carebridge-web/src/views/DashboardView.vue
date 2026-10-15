
<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getPatients } from "../api/patients";
import { getHealthcareUnits } from "../api/healthcareUnits";

const totalPatients = ref<number | null>(null);
const totalUnits = ref<number | null>(null);
const loading = ref(false);
const error = ref("");
const apiStatus = ref("Not checked");

async function loadDashboard() {
  loading.value = true;
  error.value = "";
  apiStatus.value = "Checking...";

  try {
    const [patients, units] = await Promise.all([
      getPatients(),
      getHealthcareUnits()
    ]);

    totalPatients.value = patients.length;
    totalUnits.value = units.length;
    apiStatus.value = "Connected";
  } catch (err) {
    totalPatients.value = null;
    totalUnits.value = null;
    apiStatus.value = "Connection failed";

    error.value = err instanceof Error
      ? err.message
      : "Failed to load dashboard";
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<template>
  <div class="dashboard">
    <header>
      <h1>Dashboard</h1>
      <p>Welcome to your CareBridge workspace.</p>
    </header>

    <section class="stats">
      <div class="stat-card">
        <span>Total Patients</span>
        <strong>{{ totalPatients ?? "—" }}</strong>
        <small>Registered patients</small>
      </div>

      <div class="stat-card">
        <span>Healthcare Units</span>
        <strong>{{ totalUnits ?? "—" }}</strong>
        <small>Registered healthcare units</small>
      </div>

      <div class="stat-card">
        <span>API Status</span>
        <strong class="status">{{ apiStatus }}</strong>
        <small>Backend API connection</small>
      </div>
    </section>

    <button @click="loadDashboard" :disabled="loading">
      {{ loading ? "Refreshing..." : "Refresh Dashboard" }}
    </button>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 32px;
  color: #172033;
  font-family: Arial, sans-serif;
}

header h1 {
  margin-bottom: 8px;
}

header p {
  color: #64748b;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin: 28px 0;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 24px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.stat-card span,
.stat-card small {
  color: #64748b;
}

.stat-card strong {
  font-size: 30px;
}

.stat-card .status {
  font-size: 22px;
}

button {
  padding: 12px 20px;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #dc2626;
}
</style>
