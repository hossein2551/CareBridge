<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { keycloak } from "../auth/keycloak";
import {
  createAppointment,
  filterAppointmentsPage,
  getBookedTimes,
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

// Server-side filtering and pagination
const patientSearch = ref("");
const selectedUnitId = ref<number | null>(null);
const selectedStatus = ref<AppointmentStatus | null>(null);
const currentPage = ref(0);
const pageSize = 10;
const totalAppointments = ref(0);
const totalPages = ref(0);
let searchTimer: ReturnType<typeof setTimeout> | undefined;
let latestRequest = 0;

// Loading and errors
const loading = ref(false);
const error = ref("");

const booking = ref(false);
const bookingError = ref("");
const showBookingForm = ref(false);
const route = useRoute();

function openBookingFromCalendar(): void {
  const date = route.query.date;

  if (typeof date !== "string") return;

  // Accept valid calendar dates only
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return;

  const parsedDate = new Date(`${date}T00:00:00`);

  if (
    Number.isNaN(parsedDate.getTime()) ||
    parsedDate.getFullYear() !== Number(date.slice(0, 4)) ||
    parsedDate.getMonth() + 1 !== Number(date.slice(5, 7)) ||
    parsedDate.getDate() !== Number(date.slice(8, 10))
  ) {
    return;
  }

  if (!canBook.value) return;

  openBookingForm();

  bookingForm.value.appointmentTime = `${date}T09:00`;
}
// Permissions
const canBook = computed(() =>
  ["ADMIN", "DOCTOR"].some((role) => keycloak.hasRealmRole(role))
);

// Booking form
const bookingForm = ref({
  patientId: null as number | null,
  healthcareUnitId: null as number | null,
  appointmentTime: "",
});
// Clear previous booking errors when the form changes
watch(
  bookingForm,
  () => {
    bookingError.value = "";
  },
  { deep: true }
);
// Booked times
const bookedTimes = ref<string[]>([]);
const loadingBookedTimes = ref(false);
const bookedTimesError = ref("");
let bookedTimesRequestId = 0;

const selectedBookingDate = computed(() =>
  bookingForm.value.appointmentTime.slice(0, 10)
);
const selectedBookingTime = computed(() =>
  bookingForm.value.appointmentTime.slice(11, 16)
);

const isSelectedTimeBooked = computed(() =>
  !loadingBookedTimes.value &&
  !bookedTimesError.value &&
  bookedTimes.value.includes(selectedBookingTime.value)
);

watch(
  [() => bookingForm.value.healthcareUnitId, selectedBookingDate],
  async ([unitId, date]) => {
    const requestId = ++bookedTimesRequestId;
    bookedTimes.value = [];
    bookedTimesError.value = "";

    if (unitId === null || !date) {
      loadingBookedTimes.value = false;
      return;
    }

    loadingBookedTimes.value = true;
    try {
      const times = await getBookedTimes(unitId, date);
      if (requestId !== bookedTimesRequestId) return;
      bookedTimes.value = times;
    } catch (err) {
      if (requestId !== bookedTimesRequestId) return;
      bookedTimesError.value =
        err instanceof Error ? err.message : "Could not load booked times.";
    } finally {
      if (requestId === bookedTimesRequestId) {
        loadingBookedTimes.value = false;
      }
    }
  }
);

function getPatientName(patientId: number): string {
  const patient = patients.value.find((item) => item.id === patientId);
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

// Load only the current page of appointments from the server
async function loadAppointments(page = currentPage.value): Promise<void> {
  const request = ++latestRequest;
  loading.value = true;
  error.value = "";

  try {
    const result = await filterAppointmentsPage(
      patientSearch.value,
      selectedUnitId.value,
      selectedStatus.value,
      page,
      pageSize
    );

    if (request !== latestRequest) return;

    appointments.value = result.appointments;
    currentPage.value = result.page;
    totalAppointments.value = result.totalAppointments;
    totalPages.value = result.totalPages;
  } catch (err) {
    if (request !== latestRequest) return;
    error.value = err instanceof Error ? err.message : "Failed to load appointments.";
    appointments.value = [];
  } finally {
    if (request === latestRequest) loading.value = false;
  }
}

async function loadReferenceData(): Promise<void> {
  try {
    const [patientsData, unitsData] = await Promise.all([
      getPatients(),
      getHealthcareUnits(),
    ]);
    patients.value = patientsData;
    healthcareUnits.value = unitsData;
  } catch (err) {
    error.value = err instanceof Error ? err.message : "Failed to load patients or healthcare units.";
  }
}

function changePage(page: number): void {
  if (page < 0 || page >= totalPages.value || loading.value) return;
  void loadAppointments(page);
}

// Search is debounced; every filter change resets to the first page.
watch(patientSearch, () => {
  if (searchTimer !== undefined) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    searchTimer = undefined;
    void loadAppointments(0);
  }, 350);
});

