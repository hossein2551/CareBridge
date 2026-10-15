
<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from "vue";
import {
  searchPatientsPage,
  type Patient
} from "../api/patients";

const patients = ref<Patient[]>([]);
const search = ref("");
const loading = ref(false);
const error = ref("");
const selectedPatient = ref<Patient | null>(null);

const currentPage = ref(0);
const pageSize = 10;
const totalPages = ref(0);
const totalPatients = ref(0);

let requestId = 0;
let searchTimeout: ReturnType<typeof setTimeout> | undefined;

async function loadPatients(page = currentPage.value) {
  const id = ++requestId;

  loading.value = true;
  error.value = "";
  selectedPatient.value = null;

  try {
    const result = await searchPatientsPage(
      search.value.trim(),
      page,
      pageSize
    );

    // Ignore responses from outdated requests
    if (id !== requestId) return;

    patients.value = result.patients;
    currentPage.value = result.page;
    totalPages.value = result.totalPages;
    totalPatients.value = result.totalPatients;

  } catch (err) {
    if (id !== requestId) return;

    patients.value = [];
    totalPages.value = 0;
    totalPatients.value = 0;

    error.value =
      err instanceof Error
        ? err.message
        : "Failed to load patients";

  } finally {
    if (id === requestId) {
      loading.value = false;
    }
  }
}

async function changePage(newPage: number) {
  if (
    loading.value ||
    newPage < 0 ||
    newPage >= totalPages.value
  ) {
    return;
  }

  await loadPatients(newPage);
}

// Search the entire database with a short delay
watch(search, () => {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
  }

  // Invalidate older requests immediately
  requestId++;
  loading.value = true;
  error.value = "";
  selectedPatient.value = null;

  searchTimeout = setTimeout(() => {
    searchTimeout = undefined;
    void loadPatients(0);
  }, 350);
});

function refreshPatients() {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
    searchTimeout = undefined;
  }

  void loadPatients(currentPage.value);
}

onMounted(() => {
  void loadPatients(0);
});

onUnmounted(() => {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
  }

  requestId++;
});
</script>

<template>
  <div class="patients-page">
    <header class="page-header">
      <div>
        <h1>Patients</h1>
        <p>View and search registered patients.</p>
      </div>

      <button
        @click="refreshPatients"
        :disabled="loading"
      >
        {{ loading ? "Loading..." : "Refresh" }}
      </button>
    </header>

    <div class="table-card">
      <div class="toolbar">
        <div class="search-area">
          <input
            v-model="search"
            type="search"
            placeholder="Search patients by name..."
            aria-label="Search all registered patients"
          />

          <small>
            Search across all registered patients.
          </small>
        </div>

        <span>
          {{ totalPatients }}
          {{ search.trim() ? "matching patients" : "registered patients" }}
        </span>
      </div>

      <p
        v-if="error"
        class="error"
        role="alert"
      >
        {{ error }}
      </p>

      <p
        v-if="loading"
        class="loading-message"
      >
        Loading patients...
      </p>

      <table v-if="!loading && !error">
        <thead>
          <tr>
            <th>ID</th>
            <th>First name</th>
            <th>Last name</th>
            <th>Birth year</th>
            <th>Healthcare unit</th>
            <th>Details</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="patient in patients"
            :key="patient.id"
          >
            <td>{{ patient.id }}</td>
            <td>{{ patient.firstName }}</td>
            <td>{{ patient.lastName }}</td>
            <td>{{ patient.birthYear }}</td>

            <td>
              {{ patient.healthcareUnitId ?? "Not assigned" }}
            </td>

            <td>
              <button
                class="view-btn"
                @click="selectedPatient = patient"
              >
                View
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <p
        v-if="!loading && !error && patients.length === 0"
        class="empty-message"
      >
        No patients found.
      </p>

      <div v-if="!error" class="pagination">
        <span>
          Page {{ currentPage + 1 }}
          of {{ Math.max(totalPages, 1) }}
          ({{ totalPatients }} total patients)
        </span>

        <div class="pagination-buttons">
          <button
            :disabled="loading || currentPage === 0"
            @click="changePage(currentPage - 1)"
          >
            Previous
          </button>

          <button
            :disabled="
              loading ||
              currentPage + 1 >= totalPages
            "
            @click="changePage(currentPage + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>

    <section
      v-if="selectedPatient"
      class="detail-card"
    >
      <div class="detail-header">
        <h2>Patient details</h2>

        <button @click="selectedPatient = null">
          Close
        </button>
      </div>

      <p>
        <strong>ID:</strong>
        {{ selectedPatient.id }}
      </p>

      <p>
        <strong>First name:</strong>
        {{ selectedPatient.firstName }}
      </p>

      <p>
        <strong>Last name:</strong>
        {{ selectedPatient.lastName }}
      </p>

      <p>
        <strong>Birth year:</strong>
        {{ selectedPatient.birthYear }}
      </p>

      <p>
        <strong>Healthcare unit:</strong>
        {{ selectedPatient.healthcareUnitId ?? "Not assigned" }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.patients-page {
  padding: 32px;
  color: #172033;
  font-family: Arial, sans-serif;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

button {
  background: #2563eb;
  color: white;
  border: none;
  padding: 11px 18px;
  border-radius: 8px;
  cursor: pointer;
}

button:hover:not(:disabled) {
  background: #1d4ed8;
}

button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.table-card,
.detail-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  margin-top: 20px;
}

.table-card {
  overflow-x: auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.search-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-area small {
  color: #64748b;
}

input {
  padding: 12px;
  width: 320px;
  max-width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th,
td {
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

th {
  color: #475569;
  background: #f8fafc;
}

.view-btn {
  background: #e8efff;
  color: #1d4ed8;
}

.view-btn:hover:not(:disabled) {
  background: #dbeafe;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.pagination-buttons {
  display: flex;
  gap: 10px;
}

.error {
  color: #dc2626;
}

.loading-message,
.empty-message {
  color: #64748b;
  margin: 20px 0;
}

@media (max-width: 850px) {
  .patients-page {
    padding: 16px;
  }

  .page-header,
  .toolbar {
    flex-wrap: wrap;
  }

  .pagination {
    align-items: flex-start;
  }
}
</style>
