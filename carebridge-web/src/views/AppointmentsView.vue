
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { keycloak } from "../auth/keycloak";
import {
  createAppointment,
  getAppointments,
  updateAppointmentStatus,
  type Appointment,
  type AppointmentStatus,
} from "../api/appointments";

import {
  getPatients,
  type Patient,
} from "../api/patients";

import {
  getHealthcareUnits,
  type HealthcareUnit,
} from "../api/healthcareUnits";

// Data
const appointments = ref<Appointment[]>([]);
const patients = ref<Patient[]>([]);
const healthcareUnits = ref<HealthcareUnit[]>([]);

// Loading and errors
const loading = ref(false);
const error = ref("");

const booking = ref(false);
const bookingError = ref("");
const showBookingForm = ref(false);

// Permissions
const canBook = computed(() =>
  ["ADMIN", "DOCTOR"].some((role) =>
    keycloak.hasRealmRole(role)
  )
);

// Booking form
const bookingForm = ref({
  patientId: null as number | null,
  healthcareUnitId: null as number | null,
  appointmentTime: "",
});

// Get patient name
function getPatientName(patientId: number): string {
  const patient = patients.value.find(
    (item) => item.id === patientId
  );

  return patient
    ? `${patient.firstName} ${patient.lastName}`
    : `Patient ${patientId}`;
}

// Get healthcare unit name
function getHealthcareUnitName(unitId: number): string {
  const unit = healthcareUnits.value.find(
    (item) => item.id === unitId
  );

  return unit ? unit.name : `Unit ${unitId}`;
}