watch([selectedUnitId, selectedStatus], () => {
  if (searchTimer !== undefined) {
    clearTimeout(searchTimer);
    searchTimer = undefined;
  }
  void loadAppointments(0);
});

onUnmounted(() => {
  if (searchTimer !== undefined) clearTimeout(searchTimer);
  latestRequest++;
});

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

    await loadAppointments(0);
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
  void loadReferenceData();
  void loadAppointments(0);
  openBookingFromCalendar();
});
// Update booking form when calendar date changes
watch(
  () => route.query.date,
  (newDate, oldDate) => {
    if (newDate !== oldDate) {
      openBookingFromCalendar();
    }
  }
);
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
          @click="loadAppointments()"
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
{{ unit.name }} - {{ unit.city }}              </option>
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

<div
  v-if="bookingForm.healthcareUnitId !== null && selectedBookingDate"
  class="booked-times-panel"
>
  <h3>Booked times for {{ selectedBookingDate }}</h3>

  <p v-if="loadingBookedTimes">
    Loading booked times...
  </p>

  <p v-else-if="bookedTimesError" class="error">
    {{ bookedTimesError }}
  </p>

  <p v-else-if="bookedTimes.length === 0">
    No booked times found for this date.
  </p>

  <div v-else class="booked-times-list">
    <span
      v-for="time in bookedTimes"
      :key="time"
      class="booked-time"
    >
      {{ time }}
    </span>
  </div>
</div>
<p
  v-if="isSelectedTimeBooked"
  class="error"
  role="alert"
>
  This time is already booked. Please select another time.
</p>

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
            :disabled="booking || isSelectedTimeBooked || loadingBookedTimes || !!bookedTimesError"
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

    <!-- Search and server-side filters -->
    <div class="filter-card">
      <div class="filters-grid">
        <div class="form-field">
          <label for="appointment-search">Search patient name</label>
          <input
            id="appointment-search"
            v-model="patientSearch"
            type="search"
            placeholder="Search by first or last name..."
          />
        </div>
        <div class="form-field">
          <label for="appointment-unit-filter">Healthcare unit</label>
          <select id="appointment-unit-filter" v-model="selectedUnitId">
            <option :value="null">All healthcare units</option>
            <option v-for="unit in healthcareUnits" :key="unit.id" :value="unit.id">
              {{ unit.name }} - {{ unit.city }}
            </option>
          </select>
        </div>
        <div class="form-field">
          <label for="appointment-status-filter">Status</label>
          <select id="appointment-status-filter" v-model="selectedStatus">
            <option :value="null">All statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>
      <p class="results-count" aria-live="polite">
        {{ totalAppointments }} matching appointments
      </p>
    </div>

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
<span v-else class="no-actions">-</span>            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!error && !loading" class="pagination">
        <span>
          Page {{ currentPage + 1 }} of {{ Math.max(totalPages, 1) }}
          ({{ totalAppointments }} total)
        </span>
        <div class="pagination-buttons">
          <button type="button" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">
            Previous
          </button>
          <button type="button" :disabled="currentPage + 1 >= totalPages" @click="changePage(currentPage + 1)">
            Next
          </button>
        </div>
      </div>

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
.booking-card,
.filter-card {
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
.filters-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.results-count {
  margin: 14px 0 0;
  font-size: 13px;
  color: #64748b;
}

.pagination,
.pagination-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 18px;
  color: #475569;
  font-size: 14px;
}

.pagination-buttons button {
  background: #e2e8f0;
  color: #334155;
}

@media (max-width: 800px) {
  .filters-grid { grid-template-columns: 1fr; }
}

.booked-times-panel {
  margin-top: 16px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.booked-times-panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #334155;
}

.booked-times-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.booked-time {
  background: #fee2e2;
  color: #991b1b;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
}

</style>
