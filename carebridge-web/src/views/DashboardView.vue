
<script setup lang="ts">
import { onMounted, ref } from "vue";

import { getPatientsPage } from "../api/patients";
import { getHealthcareUnits } from "../api/healthcareUnits";
import {
  getAppointments,
  type Appointment,
} from "../api/appointments";

// Dashboard statistics
const totalPatients = ref<number | null>(null);
const totalUnits = ref<number | null>(null);
const totalAppointments = ref<number | null>(null);

const scheduledAppointments = ref<number | null>(null);
const completedAppointments = ref<number | null>(null);
const cancelledAppointments = ref<number | null>(null);

// Loading and status
const loading = ref(false);
const error = ref("");
const apiStatus = ref("Not checked");

// Count appointments by status
function countAppointments(
  appointments: Appointment[],
  status: Appointment["status"]
): number {
  return appointments.filter(
    (appointment) => appointment.status === status
  ).length;
}

// Load dashboard statistics
async function loadDashboard(): Promise<void> {
  if (loading.value) {
    return;
  }

  loading.value = true;
  error.value = "";
  apiStatus.value = "Checking...";

  try {
    const [patientPage, units, appointments] =
      await Promise.all([
        getPatientsPage(0, 1),
        getHealthcareUnits(),
        getAppointments(),
      ]);

    totalPatients.value = patientPage.totalPatients;
    totalUnits.value = units.length;

    totalAppointments.value = appointments.length;

    scheduledAppointments.value = countAppointments(
      appointments,
      "SCHEDULED"
    );

    completedAppointments.value = countAppointments(
      appointments,
      "COMPLETED"
    );

    cancelledAppointments.value = countAppointments(
      appointments,
      "CANCELLED"
    );

    apiStatus.value = "Connected";
  } catch (err) {
    totalPatients.value = null;
    totalUnits.value = null;
    totalAppointments.value = null;

    scheduledAppointments.value = null;
    completedAppointments.value = null;
    cancelledAppointments.value = null;

    apiStatus.value = "Connection failed";

    error.value =
      err instanceof Error
        ? err.message
        : "Failed to load dashboard.";
  } finally {
    loading.value = false;
  }
}

// Load dashboard when the page opens
onMounted(() => {
  void loadDashboard();
});
</script>

<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <div>
        <h1>Dashboard</h1>
        <p>
          Welcome to your CareBridge workspace.
          Here is an overview of your healthcare operations.
        </p>
      </div>

      <button
        class="refresh-btn"
        :disabled="loading"
        @click="loadDashboard"
      >
        {{ loading ? "Refreshing..." : "Refresh Dashboard" }}
      </button>
    </header>

    <!-- API connection status -->
    <section class="connection-section">
      <div class="connection-info">
        <span
          class="connection-dot"
          :class="{
            connected: apiStatus === 'Connected',
            disconnected: apiStatus === 'Connection failed'
          }"
        ></span>

        <div>
          <strong>Backend API</strong>
          <p>{{ apiStatus }}</p>
        </div>
      </div>
    </section>

    <!-- General statistics -->
    <section class="dashboard-section">
      <h2>General overview</h2>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon patients-icon">
            PT
          </div>

          <span class="stat-label">
            Total Patients
          </span>

          <strong class="stat-value">
            {{ totalPatients ?? "—" }}
          </strong>

          <small>Registered patients</small>
        </div>

        <div class="stat-card">
          <div class="stat-icon units-icon">
            HC
          </div>

          <span class="stat-label">
            Healthcare Units
          </span>

          <strong class="stat-value">
            {{ totalUnits ?? "—" }}
          </strong>

          <small>Registered healthcare units</small>
        </div>

        <div class="stat-card">
          <div class="stat-icon appointments-icon">
            AP
          </div>

          <span class="stat-label">
            Total Appointments
          </span>

          <strong class="stat-value">
            {{ totalAppointments ?? "—" }}
          </strong>

          <small>All registered appointments</small>
        </div>
      </div>
    </section>

    <!-- Appointment statistics -->
    <section class="dashboard-section">
      <h2>Appointment overview</h2>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon scheduled-icon">
            S
          </div>

          <span class="stat-label">
            Scheduled
          </span>

          <strong class="stat-value">
            {{ scheduledAppointments ?? "—" }}
          </strong>

          <small>Upcoming or pending appointments</small>

          <span class="status-indicator scheduled">
            Scheduled
          </span>
        </div>

        <div class="stat-card">
          <div class="stat-icon completed-icon">
            C
          </div>

          <span class="stat-label">
            Completed
          </span>

          <strong class="stat-value">
            {{ completedAppointments ?? "—" }}
          </strong>

          <small>Completed appointments</small>

          <span class="status-indicator completed">
            Completed
          </span>
        </div>

        <div class="stat-card">
          <div class="stat-icon cancelled-icon">
            X
          </div>

          <span class="stat-label">
            Cancelled
          </span>

          <strong class="stat-value">
            {{ cancelledAppointments ?? "—" }}
          </strong>

          <small>Cancelled appointments</small>

          <span class="status-indicator cancelled">
            Cancelled
          </span>
        </div>
      </div>
    </section>

    <!-- Error message -->
    <p
      v-if="error"
      class="error-message"
      role="alert"
    >
      {{ error }}
    </p>

    <p class="dashboard-footer">
      Statistics are loaded from the CareBridge backend.
      Use Refresh Dashboard to update the information.
    </p>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 32px;
  color: #172033;
  font-family: Arial, sans-serif;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}

.dashboard-header h1 {
  font-size: 30px;
  margin: 0 0 10px;
}

.dashboard-header p {
  color: #64748b;
  margin: 0;
  line-height: 1.6;
  max-width: 620px;
}

.refresh-btn {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 18px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* API connection */
.connection-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 22px;
  margin-bottom: 30px;
}

.connection-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.connection-info strong {
  font-size: 14px;
}

.connection-info p {
  color: #64748b;
  font-size: 13px;
  margin: 5px 0 0;
}

.connection-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.connection-dot.connected {
  background: #16a34a;
}

.connection-dot.disconnected {
  background: #dc2626;
}

/* Statistics */
.dashboard-section {
  margin-bottom: 32px;
}

.dashboard-section h2 {
  font-size: 19px;
  color: #1e293b;
  margin: 0 0 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 24px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  min-width: 0;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.patients-icon {
  color: #2563eb;
  background: #dbeafe;
}

.units-icon {
  color: #7c3aed;
  background: #ede9fe;
}

.appointments-icon {
  color: #0891b2;
  background: #cffafe;
}

.scheduled-icon {
  color: #1d4ed8;
  background: #dbeafe;
}

.completed-icon {
  color: #15803d;
  background: #dcfce7;
}

.cancelled-icon {
  color: #b91c1c;
  background: #fee2e2;
}

.stat-label {
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  color: #0f172a;
}

.stat-card small {
  color: #94a3b8;
  font-size: 12px;
}

.status-indicator {
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
}

.status-indicator.scheduled {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-indicator.completed {
  background: #dcfce7;
  color: #166534;
}

.status-indicator.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.error-message {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 14px;
  overflow-wrap: anywhere;
}

.dashboard-footer {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 20px;
}

/* Responsive design */
@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 650px) {
  .dashboard {
    padding: 16px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
