
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { keycloak } from "../auth/keycloak";
import { getPatientById, type Patient } from "../api/patients";

import {
  getAppointmentsByPatient,
  type Appointment,
  type AppointmentStatus,
} from "../api/appointments";
import {
  getHealthcareUnits,
  type HealthcareUnit,
} from "../api/healthcareUnits";

const route = useRoute();
const router = useRouter();
const canBook = computed(() =>
  ["ADMIN", "DOCTOR"].some((role) =>
    keycloak.hasRealmRole(role)
  )
);

function bookForPatient(): void {
  if (!canBook.value || !patient.value) return;

  void router.push({
    path: "/appointments",
    query: {
      patientId: String(patient.value.id),
    },
  });
}
const patient = ref<Patient | null>(null);
const appointments = ref<Appointment[]>([]);
const healthcareUnits = ref<HealthcareUnit[]>([]);

const loading = ref(true);
const error = ref("");
type HistoryFilter = "ALL" | "UPCOMING" | "COMPLETED" | "CANCELLED";

const historyFilter = ref<HistoryFilter>("ALL");
const patientId = computed(() => {
  const id = Number(route.params.id);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
});

const patientAppointments = computed(() => {
  if (!patient.value) return [];

  return appointments.value
    .filter((appointment) => appointment.patientId === patient.value?.id)
    .sort((a, b) =>
      b.appointmentTime.localeCompare(a.appointmentTime)
    );
});

const upcomingAppointments = computed(() =>
  patientAppointments.value.filter(
    (appointment) =>
      appointment.status === "SCHEDULED" &&
      new Date(appointment.appointmentTime).getTime() > Date.now()
  )
);
const filteredPatientAppointments = computed(() => {
  switch (historyFilter.value) {
    case "UPCOMING":
      return upcomingAppointments.value;

    case "COMPLETED":
      return patientAppointments.value.filter(
        (appointment) => appointment.status === "COMPLETED"
      );

    case "CANCELLED":
      return patientAppointments.value.filter(
        (appointment) => appointment.status === "CANCELLED"
      );

    default:
      return patientAppointments.value;
  }
});

function healthcareUnitName(unitId: number | null): string {
  if (unitId === null) return "Not assigned";

  const unit = healthcareUnits.value.find(
    (item) => item.id === unitId
  );

  return unit ? unit.name : `Unit ${unitId}`;
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("sv-SE", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function statusLabel(status: AppointmentStatus): string {
  switch (status) {
    case "SCHEDULED":
      return "Scheduled";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
  }
}

async function loadPatientDetails(): Promise<void> {
  if (patientId.value === null) {
    error.value = "Invalid patient ID.";
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    const [patientData, appointmentData, unitsData] =
  await Promise.all([
    getPatientById(patientId.value),
    getAppointmentsByPatient(patientId.value),
    getHealthcareUnits(),
  ]);

    patient.value = patientData;
    appointments.value = appointmentData;
    healthcareUnits.value = unitsData;
  } catch (err) {
    error.value =
      err instanceof Error
        ? err.message
        : "Failed to load patient details.";
  } finally {
    loading.value = false;
  }
}

function goBack(): void {
  void router.push("/patients");
}

onMounted(() => {
  void loadPatientDetails();
});
</script>

<template>
  <section class="patient-details-page">
    <header class="page-header">
      <div>
        <button class="back-btn" @click="goBack">
          ← Back to patients
        </button>

        <h1>Patient Details</h1>
        <p>Patient information and appointment history.</p>
      </div>

      <button
        class="refresh-btn"
        :disabled="loading"
        @click="loadPatientDetails"
      >
        Refresh
      </button>
    </header>

    <p v-if="loading">Loading patient information...</p>

    <p v-else-if="error" class="error" role="alert">
      {{ error }}
    </p>

    <template v-else-if="patient">
      <div class="patient-card">
        <div class="patient-avatar">
          {{ patient.firstName.charAt(0) }}
          {{ patient.lastName.charAt(0) }}
        </div>

        <div>
          <h2>{{ patient.firstName }} {{ patient.lastName }}</h2>
          <p>Patient ID: {{ patient.id }}</p>
        </div>
      </div>
<div v-if="canBook" class="patient-actions">
  <button
    type="button"
    class="book-patient-btn"
    @click="bookForPatient"
  >
    + Book appointment
  </button>
</div>
      <div class="info-grid">
        <div class="info-card">
          <span>Birth year</span>
          <strong>{{ patient.birthYear }}</strong>
        </div>

        <div class="info-card">
          <span>Healthcare unit</span>
          <strong>
            {{ healthcareUnitName(patient.healthcareUnitId) }}
          </strong>
        </div>

        <div class="info-card">
          <span>Total appointments</span>
          <strong>{{ patientAppointments.length }}</strong>
        </div>

        <div class="info-card">
          <span>Upcoming appointments</span>
          <strong>{{ upcomingAppointments.length }}</strong>
        </div>
      </div>

      <div class="history-card">
        <h2>Appointment History</h2>
        <div class="history-filter">
  <label for="history-status">Show appointments</label>

  <select id="history-status" v-model="historyFilter">
    <option value="ALL">All appointments</option>
    <option value="UPCOMING">Upcoming appointments</option>
    <option value="COMPLETED">Completed appointments</option>
    <option value="CANCELLED">Cancelled appointments</option>
  </select>
</div>
<p v-if="filteredPatientAppointments.length === 0">
  No appointments found for this patient.

        </p>

        <div v-else class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Date and time</th>
                <th>Healthcare unit</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              <tr
v-for="appointment in filteredPatientAppointments"                :key="appointment.id"
              >
                <td>
                  {{ formatDateTime(appointment.appointmentTime) }}
                </td>

                <td>
                  {{ healthcareUnitName(appointment.healthcareUnitId) }}
                </td>

                <td>
                  <span
                    class="status"
                    :class="appointment.status.toLowerCase()"
                  >
                    {{ statusLabel(appointment.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.patient-details-page {
  padding: 28px;
  color: #0f172a;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 12px 0 8px;
}

.page-header p,
.patient-card p {
  color: #64748b;
}

button {
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn {
  background: #e2e8f0;
  color: #334155;
}

.refresh-btn {
  background: #2563eb;
  color: white;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.patient-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.patient-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 50%;
  width: 64px;
  height: 64px;
  font-size: 24px;
  font-weight: 700;
}

.patient-card h2 {
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.info-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
}

.info-card span {
  color: #64748b;
  font-size: 13px;
}

.info-card strong {
  font-size: 20px;
  overflow-wrap: anywhere;
}

.history-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
}

.history-card h2 {
  margin-top: 0;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

th {
  color: #64748b;
  font-size: 13px;
}

.status {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.scheduled {
  background: #dbeafe;
  color: #1d4ed8;
}

.completed {
  background: #dcfce7;
  color: #166534;
}

.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.error {
  color: #b91c1c;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 650px) {
  .patient-details-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}.history-filter {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 280px;
  margin-bottom: 20px;
}

.history-filter label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.history-filter select {
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
}
.patient-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.book-patient-btn {
  background: #2563eb;
  color: white;
  font-weight: 600;
}
</style>
