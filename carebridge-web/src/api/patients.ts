
import { keycloak } from "../auth/keycloak";

const API_URL = "http://localhost:8080";

// Patient data returned from backend
export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  birthYear: number;
  healthcareUnitId: number | null;
}

// Data required to create or update a patient
export interface PatientRequest {
  firstName: string;
  lastName: string;
  birthYear: number;
  healthcareUnitId: number | null;
}

// Paginated patient response
export interface PatientPage {
  patients: Patient[];
  page: number;
  size: number;
  totalPatients: number;
  totalPages: number;
}

// Retrieve a valid Keycloak access token
async function getAccessToken(): Promise<string> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  if (!keycloak.token) {
    throw new Error("No access token available.");
  }

  return keycloak.token;
}

// Build authorization headers for API requests
async function getPatientAuthHeaders(
  includeContentType = false
): Promise<Headers> {
  const token = await getAccessToken();

  const headers = new Headers({
    Authorization: `Bearer ${token}`,
    Accept: "application/json",
  });

  if (includeContentType) {
    headers.set("Content-Type", "application/json");
  }

  return headers;
}

// Handle API errors
function checkResponse(response: Response): void {
  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 403) {
    throw new Error("Access denied (403).");
  }

  if (response.status === 404) {
    throw new Error("Patient not found (404).");
  }

  if (!response.ok) {
    throw new Error(
      `Patient API request failed: ${response.status}`
    );
  }
}

// Get all patients
export async function getPatients(): Promise<Patient[]> {
  const response = await fetch(`${API_URL}/api/patients`, {
    method: "GET",
    headers: await getPatientAuthHeaders(),
  });

  checkResponse(response);

  return (await response.json()) as Patient[];
}

// Test that DOCTOR cannot delete patients
export async function testDoctorDeletePermission(): Promise<number> {
  const response = await fetch(
    `${API_URL}/api/patients/999999999`,
    {
      method: "DELETE",
      headers: await getPatientAuthHeaders(),
    }
  );

  return response.status;
}

// Get paginated patients
export async function getPatientsPage(
  page: number = 0,
  size: number = 10
): Promise<PatientPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const response = await fetch(
    `${API_URL}/api/patients/page?${params.toString()}`,
    {
      method: "GET",
      headers: await getPatientAuthHeaders(),
    }
  );

  checkResponse(response);

  return (await response.json()) as PatientPage;
}

// Search across all patients with pagination
export async function searchPatientsPage(
  name: string,
  page: number = 0,
  size: number = 10
): Promise<PatientPage> {
  const params = new URLSearchParams({
    name,
    page: String(page),
    size: String(size),
  });

  const response = await fetch(
    `${API_URL}/api/patients/search/page?${params.toString()}`,
    {
      method: "GET",
      headers: await getPatientAuthHeaders(),
    }
  );

  checkResponse(response);

  return (await response.json()) as PatientPage;
}

// Create a new patient - ADMIN or DOCTOR
export async function createPatient(
  patient: PatientRequest
): Promise<Patient> {
  const response = await fetch(`${API_URL}/api/patients`, {
    method: "POST",
    headers: await getPatientAuthHeaders(true),
    body: JSON.stringify(patient),
  });

  checkResponse(response);

  return (await response.json()) as Patient;
}

// Update a patient - ADMIN or DOCTOR
export async function updatePatient(
  id: number,
  patient: PatientRequest
): Promise<Patient> {
  const response = await fetch(
    `${API_URL}/api/patients/${id}`,
    {
      method: "PUT",
      headers: await getPatientAuthHeaders(true),
      body: JSON.stringify(patient),
    }
  );

  checkResponse(response);

  return (await response.json()) as Patient;
}

// Delete a patient - ADMIN only
export async function deletePatient(
  id: number
): Promise<void> {
  const response = await fetch(
    `${API_URL}/api/patients/${id}`,
    {
      method: "DELETE",
      headers: await getPatientAuthHeaders(),
    }
  );

  if (response.status === 403) {
    throw new Error(
      "Only administrators can delete patients."
    );
  }

  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 404) {
    throw new Error("Patient not found (404).");
  }

  if (response.status !== 204) {
    throw new Error(
      `Failed to delete patient: ${response.status}`
    );
  }
  
}

export async function filterPatientsPage(
  name: string,
  healthcareUnitId: number | null,
  page: number = 0,
  size: number = 10
): Promise<PatientPage> {
  const params = new URLSearchParams({
    name,
    page: String(page),
    size: String(size),
  });

  if (healthcareUnitId !== null) {
    params.set("healthcareUnitId", String(healthcareUnitId));
  }

  const response = await fetch(
    `${API_URL}/api/patients/filter/page?${params.toString()}`,
    {
      method: "GET",
      headers: await getPatientAuthHeaders(),
    }
  );

  checkResponse(response);

  return (await response.json()) as PatientPage;
}
