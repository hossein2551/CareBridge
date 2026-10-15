
<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import {
  getHealthcareUnits,
  type HealthcareUnit
} from "../api/healthcareUnits";
import { getPatients, type Patient } from "../api/patients";

const units = ref<HealthcareUnit[]>([]);
const patients = ref<Patient[]>([]);
const search = ref("");
const loading = ref(false);
const error = ref("");
const selectedUnit = ref<HealthcareUnit | null>(null);
const selectedPatients = ref<Patient[]>([]);

const filteredUnits = computed(() => {
  const query = search.value.trim().toLowerCase();

  return units.value.filter(unit =>
    unit.name.toLowerCase().includes(query) ||
    unit.city.toLowerCase().includes(query)
  );
});

async function loadUnits() {
  loading.value = true;
  error.value = "";

  try {
    units.value = await getHealthcareUnits();
  } catch (err) {
    error.value =
      err instanceof Error ? err.message : "Failed to load units";
  } finally {
    loading.value = false;
  }
}

async function viewPatients(unit: HealthcareUnit) {
  error.value = "";

  try {
    patients.value = await getPatients();
    selectedPatients.value = patients.value.filter(
      patient => patient.healthcareUnitId === unit.id
    );
    selectedUnit.value = unit;
  } catch (err) {
    error.value =
      err instanceof Error ? err.message : "Failed to load patients";
  }
}

onMounted(loadUnits);
</script>

<template>
  <div class="page">
    <header class="header">
      <div>
        <h1>Healthcare Units</h1>
        <p>Manage and explore registered healthcare units.</p>
      </div>
      <button @click="loadUnits" :disabled="loading">
        {{ loading ? "Loading..." : "Refresh" }}
      </button>
    </header>

    <div class="card">
      <div class="toolbar">
        <input
          v-model="search"
          type="search"
          placeholder="Search by name or city..."
        />
        <span>{{ filteredUnits.length }} units</span>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <table v-if="!loading">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>City</th>
            <th>Organization code</th>
            <th>Patients</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="unit in filteredUnits" :key="unit.id">
            <td>{{ unit.id }}</td>
            <td>{{ unit.name }}</td>
            <td>{{ unit.city }}</td>
            <td>{{ unit.organizationCode }}</td>
            <td>
              <button class="view" @click="viewPatients(unit)">
                View patients
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <p v-if="!loading && !error && filteredUnits.length === 0">
        No healthcare units found.
      </p>
    </div>

    <section v-if="selectedUnit" class="card">
      <div class="header">
        <h2>{{ selectedUnit.name }}</h2>
        <button @click="selectedUnit = null">Close</button>
      </div>

      <p>City: {{ selectedUnit.city }}</p>
      <p>Organization code: {{ selectedUnit.organizationCode }}</p>

      <h3>Assigned patients</h3>

      <ul v-if="selectedPatients.length">
        <li v-for="patient in selectedPatients" :key="patient.id">
          {{ patient.firstName }} {{ patient.lastName }}
        </li>
      </ul>

      <p v-else>No patients assigned to this unit.</p>
    </section>
  </div>
</template>

<style scoped>
.page {
  padding: 32px;
  font-family: Arial, sans-serif;
  color: #172033;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.header h1 { margin-bottom: 8px; }
.header p { color: #64748b; }

.card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  overflow-x: auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

input {
  width: 320px;
  max-width: 100%;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th, td {
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

th { background: #f8fafc; }

button {
  background: #2563eb;
  color: white;
  border: none;
  padding: 11px 18px;
  border-radius: 8px;
  cursor: pointer;
}

.view {
  background: #e8efff;
  color: #1d4ed8;
}

.error { color: #dc2626; }

@media (max-width: 850px) {
  .page { padding: 16px; }
  .header, .toolbar { flex-wrap: wrap; }
}
</style>
