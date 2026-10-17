<script setup lang="ts">
  import {computed,onMounted,ref} from "vue";
  import {useRouter} from "vue-router";
  import {keycloak} from "../auth/keycloak";

  import {
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
  const appointments=ref<Appointment[]>([]);
  const patients=ref<Patient[]>([]);
  const healthcareUnits=ref<HealthcareUnit[]>([]);

  const loading=ref(false);
  const error=ref("");
  // Selected appointment details
  const selectedAppointment=ref<Appointment|null>(null);

  const updatingStatus=ref(false);
  const statusError=ref("");

  const canManageAppointments=computed(() =>
    ["ADMIN","DOCTOR"].some((role) =>
      keycloak.hasRealmRole(role)
    )
  );

  async function changeStatus(
    status: "COMPLETED"|"CANCELLED"
  ): Promise<void> {
    const appointment=selectedAppointment.value;

    if(
      !appointment||
      !canManageAppointments.value||
      appointment.status!=="SCHEDULED"||
      updatingStatus.value
    ) {
      return;
    }

    const action=status==="COMPLETED"? "complete":"cancel";

    if(!window.confirm(`Are you sure you want to ${action} this appointment?`)) {
      return;
    }

    updatingStatus.value=true;
    statusError.value="";

    try {
      const updated=await updateAppointmentStatus(appointment.id,status);

      appointments.value=appointments.value.map((item) =>
        item.id===updated.id? updated:item
      );

      selectedAppointment.value=updated;
    } catch(err) {
      statusError.value=
        err instanceof Error
          ? err.message
          :"Failed to update appointment status.";
    } finally {
      updatingStatus.value=false;
    }
  }

  function openAppointmentDetails(appointment: Appointment): void {
    selectedAppointment.value=appointment;
  }

  function closeAppointmentDetails(): void {
    selectedAppointment.value=null;
  }
  const selectedUnitId=ref<number|null>(null);
  const selectedStatus=ref<AppointmentStatus|null>(null);

  // Calendar settings
  type CalendarMode="day"|"week";

  const mode=ref<CalendarMode>("week");
  const selectedDate=ref(new Date());
  const router=useRouter();

  const canCreateAppointment=computed(() =>
    ["ADMIN","DOCTOR"].some((role) =>
      keycloak.hasRealmRole(role)
    )
  );

  function bookFromCalendar(): void {
    void router.push({
      path: "/appointments",
      query: {
        date: dateKey(selectedDate.value),
      },
    });
  }
  function dateKey(date: Date): string {
    const year=date.getFullYear();
    const month=String(date.getMonth()+1).padStart(2,"0");
    const day=String(date.getDate()).padStart(2,"0");

    return `${year}-${month}-${day}`;
  }

  function dateFromKey(value: string): Date {
    const [year,month,day]=value.split("-").map(Number);
    return new Date(year,month-1,day);
  }

  function addDays(date: Date,amount: number): Date {
    const result=new Date(date);
    result.setDate(result.getDate()+amount);
    return result;
  }

  function startOfWeek(date: Date): Date {
    const result=new Date(date);
    const weekday=(result.getDay()+6)%7;
    result.setDate(result.getDate()-weekday);
    return result;
  }

  const visibleDays=computed(() => {
    if(mode.value==="day") {
      return [new Date(selectedDate.value)];
    }

    const firstDay=startOfWeek(selectedDate.value);

    return Array.from(
      {length: 7},
      (_,index) => addDays(firstDay,index)
    );
  });

  const calendarTitle=computed(() => {
    if(mode.value==="day") {
      return selectedDate.value.toLocaleDateString("sv-SE",{
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      });
    }

    const first=visibleDays.value[0];
    const last=visibleDays.value[6];

    return `${first.toLocaleDateString("sv-SE",{
      day: "numeric",
      month: "short",
    })} – ${last.toLocaleDateString("sv-SE",{
      day: "numeric",
      month: "short",
      year: "numeric",
    })}`;
  });

  const totalVisibleAppointments=computed(() =>
    visibleDays.value.reduce(
      (total,day) => total+appointmentsForDay(day).length,
      0
    )
  );

  // Appointment helpers
  function appointmentsForDay(date: Date): Appointment[] {
    const selected=dateKey(date);

    return appointments.value
      .filter((appointment) => {
        const matchesDate=
          appointment.appointmentTime.slice(0,10)===selected;

        const matchesUnit=
          selectedUnitId.value===null||
          appointment.healthcareUnitId===selectedUnitId.value;

        const matchesStatus=
          selectedStatus.value===null||
          appointment.status===selectedStatus.value;

        return matchesDate&&matchesUnit&&matchesStatus;
      })
      .sort((a,b) =>
        a.appointmentTime.localeCompare(b.appointmentTime)
      );
  }

  function getPatientName(patientId: number): string {
    const patient=patients.value.find(
      (item) => item.id===patientId
    );

    return patient
      ? `${patient.firstName} ${patient.lastName}`
      :`Patient ${patientId}`;
  }

  function getHealthcareUnitName(unitId: number): string {
    const unit=healthcareUnits.value.find(
      (item) => item.id===unitId
    );

    return unit? unit.name:`Unit ${unitId}`;
  }

  function getAppointmentTime(value: string): string {
    return value.slice(11,16);
  }

  function getStatusLabel(status: AppointmentStatus): string {
    switch(status) {
      case "SCHEDULED":
        return "Scheduled";
      case "COMPLETED":
        return "Completed";
      case "CANCELLED":
        return "Cancelled";
    }
  }

  function isToday(date: Date): boolean {
    return dateKey(date)===dateKey(new Date());
  }

  // Navigation
  function previousPeriod(): void {
    const amount=mode.value==="day"? -1:-7;
    selectedDate.value=addDays(selectedDate.value,amount);
  }

  function nextPeriod(): void {
    const amount=mode.value==="day"? 1:7;
    selectedDate.value=addDays(selectedDate.value,amount);
  }

  function goToToday(): void {
    selectedDate.value=new Date();
  }

  function selectDay(date: Date): void {
    selectedDate.value=new Date(date);
    mode.value="day";
  }

  function changeDate(event: Event): void {
    const input=event.target as HTMLInputElement;

    if(input.value) {
      selectedDate.value=dateFromKey(input.value);
    }
  }

  // Load real data from Quarkus
  async function loadCalendar(): Promise<void> {
    loading.value=true;
    error.value="";

    try {
      const [appointmentsData,patientsData,unitsData]=
        await Promise.all([
          getAppointments(),
          getPatients(),
          getHealthcareUnits(),
        ]);

      appointments.value=appointmentsData;
      patients.value=patientsData;
      healthcareUnits.value=unitsData;
    } catch(err) {
      error.value=
        err instanceof Error
          ? err.message
          :"Could not load calendar data.";
    } finally {
      loading.value=false;
    }
  }

  onMounted(() => {
    void loadCalendar();
  });
</script>

<template>
  <section class="calendar-page">
    <header class="page-header">
      <div>
        <h1>Appointment Calendar</h1>
        <p>View and manage healthcare appointments.</p>
      </div>

      <button class="refresh-btn" :disabled="loading" @click="loadCalendar">
        {{ loading ? "Loading..." : "Refresh" }}
      </button>
    </header>

    <div class="calendar-toolbar">
      <div class="navigation">
        <button @click="previousPeriod">
          Previous
        </button>

        <button @click="goToToday">
          Today
        </button>

        <button @click="nextPeriod">
          Next
        </button>
      </div>
      <button v-if="canCreateAppointment" class="book-calendar-btn" @click="bookFromCalendar">
        + Book appointment
      </button>
      <div class="date-controls">
        <input aria-label="Select calendar date" type="date" :value="dateKey(selectedDate)" @change="changeDate" />

        <select v-model="mode" aria-label="Calendar view">
          <option value="day">Day</option>
          <option value="week">Week</option>
        </select>
      </div>
    </div>

    <div class="calendar-filters">
      <div class="filter-field">
        <label for="calendar-unit">Healthcare unit</label>
        <select id="calendar-unit" v-model="selectedUnitId">
          <option :value="null">All healthcare units</option>
          <option v-for="unit in healthcareUnits" :key="unit.id" :value="unit.id">
            {{ unit.name }} - {{ unit.city }}
          </option>
        </select>
      </div>

      <div class="filter-field">
        <label for="calendar-status">Appointment status</label>
        <select id="calendar-status" v-model="selectedStatus">
          <option :value="null">All statuses</option>
          <option value="SCHEDULED">Scheduled</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>
    </div>

    <div class="calendar-heading">
      <h2>{{ calendarTitle }}</h2>
      <p>
        {{ totalVisibleAppointments }}
        appointments in this period
      </p>
    </div>

    <p v-if="error" class="error" role="alert">
      {{ error }}
    </p>

    <div v-if="loading" class="loading-card">
      Loading appointments...
    </div>

    <div v-else-if="!error" class="calendar-grid" :class="{ 'day-mode': mode === 'day' }">
      <div v-for="day in visibleDays" :key="dateKey(day)" class="calendar-day" :class="{ today: isToday(day) }">
        <button class="day-header" @click="selectDay(day)">
          <span class="weekday">
            {{
            day.toLocaleDateString("sv-SE", {
            weekday: "short",
            })
            }}
          </span>

          <span class="day-number">
            {{ day.getDate() }}
          </span>

          <span class="appointment-count">
            {{ appointmentsForDay(day).length }} visits
          </span>
        </button>

        <div class="day-content">
          <p v-if="appointmentsForDay(day).length === 0" class="empty-day">
            No appointments
          </p>

          <div v-for="appointment in appointmentsForDay(day)" :key="appointment.id" class="appointment-card"
            :class="appointment.status.toLowerCase()" role="button" tabindex="0"
            @click="openAppointmentDetails(appointment)" @keydown.enter="openAppointmentDetails(appointment)"
            @keydown.space.prevent="openAppointmentDetails(appointment)">
            <strong class="appointment-time">
              {{ getAppointmentTime(appointment.appointmentTime) }}
            </strong>

            <span class="patient-name">
              {{ getPatientName(appointment.patientId) }}
            </span>

            <span class="unit-name">
              {{ getHealthcareUnitName(appointment.healthcareUnitId) }}
            </span>

            <span class="status-badge" :class="appointment.status.toLowerCase()">
              {{ getStatusLabel(appointment.status) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <p class="calendar-note">
      Select a day to open the daily view.
      Appointments are loaded from CareBridge API.
    </p>

    <div v-if="selectedAppointment" class="details-overlay" @click.self="closeAppointmentDetails">
      <div class="details-panel" role="dialog" aria-modal="true" aria-label="Appointment details">
        <div class="details-header">
          <h2>Appointment details</h2>
          <button @click="closeAppointmentDetails">Close</button>
        </div>

        <p>
          <strong>Patient:</strong>
          {{ getPatientName(selectedAppointment.patientId) }}
        </p>

        <p>
          <strong>Healthcare unit:</strong>
          {{ getHealthcareUnitName(selectedAppointment.healthcareUnitId) }}
        </p>

        <p>
          <strong>Date:</strong>
          {{ selectedAppointment.appointmentTime.slice(0, 10) }}
        </p>

        <p>
          <strong>Time:</strong>
          {{ getAppointmentTime(selectedAppointment.appointmentTime) }}
        </p>

        <p>
          <strong>Status:</strong>
          {{ getStatusLabel(selectedAppointment.status) }}
        </p>

        <p v-if="statusError" class="error" role="alert">
          {{ statusError }}
        </p>

        <div v-if="canManageAppointments && selectedAppointment.status === 'SCHEDULED'" class="details-actions">
          <button class="complete-action" :disabled="updatingStatus" @click="changeStatus('COMPLETED')">
            Complete
          </button>

          <button class="cancel-action" :disabled="updatingStatus" @click="changeStatus('CANCELLED')">
            Cancel appointment
          </button>
        </div>

      </div>
    </div>

  </section>
</template>

<style scoped>
  .calendar-page {
    padding: 28px;
    color: #0f172a;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
  }

  .page-header h1 {
    margin: 0 0 8px;
  }

  .page-header p,
  .calendar-heading p,
  .calendar-note {
    color: #64748b;
  }

  button,
  select,
  input {
    font: inherit;
  }

  button {
    border: 0;
    border-radius: 8px;
    padding: 10px 14px;
    cursor: pointer;
    background: #e2e8f0;
    color: #334155;
  }

  button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .refresh-btn {
    background: #2563eb;
    color: white;
  }

  .calendar-toolbar {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
  }

  .navigation,
  .date-controls {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
  }

  .date-controls input,
  .date-controls select {
    padding: 10px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: white;
    color: #334155;
  }

  .calendar-heading {
    margin: 24px 0 16px;
  }

  .calendar-heading h2 {
    margin: 0 0 6px;
    text-transform: capitalize;
  }

  .calendar-heading p {
    margin: 0;
    font-size: 14px;
  }

  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 10px;
  }

  .calendar-grid.day-mode {
    grid-template-columns: minmax(0, 1fr);
  }

  .calendar-day {
    min-width: 0;
    min-height: 240px;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    overflow: hidden;
  }

  .calendar-day.today {
    border: 2px solid #2563eb;
  }

  .day-header {
    width: 100%;
    border-radius: 0;
    background: #f1f5f9;
    display: flex;
    flex-direction: column;
    gap: 6px;
    align-items: flex-start;
    text-align: left;
  }

  .today .day-header {
    background: #dbeafe;
  }

  .weekday {
    font-size: 12px;
    color: #64748b;
    text-transform: capitalize;
  }

  .day-number {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
  }

  .appointment-count {
    font-size: 11px;
    color: #64748b;
  }

  .day-content {
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .empty-day {
    color: #94a3b8;
    font-size: 12px;
    text-align: center;
    padding: 15px 0;
  }

  .appointment-card {
    display: flex;
    flex-direction: column;
    gap: 5px;
    min-width: 0;
    padding: 10px;
    border-radius: 8px;
    border-left: 4px solid #2563eb;
    background: #eff6ff;
    overflow-wrap: anywhere;
  }

  .appointment-card.completed {
    border-left-color: #16a34a;
    background: #f0fdf4;
  }

  .appointment-card.cancelled {
    border-left-color: #dc2626;
    background: #fef2f2;
  }

  .appointment-time {
    color: #0f172a;
    font-size: 14px;
  }

  .patient-name {
    font-size: 12px;
    font-weight: 600;
  }

  .unit-name {
    font-size: 11px;
    color: #64748b;
  }

  .status-badge {
    font-size: 10px;
    font-weight: 700;
    color: #1d4ed8;
    text-transform: uppercase;
  }

  .status-badge.completed {
    color: #15803d;
  }

  .status-badge.cancelled {
    color: #b91c1c;
  }

  .loading-card {
    background: white;
    padding: 30px;
    border-radius: 12px;
  }

  .error {
    color: #b91c1c;
    margin: 16px 0;
  }

  .calendar-note {
    margin-top: 18px;
    font-size: 12px;
  }

  @media (max-width: 1100px) {
    .calendar-grid {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .calendar-grid.day-mode {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 650px) {
    .calendar-page {
      padding: 16px;
    }

    .page-header {
      align-items: flex-start;
      flex-direction: column;
    }

    .calendar-grid {
      grid-template-columns: 1fr;
    }
  }

  .calendar-filters {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
    margin-top: 16px;
    padding: 16px;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
  }

  .filter-field {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .filter-field label {
    font-size: 13px;
    font-weight: 600;
    color: #334155;
  }

  .filter-field select {
    width: 100%;
    padding: 10px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: white;
  }

  @media (max-width: 650px) {
    .calendar-filters {
      grid-template-columns: 1fr;
    }
  }

  .appointment-card {
    cursor: pointer;
  }

  .appointment-card:focus-visible {
    outline: 3px solid #2563eb;
    outline-offset: 2px;
  }

  .details-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.55);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
    z-index: 1000;
  }

  .details-panel {
    width: 100%;
    max-width: 450px;
    background: white;
    padding: 24px;
    border-radius: 14px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  }

  .details-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
  }

  .details-header h2 {
    margin: 0;
  }

  .details-panel p {
    margin: 14px 0;
  }

  .details-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 24px;
  }

  .complete-action {
    background: #dcfce7;
    color: #166534;
  }

  .cancel-action {
    background: #fee2e2;
    color: #991b1b;
  }

  .book-calendar-btn {
    background: #2563eb;
    color: white;
    font-weight: 600;
  }
</style>