// Format date and time
function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("sv-SE", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

// Appointment status label
function getStatusLabel(status: AppointmentStatus): string {
  switch (status) {
    case "SCHEDULED":
      return "Scheduled";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
  }
}

// Load appointments, patients and healthcare units
async function loadAppointments(): Promise<void> {
  loading.value = true;
  error.value = "";

  try {
    const [appointmentsData, patientsData, unitsData] =
      await Promise.all([
        getAppointments(),
        getPatients(),
        getHealthcareUnits(),
      ]);

    appointments.value = appointmentsData;
    patients.value = patientsData;
    healthcareUnits.value = unitsData;
  } catch (err) {
    error.value =
      err instanceof Error
        ? err.message
        : "Failed to load appointments.";
  } finally {
    loading.value = false;
  }
}

// Open booking form
function openBookingForm(): void {
  bookingError.value = "";

  bookingForm.value = {
    patientId: null,
    healthcareUnitId: null,
    appointmentTime: "",
  };

  showBookingForm.value = true;
}

// Close booking form
function closeBookingForm(): void {
  if (booking.value) {
    return;
  }

  showBookingForm.value = false;
  bookingError.value = "";
}

// Create a new appointment
async function bookAppointment(): Promise<void> {
  if (!canBook.value || booking.value) {
    return;
  }

  bookingError.value = "";

  const { patientId, healthcareUnitId, appointmentTime } =
    bookingForm.value;

  if (
    patientId === null ||
    healthcareUnitId === null ||
    !appointmentTime
  ) {
    bookingError.value = "Please fill in all fields.";
    return;
  }

  const selectedDate = new Date(appointmentTime);

  if (
    Number.isNaN(selectedDate.getTime()) ||
    selectedDate.getTime() <= Date.now()
  ) {
    bookingError.value = "Appointment must be in the future.";
    return;
  }

  booking.value = true;

  try {
    await createAppointment({
      patientId,
      healthcareUnitId,
      appointmentTime: `${appointmentTime}:00`,
    });

    showBookingForm.value = false;

    bookingForm.value = {
      patientId: null,
      healthcareUnitId: null,
      appointmentTime: "",
    };

    await loadAppointments();
  } catch (err) {
    bookingError.value =
      err instanceof Error
        ? err.message
        : "Failed to book appointment.";
  } finally {
    booking.value = false;
  }
}

const updatingAppointmentId = ref<number | null>(null);
const statusError = ref("");

async function changeAppointmentStatus(
  appointment: Appointment,
  status: "COMPLETED" | "CANCELLED"
): Promise<void> {
  if (
    !canBook.value ||
    appointment.status !== "SCHEDULED" ||
    updatingAppointmentId.value !== null
  ) {
    return;
  }

  const action =
    status === "COMPLETED" ? "complete" : "cancel";

  if (
    !window.confirm(
      `Are you sure you want to ${action} this appointment?`
    )
  ) {
    return;
  }

  updatingAppointmentId.value = appointment.id;
  statusError.value = "";

  try {
    await updateAppointmentStatus(appointment.id, status);
    await loadAppointments();
  } catch (err) {
    statusError.value =
      err instanceof Error
        ? err.message
        : "Failed to update appointment.";
  } finally {
    updatingAppointmentId.value = null;
  }
}

// Load data when page opens
onMounted(() => {
  void loadAppointments();
});
</script>

<template>
  <section class="appointments-page">
    <header class="page-header">
      <div>
        <h1>Appointments</h1>
        <p>Manage patient appointments and healthcare visits.</p>
      </div>

      <div class="header-actions">
        <button
          v-if="canBook"
          class="book-btn"
          :disabled="booking || loading"
          @click="openBookingForm"
        >
          + Book appointment
        </button>

        <button
          class="refresh-btn"
          :disabled="loading"
          @click="loadAppointments"
        >
          {{ loading ? "Loading..." : "Refresh" }}
        </button>
      </div>
    </header>

    <!-- Booking form -->
    <div
      v-if="showBookingForm && canBook"
      class="booking-card"
    >
      <div class="booking-header">
        <h2>Book new appointment</h2>

        <button
          type="button"
          class="close-btn"
          :disabled="booking"
          @click="closeBookingForm"
        >
          Cancel
        </button>
      </div>

      <form @submit.prevent="bookAppointment">
        <div class="form-grid">
          <div class="form-field">
            <label for="appointment-patient">
              Patient
            </label>

            <select
              id="appointment-patient"
              v-model="bookingForm.patientId"
              required
              :disabled="booking"
            >
              <option :value="null" disabled>
                Select patient
              </option>

              <option
                v-for="patient in patients"
                :key="patient.id"
                :value="patient.id"
              >
                {{ patient.firstName }}
                {{ patient.lastName }}
                (ID {{ patient.id }})
              </option>
            </select>
          </div>

          <div class="form-field">
            <label for="appointment-unit">
              Healthcare unit
            </label>

            <select
              id="appointment-unit"
              v-model="bookingForm.healthcareUnitId"
              required
              :disabled="booking"
            >
              <option :value="null" disabled>
                Select healthcare unit
              </option>

              <option
                v-for="unit in healthcareUnits"
                :key="unit.id"
                :value="unit.id"
              >
                {{ unit.name }} – {{ unit.city }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label for="appointment-time">
              Date and time
            </label>

            <input
              id="appointment-time"
              v-model="bookingForm.appointmentTime"
              type="datetime-local"
              required
              :disabled="booking"
            />
          </div>
        </div>

        <p
          v-if="bookingError"
          class="error"
          role="alert"
        >
          {{ bookingError }}
        </p>

        <div class="form-actions">
          <button
            type="button"
            class="cancel-btn"
            :disabled="booking"
            @click="closeBookingForm"
          >
            Cancel
          </button>

          <button
            type="submit"
            class="save-btn"
            :disabled="booking"
          >
            {{
              booking
                ? "Booking..."
                : "Save appointment"
            }}
          </button>
        </div>
      </form>
    </div>

    <!-- Status update errors -->
    <p v-if="statusError" class="error" role="alert">
      {{ statusError }}
    </p>

    <!-- General errors -->
    <p
      v-if="error"
      class="error"
      role="alert"
    >
      {{ error }}
    </p>

    <!-- Appointment table -->
    <div class="table-card">
      <p v-if="loading">
        Loading appointments...
      </p>

      <table v-else-if="!error">
        <thead>
          <tr>
            <th>ID</th>
            <th>Patient</th>
            <th>Healthcare unit</th>
            <th>Date and time</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="appointment in appointments"
            :key="appointment.id"
          >
            <td>
              {{ appointment.id }}
            </td>

            <td>
              {{ getPatientName(appointment.patientId) }}
            </td>

            <td>
              {{
                getHealthcareUnitName(
                  appointment.healthcareUnitId
                )
              }}
            </td>

            <td>
              {{
                formatDateTime(
                  appointment.appointmentTime
                )
              }}
            </td>

            <td>
              <span
                class="status"
                :class="appointment.status.toLowerCase()"
              >
                {{ getStatusLabel(appointment.status) }}
              </span>
            </td>

            <td>
              <div
                v-if="canBook && appointment.status === 'SCHEDULED'"
                class="table-actions"
              >
                <button
                  type="button"
                  class="complete-btn"
                  :disabled="updatingAppointmentId !== null || loading"
                  @click="changeAppointmentStatus(appointment, 'COMPLETED')"
                >
                  Complete
                </button>
                <button
                  type="button"
                  class="cancel-appointment-btn"
                  :disabled="updatingAppointmentId !== null || loading"
                  @click="changeAppointmentStatus(appointment, 'CANCELLED')"
                >
                  Cancel
                </button>
              </div>
              <span v-else class="no-actions">—</span>
            </td>
          </tr>
        </tbody>
      </table>

      <p
        v-if="
          !loading &&
          !error &&
          appointments.length === 0
        "
        class="empty-message"
      >
        No appointments found.
      </p>
    </div>
  </section>
</template>

<style scoped>
.appointments-page {
  padding: 28px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: 8px;
  padding: 10px 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-btn {
  background: #e2e8f0;
  color: #334155;
}

.book-btn,
.save-btn {
  background: #2563eb;
  color: white;
}

.cancel-btn,
.close-btn {
  background: #f1f5f9;
  color: #334155;
}

.table-card,
.booking-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  overflow-x: auto;
}

.booking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.booking-header h2 {
  margin: 0;
  font-size: 19px;
  color: #0f172a;
}

.form-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-field label {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.form-field input,
.form-field select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 11px;
  background: white;
  font-size: 14px;
}

.form-field input:focus,
.form-field select:focus {
  outline: 2px solid #93c5fd;
  border-color: #2563eb;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

th {
  color: #475569;
  font-size: 13px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.complete-btn {
  background: #dcfce7;
  color: #166534;
}

.cancel-appointment-btn {
  background: #fee2e2;
  color: #991b1b;
}

.no-actions {
  color: #94a3b8;
}

.status {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.scheduled {
  color: #1d4ed8;
  background: #dbeafe;
}

.completed {
  color: #166534;
  background: #dcfce7;
}

.cancelled {
  color: #991b1b;
  background: #fee2e2;
}

.error {
  color: #b91c1c;
  margin: 16px 0;
  overflow-wrap: anywhere;
}

.empty-message {
  text-align: center;
  color: #64748b;
  padding: 24px;
}

@media (max-width: 650px) {
  .appointments-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
  }

  .booking-header {
    align-items: flex-start;
  }
}
</style>
