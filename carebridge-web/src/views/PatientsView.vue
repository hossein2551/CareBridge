
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import {
filterPatientsPage,
  createPatient,
  updatePatient,
  deletePatient,
  type Patient,
  type PatientRequest,
} from "../api/patients";
import {
  canManagePatients,
  canDeletePatients,
} from "../auth/permissions";
import {
  getHealthcareUnits,
  type HealthcareUnit,
} from "../api/healthcareUnits";
const patients = ref<Patient[]>([]);
const healthcareUnits = ref<HealthcareUnit[]>([]);
const unitsLoading = ref(false);
const unitsError = ref("");
const search = ref("");
const selectedHealthcareUnitId = ref<number | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const formError = ref("");
const selectedPatient = ref<Patient | null>(null);

const currentPage = ref(0);
const pageSize = 10;
const totalPages = ref(0);
const totalPatients = ref(0);

const canManage = canManagePatients();
const canDelete = canDeletePatients();

const showForm = ref(false);
const editingId = ref<number | null>(null);

const form = ref<PatientRequest>({
  firstName: "",
  lastName: "",
  birthYear: new Date().getFullYear(),
  healthcareUnitId: null,
});

let requestId = 0;
let searchTimeout: ReturnType<typeof setTimeout> | undefined;

async function loadPatients(page = currentPage.value) {
  const id = ++requestId;

  loading.value = true;
  error.value = "";

  try {
    const result = await filterPatientsPage(
  search.value.trim(),
  selectedHealthcareUnitId.value,
  page,
  pageSize
);

    if (id !== requestId) return;

    patients.value = result.patients;
    currentPage.value = result.page;
    totalPages.value = result.totalPages;
    totalPatients.value = result.totalPatients;

    selectedPatient.value = null;
  } catch (err) {
    if (id !== requestId) return;

    patients.value = [];
    totalPages.value = 0;
    totalPatients.value = 0;
    selectedPatient.value = null;

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

watch(search, () => {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
  }

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

  void loadPatients(search.value.trim() ? 0 : currentPage.value);
}
async function loadHealthcareUnits() {
  unitsLoading.value = true;
  unitsError.value = "";

  try {
    healthcareUnits.value = await getHealthcareUnits();
  } catch (err) {
    healthcareUnits.value = [];
    unitsError.value =
      err instanceof Error
        ? err.message
        : "Failed to load healthcare units.";
  } finally {
    unitsLoading.value = false;
  }
}

function openCreateForm() {
  if (!canManage) return;

  editingId.value = null;
  formError.value = "";

  form.value = {
    firstName: "",
    lastName: "",
    birthYear: new Date().getFullYear(),
    healthcareUnitId: null,
  };

  showForm.value = true;
}

function openEditForm(patient: Patient) {
  if (!canManage) return;

  editingId.value = patient.id;
  formError.value = "";

  form.value = {
    firstName: patient.firstName,
    lastName: patient.lastName,
    birthYear: patient.birthYear,
    healthcareUnitId: patient.healthcareUnitId,
  };

  selectedPatient.value = null;
  showForm.value = true;
}

function closeForm() {
  if (saving.value) return;

  showForm.value = false;
  editingId.value = null;
  formError.value = "";
}

async function savePatient() {
  if (!canManage || saving.value) return;

  formError.value = "";

  const request: PatientRequest = {
    firstName: form.value.firstName.trim(),
    lastName: form.value.lastName.trim(),
    birthYear: Number(form.value.birthYear),
    healthcareUnitId:
      form.value.healthcareUnitId === null ||
      form.value.healthcareUnitId === undefined ||
      String(form.value.healthcareUnitId).trim() === ""
        ? null
        : Number(form.value.healthcareUnitId),
  };

  if (!request.firstName || !request.lastName) {
    formError.value = "First name and last name are required.";
    return;
  }

  const year = new Date().getFullYear();

  if (
    !Number.isInteger(request.birthYear) ||
    request.birthYear < 1900 ||
    request.birthYear > year
  ) {
    formError.value = "Enter a valid birth year.";
    return;
  }

  if (
    request.healthcareUnitId !== null &&
    (!Number.isInteger(request.healthcareUnitId) ||
      request.healthcareUnitId < 1)
  ) {
    formError.value = "Healthcare unit ID must be a positive number.";
    return;
  }

  saving.value = true;

  try {
    if (editingId.value === null) {
      await createPatient(request);
    } else {
      await updatePatient(editingId.value, request);
    }

    showForm.value = false;
    editingId.value = null;
    await loadPatients(currentPage.value);
  } catch (err) {
    formError.value =
      err instanceof Error
        ? err.message
        : "Failed to save patient.";
  } finally {
    saving.value = false;
  }
}

async function removePatient(patient: Patient) {
  if (!canDelete || saving.value) return;

  const confirmed = window.confirm(
    `Delete ${patient.firstName} ${patient.lastName}? This cannot be undone.`
  );

  if (!confirmed) return;

  saving.value = true;
  error.value = "";

  try {
    await deletePatient(patient.id);
    selectedPatient.value = null;

    // Reload the same page, or return to the previous page
    // if the current page becomes empty.
    const nextPage =
      patients.value.length === 1 && currentPage.value > 0
        ? currentPage.value - 1
        : currentPage.value;

    await loadPatients(nextPage);
  } catch (err) {
    error.value =
      err instanceof Error
        ? err.message
        : "Failed to delete patient.";
  } finally {
    saving.value = false;
  }
}

function getHealthcareUnitName(unitId: number | null): string {
  if (unitId === null) {
    return "Not assigned";
  }

  const unit = healthcareUnits.value.find(
    (item) => item.id === unitId
  );

  return unit ? unit.name : `Unit ${unitId}`;
}

onUnmounted(() => {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
  }

  requestId++;
});
onMounted(() => {
  void loadPatients(0);
  void loadHealthcareUnits();
});

watch(selectedHealthcareUnitId, () => {
  if (searchTimeout !== undefined) {
    clearTimeout(searchTimeout);
    searchTimeout = undefined;
  }

  requestId++;
  void loadPatients(0);
});


</script>

<template>
  <div class="patients-page">
    <header class="page-header">
      <div>
        <h1>Patients</h1>
        <p>View and manage registered patients.</p>
      </div>

      <div class="header-actions">
        <button
          v-if="canManage"
          @click="openCreateForm"
          :disabled="saving"
        >
          + Add Patient
        </button>

        <button
          class="secondary-btn"
          @click="refreshPatients"
          :disabled="loading || saving"
        >
          {{ loading ? "Loading..." : "Refresh" }}
        </button>
      </div>
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

    <small>Search across all registered patients.</small>
  </div>

  <div class="filter-area">
    <label for="unit-filter">Filter by healthcare unit</label>

    <select
      id="unit-filter"
      v-model="selectedHealthcareUnitId"
      :disabled="unitsLoading || !!unitsError"
    >
      <option :value="null">All healthcare units</option>

      <option
        v-for="unit in healthcareUnits"
        :key="unit.id"
        :value="unit.id"
      >
        {{ unit.name }} – {{ unit.city }}
      </option>
    </select>

    <small v-if="unitsError" class="error">
      {{ unitsError }}
    </small>
  </div>

  <span>
    {{ totalPatients }}
    {{ search.trim() ? "matching patients" : "registered patients" }}
  </span>
</div>
    

      <p v-if="error" class="error" role="alert">
        {{ error }}
      </p>

      <p v-if="loading" class="loading-message">
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
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="patient in patients" :key="patient.id">
            <td>{{ patient.id }}</td>
            
<td>
  <RouterLink
    :to="`/patients/${patient.id}`"
    class="patient-link"
  >
    {{ patient.firstName }}
  </RouterLink>
</td>

<td>
  <RouterLink
    :to="`/patients/${patient.id}`"
    class="patient-link"
  >
    {{ patient.lastName }}
  </RouterLink>
</td>

            <td>{{ patient.birthYear }}</td>
<td>{{ getHealthcareUnitName(patient.healthcareUnitId) }}</td>
            <td>
              <div class="table-actions">
                <button
                  class="view-btn"
                  @click="selectedPatient = patient"
                >
                  View
                </button>

                <button
                  v-if="canManage"
                  class="edit-btn"
                  @click="openEditForm(patient)"
                  :disabled="saving"
                >
                  Edit
                </button>

                <button
                  v-if="canDelete"
                  class="delete-btn"
                  @click="removePatient(patient)"
                  :disabled="saving"
                >
                  Delete
                </button>
              </div>
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
            :disabled="loading || currentPage + 1 >= totalPages"
            @click="changePage(currentPage + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>

    <!-- Create / edit form -->
    <section v-if="showForm" class="detail-card">
      <div class="detail-header">
        <h2>
          {{ editingId === null ? "Add Patient" : "Edit Patient" }}
        </h2>

        <button
          class="secondary-btn"
          @click="closeForm"
          :disabled="saving"
        >
          Cancel
        </button>
      </div>

      <form class="patient-form" @submit.prevent="savePatient">
        <label>
          First name
          <input
            v-model="form.firstName"
            required
            maxlength="100"
          />
        </label>

        <label>
          Last name
          <input
            v-model="form.lastName"
            required
            maxlength="100"
          />
        </label>

        <label>
          Birth year
          <input
            v-model.number="form.birthYear"
            type="number"
            min="1900"
            :max="new Date().getFullYear()"
            required
          />
        </label>

        <label>
  Healthcare unit
  <select
    v-model="form.healthcareUnitId"
    :disabled="unitsLoading || !!unitsError"
  >
    <option :value="null">Not assigned</option>

    <option
      v-for="unit in healthcareUnits"
      :key="unit.id"
      :value="unit.id"
    >
      {{ unit.name }} – {{ unit.city }}
    </option>
  </select>

  <small v-if="unitsLoading">
    Loading healthcare units...
  </small>

  <small v-if="unitsError" class="error">
    {{ unitsError }}
  </small>
</label>

        <p v-if="formError" class="error" role="alert">
          {{ formError }}
        </p>

        <div class="form-actions">
          <button type="submit" :disabled="saving">
            {{ saving ? "Saving..." : "Save Patient" }}
          </button>
        </div>
      </form>
    </section>

    <!-- Patient details -->
    <section v-if="selectedPatient" class="detail-card">
      <div class="detail-header">
        <h2>Patient details</h2>

        <button
          class="secondary-btn"
          @click="selectedPatient = null"
        >
          Close
        </button>
      </div>

      <p><strong>ID:</strong> {{ selectedPatient.id }}</p>
      <p><strong>First name:</strong> {{ selectedPatient.firstName }}</p>
      <p><strong>Last name:</strong> {{ selectedPatient.lastName }}</p>
      <p><strong>Birth year:</strong> {{ selectedPatient.birthYear }}</p>
      <p>
        <strong>Healthcare unit:</strong>
{{ getHealthcareUnitName(selectedPatient.healthcareUnitId) }}      </p>
    </section>
  </div>
</template>
select {
  padding: 12px;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  color: #172033;
}

.patient-form small {
  font-size: 12px;
  font-weight: normal;
  color: #64748b;
}

.patient-form small.error {
  color: #dc2626;
}
<style scoped>
.patients-page {
  padding: 32px;
  color: #172033;
  font-family: Arial, sans-serif;
}

.page-header,
.header-actions,
.toolbar,
.detail-header,
.pagination,
.pagination-buttons,
.table-actions,
.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header,
.toolbar,
.detail-header,
.pagination {
  justify-content: space-between;
}

.page-header {
  margin-bottom: 28px;
  flex-wrap: wrap;
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

.secondary-btn,
.view-btn {
  background: #e8efff;
  color: #1d4ed8;
}

.secondary-btn:hover:not(:disabled),
.view-btn:hover:not(:disabled) {
  background: #dbeafe;
}

.edit-btn {
  background: #0f766e;
}

.edit-btn:hover:not(:disabled) {
  background: #115e59;
}

.delete-btn {
  background: #dc2626;
}

.delete-btn:hover:not(:disabled) {
  background: #b91c1c;
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
  box-sizing: border-box;
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

.table-actions {
  flex-wrap: wrap;
}

.patient-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.patient-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 600;
}

.patient-form input {
  width: 100%;
}

.patient-form .error,
.form-actions {
  grid-column: 1 / -1;
}

.pagination {
  margin-top: 24px;
  flex-wrap: wrap;
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

  .patient-form {
    grid-template-columns: 1fr;
  }

  .pagination {
    align-items: flex-start;
  }
}
.patient-link {
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
}

.patient-link:hover {
  text-decoration: underline;
}

.patient-link:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 3px;
  border-radius: 3px;
}
</style>
