
import { keycloak } from "../auth/keycloak";

const API_URL = "http://localhost:8080";

export type AppointmentStatus =
  | "SCHEDULED"
  | "COMPLETED"
  | "CANCELLED";

export interface Appointment {
  id: number;
  patientId: number;
  healthcareUnitId: number;
  appointmentTime: string;
  status: AppointmentStatus;
}

export interface AppointmentRequest {
  patientId: number;
  healthcareUnitId: number;
  appointmentTime: string;
}

async function getAuthHeaders(): Promise<HeadersInit> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  return {
    Authorization: `Bearer ${keycloak.token}`,
    "Content-Type": "application/json",
  };
}

async function handleResponse<T>(
  response: Response
): Promise<T> {
  if (!response.ok) {
    const message = await response.text();

    throw new Error(
      message ||
      `Request failed with status ${response.status}`
    );
  }

  return (await response.json()) as T;
}

export async function getAppointments(): Promise<Appointment[]> {
  const response = await fetch(`${API_URL}/api/appointments`, {
    headers: await getAuthHeaders(),
  });

  return handleResponse<Appointment[]>(response);
}

export async function getAppointmentById(
  id: number
): Promise<Appointment> {
  const response = await fetch(`${API_URL}/api/appointments/${id}`, {
    headers: await getAuthHeaders(),
  });

  return handleResponse<Appointment>(response);
}

export async function createAppointment(
  request: AppointmentRequest
): Promise<Appointment> {
  const response = await fetch(`${API_URL}/api/appointments`, {
    method: "POST",
    headers: await getAuthHeaders(),
    body: JSON.stringify(request),
  });

  return handleResponse<Appointment>(response);
}

export async function updateAppointmentStatus(
  id: number,
  status: "COMPLETED" | "CANCELLED"
): Promise<Appointment> {
  const response = await fetch(
    `${API_URL}/api/appointments/${id}/status`,
    {
      method: "PATCH",
      headers: await getAuthHeaders(),
      body: JSON.stringify(status),
    }
  );

  return handleResponse<Appointment>(response);
}
